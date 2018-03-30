/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.saim.util;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.HybridCache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.execution.ExecutionEngine;
import de.uni_leipzig.simba.execution.Instruction;
import de.uni_leipzig.simba.execution.Instruction.Command;
import de.uni_leipzig.simba.filter.LinearFilter;
import de.uni_leipzig.simba.io.ConfigReader;
import de.uni_leipzig.simba.io.KBInfo;
import de.uni_leipzig.simba.mapper.SetConstraintsMapper;
import de.uni_leipzig.simba.mapper.SetConstraintsMapperFactory;
import org.aksw.saim.transfer.TransferLearner;
import org.aksw.saim.transfer.config.Configuration;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ngonga
 */
public class Execution {

    Logger logger = LoggerFactory.getLogger(Execution.class);

    public static Mapping execute(Cache source, Cache target, String measure, double threshold) {
        System.out.println(measure);
        if(!measure.contains("("))
            measure = measure +"(a.p, b.p)";
        Instruction inst = new Instruction(Command.RUN, measure, threshold + "", -1, -1, -1);
        ExecutionEngine ee = new ExecutionEngine(source, target, "?a", "?b");
        return ee.executeRun(inst);
    }

    public Mapping executeComplex(Configuration c) {
        long startTime = System.currentTimeMillis();
        //0. configure logger
        try {
            PatternLayout layout = new PatternLayout("%d{dd.MM.yyyy HH:mm:ss} %-5p [%t] %l: %m%n");
            } catch (Exception e) {
            logger.warn("Exception creating file appender.");
        }

        logger.isDebugEnabled();

        logger.info(c.getSource().toString());
        logger.info(c.getTarget().toString());
        //System.exit(1);

        //2. Fill caches using the query module
        //2.1 First sourceInfo
        logger.info("Loading source data ..."+c.getSource().id);
        HybridCache source = new HybridCache();
        source = HybridCache.getData(c.getSource());

        //2.2 Then targetInfo
        logger.info("Loading target data ..."+c.getTarget().id);
        HybridCache target = new HybridCache();
        target = HybridCache.getData(c.getTarget());
        SetConstraintsMapper mapper = SetConstraintsMapperFactory.getMapper("simple",
                c.getSource(), c.getTarget(), source, target, new LinearFilter(), 4);
        //cr.sourceInfo, cr.targetInfo, sourceInfo, targetInfo, new PPJoinMapper(), new LinearFilter());
        logger.info("Getting links ...");
        long time = System.currentTimeMillis();
        Mapping mapping = mapper.getLinks(c.getMeasure(), c.getThreshold());
        logger.info("Got links in " + (System.currentTimeMillis() - time) + "ms.");
        //get Writer ready
        return mapping;
    }
}
