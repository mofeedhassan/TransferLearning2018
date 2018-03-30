package org.aksw.saim.transfer;

import java.util.List;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Handler;

import org.aksw.saim.io.ReadFile;
import org.aksw.saim.io.WriteFile;
import org.aksw.saim.transfer.config.Configuration;
import org.aksw.saim.util.DataSetUtils;
import org.aksw.saim.util.SparqlUtils;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.util.DatasetUtils;

public class Main {

    static Logger logger = LoggerFactory.getLogger(TransferLearner.class);

	static FileHandler fh;
	static String currentFolder = "";
	public static int sleepTime=0;
	
	
	public static void main(String[] args) {
		logger.info("Hello");
/*		List<String> uris = ReadFile.readFile("/media/mofeed/TOSHIBA2T/00-PhDProjects2018/TransferLearning/TransferLearningTmp/geonamesgolduris/uris");
		List<String> gold = ReadFile.readFile("/media/mofeed/TOSHIBA2T/00-PhDProjects2018/TransferLearning/TransferLearningTmp/geonamesgolduris/gold.rdf");
		for (String uri : uris) {
			boolean found=false;
			for (String guri : gold) {
				if(guri.contains(uri))
					found=true;
			}
			if(!found)
				System.out.println(uri);
		}*/

       	
    	String option = args[0];
    	if(option.toLowerCase().equals("dumpclass"))
    	{
    		String className = args[1];
    		String endpoint = args[2];
    		boolean isEndpoint = Boolean.valueOf(args[3]);
    		String filePath = args[4];
    		TransferLearner.getDumpForClass(className, endpoint, isEndpoint,filePath);

    	}
    	else if(option.toLowerCase().equals("minicache"))
    	{
    		String folder = args[1];
    		int requiredSize = Integer.parseInt(args[2]);
    		TransferLearner.runMiniCaching(folder,requiredSize);
    	}
    	else if(option.toLowerCase().equals("dumpendpoint"))
    	{
    		org.aksw.saim.util.DataSetUtils.getEndpointDump(args[1], args[2], args[3]);
    	   	 
    	}
    	else if(option.toLowerCase().equals("misseduris"))//retrieve the uris do not exist in the dump file from the endpoint
    	{
    		List<String> missed =org.aksw.saim.util.DataSetUtils.checkMissedURIs(args[1], args[2], args[3]);
    		WriteFile.writeToFile(missed, "misseduris");
    	   	 
    	}
    	else if(option.toLowerCase().equals("dumpuris"))//It dumps list of URIs either in a link file or standalone file FROM endpoint or dump file
    	{
    		org.aksw.saim.util.DataSetUtils.dumpURIs(args[1], args[2],args[3], Boolean.valueOf(args[4]), Boolean.valueOf(args[5]));
   	   	 
    	}
    	else if(option.toLowerCase().equals("getclassuri"))
    	{
    		org.aksw.saim.util.DataSetUtils.getClassURIs(args[1], args[2], args[3]);
   	   	 
    	}
    	else if(option.toLowerCase().equals("tl"))
    	{
    		TransferLearner.runTransferLearning(args[1], args[2], args[3],1);
    	}
    	else if(option.toLowerCase().equals("sparql"))
    	{
    		TransferLearner.sparqlToSerialize(args[1], args[2]);
    	}
    	else{
    		System.out.println("WRONG OPTION\n. The available options are:\n");
    		System.out.println("\tDump a class -> dumpclass className endpoint isEndpoint filePath(Folder/filename) \n");
    		System.out.println("\tCache the pair data sets for each spec in folder -> minicache folder requiredSize\n");
    		System.out.println("\tDump an endpoint -> dumpendpoint endpoint file offset\n");
    		System.out.println("\tRetrieve the uris do not exist in the dump file from the endpoint for a class-> misseduris classname endpoint dump\n");
    		System.out.println("\tDumps list of URIs either in a link file or standalone file FROM endpoint or dump file -> dumpuris urisFile repository dumpFolder isRightSide isLinksFile\n");
    		System.out.println("\tGet subjects of specific data class in a repository and store it into file -> getclassuri className repository file\n");
    		System.out.println("\tRun Transfer Learning -> tl specifications_folder classSim propSim\n");
    		System.out.println("\tRun SPARQQL query on endpoint and serialize the retrieved result into cache folder in jar location-> sparql sparqlQueryFile endpoint\n");
    	}

	}
	private static void trial()
	{
		/*ConsoleAppender console = new ConsoleAppender(); //create appender
		  //configure the appender
		  String PATTERN = "%d [%p|%c|%C{1}] %m%n";
		  console.setLayout(new PatternLayout(PATTERN)); 
		  console.setThreshold(Level.FATAL);
		  console.activateOptions();
		  //add appender to any Logger (here is root)
		  org.apache.logging.log4j.LogManager.getRootLogger().addAppender(console);

		  FileAppender fa = new FileAppender();
		  fa.setName("FileLogger");
		  fa.setFile("mylog.log");
		  fa.setLayout(new PatternLayout("%d %-5p [%c{1}] %m%n"));
		  fa.setThreshold(Level.DEBUG);
		  fa.setAppend(true);
		  fa.activateOptions();

		  //add appender to any Logger (here is root)
		  Logger.getRootLogger().addAppender(fa);
		  //repeat with all other desired appenders
*/	}

}
