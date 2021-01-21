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

package org.matsim.vsp.demandGeneration;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import javax.management.InvalidAttributeValueException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.Freight;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierUtils;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
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
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.opengis.feature.simple.SimpleFeature;

/**
 * @author: rewert TODO
 */

public class GeneralDemandGeneration {

	private static final Logger log = LogManager.getLogger(GeneralDemandGeneration.class);

	private static final String inputBerlinNetwork = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";
	private static final String inputGridNetwork = "https://raw.githubusercontent.com/matsim-org/matsim/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml";

	private enum NetworkChoice {
		grid9x9, berlinNetwork, otherNetwork
	};

	private enum CarrierInputOptions {
		createNewCarrierAndAddVehicleTypes, readCarrierFile, createFromCSV
	};

	private enum VehicleInputOptions {
		createNewVehicles, readVehicleFile
	};

	private enum DemandGenerationOptions {
		generateServices, generateShipments, useDemandFromUsedCarrierFile, getDataForGenerationFromCSV
	};

	private enum OptionsOfVRPSolutions {
		runJspritAndMATSim, onlyRunJsprit, createNoSolutionAndOnlyWriteCarrierFile
	};

	public static void main(String[] args) throws IOException, InvalidAttributeValueException {
		NetworkChoice selectedNetwork = null;
		CarrierInputOptions selectedCarrierInputOption = null;
		VehicleInputOptions selectedVehicleInputOption = null;
		DemandGenerationOptions selectedDemandGenerationOption = null;
		OptionsOfVRPSolutions selectedSolution = null;

// create and prepare MATSim config
		String outputLocation = "output/demandGeneration/Test1";
		int lastMATSimIteration = 0;
		String coordinateSystem = TransformationFactory.GK4; // TODO perhaps add transformation shape/network
		Config config = prepareConfig(lastMATSimIteration, outputLocation, coordinateSystem);

		log.info("Starting class to create a freight scenario");

// select network configurations
		selectedNetwork = NetworkChoice.berlinNetwork;
		String networkPathOfOtherNetwork = "";
		boolean usingNetworkChangeEvents = false;
		String networkChangeEventsFilePath = "";
		setNetworkAndNetworkChangeEvents(config, selectedNetwork, networkPathOfOtherNetwork, usingNetworkChangeEvents,
				networkChangeEventsFilePath);

// load or create carrierVehicle
		selectedVehicleInputOption = VehicleInputOptions.readVehicleFile;
		String vehicleTypesFileLocation = "scenarios/demandGeneration/testInput/vehicleTypes_default.xml";
		prepareVehicles(config, selectedVehicleInputOption, vehicleTypesFileLocation);

// load or create carrier
		selectedCarrierInputOption = CarrierInputOptions.createFromCSV;
		Scenario scenario = ScenarioUtils.loadScenario(config);
		String carriersFileLocation = "scenarios/demandGeneration/testInput/carrier_berlin_withDemand.xml";
		String csvLocation = "scenarios/demandGeneration/testInput/testCarrierCSV.csv";

		Set<NewCarrier> allNewCarrier = new HashSet<>();
		allNewCarrier.add(new NewCarrier("csv2", new String[] { "medium18t_electro" },
				new String[] { "i(5,1)R", "i(5,9)R" }, FleetSize.INFINITE, 0, 50000));
		allNewCarrier.add(new NewCarrier("csv2", new String[] { "medium18t" }, new String[] { "i(5,1)R" },
				FleetSize.INFINITE, 2000, 20000));

		prepareCarrier(scenario, selectedCarrierInputOption, carriersFileLocation, allNewCarrier, csvLocation);

// create the demand
		// *distributeServicesOverAllLinks*:
		// distributes a demand proportional to the lengths of all links in the network;
		// if

		// *amountOfJobs* sets the number of jobs to be generated; if the amount should
		// be result of the simulation set it to 0; if a amount of jobs is set the
		// demand will evenly distributed
		//
		// remarks:
		// - if existing carrier was read and jobs can be added to this carrier
		// -
		// TODO add possible locations characteristics of links, speed, type etc.
		// TODO perhaps add iterations to csv
		String shapeFileLocation = "scenarios/wasteCollection/garbageInput/districtsWithGarbageInformations.shp";
		boolean demandLocationsInShape = true;

		for (NewCarrier newCarrier : allNewCarrier) {
			if (newCarrier.getName().contains("csv2")) {
				newCarrier.setNumberOfJobs(3);
				newCarrier.setDemandToDistribute(0);
				newCarrier.setServiceTimePerUnit(300);
				newCarrier.setAreasForTheDemand(new String[] { "Mahlsdorf" });
			}
		}

		// TODO check options (generateService and createFromCSV)
		selectedDemandGenerationOption = DemandGenerationOptions.generateServices;
		createDemand(selectedDemandGenerationOption, scenario, allNewCarrier, demandLocationsInShape,
				shapeFileLocation);

// prepare the VRP and get a solution
		selectedSolution = OptionsOfVRPSolutions.runJspritAndMATSim;
		int nuOfJspritIteration = 10;
		boolean usingRangeRestriction = false;
		Controler controler = prepareControler(scenario);

		solveSelectedSolution(selectedSolution, config, nuOfJspritIteration, usingRangeRestriction, controler);

// analyze results
		String[] argsAnalysis = { config.controler().getOutputDirectory() };
		FreightAnalyse.main(argsAnalysis);
		// TODO add service/Shipment ID to events to also analyze the jobs or add the
		// carrierFile to the analyzes

		log.info("Finished");
	}

