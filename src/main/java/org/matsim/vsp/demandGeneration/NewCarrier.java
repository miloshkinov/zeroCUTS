package org.matsim.vsp.demandGeneration;

import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class NewCarrier {

	private static Logger log = LogManager.getLogger(NewCarrier.class);

	private String name;
	private String[] vehilceTypes;
	private String[] vehicleDepots;
	private FleetSize fleetSize;
	private int carrierStartTime;
	private int carrierEndTime;
	private String[] areasForTheDemand;
	private int demandToDistribute;
	private int numberOfJobs;
	private int serviceTimePerUnit;
	private TimeWindow serviceTimeWindow;

	NewCarrier(String name, String[] vehilceTypes, String[] vehicleDepots, FleetSize fleetSize, int carrierStartTime,
			int carrierEndTime, String[] areasForTheDemand, int demandToDistribute, int numberOfJobs,
			int serviceTimePerUnit, TimeWindow serviceTimeWindow) {
		this.setId(name);
		this.setVehicleTypes(vehilceTypes);
		this.setVehicleDepots(vehicleDepots);
		this.setFleetSize(fleetSize);
		this.setCarrierStartTime(carrierStartTime);
		this.setCarrierEndTime(carrierEndTime);
		this.setAreasForTheDemand(areasForTheDemand);
		this.setDemandToDistribute(demandToDistribute);
		this.setNumberOfJobs(numberOfJobs);
		this.setServiceTimePerUnit(serviceTimePerUnit);
		this.setServiceTimeWindow(serviceTimeWindow);
	}

	public NewCarrier(String name, String[] vehilceTypes, String[] vehicleDepots, FleetSize fleetSize,
			int carrierStartTime, int carrierEndTime) {
		this.setId(name);
		this.setVehicleTypes(vehilceTypes);
		this.setVehicleDepots(vehicleDepots);
		this.setFleetSize(fleetSize);
		this.setCarrierStartTime(carrierStartTime);
		this.setCarrierEndTime(carrierEndTime);
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

	public int getCarrierStartTime() {
		return carrierStartTime;
	}

	public void setCarrierStartTime(int carrierStartTime) {
		this.carrierStartTime = carrierStartTime;
	}

	public int getCarrierEndTime() {
		return carrierEndTime;
	}

	public void setCarrierEndTime(int carrierEndTime) {
		this.carrierEndTime = carrierEndTime;
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
}
