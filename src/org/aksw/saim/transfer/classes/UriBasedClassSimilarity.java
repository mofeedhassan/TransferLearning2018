/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.saim.transfer.classes;

import org.aksw.saim.transfer.config.Configuration;
import uk.ac.shef.wit.simmetrics.similaritymetrics.QGramsDistance;

/**
 *
 * @author ngonga
 */
public class UriBasedClassSimilarity implements ClassSimilarity {

    @Override
    public double getSimilarity(String class1, String class2, Configuration config2) {
        if (class1 == null || class2 == null) {
            System.err.println("One of " + class1 + " and " + class2 + " is " + null);
            return 0D;
        }
        class1 = cleanUri(class1);
        class2 = cleanUri(class2);

        return new QGramsDistance().getSimilarity(class1, class2);
    }
	@Override
	public double getSimilarity(String class2, String class1, Configuration config1, Configuration config2,boolean isSource) {
        if (class1 == null || class2 == null) {
            System.err.println("One of " + class1 + " and " + class2 + " is " + null);
            return 0D;
        }
        class1 = cleanUri(class1);
        class2 = cleanUri(class2);

        return new QGramsDistance().getSimilarity(class1, class2);
	}

    public String cleanUri(String classLabel) {
        if (classLabel.contains("/")) {
            classLabel = classLabel.substring(classLabel.indexOf("/") + 1);
        }
        if (classLabel.contains("#")) {
            classLabel = classLabel.substring(classLabel.indexOf("#") + 1);
        }
        if (classLabel.contains(":")) {
            classLabel = classLabel.substring(classLabel.indexOf(":") + 1);
        }
        return classLabel;
    }


}
