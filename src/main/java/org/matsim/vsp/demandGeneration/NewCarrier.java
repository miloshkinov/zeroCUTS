package org.matsim.vsp.demandGeneration;

import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class NewCarrier {

	private static Logger log = LogManager.getLogger(NewCarrier.class);

	private String name;
	private String[] vehilceTypes;
	private int numberOfDepotsPerType;
	private String[] vehicleDepots;
	private String[] areaOfAdditonalDepots;
	private FleetSize fleetSize;
	private int vehicleStartTime;
	private int vehicleEndTime;
	private int jspritIterations;
	private String[] areasForTheDemand;
	private int demandToDistribute;
	private int numberOfJobs;
	private int serviceTimePerUnit;
	private TimeWindow serviceTimeWindow;

	NewCarrier(String name, String[] vehilceTypes, int numberOfDepotsPerType, String[] vehicleDepots, String[] areaOfAdditonalDepots, FleetSize fleetSize, int vehicleStartTime,
			int vehicleEndTime, int jspritIterations, String[] areasForTheDemand, int demandToDistribute, int numberOfJobs,
			int serviceTimePerUnit, TimeWindow serviceTimeWindow) {
		this.setId(name);
		this.setVehicleTypes(vehilceTypes);
		this.setVehicleDepots(vehicleDepots);
		this.setNumberOfDepotsPerType(numberOfDepotsPerType);
		this.setAreaOfAdditonalDepots(areaOfAdditonalDepots);
		this.setFleetSize(fleetSize);
		this.setVehicleStartTime(vehicleStartTime);
		this.setVehicleEndTime(vehicleEndTime);
		this.setJspritIterations(jspritIterations);
		this.setAreasForTheDemand(areasForTheDemand);
		this.setDemandToDistribute(demandToDistribute);
		this.setNumberOfJobs(numberOfJobs);
		this.setServiceTimePerUnit(serviceTimePerUnit);
		this.setServiceTimeWindow(serviceTimeWindow);
	}

	public NewCarrier(String name, String[] vehilceTypes, int numberOfDepotsPerType, String[] vehicleDepots, String[] areaOfAdditonalDepots, FleetSize fleetSize,
			int vehicleStartTime, int vehicleEndTime, int jspritIterations) {
		this.setId(name);
		this.setVehicleTypes(vehilceTypes);
		this.setVehicleDepots(vehicleDepots);
		this.setAreaOfAdditonalDepots(areaOfAdditonalDepots);
		this.setJspritIterations(jspritIterations);
		this.setFleetSize(fleetSize);
		this.setVehicleStartTime(vehicleStartTime);
		this.setVehicleEndTime(vehicleEndTime);
	}

	public String getName() {
		return name;
	}

	void setId(String name) {
		this.name = name;
	}

	public String[] getVehicleTypes() {
		return vehilceTypes;
	}

	void setVehicleTypes(String[] vehicleTypes) {
		this.vehilceTypes = vehicleTypes;
	}

	public String[] getVehicleDepots() {
		return vehicleDepots;
	}

	public void setVehicleDepots(String[] vehicleDepots) {
		this.vehicleDepots = vehicleDepots;
	}

	public FleetSize getFleetSize() {
		return fleetSize;
	}

	public void setFleetSize(FleetSize fleetSize) {
		this.fleetSize = fleetSize;
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

	public int getVehicleStartTime() {
		return vehicleStartTime;
	}

	public void setVehicleStartTime(int carrierStartTime) {
		this.vehicleStartTime = carrierStartTime;
	}

	public int getVehicleEndTime() {
		return vehicleEndTime;
	}

	public void setVehicleEndTime(int vehicleEndTime) {
		this.vehicleEndTime = vehicleEndTime;
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

	public int getJspritIterations() {
		return jspritIterations;
	}

	public void setJspritIterations(int jspritIterations) {
		this.jspritIterations = jspritIterations;
	}

	public int getNumberOfDepotsPerType() {
		return numberOfDepotsPerType;
	}

	public void setNumberOfDepotsPerType(int numberOfDepotsPerType) {
		this.numberOfDepotsPerType = numberOfDepotsPerType;
	}

	public String[] getAreaOfAdditonalDepots() {
		return areaOfAdditonalDepots;
	}

	public void setAreaOfAdditonalDepots(String[] areaOfAdditonalDepots) {
		this.areaOfAdditonalDepots = areaOfAdditonalDepots;
	}
}
