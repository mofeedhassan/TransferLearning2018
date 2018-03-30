package org.aksw.saim.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.aksw.saim.transfer.config.ConfigReader;
import org.aksw.saim.transfer.config.Configuration;
import org.apache.log4j.Logger;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.HybridCache;



public class ReadFile {
	static Logger logger = Logger.getLogger("LIMES");
	
	public static List<String> readFile(String file)
	{
		Scanner in=null;
		List<String> datasets = new ArrayList<>();

		
		try {
			in = new Scanner(new File(file));
			while(in.hasNextLine()) {
				datasets.add(in.nextLine());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		finally{
			in.close();
		}
		return datasets;		
	}
	
	public static List<String> readLinksURIs(String file, boolean isRightSide)
	{
		Scanner in=null;
		List<String> datasets = new ArrayList();
		
		try {
			in = new Scanner(new File(file));
			while(in.hasNextLine()) {
				String[] data = in.nextLine().split("\\s");
				if(isRightSide)
					datasets.add(data[0].trim());
				else
					datasets.add(data[2].trim());			
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		finally{
			in.close();
		}
		return datasets;		
	}
	
	public static Map<Integer,List<String>> readLinksFile(String file)
	{
		Scanner in=null;
		Map<Integer,List<String>> datasets = new HashMap<>();
		datasets.put(1, new ArrayList<>());
		datasets.put(2, new ArrayList<>());
		
		try {
			in = new Scanner(new File(file));
			while(in.hasNextLine()) {
				String[] data = in.nextLine().split("\\s");
				data[0].trim();
				data[0]=data[0].replaceAll("<", "");
				data[0]=data[0].replaceAll(">", "");
				datasets.get(1).add(data[0].trim());
				
				data[2].trim();
				if(data[2].endsWith("."))
					data[2]=data[2].substring(0, data[2].lastIndexOf("."));
				data[2]=data[2].replaceAll("<", "");
				data[2]=data[2].replaceAll(">", "");
				datasets.get(2).add(data[2].trim());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		finally{
			in.close();
		}
		return datasets;		
	}
	
	public static Configuration readLimesConfig(String limesconfig)
	{
		Configuration configuration = new ConfigReader().readLimesConfig(limesconfig);
		return configuration;
	}
	
	   /**
     * read RDF model from file/URL
     *
     * @param fileNameOrUri file name or URI to be read
     * @return Model that contains the data in the fileNameOrUri
     */
    public static com.hp.hpl.jena.rdf.model.Model readModel(String fileNameOrUri) {
        long startTime = System.currentTimeMillis();
        com.hp.hpl.jena.rdf.model.Model model = com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel();

        try (InputStream in = com.hp.hpl.jena.util.FileManager.get().open(fileNameOrUri)) {
            if (fileNameOrUri.contains(".ttl") || fileNameOrUri.contains(".n3")) {
            	logger.info("Opening Turtle file");
                model.read(in, null, "TTL");
            } else if (fileNameOrUri.contains(".rdf")) {
            	logger.info("Opening RDFXML file");
                model.read(in, null);
            } else if (fileNameOrUri.contains(".nt")) {
            	logger.info("Opening N-Triples file");
                model.read(in, null, "N-TRIPLE");
            } else {
            	logger.info("Content negotiation to get RDFXML from " + fileNameOrUri);
                model.read(fileNameOrUri);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Loading " + fileNameOrUri + " is done in " + (System.currentTimeMillis() - startTime) + "ms.");
        return model;
    }

    public static File parentFolder=null;
    public enum SimilarityType {LABEL,SAMPLE}
    
    /**
     * given a set of parameters include class, endpoint and samples size the method load
     * the class data from a cached file or initiate a query using the given parameters where the sparql query is
     * formulated based on the similarity type
     * @param simType
     * @param parameters
     * @return
     */
    public static Cache loadClassData(SimilarityType simType, List<String> parameters)
    {
    	Cache data =null;
    	String currentFolder = System.getProperty("user.dir");
    	File folder = new File(currentFolder);
    	parentFolder = folder;
    	Cache cache = new HybridCache(folder);
    	String hash = hashCode(parameters) + "";
    	File cacheFile = new File(currentFolder+"/" + "cache/" + hash + ".ser");
    	
    	logger.info("Checking for file " + cacheFile.getAbsolutePath());
    	
    	try {
    	    if (cacheFile.exists()) {
    		logger.info("Found cached data. Loading data from file " + cacheFile.getAbsolutePath());
    		cache = HybridCache.loadFromFile(cacheFile);
    	    }
    	    if (cache.size() == 0) {
    		throw new Exception();
    	    } else {
    		logger.info("Cached data loaded successfully from file " + cacheFile.getAbsolutePath());
    		logger.info("Size = " + cache.size());
    	    }
    	} // 2. If it does not work, then get it from data sourceInfo as
    	catch (Exception e) {
    		 logger.info("No cached data found for labels of class" + parameters.get(0));
    		 if(simType.equals(SimilarityType.LABEL))
    			 cache = getClassLabelData(parameters.get(0), parameters.get(1),hash);
    		 else if(simType.equals(SimilarityType.SAMPLE))
    			 cache = getSampleData(parameters.get(0), parameters.get(1),parameters.get(2),hash);
    	}
    	return cache;
    }
    
    private static Cache getClassLabelData(String clas,String endpoint,String hash) {
    		Cache cache = new HybridCache();
    	    String query = "SELECT ?l "
                    	+ "WHERE { <"+clas+"> <http://www.w3.org/2000/01/rdf-schema#label> ?l. }";
            Query sparqlQuery = QueryFactory.create(query);

            QueryExecution qexec;
            if(endpoint.startsWith("http"))
            	qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery);
            else{
            	Model m = ReadFile.readModel(endpoint.substring(0, endpoint.lastIndexOf("/"))+"class");
            	qexec = QueryExecutionFactory.create(query, m);
            }
            qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery);
            ResultSet results = qexec.execSelect();

            String x, a, b;
            QuerySolution soln;
            while (results.hasNext()) {
                soln = results.nextSolution();
                if (soln.get("l").isLiteral()) {
                    cache.addTriple(clas, "p", ((Literal) soln.get("l")).getLexicalForm());
                }
            }

    	    if (!new File(parentFolder.getAbsolutePath() + File.separatorChar + "cache").exists()
    		    || !new File(parentFolder.getAbsolutePath() + File.separatorChar + "cache").isDirectory()) {
    		new File(parentFolder.getAbsolutePath() + File.separatorChar + "cache").mkdir();
    	    }
    	    saveToFile(new File(parentFolder.getAbsolutePath() + File.separatorChar + "cache/" + hash + ".ser"),(HybridCache) cache);
    	    return cache;
    	}
    private static Cache getSampleData(String clas,String endpoint,String size, String hash)
    {
    	Cache cache = new HybridCache();
        String query;
        if(clas.startsWith("http")) query = "SELECT ?x ?a "
                + "WHERE { ?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + clas + ">. "
                + "?a ?p ?x . filter isLiteral(?x) . FILTER (lang(?x) = 'en')} LIMIT " + size;
        else query = "SELECT ?x ?a "
                + "WHERE { ?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> " + clas + ". "
                + "?a ?p ?x . filter isLiteral(?x) . FILTER (lang(?x) = 'en')} LIMIT " + size;
        Query sparqlQuery = QueryFactory.create(query);

        QueryExecution qexec=null;
        Model model;
        if(endpoint.startsWith("http"))
        	qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery);
        else
        {
        	model = ReadFile.readModel(endpoint);
			qexec = QueryExecutionFactory.create(query, model);
        }
        ResultSet results = qexec.execSelect();
        String x, a, b;
        QuerySolution soln;
        while (results.hasNext()) {
            soln = results.nextSolution();
            if (soln.get("x").isLiteral()) {
                cache.addTriple(soln.get("a").toString(), "p", ((Literal) soln.get("x")).getLexicalForm());
            }
        }
        
	    if (!new File(parentFolder.getAbsolutePath() + File.separatorChar + "cache").exists()
    		    || !new File(parentFolder.getAbsolutePath() + File.separatorChar + "cache").isDirectory()) {
    		new File(parentFolder.getAbsolutePath() + File.separatorChar + "cache").mkdir();
    	    }
    	    saveToFile(new File(parentFolder.getAbsolutePath() + File.separatorChar + "cache/" + hash + ".ser"),(HybridCache) cache);
    	    
        return cache;
    }

    ////////////////////////////////////////////////////////////////////////////
    /**
     *given a set of parameters include class, property, endpoint and samples size the method load
     * the property data of certain class from a cached file or initiate a query using the given parameters where the sparql query is
     * formulated based on the similarity type
     * @param simType
     * @param parameters
     * @return
     */
    public static Cache loadPropertyData(SimilarityType simType, List<String> parameters)
    {
    	Cache data =null;
    	String currentFolder = System.getProperty("user.dir");
    	File folder = new File(currentFolder);
    	parentFolder = folder;
    	Cache cache = new HybridCache(folder);
    	String hash = hashCode(parameters) + "";
    	File cacheFile = new File(currentFolder+"/" + "cache/" + hash + ".ser");
    	
    	logger.info("Checking for file " + cacheFile.getAbsolutePath());
    	
    	try {
    	    if (cacheFile.exists()) {
    		logger.info("Found cached data. Loading data from file " + cacheFile.getAbsolutePath());
    		cache = HybridCache.loadFromFile(cacheFile);
    	    }
    	    if (cache.size() == 0) {
    		throw new Exception();
    	    } else {
    		logger.info("Cached data loaded successfully from file " + cacheFile.getAbsolutePath());
    		logger.info("Size = " + cache.size());
    	    }
    	} // 2. If it does not work, then get it from data sourceInfo as
    	catch (Exception e) {
    		 logger.info("No cached data found for labels of class" + parameters.get(0));
   			 cache = getPropertySampleData(parameters.get(0), parameters.get(1),parameters.get(2),parameters.get(3),hash);
    	}
    	return cache;
    }
    
    private static int countOccurences(String string, String substring)
    {
    	int lastIndex = 0;
    	int count = 0;

    	while(lastIndex != -1){

    	    lastIndex = string.indexOf(substring,lastIndex);

    	    if(lastIndex != -1){
    	        count ++;
    	        lastIndex += substring.length();
    	    }
    	}
    	return count;
    }
    
    private static Cache getPropertySampleData(String clas,String property ,String endpoint,String size, String hash)
    {
      
    	Cache cache = new HybridCache();
        String query;
        String propertiesPart="";
        int occurences = countOccurences(property, "http");
        
        if(clas.startsWith("http"))
        	if(occurences>1)
        	{
        		String property1 = property.substring(0, property.lastIndexOf("http")-1);
        		String property2 = property.substring(property.lastIndexOf("http"),property.length());
        		query = "SELECT ?a ?x "
                        + "WHERE { ?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + clas + ">. "
                        + "?a <" + property1 + "> ?v1 . "
                        + "?v1 <" + property2 + "> ?x .} LIMIT " + size;
        	}
        	else
        	query = "SELECT ?a ?x "
                + "WHERE { ?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + clas + ">. "
                + "?a <" + property + "> ?x .} LIMIT " + size;
        else
        	if(occurences>1)
        	{
        		String property1 = property.substring(0, property.lastIndexOf("http"));
        		String property2 = property.substring(property.lastIndexOf("http")+1,property.length());
        		query = "SELECT ?a ?x "
                        + "WHERE { ?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> " + clas + ". "
                        + "?a <" + property1 + "> ?v1 . "
                        + "?v1 <" + property2 + "> ?x .} LIMIT " + size;
        	}
        	else
	        	query = "SELECT ?a ?x "
	                + "WHERE { ?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> " + clas + ". "
	                + "?a <" + property + "> ?x .} LIMIT " + size;
        Query sparqlQuery = QueryFactory.create(query);

        QueryExecution qexec=null;
        Model model;
        if(endpoint.startsWith("http"))
        	qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery);
        else
        {
        	model = ReadFile.readModel(endpoint);
			qexec = QueryExecutionFactory.create(query, model);
        }
        ResultSet results = qexec.execSelect();
        String x, a, b;
        QuerySolution soln;
        while (results.hasNext()) {
            soln = results.nextSolution();
            if (soln.get("x").isLiteral()) {
                cache.addTriple(soln.get("a").toString(), "p", ((Literal) soln.get("x")).getLexicalForm());
            }
        }
        
	    if (!new File(parentFolder.getAbsolutePath() + File.separatorChar + "cache").exists()
    		    || !new File(parentFolder.getAbsolutePath() + File.separatorChar + "cache").isDirectory()) {
    		new File(parentFolder.getAbsolutePath() + File.separatorChar + "cache").mkdir();
    	    }
    	    saveToFile(new File(parentFolder.getAbsolutePath() + File.separatorChar + "cache/" + hash + ".ser"),(HybridCache) cache);
    	    
        return cache;
    }
    /////////////////////////////////////////////////////////////////
    
    ////////////////////////////////////////////////////////////////
	public static void saveToFile(File file, HybridCache cache) {
		FileOutputStream out;
		Logger logger = Logger.getLogger("LIMES");
		logger.info("Serializing " + cache.size() + " objects to " + file.getAbsolutePath());
	
		try {
		    out = new FileOutputStream(file);
		    ObjectOutputStream serializer = new ObjectOutputStream(out);
		    serializer.writeObject(cache);
		    out.close();
		} catch (Exception e) {
		    e.printStackTrace();
		    file.delete();
		}
	    }
    
    public static int hashCode(List<String> parameters) {
        final int prime = 31;
        int result = 1;
        for (String parameter : parameters) {
        	result = prime * result + ((parameter == null) ? 0 : parameter.hashCode());
		}
        return result;
    }

	


}
