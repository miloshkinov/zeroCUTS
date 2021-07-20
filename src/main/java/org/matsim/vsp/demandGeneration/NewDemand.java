package org.matsim.vsp.demandGeneration;

import org.matsim.contrib.freight.carrier.TimeWindow;

final class NewDemand {

	private String name;
	private String[] areasForTheDemand;
	private int demandToDistribute;
	private int numberOfJobs;
	private int firstJobTimePerUnit;
	private TimeWindow firstJobTimeWindow;
	private Double shareOfPopulationWithThisDemand;

	NewDemand(String name, String[] areasForTheDemand, int demandToDistribute, int numberOfJobs,
			int firstJobTimePerUnit, TimeWindow firstJobTimeWindow, double shareOfPopulationWithThisDemand) {
		this.setId(name);
		this.setAreasForTheDemand(areasForTheDemand);
		this.setDemandToDistribute(demandToDistribute);
		this.setNumberOfJobs(numberOfJobs);
		this.setFirstJobTimePerUnit(firstJobTimePerUnit);
		this.setFirstJobTimeWindow(firstJobTimeWindow);
		this.setShareOfPopulationWithThisDemand(shareOfPopulationWithThisDemand);
	}

	public String getName() {
		return name;
	}

	void setId(String name) {
		this.name = name;
	}

	public int getDemandToDistribute() {
		return demandToDistribute;
	}

	public void setDemandToDistribute(int demandToDistribute) {
		this.demandToDistribute = demandToDistribute;
	}

	public int getNumberOfJobs() {
		return numberOfJobs;
	}

	public void setNumberOfJobs(int numberOfJobs) {
		this.numberOfJobs = numberOfJobs;
	}

	public int getFirstJobTimePerUnit() {
		return firstJobTimePerUnit;
	}

	public void setFirstJobTimePerUnit(int firstJobTimePerUnit) {
		this.firstJobTimePerUnit = firstJobTimePerUnit;
	}

	public String[] getAreasForTheDemand() {
		return areasForTheDemand;
	}

	public void setAreasForTheDemand(String[] areasForTheDemand) {
		this.areasForTheDemand = areasForTheDemand;
	}

	public TimeWindow getFirstJobTimeWindow() {
		return firstJobTimeWindow;
	}

	public void setFirstJobTimeWindow(TimeWindow firstJobTimeWindow) {
		this.firstJobTimeWindow = firstJobTimeWindow;
	}

	public double getShareOfPopulationWithThisDemand() {
		return shareOfPopulationWithThisDemand;
	}

	public void setShareOfPopulationWithThisDemand(double shareOfPopulationWithThisDemand) {
		this.shareOfPopulationWithThisDemand = shareOfPopulationWithThisDemand;
	}
}
