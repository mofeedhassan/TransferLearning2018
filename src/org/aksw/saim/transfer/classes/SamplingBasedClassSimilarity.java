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

import java.util.ArrayList;
import java.util.List;

import org.aksw.saim.io.ReadFile;
import org.aksw.saim.io.ReadFile.SimilarityType;
import org.aksw.saim.transfer.config.Configuration;
import org.aksw.saim.transfer.properties.SamplingBasedPropertySimilarity;
import org.aksw.saim.util.Execution;

/**
 *
 * @author ngonga
 */
public class SamplingBasedClassSimilarity implements ClassSimilarity {

    public int SAMPLING_RATE = 200;
    public double THRESHOLD = 0.5;

    private static Cache getPropertyValues(String c, String endpoint, int size) {
        Cache cache = new HybridCache();
        String query;
        if(c.startsWith("http")) query = "SELECT ?x ?a "
                + "WHERE { ?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + c + ">. "
                + "?a ?p ?x .} LIMIT " + size;
        else query = "SELECT ?x ?a "
                + "WHERE { ?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> " + c + ". "
                + "?a ?p ?x .} LIMIT " + size;
        Query sparqlQuery = QueryFactory.create(query);

        QueryExecution qexec;
        System.out.println(endpoint);
        qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery);
        ResultSet results = qexec.execSelect();

        String x, a, b;
        QuerySolution soln;
        while (results.hasNext()) {
            soln = results.nextSolution();
            if (soln.get("x").isLiteral()) {
                cache.addTriple(soln.get("a").toString(), "p", ((Literal) soln.get("x")).getLexicalForm());
            }
        }
        return cache;
    }
    
    private static Cache getPropertyValues2(String c, String endpoint, int size) {
    	List<String> parameters = new ArrayList<>();
    	parameters.add(c);
    	parameters.add(endpoint);
    	parameters.add(String.valueOf(size));
    	return ReadFile.loadClassData(SimilarityType.SAMPLE, parameters);
    }
    

    @Override
    public double getSimilarity(String class1, String class2, Configuration config2) {
        String c1 = SamplingBasedPropertySimilarity.expand(class1, config2);
        String c2 = SamplingBasedPropertySimilarity.expand(class2, config2);
       
        Cache target = getPropertyValues2(c1, config2.target.endpoint, SAMPLING_RATE);//c1
        Cache source = getPropertyValues2(c2, config2.source.endpoint, SAMPLING_RATE);//c2
        
        Mapping m = Execution.execute(source, target, "trigrams", THRESHOLD);
        //System.out.println(source+"\n"+target+"\n"+m);
        double counter = 0.0;
        double total = 0.0;
        //we could use max, min instead
        //could also
        for (String s : m.map.keySet()) {
            for (String t : m.map.get(s).keySet()) {
                counter++;
                total = total + m.getSimilarity(s, t);
            }
        }
        if (counter == 0) {
            return counter;
        }
        return total/(Math.min((double)source.getAllUris().size(),(double)target.getAllUris().size()));
    }
    
	@Override
	public double getSimilarity(String class2, String class1, Configuration config1, Configuration config2,boolean isSource) {
		
		String c1 = SamplingBasedPropertySimilarity.expand(class1, config1);
        String c2 = SamplingBasedPropertySimilarity.expand(class2, config2);
        
        Cache source=null;
    	Cache target=null;
    	
       if(isSource)
       {
    	   source = getPropertyValues2(c1, config1.source.endpoint, SAMPLING_RATE);//c1
    	   target = getPropertyValues2(c2, config2.source.endpoint, SAMPLING_RATE);//c2
       }
       else
       {
    	   source = getPropertyValues2(c1, config1.target.endpoint, SAMPLING_RATE);//c1
    	   target = getPropertyValues2(c2, config2.target.endpoint, SAMPLING_RATE);//c2
       }
        
       double counter = 0.0;
       double total = 0.0;
       if(source != null && target != null)
       {
    	   Mapping m = Execution.execute(source, target, "trigrams", THRESHOLD);
           //System.out.println(source+"\n"+target+"\n"+m);
           
           //we could use max, min instead
           //could also
           for (String s : m.map.keySet()) {
               for (String t : m.map.get(s).keySet()) {
                   counter++;
                   total = total + m.getSimilarity(s, t);
               }
           }
       }
       
       if (counter == 0)
           return counter;
       
        return total/(Math.min((double)source.getAllUris().size(),(double)target.getAllUris().size()));
	}
    /** More for testing than anything else
     * 
     * @param class1
     * @param class2
     * @param endpoint1
     * @param endpoint2
     * @return 
     */
    public double getSimilarity(String class1, String class2, String endpoint1, String endpoint2) {
        Cache source = getPropertyValues2(class1, endpoint1, SAMPLING_RATE);
        Cache target = getPropertyValues2(class2, endpoint2, SAMPLING_RATE);
        
        Mapping m = Execution.execute(source, target, "trigrams", THRESHOLD);
        System.out.println(source+"\n"+target+"\n"+m);
        double counter = 0.0;
        double total = 0.0;
        //we could use max, min instead
        //could also
        for (String s : m.map.keySet()) {
            for (String t : m.map.get(s).keySet()) {
                counter++;
                total = total + m.getSimilarity(s, t);
            }
        }
        if (counter == 0) {
            return counter;
        }
        return total/(Math.min((double)source.getAllUris().size(),(double)target.getAllUris().size()));
    }

    public static void test() {
        SamplingBasedClassSimilarity sbc = new SamplingBasedClassSimilarity();
        double value = sbc.getSimilarity("http://dbpedia.org/ontology/Place", "http://dbpedia.org/ontology/Town", "http://live.dbpedia.org/sparql", "http://live.dbpedia.org/sparql");
        System.out.println(value);

    }

    public static void main(String args[]) {
        test();
    }

    
}
