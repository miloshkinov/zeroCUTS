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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.vehicles.Vehicle;

/**
 * @author kturner
 *
 * TODO: setVehicleType
 * TODO: distances -> need network available //via LinkEnterEvents
 * TODO: other trajectories
 * TODO: write data to file (kind of file?) instead of console;
 */
class EventHandlerTrajAgents implements BasicEventHandler {

	static Logger log = Logger.getLogger(EventHandlerTrajAgents.class);
	
	private TreeMap<Id<Vehicle>, TrajectoriesData > vehicles2trajectorities= new TreeMap<Id<Vehicle>, TrajectoriesData>();
	private TreeMap<Id<Vehicle>, Double > vehicle2StartingTime = new TreeMap<Id<Vehicle>, Double>();

	void writeDriversDataToConsole() {
		for (Id<Vehicle> vehicleId: vehicles2trajectorities.keySet()) {
			System.out.println("Agent: " + vehicleId.toString() + " : " + vehicles2trajectorities.get(vehicleId).toString());
		}
	}

	void writeDriversDataToFile(File filename) throws Exception {
		throw new Exception("Not implemented");
	}

	public void reset(int iteration) {
		this.vehicles2trajectorities.clear();
	}

	
	public void handleEvent(Event event) {
		log.debug("handle Event: " + event.getEventType() +", Instanceof: " + event.getClass().getName() + ", Event: " + event.toString());
		
		if (event instanceof VehicleEntersTrafficEvent) { 
			Id<Vehicle> vehicleId = ((VehicleEntersTrafficEvent) event).getVehicleId();
			if (! vehicles2trajectorities.containsKey(vehicleId )){
				vehicles2trajectorities.put(vehicleId, new TrajectoriesData());
			}
			double newTimeVehicleInTraffic = vehicles2trajectorities.get(vehicleId).getTimeVehicleInTraffic() - event.getTime();
			vehicles2trajectorities.get(vehicleId).setTimeVehicleInTraffic(newTimeVehicleInTraffic);
			
			//Merke früheste Startzeit
			if (! vehicle2StartingTime.containsKey(vehicleId)){
				vehicle2StartingTime.put(vehicleId, event.getTime());
			} else {	//Sollte eigentlich nicht passieren, da eventsfile chronologisch ist.
				if (event.getTime() < vehicle2StartingTime.get(vehicleId)) {
					vehicle2StartingTime.put(vehicleId, event.getTime());
				}
			}

		}

		if (event instanceof VehicleLeavesTrafficEvent) { 
			Id<Vehicle> vehicleId = ((VehicleLeavesTrafficEvent) event).getVehicleId();
			//			if (! vehicles2trajectorities.containsKey(vehicleId )){			//Not necessary, because vehicle must enter traffic first! 
			//				vehicles2trajectorities.put(vehicleId, new TrajectoriesData());
			//			}
			double newTimeVehicleInTraffic = vehicles2trajectorities.get(vehicleId).getTimeVehicleInTraffic() + event.getTime();
			vehicles2trajectorities.get(vehicleId).setTimeVehicleInTraffic(newTimeVehicleInTraffic);


			double newTimeVehicleOnTravel = ((VehicleLeavesTrafficEvent) event).getTime() - vehicle2StartingTime.get(vehicleId);
			if (newTimeVehicleOnTravel > vehicles2trajectorities.get(vehicleId).getTimeVehicleInTraffic()){
				vehicles2trajectorities.get(vehicleId).setTimeOnTravel(newTimeVehicleOnTravel);
			}
		}

		//Note: coldEmissionEvent are instanceof: org.matsim.api.core.v01.events.GenericEvent , kmt nov'18
		if (event.getEventType().equals(ColdEmissionEvent.EVENT_TYPE)) {
			//TODO: Add all Values ;)
			Id<Vehicle> vehicleId = Id.createVehicleId(event.getAttributes().get(ColdEmissionEvent.ATTRIBUTE_VEHICLE_ID));
			//HC
			double currentHC = Double.parseDouble(event.getAttributes().get(ColdPollutant.HC.toString())); 
			vehicles2trajectorities.get(vehicleId).setHC( vehicles2trajectorities.get(vehicleId).getHC() + currentHC);
			
		}
		
		//Note: warmEmissionEvent are instanceof: org.matsim.api.core.v01.events.GenericEvent , kmt nov'18
		if (event.getEventType().equals(WarmEmissionEvent.EVENT_TYPE)) {
			//TODO: Add all Values ;)
			Id<Vehicle> vehicleId = Id.createVehicleId(event.getAttributes().get(WarmEmissionEvent.ATTRIBUTE_VEHICLE_ID));
			//HC
			double currentHC = Double.parseDouble(event.getAttributes().get(WarmPollutant.HC.toString())); 
			vehicles2trajectorities.get(vehicleId).setHC( vehicles2trajectorities.get(vehicleId).getHC() + currentHC);
		}

		if (event instanceof VehicleAbortsEvent) {
			vehicles2trajectorities.get(((VehicleAbortsEvent) event).getVehicleId()).setAborted(true);
		}
	}

	//TODO: Assign vehicleTypes to vehicleTajectories

}
