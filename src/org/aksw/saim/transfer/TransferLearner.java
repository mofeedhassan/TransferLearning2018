package org.aksw.saim.transfer;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.evaluation.PRFComputer;
import sun.awt.datatransfer.DataTransferer;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import org.aksw.saim.io.ReadFile;
import org.aksw.saim.io.WriteFile;
import org.aksw.saim.replacement.Replacer;
import org.aksw.saim.structure.BestConfigurationDO;
import org.aksw.saim.transfer.classes.ClassSimilarity;
import org.aksw.saim.transfer.classes.LabelBasedClassSimilarity;
import org.aksw.saim.transfer.classes.SamplingBasedClassSimilarity;
import org.aksw.saim.transfer.classes.UriBasedClassSimilarity;
import org.aksw.saim.transfer.config.ConfigAccuracy;
import org.aksw.saim.transfer.config.ConfigAccuracyWald95;
import org.aksw.saim.transfer.config.ConfigReader;
import org.aksw.saim.transfer.config.Configuration;
import org.aksw.saim.transfer.properties.UriBasedPropertySimilarity;
import org.aksw.saim.transfer.properties.PropertySimilarity;
import org.aksw.saim.transfer.properties.SamplingBasedPropertySimilarity;
import org.aksw.saim.util.ClassSimType;
import org.aksw.saim.util.ClassSimilarityFactory;
import org.aksw.saim.util.DataSetUtils;
import org.aksw.saim.util.Execution;
import org.aksw.saim.util.PropertySimType;
import org.aksw.saim.util.PropertySimilarityFactory;
import org.aksw.saim.util.SparqlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.util.DatasetUtils;

/**
 * The transfer learning algorithm core.
 *
 * @author Jens Lehmann
 * @author Axel Ngonga
 *
 */
public class TransferLearner {

    public static double COVERAGE = 0.6;
    public static int SAMPLESIZE = 300;
    private Set<Configuration> configurations;
    private Map<Configuration, String> posExamples;
    private Map<Configuration, String> negExamples;
    private Map<Configuration, Set<String>> sourceProperties;
    private Map<Configuration, Set<String>> targetProperties;
    private Map<String, Set<String>> sourcePropertyCache;
    private Map<String, Set<String>> targetPropertyCache;
    private String resultBuffer;
    public static ClassSimilarity classSim = null;//new UriBasedClassSimilarity();
    public static PropertySimilarity propSim = null; //new UriBasedPropertySimilarity();
    public static String specsFolder="";
    Logger logger = LoggerFactory.getLogger(TransferLearner.class);
    public static Map<Configuration,Configuration> config_bestConfig = new HashMap<>();
    
    
    public static void main(String args[]) {
       	
    	String option = args[0];
    	if(option.toLowerCase().equals("dumpclass"))
    	{
    		String className = args[1];
    		String endpoint = args[2];
    		boolean isEndpoint = Boolean.valueOf(args[3]);
    		String filePath = args[4];
        	getDumpForClass(className, endpoint, isEndpoint,filePath);

    	}
    	else if(option.toLowerCase().equals("minicache"))
    	{
    		String folder = args[1];
    		runMiniCaching(args[1],1000);
    	}
    	else if(option.toLowerCase().equals("dumpendpoint"))
    	{
    		org.aksw.saim.util.DataSetUtils.getEndpointDump(args[0], args[1], args[2]);
    	   	 
    	}
    	else
    		System.out.println("WRONG OPTION");


    	//runMiniCaching("/media/mofeed/TOSHIBA2T/00-PhDProjects2018/TransferLearning/TransferLearningTmp/testDumping/",1000);
    	//org.aksw.saim.util.DataSetUtils.getEndpointDump(args[0], args[1], args[2]);
   	 
    	//getDumpForClass("http://www.aktors.org/ontology/portal#Person", "http://citeseer.rkbexplorer.com/sparql/", true, "/media/mofeed/TOSHIBA2T/00-PhDProjects2018/TransferLearning/TransferLearningTmp/miniDatastsSpecs/Citseer-Bibase/citseerPersonDump.nt");
    	

    }
    
    public static Model model=null;
    public static void getDumpForClass(String className, String dataRepository, boolean endpoint, String fileName)
    {
    	DataSetUtils.createMiniDumpForClass(className, dataRepository, endpoint,fileName);
    }
    
    public static void checkMissedURI(String className, String dataRepository, String dumpFile)
    {
    	List<String>  misseduris = DataSetUtils.checkMissedURIs(className, dataRepository, dumpFile);
    	WriteFile.writeToFile(misseduris, "missedURIs");
    }
    public TransferLearner(Set<Configuration> configurations,
            Map<Configuration, String> posExamples,
            Map<Configuration, String> negExamples) {
        this.configurations = configurations;
        this.posExamples = posExamples;
        this.negExamples = negExamples;
    }