	/**
	 * @param selectedSolution
	 * @param config
	 * @param nuOfJspritIteration
	 * @param usingRangeRestriction
	 * @param controler
	 * @throws InvalidAttributeValueException
	 */
	private static void solveSelectedSolution(OptionsOfVRPSolutions selectedSolution, Config config,
			int nuOfJspritIteration, boolean usingRangeRestriction, Controler controler)
			throws InvalidAttributeValueException {
		switch (selectedSolution) {
		case runJspritAndMATSim:
			runJsprit(controler, nuOfJspritIteration, usingRangeRestriction);
			controler.run();
			break;
		case onlyRunJsprit:
			runJsprit(controler, nuOfJspritIteration, usingRangeRestriction);
			new CarrierPlanXmlWriterV2((Carriers) controler.getScenario().getScenarioElement("carriers"))
					.write(config.controler().getOutputDirectory() + "/output_carriersNoPlans.xml");
			log.warn(
					"##Finished with the jsprit solution. If you also want to run MATSim, please change  case of optionsOfVRPSolutions");
			System.exit(0);
			// TODO find perhaps better solution
			break;
		case createNoSolutionAndOnlyWriteCarrierFile:
			new CarrierPlanXmlWriterV2((Carriers) controler.getScenario().getScenarioElement("carriers"))
					.write(config.controler().getOutputDirectory() + "/output_carriersNoPlans.xml");
			log.warn(
					"##Finished without solution of the VRP. If you also want to run jsprit and/or MATSim, please change  case of optionsOfVRPSolutions");
			System.exit(0);
			// TODO find perhaps better solution
			break;
		default:
			break;
		}
	}

	/**
	 * @param selectedDemandGenerationOption
	 * @param scenario
	 * @param allNewCarrier
	 * @param demandLocationsInShape
	 * @param shapeFileLocation
	 * @throws MalformedURLException
	 */
	private static void createDemand(DemandGenerationOptions selectedDemandGenerationOption, Scenario scenario,
			Set<NewCarrier> allNewCarrier, boolean demandLocationsInShape, String shapeFileLocation)
			throws MalformedURLException {

		Collection<SimpleFeature> polygonsInShape = null;
		if (demandLocationsInShape)
			polygonsInShape = ShapeFileReader.getAllFeatures(shapeFileLocation);
		switch (selectedDemandGenerationOption) {
		case generateServices:
			createServices(scenario, allNewCarrier, demandLocationsInShape, polygonsInShape);
			break;
		case generateShipments:
//			createServicesOverAllLinks(scenario, demandToDistribute, serviceTimePerUnit);
			break;
		case useDemandFromUsedCarrierFile:
			boolean oneCarrierHasJobs = false;
			for (Carrier carrier : FreightUtils.getCarriers(scenario).getCarriers().values()) {
				if (carrier.getServices().isEmpty() && carrier.getShipments().isEmpty()) {
					log.warn(carrier.getId().toString() + " has no jobs which can be used");
				} else {
					oneCarrierHasJobs = true;
					log.info("Used the demand of the carrier " + carrier.getId().toString() + " from the carrierFile!");
				}
			}
			if (!oneCarrierHasJobs)
				throw new RuntimeException("Minimum one carrier has no jobs");
			break;
		default:
			break;
		}
	}

