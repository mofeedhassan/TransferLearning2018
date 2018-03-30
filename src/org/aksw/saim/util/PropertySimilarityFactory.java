package org.aksw.saim.util;

import org.aksw.saim.transfer.classes.ClassSimilarity;
import org.aksw.saim.transfer.classes.LabelBasedClassSimilarity;
import org.aksw.saim.transfer.classes.SamplingBasedClassSimilarity;
import org.aksw.saim.transfer.classes.UriBasedClassSimilarity;
import org.aksw.saim.transfer.properties.PropertySimilarity;
import org.aksw.saim.transfer.properties.SamplingBasedPropertySimilarity;
import org.aksw.saim.transfer.properties.UriBasedPropertySimilarity;

public class PropertySimilarityFactory {
	public static PropertySimilarity createPropertySimilarity(PropertySimType classSimType)
	{
		if(classSimType.equals(PropertySimType.SAMPLE))
			return new SamplingBasedPropertySimilarity();
		else //default uri
			return new UriBasedPropertySimilarity();
	}
}
