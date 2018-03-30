package org.aksw.saim.transfer.properties;

import org.aksw.saim.transfer.config.Configuration;

/**
 * Measures similarity between classes.
 * 
 * @author Jens Lehmann
 * @author Axel Ngonga
 */
public interface PropertySimilarity {

	public double getSimilarity(String property1, String property2, String class1, String class2, Configuration config);
	//public double getSimilarity(String property1, String property2, String class1, String class2, Configuration config1,Configuration config2);
	public double getSimilarity(String property, String inutProperty, String class1, String inputClass,Configuration configuration, Configuration inputConfiguration, boolean isSource);

	
}
