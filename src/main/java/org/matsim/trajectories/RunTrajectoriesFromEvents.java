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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.utils.io.IOUtils;


class RunTrajectoriesFromEvents{
	
	private static enum AnalysisType { test, onePercent }
	private static final AnalysisType analysisType = AnalysisType.onePercent ;
	
	static Logger log = Logger.getLogger(RunTrajectoriesFromEvents.class);


	public static void main(String[] args) throws Exception {
		log.setLevel(Level.INFO);

		final String inputFileEvents  ;
		final String inputFileNetwork ;
		final String outputDirectory  ;
//		final String outputExperiancedPlansFileName = "/expericedPlans.xml.gz";
		
		switch ( analysisType ) {
			case test:
				inputFileEvents  = "scenarios/emissionsExample/output_events.xml.gz";
				inputFileNetwork = "scenarios/emissionsExample/output_network.xml.gz";
				outputDirectory  = "output/trajectories" ;
				break;
			case onePercent:
//				inputFileEvents  = "../tubCloud/Shared/vsp_zerocuts/scenarios/berlin-v5.2-1pct/berlin-v5.2-1pct.output_events.xml.gz";
//				inputFileNetwork = "../tubCloud/Shared/vsp_zerocuts/scenarios/berlin-v5.2-1pct/berlin-v5.2-1pct.output_network.xml.gz";
				inputFileEvents  = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.2-1pct/"
								   + "output-berlin-v5.2-1pct/berlin-v5.2-1pct.output_events.xml.gz" ;
				inputFileNetwork = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.2-1pct/"
								   + "output-berlin-v5.2-1pct/berlin-v5.2-1pct.output_network.xml.gz" ;
				outputDirectory  = "output/trajectories/1pct" ;
				break;
			default:
					throw new RuntimeException("undefined") ;
		}

		EventsManager eventsManager = EventsUtils.createEventsManager();

		// if network is needed for analysis:
 		final Config config = ConfigUtils.createConfig();
		config.network().setInputFile( inputFileNetwork );
//		config.global().setCoordinateSystem( "GK4" );		//Derzeit Transformation von Atlantis (test-Daten) auf WGS84 nicht möglich, daher nutze WGS84
		config.global().setCoordinateSystem( "WGS84" ); 
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		Network network = scenario.getNetwork() ;
		
		EventHandlerTrajAgents handlerTrajAgents = new EventHandlerTrajAgents(network);
		eventsManager.addHandler(handlerTrajAgents);
		
		MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
		eventsReader.readURL( IOUtils.newUrl( null, inputFileEvents ) );
		
		eventsManager.finishProcessing();

		handlerTrajAgents.writeVehiclesDataToConsole();
		handlerTrajAgents.writeDriversDataToFile( "out.txt" );
		System.out.println("### Done");
		
	}
	
}
