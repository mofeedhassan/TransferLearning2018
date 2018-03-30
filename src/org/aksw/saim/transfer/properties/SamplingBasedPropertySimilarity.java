/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.saim.transfer.properties;

import com.hp.hpl.jena.query.*;
import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.HybridCache;
import de.uni_leipzig.simba.data.Mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aksw.saim.io.ReadFile;
import org.aksw.saim.io.ReadFile.SimilarityType;
import org.aksw.saim.transfer.config.Configuration;
import org.aksw.saim.util.Execution;

/**
 *
 * @author ngonga
 */
public class SamplingBasedPropertySimilarity implements PropertySimilarity {

    public int SAMPLING_RATE = 200;
    public double THRESHOLD = 0.5;
    public static Map<String, String> prefixes = new HashMap<String, String>();
    
    private static Cache getPropertyValues(String c, String p, String endpoint, int size) {
        Cache cache = new HybridCache();
        String query = "SELECT ?a ?x "
                + "WHERE { ?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + c + ">. "
                + "?a <" + p + "> ?x .} LIMIT " + size;
        Query sparqlQuery = QueryFactory.create(query);

        QueryExecution qexec;
        qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery);
        ResultSet results = qexec.execSelect();

        String x, a, b;
        QuerySolution soln;
        while (results.hasNext()) {
            soln = results.nextSolution();
            cache.addTriple(soln.get("a").toString(), "p", soln.get("x").toString());
        }
        return cache;
    }
    private static Cache getPropertyValues2(String c, String p, String endpoint, int size){
    	List<String> parameters = new ArrayList<>();
    	parameters.add(c);
    	parameters.add(p);
    	parameters.add(endpoint);
    	parameters.add(String.valueOf(size));
    	return ReadFile.loadPropertyData(SimilarityType.SAMPLE, parameters);
    }

/*    @Override
    public double getSimilarity(String property1, String property2, String class1, String class2, Configuration config) {
        String p1 = expand(property1, config);
        String p2 = expand(property2, config);
        String c1 = expand(class1, config);
        String c2 = expand(class2, config);
        
        Cache source = getPropertyValues2(c1, p1, config.source.endpoint, SAMPLING_RATE);
        Cache target = getPropertyValues2(c2, p2, config.target.endpoint, SAMPLING_RATE);

        Mapping m = Execution.execute(source, target, "trigrams", THRESHOLD);
        //System.out.println(source + "\n" + target + "\n" + m);
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
        return total / (Math.min((double) source.getAllUris().size(), (double) target.getAllUris().size()));
    }*/
    
    @Deprecated
    @Override
    public double getSimilarity(String property1, String property2, String class1, String class2, Configuration config) {
        String p1 = expand(property1, config);
        String p2 = expand(property2, config);
        String c1 = expand(class1, config);
        String c2 = expand(class2, config);
        
        Cache source = getPropertyValues2(c1, p1, config.source.endpoint, SAMPLING_RATE);
        Cache target = getPropertyValues2(c2, p2, config.target.endpoint, SAMPLING_RATE);

        Mapping m = Execution.execute(source, target, "trigrams", THRESHOLD);
        //System.out.println(source + "\n" + target + "\n" + m);
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
        return total / (Math.min((double) source.getAllUris().size(), (double) target.getAllUris().size()));
    }
	@Override
	public double getSimilarity(String property, String inutProperty, String class1, String inputClass, Configuration configuration, Configuration inputConfiguration, boolean isSource) {
        String p1 = expand(property, configuration);
        String p2 = expand(inutProperty, inputConfiguration);
        String c1 = expand(class1, configuration);
        String c2 = expand(inputClass, inputConfiguration);
        
        Cache source = null;
        Cache target = null;
        
        if(isSource)
        {
            source = getPropertyValues2(c1, p1, configuration.source.endpoint, SAMPLING_RATE);
            target = getPropertyValues2(c2, p2, inputConfiguration.source.endpoint, SAMPLING_RATE);
        }
        else
        {
            source = getPropertyValues2(c1, p1, configuration.target.endpoint, SAMPLING_RATE);
            target = getPropertyValues2(c2, p2, inputConfiguration.target.endpoint, SAMPLING_RATE);
        }

        Mapping m = Execution.execute(source, target, "trigrams", THRESHOLD);
        //System.out.println(source + "\n" + target + "\n" + m);
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
        return total / (Math.min((double) source.getAllUris().size(), (double) target.getAllUris().size()));
	}

    public static void test() {
        System.out.println(getPropertyValues("http://dbpedia.org/ontology/Place", "http://www.w3.org/2000/01/rdf-schema#label", "http://live.dbpedia.org/sparql", 100));
    }

    public static void main(String args[]) {
        test();
    }

    public static String expand(String property, Configuration config) {
        if(property.startsWith("http")) return property;
        String prefix = property.substring(0, property.indexOf(":"));
        String name = property.substring(property.indexOf(":") + 1);
        
        String prefix2="";
        String name2="";
        if(name.contains("/")) //cascading properties
        {
        	String[] cascaded = name.split("/");
        	name= cascaded[0];
        	prefix2=cascaded[1].substring(0, cascaded[1].indexOf(":"));
            name2=cascaded[1].substring(cascaded[1].indexOf(":") + 1);
            
            if(config.source.prefixes.containsKey(prefix) && config.source.prefixes.containsKey(prefix2)){
            	return config.source.prefixes.get(prefix)+name+"/"+config.source.prefixes.get(prefix2)+name2;
            }
            else if (config.target.prefixes.containsKey(prefix) && config.target.prefixes.containsKey(prefix2))
            	return config.target.prefixes.get(prefix)+name+"/"+config.target.prefixes.get(prefix2)+name2;
            
            else if (config.source.prefixes.containsKey(prefix) && config.target.prefixes.containsKey(prefix2))
            	return config.source.prefixes.get(prefix)+name+"/"+config.target.prefixes.get(prefix2)+name2;
            
            else if (config.target.prefixes.containsKey(prefix) && config.source.prefixes.containsKey(prefix2))
            	return config.target.prefixes.get(prefix)+name+"/"+config.source.prefixes.get(prefix2)+name2;
         
            
        }
        
        if(prefixes.containsKey(prefix)) return prefixes.get(prefix)+name;
        if(config.source.prefixes.containsKey(prefix)) return config.source.prefixes.get(prefix)+name;
        if(config.target.prefixes.containsKey(prefix)) return config.target.prefixes.get(prefix)+name;
        if(prefix.equals("dbpedia-owl")) return "http://dbpedia.org/ontology/"+name;
        if(prefix.equals("am")) return "http://purl.org/collections/nl/am/"+name;
        if(prefix.equals("lgdo")) return "http://linkedgeodata.org/ontology/"+name;
        if(prefix.equals("yago")) return "http://dbpedia.org/class/yago/"+name;
        if(prefix.equals("BibTex")) return "http://data.bibbase.org/ontology/#"; //
        if(prefix.equals("linkedct")) return "http://data.linkedct.org/vocab/resource/"+name;
        if(prefix.equals("administrative-geography")) return "http://statistics.data.gov.uk/def/administrative-geography/";
        else System.err.println("Prefix "+prefix+" not found for property "+property);
        System.exit(1);
        return null;
    }



}
