package org.matsim.vsp.freightDemandGeneration;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import java.io.File;
import java.io.IOException;

public class FreightAnalyse {

	/**
	 *  Calculates and writes some analysis for the defined Runs.
	 *
	 *  @author rewert
	 */


	private static String RUN_DIR = null ;

	private static String OUTPUT_DIR = null ;

	private static final Logger log = Logger.getLogger(FreightAnalyse.class);

	public static void main(String[] args) throws UncheckedIOException, IOException {
		RUN_DIR = args[0]+"/";
		OUTPUT_DIR = RUN_DIR + "Analysis/" ;
		OutputDirectoryLogging.initLoggingWithOutputDirectory(OUTPUT_DIR);

		FreightAnalyse analysis = new FreightAnalyse();
		analysis.run();
		log.info("### Finished ###");
		OutputDirectoryLogging.closeOutputDirLogging();
	}

	private void run() throws UncheckedIOException, IOException {

//			File configFile = new File(RUN_DIR + "output_config.xml");
////			File configFile = new File(RUN_DIR + "output_config.xml.gz");
//			File populationFile = new File(RUN_DIR + "output_plans.xml.gz");
		File networkFile = new File(RUN_DIR+ "output_network.xml.gz");
		File carrierFile = new File(RUN_DIR+ "output_carriers.xml.gz");
		File vehicleTypeFile = new File(RUN_DIR+ "output_carriersVehicleTypes.xml.gz");

		Network network = NetworkUtils.readNetwork(networkFile.getAbsolutePath());



		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
		new CarrierVehicleTypeReader(vehicleTypes).readFile(vehicleTypeFile.getAbsolutePath()) ;

		log.warn("VehicleTypes: "+ vehicleTypes.getVehicleTypes().keySet().toString());

		Carriers carriers = new Carriers() ;
		new CarrierPlanXmlReader(carriers, vehicleTypes).readFile(carrierFile.getAbsolutePath() ) ;

		EventsManager eventsManager = EventsUtils.createEventsManager();
		TripEventHandler tripHandler = new TripEventHandler(network, vehicleTypes);
		eventsManager.addHandler(tripHandler);

		log.info("Reading the event file...");
		eventsManager.initProcessing();
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile(RUN_DIR + "output_events.xml.gz");
		eventsManager.finishProcessing();
		log.info("Reading the event file... Done.");

		TripWriter tripWriter = new TripWriter(tripHandler, OUTPUT_DIR);
		for (Carrier carrier : carriers.getCarriers().values()){
		//	tripWriter.writeDetailedResultsSingleCarrier(carrier.getId().toString());
			tripWriter.writeTourResultsSingleCarrier(carrier.getId().toString());
		}

		tripWriter.writeResultsPerVehicleTypes();
		tripWriter.writeTourResultsAllCarrier();
		tripWriter.writeResultsAllCarrier(carriers);


		log.info("### Analysis DONE");

	}

}
