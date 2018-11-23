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
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.core.events.handler.BasicEventHandler;

/**
 * @author kturner
 *
 * TODO: distances -> need network available
 * TODO: other trajectories
 * TODO: write data to file (kind of file?) instead of console;
 */
class EventHandlerTrajAgents implements
PersonDepartureEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, LinkEnterEventHandler, BasicEventHandler{

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
	public void handleEvent( Event event ) {
		if ( event instanceof PersonArrivalEvent ){
			PersonArrivalEvent de = (PersonArrivalEvent) event;
			if( persons2trajectorities.containsKey( de.getPersonId() ) ){
				double newTimeOnTravel = persons2trajectorities.get( de.getPersonId() ).getTimeOnTravel() + de.getTime();
				persons2trajectorities.get( de.getPersonId() ).setTimeOnTravel( newTimeOnTravel );
			} else{
				persons2trajectorities.put( de.getPersonId(), new TrajectoriesData() );
				persons2trajectorities.get( de.getPersonId() ).setTimeOnTravel( de.getTime() );
			}
		} else if ( event instanceof WarmEmissionEvent ) {
			// I think that this will not work!!!!!  kn
		} else {
			if ( WarmEmissionEvent.EVENT_TYPE.equals( event.getEventType() ) ) {
				WarmEmissionEvent ev = (WarmEmissionEvent) event ; // will still not work (I think)
				double nox = Double.parseDouble( ev.getAttributes().get("NOx") ) ;  // will work (I hope) as long as key is spelled correctly.
			}
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
