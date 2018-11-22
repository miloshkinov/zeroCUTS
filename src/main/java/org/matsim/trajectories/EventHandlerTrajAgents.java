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

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.population.Person;

class EventHandlerTrajAgents implements 	PersonArrivalEventHandler,
PersonDepartureEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

	TreeMap<Id<Person>, TrajectoriesData > persons2trajectorities= new TreeMap<Id<Person>, TrajectoriesData>();
	//	private double timePersonOnTravel = 0.0;
	//	private double timeVehicleInTraffic = 0.0 ;
	//	// NOTE: drivers depart, enter vehicles, eventually enter traffic, drive to destination, leave traffic, leave vehicle, arrive.
	//	// In consequence, the time between departure and arrival of the person may be longer than the time
	//	// between the vehicle entering and leaving the traffic (network).

	TreeMap<Id<Person>, TrajectoriesData> getDriversData() {
		return this.persons2trajectorities;
	}

	@Override
	public void reset(int iteration) {
		this.persons2trajectorities.clear();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		//		this.timePersonOnTravel += event.getTime();
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		//		this.timePersonOnTravel -= event.getTime();
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		//		this.timeVehicleInTraffic += event.getTime() ;
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		//		this.timeVehicleInTraffic -= event.getTime() ;
	}

}
