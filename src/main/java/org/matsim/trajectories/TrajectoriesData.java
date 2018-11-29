/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
  
package org.matsim.trajectories;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.VehicleType;

class TrajectoriesData {

	private Id<VehicleType> vehicleTypeId; 
	private double timeOnTravel = 0;
	private double timeVehicleInTraffic = 0;
	private double distanceRouteTravelled = 0;
	private int numOfActities  = 0;;
	private boolean aborted = false;
	private double HC = 0;
	private double PM = 0;
	private double NO2 = 0 ;
	private double NMHC = 0 ;
	private double NOX  = 0 ;
	private double XO = 0 ;
	private double FC = 0 ;
	private double CO2_TOTAL = 0 ;
	private double SO2 = 0 ;
	//TODO: Continue
	
	void reset(){
		this.vehicleTypeId = null;
		this.timeOnTravel = 0.0;
		this.timeVehicleInTraffic = 0.0;
		this.distanceRouteTravelled = 0.0;
		this.numOfActities = 0;
		this.aborted = false;
	}

	Id<VehicleType> getVehicleTypeId() {
		return vehicleTypeId;
	}

	void setVehicleTypeId(Id<VehicleType> vehicleTypeId) {
		this.vehicleTypeId = vehicleTypeId;
	}
	double getTimeOnTravel() {
		return timeOnTravel;
	}

	void setTimeOnTravel(double timeOnTravel) {
		this.timeOnTravel = timeOnTravel;
	}

	double getTimeVehicleInTraffic() {
		return timeVehicleInTraffic;
	}

	void setTimeVehicleInTraffic(double timeVehicleInTraffic) {
		this.timeVehicleInTraffic = timeVehicleInTraffic;
	}

	double getDistanceRouteTravelled() {
		return distanceRouteTravelled;
	}

	void setDistanceRouteTravelled(double distanceTravelled) {
		this.distanceRouteTravelled = distanceTravelled;
	}

	int getNumOfActities() {
		return numOfActities;
	}

	void setNumOfActities(int numOfActities) {
		this.numOfActities = numOfActities;
	}

	boolean isAborted() {
		return aborted;
	}

	void setAborted(boolean aborted) {
		this.aborted = aborted;
	}

	double getHC() {
		return HC;
	}

	void setHC(double hC) {
		HC = hC;
	}

	double getPM() {
		return PM;
	}

	void setPM(double pM) {
		PM = pM;
	}

	double getNO2() {
		return NO2;
	}

	void setNO2(double nO2) {
		NO2 = nO2;
	}

	double getNMHC() {
		return NMHC;
	}

	void setNMHC(double nMHC) {
		NMHC = nMHC;
	}

	double getNOX() {
		return NOX;
	}

	void setNOX(double nOX) {
		NOX = nOX;
	}

	double getXO() {
		return XO;
	}

	void setXO(double xO) {
		XO = xO;
	}

	double getFC() {
		return FC;
	}

	void setFC(double fC) {
		FC = fC;
	}

	double getCO2_TOTAL() {
		return CO2_TOTAL;
	}

	void setCO2_TOTAL(double cO2_TOTAL) {
		CO2_TOTAL = cO2_TOTAL;
	}

	double getSO2() {
		return SO2;
	}

	void setSO2(double sO2) {
		SO2 = sO2;
	}

	@Override
	public String toString() {
		return "TrajectoriesData [vehicleTypeId=" + vehicleTypeId + ", timeOnTravel=" + timeOnTravel
				+ ", timeVehicleInTraffic=" + timeVehicleInTraffic + ", distanceRouteTravelled="
				+ distanceRouteTravelled + ", numOfActities=" + numOfActities + ", aborted=" + aborted + ", HC=" + HC
				+ ", PM=" + PM + ", NO2=" + NO2 + ", NMHC=" + NMHC + ", NOX=" + NOX + ", XO=" + XO + ", FC=" + FC
				+ ", CO2_TOTAL=" + CO2_TOTAL + ", SO2=" + SO2 + "]";
	}
	
	
}
