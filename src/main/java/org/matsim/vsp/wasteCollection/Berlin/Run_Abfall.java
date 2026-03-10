package org.matsim.vsp.wasteCollection.Berlin;

import java.util.*;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.application.options.ShpOptions;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.freight.carriers.CarrierPlanWriter;
import org.matsim.freight.carriers.CarrierVehicleTypes;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.geotools.api.feature.simple.SimpleFeature;
import org.matsim.application.MATSimAppCommand;
import picocli.CommandLine;

import java.nio.file.Path;

/**
 * @author Ricardo Ewert
 *
 */
	@CommandLine.Command(
			name = "run-berlin-garbage",
			description = "Runs the Berlin garbage collection scenario.",
			showDefaultValues = true
	)
	public class Run_Abfall implements MATSimAppCommand {

		private static final String original_Chessboard = "https://raw.githubusercontent.com/matsim-org/matsim/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml";
		private static final String berlin = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.2-1pct/output-berlin-v5.2-1pct/berlin-v5.2-1pct.output_network.xml.gz";
		private static final String berlinDistrictsWithGarbageInformations = "scenarios/wasteCollection/Berlin/garbageInput/districtsWithGarbageInformations.shp";
		private static final String inputVehicleTypes = "scenarios/wasteCollection/vehicleTypes.xml";
		private static final String inputCarriersWithDieselVehicle = "scenarios/wasteCollection/Berlin/carriers_diesel_vehicle.xml";
		private static final String inputCarriersWithMediumBatteryVehicle = "scenarios/wasteCollection/Berlin/carriers_medium_EV.xml";
		private static final String inputCarriersWithSmallBatteryVehicle = "scenarios/wasteCollection/Berlin/carriers_small_EV.xml";
		private static final String inputCarriersFromInputFile = "scenarios/wasteCollection/Berlin/carriers_chessboard.xml";

		private enum netzwerkAuswahl {
			originalChessboard, berlinNetwork
		}

		private enum scenarioAuswahl {
			chessboardTotalGarbageToCollect, chessboardGarbagePerMeterToCollect, berlinSelectedDistricts,
			berlinDistrictsWithInputTotalGarbagePerDistrict, berlinDistrictsWithInputGarbagePerMeter,
			berlinCollectedGarbageForOneDay

		}

		private enum carrierChoice {
			carriersWithDieselVehicle, carriersWithMediumBattereyVehicle, carriersWithSmallBatteryVehicle,
			carriersFromInputFile
		}

		private static final Logger log = LogManager.getLogger(Run_Abfall.class);

		// --- Network and scenario selection ---
		@CommandLine.Option(names = "--netzwerkWahl", description = "Network selection (e.g., berlinNetwork, chessboardNetwork).", defaultValue = "berlinNetwork")
		private netzwerkAuswahl netzwerkWahl;

		@CommandLine.Option(names = "--scenarioWahl", description = "Scenario selection (e.g., berlinCollectedGarbageForOneDay).", defaultValue = "berlinCollectedGarbageForOneDay")
		private scenarioAuswahl scenarioWahl;

		// --- Core simulation parameters ---
		@CommandLine.Option(names = "--jspritIterations", description = "Number of jsprit iterations.", defaultValue = "100")
		private int jspritIterations;

		@CommandLine.Option(names = "--volumeDustbinInLiters", description = "Volume of a dustbin in liters.", defaultValue = "1100")
		private double volumeDustbinInLiters;

		@CommandLine.Option(names = "--secondsServiceTimePerDustbin", description = "Service time per dustbin in seconds.", defaultValue = "41")
		private double secondsServiceTimePerDustbin;

		// --- Files and paths ---
		@CommandLine.Option(names = "--outputLocation", description = "Path to the output directory.", defaultValue = "output/")
		private String outputLocation;

		@CommandLine.Option(names = "--networkChangeEventsFile", description = "Path to network change events file.", defaultValue = "")
		private String networkChangeEventsFileLocation;

		@CommandLine.Option(names = "--carriersFilePath", description = "Path to the carriers file.")
		private String carriersFileLocation;

		@CommandLine.Option(names = "--vehicleTypesFilePath", description = "Path to the vehicle types file.")
		private String vehicleTypesFileLocation;

		@CommandLine.Option(names = "--shapeFilePath", description = "Path to the shapefile with district data.", defaultValue = berlinDistrictsWithGarbageInformations)
		private Path shapeFileLocation;

		// --- Other options ---
		@CommandLine.Option(names = "--day", description = "Day of the week (MO, DI, MI, DO, FR).", required = true)
		private String day;

		@CommandLine.Option(names = "--clusterStrategy", description = "Clustering strategy for VRP splitting.", required = true, defaultValue = "none")
		private VrpSplitUtils.clusteringStrategy clusterStrategy;

		@CommandLine.Option(names = "--numberOfShipmentsPerCarrier", description = "Number of shipments per carrier.", defaultValue = "300")
		private int numberOfShipmentsPerCarrier;

		@CommandLine.Option(names = "--oneCarrierForOneDistrict", description = "Use one carrier per district.", defaultValue = "false")
		private boolean oneCarrierForOneDistrict;

		@CommandLine.Option(names = "--chosenCarrier", description = "Carrier choice (e.g., carriersWithDieselVehicle).", defaultValue = "carriersWithDieselVehicle")
		private carrierChoice chosenCarrier;

		public static void main(String[] args) {
			System.exit(new CommandLine(new Run_Abfall()).execute(args));
		}

		@Override
		public Integer call() throws Exception {
			log.info("Starting Berlin Garbage Collection Simulation...");

			log.info("Network: {}", netzwerkWahl);
			log.info("Scenario: {}", scenarioWahl);
			log.info("Iterations: {}", jspritIterations);
			log.info("Volume (L): {}", volumeDustbinInLiters);
			log.info("Service time (s): {}", secondsServiceTimePerDustbin);
			log.info("Day: {}", day);
			log.info("Clustering Strategy: {}", clusterStrategy);
			log.info("Shipments per carrier: {}", numberOfShipmentsPerCarrier);
			log.info("Output: {}", outputLocation);

			// MATSim config
			Config config = ConfigUtils.createConfig();

			switch (netzwerkWahl) {
				case originalChessboard -> {
					config.controller().setOutputDirectory("output/original_Chessboard/withVRPSplitv1");
					config.network().setInputFile(original_Chessboard);
				}
				case berlinNetwork -> {
					// Berlin scenario network
					config.controller().setOutputDirectory(outputLocation);
					config.network().setInputFile(berlin);
					if (!Objects.equals(networkChangeEventsFileLocation, "")) {
						log.info("Setting networkChangeEventsInput file: {}", networkChangeEventsFileLocation);
						config.network().setTimeVariantNetwork(true);
						config.network().setChangeEventsInputFile(networkChangeEventsFileLocation);
					}
				}
				default -> throw new RuntimeException("no network selected.");
			}

			switch (chosenCarrier) {
				case carriersWithDieselVehicle:
					vehicleTypesFileLocation =  inputVehicleTypes;
					carriersFileLocation = inputCarriersWithDieselVehicle;
					break;
				case carriersWithSmallBatteryVehicle:
					vehicleTypesFileLocation =  inputVehicleTypes;
					carriersFileLocation = inputCarriersWithSmallBatteryVehicle;
					break;
				case carriersWithMediumBattereyVehicle:
					vehicleTypesFileLocation =  inputVehicleTypes;
					carriersFileLocation = inputCarriersWithMediumBatteryVehicle;
					break;
				case carriersFromInputFile:
					vehicleTypesFileLocation =  inputVehicleTypes;
					carriersFileLocation = inputCarriersFromInputFile;
					break;
				default:
					throw new RuntimeException("no carriers selected.");
			}

			AbfallUtils.prepareConfig(config, 0, vehicleTypesFileLocation, carriersFileLocation);
			Scenario scenario = ScenarioUtils.loadScenario(config);
			CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);

			// creates carrier
			Carriers carriers = CarriersUtils.addOrGetCarriers(scenario);
			HashMap<String, Carrier> carrierMap = AbfallUtils.createCarrier(carriers);

			Map<Id<Link>, ? extends Link> allLinks = scenario.getNetwork().getLinks();
			HashMap<String, Id<Link>> garbageDumps = AbfallUtils.createDumpMap();
			ShpOptions shpOptions = new ShpOptions(shapeFileLocation, null, null);
			List<SimpleFeature> districtsWithGarbage = shpOptions.readFeatures();

			AbfallUtils.createMapWithLinksInDistricts(districtsWithGarbage, allLinks);

			carriers.getCarriers().clear();

			switch (scenarioWahl) {
				case chessboardTotalGarbageToCollect -> {
					int kgGarbageToCollect = 12 * 1000;
					CarrierVehicleTypes carrierVehicleTypes = CarriersUtils.getCarrierVehicleTypes(scenario);
					AbfallChessboardUtils.createShipmentsForChessboardI(carrierMap, kgGarbageToCollect, allLinks,
							volumeDustbinInLiters, secondsServiceTimePerDustbin, scenario, carriers);
					FleetSize fleetSize = FleetSize.INFINITE;
					AbfallChessboardUtils.createCarriersForChessboard(carriers, fleetSize, carrierVehicleTypes);
				}
				case chessboardGarbagePerMeterToCollect -> {
					double kgGarbagePerMeterToCollect = 0.2;
					CarrierVehicleTypes carrierVehicleTypes2 = CarriersUtils.getCarrierVehicleTypes(scenario);
					AbfallChessboardUtils.createShipmentsForChessboardII(carrierMap, kgGarbagePerMeterToCollect, allLinks,
							volumeDustbinInLiters, secondsServiceTimePerDustbin, scenario, carriers);
					FleetSize fleetSize2 = FleetSize.INFINITE;
					AbfallChessboardUtils.createCarriersForChessboard(carriers, fleetSize2, carrierVehicleTypes2);
				}
				case berlinSelectedDistricts -> {
					// day input: MO or DI or MI or DO or FR
					List<String> districtsForShipments = List.of("Malchow");
					day = "MI";
					AbfallUtils.createShipmentsForSelectedArea(districtsWithGarbage, districtsForShipments, day, garbageDumps,
							scenario, carriers, carrierMap, allLinks, volumeDustbinInLiters, secondsServiceTimePerDustbin);
				}
				case berlinDistrictsWithInputGarbagePerMeter -> {
					// day input: MO or DI or MI or DO or FR
					// input for Map .put("district", double kgGarbagePerMeterToCollect)
					HashMap<String, Double> areasForShipmentPerMeterMap = new HashMap<>();
					areasForShipmentPerMeterMap.put("Malchow", 1.0);
					day = "MI";
					AbfallUtils.createShipmentsWithGarbagePerMeter(districtsWithGarbage, areasForShipmentPerMeterMap, day,
							garbageDumps, scenario, carriers, carrierMap, allLinks, volumeDustbinInLiters,
							secondsServiceTimePerDustbin);
				}
				case berlinDistrictsWithInputTotalGarbagePerDistrict -> {
					// day input: MO or DI or MI or DO or FR
					// input for Map .put("district", int kgGarbageToCollect)
					HashMap<String, Integer> areasForShipmentPerVolumeMap = new HashMap<>();
					areasForShipmentPerVolumeMap.put("Malchow", 5 * 1000);
					// areasForShipmentPerVolumeMap.put("Hansaviertel", 20 * 1000);
					day = "MI";
					AbfallUtils.createShipmentsGarbagePerVolume(districtsWithGarbage, areasForShipmentPerVolumeMap, day,
							garbageDumps, scenario, carriers, carrierMap, allLinks, volumeDustbinInLiters,
							secondsServiceTimePerDustbin);
				}
				case berlinCollectedGarbageForOneDay ->
					// MO or DI or MI or DO or FR
						AbfallUtils.createShipmentsForSelectedDay(districtsWithGarbage, day, garbageDumps, scenario, carriers,
								carrierMap, allLinks, volumeDustbinInLiters, secondsServiceTimePerDustbin, oneCarrierForOneDistrict);
				default -> throw new RuntimeException("no scenario selected.");
			}


			//-----------------RUN THE SPLIT------------------------
			VrpSplitUtils.splitCarriers(scenario, clusterStrategy, numberOfShipmentsPerCarrier, jspritIterations, outputLocation);

			/*
			 * This xml output gives a summary with information about the created shipments,
			 * so that you can already have this information, while jsprit and matsim are
			 * still running.
			 */
			AbfallUtils.outputSummaryShipments(scenario, day, carrierMap);

			// jsprit
			CarriersUtils.runJsprit(scenario);

			// final Controler controler = new Controler(scenario);
			Controler controler = AbfallUtils.prepareController(scenario);

			//AbfallUtils.scoringAndManagerFactory(scenario, controler);

			//The VSP default settings are designed for person transport simulation. After talking to Kai, they will be set to WARN here. Kai MT may'23
			controler.getConfig().vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
			controler.run();

			new CarrierPlanWriter(carriers).write(scenario.getConfig().controller().getOutputDirectory() + "/output_CarrierPlans.xml");

	//		AbfallUtils.outputSummary(districtsWithGarbage, scenario, carrierMap, day, volumeDustbinInLiters,
	//				secondsServiceTimePerDustbin);
	//		AbfallUtils.createResultFile(scenario, carriers);

			return 0;
		}
	}
