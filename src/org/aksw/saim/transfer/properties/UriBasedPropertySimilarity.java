/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.saim.transfer.properties;

import org.aksw.saim.transfer.config.Configuration;
import uk.ac.shef.wit.simmetrics.similaritymetrics.QGramsDistance;

/**
 *
 * @author ngonga
 */
public class UriBasedPropertySimilarity implements PropertySimilarity{
    
    @Override
    public double getSimilarity(String property1, String property2, String class1, String class2, Configuration config) {
        String p1 = cleanUri(property1);
        String p2 = cleanUri(property2);
        return new QGramsDistance().getSimilarity(p1, p2);
    }

	
    public String cleanUri(String propertyLabel) {
        if (propertyLabel.contains("/")) {
            propertyLabel = propertyLabel.substring(propertyLabel.indexOf("/") + 1);
        }
        if (propertyLabel.contains("#")) {
            propertyLabel = propertyLabel.substring(propertyLabel.indexOf("#") + 1);
        }
        return propertyLabel;
    }


	@Override
	public double getSimilarity(String property, String inutProperty, String class1, String inputClass,
			Configuration configuration, Configuration inputConfiguration, boolean isSource) {
		// TODO Auto-generated method stub
		return 0;
	}


}
