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
import org.matsim.api.core.v01.population.Population;
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
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
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
	static CoordinateTransformation crsTransformation = null;

	private enum NetworkChoice {
		grid9x9, berlinNetwork, otherNetwork
	};

	private enum CarrierInputOptions {
		readCarrierFile, createFromCSV, addCSVDataToExistingCarrierFileData
	};

	private enum VehicleInputOptions {
		readVehicleFile
	};

	private enum DemandGenerationOptions {
		useDemandFromCarrierFile, loadCSVData, usePopulationWithoutAgeGroups, usePopulationWithAgeGroups // TODO here perhaps only options of generation (population, random, etc.)
	};

	private enum OptionsOfVRPSolutions {
		runJspritAndMATSim, onlyRunJsprit, createNoSolutionAndOnlyWriteCarrierFile
	};

	private enum AnalyseOptions {
		withoutAnalyseOutput, withAnalyseOutput
	};

	public static void main(String[] args) throws IOException, InvalidAttributeValueException {

// create and prepare MATSim config
		String outputLocation = "output/demandGeneration/Test1";
		int lastMATSimIteration = 0;
		String networkCRS = "EPSG:31468";
		String shapeCRS = "EPSG:3857";
		crsTransformation = TransformationFactory.getCoordinateTransformation(networkCRS, shapeCRS);
		Config config = prepareConfig(lastMATSimIteration, outputLocation, networkCRS);

		log.info("Starting class to create a freight scenario");

// select network configurations
		NetworkChoice selectedNetwork = NetworkChoice.berlinNetwork;
		String networkPathOfOtherNetwork = "";
		boolean usingNetworkChangeEvents = false;
		String networkChangeEventsFilePath = "";
		setNetworkAndNetworkChangeEvents(config, selectedNetwork, networkPathOfOtherNetwork, usingNetworkChangeEvents,
				networkChangeEventsFilePath);

// load or create carrierVehicle
		VehicleInputOptions selectedVehicleInputOption = VehicleInputOptions.readVehicleFile;
		String vehicleTypesFileLocation = "scenarios/demandGeneration/testInput/vehicleTypes_default.xml";
		prepareVehicles(config, selectedVehicleInputOption, vehicleTypesFileLocation);

// load or create carrier
		CarrierInputOptions selectedCarrierInputOption = CarrierInputOptions.addCSVDataToExistingCarrierFileData;
		Scenario scenario = ScenarioUtils.loadScenario(config);
		String carriersFileLocation = "scenarios/demandGeneration/testInput/carrier_berlin_noDemand.xml";
		String csvLocation = "scenarios/demandGeneration/testInput/testCarrierCSV.csv";
		String shapeFileLocation = "scenarios/demandGeneration/shp_berlin_districts/Berlin_Ortsteile.shp";
		int defaultJspritIterations = 3;
		boolean useShapeFileforLocationsChoice = true;

		Set<NewCarrier> allNewCarrier = new HashSet<>();
		Collection<SimpleFeature> polygonsInShape = null;
		if (useShapeFileforLocationsChoice)
			polygonsInShape = ShapeFileReader.getAllFeatures(shapeFileLocation);
		prepareCarrier(scenario, selectedCarrierInputOption, carriersFileLocation, allNewCarrier, csvLocation,
				polygonsInShape, defaultJspritIterations);

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
		String populationFile = "../public-svn/matsim/scenarios/countries/de/episim/openDataModel/input/be_2020-week_snz_entirePopulation_emptyPlans_withDistricts_25pt_split.xml.gz";
		DemandGenerationOptions selectedDemandGenerationOption = DemandGenerationOptions.loadCSVData;
		createDemand(selectedDemandGenerationOption, scenario, allNewCarrier, useShapeFileforLocationsChoice,
				polygonsInShape, populationFile);

// prepare the VRP and get a solution
		OptionsOfVRPSolutions selectedSolution = OptionsOfVRPSolutions.runJspritAndMATSim;
		boolean usingRangeRestriction = false;
		Controler controler = prepareControler(scenario);

		solveSelectedSolution(selectedSolution, config, usingRangeRestriction, controler);

// analyze results
		AnalyseOptions analyseOption = AnalyseOptions.withAnalyseOutput;
		analyseResult(analyseOption, config.controler().getOutputDirectory());

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
			boolean usingRangeRestriction, Controler controler) throws InvalidAttributeValueException {
		switch (selectedSolution) {
		case runJspritAndMATSim:
			runJsprit(controler, usingRangeRestriction);
			controler.run();
			break;
		case onlyRunJsprit:
			runJsprit(controler, usingRangeRestriction);
			new CarrierPlanXmlWriterV2((Carriers) controler.getScenario().getScenarioElement("carriers"))
					.write(config.controler().getOutputDirectory() + "/output_jspritCarriersWithPlans.xml");
			log.warn(
					"##Finished with the jsprit solution. If you also want to run MATSim, please change  case of optionsOfVRPSolutions");
			System.exit(0);
			break;
		case createNoSolutionAndOnlyWriteCarrierFile:
			new CarrierPlanXmlWriterV2((Carriers) controler.getScenario().getScenarioElement("carriers"))
					.write(config.controler().getOutputDirectory() + "/output_carriersNoPlans.xml");
			log.warn(
					"##Finished without solution of the VRP. If you also want to run jsprit and/or MATSim, please change  case of optionsOfVRPSolutions");
			System.exit(0);
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
	 * @param polygonsInShape
	 * @param populationFile 
	 * @param defaultJspritIterations 
	 * @throws MalformedURLException
	 */
	private static void createDemand(DemandGenerationOptions selectedDemandGenerationOption, Scenario scenario,
			Set<NewCarrier> allNewCarrier, boolean demandLocationsInShape, Collection<SimpleFeature> polygonsInShape, String populationFile)
			throws MalformedURLException {

		switch (selectedDemandGenerationOption) {
		case loadCSVData:
			createServices(scenario, allNewCarrier, demandLocationsInShape, polygonsInShape);
			break;
		case useDemandFromCarrierFile:
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
		case usePopulationWithoutAgeGroups:
			Population population = PopulationUtils.readPopulation(populationFile);
			break;
		case usePopulationWithAgeGroups:
//			Population population = PopulationUtils.readPopulation(populationFile); //TODO
			break;
		default:
			break;
		}
	}

	/**
	 * @param analyseOption
	 * @param outputDirectory
	 * @throws UncheckedIOException
	 * @throws IOException
	 */
	private static void analyseResult(AnalyseOptions analyseOption, String outputDirectory)
			throws UncheckedIOException, IOException {

		switch (analyseOption) {
		case withAnalyseOutput:
			String[] argsAnalysis = { outputDirectory };
			FreightAnalyse.main(argsAnalysis);
			break;
		case withoutAnalyseOutput:
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
	 * @param defaultJspritIterations 
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
			String[] possibleAreas) {

		if (possibleAreas == null) {
			return true;
		}
		boolean isInShape = false;
		double x, y, xCoordFrom, xCoordTo, yCoordFrom, yCoordTo;

		xCoordFrom = crsTransformation.transform(link.getFromNode().getCoord()).getX();
		xCoordTo = crsTransformation.transform(link.getFromNode().getCoord()).getX();
		yCoordFrom = crsTransformation.transform(link.getFromNode().getCoord()).getY();
		yCoordTo = crsTransformation.transform(link.getFromNode().getCoord()).getY();
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
			for (String area : possibleAreas) {
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
	 * @param polygonsInShape
	 * @param defaultJspritIterations 
	 * @throws IOException
	 */
	private static void prepareCarrier(Scenario scenario, CarrierInputOptions selectedCarrierInputOption,
			String carriersFileLocation, Set<NewCarrier> allNewCarrier, String csvLocation,
			Collection<SimpleFeature> polygonsInShape, int defaultJspritIterations) throws IOException {

		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(),
				FreightConfigGroup.class);
		switch (selectedCarrierInputOption) {
		case addCSVDataToExistingCarrierFileData:
			if (carriersFileLocation == "")
				throw new RuntimeException("No path to the carrier file selected");
			else {
				freightConfigGroup.setCarriersFile(carriersFileLocation);
				FreightUtils.loadCarriersAccordingToFreightConfig(scenario);
				log.info("Load carriers from: " + carriersFileLocation);
				readAndCreateCarrierFromCSV(scenario, allNewCarrier, freightConfigGroup, csvLocation, polygonsInShape, defaultJspritIterations);
			}
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
			readAndCreateCarrierFromCSV(scenario, allNewCarrier, freightConfigGroup, csvLocation, polygonsInShape, defaultJspritIterations);
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
	 * @param polygonsInShape
	 * @param polygonsInShape
	 * @param CarrierVehicleTypes 
	 */
	private static void createNewCarrierAndAddVehilceTypes(Scenario scenario, Set<NewCarrier> allNewCarrier,
			FreightConfigGroup freightConfigGroup, Collection<SimpleFeature> polygonsInShape, int defaultJspritIterations) {
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
				if (carrierCapabilities.getFleetSize() == null && singleNewCarrier.getFleetSize() != null)
					carrierCapabilities.setFleetSize(singleNewCarrier.getFleetSize());
				if (singleNewCarrier.getJspritIterations() > 0)
					CarrierUtils.setJspritIterations(thisCarrier, singleNewCarrier.getJspritIterations());
			} else {
				thisCarrier = CarrierUtils.createCarrier(Id.create(singleNewCarrier.getName(), Carrier.class));
				if (singleNewCarrier.getJspritIterations() > 0)
					CarrierUtils.setJspritIterations(thisCarrier, singleNewCarrier.getJspritIterations());
				carrierCapabilities = CarrierCapabilities.Builder.newInstance()
						.setFleetSize(singleNewCarrier.getFleetSize()).build();
				carriers.addCarrier(thisCarrier);
			}
			while (singleNewCarrier.getVehicleDepots().length < singleNewCarrier.getNumberOfDepotsPerType()) {
				Random rand = new Random();
				Link link = scenario.getNetwork().getLinks().values().stream()
						.skip(rand.nextInt(scenario.getNetwork().getLinks().size())).findFirst().get();
				if (!link.getId().toString().contains("pt")
						&& (checkPositionInShape(link, polygonsInShape, singleNewCarrier.getAreaOfAdditonalDepots()))) {
					singleNewCarrier.addVehicleDepots(singleNewCarrier.getVehicleDepots(), link.getId().toString());
				}
			}
			for (String singleDepot : singleNewCarrier.getVehicleDepots()) {
				for (String thisVehicleType : singleNewCarrier.getVehicleTypes()) {
					VehicleType thisType = carrierVehicleTypes.getVehicleTypes()
							.get(Id.create(thisVehicleType, VehicleType.class));
					usedCarrierVehicleTypes.getVehicleTypes().putIfAbsent(Id.create(thisVehicleType, VehicleType.class),
							thisType);
					CarrierVehicle newCarrierVehicle = CarrierVehicle.Builder
							.newInstance(Id.create(
									thisType.getId().toString() + "_" + thisCarrier.getId().toString() + "_"
											+ singleDepot + "_start" + singleNewCarrier.getVehicleStartTime(),
									Vehicle.class), Id.createLinkId(singleDepot))
							.setEarliestStart(singleNewCarrier.getVehicleStartTime())
							.setLatestEnd(singleNewCarrier.getVehicleEndTime()).setTypeId(thisType.getId()).build();
					carrierCapabilities.getCarrierVehicles().put(newCarrierVehicle.getId(), newCarrierVehicle);
					if (!carrierCapabilities.getVehicleTypes().contains(thisType))
						carrierCapabilities.getVehicleTypes().add(thisType);
				}
			}
			thisCarrier.setCarrierCapabilities(carrierCapabilities);
		}
		for (Carrier carrier: carriers.getCarriers().values()) {
			if (CarrierUtils.getJspritIterations(carrier)==Integer.MIN_VALUE) {
				CarrierUtils.setJspritIterations(carrier, defaultJspritIterations);
				log.warn("The jspritIterations are now set to the default value of "+defaultJspritIterations+" in this simulation!");
				}
		}
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(carrierVehicleTypes);
		if (scenario.getScenarioElement("carrierVehicleTypes") == null)
			scenario.addScenarioElement("carrierVehicleTypes", usedCarrierVehicleTypes); // TODO add to FreightUtils
		else {
			CarrierVehicleTypes existingCarrierVehicleTypes = (CarrierVehicleTypes) scenario.getScenarioElement("carrierVehicleTypes");
			for (VehicleType newType :usedCarrierVehicleTypes.getVehicleTypes().values()) {
				if (!existingCarrierVehicleTypes.getVehicleTypes().containsKey(newType.getId()))
					existingCarrierVehicleTypes.getVehicleTypes().put(newType.getId(), newType);
			}
			
	}}

	/**
	 * @param scenario
	 * @param allNewCarrier
	 * @param freightConfigGroup
	 * @param csvLocation
	 * @param polygonsInShape
	 * @param defaultJspritIterations 
	 * @throws IOException
	 */
	private static void readAndCreateCarrierFromCSV(Scenario scenario, Set<NewCarrier> allNewCarrier,
			FreightConfigGroup freightConfigGroup, String csvLocation, Collection<SimpleFeature> polygonsInShape, int defaultJspritIterations)
			throws IOException {
		CSVParser parse = CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader()
				.parse(IOUtils.getBufferedReader(csvLocation));

		for (CSVRecord record : parse) {

			String carrierID = null;
			if (!record.get("carrierName").isBlank())
				carrierID = record.get("carrierName");
			String[] vehilceTypes = null;
			if (!record.get("vehicleTypes").isBlank())
				vehilceTypes = record.get("vehicleTypes").split(":");
			int numberOfDepots = 0;
			if (!record.get("numberOfDepots").isBlank())
				numberOfDepots = Integer.parseInt(record.get("numberOfDepots"));
			String[] vehicleDepots = null;
			if (!record.get("selectedVehicleDepots").isBlank())
				vehicleDepots = record.get("selectedVehicleDepots").split(":");
			String[] areaOfAdditonalDepots = null;
			if (!record.get("areaOfAdditonalDepots").isBlank())
				areaOfAdditonalDepots = record.get("areaOfAdditonalDepots").split(":");
			FleetSize fleetSize = null;
			if (!record.get("fleetSize").isBlank() && record.get("fleetSize").contentEquals("infinite"))
				fleetSize = FleetSize.INFINITE;
			else if (!record.get("fleetSize").isBlank() && record.get("fleetSize").contentEquals("finite"))
				fleetSize = FleetSize.FINITE;
			else if (!record.get("fleetSize").isBlank())
				throw new RuntimeException("Select a valid FleetSize for the carrier: " + carrierID
						+ ". Possible is finite or infinite!!");
			int vehicleStartTime = 0;
			if (!record.get("vehicleStartTime").isBlank())
				vehicleStartTime = Integer.parseInt(record.get("vehicleStartTime"));
			int vehicleEndTime = 0;
			if (!record.get("vehicleEndTime").isBlank())
				vehicleEndTime = Integer.parseInt(record.get("vehicleEndTime"));
			int jspritIterations = 0;
			if (!record.get("jspritIterations").isBlank())
				jspritIterations = Integer.parseInt(record.get("jspritIterations"));
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
			NewCarrier newCarrier = new NewCarrier(carrierID, vehilceTypes, numberOfDepots, vehicleDepots,
					areaOfAdditonalDepots, fleetSize, vehicleStartTime, vehicleEndTime, jspritIterations,
					areasForTheDemand, demandToDistribute, numberOfJobs, serviceTimePerUnit, serviceTimeWindow);
			allNewCarrier.add(newCarrier);
		}
		checkNewCarrier(allNewCarrier, freightConfigGroup, scenario, polygonsInShape);
		createNewCarrierAndAddVehilceTypes(scenario, allNewCarrier, freightConfigGroup, polygonsInShape, defaultJspritIterations);

	}

	/**
	 * @param allNewCarrier
	 * @param freightConfigGroup
	 * @param scenario
	 * @param polygonsInShape
	 */
	private static void checkNewCarrier(Set<NewCarrier> allNewCarrier, FreightConfigGroup freightConfigGroup,
			Scenario scenario, Collection<SimpleFeature> polygonsInShape) {

		for (NewCarrier carrier : allNewCarrier) {
			if (FreightUtils.getCarriers(scenario).getCarriers().containsKey(Id.create(carrier.getName(), Carrier.class)))
				throw new RuntimeException(
						"The Carrier "+carrier.getName()+" being loaded from the csv is already in the given Carrier file. It is not possible to add to an existing Carrier. Please check!");

			if (carrier.getName() == null || carrier.getName().isBlank())
				throw new RuntimeException(
						"Minimum one carrier has no name. Every carrier information has to be related to one carrier. Please check the input csv file!");
			CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
			new CarrierVehicleTypeReader(carrierVehicleTypes)
					.readFile(freightConfigGroup.getCarriersVehicleTypesFile());
			if (carrier.getVehicleTypes() != null)
				for (String type : carrier.getVehicleTypes()) {
					if (!carrierVehicleTypes.getVehicleTypes().containsKey(Id.create(type, VehicleType.class)))
						throw new RuntimeException("The selected vehicleType " + type + " of the carrier "
								+ carrier.getName()
								+ " in the input file is not part of imported vehicle types. Please change the type or add the type in the vehicleTypes input file!");
				}
			if (carrier.getVehicleDepots() != null) {
				if (carrier.getNumberOfDepotsPerType() < carrier.getVehicleDepots().length)
					throw new RuntimeException("For the carrier " + carrier.getName()
							+ " more certain depots than the given number of depots are selected. (numberOfDepots < selectedVehicleDepots)");

				for (String linkDepot : carrier.getVehicleDepots()) {
					if (!scenario.getNetwork().getLinks().containsKey(Id.create(linkDepot, Link.class)))
						throw new RuntimeException("The selected link " + linkDepot + " for a depot of the carrier "
								+ carrier.getName() + " is not part of the network. Please check!");
				}
			}
			if (carrier.getVehicleTypes() != null && carrier.getNumberOfDepotsPerType() == 0
					&& carrier.getVehicleDepots() == null)
				throw new RuntimeException(
						"If a vehicle type is selected in the input file, numberOfDepots or selectedVehicleDepots should be set. Please check carrier "
								+ carrier.getName());
			if (carrier.getVehicleDepots() != null
					&& (carrier.getNumberOfDepotsPerType() > carrier.getVehicleDepots().length)
					&& carrier.getAreaOfAdditonalDepots() == null)
				log.warn(
						"No possible area for addional depot given. Random choice in the hole network of a possible position");
			if (carrier.getVehicleDepots() == null && (carrier.getNumberOfDepotsPerType() > 0)
					&& carrier.getAreaOfAdditonalDepots() == null)
				log.warn(
						"No possible area for addional depot given. Random choice in the hole network of a possible position");
			if (carrier.getAreaOfAdditonalDepots() != null)
				for (String depotArea : carrier.getAreaOfAdditonalDepots()) {
					boolean isInShape = false;
					for (SimpleFeature singlePolygon : polygonsInShape) {
						if (singlePolygon.getAttribute("Ortsteil").equals(depotArea)) {
							isInShape = true;
							break;
						}
					}
					if (!isInShape)
						throw new RuntimeException("The area " + depotArea + " of the possible depots of carrier"
								+ carrier.getName() + " is not part of the given shapeFile");
				}
			if (carrier.getAreasForTheDemand() != null)
				for (String demand : carrier.getAreasForTheDemand()) {
					boolean isInShape = false;
					for (SimpleFeature singlePolygon : polygonsInShape) {
						if (singlePolygon.getAttribute("Ortsteil").equals(demand)) {
							isInShape = true;
							break;
						}
					}
					if (!isInShape)
						throw new RuntimeException("The area " + demand + " for the demand generation of carrier"
								+ carrier.getName() + " is not part of the given shapeFile");
				}
			if (carrier.getFleetSize() != null)
				for (NewCarrier existingCarrier : allNewCarrier)
					if (existingCarrier.getName().equals(carrier.getName()) && existingCarrier.getFleetSize() != null
							&& existingCarrier.getFleetSize() != carrier.getFleetSize())
						throw new RuntimeException("For the carrier " + carrier.getName()
								+ " different fleetSize configuration was set. Please check and select only one!");
			if (carrier.getVehicleTypes() != null) {
				if (carrier.getVehicleStartTime() == 0 || carrier.getVehicleEndTime() == 0)
					throw new RuntimeException("For the vehicle types of the carrier " + carrier.getName()
							+ " no start and/or end time for the vehicles was selected . Please set both times!!");
				if (carrier.getVehicleStartTime() >= carrier.getVehicleEndTime())
					throw new RuntimeException("For the vehicle types of the carrier " + carrier.getName()
							+ " a startTime after the endTime for the vehicles was selected . Please check!");
			}
			if (carrier.getJspritIterations() != 0)
				for (NewCarrier existingCarrier : allNewCarrier)
					if (existingCarrier.getName().equals(carrier.getName())
							&& existingCarrier.getJspritIterations() != 0
							&& existingCarrier.getJspritIterations() != carrier.getJspritIterations())
						throw new RuntimeException("For the carrier " + carrier.getName()
								+ " different number of jsprit iterations are set. Please check!");
		}
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
	private static void runJsprit(Controler controler, boolean usingRangeRestriction)
			throws InvalidAttributeValueException {
		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(controler.getConfig(),
				FreightConfigGroup.class);
		if (usingRangeRestriction)
			freightConfigGroup.setUseDistanceConstraintForTourPlanning(
					FreightConfigGroup.UseDistanceConstraintForTourPlanning.basedOnEnergyConsumption);
		FreightUtils.runJsprit(controler.getScenario(), freightConfigGroup);

	}

}
