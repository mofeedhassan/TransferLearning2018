/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.saim.util;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;

import LBJ2.learn.Log;
import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.HybridCache;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aksw.saim.io.ReadFile;
import org.aksw.saim.transfer.Eval;
import org.aksw.saim.transfer.TransferLearner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ngonga
 */
public class SparqlUtils {

    static Logger logger = LoggerFactory.getLogger(SparqlUtils.class);

    public static Set<String> getRelevantProperties(String endpoint, String className, int instanceSampleSize, double minCoverage) {
        try {


            Set<String> result = new HashSet<String>();
            Set<String> instances = new HashSet<String>();

            //get sample of instances
            String query = "SELECT DISTINCT ?a "
                    + "WHERE { ?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + className + ">. "
                    + "} LIMIT " + instanceSampleSize;
            Query sparqlQuery = QueryFactory.create(query);
            QueryExecution qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery);
            ResultSet results = qexec.execSelect();
            QuerySolution soln;
            while (results.hasNext()) {
                soln = results.nextSolution();
                instances.add(soln.get("a").toString());
            }
            String property;
            Map<String, Integer> count = new HashMap<String, Integer>();
            for (String instance : instances) {
                //for each instance, get property values it possesses
                query = "SELECT DISTINCT ?p WHERE { <" + instance + "> ?p ?x. }";
                sparqlQuery = QueryFactory.create(query);
                qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery);
                results = qexec.execSelect();
                while (results.hasNext()) {
                    soln = results.nextSolution();
                    property = soln.get("p").toString();
                    if (!count.containsKey(property)) {
                        count.put(property, 1);
                    } else {
                        count.put(property, count.get(property) + 1);
                    }
                }
            }

            // now only take those above the coverage threshold
            // coverage is a fraction
            //use size just in case we do not have enough instances
            if (minCoverage < 1) {
                minCoverage = minCoverage * instances.size();
            }

            for (String p : count.keySet()) {
                if (count.get(p) >= minCoverage) {
                    result.add(p);
                }
            }

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Class = " + className);
        }
        return new HashSet<String>();
    }
    
    public static String formulateQuery(String uri, String graph, String className, String property, int offset, int limit)
    {
    	StringBuilder query = new StringBuilder("SELECT *");
    	String subject="";
    	if(uri.length() != 0)
    		subject="<"+uri+">";
    	else
    		subject="?a";
    	if(graph.length()!=0)
    		query.append(" FROM <"+graph+">");
    	query.append(" WHERE {");
    	if(uri.length() != 0)
   		    query.append(" "+subject+" ?p ?o. ");
    	if(className.length()!=0)
    		query.append(" "+subject+" <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + className + ">. ");
    	if(property.length()!=0)
        		query.append(" "+subject+" <"+property+"> ?o. ");

    	query.append("}");
    	if(offset != -1)
    		query.append(" OFFSET " + offset);
    	if(offset != -1)
    		query.append(" LIMIT " + limit);
    	return query.toString();
    }
    
    //give a uri and return all its properties in a model
    public static Model getAllDataOfUri(String uri, String endpoint) {
    	
    	    Model model =ModelFactory.createDefaultModel();

        try {        	
            //get sample of instances
            String query = formulateQuery(uri.trim(), "", "", "", -1, -1);//trim to stop query parsing problems
            logger.info("The query = "+ query);
            Query sparqlQuery = QueryFactory.create(query);
            QueryExecution qexec = null;
            
            if(endpoint.startsWith("http"))
         	   qexec =  QueryExecutionFactory.sparqlService(endpoint, sparqlQuery);
            else
            {
         	   Model modelDumpFile = ReadFile.readModel(endpoint); // read from the dump file into this model that represents repository
         	   qexec = QueryExecutionFactory.create(query, modelDumpFile);// apply the query to this dump
            }
            
            logger.info("Get data of the URI = "+ uri);
            ResultSet results = qexec.execSelect();
            Resource resource = model.createResource(uri); 
            Property property = null; 
            QuerySolution soln;
            while (results.hasNext()) {
                soln = results.nextSolution();
                //System.out.println(soln.get("?p"));
                property = model.createProperty(soln.get("?p").toString());
                //System.out.println(soln.get("?o"));
                resource.addProperty(property, soln.get("?o"));
            }
            
        } catch (Exception e) {
        	logger.error("ERROR using URI = "+ uri);
            e.printStackTrace();
            System.err.println("Problem with uri = "+uri);
        }
        return model;
    }
    
    public static Map<String, Map<String,String>> getAllDatasetData(List<String> uris, String endpoint) {
    	
	   Map<String, Map<String,String>> data = new HashMap<>();
	   int count=1;
    try {
	    	for (String uri : uris) {
	    		//get sample of instances
	           // String query = formulateQuery(uri, "", "", "", -1, -1); //SELECT * WHERE { <http://dbpedia.org/resource/Atropine> ?p ?o. }
	          
	    		String query="SELECT *\n"
	    				    +"WHERE { <"+uri.trim()+"> ?p ?o .}";
	            Query sparqlQuery = QueryFactory.create(query);
	            QueryExecution qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery);
	            ResultSet results = qexec.execSelect();
	            QuerySolution soln;
	            while (results.hasNext()) {
	                soln = results.nextSolution();
	                if(null != data && null == data.get(uri))//first time
	                	data.put(uri, new HashMap<>());
	                data.get(uri).put(soln.get("?p").toString(), soln.get("?o").toString());
			}
	        System.out.println("Retrieved uri Nr. "+count);
	        count++;
        }
        
    } catch (Exception e) {
        e.printStackTrace();
        System.err.println("Problem with uri = "+count);
    }
    return data;
}
 /**
  * The method retrieves all the object values of a property for a certain URI
  * i.e. Given URI and one of its PROPERTIES and get its OBJECT VALUES
  * @param uri
  * @param proeprty
  * @param endpoint
  * @return
  */
