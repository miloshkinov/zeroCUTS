/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package org.matsim.vsp.SmallScaleFreightTraffic;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

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
import org.matsim.application.options.ShpOptions.Index;
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
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vsp.freightAnalysis.FreightAnalyse;
import org.opengis.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Geometry;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import picocli.CommandLine;

/**
 * @author Ricardo Ewert
 *
 */
@CommandLine.Command(name = "generate-business-passenger-traffic", description = "Generate business passenger traffic model", showDefaultValues = true)
public class CreateSmallScaleCommercialTrafficDemand implements Callable<Integer> {

	private static final Logger log = LogManager.getLogger(CreateSmallScaleCommercialTrafficDemand.class);
	private static HashMap<String, HashMap<String, ArrayList<SimpleFeature>>> buildingsPerZone = new HashMap<>();
	private static Path shapeFileLandusePath = null;
	private static Path shapeFileZonePath = null;
	private static Path shapeFileBuildingsPath = null;
	private static List<Link> links = new ArrayList<Link>();
	private static Map<String, List<Link>> regionLinksMap = new HashMap<>();
	private static HashMap<String, ArrayList<String>> landuseCategoriesAndDataConnection = new HashMap<String, ArrayList<String>>();

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

	@CommandLine.Option(names = "--zoneChoice", defaultValue = "useTrafficCells", description = "Set option input zones. Options: useDistricts, useTrafficCells")
	private ZoneChoice usedZoneChoice;
// useDistricts, useTrafficCells
	@CommandLine.Option(names = "--landuseConfiguration", defaultValue = "useOSMBuildingsAndLanduse", description = "Set option of used OSM data. Options: useOnlyOSMLanduse, useOSMBuildingsAndLanduse, useExistingDataDistribution")
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
			shapeFileZonePath = inputDataDirectory.resolve("shp").resolve("zones").resolve("bezirksgrenzen_Berlin.shp");
			break;
		case useTrafficCells:
			shapeFileZonePath = inputDataDirectory.resolve("shp").resolve("berlinBrandenburg")
					.resolve("berlinBrandenburg_Zones_4326.shp");
			break;
		default:
			break;
		}

//		shapeFileLandusePath = inputDataDirectory.resolve("shp").resolve("landuse")
//				.resolve("gis_osm_landuse_a_free_1.shp");
		shapeFileLandusePath = inputDataDirectory.resolve("shp").resolve("berlinBrandenburg")
				.resolve("berlinBranburg_landuse_4326.shp");

//		shapeFileBuildingsPath = inputDataDirectory.resolve("shp").resolve("landuse")
//				.resolve("allBuildingsWithLevels.shp");
		shapeFileBuildingsPath = inputDataDirectory.resolve("shp").resolve("berlinBrandenburg")
				.resolve("buildingSample_BB.shp");
