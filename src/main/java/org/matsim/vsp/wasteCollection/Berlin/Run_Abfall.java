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
import org.matsim.core.utils.gis.ShapeFileReader;
import org.geotools.api.feature.simple.SimpleFeature;

/**
 * @author Ricardo Ewert
 *
 */
public class Run_Abfall {

	static final Logger log = LogManager.getLogger(Run_Abfall.class);

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

	public static void main(String[] args) throws Exception {

		/*
		 * You have to decide the network. If you choose one of the chessboard networks,
		 * you have to select a chessboard scenario and if you select the Berlin
		 * network, you have to select one of the Berlin cases. The beginning of the
		 * name of the scenario shows you the needed network.
		 */

		netzwerkAuswahl netzwerkWahl = netzwerkAuswahl.berlinNetwork;
		scenarioAuswahl scenarioWahl;
		carrierChoice chosenCarrier;
		int jspritIterations;
		double volumeDustbinInLiters;
		double secondsServiceTimePerDustbin;
        String runName = null;
		String outputLocation;
		String day;
		String networkChangeEventsFileLocation;
		String carriersFileLocation = null;
		String vehicleTypesFileLocation = null;
		String shapeFileLocation;
		boolean oneCarrierForOneDistrict;

		for (String arg : args) {
			log.info(arg);
		}
		if (args.length == 0) {
			chosenCarrier = carrierChoice.carriersWithDieselVehicle;        //Change this when switching between chessboard and berlin
			scenarioWahl = scenarioAuswahl.berlinCollectedGarbageForOneDay; //and this
			shapeFileLocation = berlinDistrictsWithGarbageInformations;
			oneCarrierForOneDistrict = true;
			jspritIterations = 100;
			volumeDustbinInLiters = 1100; // in liter
			secondsServiceTimePerDustbin = 41;
            runName = "NameTest";
			outputLocation = "output/" + runName;
			day = "MO";
			networkChangeEventsFileLocation = "";
		} else {
			scenarioWahl = scenarioAuswahl.chessboardTotalGarbageToCollect;
			jspritIterations = Integer.parseInt(args[0]);
			volumeDustbinInLiters = Double.parseDouble(args[1]); // in liter
			secondsServiceTimePerDustbin = Double.parseDouble(args[2]);
			day = args[3];
			outputLocation = args[4];
			vehicleTypesFileLocation = args[5];
			networkChangeEventsFileLocation = args[6];
			carriersFileLocation = args[7];
			shapeFileLocation = args[8];
			oneCarrierForOneDistrict = Boolean.parseBoolean(args[9]);
			chosenCarrier = carrierChoice.carriersWithDieselVehicle;
		}
		LogManager.getRootLogger().atLevel(Level.INFO);

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
					log.info("Setting networkChangeEventsInput file: " + networkChangeEventsFileLocation);
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

		//TESTING
		for (Carrier singleCarrier : carriers.getCarriers().values()) {
			System.out.println(singleCarrier.getId().toString());
		}

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

		//TESTING
		for (Carrier singleCarrier : carriers.getCarriers().values()) {
			System.out.println(singleCarrier.getId().toString());
		}

		//-----------------TEST A SINGLE CARRIER------------------------
		System.out.println("TESTING ONE CARRIER: ");
		var carrier1 = carriers.getCarriers().get(Id.create("Carrier Haselhorst", Carrier.class));
//		var carrier2 = carriers.getCarriers().get(Id.create("Carrier Wilhelmstadt", Carrier.class));
		carriers.getCarriers().clear();
		carriers.addCarrier(carrier1);
//		carriers.addCarrier(carrier2);

		//-----------------RUN THE SPLIT------------------------
		//System.out.println("VRP SPLIT: ");
		int numberOfCarriers = 3;
		VrpSplitUtils.createGeoSeedCarriers(scenario, numberOfCarriers, jspritIterations, runName);
//		//TESTING
//		for (Carrier singleCarrier : carriers.getCarriers().values()) {
//			System.out.println(singleCarrier.getId().toString());
//		}

		/*
		 * This xml output gives a summary with information about the created shipments,
		 * so that you can already have this information, while jsprit and matsim are
		 * still running.
		 */
		AbfallUtils.outputSummaryShipments(scenario, day, carrierMap);

		// jsprit
        CarriersUtils.runJsprit(scenario);
//		AbfallUtils.solveWithJsprit(scenario, carriers, carrierMap, jspritIterations, numberOfCarriers);

		// final Controler controler = new Controler(scenario);
		Controler controler = AbfallUtils.prepareController(scenario);

//		AbfallUtils.scoringAndManagerFactory(scenario, controler);

		//The VSP default settings are designed for person transport simulation. After talking to Kai, they will be set to WARN here. Kai MT may'23
		controler.getConfig().vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
		controler.run();

		new CarrierPlanWriter(carriers)
				.write(scenario.getConfig().controller().getOutputDirectory() + "/output_CarrierPlans.xml");

//		AbfallUtils.outputSummary(districtsWithGarbage, scenario, carrierMap, day, volumeDustbinInLiters,
//				secondsServiceTimePerDustbin);
//		AbfallUtils.createResultFile(scenario, carriers);
	}
}
