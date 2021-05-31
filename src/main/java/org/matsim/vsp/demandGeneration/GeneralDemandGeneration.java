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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.management.InvalidAttributeValueException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
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
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.opengis.feature.simple.SimpleFeature;

/**
 * @author: rewert TODO
 */

public class GeneralDemandGeneration {

	private enum NetworkChoice {
		grid9x9, berlinNetwork, otherNetwork
	}

	private enum VehicleInputOptions {
		readVehicleFile
	}

	private enum CarrierInputOptions {
		readCarrierFile, createFromCSV, addCSVDataToExistingCarrierFileData
	}

	private enum DemandGenerationOptions {
		useDemandFromCarrierFile, loadCSVData, loadCSVDataAndUsePopulation
	}

	private enum PopulationOptions {
		usePopulationHolePopulation, usePopulationInShape, usePopulationInAgeGroups
	}

	private enum PopulationSamplingOption {
		createMoreLocations, increaseDemandOnLocation
	}

	private enum OptionsOfVRPSolutions {
		runJspritAndMATSim, onlyRunJsprit, createNoSolutionAndOnlyWriteCarrierFile
	}

	private enum AnalyseOptions {
		withoutAnalyseOutput, withAnalyseOutput
	}

	private static final Logger log = LogManager.getLogger(GeneralDemandGeneration.class);

	private static final String inputBerlinNetwork = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";;

	private static final String inputGridNetwork = "https://raw.githubusercontent.com/matsim-org/matsim/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml";;

	static CoordinateTransformation crsTransformationNetworkAndShape = null;

	public static void main(String[] args)
			throws IOException, InvalidAttributeValueException, ExecutionException, InterruptedException {

		// create and prepare MATSim config
		String outputLocation = "output/demandGeneration/Test1";
		int lastMATSimIteration = 0;
		String networkCRS = "EPSG:31468";
		String shapeCRS = "EPSG:3857";
		crsTransformationNetworkAndShape = TransformationFactory.getCoordinateTransformation(networkCRS, shapeCRS);
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
		CarrierInputOptions selectedCarrierInputOption = CarrierInputOptions.createFromCSV;
		Scenario scenario = ScenarioUtils.loadScenario(config);
		String carriersFileLocation = "scenarios/demandGeneration/testInput/carrier_berlin_noDemand.xml";
		String csvLocationCarrier = "scenarios/demandGeneration/testInput/testCarrierCSV.csv";
		String csvLocationDemand = "scenarios/demandGeneration/testInput/testDemandCSV.csv";
		String shapeFileLocation = "scenarios/demandGeneration/shp_berlin_districts/Berlin_Ortsteile.shp";
		int defaultJspritIterations = 3;
		boolean useShapeFileforLocationsChoice = true;

		Set<NewCarrier> allNewCarrier = new HashSet<>();
		Collection<SimpleFeature> polygonsInShape = null;
		if (useShapeFileforLocationsChoice)
			polygonsInShape = ShapeFileReader.getAllFeatures(shapeFileLocation);
		prepareCarrier(scenario, selectedCarrierInputOption, carriersFileLocation, allNewCarrier, csvLocationCarrier,
				polygonsInShape, defaultJspritIterations, useShapeFileforLocationsChoice);

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
		String populationFile = "../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-1pct/input/berlin-v5.5-1pct.plans.xml.gz";

		DemandGenerationOptions selectedDemandGenerationOption = DemandGenerationOptions.loadCSVData;
		PopulationOptions selectedPopulationOption = PopulationOptions.usePopulationInShape;
		PopulationSamplingOption selectedSamplingOption = PopulationSamplingOption.increaseDemandOnLocation;
		createDemand(selectedDemandGenerationOption, scenario, csvLocationDemand, useShapeFileforLocationsChoice,
				polygonsInShape, populationFile, selectedSamplingOption, selectedPopulationOption);

		// prepare the VRP and get a solution
		OptionsOfVRPSolutions selectedSolution = OptionsOfVRPSolutions.createNoSolutionAndOnlyWriteCarrierFile;
		boolean usingRangeRestriction = false;
		Controler controler = prepareControler(scenario);
		createFaciltyFile(controler);
		solveSelectedSolution(selectedSolution, config, usingRangeRestriction, controler);

		// analyze results

		AnalyseOptions analyseOption = AnalyseOptions.withAnalyseOutput;
		analyseResult(analyseOption, config.controler().getOutputDirectory());

		// TODO add service/Shipment ID to events to also analyze the jobs or add the
		// carrierFile to the analyzes

		log.info("Finished");
	}