    public TransferLearner(String configFolder) {
        resultBuffer = "";
        System.out.println(new File(configFolder).getAbsolutePath());
        configurations = new HashSet<>();
        posExamples = new HashMap<>();
        negExamples = new HashMap<>();
        sourceProperties = new HashMap<>();
        targetProperties = new HashMap<>();
        negExamples = new HashMap<>();
        sourcePropertyCache = new HashMap<>();
        targetPropertyCache = new HashMap<>();

        File f = new File(configFolder), folderName;
        String[] files = f.list();
        ConfigReader cr = new ConfigReader();
        Configuration config;
        int count = 0;

        //check for writeCache
        File cache = new File(configFolder + "/propertyCache.tsv");
        if (cache.exists()) {
            System.out.println("Found cached data");
            readCache(configFolder);
        }
        for (String file : files) {
            folderName = new File(configFolder + "/" + file);
            if (folderName.isDirectory()) {
                if (!new File(folderName.getAbsoluteFile() + "/fixme.txt").exists()) {
                    config = cr.readLimesConfig(folderName.getAbsolutePath() + "/spec.xml");
                    count++;
                    System.out.println("Processing " + count + ".\t" + config.getName());
                    String sourceClass = config.source.getClassOfendpoint(true);
                    String targetClass = config.target.getClassOfendpoint(true);
                    for(String p: config.source.prefixes.keySet())
                    SamplingBasedPropertySimilarity.prefixes.put(p, config.source.prefixes.get(p));
                    for(String p: config.target.prefixes.keySet())
                    SamplingBasedPropertySimilarity.prefixes.put(p, config.target.prefixes.get(p));
                    SamplingBasedPropertySimilarity.prefixes.put("foaf", "http://xmlns.com/foaf/0.1/");
                    SamplingBasedPropertySimilarity.prefixes.put("BibTeX", "http://data.bibbase.org/ontology/#");
                    SamplingBasedPropertySimilarity.prefixes.put("epo", "http://epo.publicdata.eu/ebd/ontology/");

                    Set<String> relevantSourceProperties = new HashSet<>();
                    Set<String> relevantTargetProperties = new HashSet<>();

                    //check whether data already cached
                    if (sourcePropertyCache.containsKey(config.name)) {
                        relevantSourceProperties = sourcePropertyCache.get(config.name);
                    } else {
                        relevantSourceProperties = SparqlUtils.getRelevantProperties(config.getSource().endpoint, sourceClass, SAMPLESIZE, COVERAGE); //cache the properties relevant to the configuration
                        if (relevantSourceProperties.size() > 0) {
                            sourcePropertyCache.put(config.name, relevantSourceProperties);
                        }
                    }
                    //same here
                    if (targetPropertyCache.containsKey(config.name)) {
                        relevantTargetProperties = targetPropertyCache.get(config.name);
                    } else {
                        relevantTargetProperties = SparqlUtils.getRelevantProperties(config.getTarget().endpoint, targetClass, SAMPLESIZE, COVERAGE);
                        if (relevantTargetProperties.size() > 0) {
                            targetPropertyCache.put(config.name, relevantTargetProperties);
                        }
                    }

                    if (!relevantSourceProperties.isEmpty() && !relevantTargetProperties.isEmpty()) {
                        configurations.add(config);
                        sourceProperties.put(config, relevantSourceProperties);
                        targetProperties.put(config, relevantTargetProperties);
                        posExamples.put(config, folderName.getAbsolutePath() + "/positive.nt");
                        negExamples.put(config, folderName.getAbsolutePath() + "/negative.nt");/////
                    }
                }
            }
        }
        writeCache(configFolder);
    }

    public TransferLearner(String configFolder, boolean useSparql) {
        resultBuffer = "";
        configurations = new HashSet<>();
        posExamples = new HashMap<>();
        negExamples = new HashMap<>();
        sourceProperties = new HashMap<>();
        targetProperties = new HashMap<>();
        negExamples = new HashMap<>();
        sourcePropertyCache = new HashMap<>();
        targetPropertyCache = new HashMap<>();

        File f = new File(configFolder), folderName;
        String[] files = f.list();
        ConfigReader cr = new ConfigReader();
        Configuration config;
        int count = 0;

        //check for writeCache        
        for (String file : files) 
        {
            folderName = new File(configFolder + "/" + file);
            if (folderName.isDirectory()) 
            {
                if (!new File(folderName.getAbsoluteFile() + "/fixme.txt").exists()) {
                    config = cr.readLimesConfig(folderName.getAbsolutePath() + "/spec.xml");
                    count++;
                    System.out.println("Processing " + count + ".\t" + config.getName());
                    String sourceClass = config.source.getClassOfendpoint(true);
                    String targetClass = config.target.getClassOfendpoint(true);
                    Set<String> relevantSourceProperties = new HashSet<>();
                    Set<String> relevantTargetProperties = new HashSet<>();

                    //check whether data already cached-boolean to differentiate sourc from target
                    relevantSourceProperties = getRelevantProperties(config, true);
                    relevantTargetProperties = getRelevantProperties(config, false);

                    if (!relevantSourceProperties.isEmpty() && !relevantTargetProperties.isEmpty()) {
                        configurations.add(config);
                        sourceProperties.put(config, relevantSourceProperties);
                        targetProperties.put(config, relevantTargetProperties);
                        posExamples.put(config, folderName.getAbsolutePath() + "/positive.nt");
                        negExamples.put(config, folderName.getAbsolutePath() + "/negative.nt");//{
                    }
                }
            }
        }
    }

