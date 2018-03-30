/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.saim.transfer.classes;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Literal;
import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.HybridCache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.query.QueryModule;
import de.uni_leipzig.simba.query.QueryModuleFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aksw.saim.io.ReadFile;
import org.aksw.saim.io.ReadFile.SimilarityType;
import org.aksw.saim.transfer.config.Configuration;
import org.aksw.saim.util.Execution;
import org.apache.log4j.Logger;

/**
 *
 * @author ngonga
 */
public class LabelBasedClassSimilarity implements ClassSimilarity{
    
    static Logger logger = Logger.getLogger("LIMES");
    public int SAMPLING_RATE = 100;
    public double THRESHOLD = 0.25;
    public static Map<String, String> prefixes = new HashMap<String, String>();

    private static Cache getLabels(String c, String endpoint) {
        Cache cache = new HybridCache();
        String query = "SELECT ?l "
                + "WHERE { <"+c+"> <http://www.w3.org/2000/01/rdf-schema#label> ?l. }";
        Query sparqlQuery = QueryFactory.create(query);

        QueryExecution qexec;
        qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery);
        ResultSet results = qexec.execSelect();

        String x, a, b;
        QuerySolution soln;
        while (results.hasNext()) {
            soln = results.nextSolution();
            if (soln.get("l").isLiteral()) {
                cache.addTriple(c, "p", ((Literal) soln.get("l")).getLexicalForm());
            }
        }
        
        return cache;
    }
    
    private static Cache getLabels2(String c, String endpoint) {
    	List<String> parameters = new ArrayList<>();
    	parameters.add(c);
    	parameters.add(endpoint);
    	return ReadFile.loadClassData(SimilarityType.LABEL, parameters);
    }
    

    @Override
    public double getSimilarity(String class1, String class2, Configuration config2) {
    	if (class1 == null || class2 == null) {
            System.err.println("One of " + class1 + " and " + class2 + " is " + null);
            return 0D;
        }
    	if(class1.contains(":"))
    		class1=LabelBasedClassSimilarity.expand(class1,config2);
    	if(class2.contains(":"))
    		class2=LabelBasedClassSimilarity.expand(class2,config2);
    	
        Cache target = getLabels2(class1, config2.target.endpoint);//c1
    	Cache source = getLabels2(class2, config2.source.endpoint);//c2
    	
        Mapping m = Execution.execute(source, target, "trigrams", THRESHOLD);
        System.out.println(source+"\n"+target+"\n"+m);
        double max = 0.0, sim = 0.0;
        //we could use max, min instead
        //could also
        for (String s : m.map.keySet()) {
            for (String t : m.map.get(s).keySet()) {
                sim = m.getSimilarity(s, t);
                if(sim > max)
                    max = sim;
            }
        }
        return max;        
    }
    
	@Override
	public double getSimilarity(String class2, String class1, Configuration config1, Configuration config2,boolean isSource) {
		if (class1 == null || class2 == null) {
            System.err.println("One of " + class1 + " and " + class2 + " is " + null);
            return 0D;
        }
    	if(class1.contains(":"))
    		class1=LabelBasedClassSimilarity.expand(class1,config1);
    	if(class2.contains(":"))
    		class2=LabelBasedClassSimilarity.expand(class2,config2);
    	
    	Cache source=null;
    	Cache target=null;
    	if(isSource)
    	{
    		source = getLabels2(class1, config1.source.endpoint);//c1
    		target = getLabels2(class2, config2.source.endpoint);//c2
    	}
    	else
    	{
    		source = getLabels2(class1, config1.target.endpoint);//c1
    		target = getLabels2(class2, config2.target.endpoint);//c2
    	}
    	double max = 0.0;
    	if(source != null && target != null)
    	{
    		 Mapping m = Execution.execute(source, target, "trigrams", THRESHOLD);
    	        System.out.println(source+"\n"+target+"\n"+m);
    	        double sim = 0.0;
    	        //we could use max, min instead
    	        //could also
    	        for (String s : m.map.keySet()) {
    	            for (String t : m.map.get(s).keySet()) {
    	                sim = m.getSimilarity(s, t);
    	                if(sim > max)
    	                    max = sim;
    	            }
    	        }
    	}
       
        return max;        
	}
	
	public double getSimilarity2(String class1, String class2, Configuration config1, Configuration config2,boolean isSource) {
		if (class1 == null || class2 == null) {
            System.err.println("One of " + class1 + " and " + class2 + " is " + null);
            return 0D;
        }
    	if(class1.contains(":"))
    		class1=LabelBasedClassSimilarity.expand(class1,config1);
    	if(class2.contains(":"))
    		class2=LabelBasedClassSimilarity.expand(class2,config2);
    	
    	Cache source=null;
    	Cache target=null;
    	if(isSource)
    	{
    		source = getLabels(class1, config1.source.endpoint);//c1
    		target = getLabels(class2, config2.source.endpoint);//c2
    	}
    	else
    	{
    		source = getLabels(class1, config1.target.endpoint);//c1
    		target = getLabels(class2, config2.target.endpoint);//c2
    	}
    	double max = 0.0;
    	if(source != null && target != null)
    	{
    		 Mapping m = Execution.execute(source, target, "trigrams", THRESHOLD);
    	        System.out.println(source+"\n"+target+"\n"+m);
    	        double sim = 0.0;
    	        //we could use max, min instead
    	        //could also
    	        for (String s : m.map.keySet()) {
    	            for (String t : m.map.get(s).keySet()) {
    	                sim = m.getSimilarity(s, t);
    	                if(sim > max)
    	                    max = sim;
    	            }
    	        }
    	}
       
        return max;        
	}
	
    public static String expand(String property, Configuration config) {
        if(property.startsWith("http")) return property;
        String prefix = property.substring(0, property.indexOf(":"));
        String name = property.substring(property.indexOf(":") + 1);
        if(prefixes.containsKey(prefix)) return prefixes.get(prefix)+name;
        if(config.source.prefixes.containsKey(prefix)) return config.source.prefixes.get(prefix)+name;
        if(config.target.prefixes.containsKey(prefix)) return config.target.prefixes.get(prefix)+name;
        if(prefix.equals("dbpedia-owl")) return "http://dbpedia.org/ontology/"+name;
        if(prefix.equals("lgdo")) return "http://linkedgeodata.org/ontology/"+name;
        if(prefix.equals("yago")) return "http://dbpedia.org/class/yago/"+name;
        if(prefix.equals("BibTex")) return "http://data.bibbase.org/ontology/#";
        if(prefix.equals("administrative-geography")) return "http://statistics.data.gov.uk/def/administrative-geography/";
        else System.err.println("Prefix "+prefix+" not found for property "+property);
        System.exit(1);
        return null;
    }
    public static void main(String[] args)
    {
        System.out.println(getLabels("http://dbpedia.org/resource/Leipzig", "http://live.dbpedia.org/sparql"));
    }


}