	private static void createFaciltyFile(Controler controler) {

		Network network = controler.getScenario().getNetwork();
		ActivityFacilities facilities = controler.getScenario().getActivityFacilities();
		ActivityFacilitiesFactory factory = facilities.getFactory();

		FileWriter writer;
		File file;
		file = new File(controler.getConfig().controler().getOutputDirectory() + "/outputFacilitiesFile.csv");
		try {
			writer = new FileWriter(file, true);
			writer.write("id;x;y;type;linkID;personID\n");

			for (Carrier thisCarrier : FreightUtils.getCarriers(controler.getScenario()).getCarriers().values()) {
				for (CarrierService thisService : thisCarrier.getServices().values()) {
					Coord coord = MGC
							.point2Coord(middlePointOfLink(network.getLinks().get(thisService.getLocationLinkId())));
					ActivityFacility fac1 = factory.createActivityFacility(
							Id.create(thisCarrier.getId().toString() + thisService.getId().toString(),
									ActivityFacility.class),
							coord, thisService.getLocationLinkId());
					fac1.addActivityOption(new ActivityOptionImpl("Service"));
					facilities.addActivityFacility(fac1);
					if (thisService.getAttributes().getAsMap().containsKey("relatedPerson"))
						writer.write(thisCarrier.getId().toString() + thisService.getId().toString() + ";"
								+ coord.getX() + ";" + coord.getY() + ";" + "Service" + ";"
								+ thisService.getLocationLinkId().toString() + ";"
								+ thisService.getAttributes().getAttribute("relatedPerson") + "\n");
					else
						writer.write(thisCarrier.getId().toString() + thisService.getId().toString() + ";"
								+ coord.getX() + ";" + coord.getY() + ";" + "Service" + ";"
								+ thisService.getLocationLinkId().toString() + ";noPerson" + "\n");

				}
			}
//		new FacilitiesWriter(facilities).write(controler.getConfig().controler().getOutputDirectory()+"/outputFacilitiesFile.xml.gz");		

			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Wrote job locations file under " + "/outputLocationFile.xml.gz");
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
	 * @param selectedCarrierInputOption
	 * @param carriersFileLocation
	 * @param allNewCarrier
	 * @param csvLocation
	 * @param polygonsInShape
	 * @param defaultJspritIterations
	 * @param useShapeFileforLocationsChoice
	 * @throws IOException
	 */
	private static void prepareCarrier(Scenario scenario, CarrierInputOptions selectedCarrierInputOption,
			String carriersFileLocation, Set<NewCarrier> allNewCarrier, String csvLocationCarrier,
			Collection<SimpleFeature> polygonsInShape, int defaultJspritIterations,
			boolean useShapeFileforLocationsChoice) throws IOException {

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
				readAndCreateCarrierFromCSV(scenario, allNewCarrier, freightConfigGroup, csvLocationCarrier,
						polygonsInShape, defaultJspritIterations, useShapeFileforLocationsChoice);
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
			readAndCreateCarrierFromCSV(scenario, allNewCarrier, freightConfigGroup, csvLocationCarrier,
					polygonsInShape, defaultJspritIterations, useShapeFileforLocationsChoice);
			break;
		default:
			throw new RuntimeException("no methed to create or read carrier selected.");
		}
	}

	/**
	 * @param selectedDemandGenerationOption
	 * @param scenario
	 * @param allNewCarrier
	 * @param demandLocationsInShape
	 * @param polygonsInShape
	 * @param populationFile
	 * @param selectedSamplingOption2
	 * @param selectedUpSamplingOption
	 * @param defaultJspritIterations
	 * @throws IOException
	 */
	private static void createDemand(DemandGenerationOptions selectedDemandGenerationOption, Scenario scenario,
			String csvLocationDemand, boolean demandLocationsInShape, Collection<SimpleFeature> polygonsInShape,
			String populationFile, PopulationSamplingOption selectedSamplingOption,
			PopulationOptions selectedPopulationOption) throws IOException {

		Set<NewDemand> demandInformation = new HashSet<>();
		switch (selectedDemandGenerationOption) {
		case loadCSVData:
			demandInformation = readDemandInformation(csvLocationDemand, demandInformation, scenario, polygonsInShape);
			createServices(scenario, demandInformation, demandLocationsInShape, polygonsInShape, null);
			break;
		case loadCSVDataAndUsePopulation:
			Population population = PopulationUtils.readPopulation(populationFile);
			double sampleSizeInputPopulation = 0.01;
			double upSampleTo = 1;

			demandInformation = readDemandInformation(csvLocationDemand, demandInformation, scenario, polygonsInShape);
			switch (selectedSamplingOption) {
			case createMoreLocations:
				preparePopulation(population, sampleSizeInputPopulation, upSampleTo,
						"changeNumberOfLocationsWithDemand");
				break;
			case increaseDemandOnLocation:
				preparePopulation(population, sampleSizeInputPopulation, upSampleTo, "changeDemandOnLocation");
				break;
			default:
				break;
			}

			switch (selectedPopulationOption) {
			case usePopulationHolePopulation:
				createServices(scenario, demandInformation, demandLocationsInShape, polygonsInShape, population);
				break;
			case usePopulationInAgeGroups:
				// TODO
				break;
			case usePopulationInShape:
				String populationCRS = TransformationFactory.DHDN_GK4;
				String shapeCRS = "EPSG:3857";
				CoordinateTransformation crsTransformationPopulationAndShape = TransformationFactory
						.getCoordinateTransformation(populationCRS, shapeCRS);
				reducePopulationToShapeArea(population, crsTransformationPopulationAndShape, polygonsInShape);
				createServices(scenario, demandInformation, demandLocationsInShape, polygonsInShape, population);
				break;
			default:
				break;
			}
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
		default:
			break;
		}
	}

	private static Set<NewDemand> readDemandInformation(String csvLocationDemand, Set<NewDemand> demandInformation,
			Scenario scenario, Collection<SimpleFeature> polygonsInShape) throws IOException {

		CSVParser parse = CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader()
				.parse(IOUtils.getBufferedReader(csvLocationDemand));

		for (CSVRecord record : parse) {
			String carrierID = null;
			if (!record.get("carrierName").isBlank())
				carrierID = record.get("carrierName");
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
			double shareOfPopulationWithThisDemand = Double.MAX_VALUE;
			if (!record.get("shareOfPopulationWithThisDemand").isBlank())
				shareOfPopulationWithThisDemand = Double.parseDouble(record.get("shareOfPopulationWithThisDemand"));
			NewDemand newDemand = new NewDemand(carrierID, areasForTheDemand, demandToDistribute, numberOfJobs,
					serviceTimePerUnit, serviceTimeWindow, shareOfPopulationWithThisDemand);
			demandInformation.add(newDemand);
		}
		checkNewDemand(scenario, demandInformation, polygonsInShape);
		return demandInformation;
	}

	private static void checkNewDemand(Scenario scenario, Set<NewDemand> demandInformation,
			Collection<SimpleFeature> polygonsInShape) {

		for (NewDemand newDemand : demandInformation) {

			if (newDemand.getName() == null || newDemand.getName().isBlank())
				throw new RuntimeException(
						"Minimum one demand is not related to a carrier. Every demand information has to be related to one carrier. Please check the input csv file!");

			Carriers carriers = (Carriers) scenario.getScenarioElement("carriers");
			if (!carriers.getCarriers().containsKey(Id.create(newDemand.getName(), Carrier.class))) {
				throw new RuntimeException(
						"The created demand is not create for an existing carrier. Please create the carrier first or relate the demand to another carrier");
			}

			if (newDemand.getAreasForTheDemand() != null)
				for (String demand : newDemand.getAreasForTheDemand()) {
					boolean isInShape = false;
					for (SimpleFeature singlePolygon : polygonsInShape) {
						if (singlePolygon.getAttribute("Ortsteil").equals(demand)) {
							isInShape = true;
							break;
						}
					}
					if (!isInShape)
						throw new RuntimeException("The area " + demand + " for the demand generation of carrier"
								+ newDemand.getName() + " is not part of the given shapeFile");
				}
			if (newDemand.getShareOfPopulationWithThisDemand() != Double.MAX_VALUE)
				if (newDemand.getShareOfPopulationWithThisDemand() > 100
						|| newDemand.getShareOfPopulationWithThisDemand() <= 0)
					throw new RuntimeException("For the carrier " + newDemand.getName()
							+ ": The percentage of the population should be more than 0 and maximum 100pct. Please check!");
			if (newDemand.getNumberOfJobs() != Integer.MAX_VALUE
					&& newDemand.getShareOfPopulationWithThisDemand() != Double.MAX_VALUE)
				throw new RuntimeException("For the carrier " + newDemand.getName()
						+ ": Select either a numberOfJobs or a share of the population. Please check!");
			if (newDemand.getNumberOfJobs() == 0)
				throw new RuntimeException("For the carrier " + newDemand.getName()
						+ ": The number of jobs can not be 0 !. Please check!");
		}

	}

	private static void reducePopulationToShapeArea(Population population,
			CoordinateTransformation crsTransformationPopulationAndShape, Collection<SimpleFeature> polygonsInShape) {

		List<Id<Person>> personsToRemove = new ArrayList<>();

		double x, y;
		for (Person person : population.getPersons().values()) {
			boolean isInShape = false;
			x = (double) person.getAttributes().getAttribute("homeX");
			y = (double) person.getAttributes().getAttribute("homeY");
			Point test = MGC
					.coord2Point(crsTransformationPopulationAndShape.transform(MGC.point2Coord(MGC.xy2Point(x, y))));
			for (SimpleFeature singlePolygon : polygonsInShape) {
				if (((Geometry) singlePolygon.getDefaultGeometry()).contains(test)) {
					isInShape = true;
					break;
				}
			}
			if (!isInShape)
				personsToRemove.add(person.getId());
		}
		for (Id<Person> id : personsToRemove) {
			population.removePerson(id);
		}
	}

	/**
	 * @param selectedSolution
	 * @param config
	 * @param nuOfJspritIteration
	 * @param usingRangeRestriction
	 * @param controler
	 * @throws InvalidAttributeValueException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	private static void solveSelectedSolution(OptionsOfVRPSolutions selectedSolution, Config config,
			boolean usingRangeRestriction, Controler controler)
			throws InvalidAttributeValueException, ExecutionException, InterruptedException {
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
	 * @param freightConfigGroup
	 * @param csvLocation
	 * @param polygonsInShape
	 * @param defaultJspritIterations
	 * @param useShapeFileforLocationsChoice
	 * @throws IOException
	 */
	private static void readAndCreateCarrierFromCSV(Scenario scenario, Set<NewCarrier> allNewCarrier,
			FreightConfigGroup freightConfigGroup, String csvLocationCarrier, Collection<SimpleFeature> polygonsInShape,
			int defaultJspritIterations, boolean useShapeFileforLocationsChoice) throws IOException {
		CSVParser parse = CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader()
				.parse(IOUtils.getBufferedReader(csvLocationCarrier));

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
			int fixedNumberOfVehilcePerTypeAndLocation = 0;
			if (!record.get("fixedNumberOfVehilcePerTypeAndLocation").isBlank())
				fixedNumberOfVehilcePerTypeAndLocation = Integer
						.parseInt(record.get("fixedNumberOfVehilcePerTypeAndLocation"));
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
			NewCarrier newCarrier = new NewCarrier(carrierID, vehilceTypes, numberOfDepots, vehicleDepots,
					areaOfAdditonalDepots, fleetSize, vehicleStartTime, vehicleEndTime, jspritIterations,
					fixedNumberOfVehilcePerTypeAndLocation);
			allNewCarrier.add(newCarrier);
		}
		checkNewCarrier(allNewCarrier, freightConfigGroup, scenario, polygonsInShape);
		createNewCarrierAndAddVehilceTypes(scenario, allNewCarrier, freightConfigGroup, polygonsInShape,
				defaultJspritIterations, useShapeFileforLocationsChoice);

	}