public static Set<String> getUriPropertyValues(String uri, String proeprty, String endpoint) {
    	
	    Set<String> propertyValues = new HashSet<>();

    try {        	
        //get sample of instances
        String query = formulateQuery(uri, "", "", proeprty, -1, -1);
      
        Query sparqlQuery = QueryFactory.create(query);
        QueryExecution qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery);
        ResultSet results = qexec.execSelect();
 
        QuerySolution soln;
        while (results.hasNext()) {
            soln = results.nextSolution();
            propertyValues.add(soln.get("?o").toString());
        }
        
    } catch (Exception e) {
        e.printStackTrace();
        System.err.println("Problem with uri = "+uri);
    }
    return propertyValues;
} 

//get the values of a propert related to a specific uri
/**
 * The method retrieves all the URIs that belong to the given CLASS
 * i.e. Given CLASS get URIs belong to this class
 * @param className : the type of the resources to retireve their URIs
 * @param endpoint
 * @param offset
 * @param limit
 * @return
 */
public static List<String> getClassUris(String className,String endpoint, int offset, int limit) {
   	
	    List<String> uris = new ArrayList<>();

   try {        	
       String query = formulateQuery("", "", className, "", offset, limit);
	   logger.info("Query =  "+query);

       Query sparqlQuery = QueryFactory.create(query);
       QueryExecution qexec = null;
       
       if(endpoint.startsWith("http"))
    	   qexec =  QueryExecutionFactory.sparqlService(endpoint, sparqlQuery);
       else
       {
    	   Model modelDumpFile = ReadFile.readModel(endpoint); // read from the dump file into this model that represents repository
    	   qexec = QueryExecutionFactory.create(query, modelDumpFile);// apply the query to this dump
       }

       ResultSet results = qexec.execSelect();
       
       logger.info("Get query results...");
       QuerySolution soln;
       while (results.hasNext()) {
           soln = results.nextSolution();
           uris.add(soln.get("?a").toString());
       }
       logger.info("Number of retrieved URIs = "+uris.size());
   } catch (Exception e) {
	   logger.info("ERROR while retrieving URIs from class = "+className);
       e.printStackTrace();
       System.err.println("Problem with uri = "+className);
   }
   return uris;
} 

 

/**
 * The methods retrieve all the ONLY the PROPERTIES belongs the the URIs of the specified CLASS
 * i.e. Given CLASS get all PROPERTIES related to this class
 * @param endpoint
 * @param className
 * @return
 */
    public static Set<String> getAllProperties(String endpoint, String className) {
        Set<String> result = new HashSet<String>();
        String query = "SELECT DISTINCT ?p "
                + "WHERE { ?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + className + ">. "
                + "?a ?p ?x .}";
        Query sparqlQuery = QueryFactory.create(query);

        QueryExecution qexec;
        qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery);
        ResultSet results = qexec.execSelect();

        String x, a, b;
        QuerySolution soln;
        while (results.hasNext()) {
            soln = results.nextSolution();
            result.add(soln.get("p").toString());
        }
        return result;
    }
    
    public static Cache runSparqlQuery(String query,String endpoint, String[] variables)
    {
      
    	HybridCache cache = new HybridCache(); 
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
            for(int i=0; i<variables.length;i++){
            	if(variables[i].startsWith("?"))
            	{
            		String variable = variables[i].substring(variables[i].lastIndexOf("?")+1, variables[i].length());
            		if (soln.get(variable).isLiteral()) {
                        cache.addTriple(soln.get("a").toString(), "p", ((Literal) soln.get(variable)).getLexicalForm());
                    }
                    else
                    {
                    	cache.addTriple(soln.get("a").toString(), "p", soln.get(variable).toString());
                    }
            	}
            	
            }
            
        }
        return cache;
    }


    /**
     * The endpoint is alive if we can get a single triple from it.
     *
     * @param endpoint
     * @param graph
     * @return
     */
    public static boolean isAlive(String endpoint, String graph) {
        try {
            String query = "SELECT * {?s ?p ?o } LIMIT 1";
            Query sparqlQuery = QueryFactory.create(query);

            QueryExecution qexec;
            if (graph != null) {
                List<String> defaultGraphs = new LinkedList<String>();

                defaultGraphs.add(graph);
                qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery, defaultGraphs, null);
            } else {
                qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery);
            }
            ResultSet results = qexec.execSelect();
            results.hasNext();
        } catch (Exception e) {
            LoggerFactory.getLogger(SparqlUtils.class).debug(e.getMessage());
            logger.error("Error as "+e.getMessage());
            return false;
        }
        return true;
    }
}
