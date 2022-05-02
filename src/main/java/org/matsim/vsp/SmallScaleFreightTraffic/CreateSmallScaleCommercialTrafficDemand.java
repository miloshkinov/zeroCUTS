package org.matsim.vsp.SmallScaleFreightTraffic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.freight.Freight;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierUtils;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vsp.freightAnalysis.FreightAnalyse;
import org.opengis.feature.simple.SimpleFeature;
import com.google.common.base.Joiner;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import picocli.CommandLine;

/**
 * @author Ricardo Ewert
 *
 */
@CommandLine.Command(name = "generate-business-passenger-traffic", description = "Generate business passenger traffic model", showDefaultValues = true)
public class CreateSmallScaleCommercialTrafficDemand implements Callable<Integer> {

	private static final Logger log = LogManager.getLogger(CreateSmallScaleCommercialTrafficDemand.class);
	private static final Joiner JOIN = Joiner.on("\t");
	private static HashMap<String, HashMap<String, ArrayList<SimpleFeature>>> buildingsPerZone = new HashMap<>();
	private static Path shapeFileLandusePath = null;
	private static Path shapeFileZonePath = null;
	private static Path shapeFileBuildingsPath = null;
	private static List<Link> links = new ArrayList<Link>();
	private static Map<String, List<Link>> regionLinksMap = new HashMap<>();
	private static HashMap<String, ArrayList<String>> landuseCategoriesAndDataConnection = new HashMap<String, ArrayList<String>>();
	private static ShpOptions.Index indexLanduse = null;
	private static ShpOptions.Index indexZones = null;

	private enum LanduseConfiguration {
		useOnlyOSMLanduse, useOSMBuildingsAndLanduse, useExistingDataDistribution
	}

	private enum ZoneChoice {
		useDistricts, useTrafficCells
	}

	private enum ModeDifferentiation {
		createOneODMatrix, createSeperateODMatricesForModes
	}

	private enum TrafficType {
		businessTraffic, freightTraffic
	}

	@CommandLine.Parameters(arity = "1", paramLabel = "INPUT", description = "Path to the freight data directory", defaultValue = "../public-svn/matsim/scenarios/countries/de/berlin/projects/zerocuts/small-scale-commercial-traffic/input")
	private static Path inputDataDirectory;

	@CommandLine.Option(names = "--network", defaultValue = "../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz", description = "Path to desired network file", required = true)
	private static Path networkPath;

	@CommandLine.Option(names = "--sample", defaultValue = "0.01", description = "Scaling factor of the freight traffic (0, 1)", required = true)
	private double sample;

	@CommandLine.Option(names = "--output", description = "Path to output folder", required = true, defaultValue = "output/BusinessPassengerTraffic/")
	private Path output;

	@CommandLine.Option(names = "--jspritIterations", description = "Set number of jsprit iterations", required = true, defaultValue = "15")
	private static int jspritIterations;

	@CommandLine.Option(names = "--modeDifferentiation", defaultValue = "createOneODMatrix", description = "Set option of mode differentiation:  createOneODMatrix, createSeperateODMatricesForModes")
	private ModeDifferentiation usedModeDifferentiationForPassangerTraffic;

	@CommandLine.Option(names = "--useDistricts", defaultValue = "useDistricts", description = "Set option input zones. Options: useDistricts, useTrafficCells")
	private ZoneChoice usedZoneChoice;
// useDistricts, useTrafficCells
	@CommandLine.Option(names = "--landuseConfiguration", defaultValue = "useExistingDataDistribution", description = "Set option of used OSM data. Options: useOnlyOSMLanduse, useOSMBuildingsAndLanduse, useExistingDataDistribution")
	private LanduseConfiguration usedLanduseConfiguration;
// useOnlyOSMLanduse, useOSMBuildingsAndLanduse, useExistingDataDistribution

	@CommandLine.Option(names = "--trafficType", defaultValue = "freightTraffic", description = "Select traffic type. Options: commercialPassengerTraffic, freightTraffic")
	private TrafficType usedTrafficType;
// businessTraffic, freightTraffic
	private final static SplittableRandom rnd = new SplittableRandom(4711);

	public static void main(String[] args) {
		System.exit(new CommandLine(new CreateSmallScaleCommercialTrafficDemand()).execute(args));
	}

