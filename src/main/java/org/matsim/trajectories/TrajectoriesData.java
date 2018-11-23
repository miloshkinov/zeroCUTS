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
  
class TrajectoriesData {

	double timeOnTravel;
	double timeVehicleInTraffic;
	double distanceRouteTravelled;
	int numOfActities;
	//TODO: Continue
	
	void reset(){
		this.timeOnTravel = 0.0;
		this.timeVehicleInTraffic = 0.0;
		this.distanceRouteTravelled = 0.0;
		this.numOfActities = 0;
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

	@Override
	public String toString() {
		return "TrajectoriesData [timeOnTravel=" + timeOnTravel + ", timeVehicleInTraffic=" + timeVehicleInTraffic
				+ ", distanceTravelled=" + distanceRouteTravelled + ", numOfActities=" + numOfActities + "]";
	}
	
	
}