    public TransferLearner(String configFolder,int requiredSize) {
    	
    	resultBuffer = "";
        configurations = new HashSet<>();
        posExamples = new HashMap<>();
        negExamples = new HashMap<>();
        sourceProperties = new HashMap<>();
        targetProperties = new HashMap<>();
        negExamples = new HashMap<>();
        sourcePropertyCache = new HashMap<>();
        targetPropertyCache = new HashMap<>();

        File f = new File(configFolder), folderName;
        if(f.exists())//folder exists?/
        {
        	String[] files = f.list();
            ConfigReader cr = new ConfigReader();
            Configuration config;
            int count = 0;
            if(null!=files) //folder contains files
            {
                //check for writeCache        
                for (String file : files) 
                {
                    folderName = new File(configFolder + "/" + file);
                    if (folderName.isDirectory())//file exists 
                    {
                    		logger.info("Read configuration " + folderName.getAbsolutePath() + "/spec.xml");
                            config = cr.readLimesConfig(folderName.getAbsolutePath() + "/spec.xml");

                    		logger.info("Read goldstandard " + folderName.getAbsolutePath() + "/spec.xml");
                            Map<Integer,List<String>> gold = ReadFile.readLinksFile(folderName.getAbsolutePath() + "/reference.nt");
                            logger.info("Loaded data from gold standard for Source: "+gold.get(1).size()+ " URIs and for target: "+gold.get(2).size()+" URIs");
                            count++;
                            logger.info("Processing " + count + ".\t" + config.getName());
                            String sourceClass = config.source.getClassOfendpoint(true);
                            String targetClass = config.target.getClassOfendpoint(true);
                            String Sendpoint = config.source.endpoint;
                            String Tendpoint = config.target.endpoint;
                         
                            		
                    		logger.info("create mini dump for configuration " + folderName.getAbsolutePath() + "/spec.xml");
                    		logger.info("create mini dump for Source data: " + config.source.id);
                    		Model model=null;
/*                    		if(Sendpoint.startsWith("http"))
                   			  model = DataSetUtils.createMiniDumpForURIsList(gold.get(1), sourceClass, Sendpoint, requiredSize, true);
                    		else
                  			  model = DataSetUtils.createMiniDumpForURIsList(gold.get(1), sourceClass, Sendpoint, requiredSize, false);
                    		
                    		logger.info("write the source data into mini dump ");
                    		DataSetUtils.writeTheDump( model,folderName.getAbsolutePath(), config.source.id, sourceClass, requiredSize);*/
                    		
                    		if(Tendpoint.startsWith("http"))
                    			model = DataSetUtils.createMiniDumpForURIsList(gold.get(2), targetClass, Tendpoint, requiredSize, true);
                      		else
                      			model = DataSetUtils.createMiniDumpForURIsList(gold.get(2), targetClass, Tendpoint, requiredSize, false);

                    		logger.info("write the target data into mini dump ");
                    		DataSetUtils.writeTheDump( model,folderName.getAbsolutePath(), config.target.id, targetClass, requiredSize);

                    }
                }
            }
            else
            	logger.error("No files exist in the folder");
        }
        else
        	logger.error("Folder does not exist");

    	
    	
        /*resultBuffer = "";
        configurations = new HashSet<>();
        posExamples = new HashMap<>();
        negExamples = new HashMap<>();
        sourceProperties = new HashMap<>();
        targetProperties = new HashMap<>();
        negExamples = new HashMap<>();
        sourcePropertyCache = new HashMap<>();
        targetPropertyCache = new HashMap<>();

        File f = new File(configFolder), folderName;
        if(f.exists())//folder exists?/
        {
        	String[] files = f.list();
            ConfigReader cr = new ConfigReader();
            Configuration config;
            int count = 0;
            if(null!=files) //folder contains files
            {
                //check for writeCache        
                for (String file : files) 
                {
                    folderName = new File(configFolder + "/" + file);
                    if (folderName.isDirectory())//file exists 
                    {
                            config = cr.readLimesConfig(folderName.getAbsolutePath() + "/spec.xml");
                            //Model m = ReadFile.readModel(folderName.getAbsolutePath() + "/spec.xml");
                            Map<Integer,List<String>> gold = ReadFile.readFile(folderName.getAbsolutePath() + "/gold.nt");
                            System.out.println("Loading data from gold standard "+gold.get(1).size()+ " and "+gold.get(2).size());
                            count++;
                            System.out.println("Processing " + count + ".\t" + config.getName());
                            String sourceClass = config.source.getClassOfendpoint(true);
                            String targetClass = config.target.getClassOfendpoint(true);
                            String Sendpoint = config.source.endpoint;
                            String Tendpoint = config.target.endpoint;
                           // DataSetUtils.cacheMiniDataset(gold.get(1),folderName.getAbsolutePath(),config.source.id,sourceClass,miniSize ,Sendpoint);
                           // DataSetUtils.cacheMiniDataset(gold.get(2),folderName.getAbsolutePath(),config.target.id,targetClass,miniSize ,Tendpoint);

                    }
                }
            }
            else
            	logger.error("No files exist in the folder");
        }
        else
        	logger.error("Folder does not exist");
*/    }
    
