package org.matsim.vsp.freight.food.analyse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.freight.carriers.*;

import java.io.File;
import java.io.IOException;

public class FreightAnalyseKT {

	/**
	 *  Calculates and writes some analysis for the defined Runs.
	 *
	 *  @author kturner
	 */

//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/01_ICEVBEV_NwCE_BVWP_2000it_DC_noTax/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/02_ICEVBEV_NwCE_BVWP_2000it_DC_Tax25/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/03_ICEVBEV_NwCE_BVWP_2000it_DC_Tax50/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/04_ICEVBEV_NwCE_BVWP_2000it_DC_Tax100/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/05_ICEVBEV_NwCE_BVWP_2000it_DC_Tax150/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/06_ICEVBEV_NwCE_BVWP_2000it_DC_Tax200/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/07_ICEVBEV_NwCE_BVWP_2000it_DC_Tax250/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/08_ICEVBEV_NwCE_BVWP_2000it_DC_Tax300/" ;

//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/51_ICEVBEV_NwCE_BVWP_2000it_DCoff_noTax/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/52_ICEVBEV_NwCE_BVWP_2000it_DCoff_Tax25/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/53_ICEVBEV_NwCE_BVWP_2000it_DCoff_Tax50/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/54_ICEVBEV_NwCE_BVWP_2000it_DCoff_Tax100/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/55_ICEVBEV_NwCE_BVWP_2000it_DCoff_Tax150/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/56_ICEVBEV_NwCE_BVWP_2000it_DCoff_Tax200/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/57_ICEVBEV_NwCE_BVWP_2000it_DCoff_Tax250/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/58_ICEVBEV_NwCE_BVWP_2000it_DCoff_Tax300/" ;

//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/21_ICEVBEV_NwCE_BVWP_10000it_DC_noTax/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/22_ICEVBEV_NwCE_BVWP_10000it_DC_Tax25/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/23_ICEVBEV_NwCE_BVWP_10000it_DC_Tax50/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/24_ICEVBEV_NwCE_BVWP_10000it_DC_Tax100/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/25_ICEVBEV_NwCE_BVWP_10000it_DC_Tax150/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/26_ICEVBEV_NwCE_BVWP_10000it_DC_Tax200/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/27_ICEVBEV_NwCE_BVWP_10000it_DC_Tax250/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/28_ICEVBEV_NwCE_BVWP_10000it_DC_Tax300/" ;

//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/71_ICEVBEV_NwCE_BVWP_10000it_DCoff_noTax/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/72_ICEVBEV_NwCE_BVWP_10000it_DCoff_Tax25/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/73_ICEVBEV_NwCE_BVWP_10000it_DCoff_Tax50/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/74_ICEVBEV_NwCE_BVWP_10000it_DCoff_Tax100/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/75_ICEVBEV_NwCE_BVWP_10000it_DCoff_Tax150/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/76_ICEVBEV_NwCE_BVWP_10000it_DCoff_Tax200/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/77_ICEVBEV_NwCE_BVWP_10000it_DCoff_Tax250/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/78_ICEVBEV_NwCE_BVWP_10000it_DCoff_Tax300/" ;

//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/11_ICEVBEV_NwCE_BVWP_1it_DC_noTax/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/12_ICEVBEV_NwCE_BVWP_1it_DC_Tax25/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/13_ICEVBEV_NwCE_BVWP_1it_DC_Tax50/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/14_ICEVBEV_NwCE_BVWP_1it_DC_Tax100/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/15_ICEVBEV_NwCE_BVWP_1it_DC_Tax150/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/16_ICEVBEV_NwCE_BVWP_1it_DC_Tax200/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/17_ICEVBEV_NwCE_BVWP_1it_DC_Tax250/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/18_ICEVBEV_NwCE_BVWP_1it_DC_Tax300/" ;

//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/61_ICEVBEV_NwCE_BVWP_1it_DCoff_noTax/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/62_ICEVBEV_NwCE_BVWP_1it_DCoff_Tax25/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/63_ICEVBEV_NwCE_BVWP_1it_DCoff_Tax50/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/64_ICEVBEV_NwCE_BVWP_1it_DCoff_Tax100/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/65_ICEVBEV_NwCE_BVWP_1it_DCoff_Tax150/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/66_ICEVBEV_NwCE_BVWP_1it_DCoff_Tax200/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/67_ICEVBEV_NwCE_BVWP_1it_DCoff_Tax250/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/68_ICEVBEV_NwCE_BVWP_1it_DCoff_Tax300/" ;


//#### Diesel only
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/01a_ICEV_NwCE_BVWP_2000it_DC_noTax/" ;
//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/51a_ICEV_NwCE_BVWP_2000it_DCoff_noTax/" ;

//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/21a_ICEV_NwCE_BVWP_10000it_DC_noTax/" ;
	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/20200611_fa8d691/10000it/71a_ICEV_NwCE_BVWP_10000it_DCoff_noTax/" ;


//	private static final String RUN_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/Demo1ItDC/" ;

// 	private static final String RUN_DIR = "../runs-svn/zeroCUTS/Food_ETrucks/I-Base_NwCE_BVWP_Pickup_10000it/";


	private static final String OUTPUT_DIR = RUN_DIR + "Analysis2/" ;

	private static final Logger log = LogManager.getLogger(FreightAnalyseKT.class);

	public static void main(String[] args) throws UncheckedIOException, IOException {
		OutputDirectoryLogging.initLoggingWithOutputDirectory(OUTPUT_DIR);

		FreightAnalyseKT analysis = new FreightAnalyseKT();
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
		File vehicleTypeFile = new File(RUN_DIR+ "output_vehicleTypes.xml.gz");

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
			tripWriter.writeDetailedResultsSingleCarrier(carrier.getId().toString());
			tripWriter.writeTourResultsSingleCarrier(carrier.getId().toString());
		}

		tripWriter.writeResultsPerVehicleTypes();
		tripWriter.writeTourResultsAllCarrier();


		log.info("### Analysis DONE");

	}

}
