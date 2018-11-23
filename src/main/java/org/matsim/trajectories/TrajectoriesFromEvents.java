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
import java.io.IOException;
import java.net.URL;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkReaderMatsimV2;
import org.matsim.core.utils.io.IOUtils;

public class TrajectoriesFromEvents {
	
	public static final String outputDirectory = "output/trajectories" ;
	
	//TODO: Warum findet er die Datei im Public-SVN nicht?
//	public static final String inputFileEvents  = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.2-1pct/output-berlin-v5.2-1pct/berlin-v5.2-1pct.output_events.xml.gz";
	public static final String inputFileEvents = "../tubCloud/Shared/vsp_zerocuts/scenarios/berlin-v5.2-1pct/berlin-v5.2-1pct.output_events.xml.gz";
//	public static final String inputFileNetwork = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.2-1pct/output-berlin-v5.2-1pct/berlin-v5.2-1pct.output_network.xml.gz";
	public static final String inputFileNetwork = "../tubCloud/Shared/vsp_zerocuts/scenarios/berlin-v5.2-1pct/berlin-v5.2-1pct.output_network.xml.gz";
	
	public static void main(String[] args) throws Exception {
		
		EventsManager eventsManager = EventsUtils.createEventsManager();

//		Network network = null;
//		MatsimNetworkReader networkReader = new MatsimNetworkReader(network);		//Warum NullPointer bei einem OutputNetzwerk?
//		networkReader.readFile(inputFileNetwork);
		
		EventHandlerTrajAgents handlerTrajAgents = new EventHandlerTrajAgents();
		eventsManager.addHandler(handlerTrajAgents);
		
		MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
		eventsReader.readFile(inputFileEvents); 
		
		handlerTrajAgents.writeDriversDataToConsole();
//		handlerTrajAgents.writeDriversDataToFile(new File("Dummy"));
		System.out.println("### Done");
		
	}
	
}