	@Override
	public Integer call() throws Exception {
		Configurator.setLevel("org.matsim.core.utils.geometry.geotools.MGC", Level.ERROR);
		/*
		 * Fragen: bei only landuse; was passiert mit construction?
		 */

		switch (usedZoneChoice) {
		case useDistricts:
			shapeFileZonePath = inputDataDirectory.resolve("shp").resolve("districts")
					.resolve("bezirksgrenzen_Berlin.shp");
			break;
		case useTrafficCells:
			shapeFileZonePath = inputDataDirectory.resolve("shp").resolve("districts")
					.resolve("verkehrszellen_Berlin.shp");
			break;
		default:
			break;
		}

		shapeFileLandusePath = inputDataDirectory.resolve("shp").resolve("landuse")
				.resolve("gis_osm_landuse_a_free_1.shp");

		shapeFileBuildingsPath = inputDataDirectory.resolve("shp").resolve("landuse")
				.resolve("allBuildingsWithLevels.shp");
// 		shapeFileBuildingsPath = rawDataDirectory.resolve("shp").resolve("landuse").resolve("buildingSample.shp");

		if (!Files.exists(shapeFileLandusePath)) {
			log.error("Required landuse shape file {} not found", shapeFileLandusePath);
		}
		if (!Files.exists(shapeFileBuildingsPath)) {
			log.error("Required OSM buildings shape file {} not found", shapeFileBuildingsPath);
		}
		if (!Files.exists(shapeFileZonePath)) {
			log.error("Required distrcits shape file {} not found", shapeFileZonePath);
		}
		output = output.resolve(java.time.LocalDate.now().toString() + "_" + java.time.LocalTime.now().toSecondOfDay());

		Config config = prepareConfig();

		prepareVehicles(config);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		HashMap<String, Object2DoubleMap<String>> resultingDataPerZone = createInputDataDistribution();

		ArrayList<String> modesORvehTypes;
		switch (usedTrafficType) {
		case businessTraffic:
			switch (usedModeDifferentiationForPassangerTraffic) {
			case createOneODMatrix:
				modesORvehTypes = new ArrayList<String>(Arrays.asList("total"));
				break;
			case createSeperateODMatricesForModes:
				modesORvehTypes = new ArrayList<String>(Arrays.asList("pt", "it", "op"));
				break;
			default:
				throw new RuntimeException("No mode differentiation selected.");
			}
			break;
		case freightTraffic:
			modesORvehTypes = new ArrayList<String>(
					Arrays.asList("vehTyp1", "vehTyp2", "vehTyp3", "vehTyp4", "vehTyp5"));
			break;
		default:
			throw new RuntimeException("No traffic type selected.");
		}

		TrafficVolumeGeneration.loadInputParamters(inputDataDirectory, usedTrafficType.toString());

		HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerTypeAndZone_start = TrafficVolumeGeneration
				.createTrafficVolume_start(resultingDataPerZone, output, inputDataDirectory, sample, modesORvehTypes);
		HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerTypeAndZone_stop = TrafficVolumeGeneration
				.createTrafficVolume_stop(resultingDataPerZone, output, inputDataDirectory, sample, modesORvehTypes);
		ShpOptions shpZones = new ShpOptions(shapeFileZonePath, null, StandardCharsets.UTF_8);
		final TripDistributionMatrix odMatrix = createTripDistribution(trafficVolumePerTypeAndZone_start,
				trafficVolumePerTypeAndZone_stop, shpZones);

		createCarriers(config, scenario, odMatrix, resultingDataPerZone, usedTrafficType.toString());
		Controler controler = prepareControler(scenario);

		FreightUtils.runJsprit(controler.getScenario());
		controler.run();
		new CarrierPlanXmlWriterV2((Carriers) controler.getScenario().getScenarioElement("carriers"))
				.write(config.controler().getOutputDirectory() + "/output_jspritCarriersWithPlans.xml");

		FreightAnalyse.main(new String[] { scenario.getConfig().controler().getOutputDirectory() });

		return 0;
	}

	/**
	 * @return
	 */
	private Config prepareConfig() {
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("EPSG:4326");
		config.network().setInputFile(networkPath.toString());
		config.controler().setOutputDirectory(output.toString());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(0);
		config.global().setRandomSeed(4177);
		new OutputDirectoryHierarchy(config.controler().getOutputDirectory(), config.controler().getRunId(),
				config.controler().getOverwriteFileSetting(), ControlerConfigGroup.CompressionType.gzip);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		new File(output.resolve("caculatedData").toString()).mkdir();
		return config;
	}

