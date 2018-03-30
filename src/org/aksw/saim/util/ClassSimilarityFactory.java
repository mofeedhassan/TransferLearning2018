package org.aksw.saim.util;

import org.aksw.saim.transfer.classes.ClassSimilarity;
import org.aksw.saim.transfer.classes.LabelBasedClassSimilarity;
import org.aksw.saim.transfer.classes.SamplingBasedClassSimilarity;
import org.aksw.saim.transfer.classes.UriBasedClassSimilarity;

public class ClassSimilarityFactory {
	public static ClassSimilarity createClassSimilarity(ClassSimType classSimType)
	{
		if(classSimType.equals(ClassSimType.SAMPLE))
			return new SamplingBasedClassSimilarity();
		else if (classSimType.equals(ClassSimType.LABEL))
			return new LabelBasedClassSimilarity();
		else //default uri
			return new UriBasedClassSimilarity();
	}
}