	/**
	 * @param scenario
	 * @param allNewCarrier
	 * @param demandLocationsInShape
	 * @param singlePolygons
	 * @param polygonsInShape
	 * @throws MalformedURLException
	 */
	private static void createServices(Scenario scenario, Set<NewCarrier> allNewCarrier, boolean demandLocationsInShape,
			Collection<SimpleFeature> polygonsInShape) {

		int linksInNetwork = scenario.getNetwork().getLinks().size();
		for (NewCarrier singleCarrier : allNewCarrier) {
			int countOfLinks = 1;
			int distributedDemand = 0;
			double roundingError = 0;
			double sumOfLinkLenght = 0;
			int numberOfJobs = singleCarrier.getNumberOfJobs();
			int demandToDistribute = singleCarrier.getDemandToDistribute();
			int count = 0;

			if (demandToDistribute == Integer.MAX_VALUE)
				continue;

			if (numberOfJobs == Integer.MAX_VALUE) {
				if (scenario.getNetwork().getLinks().size() > demandToDistribute) {

					for (int i = 0; i < demandToDistribute; i++) {
						count++;
						if (count > linksInNetwork)
							throw new RuntimeException("Not enough links in the shape file to distribute the demand");
						Random rand = new Random();
						// TODO check if a twice selection of a link is possible
						Link link = scenario.getNetwork().getLinks().values().stream()
								.skip(rand.nextInt(scenario.getNetwork().getLinks().size())).findFirst().get();
						if (!link.getId().toString().contains("pt") && (!demandLocationsInShape
								|| checkPositionInShape(link, polygonsInShape, singleCarrier.getAreasForTheDemand()))) {
							double serviceTime = singleCarrier.getServiceTimePerUnit();
							int demandForThisLink = 1;
							CarrierService thisService = CarrierService.Builder
									.newInstance(Id.create("Service_" + link.getId(), CarrierService.class),
											link.getId())
									.setCapacityDemand(demandForThisLink).setServiceDuration(serviceTime)
									.setServiceStartTimeWindow(singleCarrier.getServiceTimeWindow()).build();
							FreightUtils.getCarriers(scenario).getCarriers()
									.get(Id.create(singleCarrier.getName(), Carrier.class)).getServices()
									.put(thisService.getId(), thisService);
						} else
							i = i - 1;
					}
				} else {
					for (Link link : scenario.getNetwork().getLinks().values()) {
						if (!link.getId().toString().contains("pt") && (!demandLocationsInShape
								|| checkPositionInShape(link, polygonsInShape, singleCarrier.getAreasForTheDemand())))
							sumOfLinkLenght = sumOfLinkLenght + link.getLength();
					}
					if (sumOfLinkLenght == 0)
						throw new RuntimeException(
								"Not enough links in the shape file to distribute the demand. Select an different shapefile or check if shapefile and network has the same coordinateSystem.");

					for (Link link : scenario.getNetwork().getLinks().values()) {
						if (!link.getId().toString().contains("pt") && (!demandLocationsInShape
								|| checkPositionInShape(link, polygonsInShape, singleCarrier.getAreasForTheDemand()))) {
							int demandForThisLink;
							if (countOfLinks == scenario.getNetwork().getLinks().size()) {
								demandForThisLink = demandToDistribute - distributedDemand;
							} else {
								demandForThisLink = (int) Math
										.ceil(link.getLength() / sumOfLinkLenght * demandToDistribute);
								roundingError = roundingError + (demandForThisLink
										- (link.getLength() / sumOfLinkLenght * demandToDistribute));
								if (roundingError > 1) {
									demandForThisLink = demandForThisLink - 1;
									roundingError = roundingError - 1;
								}
								countOfLinks++;
							}
							double serviceTime = singleCarrier.getServiceTimePerUnit() * demandForThisLink;
							if (demandToDistribute > 0 && demandForThisLink > 0) {
								CarrierService thisService = CarrierService.Builder
										.newInstance(Id.create("Service_" + link.getId(), CarrierService.class),
												link.getId())
										.setCapacityDemand(demandForThisLink).setServiceDuration(serviceTime)
										.setServiceStartTimeWindow(singleCarrier.getServiceTimeWindow()).build();
								FreightUtils.getCarriers(scenario).getCarriers().values().iterator().next()
										.getServices().put(thisService.getId(), thisService);
							} else if (demandToDistribute == 0) {
								CarrierService thisService = CarrierService.Builder
										.newInstance(Id.create("Service_" + link.getId(), CarrierService.class),
												link.getId())
										.setServiceDuration(serviceTime)
										.setServiceStartTimeWindow(singleCarrier.getServiceTimeWindow()).build();
								FreightUtils.getCarriers(scenario).getCarriers()
										.get(Id.create(singleCarrier.getName(), Carrier.class)).getServices()
										.put(thisService.getId(), thisService);
							}
							distributedDemand = distributedDemand + demandForThisLink;
						}
					}
				}
			} else {
				// if a certain amount of services is selected
				for (int i = 0; i < numberOfJobs; i++) {
					count++;
					if (count > linksInNetwork)
						throw new RuntimeException(
								"Not enough links in the shape file to distribute the demand. Select an different shapefile or check if shapefile and network has the same coordinateSystem.");
					Random rand = new Random();
					// TODO check if a twice selection of a link is possible
					Link link = scenario.getNetwork().getLinks().values().stream()
							.skip(rand.nextInt(scenario.getNetwork().getLinks().size())).findFirst().get();
					if (!link.getId().toString().contains("pt") && (!demandLocationsInShape
							|| checkPositionInShape(link, polygonsInShape, singleCarrier.getAreasForTheDemand()))) {
						int demandForThisLink = (int) Math.ceil(demandToDistribute / numberOfJobs);
						if (numberOfJobs == (i + 1)) {
							demandForThisLink = demandToDistribute - distributedDemand;
						} else {
							roundingError = roundingError + (demandForThisLink - (demandToDistribute / numberOfJobs));
							if (roundingError > 1) {
								demandForThisLink = demandForThisLink - 1;
								roundingError = roundingError - 1;
							}
						}
						double serviceTime = demandForThisLink * singleCarrier.getServiceTimePerUnit();

						CarrierService thisService = CarrierService.Builder
								.newInstance(Id.create("Service_" + link.getId(), CarrierService.class), link.getId())
								.setCapacityDemand(demandForThisLink).setServiceDuration(serviceTime)
								.setServiceStartTimeWindow(singleCarrier.getServiceTimeWindow()).build();
						FreightUtils.getCarriers(scenario).getCarriers()
								.get(Id.create(singleCarrier.getName(), Carrier.class)).getServices()
								.put(thisService.getId(), thisService);

						distributedDemand = distributedDemand + demandForThisLink;
					} else
						i = i - 1;
				}
			}
		}
	}