//		shapeFileBuildingsPath = inputDataDirectory.resolve("shp").resolve("berlinBrandenburg")
//				.resolve("gis_osm_buildings_a_free_1.shp");
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

		HashMap<String, Object2DoubleMap<String>> resultingDataPerZone = LanduseBuildingAnalysis
				.createInputDataDistribution(output, landuseCategoriesAndDataConnection, inputDataDirectory,
						usedLanduseConfiguration.toString(), shapeFileLandusePath, shapeFileZonePath,
						shapeFileBuildingsPath, buildingsPerZone);

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
	 * Creates the carriers and the related demand, based on the generated
	 * TripDistributionMatrix.
	 * 
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

	/**
	 * Creates the services for one carrier.
	 * 
	 * @param scenario
	 * @param purpose
	 * @param noPossibleLinks
	 * @param selectedStopCategory
	 * @param carrierName
	 * @param demand
	 * @param numberOfJobs
	 * @param serviceArea
	 * @param serviceTimePerStop
	 * @param serviceTimeWindow
	 */
	private void createServices(Scenario scenario, Integer purpose, ArrayList<String> noPossibleLinks,
			String selectedStopCategory, String carrierName, int demand, int numberOfJobs, String[] serviceArea,
			Integer serviceTimePerStop, TimeWindow serviceTimeWindow) {

		String stopZone = serviceArea[0];

		for (int i = 0; i < numberOfJobs; i++) {

			Id<Link> linkId = findPossibleLink(stopZone, selectedStopCategory, noPossibleLinks);
			Id<CarrierService> idNewService = Id.create(carrierName + "_" + linkId + "_" + rnd.nextInt(10000),
					CarrierService.class);

			CarrierService thisService = CarrierService.Builder.newInstance(idNewService, linkId)
					.setServiceDuration(serviceTimePerStop).setServiceStartTimeWindow(serviceTimeWindow).build();
			FreightUtils.getCarriers(scenario).getCarriers().get(Id.create(carrierName, Carrier.class)).getServices()
					.put(thisService.getId(), thisService);
		}

	}

	/**
	 * Creates the carrier and the related vehicles.
	 * 
	 * @param scenario
	 * @param purpose
	 * @param startZone
	 * @param freightConfigGroup
	 * @param selectedStartCategory
	 * @param carrierName
	 * @param vehilceTypes
	 * @param numberOfDepots
	 * @param fleetSize
	 * @param fixedNumberOfVehilcePerTypeAndLocation
	 * @param vehicleDepots
	 */
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
			Id<Link> link = findPossibleLink(startZone, selectedStartCategory, null);
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

	/**
	 * Finds a possible link for a service or the vehicle location.
	 * 
	 * @param zone
	 * @param selectedCategory
	 * @param noPossibleLinks
	 * @return
	 */
	private Id<Link> findPossibleLink(String zone, String selectedCategory, ArrayList<String> noPossibleLinks) {
		ShpOptions shpZones = new ShpOptions(shapeFileZonePath, "EPSG:4326", StandardCharsets.UTF_8);

//		Index indexLanduse = SmallScaleFreightTrafficUtils.getIndexLanduse(shapeFileLandusePath);
		Index indexZones = SmallScaleFreightTrafficUtils.getIndexZones(shapeFileZonePath);

		if (links.isEmpty()) {
			log.info("Filtering and assign links to zones. This take some time...");
			Network networkToChange = NetworkUtils.readNetwork(networkPath.toString());
			links = networkToChange.getLinks().values().stream().filter(l -> l.getAllowedModes().contains("car"))
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
			LanduseBuildingAnalysis.analyzeBuildingType(buildingsFeatures, buildingsPerZone,
					landuseCategoriesAndDataConnection, shapeFileLandusePath, indexZones);
		}
		Id<Link> newLink = null;
		for (int a = 0; newLink == null && a < buildingsPerZone.get(zone).get(selectedCategory).size() * 2; a++) {

			SimpleFeature possibleBuilding = buildingsPerZone.get(zone).get(selectedCategory)
					.get(rnd.nextInt(buildingsPerZone.get(zone).get(selectedCategory).size()));
			Coord centroidPointOfBuildingPolygon = MGC
					.point2Coord(((Geometry) possibleBuilding.getDefaultGeometry()).getCentroid());
			double minDistance = Double.MAX_VALUE;
			int numberOfPossibleLinks = regionLinksMap.get(zone.replaceFirst(zone.split("_")[0] + "_", "")).size();
//TODO eventuell auch opposite Links als noPossible deklarieren
			searchLink: for (Link possibleLink : regionLinksMap.get(zone.replaceFirst(zone.split("_")[0] + "_", ""))) {
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
	 * Reads the vehicle types.
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
	 * @return TripDistributionMatrix
	 * @throws UncheckedIOException
	 * @throws MalformedURLException
	 */
	private TripDistributionMatrix createTripDistribution(
			HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolume_start,
			HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolume_stop, ShpOptions shpZones)
			throws UncheckedIOException, MalformedURLException {

		final TripDistributionMatrix odMatrix = TripDistributionMatrix.Builder
				.newInstance(shpZones, trafficVolume_start, trafficVolume_stop, sample).build();

		int count = 0;

		for (String startZone : trafficVolume_start.keySet()) {
			count++;
			if (count % 50 == 0 || count == 1)
				log.info("Create OD pair " + count + " of " + trafficVolume_start.size());

			for (String modeORvehType : trafficVolume_start.get(startZone).keySet())
				for (Integer purpose : trafficVolume_start.get(startZone).get(modeORvehType).keySet())
					for (String stopZone : trafficVolume_stop.keySet()) {
						odMatrix.setTripDistributionValue(startZone, stopZone, modeORvehType, purpose);
					}
		}
		odMatrix.writeODMatrices(output);
		return odMatrix;
	}
}
