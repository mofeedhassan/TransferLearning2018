/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.saim.replacement;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import org.aksw.saim.transfer.config.ConfigReader;
import org.aksw.saim.transfer.config.Configuration;

/**
 *
 * @author ngonga
 */
public class Replacer {

    /**
     * Replaces the measure in a configuration
     *
     * @param goalConfig Configuration
     * @param goalConfigProperty Property in the initial config
     * @param property Property in the novel config
     * @return New configuration
     */
    public static int COUNT = 0;

    public static Configuration replace(Configuration goalConfig, String goalConfigProperty, Configuration config, String property, boolean source) {
        // check which variable is to replace
        String var;
        String help = property;
        if (source) {
            var = goalConfig.source.var;
        } else {
            var = goalConfig.target.var;
        }
        if (var.startsWith("?")) {
            var = var.substring(1);
        }

        String nameSpace = "";
        String shortName="";
        String nameSpace2 = ""; // with postfix 2 are used in case of cascading properties dbpedia:director/rdfs:label
        String shortName2="";
        //In the following in case the proeprty is not shortened we identify the style
        //of the property if it contains # or does not contain # and get the prefix of it otherwise
        // you add one. This is used to replace the property in the measure as measure uses only shortened properties
        //property="http://www.w3.org/2003/01/geo/wgs84_pos#property";
        if (property.contains("#") && property.startsWith("http")) {// http://debpedia.org#property
            nameSpace = property.substring(0, property.indexOf("#") + 1);
            property = property.substring(property.indexOf("#") + 1);
            shortName = getShortName(config, goalConfig, nameSpace);
            property = shortName + ":" + property;//put as prefix:property
        } 
        else if (property.contains("/") && property.startsWith("http"))//does not contain # e.g. http://debpedia.org/property
        {
            nameSpace = property.substring(0, property.lastIndexOf("/") + 1);
            property = property.substring(property.lastIndexOf("/") + 1);
            shortName = getShortName(config, goalConfig, nameSpace);
            property = shortName + ":" + property;//put as prefix:property
        } 
        else if(property.contains("/") &&!property.startsWith("http")) //cascaded properties dbpedia:director/rdfs:label
        	{
        		String[] properties = property.split("/");
        		shortName = properties[0].split(":")[0];
        		//nameSpace = properties[0].split(":")[1];
        		shortName2 = properties[1].split(":")[0];
        		//nameSpace = properties[1].split(":")[1];
        	}
        else// property is already shortend, not cascaded rdfs:label
        	shortName = property.substring(0,property.lastIndexOf(":"));
        
        
        if (property.startsWith("null")) {
            property = "error";
        }
        String replacement = var + "." + property;
        String toReplace = var + "." + goalConfigProperty;
        
/*        String replacement = "";
        String toReplace = "";
        
        int paroeprtyIndex = config.measure.indexOf(goalConfigProperty)-1;
        if(config.measure.charAt(paroeprtyIndex-1) == '|')//for cases lie ecliudean measure
        {
        	replacement = property;
            toReplace = goalConfigProperty;
        }
        else
        	{
        	  replacement = var + "." + property;
              toReplace = var + "." + goalConfigProperty;
        	}*/
    	goalConfig.measure = goalConfig.measure.replaceAll(Pattern.quote(toReplace), replacement);


        Map<String, String> processing = new HashMap<String, String>();
        processing.put(property, "lowercase");
        if (source) {
            int index = goalConfig.source.properties.indexOf(goalConfigProperty);
            if (index == -1) {
                System.err.println("Goal property " + goalConfigProperty + " no found ");
            } else {

                goalConfig.source.properties.set(index, property);
                goalConfig.source.functions.put(property, processing);
                /* original
                goalConfig.source.prefixes.put(config.source.prefixes.get(nameSpace), nameSpace);
                goalConfig.target.prefixes.put(config.source.prefixes.get(nameSpace), nameSpace);*/
                if(nameSpace!="")
                {
                	goalConfig.source.prefixes.put(shortName, nameSpace);
                    goalConfig.target.prefixes.put(shortName, nameSpace);
                }
                else //already was shortend
                {
                	if(shortName2.length()==0)//not cascaded
                	{
                		goalConfig.source.prefixes.put(shortName, config.source.prefixes.get(shortName));
                		goalConfig.target.prefixes.put(shortName, config.source.prefixes.get(shortName));
                	}
                	else
                	{
                		goalConfig.source.prefixes.put(shortName, config.source.prefixes.get(shortName));
                		goalConfig.target.prefixes.put(shortName, config.source.prefixes.get(shortName));
                		goalConfig.source.prefixes.put(shortName2, config.source.prefixes.get(shortName2));
                		goalConfig.target.prefixes.put(shortName2, config.source.prefixes.get(shortName2));
                	}
                	
                }
                
            }
        } else {
            int index = goalConfig.target.properties.indexOf(goalConfigProperty);
            if (index == -1) {
                System.err.println("Goal property " + goalConfigProperty + " no found ");
            } else {
                goalConfig.target.properties.set(index, property);
                goalConfig.target.functions.put(property, processing);
                /* original
 				goalConfig.target.prefixes.put(config.target.prefixes.get(nameSpace), nameSpace);
                goalConfig.source.prefixes.put(config.target.prefixes.get(nameSpace), nameSpace);*/
                if(nameSpace!="")
                {
                	goalConfig.target.prefixes.put(shortName, nameSpace);
                    goalConfig.source.prefixes.put(shortName, nameSpace);
                }
                else //already was shortend
                {
                	if(shortName2.length()==0)//not cascaded
                	{
                		goalConfig.target.prefixes.put(shortName, config.target.prefixes.get(shortName));
                		goalConfig.source.prefixes.put(shortName, config.target.prefixes.get(shortName));
                	}
                	else
                	{
                		goalConfig.target.prefixes.put(shortName, config.target.prefixes.get(shortName));
                		goalConfig.source.prefixes.put(shortName, config.target.prefixes.get(shortName));
                		goalConfig.target.prefixes.put(shortName2, config.target.prefixes.get(shortName2));
                		goalConfig.source.prefixes.put(shortName2, config.target.prefixes.get(shortName2));
                	}
                }
            }
        }

        //get reduced from of property

        return goalConfig;
    }