	/**
	 * @param link
	 * @param strings
	 * @param polygonsInShape2
	 * @return
	 * @throws MalformedURLException
	 */
	private static boolean checkPositionInShape(Link link, Collection<SimpleFeature> polygonsInShape,
			String[] areasOfDemand) {
		boolean isInShape = false;

		double x, y, xCoordFrom, xCoordTo, yCoordFrom, yCoordTo;

		xCoordFrom = link.getFromNode().getCoord().getX();
		xCoordTo = link.getToNode().getCoord().getX();
		yCoordFrom = link.getFromNode().getCoord().getY();
		yCoordTo = link.getToNode().getCoord().getY();
		Point p;
		if (xCoordFrom > xCoordTo)
			x = xCoordFrom - ((xCoordFrom - xCoordTo) / 2);
		else
			x = xCoordTo - ((xCoordTo - xCoordFrom) / 2);
		if (yCoordFrom > yCoordTo)
			y = yCoordFrom - ((yCoordFrom - yCoordTo) / 2);
		else
			y = yCoordTo - ((yCoordTo - yCoordFrom) / 2);
		p = MGC.xy2Point(x, y);
		for (SimpleFeature singlePolygon : polygonsInShape) {
			for (String area : areasOfDemand) {
				if (area.equals(singlePolygon.getAttribute("Ortsteil")))
					if (((Geometry) singlePolygon.getDefaultGeometry()).contains(p)) {
						isInShape = true;
						return isInShape;
					}
			}
		}
		return isInShape;
	}

