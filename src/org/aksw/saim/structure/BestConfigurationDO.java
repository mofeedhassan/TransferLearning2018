package org.aksw.saim.structure;

import org.aksw.saim.transfer.config.Configuration;

public class BestConfigurationDO {
	Configuration bestconfig=null;
	double precision=0,recal=0,fscore=0;
	
	/**
	 * @param bestconfig
	 * @param precision
	 * @param recal
	 * @param fscore
	 */
	public BestConfigurationDO(Configuration bestconfig, double precision, double recal, double fscore) {
		super();
		this.bestconfig = bestconfig;
		this.precision = precision;
		this.recal = recal;
		this.fscore = fscore;
	}

	public Configuration getBestconfig() {
		return bestconfig;
	}
	public void setBestconfig(Configuration bestconfig) {
		this.bestconfig = bestconfig;
	}
	public double getPrecision() {
		return precision;
	}
	public void setPrecision(double precision) {
		this.precision = precision;
	}
	public double getRecal() {
		return recal;
	}
	public void setRecal(double recal) {
		this.recal = recal;
	}
	public double getFscore() {
		return fscore;
	}
	public void setFscore(double fscore) {
		this.fscore = fscore;
	}

	@Override
	public String toString() {
		return "[bestconfig=" + bestconfig.name + ", precision=" + precision + ", recal=" + recal+ ", fscore=" + fscore + "]";
	}
	

}
