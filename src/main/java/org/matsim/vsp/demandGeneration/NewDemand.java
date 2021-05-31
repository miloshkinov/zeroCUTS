package org.matsim.vsp.demandGeneration;

import org.matsim.contrib.freight.carrier.TimeWindow;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class NewDemand {

	private static Logger log = LogManager.getLogger(NewDemand.class);

	private String name;
	private String[] areasForTheDemand;
	private int demandToDistribute;
	private int numberOfJobs;
	private int serviceTimePerUnit;
	private TimeWindow serviceTimeWindow;
	private Double shareOfPopulationWithThisDemand;

	NewDemand(String name, String[] areasForTheDemand, int demandToDistribute, int numberOfJobs,
			int serviceTimePerUnit, TimeWindow serviceTimeWindow, double shareOfPopulationWithThisDemand) {
		this.setId(name);
		this.setAreasForTheDemand(areasForTheDemand);
		this.setDemandToDistribute(demandToDistribute);
		this.setNumberOfJobs(numberOfJobs);
		this.setServiceTimePerUnit(serviceTimePerUnit);
		this.setServiceTimeWindow(serviceTimeWindow);
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

	public int getServiceTimePerUnit() {
		return serviceTimePerUnit;
	}

	public void setServiceTimePerUnit(int serviceTimePerUnit) {
		this.serviceTimePerUnit = serviceTimePerUnit;
	}

	public String[] getAreasForTheDemand() {
		return areasForTheDemand;
	}

	public void setAreasForTheDemand(String[] areasForTheDemand) {
		this.areasForTheDemand = areasForTheDemand;
	}

	public TimeWindow getServiceTimeWindow() {
		return serviceTimeWindow;
	}

	public void setServiceTimeWindow(TimeWindow serviceTimeWindow) {
		this.serviceTimeWindow = serviceTimeWindow;
	}

	public double getShareOfPopulationWithThisDemand() {
		return shareOfPopulationWithThisDemand;
	}

	public void setShareOfPopulationWithThisDemand(double shareOfPopulationWithThisDemand) {
		this.shareOfPopulationWithThisDemand = shareOfPopulationWithThisDemand;
	}
}