	/**
	 * @param config
	 * @param networkChoice
	 * @param networkPathOfOtherNetwork
	 * @param usingNetworkChangeEvents
	 * @param networkChangeEventsFilePath
	 * @throws RuntimeException
	 */
	private static void setNetworkAndNetworkChangeEvents(Config config, NetworkChoice networkChoice,
			String networkPathOfOtherNetwork, boolean usingNetworkChangeEvents, String networkChangeEventsFilePath)
			throws RuntimeException {

		switch (networkChoice) {
		case grid9x9:
			config.network().setInputFile(inputGridNetwork);
			log.info("The following input network is selected: 9x9 grid network");
			if (usingNetworkChangeEvents) {
				if (networkChangeEventsFilePath == "")
					throw new RuntimeException("no networkChangeEvents file is selected.");
				log.info("Setting networkChangeEventsInput file: " + networkChangeEventsFilePath);
				config.network().setTimeVariantNetwork(true);
				config.network().setChangeEventsInputFile(networkChangeEventsFilePath);
			}
			break;
		case berlinNetwork:
			config.network().setInputFile(inputBerlinNetwork);
			log.info("The following input network is selected: Open Berlin network");
			if (usingNetworkChangeEvents) {
				if (networkChangeEventsFilePath == "")
					throw new RuntimeException("no networkChangeEvents file is selected.");
				log.info("Setting networkChangeEventsInput file: " + networkChangeEventsFilePath);
				config.network().setTimeVariantNetwork(true);
				config.network().setChangeEventsInputFile(networkChangeEventsFilePath);
			}
			break;
		case otherNetwork:
			if (networkPathOfOtherNetwork == "")
				throw new RuntimeException("no network selected.");
			else {
				config.network().setInputFile(networkPathOfOtherNetwork);
				log.info("The following input network is selected: imported network from " + networkPathOfOtherNetwork);
				if (usingNetworkChangeEvents) {
					if (networkChangeEventsFilePath == "")
						throw new RuntimeException("no networkChangeEvents file is selected.");
					log.info("Setting networkChangeEventsInput file: " + networkChangeEventsFilePath);
					config.network().setTimeVariantNetwork(true);
					config.network().setChangeEventsInputFile(networkChangeEventsFilePath);
				}
			}
			break;
		default:
			throw new RuntimeException("no network selected.");
		}

	}

	/**
	 * Deletes the existing output file and sets the number of the last iteration
	 * 
	 * @param lastMATSimIteration
	 * @param outputLocation
	 * @param coordinateSystem
	 * @return
	 */
	private static Config prepareConfig(int lastMATSimIteration, String outputLocation, String coordinateSystem) {
		Config config = ConfigUtils.createConfig();
		ScenarioUtils.loadScenario(config);
		config.controler().setOutputDirectory(outputLocation);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		new OutputDirectoryHierarchy(config.controler().getOutputDirectory(), config.controler().getRunId(),
				config.controler().getOverwriteFileSetting(), ControlerConfigGroup.CompressionType.gzip);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.controler().setLastIteration(lastMATSimIteration);
		config.global().setRandomSeed(4177);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.global().setCoordinateSystem(coordinateSystem);
		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
		freightConfigGroup.setTravelTimeSliceWidth(1800);
		freightConfigGroup.setTimeWindowHandling(FreightConfigGroup.TimeWindowHandling.enforceBeginnings);

		return config;
	}

