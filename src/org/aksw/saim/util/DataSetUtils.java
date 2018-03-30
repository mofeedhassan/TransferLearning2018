package org.aksw.saim.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aksw.saim.io.KBIInfoMini;
import org.aksw.saim.io.ReadFile;
import org.aksw.saim.io.WriteFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.HybridCache;
import de.uni_leipzig.simba.io.KBInfo;

import com.google.common.base.CharMatcher;
import com.google.common.io.*;

public class DataSetUtils {

    static Logger logger = LoggerFactory.getLogger(DataSetUtils.class);

    private static String[] whiteList=  {"dynasty","caliphate","league","papacy"};
    
    /**
     * This method retrieves all the data exist in an endpoint and save it in dump
     * @param endpoint : the endpoint to be dumped
     * @param file : the file to dump the data in it
     */
    public static void getEndpointDump(String endpoint, String file , String offset_param){
   	 try {
   		 Model model = ModelFactory.createDefaultModel();
   		 Resource resource =null;
            Property property = null; 
            QuerySolution soln;
		int offset =Integer.valueOf(offset_param);
   		 int limit =1000;

   		 if(SparqlUtils.isAlive(endpoint, null))
   		 {
	    		 StringBuilder query = new StringBuilder();
	    		 query.append("SELECT * {?s ?p ?o } OFFSET ");
   				 query.append(offset);
   				 query.append(" LIMIT ");
   				 query.append(limit);
   				 logger.info("The query is : "+query+"\n");
	             Query sparqlQuery = QueryFactory.create(query.toString());
	             QueryExecution qexec;
	             qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery);
	             ResultSet results = qexec.execSelect();
	             
   			 while(true)
   			 {
   				 sparqlQuery = QueryFactory.create(query.toString());
   	             qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery);
   	             results = qexec.execSelect();
   	             
   				 if(!results.hasNext()){//empty query is returned
   	            	 logger.info("No more data to retrieve");
   	            	break; 
   	             }
   				 while(results.hasNext())
   	             {
   	            	 soln = results.nextSolution();
   	            	 resource = model.createResource(soln.get("?s").toString());
   	            	 property = model.createProperty(soln.get("?p").toString());
   	            	 resource.addProperty(property, soln.get("?o"));
   	             }
   				 
 					 
   				 
   				 if((offset%10000) == 0)
   				 {
   		   			 logger.info("Writing the dumped data into the file {0}"+file+"-"+(offset/10000)+".nt");
   		   			 FileWriter out = new FileWriter( file+"-"+(offset/10000)+".nt" );
   		   			 try {
   		   			     model.write( out, "N-TRIPLES" );
   		   			     model.removeAll();
   		   			 }
   		   			 finally {
   		   			    try {
   		   			        out.close();
   		   			        Thread.sleep(10000);

   		   			    }
   		   			    catch (IOException closeException) {
   		   			        logger.error("Error in writing the model into the file as "+closeException.getMessage());
   		   			        closeException.printStackTrace();
   		   			    }
   		   			 }
   				 }

   				 offset+=limit;
   				query=new StringBuilder("SELECT * {?s ?p ?o } OFFSET ");
  				 query.append(offset);
  				 query.append(" LIMIT ");
  				 query.append(limit);
  				 logger.info("The query is : "+query+"\n");
  				 
   			 }
    
   		 }
   		 
        } catch (Exception e) {
        	logger.error("Error while retrieving the data from teh endpoint as "+e.getMessage());
            LoggerFactory.getLogger(SparqlUtils.class).debug(e.getMessage());
        }

   }
    
    public static List<String> checkMissedURIs(String className, String endpoint, String dumpfile)
    {
    	List<String> uris=SparqlUtils.getClassUris(className, endpoint, -1, -1);
    	List<String> urismissed = new ArrayList<>();
    	Model model = ReadFile.readModel(dumpfile);
    	for (String uri : uris) {
    		if (model.getResource(uri) == null) 
    			urismissed.add(uri); 
		}
    	return urismissed;
    }
    public static boolean isEndpointContainsClass(String className, String endpoint)
    {
    	List<String> uris=SparqlUtils.getClassUris(className, endpoint, -1, 10);
    	return (uris.size()>0);
    }
    
	public static Map<Integer,List<String>> getDisjointDataSetsSample(String className1, String endpoint1, String className2, String endpoint2, String comparisonProperty, int sampleSize)
	{
		Map<Integer,List<String>> disjointDataset = new HashMap<>();
		
		List<String> dsUris = SparqlUtils.getClassUris(className1, endpoint1, 0, sampleSize);
		Set<String> uriProperrites=null;
		Set<String> ds1AllPropertiesPool = new HashSet<>();
		for (String uri : dsUris) {
			uriProperrites = SparqlUtils.getUriPropertyValues(uri, comparisonProperty, endpoint1);
			ds1AllPropertiesPool.addAll(uriProperrites);
		}
		disjointDataset.put(1, dsUris);
		disjointDataset.put(2, new ArrayList<>());

		dsUris = SparqlUtils.getClassUris(className2, endpoint2, 0, 2*sampleSize);
		List<String> ds2Uris = new ArrayList<>();
		boolean found = false;
		for (String uri2 : dsUris) {
			found = false;
			uriProperrites = SparqlUtils.getUriPropertyValues(uri2, comparisonProperty, endpoint2);
			for (String uri2Propery : uriProperrites) {
				if(ds1AllPropertiesPool.contains(uri2Propery))
					found=true;
			}
			if(!found)
				disjointDataset.get(2).add(uri2);
		}
		return disjointDataset;		
	}
	
	public static Model createMiniDumpForClass(String className, String dataRepository,boolean endpoint, String fileName)
	{
		
		Model model = null;
		logger.info("Retrieve all the URIs of type "+className);
		List<String> uris=null;
		
			uris = SparqlUtils.getClassUris(className, dataRepository, -1, -1);
			long counter=0;
			for (String uri : uris) {
				try
				{
					model = SparqlUtils.getAllDataOfUri(uri, dataRepository);
					if(null!=model)
					{
						logger.info(model.size()+ " triples of "+uri);
						WriteFile.writeModel(model, fileName+"-"+counter++, "nt");
						model.removeAll();

					}
					else
						logger.error("Not retrieved tripeles for URI= "+uri);
				}
				catch(Exception e)
				{ 
					logger.error("Error as"+e.getMessage());
					e.printStackTrace();
				}
				finally
				{
					continue; //keep going for other uris
				}
			}
			
		
		return model;	
		
	}
	
	/*public static Model createMiniDumpForClass(String className, String dataRepository,boolean endpoint, String fileName)
	{
		
		Model model = null;
		logger.info("Retrieve all the URIs of type "+className);
		List<String> uris=null;
		List<String> finishedUris=new ArrayList<>();
		try
		{
			if((new File("uris").exists()))
			{
				logger.info("get data for URIs in file and save it locally");
				uris=ReadFile.readFile("uris");
				WriteFile.writeToFile(uris, "uris");
			}
				
			else
			{
				uris = SparqlUtils.getUris(className, dataRepository, -1, -1);
			}
			long counter=0;
			for (String uri : uris) {
				model = SparqlUtils.getAllDataOfUri(uri, dataRepository);
				if(null!=model)
				{
					logger.info(model.size()+ " triples of "+uri);
			    	WriteFile.writeModel(model, fileName+"-"+counter++, "nt");
			    	model.removeAll();
			    	finishedUris.add(uri);
			    	if(counter==3)
			    		throw new Exception();
				}
				else
					logger.error("Not retrieved tripeles for URI= "+uri);
			}
			
		}
		catch(Exception e)
		{ 
			logger.error("Error as"+e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			uris.removeAll(finishedUris);
			WriteFile.writeToFile(uris, "uris");
		}
		return model;	
		if(endpoint)
			return createMiniDumpFromEdnpoint(uris, className, dataRepository, requiredSize);
		else
			return createMiniDumpFromDump(uris, className, dataRepository, requiredSize);
			
	}*/

	
	/**
	 * The method restrieves the dump data for the list of URIs of a given class
	 * @param uris : List of URIs to get the dumop for them
	 * @param className : the type of URIs to rtrieve
	 * @param dataRepository : The sourc od data to get the dump from. It is either enpoint or dump file
	 * @param requiredSize :  The size of the dump to retrieve = #resources you want to get data about
	 * @param endpoint : flag if the repository is an endpoint or a big dump file
	 * @return : the cached dump data
	 */
	public static Model createMiniDumpForURIsList(List<String> uris, String className, String dataRepository,int requiredSize,boolean endpoint)
	{
		if(endpoint)
			return createMiniDumpFromEdnpointForClass(uris, className, dataRepository, requiredSize);
		else
			return createMiniDumpFromDumpForClass(uris, className, dataRepository, requiredSize);
			
	}

	/**
	 * get subjects of specific data class in a repository and store it into file
	 * @param className
	 * @param repository
	 * @param file
	 */
	public static void getClassURIs (String className, String repository, String file)
	{
		List<String> uris = new ArrayList<>();
		Model model = ModelFactory.createDefaultModel();
		String query = "SELECT * WHERE {?s ?p ?o . "
									+ "?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> "+className+" }";
		QueryExecution qexec =null;
		QuerySolution soln=null;
		Resource resource=null;
		ResultSet results=null;
		if(repository.startsWith("http"))
		{
			qexec = QueryExecutionFactory.sparqlService(repository, query);
            results = qexec.execSelect(); 
            if(results.hasNext()) // there are data retrieved for this uri
            {
            	while (results.hasNext()) {
            		soln = results.nextSolution();
            		uris.add(soln.get("s").toString());
            	}
            }

		}
		else
		{
			model = ReadFile.readModel(repository);
			qexec = QueryExecutionFactory.create(query, model);
			results = qexec.execSelect(); 
			if(results.hasNext()) // there are data retrieved for this uri
            {
            	while (results.hasNext()) {
            		soln = results.nextSolution();
            		uris.add(soln.get("s").toString());
            	}
            }

		}
		WriteFile.writeToFile(uris, file);
	}
	/**
	 * It reads list of URIs either from a linking file or from standalone file
	 */
	private static List<String> extractURIs(String file, boolean isRightSide, boolean isLinksFile)
	{
		if(isLinksFile) // the uris are extracted from a linking file with format S samaAs T
			return ReadFile.readLinksURIs(file, isRightSide);
		else
			return ReadFile.readFile(file);
	}
	/**
	 * It dumps list of URIs either in a link file or standalone file FROM endpoint or dump file
	 * @param urisFile
	 * @param isRightSide
	 * @param isLinksFile
	 */
	public static void dumpURIs(String urisFile, String repository, String dumpFolder, boolean isRightSide, boolean isLinksFile)
	{
		
		Model model = null;
		List<String> uris =null;
		List<String> missedURIs= new ArrayList<>();
		int counter = 0;
		String workingURI="";
		
			uris = extractURIs(urisFile, isRightSide, isLinksFile);
		
			for (String uri : uris) {
				workingURI = uri;
				try{
				model = cacheURI(uri, repository);
				WriteFile.writeModel(model, dumpFolder+uri+"-"+(counter++), "nt");
				}
				catch(Exception e){
					logger.error("ERROR as: "+ e.getMessage());
					missedURIs.add(workingURI);
					continue;
				}
			}
		WriteFile.writeToFile(missedURIs, urisFile);
		logger.info(counter+" out of "+ uris.size()+" are cached");
	
	}
	
	private static Model cacheURI(String uri, String repository) throws IOException
	{
		Model model = ModelFactory.createDefaultModel();
		String query = "SELECT * WHERE {<"+uri.trim()+"> ?p ?o}";
		QueryExecution qexec =null;
		QuerySolution soln=null;
		Resource resource=null;
		ResultSet results=null;
		if(repository.startsWith("http"))
		{
			qexec = QueryExecutionFactory.sparqlService(repository, query);
            results = qexec.execSelect(); 
            if(results.hasNext()) // there are data retrieved for this uri
            {
            	resource = model.createResource(uri);
            	while (results.hasNext()) {
            		soln = results.nextSolution();
            		if (soln.get("o").isLiteral())
            			resource.addProperty(ResourceFactory.createProperty(soln.get("p").toString()), soln.get("o").toString());
            		else
            			resource.addProperty(ResourceFactory.createProperty(soln.get("p").toString()), ResourceFactory.createResource(soln.get("o").toString()));
            	}
            }
            else
            	throw new IOException(uri + ": has no retrieved data");
		}
		else
		{
			model = ReadFile.readModel(repository);
			qexec = QueryExecutionFactory.create(query, model);
			results = qexec.execSelect(); 
	       	 if(results.hasNext()) // found in the dump
	       	 {
		       	 resource = model.createResource(uri);
	       		while (results.hasNext()) {
		             soln = results.nextSolution();
		             if (soln.get("o").isLiteral())
		                	resource.addProperty(ResourceFactory.createProperty(soln.get("p").toString()), soln.get("o").toString());
		                else
		                	resource.addProperty(ResourceFactory.createProperty(soln.get("p").toString()), ResourceFactory.createResource(soln.get("o").toString()));
		            } 
	       	 }
	            else
	            	throw new IOException(uri + ": has no retrieved data");
		}
		return model;
	}
	/**
	 * This method constructs the mini dataset from the dataset's dump file with the required sample size
	 * @param dump : the file where the whole dataset dump exists
	 * @param gold : the file contains the gold standard links. It contains only the uris of both sides with no information
	 * @param className: the class is used to select specific URIs from the whole datasets related to the gold standard types (in case dump diversity e.g. dbpedia)
	 * @param rightSide : specifies if the mini dump is creted for the right/left side uris in the gold
	 * @param sampleSize: the size of the mini dump reqquired
	 */
	public static Model createMiniDumpFromDumpForClass(List<String> uris, String className, String dump,int requiredSize)
	{
		List<String> missedURIs = new ArrayList<>();

		
		File dumpFile= new File(dump);
		// check if the dump file exists

		if(!dumpFile.exists())
		{
			logger.error("Error the dump file does not exist");
			return null;
		}
		
		logger.info("The gold standard abd dump files exist");


		
		Model modelDumpDataSet = ModelFactory.createDefaultModel(); // the model that holds the whole dump data (primarily filled)
		Model modelDumpMini = ModelFactory.createDefaultModel(); // the model that holds the mini dump data (to be filled)

		logger.info("Loading the dump file data");
		modelDumpDataSet = ReadFile.readModel(dump);
	 
		
		String queryString="";
		Query query =null;
		QueryExecution qexec =null;
		QuerySolution soln=null;
		Resource resource=null;
		
		//iterate over the Uris of the gold standard
		for (String uri : uris) {
			
			queryString = "SELECT * WHERE {<"+uri.trim()+"> ?p ?o .}";
			query =  QueryFactory.create(queryString);
	        qexec = QueryExecutionFactory.create(query, modelDumpDataSet);// get all uri data from dump

	        try{
	        	 ResultSet results = qexec.execSelect(); //query the dump data model for information regarding this goldstandard.URI
	        	 //add the goldstandard.URI data into the minidump model
	        	 resource = modelDumpMini.createResource(uri);
	        	 if(results.hasNext()) // found in the dump
	        	 {
	        		while (results.hasNext()) {
		             soln = results.nextSolution();
		             if (soln.get("o").isLiteral())
		                	resource.addProperty(ResourceFactory.createProperty(soln.get("p").toString()), soln.get("o").toString());
		                else
		                	resource.addProperty(ResourceFactory.createProperty(soln.get("p").toString()), ResourceFactory.createResource(soln.get("o").toString()));
		            } 
	        		requiredSize--; //update number of resources remained to fetch their data
	        		try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                	writeTheDump(modelDumpMini, "/media/mofeed/TOSHIBA2T/00-PhDProjects2018/TransferLearning/TransferLearningTmp/testDumping/dbpedia-sider-drug/x/", "sider"+requiredSize, className, requiredSize);
                	modelDumpMini.removeAll();
   					logger.info("Remaining "+requiredSize+" resource");

	        	 }
	        	 else{//not found
	        		 logger.warn("URI from gold is not found in the dump: "+ uri);
	        		 missedURIs.add(uri);
	        	 }
		         

	         } finally{
	        	 qexec.close();
	         }
		}

		//get all URIs of the specified type from the dump model and set an iterator over it
		ResIterator iter = modelDumpDataSet.listSubjectsWithProperty(ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), ResourceFactory.createResource(className));
		Resource dumpResource=null;
		//as long as there are URIs and not fullfil the sample size
		while (iter.hasNext() && requiredSize > 0) {
			dumpResource = iter.next(); // get a subject/resource from the dump list of resources
			if (!uris.contains( dumpResource.getURI().trim())) { //the resource from the dump is not contained in golddump model, so it is new add it!
				logger.info("This new URI will be added: "+dumpResource);
				
				//get the data of the new URI retrieved from the dump model from the dump model itself (rest of its info)
				queryString = "SELECT * WHERE {<"+dumpResource.getURI()+"> ?p ?o .}";
				query =  QueryFactory.create(queryString);
		        qexec = QueryExecutionFactory.create(query, modelDumpDataSet);// get all uri data from dump

		        try{
		        	 ResultSet results = qexec.execSelect();
		        	 resource = modelDumpMini.createResource(dumpResource.getURI()); // add the resource to the minidump model
			         while (results.hasNext()) { //add the uri info too
			             soln = results.nextSolution();
			             if (soln.get("o").isLiteral())
			                	resource.addProperty(ResourceFactory.createProperty(soln.get("p").toString()), soln.get("o").toString());
			                else
			                	resource.addProperty(ResourceFactory.createProperty(soln.get("p").toString()), ResourceFactory.createResource(soln.get("o").toString()));
			            }
			         requiredSize--; //update number of resources remained to fetch their data
			        writeTheDump(modelDumpMini, "/media/mofeed/TOSHIBA2T/00-PhDProjects2018/TransferLearning/TransferLearningTmp/testDumping/dbpedia-sider-drug/x/", "sider"+requiredSize, className, requiredSize);
	                modelDumpMini.removeAll();
   					logger.info("Remaining "+requiredSize+" resource");


		         } finally{
		        	 qexec.close();
		         }
				}
		}
		
		WriteFile.appendToFile(missedURIs, "missedURIsDump"+className+dump);
		
		return modelDumpMini;
	}



	/**
	 * This method constructs the mini dataset from the endpoint with the required sample size including the gold URIs
	 * @param uris : the gold standard uris
	 * @param path : the mini dump path
	 * @param datasetName : used to annotate the dump file with the name of the dataset it dumps
	 * @param className : the class of the URIs to dump
	 * @param requiredSize : the sample size to dump
	 * @param endpoint : the endpoint to dump from
	 */
	public static Model createMiniDumpFromEdnpointForClass(List<String> uris, String className,  String endpoint,int requiredSize)
    {
		List<String> missedURIs = new ArrayList<>();
		logger.info("The size of goldstabdard URIs to retrieve their data = "+ uris.size());
		
		String query="";
    	QueryExecution qexec;
    	ResultSet results;
    	Model model = ModelFactory.createDefaultModel();
    	Resource resource =null;
    	int count=1;
    	for (String uri : uris) {
    		try{
    			if(count > requiredSize)
        			break;
        		logger.info(count+": Retrieve data of URI: "+uri);
    			query = "SELECT * WHERE {<"+uri.trim()+"> ?p ?o}";
    			
                qexec = QueryExecutionFactory.sparqlService(endpoint, query);
                results = qexec.execSelect(); 
                QuerySolution soln;
                if(results.hasNext()) // there are data retrieved for this uri
                {
                	resource = model.createResource(uri);
                	while (results.hasNext()) {
                		soln = results.nextSolution();
                		if (soln.get("o").isLiteral())
                			resource.addProperty(ResourceFactory.createProperty(soln.get("p").toString()), soln.get("o").toString());
                		else
                			resource.addProperty(ResourceFactory.createProperty(soln.get("p").toString()), ResourceFactory.createResource(soln.get("o").toString()));
                	}
//                	writeTheDump(model, "/media/mofeed/TOSHIBA2T/00-PhDProjects2018/TransferLearning/TransferLearningTmp/testDumping/dbpedia-sider-drug/x/", "sider"+count, className, requiredSize);
//                    model.removeAll(); 
                    count++;
                }
                else
                {
                	logger.warn("URI is not found in the endpoint:"+uri);
                	missedURIs.add(uri);
                }
    		}
    		catch(Exception e)
    		{logger.error("ERROR WITH "+uri);  Thread.sleep(5000);}
    		finally
    		{continue;}
    	}
    	
    	List<String> restURIs = getRestOfUris(uris, className, endpoint, 2*requiredSize);//get the double in case of corrupted URI
		logger.info("The number of retrieved URIs  = "+ uris.size());
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


    	for (String uri : restURIs) {
    		try{
    			if(count > requiredSize)
        			break;
        		logger.info(count+": Retrieve data of URI: "+uri);
    			query = "SELECT * WHERE {<"+uri.trim()+"> ?p ?o}";
    			
                qexec = QueryExecutionFactory.sparqlService(endpoint, query);
                results = qexec.execSelect(); 
                QuerySolution soln;
                if(results.hasNext()) // there are data retrieved for this uri
                {
                	resource = model.createResource(uri);
                	while (results.hasNext()) {
                		soln = results.nextSolution();
                		if (soln.get("o").isLiteral())
                			resource.addProperty(ResourceFactory.createProperty(soln.get("p").toString()), soln.get("o").toString());
                		else
                			resource.addProperty(ResourceFactory.createProperty(soln.get("p").toString()), ResourceFactory.createResource(soln.get("o").toString()));
                	}
//                	writeTheDump(model, "/media/mofeed/TOSHIBA2T/00-PhDProjects2018/TransferLearning/TransferLearningTmp/testDumping/dbpedia-sider-drug/x/", "sider"+count, className, requiredSize);
//                    model.removeAll(); 
                    count++;
                }
                else
                {
                	logger.warn("URI is not found in the endpoint:"+uri);
                	missedURIs.add(uri);
                }
    		}
    		catch(Exception e)
    		{logger.error("ERROR WITH "+uri);  Thread.sleep(5000);}
    		finally
    		{continue;}
    	}
    	//write to a file the missed URIs.
    	WriteFile.appendToFile(missedURIs, "missedURIsEndpointdbpedia");//+className+endpoint);
    	return model;
    }
	
	/**
	 * get the rest of URIs additional to the ones exist in the gold standard to fullfil the required size
	 * @param uris
	 * @param className
	 * @param endpoint
	 * @param requiredSize
	 */
	private static List<String> getRestOfUris(List<String> uris,String className,String endpoint, int requiredSize)
	{
		String query="";
    	QueryExecution qexec;
    	ResultSet results;
    	long offset=0;
    	long limit= step;
    	List<String> restURIs = new ArrayList<>();
    	
    	while((uris.size()+restURIs.size()) < requiredSize)
    	{
    		/*if(endpoint.contains("dbpedia"))
    			query="SELECT distinct ?a WHERE { {?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Place>} UNION {?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Town>} } OFFSET "+offset+" LIMIT " + limit;
    		else*/ if(className.startsWith("http")) query = "SELECT distinct ?a "
                    + "WHERE { ?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + className + ">."
                    + "} OFFSET "+offset+" LIMIT " + limit;
            else query = "SELECT distinct ?a "
                    + "WHERE { ?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> " + className + ". "
                    + "} OFFSET "+offset+" LIMIT " + limit;
    		logger.info("The query is : "+query);
    		qexec = QueryExecutionFactory.sparqlService(endpoint, query);
            results = qexec.execSelect();
            QuerySolution soln;
            
            if(!results.hasNext())//online dataset ends
            	break;
            // as long as there are URIs and still did not fullfil the required size
            while (results.hasNext() && (uris.size()+restURIs.size()) < requiredSize) {
                soln = results.nextSolution();
                String uriValue = soln.get("a").toString();
                //boolean isAscii = CharMatcher.ASCII.matchesAllOf(soln.get("a").toString());
                boolean isAscii = isAscii(uriValue);
                boolean containsNumbers = uriValue.substring(uriValue.lastIndexOf("/")+1, uriValue.length()).matches(".*\\d+.*");
                boolean containsWhiteListWord = containsWhiteListWord(uriValue);

               // if(isAscii && !containsNumbers && !containsWhiteListWord && !uris.contains(soln.get("a").toString()))//check if it was included before - to avoid the gold URIs and skip it from counting
                if(!uris.contains(uriValue))
                {        		
                	logger.info("The new URI to add is : "+uriValue);
                	restURIs.add(uriValue.trim());
                }
            }
            offset+=limit;
    	}
    	return restURIs;
	}
	
	private static boolean isAscii(String string){
		byte[] stringBytes = string.getBytes();
		CharsetDecoder decoder = Charset.forName("US-ASCII").newDecoder();
		try {

			CharBuffer buffer = decoder.decode(ByteBuffer.wrap(stringBytes));
        } catch (CharacterCodingException e) {
            System.err.println(string+" contains a non ASCII character(s).");
            return false;
        }
		return true;
	}
	private static boolean containsWhiteListWord(String string){

		for (String word : whiteList) {
			if(string.toLowerCase().contains(word))
				return true;
		}
		return false;
	}
	
	public static void writeTheDump(Model model,String path, String datasetName, String className, int miniSize){
    	WriteFile.writeModel(model, path+"/mini"+datasetName+className.substring(className.lastIndexOf("/")+1, className.length())+miniSize, "nt");
	}
	/*	public static Map<Integer,List<String>> getDisjointDataSets(Configuration configuration, String goldStandard)
	{
//		String sourceId = configuration.getSource().id;
//        String targetId = configuration.getTarget().id;
//        String sourceClass = configuration.getSource().getClassOfendpoint();
//        String targetClass = configuration.getTarget().getClassOfendpoint();
//        List<String> sourceProoperties =  configuration.getSource().properties;
//        List<String> targetProoperties =  configuration.getTarget().properties;
        KBInfo source = configuration.source;
        KBInfo target = configuration.target;
        
        String cachedFileName = hashFileName(sourceId, targetId, sourceClass, targetClass, sourceProoperties, targetProoperties);
        
		Map<Integer,List<String>> disjointDataset = new HashMap<>();
		
		List<String> dsUris = SparqlUtils.getUris(className1, endpoint1, 0, sampleSize);
		Set<String> uriProperrites=null;
		Set<String> ds1AllPropertiesPool = new HashSet<>();
		for (String uri : dsUris) {
			uriProperrites = SparqlUtils.getUriPropertyValues(uri, comparisonProperty, endpoint1);
			ds1AllPropertiesPool.addAll(uriProperrites);
		}
		disjointDataset.put(1, dsUris);
		disjointDataset.put(2, new ArrayList<>());

		dsUris = SparqlUtils.getUris(className2, endpoint2, 0, 2*sampleSize);
		List<String> ds2Uris = new ArrayList<>();
		boolean found = false;
		for (String uri2 : dsUris) {
			found = false;
			uriProperrites = SparqlUtils.getUriPropertyValues(uri2, comparisonProperty, endpoint2);
			for (String uri2Propery : uriProperrites) {
				if(ds1AllPropertiesPool.contains(uri2Propery))
					found=true;
			}
			if(!found)
				disjointDataset.get(2).add(uri2);
		}
		return disjointDataset;		
	}*/
	
	private static final long step=1000;
	public static String hashFileName(String src, String tar, String srcClass, String tarClass, List<String> srcProps, List<String> tarProps) {
		  long result = 17;
		  result = 37*result + src.hashCode();
		  result = 37*result + tar.hashCode();
		  result = 37*result + srcClass.hashCode();
		  result = 37*result + tarClass.hashCode();
		  for (String v:srcProps) result = 37*result + v.hashCode();
		  for (String v:tarProps) result = 37*result + v.hashCode();

		  return String.valueOf(result);
		}
	public static void sparqlQueryToSerialize(List<String> queryLines, String endpoint)
	{
		//get variables names
		String vaiablesLine = queryLines.get(0);
		String[] variables = vaiablesLine.split("\\s");
    	String query ="";
    	for (String line : queryLines) {
    		query+=line;
		}
    	KBInfo kbi = new KBInfo();
 		Cache data =null;
    	String currentFolder = System.getProperty("user.dir");
    	File folder = new File(currentFolder);
    	List<String> parameters = new ArrayList<>();
    	parameters.add(query);
    	Cache cache = new HybridCache();
    	String hash = ReadFile.hashCode(parameters) + "";
    	File cacheFile = new File(currentFolder+"/" + "cache/" + hash + ".ser");
    	
    	logger.info("Checking for file " + cacheFile.getAbsolutePath());
    	
    	try {
    	    if (cacheFile.exists()) {
    		logger.info("Found cached data serialized in " + cacheFile.getAbsolutePath());
    		cache = HybridCache.loadFromFile(cacheFile);
    		logger.info("Size = " + cache.size());
    	    }
    	    if (cache.size() == 0) {
    		throw new Exception();
    	    } 
    	} // 2. If it does not work, then get it from data sourceInfo as
    	catch (Exception e) {
    		 logger.info("No cached data found for query" + query);
    		 Cache results = SparqlUtils.runSparqlQuery(query, endpoint, variables);
    		 if (!new File(folder.getAbsolutePath() + File.separatorChar + "cache").exists()
    	     		    || !new File(folder.getAbsolutePath() + File.separatorChar + "cache").isDirectory()) {
    	     		new File(folder.getAbsolutePath() + File.separatorChar + "cache").mkdir();
    	     	    }
    	     	    ReadFile.saveToFile(new File(folder.getAbsolutePath() + File.separatorChar + "cache/" + hash + ".ser"),(HybridCache) cache);
    	}
    	
    	
    	
	}
	public static long countResources()
	{
		String endpoint="http://us.patents.aksw.org/sparql";
    	QueryExecution qexec;
    	ResultSet results;
    	Model model = ModelFactory.createDefaultModel();
    	Resource resource =null;
    	int offset =0; 
    	long limit =10000;

    	while(true)
    	{
			String query="SELECT  DISTINCT ?x WHERE{ ?x <http://us.patents.aksw.org/property/city> ?city .} OFFSET "+offset+" LIMIT "+limit;
	        qexec = QueryExecutionFactory.sparqlService(endpoint, query);
	        results = qexec.execSelect(); 
	        QuerySolution soln;
	        System.out.println("Retrieved URIs from endpoint = "+results.getRowNumber());
	        
	        if(results.hasNext()) // there are data retrieved for this uri
	                {
	                	while (results.hasNext()) {
	                		soln =  results.nextSolution();
	                		resource = model.createResource(soln.get("x").toString());
	                		resource.addProperty(ResourceFactory.createProperty("http://us.patents.aksw.org/property/city"), "N");
	                		}
	                	offset+=limit;
	                }
	        else
	        	break; //no more data
	        System.out.println("Till now number of retrieved resources = "+ model.size());
    	}
    	return model.size();
	}
}
