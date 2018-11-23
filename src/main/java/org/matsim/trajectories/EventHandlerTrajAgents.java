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

import java.io.File;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.population.Person;

/**
 * @author kturner
 *
 * TODO: distances -> need network available
 * TODO: other trajectories
 * TODO: write data to file (kind of file?) instead of console;
 */
class EventHandlerTrajAgents implements 	PersonArrivalEventHandler,
PersonDepartureEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, LinkEnterEventHandler {

	private TreeMap<Id<Person>, TrajectoriesData > persons2trajectorities= new TreeMap<Id<Person>, TrajectoriesData>();

	void writeDriversDataToConsole() {
		for (Id<Person> personId: persons2trajectorities.keySet()) {
			System.out.println("Agent: " + personId.toString() + " : " + persons2trajectorities.get(personId).toString());
		}
	}
	
	void writeDriversDataToFile(File filename) throws Exception {
		throw new Exception("Not implemented");
	}

	@Override
	public void reset(int iteration) {
		this.persons2trajectorities.clear();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (persons2trajectorities.containsKey(event.getPersonId())) {
			double newTimeOnTravel = persons2trajectorities.get(event.getPersonId()).getTimeOnTravel() + event.getTime();
			persons2trajectorities.get(event.getPersonId()).setTimeOnTravel(newTimeOnTravel);
		} else {
			persons2trajectorities.put(event.getPersonId(), new TrajectoriesData());
			persons2trajectorities.get(event.getPersonId()).setTimeOnTravel(event.getTime());
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (persons2trajectorities.containsKey(event.getPersonId())) {
			double newTimeOnTravel = persons2trajectorities.get(event.getPersonId()).getTimeOnTravel() - event.getTime();
			persons2trajectorities.get(event.getPersonId()).setTimeOnTravel(newTimeOnTravel);
		} else {
			persons2trajectorities.put(event.getPersonId(), new TrajectoriesData());
			persons2trajectorities.get(event.getPersonId()).setTimeOnTravel(-event.getTime());
		}
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		if (persons2trajectorities.containsKey(event.getPersonId())) {
			double newTimeVehicleInTraffic = persons2trajectorities.get(event.getPersonId()).getTimeVehicleInTraffic() + event.getTime();
			persons2trajectorities.get(event.getPersonId()).setTimeVehicleInTraffic(newTimeVehicleInTraffic);
		} else {
			persons2trajectorities.put(event.getPersonId(), new TrajectoriesData());
			persons2trajectorities.get(event.getPersonId()).setTimeVehicleInTraffic(event.getTime());
		}
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		if (persons2trajectorities.containsKey(event.getPersonId())) {
			double newTimeVehicleInTraffic = persons2trajectorities.get(event.getPersonId()).getTimeVehicleInTraffic() - event.getTime();
			persons2trajectorities.get(event.getPersonId()).setTimeVehicleInTraffic(newTimeVehicleInTraffic);
		} else {
			persons2trajectorities.put(event.getPersonId(), new TrajectoriesData());
			persons2trajectorities.get(event.getPersonId()).setTimeVehicleInTraffic(-event.getTime());
		}
	}

	//TODO: Warum hier Cast und bei den anderen Nicht?
	//TODO: Inhalt implementieren.
	@Override
	public void handleEvent(LinkEnterEvent event) {
//		if (persons2trajectorities.containsKey(event.getPersonId())) {
//			double newTimeVehicleInTraffic = persons2trajectorities.get(event.getPersonId()).getTimeVehicleInTraffic() - event.getTime();
//			persons2trajectorities.get(event.getPersonId()).setTimeVehicleInTraffic(newTimeVehicleInTraffic);
//		} else {
//			persons2trajectorities.put(event.getPersonId(), new TrajectoriesData());
//			persons2trajectorities.get(event.getPersonId()).setTimeVehicleInTraffic(-event.getTime());
//		}
		
	}

	

}