    public static String getShortName(Configuration c, Configuration goalConfig, String nameSpace) {
        for (Entry<String, String> e : c.source.prefixes.entrySet()) {
            if (e.getValue().equals(nameSpace)) {
                return e.getKey();
            }
        }
        for (Entry<String, String> e : c.target.prefixes.entrySet()) {
            if (e.getValue().equals(nameSpace)) {
                return e.getKey();
            }
        }
//           for (Entry<String, String> e : goalConfig.source.prefixes.entrySet()) {
//            if (e.getValue().equals(nameSpace)) {
//                c.getSource().prefixes.put(e.getKey(),e.getValue());
//                goalConfig.getTarget().prefixes.put(e.getKey(),e.getValue());
//                return e.getKey();
//            }
//        }
//        for (Entry<String, String> e : goalConfig.target.prefixes.entrySet()) {
//            if (e.getValue().equals(nameSpace)) {
//                c.getSource().prefixes.put(e.getKey(),e.getValue());
//                goalConfig.getTarget().prefixes.put(e.getKey(),e.getValue());
//                return e.getKey();
//            }
//        }
//        
        goalConfig.getSource().prefixes.put("ns" + COUNT, nameSpace);
        goalConfig.getTarget().prefixes.put("ns" + COUNT, nameSpace);
        COUNT++;
        return "ns" + (COUNT - 1);
    }

    public static void main(String args[]) {
        ConfigReader cr = new ConfigReader();
        Configuration c = cr.readLimesConfig("resources/dailymed-dbpedia.limes.xml");
        //c = Replacer.replace(c, "rdfs:label", "foaf:label", true);
        System.out.println(c.measure);
    }
}
