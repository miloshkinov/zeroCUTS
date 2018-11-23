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
import java.net.URL;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;


class RunTrajectoriesFromEvents{

	enum AnalysisType { test, onePercent }

	private static final AnalysisType analysisType = AnalysisType.test ;


	public static void main(String[] args) throws Exception {


		switch ( analysisType ) {
			case test:
				break;
			case onePercent:
				public static final String inputFileEvents  = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.2-1pct/output-berlin-v5.2-1pct/berlin-v5.2-1pct.output_events.xml.gz";
				public static final String inputFileNetwork = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.2-1pct/output-berlin-v5.2-1pct/berlin-v5.2-1pct.output_network.xml.gz";
				public static final String outputDirectory = "output/trajectories" ;
				break;
			case default:
					throw new RuntimeException("undefined") ;
		}



		EventsManager eventsManager = EventsUtils.createEventsManager();

		// if network is needed for analysis:
// 		final Config config = ConfigUtils.createConfig();
//		config.network().setInputFile( inputFileNetwork );
//		config.global().setCoordinateSystem( "GK4" );
//		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
//		Network network = scenario.getNetwork() ;

		EventHandlerTrajAgents handlerTrajAgents = new EventHandlerTrajAgents();
		eventsManager.addHandler(handlerTrajAgents);
		
		MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
		eventsReader.readURL( IOUtils.newUrl( null, inputFileEvents ) );

		handlerTrajAgents.writeDriversDataToConsole();
//		handlerTrajAgents.writeDriversDataToFile(new File("Dummy"));
		System.out.println("### Done");
		
	}
	
}