    public void writeCache(String folder) {
        try {
            new File(folder + "/propertyCache.tsv").delete();
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(folder + "/propertyCache.tsv")));
            for (Configuration c : configurations) {
                Set<String> sourceProps = sourceProperties.get(c);
                for (String property : sourceProps) {
                    writer.println(c.name + "\tSourceProperty\t" + property);
                }
                Set<String> targetProps = targetProperties.get(c);

                for (String property : targetProps) {
                    writer.println(c.name + "\tTargetProperty\t" + property);
                }
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readCache(String folder) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(folder + "/propertyCache.tsv"));
            String s = reader.readLine();
            sourcePropertyCache = new HashMap<>();
            targetPropertyCache = new HashMap<>();
            while (s != null) {
                String[] split = s.split("\t");
                if (split[1].equals("SourceProperty")) {
                    if (!sourcePropertyCache.containsKey(split[0])) {
                        sourcePropertyCache.put(split[0], new HashSet<String>());
                    }
                    sourcePropertyCache.get(split[0]).add(split[2]);
                } else {
                    if (!targetPropertyCache.containsKey(split[0])) {
                        targetPropertyCache.put(split[0], new HashSet<String>());
                    }
                    targetPropertyCache.get(split[0]).add(split[2]);
                }
                s = reader.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * constructs for each configuration list of configurations sorted by their
     * similarity degrees to the main configuration
     *
     * @param instanceSampleSize Sampling size
     * @param coverage Minimal coverage for
     * @return
     */
    public Map<Configuration, List<Configuration>> runOrderedMatching() {
        Map<Configuration, List<Configuration>> results = new HashMap<>();//configuration -> list of similar sorted configurations
        List<Configuration> result;
        for (Configuration config : configurations) {
        	
            List<String> sourceClasses = getRestrictionList(config.source.restrictions);
            List<String> targetClasses = getRestrictionList(config.target.restrictions);
            
            Set<String> relevantSourceProperties = sourceProperties.get(config);
            Set<String> relevantTargetProperties = targetProperties.get(config);
            
            result = runLeaveOneOutOrdered(config, sourceClasses, targetClasses, relevantSourceProperties, relevantTargetProperties);
            results.put(config, result);
        }
        return results;
    }

    /**
     * Runs the whole transfer learning experiment for all specs using the URI matching approach
     * for each configuration select a configuration
     * 1- get its specifications in terms of classes and properties  
     * 2- get the best matching configuration of the others (returned cloned and replaced with input specifications except metric)
     * 3- get the reference
     * 4- run the best configuration
     * 5- calc results accuracy
     * @param instanceSampleSize Sampling size
     * @param coverage Minimal converage for
     * @return
     */
    public Map<Configuration, BestConfigurationDO> runSimpleMatching(int instanceSampleSize, double coverage) {
        Map<Configuration, BestConfigurationDO> results = new HashMap<Configuration, BestConfigurationDO>();
        Configuration result;
        String output = "Specification \t Precision \t Recall \t F-Measure\n";
        String configOutput = "Specification \t Precision \t Recall \t F-Measure\n";
        PRFComputer prf = new PRFComputer();
        Execution exec = new Execution();
        //iterate over configurations
        for (Configuration config : configurations) {
        	//get the source and target classes
            System.out.println("Processing " + config.name + ":" + config.source.getClassOfendpoint(true) + " -> " + config.target.getClassOfendpoint(true));
            String sourceClass = config.source.getClassOfendpoint(true);
            String targetClass = config.target.getClassOfendpoint(true);
            
            //get the properties of the source and the target of the configuration
            System.out.println("Getting source properties ... ");
            Set<String> relevantSourceProperties = sourceProperties.get(config);
            System.out.println("Got " + relevantSourceProperties.size() + " source properties.\nGetting target properties ... ");
            Set<String> relevantTargetProperties = targetProperties.get(config);
            System.out.println("Got " + relevantTargetProperties.size() + " target properties.\n");
            System.out.println("Running leave one out ... ");
            
            // gets the best configuration with its specs are replaced with the input configuration - metric
            result = runLeaveOneOut(config, sourceClass, targetClass, relevantSourceProperties, relevantTargetProperties);
            
            logger.info("The best configuration infromation is \n");
            logger.info(result.source.id);
            logger.info(result.source.restrictions.toString());
            logger.info(result.source.endpoint);
            logger.info(result.source.properties.toString());
            logger.info(result.target.id);
            logger.info(result.target.restrictions.toString());
            logger.info(result.target.endpoint);
            logger.info(result.target.properties.toString());
            logger.info(result.getMeasure());
            
            
            config_bestConfig.put(config, result);
            // now run best configuration and get precision, recall and f-measure
            System.out.println("Getting reference mapping ...");
          //  Mapping reference = new Mapping().readNtFile(config.name.replaceAll(Pattern.quote("spec.xml"), "accept.nt"));
            Mapping reference = new Mapping().readNtFile(config.name.replaceAll(Pattern.quote("spec.xml"), "accept.nt"));
            System.out.println("Running new spec derived from " + result.name + " ...");
            ///comment this section as linking consumes time
            Mapping transferResult = exec.executeComplex(result);
//            Mapping configResult = exec.executeComplex(config);
            double precision=prf.computePrecision(transferResult, reference);
            double recall=prf.computeRecall(transferResult, reference);
            double fscore=prf.computeFScore(transferResult, reference);
//          configOutput = configOutput + new File(config.name).getParent() + "\t" + prf.computePrecision(configResult, reference) + "\t" + prf.computeRecall(configResult, reference) + "\t" + prf.computeFScore(configResult, reference) + "\n";
            output = output + new File(config.name).getParent() + "\t" + precision + "\t" + recall + "\t" + fscore + "\n";
/*            ConfigAccuracy ca = new ConfigAccuracyWald95();
            double acc = ca.getAccuracy(result, posExamples.get(config), negExamples.get(config));*/
            System.out.println("*"+output);
            
            //add the transfered configuration (learned) and its accuracy
            //results.put(result, acc);
            results.put(config, new BestConfigurationDO(result, precision, recall, fscore));
            //System.exit(1);
        }
        System.out.println("\n\n=== FINAL RESULTS ===\n" + output + "\n\n");
        System.out.println("\n\n" + resultBuffer + "\n\n");
        System.out.println("The accuracy measures of the link specs with the original metrics\n");
        System.out.println("\n\n=== FINAL RESULTS (Originl linking) ===\n" + configOutput + "\n\n");
        return results;
    }
//    /**
//     * Gets an input source and target class and return the best possible config
//     *
//     * @param inputSourceClass Input source class
//     * @param inputTargetClass Input target class
//     * @param relevantSourceProperties Source properties
//     * @param relevantTargetProperties Target properties
//     */
//    public Configuration run(String inputSourceClass, String inputTargetClass, Set<String> relevantSourceProperties, Set<String> relevantTargetProperties) {
//
//        // setup
//        ConfigAccuracy confAcc = new ConfigAccuracyWald95();
//        ClassSimilarity classSim = new UriBasedClassSimilarity();
//        PropertySimilarity propSim = new UriBasedPropertySimilarity();
//        double bestF = 0;
//        Configuration bestConfig = configurations.iterator().next();
//        for (Configuration configuration : configurations) {
//            // accuracy of link specification; // TODO: where to get the positive and negative examples?
//            double alpha = confAcc.getAccuracy(configuration, posExamples.get(configuration), negExamples.get(configuration));
//
//            String sourceClass = configuration.getSource().getClassOfendpoint();
//            String targetClass = configuration.getTarget().getClassOfendpoint();
//
//            double cSSim = classSim.getSimilarity(sourceClass, inputSourceClass, configuration);
//            double cTSim = classSim.getSimilarity(targetClass, inputTargetClass, configuration);
//
//            double factor = alpha * cSSim * cTSim;
//            if (bestF < factor) {
//                bestF = factor;
//                bestConfig = configuration;
//            }
//
//        }
//        // figure out best mapping properties
//        //first for source
//
//        String sourceClass = bestConfig.getSource().getClassOfendpoint();
//        String targetClass = bestConfig.getTarget().getClassOfendpoint();
//        Map<String, String> sourcePropertyMapping = getPropertyMap(relevantSourceProperties,
//                sourceClass, inputSourceClass, propSim, bestConfig, true);
//        Map<String, String> targetPropertyMapping = getPropertyMap(relevantTargetProperties,
//                targetClass, inputTargetClass, propSim, bestConfig, true);
//
//        //important: clone the config
//        bestConfig = new ConfigReader().readLimesConfig(bestConfig.name);
//        for (String property : sourcePropertyMapping.keySet()) {
//            bestConfig = Replacer.replace(bestConfig, property, sourcePropertyMapping.get(property), true);
//        }
//        for (String property : targetPropertyMapping.keySet()) {
//            bestConfig = Replacer.replace(bestConfig, property, targetPropertyMapping.get(property), false);
//        }
//
//        System.out.println(bestConfig.source.getClassOfendpoint());
//        System.out.println(bestConfig.target.getClassOfendpoint());
//        System.out.println(bestConfig.measure);
//        //System.out.println(bestConfig.source.getClassOfendpoint());
//
//        return bestConfig;
//    }

    /**
     * Gets the best similar configuration to a given configuration input to use its metric
     * 1- It finds most similar config based on the used similarity criteria in terms of URI based similarity
     * 2- clone the best configuration
     * 3- replace the cloned best configuration with the input configuration's specifications except the metric
     * 4- return the new configuration
     * Gets an input source and target class and return the best possible config
     *
     * @param inputSourceClass Input source class
     * @param inputTargetClass Input target class
     * @param inputSourceProperties Source properties
     * @param inputTargetProperties Target properties
     */
    public Configuration runLeaveOneOut(Configuration inputConfiguration, String inputSourceClass, String inputTargetClass, Set<String> inputSourceProperties, Set<String> inputTargetProperties) {

        // setup
        ConfigAccuracy confAcc = new ConfigAccuracyWald95();
       /* ClassSimilarity classSim = new UriBasedClassSimilarity();
        PropertySimilarity propSim = new UriBasedPropertySimilarity(); */
        double bestF = 0;
        Configuration bestConfig = configurations.iterator().next();
        double similarity = -1;
        
        //iterate for other configurations
        for (Configuration configuration : configurations) {
            String configName = configuration.name.trim();
            String inputConfigName = inputConfiguration.name.trim();
            if (!configName.equalsIgnoreCase(inputConfigName)) {
                System.out.println("Processing " + configuration.name);
                // accuracy of link specification; // TODO: where to get the positive and negative examples?
                double alpha = confAcc.getAccuracy(configuration, posExamples.get(configuration), negExamples.get(configuration));

                //get the source classes of the iterated (opponent) configuration
                String sourceClass = configuration.getSource().getClassOfendpoint();
                String targetClass = configuration.getTarget().getClassOfendpoint();
                
                if (sourceClass == null) {
                    System.err.println(configuration.name + " leads to sourceClass == null");
                }
                if (targetClass == null) {
                    System.err.println(configuration.name + " leads to targetClass == null");
                }
                
                //get the similarity between the input configuration classes and the opponent configuration classes
                /*double cSSim = classSim.getSimilarity(sourceClass, inputSourceClass, configuration);
                double cTSim = classSim.getSimilarity(targetClass, inputTargetClass, configuration);*/
                boolean isSource=true;
              //get the similarity between the input configuration classes and the opponent configuration classes
                double cSSim = classSim.getSimilarity(sourceClass, inputSourceClass, inputConfiguration,configuration,isSource);
                double cTSim = classSim.getSimilarity(targetClass, inputTargetClass,inputConfiguration, configuration,!isSource);

                double factor = alpha * cSSim * cTSim;
                //double factor = cSSim * cTSim;
                if (bestF < factor) {
                    bestF = factor;
                    bestConfig = configuration;
                }
            }
        }
        System.out.println("Mapping " + inputConfiguration.name + " and " + bestConfig.name + " is " + bestF);
        //clone the best config
        bestConfig = new ConfigReader().readLimesConfig(bestConfig.name);

        // figure out best mapping properties
        //first for source        
        String sourceClass = bestConfig.getSource().getClassOfendpoint();
        String targetClass = bestConfig.getTarget().getClassOfendpoint();
        
        Map<String, String> sourcePropertiesMapping = getPropertyMap(inputSourceProperties,sourceClass, inputSourceClass, propSim, inputConfiguration,bestConfig, true);
        Map<String, String> targetPropertiesMapping = getPropertyMap(inputTargetProperties,targetClass, inputTargetClass, propSim, inputConfiguration,bestConfig, false);

        //replace ids
        bestConfig.source.id = inputConfiguration.source.id;
        bestConfig.target.id = inputConfiguration.target.id;
      //replace endpoints
        bestConfig.source.endpoint = inputConfiguration.source.endpoint;
        bestConfig.target.endpoint = inputConfiguration.target.endpoint;
        //replace graphs
        bestConfig.source.graph = inputConfiguration.source.graph;
        bestConfig.target.graph = inputConfiguration.target.graph;
        
        //replace pages
        bestConfig.source.pageSize = inputConfiguration.source.pageSize;
        bestConfig.target.pageSize = inputConfiguration.target.pageSize;
        
        //fill in prefixes
        for (String entry : inputConfiguration.source.prefixes.keySet()) {
            bestConfig.source.prefixes.put(entry, inputConfiguration.source.prefixes.get(entry));
            bestConfig.target.prefixes.put(entry, inputConfiguration.source.prefixes.get(entry));
        }
        for (String entry : inputConfiguration.target.prefixes.keySet()) {
            bestConfig.source.prefixes.put(entry, inputConfiguration.target.prefixes.get(entry));
            bestConfig.target.prefixes.put(entry, inputConfiguration.target.prefixes.get(entry));
        }
        //replace properties

        for (String property : sourcePropertiesMapping.keySet()) {

            bestConfig = Replacer.replace(bestConfig, property, inputConfiguration, sourcePropertiesMapping.get(property), true);
        }
        for (String property : targetPropertiesMapping.keySet()) {
            bestConfig = Replacer.replace(bestConfig, property, inputConfiguration, targetPropertiesMapping.get(property), false);
        }

        //replace type
       	bestConfig.source.type=inputConfiguration.source.type;
       	bestConfig.target.type=inputConfiguration.target.type;
        
        //replace class
        String r = bestConfig.source.restrictions.get(0);
        r = r.replaceAll(Pattern.quote(bestConfig.source.getClassOfendpoint()), "<" + inputSourceClass + ">");
        bestConfig.source.restrictions.set(0, r);

        r = bestConfig.target.restrictions.get(0);
        r = r.replaceAll(Pattern.quote(bestConfig.target.getClassOfendpoint()), "<" + inputTargetClass + ">");
        bestConfig.target.restrictions.set(0, r);
        resultBuffer = resultBuffer + inputConfiguration.name + "\t" + bestConfig.name + "\t"
                + bestF + "\t" + bestConfig.source.getClassOfendpoint()
                + "\t" + bestConfig.target.getClassOfendpoint() + "\t" + bestConfig.measure + "\n";

        //logger.info(resultBuffer.toString());
        return bestConfig;
    }

    /**
     *
     * @param inputProperties Properties from class whose mapping is to be
     * learned
     * @param className Class name whose mapping is to be learned
     * @param inputClassName Known class from config
     * @param propSim Property similarity computation algorithm
     * @param configuration Known configuration
     * @return Mapping from properties of known config to config to be learned
     */
//    private Map<String, String> getPropertyMap(Set<String> inputProperties, String className, String inputClassName, PropertySimilarity propSim, Configuration configuration, boolean source) {
//        Map<String, String> propertyMapping = new HashMap<>();
//        List<String> properties;
//        if (source) {
//            properties = configuration.source.properties;
//        } else {
//            properties = configuration.target.properties;
//        }
//        for (String knownProperty : properties) {
//            double maxSim = -1, sim;
//            String bestProperty = "";
//            for (String inputProperty : inputProperties) // ... call replacement function using propSim, relevantSourceProperties, configuration as input ...
//            {
//                // get rid of rdf:type
//                if (!inputProperty.endsWith("ype") && !inputProperty.endsWith("sameAs")) {
//                    sim = propSim.getSimilarity(knownProperty, inputProperty, className, inputClassName, configuration);
//                    if (sim > maxSim) {
//                        bestProperty = inputProperty;
//                        maxSim = sim;
//                    }
//                }
//            }
//            propertyMapping.put(knownProperty, bestProperty);
//        }
//        return propertyMapping;
//    }

    /**
    *
    * @param inputProperties Properties from class whose mapping is to be
    * learned
    * @param className Class name whose mapping is to be learned
    * @param inputClassName Known class from config
    * @param propSim Property similarity computation algorithm
    * @param configuration Known configuration
    * @return Mapping from properties of known config to config to be learned
    */
   private Map<String, String> getPropertyMap(Set<String> inputProperties, String className, String inputClassName, PropertySimilarity propSim, Configuration inputConfiguration,Configuration configuration, boolean source) {
       Map<String, String> propertyMapping = new HashMap<>();
       List<String> properties;
       if (source) {
           properties = configuration.source.properties;
           for (String knownProperty : properties) {
               double maxSim = -1, sim;
               String bestProperty = "";
               for (String inputProperty : inputProperties) // ... call replacement function using propSim, relevantSourceProperties, configuration as input ...
               {
                   // get rid of rdf:type
                   if (!inputProperty.endsWith("ype") && !inputProperty.endsWith("sameAs")) {
                       sim = propSim.getSimilarity(knownProperty, inputProperty, className, inputClassName, configuration,inputConfiguration,source);
                       if (sim > maxSim) {
                           bestProperty = inputProperty;
                           maxSim = sim;
                       }
                   }
               }
               propertyMapping.put(knownProperty, bestProperty);
           }
       } else {
           properties = configuration.target.properties;
           for (String knownProperty : properties) {
               double maxSim = -1, sim;
               String bestProperty = "";
               for (String inputProperty : inputProperties) // ... call replacement function using propSim, relevantSourceProperties, configuration as input ...
               {
                   // get rid of rdf:type
                   if (!inputProperty.endsWith("ype") && !inputProperty.endsWith("sameAs")) {
                       sim = propSim.getSimilarity(knownProperty, inputProperty, className, inputClassName, configuration,inputConfiguration,source);
                       if (sim > maxSim) {
                           bestProperty = inputProperty;
                           maxSim = sim;
                       }
                   }
               }
               propertyMapping.put(knownProperty, bestProperty);
           }
       }

       return propertyMapping;
   }

    /**
     * An example transfer learning task.
     *
     * @param args
     */
    public void test() {
        // input (example from https://github.com/LATC/24-7-platform/blob/master/link-specifications/dbpedia-diseasome-disease/spec.xml)
//		String inputSourceClass = "http://dbpedia.org/ontology/Disease";
//		String inputTargetClass = "http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/diseases";

        // LIMES example: we learn inactive ingredients from other similar specs
        String inputSourceClass = "http://www4.wiwiss.fu-berlin.de/dailymed/resource/dailymed/drugs";
        String sourceEndpoint = "http://lgd.aksw.org:5678/sparql";
        String inputTargetClass = "http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/drugs";
        String targetEndpoint = "http://lgd.aksw.org:5678/sparql";
        Set<Configuration> configurations = new HashSet<Configuration>();

        Map<Configuration, String> posExamples = new HashMap<Configuration, String>();
        Map<Configuration, String> negExamples = new HashMap<Configuration, String>();

        boolean limesOnly = true;

        if (limesOnly) {
            ConfigReader cr = new ConfigReader();
            Configuration cf;
            cf = cr.readLimesConfig("specs/drugbank-dailymed-activeIngredients/spec.limes.xml");
            configurations.add(cf);
            posExamples.put(cf, "specs/drugbank-dailymed-activeIngredients/positive.ttl");
            negExamples.put(cf, "specs/drugbank-dailymed-activeIngredients/negative.ttl");
            cf = cr.readLimesConfig("specs/drugbank-dailymed-activeMoiety/spec.limes.xml");
            configurations.add(cf);
            posExamples.put(cf, "specs/drugbank-dailymed-activeMoiety/positive.ttl");
            negExamples.put(cf, "specs/drugbank-dailymed-activeMoiety/negative.ttl");
            cf = cr.readLimesConfig("specs/drugbank-dailymed-ingredients/spec.limes.xml");
            configurations.add(cf);
            posExamples.put(cf, "specs/drugbank-dailymed-ingredients/positive.ttl");
            negExamples.put(cf, "specs/drugbank-dailymed-ingredients/negative.ttl");
            // we learn drugbank-dailymed-inactiveIngredients from 
            // drugbank-dailymed-activeIngredients
            // drugbank-dailymed-activeMoiety
            // drugbank-dailymed-ingredients


        } else {
            // TODO: read configurations from LATC specs	
        }

        // determine relevant properties for current link Task
        Set<String> relevantSourceProperties = SparqlUtils.getRelevantProperties(sourceEndpoint, inputSourceClass, 100, 0.8);
        Set<String> relevantTargetProperties = SparqlUtils.getRelevantProperties(targetEndpoint, inputTargetClass, 100, 0.8);;

        TransferLearner tl = new TransferLearner(configurations, posExamples, negExamples);
        //tl.run(inputSourceClass, inputTargetClass, relevantSourceProperties, relevantTargetProperties);

    }

    public Set<String> getRelevantProperties(Configuration c, boolean source) {
        Set<String> properties = new HashSet<>();
        if (source) {
            for (String p : c.source.properties) {
                properties.add(p);
            }
        } else {
            for (String p : c.target.properties) {
                properties.add(p);
            }
        }
        return properties;
    }

    /**
     * returns list of the configurations sorted by their similarity degree to the input configuration
     * @param config
     * @param inputSourceClasses
     * @param inputTargetClasses
     * @param relevantSourceProperties
     * @param relevantTargetProperties
     * @return
     */
    private List<Configuration> runLeaveOneOutOrdered(Configuration config,
            List<String> inputSourceClasses, List<String> inputTargetClasses,
            Set<String> relevantSourceProperties, Set<String> relevantTargetProperties) {

        // setup
        ConfigAccuracy confAcc = new ConfigAccuracyWald95();
        /*ClassSimilarity classSim = ClassSimilarityFactory.createClassSimilarity(ClassSimType.LABEL);//new UriBasedClassSimilarity();
        PropertySimilarity propSim = PropertySimilarityFactory.createClassSimilarity(PropertySimType.URI); //new UriBasedPropertySimilarity();
*/        List<Configuration> result = new ArrayList<Configuration>();
		int counter=0;
        for (Configuration configuration : configurations) {
            String name1 = configuration.name.trim();
            String name2 = config.name.trim();
            if (!name1.equalsIgnoreCase(name2)) {
            	System.out.println("Investigating configuration number: "+(++counter));
            	System.out.println(name2 + " is compared with "+ name1);
                //System.out.println("Processing " + configuration.name);
                // accuracy of link specification; // TODO: where to get the positive and negative examples?
                double alpha = confAcc.getAccuracy(configuration, posExamples.get(configuration), negExamples.get(configuration));

                List<String> sourceClasses = getRestrictionList(configuration.getSource().restrictions);
                List<String> targetClasses = getRestrictionList(configuration.getTarget().restrictions);
                if (sourceClasses.isEmpty()) {
                    System.err.println(configuration.name + " leads to sourceClass == null");
                }
                if (targetClasses.isEmpty()) {
                    System.err.println(configuration.name + " leads to targetClass == null");
                }

                boolean isSource=true;
                double cSSim = 0, cTSim = 0;
                //get the max similarity pair-classes as source classes
                for (String sourceClass : sourceClasses) {
                    double max = -1, sim;
                    for (String inputSourceClass : inputSourceClasses) {
                    	System.out.println(configuration.name);
                        //sim = classSim.getSimilarity(inputSourceClass,sourceClass,  configuration);
                    	sim = classSim.getSimilarity(inputSourceClass,sourceClass, config, configuration,isSource);
                        if (sim > max) {
                            max = sim;
                        }
                    }
                    cSSim = cSSim + max;
                }
                cSSim = cSSim / (double) sourceClasses.size();

                //get the max similarity pair-classes as target classes
                for (String targetClass : targetClasses) {
                    double max = -1, sim;
                    for (String inputTargetClass : inputTargetClasses) {
                        //sim = classSim.getSimilarity(inputTargetClass,targetClass,  configuration);
                        sim = classSim.getSimilarity(inputTargetClass,targetClass, config,  configuration,!isSource);
                        if (sim > max) {
                            max = sim;
                        }
                    }
                    cTSim = cTSim + max;
                }
                cTSim = cTSim / (double) targetClasses.size();
                //calculate overall similarity value between the compared configurations
                double factor = alpha * cSSim * cTSim;
                //double factor = cSSim * cTSim;
                //read the configuration into c
                Configuration c = new ConfigReader().readLimesConfig(configuration.name);
                //specify the similarity value
                c.similarity = factor;
                //add it to a list
                result.add(c);
            }
        }
        //sort the list of configurations based the calculated similarity for each descending
        Collections.sort(result);
        result = reverse(result);
        return result;
    }

    public List reverse(List x) {
        List result = new ArrayList();
        for (int i = x.size() - 1; i >= 0; i--) {
            result.add(x.get(i));
        }
        return result;
    }

    /**
     * Returns the class contained in the restriction
     *
     * @return Class label
     */
    public List<String> getRestrictionList(List<String> restrictions) {

        List<String> result = new ArrayList<String>();
        List<String> buffer = new ArrayList<String>();
        for (String rest : restrictions) {
            if (rest.contains("UNION")) {
                String split[] = rest.split("UNION");
                for (int i = 0; i < split.length; i++) {
                    String s = split[i];
                    s = s.replaceAll("<", "").replaceAll(">", "").replaceAll(Pattern.quote("."), "");
                    s = s.replaceAll(Pattern.quote("{"), "").replaceAll(Pattern.quote("}"), "");
                    s = s.split(" ")[2];
                    //s = s.substring(0, s.length() - 1);
                    result.add(s.trim());
                }
            } else {
                String s = rest;
                if(s.length()!=0)//mofeed
                {
	                s = s.replaceAll("<", "").replaceAll(">", "").replaceAll(Pattern.quote("."), "");
	                s = s.replaceAll(Pattern.quote("{"), "").replaceAll(Pattern.quote("}"), "");
	                s = s.split(" ")[2];
	                //s = s.substring(0, s.length() - 1);
                
                result.add(s.trim());
                }
            }
        }
        return result;
    }

    private static void loadParamaters(String args[])
    {
    	specsFolder = args[0];
    	classSim = ClassSimilarityFactory.createClassSimilarity(ClassSimType.valueOf(args[1]));//new UriBasedClassSimilarity();
        propSim = PropertySimilarityFactory.createPropertySimilarity(PropertySimType.valueOf(args[2])); //new UriBasedPropertySimilarity();
    }

    public static void runMiniCaching(String configFolder, int requiredSize)
    {
    	
    	TransferLearner tl=  new TransferLearner(configFolder,requiredSize);
    	/*Map<Integer, List<String>> data = DataSetUtils.getDisjointDataSetsSample("http://dbpedia.org/ontology/Drug", "http://dbpedia.org/sparql", "http://wifo5-04.informatik.uni-mannheim.de/drugbank/resource/drugbank/drugs", "http://wifo5-04.informatik.uni-mannheim.de/drugbank/sparql", "http://www.w3.org/2000/01/rdf-schema#label", 10);
    	System.out.println(data);*/
    }
    /**
     * For each link specification, the most similar link specification is found in terms of the classes similarities
     * @param folder
     * @param classSim
     * @param propSim
     */
    public static void runTransferLearning(String folder, String classSim, String propSim, int type)
    {
//   	System.out.println(SparqlUtils.formulatQuery("http://dbpedia.org/resource/Abarelix", "", "http://dbpedia.org/ontology/Drug", "http://www.w3.org/2000/01/rdf-schema#label", 0, 1000));

//      TransferLearner tl = new TransferLearner("finalSpecsTosvn/finalSpecsTosvn");
//      System.out.println(tl.runSimpleMatching(100, 0.6));
  	//Model m = SparqlUtils.getAllUriData("http://dbpedia.org/resource/Abarelix","http://dbpedia.org/sparql");
//   	Set<String> propertyValues= SparqlUtils.getUriPropertyValues("http://dbpedia.org/resource/Abarelix","http://www.w3.org/2000/01/rdf-schema#label","http://dbpedia.org/sparql");
//  	System.out.println(propertyValues);
    	
    	String[] parameter= new String[3];
    	parameter[0]= folder ; //"/media/mofeed/TOSHIBA2T/00-PhDProjects2018/TransferLearning/TransferLearningTmp/20Specs/";
    	parameter[1]=classSim; //"LABEL";
    	parameter[2]=propSim; //"URI";
    	loadParamaters(parameter);
    	
        TransferLearner tl = new TransferLearner(parameter[0], true);
        tl.displayLS(tl);
        Map<Configuration, List<Configuration>> result = null;
        if(type == 0)
        {
        	result = tl.runOrderedMatching();
            for (Configuration c : result.keySet()) {
                System.out.print(c+":-\n");
                List<Configuration> list = result.get(c);
                for (Configuration l : list) {
                    System.out.print("\n" + l);
                }
                System.out.println("\n----------------------------------------------------------------------------------------------------------------------------------");
                System.out.println();
            }
        }
        else
        {
        	Map<Configuration, BestConfigurationDO> results =  tl.runSimpleMatching(200, 0.6);
            for (Configuration c : results.keySet()) {
                System.out.print(c.getName()+":-\t"+results.get(c).toString());

                System.out.println("\n----------------------------------------------------------------------------------------------------------------------------------");
                System.out.println();
            }
        }
       

        
       /* Map<Configuration, Double> simpleMatchingResults = tl.runSimpleMatching(100, 10);
        for (Configuration conf : simpleMatchingResults.keySet()) {
			System.out.println(conf.name+":"+simpleMatchingResults.get(conf));
		}*/
    }
    public static void sparqlToSerialize(String sparqlFile, String endpoint){
    	List<String> queryLines = org.aksw.saim.io.ReadFile.readFile(sparqlFile);
    	DataSetUtils.sparqlQueryToSerialize(queryLines, endpoint);
    }
    public static void displayLS(TransferLearner tl)
    {
    	for (Configuration configuration : tl.configurations) {
			System.out.println(configuration);
			System.out.println(configuration.getSource().endpoint);
			System.out.println(configuration.getTarget().endpoint);
			System.out.println(configuration.getSource().getClassRestriction());
			System.out.println(configuration.getTarget().getClassRestriction());
			System.out.println(tl.sourceProperties.get(configuration));
			System.out.println(tl.targetProperties.get(configuration));
			System.out.println(configuration.getMeasure());
			System.out.println("---------------------------------------------------------------");
		}
    	
    }
}