	/**
	 * Prepares the controller.
	 * 
	 * @param scenario
	 * @return
	 */
	private Controler prepareControler(Scenario scenario) {
		Controler controler = new Controler(scenario);

		Freight.configure(controler);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new CarrierModule());
			}
		});
		return controler;
	}

	/**
	 * @param config
	 * @param scenario
	 * @param odMatrix
	 * @param resultingDataPerZone
	 * @param trafficType
	 */
	private void createCarriers(Config config, Scenario scenario, TripDistributionMatrix odMatrix,
			HashMap<String, Object2DoubleMap<String>> resultingDataPerZone, String trafficType) {
		int maxNumberOfCarrier = odMatrix.getListOfPurposes().size() * odMatrix.getListOfZones().size()
				* odMatrix.getListOfModesOrVehTypes().size();
		int createdCarrier = 0;
		for (Integer purpose : odMatrix.getListOfPurposes()) {
			for (String startZone : odMatrix.getListOfZones()) {
				for (String modeORvehType : odMatrix.getListOfModesOrVehTypes()) {
					boolean isStartingLocation = false;
					checkIfIsStartingPosition: {
						for (String possibleStopZone : odMatrix.getListOfZones()) {
							if (!modeORvehType.equals("pt") && !modeORvehType.equals("op"))
								if (odMatrix.getTripDistributionValue(startZone, possibleStopZone, modeORvehType,
										purpose) != 0) {
									isStartingLocation = true;
									break checkIfIsStartingPosition;
								}
						}
					}
					if (isStartingLocation) {
						double occupancyRate = 0;
						String[] vehilceTypes = null;
						Integer serviceTimePerStop = null;
						ArrayList<String> startCategory = new ArrayList<String>();
						ArrayList<String> stopCategory = new ArrayList<String>();
						if (purpose == 1) {
							if (trafficType.equals("freightTraffic")) {
								occupancyRate = 1.;
							} else if (trafficType.equals("businessTraffic")) {
								vehilceTypes = new String[] { "vwCaddy" };
								serviceTimePerStop = (int) Math.round(71.7 * 60);
								occupancyRate = 1.5;
							}
							startCategory.add("Employee Secondary Sector Rest");
							stopCategory.add("Employee Secondary Sector Rest");
						} else if (purpose == 2) {
							if (trafficType.equals("freightTraffic")) {
								occupancyRate = 1.;
							} else if (trafficType.equals("businessTraffic")) {
								vehilceTypes = new String[] { "vwCaddy" };
								serviceTimePerStop = (int) Math.round(70.4 * 60); // Durschnitt aus Handel,Transp.,Einw.
								occupancyRate = 1.6;
							}
							startCategory.add("Employee Secondary Sector Rest");
							stopCategory.add("Employee Primary Sector");
							stopCategory.add("Employee Construction");
							stopCategory.add("Employee Secondary Sector Rest");
							stopCategory.add("Employee Retail");
							stopCategory.add("Employee Traffic/Parcels");
							stopCategory.add("Employee Tertiary Sector Rest");
							stopCategory.add("Inhabitants");
						} else if (purpose == 3) {
							if (trafficType.equals("freightTraffic")) {
								occupancyRate = 1.;
							} else if (trafficType.equals("businessTraffic")) {
								vehilceTypes = new String[] { "golf1.4" };
								serviceTimePerStop = (int) Math.round(70.4 * 60);
								occupancyRate = 1.2;
							}
							startCategory.add("Employee Retail");
							startCategory.add("Employee Tertiary Sector Rest");
							stopCategory.add("Employee Primary Sector");
							stopCategory.add("Employee Construction");
							stopCategory.add("Employee Secondary Sector Rest");
							stopCategory.add("Employee Retail");
							stopCategory.add("Employee Traffic/Parcels");
							stopCategory.add("Employee Tertiary Sector Rest");
							stopCategory.add("Inhabitants");
						} else if (purpose == 4) {
							if (trafficType.equals("freightTraffic")) {
								occupancyRate = 1.;
							} else if (trafficType.equals("businessTraffic")) {
								vehilceTypes = new String[] { "golf1.4" };
								serviceTimePerStop = (int) Math.round(100.6 * 60);
								occupancyRate = 1.2;
							}
							startCategory.add("Employee Traffic/Parcels");
							stopCategory.add("Employee Primary Sector");
							stopCategory.add("Employee Construction");
							stopCategory.add("Employee Secondary Sector Rest");
							stopCategory.add("Employee Retail");
							stopCategory.add("Employee Traffic/Parcels");
							stopCategory.add("Employee Tertiary Sector Rest");
							stopCategory.add("Inhabitants");
						} else if (purpose == 5) {
							if (trafficType.equals("freightTraffic")) {
								occupancyRate = 1.;
							} else if (trafficType.equals("businessTraffic")) {
								vehilceTypes = new String[] { "mercedes313" };
								serviceTimePerStop = (int) Math.round(214.7 * 60);
								occupancyRate = 1.7;
							}
							startCategory.add("Employee Construction");
							stopCategory.add("Employee Primary Sector");
							stopCategory.add("Employee Construction");
							stopCategory.add("Employee Secondary Sector Rest");
							stopCategory.add("Employee Retail");
							stopCategory.add("Employee Traffic/Parcels");
							stopCategory.add("Employee Tertiary Sector Rest");
							stopCategory.add("Inhabitants");
						} else if (purpose == 6) {
							occupancyRate = 1.;
							startCategory.add("Inhabitants");
							stopCategory.add("Employee Primary Sector");
							stopCategory.add("Employee Construction");
							stopCategory.add("Employee Secondary Sector Rest");
							stopCategory.add("Employee Retail");
							stopCategory.add("Employee Traffic/Parcels");
							stopCategory.add("Employee Tertiary Sector Rest");
							stopCategory.add("Inhabitants");
						}
						if (trafficType.equals("freightTraffic")) {
							if (modeORvehType.equals("vehTyp1")) {
								vehilceTypes = new String[] { "vwCaddy" }; // possible to add more types, see source
								serviceTimePerStop = (int) Math.round(120 * 60);
							} else if (modeORvehType.equals("vehTyp2")) {
								vehilceTypes = new String[] { "mercedes313" };
								serviceTimePerStop = (int) Math.round(150 * 60);
							} else if (modeORvehType.equals("vehTyp3")) {
								vehilceTypes = new String[] { "light8t" };
								serviceTimePerStop = (int) Math.round(120 * 60);
							} else if (modeORvehType.equals("vehTyp4")) {
								vehilceTypes = new String[] { "light8t" };
								serviceTimePerStop = (int) Math.round(75 * 60);
							} else if (modeORvehType.equals("vehTyp5")) {
								vehilceTypes = new String[] { "medium18t" };
								serviceTimePerStop = (int) Math.round(65 * 60);
							}
						}
						String selectedStartCategory = startCategory.get(rnd.nextInt(startCategory.size()));
						for (int i = 0; resultingDataPerZone.get(startZone).getDouble(selectedStartCategory) == 0
								&& i < startCategory.size() * 30; i++) {
							selectedStartCategory = startCategory.get(rnd.nextInt(startCategory.size()));
							if (i > startCategory.size() * 20)
								selectedStartCategory = stopCategory.get(rnd.nextInt(stopCategory.size()));
						} // TODO vielleicht besser lösen

						String carrierName;
						if (trafficType.equals("freightTraffic")) {
							carrierName = "Carrier_" + startZone + "_purpose_" + purpose + "_" + modeORvehType;
						} else
							carrierName = "Carrier_" + startZone + "_purpose_" + purpose;
						int numberOfDepots = (int) Math
								.ceil((double) odMatrix.getSumOfServicesForStartZone(startZone, modeORvehType, purpose)
										* serviceTimePerStop / (8 * 3600) * 2); // TODO
						FleetSize fleetSize = FleetSize.FINITE;
						int fixedNumberOfVehilcePerTypeAndLocation = 1;
						ArrayList<String> vehicleDepots = new ArrayList<String>();
						createdCarrier++;
						log.info("Create carrier number " + createdCarrier + " of a maximum Number of "
								+ maxNumberOfCarrier + " carriers.");
						log.info("Carrier: " + carrierName + "; depots: " + numberOfDepots + "; services: "
								+ (int) Math
										.ceil(odMatrix.getSumOfServicesForStartZone(startZone, modeORvehType, purpose)
												/ occupancyRate));
						createNewCarrierAndAddVehilceTypes(scenario, purpose, startZone,
								ConfigUtils.addOrGetModule(config, FreightConfigGroup.class), selectedStartCategory,
								carrierName, vehilceTypes, numberOfDepots, fleetSize,
								fixedNumberOfVehilcePerTypeAndLocation, vehicleDepots);
						log.info("Create services for carrier: " + carrierName);
						for (String stopZone : odMatrix.getListOfZones()) {
							int demand = 0;
							int numberOfJobs = (int) Math
									.ceil(odMatrix.getTripDistributionValue(startZone, stopZone, modeORvehType, purpose)
											/ occupancyRate);
							if (numberOfJobs == 0)
								continue;
							String selectedStopCategory = stopCategory.get(rnd.nextInt(stopCategory.size()));
							while (resultingDataPerZone.get(stopZone).getDouble(selectedStopCategory) == 0)
								selectedStopCategory = stopCategory.get(rnd.nextInt(stopCategory.size()));
							String[] serviceArea = new String[] { stopZone };
							TimeWindow serviceTimeWindow = TimeWindow.newInstance(6 * 3600, 20 * 3600);
							createServices(scenario, purpose, vehicleDepots, selectedStopCategory, carrierName, demand,
									numberOfJobs, serviceArea, serviceTimePerStop, serviceTimeWindow);
						}
					}
				}
			}
		}
		for (Carrier carrier : FreightUtils.getCarriers(scenario).getCarriers().values()) {
			CarrierUtils.setJspritIterations(carrier, jspritIterations);
		}
		log.warn("The jspritIterations are now set to " + jspritIterations + " in this simulation!");
		log.info("Finished creating " + createdCarrier + " carriers including related services.");

		new CarrierPlanXmlWriterV2(FreightUtils.addOrGetCarriers(scenario))
				.write(scenario.getConfig().controler().getOutputDirectory() + "/output_CarrierPlans.xml");
	}

	private void createServices(Scenario scenario, Integer purpose, ArrayList<String> noPossibleLinks,
			String selectedStopCategory, String carrierName, int demand, int numberOfJobs, String[] serviceArea,
			Integer serviceTimePerStop, TimeWindow serviceTimeWindow) {

		String stopZone = serviceArea[0];

		for (int i = 0; i < numberOfJobs; i++) {

			Id<Link> linkId = findPossibleLink(stopZone, selectedStopCategory, noPossibleLinks, scenario.getNetwork());
			Id<CarrierService> idNewService = Id.create(carrierName + "_" + linkId + "_" + rnd.nextInt(10000),
					CarrierService.class);

			CarrierService thisService = CarrierService.Builder.newInstance(idNewService, linkId)
					.setServiceDuration(serviceTimePerStop).setServiceStartTimeWindow(serviceTimeWindow).build();
			FreightUtils.getCarriers(scenario).getCarriers().get(Id.create(carrierName, Carrier.class)).getServices()
					.put(thisService.getId(), thisService);
		}

	}

	private void createNewCarrierAndAddVehilceTypes(Scenario scenario, Integer purpose, String startZone,
			FreightConfigGroup freightConfigGroup, String selectedStartCategory, String carrierName,
			String[] vehilceTypes, int numberOfDepots, FleetSize fleetSize, int fixedNumberOfVehilcePerTypeAndLocation,
			ArrayList<String> vehicleDepots) {

		Carriers carriers = FreightUtils.addOrGetCarriers(scenario);
		CarrierVehicleTypes carrierVehicleTypes = FreightUtils.getCarrierVehicleTypes(scenario);
		if (carrierVehicleTypes.getVehicleTypes().isEmpty()) {
			new CarrierVehicleTypeReader(carrierVehicleTypes)
					.readFile(freightConfigGroup.getCarriersVehicleTypesFile());
		} else
			carrierVehicleTypes = FreightUtils.getCarrierVehicleTypes(scenario);
		CarrierCapabilities carrierCapabilities = null;

		Carrier thisCarrier = CarrierUtils.createCarrier(Id.create(carrierName, Carrier.class));
		if (jspritIterations > 0)
			CarrierUtils.setJspritIterations(thisCarrier, jspritIterations);
		carrierCapabilities = CarrierCapabilities.Builder.newInstance().setFleetSize(fleetSize).build();
		carriers.addCarrier(thisCarrier);

		while (vehicleDepots.size() < numberOfDepots) {
			Id<Link> link = findPossibleLink(startZone, selectedStartCategory, null, scenario.getNetwork());
			vehicleDepots.add(link.toString());
		}
		for (String singleDepot : vehicleDepots) {
			for (String thisVehicleType : vehilceTypes) {
				int vehicleStartTime = rnd.nextInt(6 * 3600, 14 * 3600); // TODO Verteilung über den Tag prüfen
				int vehicleEndTime = vehicleStartTime + 8 * 3600;
				VehicleType thisType = carrierVehicleTypes.getVehicleTypes()
						.get(Id.create(thisVehicleType, VehicleType.class));
				if (fixedNumberOfVehilcePerTypeAndLocation == 0)
					fixedNumberOfVehilcePerTypeAndLocation = 1;
				for (int i = 0; i < fixedNumberOfVehilcePerTypeAndLocation; i++) {
					CarrierVehicle newCarrierVehicle = CarrierVehicle.Builder
							.newInstance(
									Id.create(
											thisType.getId().toString() + "_" + thisCarrier.getId().toString() + "_"
													+ (carrierCapabilities.getCarrierVehicles().size() + 1),
											Vehicle.class),
									Id.createLinkId(singleDepot), thisType)
							.setEarliestStart(vehicleStartTime).setLatestEnd(vehicleEndTime).build();
					carrierCapabilities.getCarrierVehicles().put(newCarrierVehicle.getId(), newCarrierVehicle);
					if (!carrierCapabilities.getVehicleTypes().contains(thisType))
						carrierCapabilities.getVehicleTypes().add(thisType);
				}
			}

			thisCarrier.setCarrierCapabilities(carrierCapabilities);
		}
	}

	private Id<Link> findPossibleLink(String zone, String selectedCategory, ArrayList<String> noPossibleLinks,
			Network network) {
		ShpOptions shpZones = new ShpOptions(shapeFileZonePath, "EPSG:4326", StandardCharsets.UTF_8);

		getIndexLanduse();
		getIndexZones();

		if (links.isEmpty()) {
			log.info("Filtering and assign links to zones. This take some time...");
			links = network.getLinks().values().stream().filter(l -> l.getAllowedModes().contains("car"))
					.collect(Collectors.toList());
			links.forEach(l -> l.getAttributes().putAttribute("newCoord",
					shpZones.createTransformation("EPSG:31468").transform(l.getCoord())));
			links.forEach(l -> l.getAttributes().putAttribute("zone",
					indexZones.query((Coord) l.getAttributes().getAttribute("newCoord"))));
			links = links.stream().filter(l -> l.getAttributes().getAttribute("zone") != null)
					.collect(Collectors.toList());
			links.forEach(l -> regionLinksMap
					.computeIfAbsent((String) l.getAttributes().getAttribute("zone"), (k) -> new ArrayList<>()).add(l));
		}
		if (buildingsPerZone.isEmpty()) {
			ShpOptions shpBuildings = new ShpOptions(shapeFileBuildingsPath, "EPSG:4326", StandardCharsets.UTF_8);
			List<SimpleFeature> buildingsFeatures = shpBuildings.readFeatures();
			analyzeBuildingType(buildingsFeatures);
		}
		Id<Link> newLink = null;
		for (int a = 0; newLink == null && a < buildingsPerZone.get(zone).get(selectedCategory).size() * 2; a++) {

			SimpleFeature possibleBuilding = buildingsPerZone.get(zone).get(selectedCategory)
					.get(rnd.nextInt(buildingsPerZone.get(zone).get(selectedCategory).size()));
			Coord centroidPointOfBuildingPolygon = MGC
					.point2Coord(((Geometry) possibleBuilding.getDefaultGeometry()).getCentroid());
			double minDistance = Double.MAX_VALUE;
			int numberOfPossibleLinks = regionLinksMap.get(zone).size();
//TODO eventuell auch opposite Links als noPossible deklarieren
			searchLink: for (Link possibleLink : regionLinksMap.get(zone)) {
				if (noPossibleLinks != null && numberOfPossibleLinks > noPossibleLinks.size())
					for (String depotLink : noPossibleLinks) {
						if (depotLink.equals(possibleLink.getId().toString()))
							continue searchLink;
					}
				double distance = NetworkUtils.getEuclideanDistance(centroidPointOfBuildingPolygon,
						(Coord) possibleLink.getAttributes().getAttribute("newCoord"));
				if (distance < minDistance) {
					newLink = possibleLink.getId();
					minDistance = distance;
				}
			}
		}
		if (newLink == null)
			throw new RuntimeException(
					"No possible link for buildings with type '" + selectedCategory + "' in zone '" + zone + "' found");
		return newLink;
	}

	/**
	 * @param categoriesOfBuilding
	 * @param buildingsFeatures
	 */
	private void analyzeBuildingType(List<SimpleFeature> buildingsFeatures) {
		int countOSMObjects = 0;
		log.info("Analyzing buildings types. This may take some time...");
		for (SimpleFeature singleBuildingFeature : buildingsFeatures) {
			countOSMObjects++;
			if (countOSMObjects % 10000 == 0)
				log.info("Investigate Building " + countOSMObjects + " of " + buildingsFeatures.size() + " buildings: "
						+ Math.round((double) countOSMObjects / buildingsFeatures.size() * 100) + " %");

			List<String> categoriesOfBuilding = new ArrayList<String>();
			String[] buildingTypes;
			Coord centroidPointOfBuildingPolygon = MGC
					.point2Coord(((Geometry) singleBuildingFeature.getDefaultGeometry()).getCentroid());
			String singleZone = indexZones.query(centroidPointOfBuildingPolygon);
			String buildingType = String.valueOf(singleBuildingFeature.getAttribute("type"));
			if (buildingType.equals("") || buildingType.equals("null")) {
				buildingType = indexLanduse.query(centroidPointOfBuildingPolygon);
				buildingTypes = new String[] { buildingType };
			} else {
				buildingType.replace(" ", "");
				buildingTypes = buildingType.split(";");
			}
			singleBuildingFeature.setAttribute("type", String.join(";", buildingTypes));
			for (String singleBuildingType : buildingTypes) {
				for (String category : landuseCategoriesAndDataConnection.keySet()) {
					if (landuseCategoriesAndDataConnection.get(category).contains(singleBuildingType)
							&& !categoriesOfBuilding.contains(category)) {
						categoriesOfBuilding.add(category);
					}
				}
			}
			if (singleZone != null) {
				categoriesOfBuilding.forEach(c -> buildingsPerZone
						.computeIfAbsent(singleZone, k -> new HashMap<String, ArrayList<SimpleFeature>>())
						.computeIfAbsent(c, k -> new ArrayList<SimpleFeature>()).add(singleBuildingFeature));
			}
		}
		log.info("Finished anlyzing buildings types.");
	}

	private void getIndexZones() {
		if (indexZones == null) {
			ShpOptions shpZones = new ShpOptions(shapeFileZonePath, "EPSG:4326", StandardCharsets.UTF_8);
			indexZones = shpZones.createIndex("EPSG:4326", "gml_id");
		}
	}

	private void getIndexLanduse() {
		if (indexLanduse == null) {
			ShpOptions shpLanduse = new ShpOptions(shapeFileLandusePath, "EPSG:4326", StandardCharsets.UTF_8);
			indexLanduse = shpLanduse.createIndex("EPSG:4326", "fclass");
		}
	}

	/**
	 * Reads in the vehicle types.
	 * 
	 * @param config
	 */
	private void prepareVehicles(Config config) {

		String vehicleTypesFileLocation = inputDataDirectory.resolve("vehicleTypes.xml").toString();
		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
		if (vehicleTypesFileLocation == "")
			throw new RuntimeException("No path to the vehicleTypes selected");
		else {
			freightConfigGroup.setCarriersVehicleTypesFile(vehicleTypesFileLocation);
			log.info("Get vehicleTypes from: " + vehicleTypesFileLocation);
		}
		FreightUtils.addOrGetCarriers(ScenarioUtils.loadScenario(config));
		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(carrierVehicleTypes).readFile(freightConfigGroup.getCarriersVehicleTypesFile());
	}

	/**
	 * Creates the number of trips between the zones for each mode and purpose.
	 * 
	 * @param trafficVolume_start
	 * @param trafficVolume_stop
	 * @param shpZones
	 * @param shapeFileZonesPath
	 * @return
	 * @throws MalformedURLException
	 * @throws UncheckedIOException
	 */
	private TripDistributionMatrix createTripDistribution(
			HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolume_start,
			HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolume_stop, ShpOptions shpZones)
			throws UncheckedIOException, MalformedURLException {

		final TripDistributionMatrix odMatrix = TripDistributionMatrix.Builder
				.newInstance(shpZones, trafficVolume_start, trafficVolume_stop, sample, usedTrafficType.toString())
				.build();

		int count = 0;

		for (String startZone : trafficVolume_start.keySet()) {
			count++;
			if (count % 50 == 0 || count == 1)
				log.info("Create OD pairs for start zone :" + startZone + ". Zone " + count + " of "
						+ trafficVolume_start.size());

			for (String modeORvehType : trafficVolume_start.get(startZone).keySet()) {
				for (Integer purpose : trafficVolume_start.get(startZone).get(modeORvehType).keySet()) {
					for (String stopZone : trafficVolume_stop.keySet()) {
						odMatrix.setTripDistributionValue(startZone, stopZone, modeORvehType, purpose);
					}
				}
			}
		}
		odMatrix.writeODMatrices(output);
		return odMatrix;
	}

	/**
	 * Creates a distribution of the given input data for each zone based on the
	 * used OSM data.
	 * 
	 * @param shapeFileZonePath
	 * @param shapeFileLandusePath
	 * @param shapeFileBuildingsPath
	 * @return
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private HashMap<String, Object2DoubleMap<String>> createInputDataDistribution()
			throws IOException, MalformedURLException {

		HashMap<String, Object2DoubleMap<String>> resultingDataPerZone = new HashMap<String, Object2DoubleMap<String>>();
		Path outputFileInOutputFolder = output.resolve("caculatedData").resolve("dataDistributionPerZone.csv");

		landuseCategoriesAndDataConnection.put("Inhabitants", new ArrayList<String>(Arrays.asList("residential",
				"apartments", "dormitory", "dwelling_house", "house", "retirement_home", "semidetached_house")));
		landuseCategoriesAndDataConnection.put("Employee Primary Sector",
				new ArrayList<String>(Arrays.asList("farmyard", "farmland", "farm", "farm_auxiliary", "greenhouse")));
		landuseCategoriesAndDataConnection.put("Employee Construction",
				new ArrayList<String>(Arrays.asList("construction")));
		landuseCategoriesAndDataConnection.put("Employee Secondary Sector Rest",
				new ArrayList<String>(Arrays.asList("industrial", "factory", "manufacture")));
		landuseCategoriesAndDataConnection.put("Employee Retail",
				new ArrayList<String>(Arrays.asList("retail", "kiosk", "mall", "shop", "supermarket")));
		landuseCategoriesAndDataConnection.put("Employee Traffic/Parcels", new ArrayList<String>(
				Arrays.asList("commercial", "post_office", "storage", "storage_tank", "warehouse")));
		landuseCategoriesAndDataConnection.put("Employee Tertiary Sector Rest", new ArrayList<String>(
				Arrays.asList("commercial", "embassy", "foundation", "government", "office", "townhall")));
		landuseCategoriesAndDataConnection.put("Employee", new ArrayList<String>());
		landuseCategoriesAndDataConnection.get("Employee")
				.addAll(landuseCategoriesAndDataConnection.get("Employee Primary Sector"));
		landuseCategoriesAndDataConnection.get("Employee")
				.addAll(landuseCategoriesAndDataConnection.get("Employee Construction"));
		landuseCategoriesAndDataConnection.get("Employee")
				.addAll(landuseCategoriesAndDataConnection.get("Employee Secondary Sector Rest"));
		landuseCategoriesAndDataConnection.get("Employee")
				.addAll(landuseCategoriesAndDataConnection.get("Employee Retail"));
		landuseCategoriesAndDataConnection.get("Employee")
				.addAll(landuseCategoriesAndDataConnection.get("Employee Traffic/Parcels"));
		landuseCategoriesAndDataConnection.get("Employee")
				.addAll(landuseCategoriesAndDataConnection.get("Employee Tertiary Sector Rest"));

		switch (usedLanduseConfiguration) {
		case useExistingDataDistribution:
			Path existingDataDistribution = inputDataDirectory.resolve("dataDistributionPerZone.csv");

			if (!Files.exists(existingDataDistribution)) {
				log.error("Required data per zone file {} not found", existingDataDistribution);
			}

			try (BufferedReader reader = IOUtils.getBufferedReader(existingDataDistribution.toString())) {
				CSVParser parse = CSVFormat.DEFAULT.withDelimiter('\t').withFirstRecordAsHeader().parse(reader);

				for (CSVRecord record : parse) {
					String zoneID = record.get("areaID");
					resultingDataPerZone.put(zoneID, new Object2DoubleOpenHashMap<>());
					for (int n = 1; n < parse.getHeaderMap().size(); n++) {
						resultingDataPerZone.get(zoneID).mergeDouble(parse.getHeaderNames().get(n),
								Double.valueOf(record.get(n)), Double::sum);
					}
				}
			}
			log.info("Data distribution for " + resultingDataPerZone.size() + " zones was imported from ",
					existingDataDistribution);
			Files.copy(existingDataDistribution, outputFileInOutputFolder, StandardCopyOption.COPY_ATTRIBUTES);
			break;

		default:

			log.info("New analyze for data distribution is started. The used method is: " + usedLanduseConfiguration);
			HashMap<String, Object2DoubleMap<String>> landuseCategoriesPerZone = new HashMap<String, Object2DoubleMap<String>>();
			createLanduseDistribution(landuseCategoriesPerZone);

			HashMap<String, HashMap<String, Integer>> investigationAreaData = new HashMap<String, HashMap<String, Integer>>();
			readAreaData(investigationAreaData);

			createResultingDataForLanduseInZones(landuseCategoriesPerZone, investigationAreaData, resultingDataPerZone);

			writeResultOfDataDistribution(resultingDataPerZone, outputFileInOutputFolder);
		}

		return resultingDataPerZone;
	}

	/**
	 * Method create the percentage for each land use category in each zone based on
	 * the sum of this category in all zones of the zone shape file
	 * 
	 * @param shapeFileLandusePath     Path to shape file with the land use
	 *                                 information
	 * @param shapeFileZonesPath       Path to shape file with the zone information
	 * @param landuseCategoriesPerZone
	 * @param shapeFileLandusePath
	 * @param shapeFileBuildingsPath
	 * 
	 */
	private void createLanduseDistribution(HashMap<String, Object2DoubleMap<String>> landuseCategoriesPerZone) {

		List<String> neededLanduseCategories = List.of("residential", "industrial", "commercial", "retail", "farmyard",
				"farmland", "construction");

		ShpOptions shpLanduse = new ShpOptions(shapeFileLandusePath, null, StandardCharsets.UTF_8);
		ShpOptions shpZones = new ShpOptions(shapeFileZonePath, null, StandardCharsets.UTF_8);

		List<SimpleFeature> landuseFeatures = shpLanduse.readFeatures();
		List<SimpleFeature> zonesFeatures = shpZones.readFeatures();

//		ShpOptions.Index indexLanduse = shpLanduse.createIndex("EPSG:4326", "fclass");
//		ShpOptions.Index indexZones = shpZones.createIndex("EPSG:4326", "gml_id");
		getIndexZones();
		getIndexLanduse();
		for (SimpleFeature districId : zonesFeatures) {
			Object2DoubleMap<String> landusePerCategory = new Object2DoubleOpenHashMap<>();
			landuseCategoriesPerZone.put((String) districId.getAttribute("gml_id"), landusePerCategory);
		}

		switch (usedLanduseConfiguration) {
		case useOSMBuildingsAndLanduse:

			ShpOptions shpBuildings = new ShpOptions(shapeFileBuildingsPath, null, StandardCharsets.UTF_8);
			List<SimpleFeature> buildingsFeatures = shpBuildings.readFeatures();
			analyzeBuildingType(buildingsFeatures);

			for (String zone : buildingsPerZone.keySet()) {
				for (String category : buildingsPerZone.get(zone).keySet()) {
					for (SimpleFeature building : buildingsPerZone.get(zone).get(category)) {
						String[] buildingTypes = ((String) building.getAttribute("type")).split(";");
						for (String singleCategoryOfBuilding : buildingTypes) {
							double buildingLevels;
							if (building.getAttribute("levels") == null)
								buildingLevels = 1;
							else
								buildingLevels = (long) building.getAttribute("levels") / buildingTypes.length;
							double area = (double) ((long) building.getAttribute("area")) * buildingLevels;

							landuseCategoriesPerZone.get(zone).mergeDouble(singleCategoryOfBuilding, area, Double::sum);
						}

					}
				}
			}
			break;
		case useOnlyOSMLanduse:
			for (SimpleFeature singleLanduseFeature : landuseFeatures) {
				if (!neededLanduseCategories.contains((String) singleLanduseFeature.getAttribute("fclass")))
					continue;
				Point centroidPointOfLandusePolygon = ((Geometry) singleLanduseFeature.getDefaultGeometry())
						.getCentroid();

				for (SimpleFeature singleZone : zonesFeatures) {
					if (((Geometry) singleZone.getDefaultGeometry()).contains(centroidPointOfLandusePolygon)) {
						landuseCategoriesPerZone.get(singleZone.getAttribute("gml_id")).mergeDouble(
								(String) singleLanduseFeature.getAttribute("fclass"),
								(double) singleLanduseFeature.getAttribute("area"), Double::sum);
						continue;
					}
				}
			}
			break;

		default:
			throw new RuntimeException("No possible option for the use of OSM data selected");
		}
	}

	/**
	 * Reads the input data for certain areas from the csv file.
	 * 
	 * @param areaDataPath
	 * @param areaData
	 * @throws IOException
	 */
	private void readAreaData(HashMap<String, HashMap<String, Integer>> areaData) throws IOException {

		Path areaDataPath = inputDataDirectory.resolve("investigationAreaData.csv");
		if (!Files.exists(areaDataPath)) {
			log.error("Required input data file {} not found", areaDataPath);
		}
		try (CSVParser parser = new CSVParser(Files.newBufferedReader(areaDataPath),
				CSVFormat.TDF.withFirstRecordAsHeader())) {

			for (CSVRecord record : parser) {
				HashMap<String, Integer> lookUpTable = new HashMap<>();
				for (String csvRecord : parser.getHeaderMap().keySet()) {
					if (parser.getHeaderMap().get(csvRecord) > 0)
						lookUpTable.put(csvRecord, Integer.valueOf(record.get(csvRecord)));
				}
				areaData.put(record.get("Area"), lookUpTable);
			}
		}
	}

	/**
	 * @param landuseCategoriesPerZone
	 * @param investigationAreaData
	 * @param resultingDataPerZone
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private void createResultingDataForLanduseInZones(
			HashMap<String, Object2DoubleMap<String>> landuseCategoriesPerZone,
			HashMap<String, HashMap<String, Integer>> investigationAreaData,
			HashMap<String, Object2DoubleMap<String>> resultingDataPerZone) throws MalformedURLException, IOException {

		Object2DoubleMap<String> totalSquareMetersPerCategory = new Object2DoubleOpenHashMap<>();
		Object2DoubleMap<String> totalEmployeesInCategoriesPerZone = new Object2DoubleOpenHashMap<>();
		Object2DoubleMap<String> totalEmployeesPerCategories = new Object2DoubleOpenHashMap<>();

		// connects the collected landuse data with the needed categories
		for (String zoneID : landuseCategoriesPerZone.keySet()) {
			resultingDataPerZone.put(zoneID, new Object2DoubleOpenHashMap<>());
			for (String categoryLanduse : landuseCategoriesPerZone.get(zoneID).keySet()) {
				for (String categoryData : landuseCategoriesAndDataConnection.keySet()) {
					if (landuseCategoriesAndDataConnection.get(categoryData).contains(categoryLanduse)) {
						double additionalArea = landuseCategoriesPerZone.get(zoneID).getDouble(categoryLanduse);
						resultingDataPerZone.get(zoneID).mergeDouble(categoryData, additionalArea, Double::sum);
						totalSquareMetersPerCategory.mergeDouble(categoryData, additionalArea, Double::sum);
					}
				}
			}
		}

		/*
		 * creates the percentages of each category and zones based on the sum in this
		 * category
		 */
		for (String zoneId : resultingDataPerZone.keySet()) {
			for (String categoryData : resultingDataPerZone.get(zoneId).keySet()) {
				double newValue = resultingDataPerZone.get(zoneId).getDouble(categoryData)
						/ totalSquareMetersPerCategory.getDouble(categoryData);
				resultingDataPerZone.get(zoneId).replace(categoryData,
						resultingDataPerZone.get(zoneId).getDouble(categoryData), newValue);
			}
		}
		// can be deleted or used as test
		Object2DoubleMap<String> checkPercentages = new Object2DoubleOpenHashMap<>();
		for (String landuseCategoriesForSingleZone : resultingDataPerZone.keySet()) {
			for (String category : resultingDataPerZone.get(landuseCategoriesForSingleZone).keySet()) {
				checkPercentages.mergeDouble(category,
						resultingDataPerZone.get(landuseCategoriesForSingleZone).getDouble(category), Double::sum);
			}
		}
		for (String category : checkPercentages.keySet()) {
			if (Math.abs(1 - checkPercentages.getDouble(category)) > 0.01)
				throw new RuntimeException("Sum of percenatges is not 1. For " + category + " the sum is "
						+ checkPercentages.getDouble(category) + "%");
		}
		// calculates the data per zone and category data
		for (String zoneId : resultingDataPerZone.keySet()) {
			for (String categoryData : resultingDataPerZone.get(zoneId).keySet()) {
				double percentageValue = resultingDataPerZone.get(zoneId).getDouble(categoryData);
				int inputDataForCategory = investigationAreaData.get("Berlin").get(categoryData);
				double resultingNumberPerCategory = percentageValue * inputDataForCategory;
				resultingDataPerZone.get(zoneId).replace(categoryData, percentageValue, resultingNumberPerCategory);
				totalEmployeesPerCategories.mergeDouble(categoryData, resultingNumberPerCategory, Double::sum);
				if (!categoryData.equals("Employee") && !categoryData.equals("Inhabitants"))
					totalEmployeesInCategoriesPerZone.mergeDouble(zoneId, resultingNumberPerCategory, Double::sum);

			}
		}
		// corrects the number of employees in the categories so that the sum is correct
		for (int i = 0; i < 30; i++) { // TODO perhaps find number of iterations
			for (String zoneId : resultingDataPerZone.keySet()) {
				for (String categoryData : resultingDataPerZone.get(zoneId).keySet()) {
					if (!categoryData.equals("Employee") && !categoryData.equals("Inhabitants")) {
						double correctionFactor = resultingDataPerZone.get(zoneId).getDouble("Employee")
								/ totalEmployeesInCategoriesPerZone.getDouble(zoneId);
						double resultingNumberPerCategory = correctionFactor
								* resultingDataPerZone.get(zoneId).getDouble(categoryData);
						resultingDataPerZone.get(zoneId).replace(categoryData,
								resultingDataPerZone.get(zoneId).getDouble(categoryData), resultingNumberPerCategory);
					}
				}
			}

			for (String categoryData : investigationAreaData.get("Berlin").keySet()) {
				for (String zoneId : resultingDataPerZone.keySet()) {
					if (!categoryData.equals("Employee") && !categoryData.equals("Inhabitants")) {
						double correctionFactor = investigationAreaData.get("Berlin").get(categoryData)
								/ totalEmployeesPerCategories.getDouble(categoryData);
						double resultingNumberPerCategory = correctionFactor
								* resultingDataPerZone.get(zoneId).getDouble(categoryData);
						resultingDataPerZone.get(zoneId).replace(categoryData,
								resultingDataPerZone.get(zoneId).getDouble(categoryData), resultingNumberPerCategory);
					}
				}
			}
			// update totals per sum becaus eof the changes before
			totalEmployeesInCategoriesPerZone.clear();
			totalEmployeesPerCategories.clear();
			for (String zoneId : resultingDataPerZone.keySet()) {
				for (String categoryData : resultingDataPerZone.get(zoneId).keySet()) {
					totalEmployeesPerCategories.mergeDouble(categoryData,
							resultingDataPerZone.get(zoneId).getDouble(categoryData), Double::sum);
					if (!categoryData.equals("Employee") && !categoryData.equals("Inhabitants"))
						totalEmployeesInCategoriesPerZone.mergeDouble(zoneId,
								resultingDataPerZone.get(zoneId).getDouble(categoryData), Double::sum);
				}
			}

		}
	}

	/**
	 * Writes a csv file with result of the distribution per zone of the input data.
	 * 
	 * @param resultingDataPerZone
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private void writeResultOfDataDistribution(HashMap<String, Object2DoubleMap<String>> resultingDataPerZone,
			Path outputFileInOutputFolder) throws IOException, MalformedURLException {

		writeCSVWithCategoryHeader(resultingDataPerZone, outputFileInOutputFolder);
		log.info("The data distribution is finished and written to: " + outputFileInOutputFolder);
	}

	/**
	 * @param resultingDataPerZone
	 * @param outputFileInInputFolder
	 * @throws MalformedURLException
	 */
	private void writeCSVWithCategoryHeader(HashMap<String, Object2DoubleMap<String>> resultingDataPerZone,
			Path outputFileInInputFolder) throws MalformedURLException {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFileInInputFolder.toUri().toURL(),
				StandardCharsets.UTF_8, true);
		try {
			String[] header = new String[] { "areaID", "Inhabitants", "Employee", "Employee Primary Sector",
					"Employee Construction", "Employee Secondary Sector Rest", "Employee Retail",
					"Employee Traffic/Parcels", "Employee Tertiary Sector Rest" };
			JOIN.appendTo(writer, header);
			writer.write("\n");
			for (String zone : resultingDataPerZone.keySet()) {
				List<String> row = new ArrayList<>();
				row.add(zone);
				for (String category : header) {
					if (!category.equals("areaID"))
						row.add(String.valueOf((int) Math.round(resultingDataPerZone.get(zone).getDouble(category))));
				}
				JOIN.appendTo(writer, row);
				writer.write("\n");
			}

			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