	/**
	 * @param scenario
	 * @param selectedCarrierInputOption
	 * @param carriersFileLocation
	 * @param allNewCarrier
	 * @param csvLocation
	 * @throws IOException
	 */
	private static void prepareCarrier(Scenario scenario, CarrierInputOptions selectedCarrierInputOption,
			String carriersFileLocation, Set<NewCarrier> allNewCarrier, String csvLocation) throws IOException {

		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(),
				FreightConfigGroup.class);
		switch (selectedCarrierInputOption) {
		case createNewCarrierAndAddVehicleTypes:
			createNewCarrierAndAddVehilceTypes(scenario, allNewCarrier, freightConfigGroup);
			break;
		case readCarrierFile:
			if (carriersFileLocation == "")
				throw new RuntimeException("No path to the carrier file selected");
			else {
				freightConfigGroup.setCarriersFile(carriersFileLocation);
				FreightUtils.loadCarriersAccordingToFreightConfig(scenario);
				log.info("Get carriers from: " + carriersFileLocation);
			}
			break;
		case createFromCSV:
			allNewCarrier.clear();
			readAndCreateCarrierFromCSV(scenario, allNewCarrier, freightConfigGroup, csvLocation);
			break;
		default:
			throw new RuntimeException("no methed to create or read carrier selected.");
		}
	}

	/**
	 * @param freightConfigGroup
	 * @param allNewCarrier
	 * @param scenario
	 * @param scenario
	 * @param fleetSize
	 * @param carrierWithDepots
	 * @param vehicleTypesOfThisCarrier
	 * @param freightConfigGroup
	 */
	private static void createNewCarrierAndAddVehilceTypes(Scenario scenario, Set<NewCarrier> allNewCarrier,
			FreightConfigGroup freightConfigGroup) {
		Carriers carriers = FreightUtils.getOrCreateCarriers(scenario);
		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
		CarrierVehicleTypes usedCarrierVehicleTypes = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(carrierVehicleTypes).readFile(freightConfigGroup.getCarriersVehicleTypesFile());

		for (NewCarrier singleNewCarrier : allNewCarrier) {
			if (singleNewCarrier.getVehicleTypes() == null) {
				continue;
			}
			Carrier thisCarrier = null;
			CarrierCapabilities carrierCapabilities = null;
			if (carriers.getCarriers().containsKey(Id.create(singleNewCarrier.getName(), Carrier.class))) {
				thisCarrier = carriers.getCarriers().get(Id.create(singleNewCarrier.getName(), Carrier.class));
				carrierCapabilities = thisCarrier.getCarrierCapabilities();
				if (carrierCapabilities.getFleetSize() == null && singleNewCarrier.getFleetSize()!= null)
					carrierCapabilities.setFleetSize(singleNewCarrier.getFleetSize());
				if (singleNewCarrier.getFleetSize()!= null && carrierCapabilities.getFleetSize() != null &&!carrierCapabilities.getFleetSize().equals(singleNewCarrier.getFleetSize()))
					throw new RuntimeException("For the carrier " + singleNewCarrier.getName()
							+ " different fleetSize configuration was set. Please check and select only one!");
			} else {
				thisCarrier = CarrierUtils.createCarrier(Id.create(singleNewCarrier.getName(), Carrier.class));
				carrierCapabilities = CarrierCapabilities.Builder.newInstance()
						.setFleetSize(singleNewCarrier.getFleetSize()).build();
				carriers.addCarrier(thisCarrier);
			}
			for (String singleDepot : singleNewCarrier.getVehicleDepots()) {
				for (String thisVehicleType : singleNewCarrier.getVehicleTypes()) {
					VehicleType thisType = carrierVehicleTypes.getVehicleTypes()
							.get(Id.create(thisVehicleType, VehicleType.class));
					usedCarrierVehicleTypes.getVehicleTypes().putIfAbsent(Id.create(thisVehicleType, VehicleType.class),
							thisType);
					CarrierVehicle newCarrierVehicle = CarrierVehicle.Builder
							.newInstance(Id.create(thisType.getId().toString() + "_" + thisCarrier.getId().toString()
									+ "_" + singleDepot + "_start"+singleNewCarrier.getCarrierStartTime(), Vehicle.class), Id.createLinkId(singleDepot))
							.setEarliestStart(singleNewCarrier.getCarrierStartTime())
							.setLatestEnd(singleNewCarrier.getCarrierEndTime()).setTypeId(thisType.getId()).build();
					carrierCapabilities.getCarrierVehicles().put(newCarrierVehicle.getId(), newCarrierVehicle);
					if (!carrierCapabilities.getVehicleTypes().contains(thisType))
						carrierCapabilities.getVehicleTypes().add(thisType);
				}
			}
			thisCarrier.setCarrierCapabilities(carrierCapabilities);
		}
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(carrierVehicleTypes);
		scenario.addScenarioElement("carrierVehicleTypes", usedCarrierVehicleTypes); // TODO add to FreightUtils
	}

	/**
	 * @param scenario
	 * @param allNewCarrier
	 * @param freightConfigGroup
	 * @param csvLocation
	 * @throws IOException
	 */
	private static void readAndCreateCarrierFromCSV(Scenario scenario, Set<NewCarrier> allNewCarrier,
			FreightConfigGroup freightConfigGroup, String csvLocation) throws IOException {
		CSVParser parse = CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader()
				.parse(IOUtils.getBufferedReader(csvLocation));

		for (CSVRecord record : parse) {
			String carrierID = record.get("carrierName");

			FleetSize fleetSize = null;
			if (!record.get("fleetSize").isBlank() && record.get("fleetSize").contentEquals("infinite"))
				fleetSize = FleetSize.INFINITE;
			else if (!record.get("fleetSize").isBlank() && record.get("fleetSize").contentEquals("finite"))
				fleetSize = FleetSize.FINITE;
//			else
//				throw new RuntimeException(
//						"Select a valid FleetSize for the carrier: " + carrierID + ". Possible is finite or infinite");
			String[] vehicleDepots = null;
			if (!record.get("vehicleDepots").isBlank())
				vehicleDepots = record.get("vehicleDepots").split(":");
			String[] vehilceTypes = null;
			if (!record.get("vehicleTypes").isBlank())
				vehilceTypes = record.get("vehicleTypes").split(":");
			int carrierStartTime = 0;
			if (!record.get("carrierStartTime").isBlank())
				carrierStartTime = Integer.parseInt(record.get("carrierStartTime"));
			int carrierEndTime = 0;
			if (!record.get("carrierEndTime").isBlank())
				carrierEndTime = Integer.parseInt(record.get("carrierEndTime"));
			String[] areasForTheDemand = null;
			if (!record.get("demandAreas").isBlank())
				areasForTheDemand = record.get("demandAreas").split(":");
			int demandToDistribute = Integer.MAX_VALUE;
			if (!record.get("demandToDistribute").isBlank())
				demandToDistribute = Integer.parseInt(record.get("demandToDistribute"));
			int numberOfJobs = Integer.MAX_VALUE;
			if (!record.get("numberOfJobs").isBlank())
				numberOfJobs = Integer.parseInt(record.get("numberOfJobs"));
			int serviceTimePerUnit = Integer.MAX_VALUE;
			if (!record.get("serviceTimePerUnit").isBlank())
				serviceTimePerUnit = Integer.parseInt(record.get("serviceTimePerUnit"));
			TimeWindow serviceTimeWindow = null;
			if (!record.get("serviceStartTime").isBlank() || !record.get("serviceEndTime").isBlank())
				serviceTimeWindow = TimeWindow.newInstance(Integer.parseInt(record.get("serviceStartTime")),
						Integer.parseInt(record.get("serviceEndTime")));
			allNewCarrier.add(new NewCarrier(carrierID, vehilceTypes, vehicleDepots, fleetSize, carrierStartTime,
					carrierEndTime, areasForTheDemand, demandToDistribute, numberOfJobs, serviceTimePerUnit,
					serviceTimeWindow));
		}
		createNewCarrierAndAddVehilceTypes(scenario, allNewCarrier, freightConfigGroup);

	}

	/**
	 * @param config
	 * @param selectedVehicleInputOption
	 * @param vehicleTypesFileLocation
	 */
	private static void prepareVehicles(Config config, VehicleInputOptions selectedVehicleInputOption,
			String vehicleTypesFileLocation) {

		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
		switch (selectedVehicleInputOption) {
		case createNewVehicles:
			// TODO Or do not do this
			break;
		case readVehicleFile:
			if (vehicleTypesFileLocation == "")
				throw new RuntimeException("No path to the vehicleTypes selected");
			else {
				freightConfigGroup.setCarriersVehicleTypesFile(vehicleTypesFileLocation);
				log.info("Get vehicleTypes from: " + vehicleTypesFileLocation);
			}

			break;
		default:
			throw new RuntimeException("no methed to create or read vehicle types selected.");

		}
	}

	/**
	 * @param scenario
	 * @return
	 */
	private static Controler prepareControler(Scenario scenario) {
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
	 * @param controler
	 * @param nuOfJspritIteration
	 * @param usingRangeRestriction
	 * @throws InvalidAttributeValueException
	 */
	private static void runJsprit(Controler controler, int nuOfJspritIteration, boolean usingRangeRestriction)
			throws InvalidAttributeValueException {
		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(controler.getConfig(),
				FreightConfigGroup.class);
		if (usingRangeRestriction)
			freightConfigGroup.setUseDistanceConstraintForTourPlanning(
					FreightConfigGroup.UseDistanceConstraintForTourPlanning.basedOnEnergyConsumption);

		for (Carrier carrier : FreightUtils.getCarriers(controler.getScenario()).getCarriers().values()) {
			CarrierUtils.setJspritIterations(carrier, nuOfJspritIteration);
		}
		FreightUtils.runJsprit(controler.getScenario(), freightConfigGroup);

	}

}
