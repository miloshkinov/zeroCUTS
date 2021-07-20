package org.matsim.vsp.demandGeneration;

import org.matsim.contrib.freight.carrier.TimeWindow;

final class NewDemand {

	private String name;
	private String[] areasForTheDemand;
	private Integer demandToDistribute;
	private Integer numberOfJobs;
	private Double shareOfPopulationWithThisDemand;
	private Integer firstJobTimePerUnit;
	private TimeWindow firstJobTimeWindow;
	private Integer secondJobTimePerUnit;
	private TimeWindow secondJobTimeWindow;


	NewDemand(String name, String[] areasForTheDemand, Integer demandToDistribute, Integer numberOfJobs,
			Double shareOfPopulationWithThisDemand, Integer firstJobTimePerUnit, TimeWindow firstJobTimeWindow, Integer secondJobTimePerUnit, TimeWindow secondJobTimeWindow) {
		this.setId(name);
		this.setAreasForTheDemand(areasForTheDemand);
		this.setDemandToDistribute(demandToDistribute);
		this.setNumberOfJobs(numberOfJobs);
		this.setShareOfPopulationWithThisDemand(shareOfPopulationWithThisDemand);
		this.setFirstJobTimePerUnit(firstJobTimePerUnit);
		this.setFirstJobTimeWindow(firstJobTimeWindow);
		this.setSecondJobTimePerUnit(secondJobTimePerUnit);
		this.setSecondJobTimeWindow(secondJobTimeWindow);
	}

	public String getName() {
		return name;
	}

	void setId(String name) {
		this.name = name;
	}

	public Integer getDemandToDistribute() {
		return demandToDistribute;
	}

	public void setDemandToDistribute(Integer demandToDistribute) {
		this.demandToDistribute = demandToDistribute;
	}

	public Integer getNumberOfJobs() {
		return numberOfJobs;
	}

	public void setNumberOfJobs(Integer numberOfJobs) {
		this.numberOfJobs = numberOfJobs;
	}

	public Integer getFirstJobTimePerUnit() {
		return firstJobTimePerUnit;
	}

	public void setFirstJobTimePerUnit(Integer firstJobTimePerUnit) {
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

	public Double getShareOfPopulationWithThisDemand() {
		return shareOfPopulationWithThisDemand;
	}

	public void setShareOfPopulationWithThisDemand(Double shareOfPopulationWithThisDemand) {
		this.shareOfPopulationWithThisDemand = shareOfPopulationWithThisDemand;
	}

	public TimeWindow getSecondJobTimeWindow() {
		return secondJobTimeWindow;
	}

	public void setSecondJobTimeWindow(TimeWindow secondJobTimeWindow) {
		this.secondJobTimeWindow = secondJobTimeWindow;
	}

	public Integer getSecondJobTimePerUnit() {
		return secondJobTimePerUnit;
	}

	public void setSecondJobTimePerUnit(Integer secondJobTimePerUnit) {
		this.secondJobTimePerUnit = secondJobTimePerUnit;
	}
}
