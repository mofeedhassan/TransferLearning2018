package org.aksw.saim.test;

import java.util.List;
import java.util.Map;

import org.aksw.saim.io.ReadFile;
import org.aksw.saim.util.SparqlUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;

public class Main {
	static int count=0;
	public static void main(String[] args)
	{
		Map<Integer, List<String>> datasets = ReadFile.readLinksFile("/media/mofeed/TOSHIBA2T/00-PhDProjects2018/TransferLearning/dbpedia-drugbank-links");
		
		Model dataset1= ModelFactory.createDefaultModel();
		Model dataset2= ModelFactory.createDefaultModel();
		Map<String, Map<String, String>> m=null;
		
//		System.out.println("Downloading dataset 1");
//
//			m = SparqlUtils.getAllDatasetData(datasets.get(1),"http://dbpedia.org/sparql");
//
//		System.out.println(m);
//		
//		System.out.println("Downloading dataset 2");
//		m = SparqlUtils.getAllDatasetData(datasets.get(2), "http://dbpedia.org/sparql");
//
//		System.out.println(m);
//		
		
		Model model = ModelFactory.createDefaultModel();
		System.out.println("Downloading dataset 1");
		for (String uri : datasets.get(1)) {
			model = SparqlUtils.getAllDataOfUri(uri, "http://dbpedia.org/sparql");
			//dataset1.add(m);
			progressBar();
		}
		System.out.println(dataset1);
		
		System.out.println("Downloading dataset 2");
		for (String uri : datasets.get(2)) {
			model = SparqlUtils.getAllDataOfUri(uri,"http://wifo5-04.informatik.uni-mannheim.de/drugbank/sparql");
			//dataset2.add(m);
			progressBar();
		}
		System.out.println(dataset2);

	}
	private static void progressBar()
	{
		if(count%20==0)
			System.out.println();
		System.out.print(".");
		count++;
	}

}
