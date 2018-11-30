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

import java.io.BufferedWriter;
import java.io.File;
import java.io.Writer;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

/**
 * @author kturner
 *
 * TODO: setVehicleType
 * TODO: other trajectories
 * TODO: write data to file (kind of file?) instead of console;
 */
class EventHandlerTrajAgents implements BasicEventHandler {

	static Logger log = Logger.getLogger(EventHandlerTrajAgents.class);
	
	private TreeMap<Id<Vehicle>, Double > vehicles2trajectorities= new TreeMap<>();

	private Network network;

	public EventHandlerTrajAgents(Network network) {
		this.network = network;
	}

	void writeVehiclesDataToConsole() {
		for ( Map.Entry entry : vehicles2trajectorities.entrySet() ) {
			System.out.println("VehicleId=" + entry.getKey() + "; distance=" + entry.getValue() ) ;
		}
	}

	void writeDriversDataToFile(String filename) throws Exception {
		try ( BufferedWriter writer = IOUtils.getBufferedWriter( filename ) ) {
			writer.write("vehicleId;distance") ;
			writer.newLine();
			for ( Map.Entry entry : vehicles2trajectorities.entrySet() ) {
				writer.write( entry.getKey() + ";" + entry.getValue()  );
				writer.newLine();
			}
		}
	}

	public void reset(int iteration) {
		this.vehicles2trajectorities.clear();
	}

	
	public void handleEvent(Event event) {
//		log.debug("handle Event: " + event.getEventType() +", Instanceof: " + event.getClass().getName() + ", Event: " + event.toString());
		
		if (event instanceof VehicleEntersTrafficEvent) { 
			Id<Vehicle> vehicleId = ((VehicleEntersTrafficEvent) event).getVehicleId();
			if (! vehicles2trajectorities.containsKey(vehicleId )){
				vehicles2trajectorities.put(vehicleId, 0.);
			}
		}

		if (event instanceof LinkEnterEvent) {
			LinkEnterEvent le = (LinkEnterEvent) event;
			final double lengthOfLink = network.getLinks().get( le.getLinkId() ).getLength();
			double newDistanceRoute = vehicles2trajectorities.get(le.getVehicleId() ) + lengthOfLink;
			vehicles2trajectorities.put(le.getVehicleId(), newDistanceRoute ) ;
		}


		if (event instanceof VehicleAbortsEvent) {
			log.warn("aborted vehicle; vehicleId=" + ((VehicleAbortsEvent) event).getVehicleId() ) ;
			this.vehicles2trajectorities.remove( ((VehicleAbortsEvent) event).getVehicleId() ) ;
		}
	}
}