	/**
	 * @param allNewCarrier
	 * @param freightConfigGroup
	 * @param scenario
	 * @param polygonsInShape
	 */
	private static void checkNewCarrier(Set<NewCarrier> allNewCarrier, FreightConfigGroup freightConfigGroup,
			Scenario scenario, Collection<SimpleFeature> polygonsInShape) {

		FreightUtils.addOrGetCarriers(scenario);
		for (NewCarrier carrier : allNewCarrier) {
			if (FreightUtils.getCarriers(scenario).getCarriers()
					.containsKey(Id.create(carrier.getName(), Carrier.class)))
				throw new RuntimeException("The Carrier " + carrier.getName()
						+ " being loaded from the csv is already in the given Carrier file. It is not possible to add to an existing Carrier. Please check!");

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
			if (carrier.getFixedNumberOfVehilcePerTypeAndLocation() != 0)
				for (NewCarrier existingCarrier : allNewCarrier)
					if ((existingCarrier.getName().equals(carrier.getName())
							&& existingCarrier.getFleetSize() == FleetSize.INFINITE)
							|| carrier.getFleetSize() == FleetSize.INFINITE)
						throw new RuntimeException("For the carrier " + carrier.getName()
								+ " a infinite fleetSize configuration was set, although you want to set a fixed number of vehicles. Please check!");
			if (carrier.getFleetSize() != null)
				for (NewCarrier existingCarrier : allNewCarrier)
					if (existingCarrier.getName().equals(carrier.getName()) && existingCarrier.getFleetSize() != null
							&& existingCarrier.getFleetSize() != carrier.getFleetSize())
						throw new RuntimeException("For the carrier " + carrier.getName()
								+ " different fleetSize configuration was set. Please check and select only one!");
			if (carrier.getVehicleTypes() != null) {
				if (carrier.getVehicleStartTime() == 0 || carrier.getVehicleEndTime() == 0)
					throw new RuntimeException("For the vehicle types of the carrier " + carrier.getName()
							+ " no start and/or end time for the vehicles was selected. Please set both times!!");
				if (carrier.getVehicleStartTime() >= carrier.getVehicleEndTime())
					throw new RuntimeException("For the vehicle types of the carrier " + carrier.getName()
							+ " a startTime after the endTime for the vehicles was selected. Please check!");
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
	 * @param useShapeFileforLocationsChoice
	 * @param CarrierVehicleTypes
	 */
	private static void createNewCarrierAndAddVehilceTypes(Scenario scenario, Set<NewCarrier> allNewCarrier,
			FreightConfigGroup freightConfigGroup, Collection<SimpleFeature> polygonsInShape,
			int defaultJspritIterations, boolean useShapeFileforLocationsChoice) {
		Carriers carriers = FreightUtils.addOrGetCarriers(scenario);
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
				if (!link.getId().toString().contains("pt") && checkPositionInShape(link, polygonsInShape,
						singleNewCarrier.getAreaOfAdditonalDepots(), useShapeFileforLocationsChoice)) {
					singleNewCarrier.addVehicleDepots(singleNewCarrier.getVehicleDepots(), link.getId().toString());
				}
			}
			for (String singleDepot : singleNewCarrier.getVehicleDepots()) {
				for (String thisVehicleType : singleNewCarrier.getVehicleTypes()) {
					VehicleType thisType = carrierVehicleTypes.getVehicleTypes()
							.get(Id.create(thisVehicleType, VehicleType.class));
					usedCarrierVehicleTypes.getVehicleTypes().putIfAbsent(Id.create(thisVehicleType, VehicleType.class),
							thisType);
					for (int i = 0; i <= singleNewCarrier.getFixedNumberOfVehilcePerTypeAndLocation(); i++) {
						CarrierVehicle newCarrierVehicle = CarrierVehicle.Builder
								.newInstance(Id.create(
										thisType.getId().toString() + "_" + thisCarrier.getId().toString() + "_"
												+ singleDepot + "_start" + singleNewCarrier.getVehicleStartTime() + "_"+ (i+1),
										Vehicle.class), Id.createLinkId(singleDepot))
								.setEarliestStart(singleNewCarrier.getVehicleStartTime())
								.setLatestEnd(singleNewCarrier.getVehicleEndTime()).setTypeId(thisType.getId()).build();
						carrierCapabilities.getCarrierVehicles().put(newCarrierVehicle.getId(), newCarrierVehicle);
						if (!carrierCapabilities.getVehicleTypes().contains(thisType))
							carrierCapabilities.getVehicleTypes().add(thisType);
					}
				}
			}
			thisCarrier.setCarrierCapabilities(carrierCapabilities);
		}
		for (Carrier carrier : carriers.getCarriers().values()) {
			if (CarrierUtils.getJspritIterations(carrier) == Integer.MIN_VALUE) {
				CarrierUtils.setJspritIterations(carrier, defaultJspritIterations);
				log.warn("The jspritIterations are now set to the default value of " + defaultJspritIterations
						+ " in this simulation!");
			}
		}
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(carrierVehicleTypes);
		if (scenario.getScenarioElement("carrierVehicleTypes") == null)
			scenario.addScenarioElement("carrierVehicleTypes", usedCarrierVehicleTypes); // TODO add to FreightUtils
		else {
			CarrierVehicleTypes existingCarrierVehicleTypes = (CarrierVehicleTypes) scenario
					.getScenarioElement("carrierVehicleTypes");
			for (VehicleType newType : usedCarrierVehicleTypes.getVehicleTypes().values()) {
				if (!existingCarrierVehicleTypes.getVehicleTypes().containsKey(newType.getId()))
					existingCarrierVehicleTypes.getVehicleTypes().put(newType.getId(), newType);
			}

		}
	}

	/**
	 * @param scenario
	 * @param demandInformation
	 * @param demandLocationsInShape
	 * @param singlePolygons
	 * @param polygonsInShape
	 * @param defaultJspritIterations
	 * @throws MalformedURLException
	 */
	private static void createServices(Scenario scenario, Set<NewDemand> demandInformation,
			boolean demandLocationsInShape, Collection<SimpleFeature> polygonsInShape, Population population) {

		int linksInNetwork = scenario.getNetwork().getLinks().size();
		for (NewDemand newDemand : demandInformation) {
			int countOfLinks = 1;
			int possibleLinks = 0;
			int distributedDemand = 0;
			double roundingError = 0;
			double sumOfLinkLenght = 0;
			int count = 0;
			double shareOfPopulationWithThisDemand = newDemand.getShareOfPopulationWithThisDemand();
			HashMap<Id<Link>, Point> middlePointsLinks = new HashMap<Id<Link>, Point>();
			int numberOfJobs = 0;
			int demandToDistribute = newDemand.getDemandToDistribute();

			if (demandToDistribute == Integer.MAX_VALUE && shareOfPopulationWithThisDemand == Double.MAX_VALUE)
				continue;

			if (shareOfPopulationWithThisDemand == Double.MAX_VALUE)
				numberOfJobs = newDemand.getNumberOfJobs();
			else if (population == null)
				throw new RuntimeException(
						"No population found although input paramater <ShareOfPopulationWithThisDemand> is set");
			else {
				double sampleSizeInputPopulation = (double) population.getAttributes().getAttribute("sampleSize");
				double sampleTo = (double) population.getAttributes().getAttribute("samplingTo");
				String samplingOption = String.valueOf(population.getAttributes().getAttribute("samplingOption"));

				if (sampleSizeInputPopulation == sampleTo)
					numberOfJobs = (int) Math
							.round(shareOfPopulationWithThisDemand * population.getPersons().values().size());
				else if (samplingOption.equals("changeNumberOfLocationsWithDemand"))
					numberOfJobs = (int) Math.round((sampleTo / sampleSizeInputPopulation)
							* (shareOfPopulationWithThisDemand * population.getPersons().values().size()));
				else if (samplingOption.equals("changeDemandOnLocation")) {
					demandToDistribute = (int) Math.round((sampleTo / sampleSizeInputPopulation) * demandToDistribute);
					numberOfJobs = (int) Math
							.round(shareOfPopulationWithThisDemand * population.getPersons().values().size());
				} else
					throw new RuntimeException(
							"Error with the sampling of the demand based on the population. Please check sampling sizes and sampling options!!");
				middlePointsLinks = createMapMiddlePointsLinks(scenario.getNetwork().getLinks().values());
			}

			if (numberOfJobs == Integer.MAX_VALUE) {
				for (Link link : scenario.getNetwork().getLinks().values()) {
					if (!link.getId().toString().contains("pt") && checkPositionInShape(link, polygonsInShape,
							newDemand.getAreasForTheDemand(), demandLocationsInShape)) {
						sumOfLinkLenght = sumOfLinkLenght + link.getLength();
						possibleLinks++;
					}
				}
				// creates services with a demand of 1
				if (possibleLinks > demandToDistribute) {

					for (int i = 0; i < demandToDistribute; i++) {
						count++;
						if (count * 2 > linksInNetwork)
							throw new RuntimeException(
									"Not enough links in the shape file to distribute the demand. Select an different shapefile or check if shapefile and network has the same coordinateSystem.");
						Link link = findPossibleLinkForDemand(scenario, population, middlePointsLinks,
								demandLocationsInShape, polygonsInShape, newDemand.getAreasForTheDemand());
						double serviceTime = newDemand.getServiceTimePerUnit();
						int demandForThisLink = 1;
						CarrierService thisService = CarrierService.Builder
								.newInstance(Id.create("Service_" + link.getId(), CarrierService.class), link.getId())
								.setCapacityDemand(demandForThisLink).setServiceDuration(serviceTime)
								.setServiceStartTimeWindow(newDemand.getServiceTimeWindow()).build();
						if (link.getAttributes().getAsMap().containsKey("lastPersonsWithDemand"))
							thisService.getAttributes().putAttribute("relatedPerson",
									link.getAttributes().getAttribute("lastPersonsWithDemand"));
						FreightUtils.getCarriers(scenario).getCarriers()
								.get(Id.create(newDemand.getName(), Carrier.class)).getServices()
								.put(thisService.getId(), thisService);
					}
				} else
				// creates a demand on each link, demand depends on the length of the link
				{

					if (sumOfLinkLenght == 0)
						throw new RuntimeException(
								"Not enough links in the shape file to distribute the demand. Select an different shapefile or check if shapefile and network has the same coordinateSystem.");

					for (Link link : scenario.getNetwork().getLinks().values()) {
						if (!link.getId().toString().contains("pt") && checkPositionInShape(link, polygonsInShape,
								newDemand.getAreasForTheDemand(), demandLocationsInShape)) {
							int demandForThisLink;
							if (countOfLinks == scenario.getNetwork().getLinks().size()) {
								demandForThisLink = demandToDistribute - distributedDemand;
							} else {
								demandForThisLink = (int) Math
										.ceil(link.getLength() / sumOfLinkLenght * (double) demandToDistribute);
								roundingError = roundingError + ((double) demandForThisLink
										- (link.getLength() / sumOfLinkLenght * (double) demandToDistribute));
								if (roundingError > 1) {
									demandForThisLink = demandForThisLink - 1;
									roundingError = roundingError - 1;
								}
								countOfLinks++;
							}
							double serviceTime = newDemand.getServiceTimePerUnit() * demandForThisLink;
							if (demandToDistribute > 0 && demandForThisLink > 0) {
								CarrierService thisService = CarrierService.Builder
										.newInstance(Id.create("Service_" + link.getId(), CarrierService.class),
												link.getId())
										.setCapacityDemand(demandForThisLink).setServiceDuration(serviceTime)
										.setServiceStartTimeWindow(newDemand.getServiceTimeWindow()).build();
								FreightUtils.getCarriers(scenario).getCarriers().values().iterator().next()
										.getServices().put(thisService.getId(), thisService);
							} else if (demandToDistribute == 0) {
								CarrierService thisService = CarrierService.Builder
										.newInstance(Id.create("Service_" + link.getId(), CarrierService.class),
												link.getId())
										.setServiceDuration(serviceTime)
										.setServiceStartTimeWindow(newDemand.getServiceTimeWindow()).build();
								if (link.getAttributes().getAsMap().containsKey("lastPersonsWithDemand"))
									thisService.getAttributes().putAttribute("relatedPerson",
											link.getAttributes().getAttribute("lastPersonsWithDemand"));
								FreightUtils.getCarriers(scenario).getCarriers()
										.get(Id.create(newDemand.getName(), Carrier.class)).getServices()
										.put(thisService.getId(), thisService);
							}
							distributedDemand = distributedDemand + demandForThisLink;
						}
					}
				}
			} else
			// if a certain number of services is selected
			{
				for (int i = 0; i < numberOfJobs; i++) {
					count++;
					if (demandToDistribute < numberOfJobs)
						throw new RuntimeException(
								"The resulting number of jobs is not feasible, because the demand is smaller then the number of jobs. Please check!");
					if (count * 2 > linksInNetwork)
						throw new RuntimeException(
								"Not enough links in the shape file to distribute the demand. Select an different shapefile or check if shapefile and network has the same coordinateSystem.");
					Link link = findPossibleLinkForDemand(scenario, population, middlePointsLinks,
							demandLocationsInShape, polygonsInShape, newDemand.getAreasForTheDemand());
					int demandForThisLink = (int) Math.ceil((double) demandToDistribute / (double) numberOfJobs);
					if (numberOfJobs == (i + 1)) {
						demandForThisLink = demandToDistribute - distributedDemand;
					} else {
						roundingError = roundingError
								+ ((double) demandForThisLink - ((double) demandToDistribute / (double) numberOfJobs));
						if (roundingError > 1) {
							demandForThisLink = demandForThisLink - 1;
							roundingError = roundingError - 1;
						}
					}
					double serviceTime = demandForThisLink * newDemand.getServiceTimePerUnit();

					CarrierService thisService = CarrierService.Builder
							.newInstance(Id.create("Service_" + link.getId(), CarrierService.class), link.getId())
							.setCapacityDemand(demandForThisLink).setServiceDuration(serviceTime)
							.setServiceStartTimeWindow(newDemand.getServiceTimeWindow()).build();
					if (link.getAttributes().getAsMap().containsKey("lastPersonsWithDemand"))
						thisService.getAttributes().putAttribute("relatedPerson",
								link.getAttributes().getAttribute("lastPersonsWithDemand"));
					FreightUtils.getCarriers(scenario).getCarriers().get(Id.create(newDemand.getName(), Carrier.class))
							.getServices().put(thisService.getId(), thisService);

					distributedDemand = distributedDemand + demandForThisLink;
				}
			}
		}
	}

	/**
	 * Searches a possible link for the demand
	 * 
	 * @param scenario
	 * @param population
	 * @param middlePointsLinks
	 * @param strings
	 * @param polygonsInShape
	 * @param demandLocationsInShape
	 * @return
	 */
	private static Link findPossibleLinkForDemand(Scenario scenario, Population population,
			HashMap<Id<Link>, Point> middlePointsLinks, boolean demandLocationsInShape,
			Collection<SimpleFeature> polygonsInShape, String[] areasForTheDemand) {
		Random rand = new Random();
		Link selectedlink = null;
		Link newLink = null;
		while (selectedlink == null) {
			if (middlePointsLinks.isEmpty()) {
				newLink = scenario.getNetwork().getLinks().values().stream()
						.skip(rand.nextInt(scenario.getNetwork().getLinks().size())).findFirst().get();
			} else {
				Person person = population.getPersons().values().stream()
						.skip(rand.nextInt(population.getPersons().size())).findFirst().get();
				Point homePoint = MGC.xy2Point((double) person.getAttributes().getAttribute("homeX"),
						(double) person.getAttributes().getAttribute("homeY"));
				newLink = scenario.getNetwork().getLinks().get(findNearestLink(homePoint, middlePointsLinks));
				newLink.getAttributes().putAttribute("lastPersonsWithDemand", person.getId().toString());
			}
			if (!newLink.getId().toString().contains("pt") && (!demandLocationsInShape
					|| checkPositionInShape(newLink, polygonsInShape, areasForTheDemand, demandLocationsInShape)))
				selectedlink = newLink;
		}
		return selectedlink;
	}

	/**
	 * Creates the middle points of every link in the network
	 * 
	 * @param values
	 * @return
	 */
	private static HashMap<Id<Link>, Point> createMapMiddlePointsLinks(Collection<? extends Link> allLinks) {
		HashMap<Id<Link>, Point> middlePointsLinks = new HashMap<Id<Link>, Point>();
		for (Link link : allLinks) {
			middlePointsLinks.put(link.getId(), middlePointOfLink(link));
		}
		return middlePointsLinks;

	}

	/**
	 * Creates the middle point of a link.
	 * 
	 * @param link
	 * @return Middle Point of the Link
	 */
	private static Point middlePointOfLink(Link link) {

		double x, y, xCoordFrom, xCoordTo, yCoordFrom, yCoordTo;
		xCoordFrom = link.getFromNode().getCoord().getX();
		xCoordTo = link.getToNode().getCoord().getX();
		yCoordFrom = link.getFromNode().getCoord().getY();
		yCoordTo = link.getToNode().getCoord().getY();
		if (xCoordFrom > xCoordTo)
			x = xCoordFrom - ((xCoordFrom - xCoordTo) / 2);
		else
			x = xCoordTo - ((xCoordTo - xCoordFrom) / 2);
		if (yCoordFrom > yCoordTo)
			y = yCoordFrom - ((yCoordFrom - yCoordTo) / 2);
		else
			y = yCoordTo - ((yCoordTo - yCoordFrom) / 2);

		return MGC.xy2Point(x, y);
	}

	/**
	 * @param pointActivity
	 * @param allLinks
	 * @param middlePointsLinks
	 * @return
	 */
	private static Id<Link> findNearestLink(Point pointActivity, HashMap<Id<Link>, Point> middlePointsLinks) {

		Id<Link> nearestLink = null;
		double distance;
		double minDistance = -1;

		for (Id<Link> link : middlePointsLinks.keySet()) {
			Point middlePointLink = middlePointsLinks.get(link);
			distance = Math.sqrt(
					(middlePointLink.getX() - pointActivity.getX()) * (middlePointLink.getX() - pointActivity.getX())
							+ (middlePointLink.getY() - pointActivity.getY())
									* (middlePointLink.getY() - pointActivity.getY()));

			if (minDistance == -1 || distance < minDistance) {
				minDistance = distance;
				nearestLink = link;
			}
		}
		return nearestLink;
	}

	/**
	 * @param link
	 * @param demandLocationsInShape
	 * @param strings
	 * @param polygonsInShape2
	 * @return
	 * @throws MalformedURLException
	 */
	private static boolean checkPositionInShape(Link link, Collection<SimpleFeature> polygonsInShape,
			String[] possibleAreas, boolean demandLocationsInShape) {

		if (!demandLocationsInShape) {
			return true;
		}
		boolean isInShape = false;
		double x, y, xCoordFrom, xCoordTo, yCoordFrom, yCoordTo;

		xCoordFrom = crsTransformationNetworkAndShape.transform(link.getFromNode().getCoord()).getX();
		xCoordTo = crsTransformationNetworkAndShape.transform(link.getFromNode().getCoord()).getX();
		yCoordFrom = crsTransformationNetworkAndShape.transform(link.getFromNode().getCoord()).getY();
		yCoordTo = crsTransformationNetworkAndShape.transform(link.getFromNode().getCoord()).getY();
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
			if (possibleAreas != null) {
				for (String area : possibleAreas) {
					if (area.equals(singlePolygon.getAttribute("Ortsteil")))
						if (((Geometry) singlePolygon.getDefaultGeometry()).contains(p)) {
							isInShape = true;
							return isInShape;
						}
				}
			} else {
				if (((Geometry) singlePolygon.getDefaultGeometry()).contains(p)) {
					isInShape = true;
					return isInShape;
				}
			}
		}
		return isInShape;
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
	 * add the home coordinates to attributes and remove plans
	 * 
	 * @param population
	 * @param sampleTo
	 * @param sampleSizeInputPopulation
	 * @param string
	 */
	private static void preparePopulation(Population population, double sampleSizeInputPopulation, double sampleTo,
			String samlingOption) {
		List<Id<Person>> personsToRemove = new ArrayList<>();
		population.getAttributes().putAttribute("sampleSize", sampleSizeInputPopulation);
		population.getAttributes().putAttribute("samplingTo", sampleTo);
		population.getAttributes().putAttribute("samplingOption", samlingOption);
//TODO vielleicht nur persons in Shape
		for (Person person : population.getPersons().values()) {
			if (!person.getAttributes().getAttribute("subpopulation").toString().equals("person")) {
				personsToRemove.add(person.getId());
				continue;
			}
			for (Plan plan : person.getPlans()) {
				for (PlanElement element : plan.getPlanElements()) {
					if (element instanceof Activity) {
						if (((Activity) element).getType().contains("home")) {
							double x = ((Activity) element).getCoord().getX();
							double y = ((Activity) element).getCoord().getY();
							person.getAttributes().putAttribute("homeX", x);
							person.getAttributes().putAttribute("homeY", y);
							break;
						}
					}
				}
			}
			person.removePlan(person.getSelectedPlan());
		}
		for (Id<Person> id : personsToRemove) {
			population.removePerson(id);
		}
	}

	/**
	 * @param controler
	 * @param nuOfJspritIteration
	 * @param usingRangeRestriction
	 * @throws InvalidAttributeValueException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	private static void runJsprit(Controler controler, boolean usingRangeRestriction)
			throws InvalidAttributeValueException, ExecutionException, InterruptedException {
		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(controler.getConfig(),
				FreightConfigGroup.class);
		if (usingRangeRestriction)
			freightConfigGroup.setUseDistanceConstraintForTourPlanning(
					FreightConfigGroup.UseDistanceConstraintForTourPlanning.basedOnEnergyConsumption);
		FreightUtils.runJsprit(controler.getScenario());
	}
}
