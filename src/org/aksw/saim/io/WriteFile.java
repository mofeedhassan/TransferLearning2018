package org.aksw.saim.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.HybridCache;

public class WriteFile {
	
	static Logger logger = Logger.getLogger("LIMES");

    public static void writeModel(Model model,String fileName, String format) {
    	logger.info("Writing model into file "+fileName);
        long startTime = System.currentTimeMillis();
        try (FileWriter out = new FileWriter( fileName )) {
            if (format.contains("ttl") || format.contains(".n3")) {
            	logger.info("Writing Turtle file");
                model.write(out, "TTL");
            } else if (format.contains("rdf")) {
            	logger.info("Writing RDFXML file");
                model.write(out, null);
            } else if (format.contains("nt")) {
            	logger.info("Writing N-Triples file");
                model.write(out, "N-TRIPLE");
            } else {
            	logger.error("WRONG FORMAT XXXXX");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    
    public static void appendToFile(List<String> data ,String file)
    {
    		try(FileWriter fw = new FileWriter(file, true);
    		    BufferedWriter bw = new BufferedWriter(fw);
    		    PrintWriter out = new PrintWriter(bw))
    		{
    			for (String string : data) {
        		    out.println(string);

				}

    		} catch (IOException e) {
    		    logger.error("Error while appending data to file: "+file);
    		}
    }
    
    
    public static void writeToFile(List<String> data,  String file){
    	BufferedWriter bw = null;
		FileWriter fw = null;

		try {

			fw = new FileWriter(file);
			bw = new BufferedWriter(fw);
			for (String string : data) {
				bw.write(string);
			}
	
			System.out.println("Done");

		} catch (IOException e) {

			e.printStackTrace();

		} finally {

			try {

				if (bw != null)
					bw.close();

				if (fw != null)
					fw.close();

			} catch (IOException ex) {

				ex.printStackTrace();

			}

		}
    }
    
    
}
