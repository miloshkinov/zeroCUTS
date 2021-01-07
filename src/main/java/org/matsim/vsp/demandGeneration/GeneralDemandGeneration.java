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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.Freight;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierUtils;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import com.graphhopper.jsprit.analysis.toolbox.StopWatch;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.listener.VehicleRoutingAlgorithmListeners;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;

/**
 * @author: rewert TODO
 */

public class GeneralDemandGeneration {

	private static final Logger log = LogManager.getLogger(GeneralDemandGeneration.class);

	private static final String inputBerlinNetwork = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";
	private static final String inputGridNetwork = "https://raw.githubusercontent.com/matsim-org/matsim/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml";

	private enum networkChoice {
		grid9x9, berlinNetwork, otherNetwork
	};

	private enum carrierInputOptions {
		createNewCarrier, readCarrierFile, readCarrierFileIncludingJobs
	};

	private enum vehicleInputOptions {
		createNewVehicles, readVehicleFile
	};

	private enum demandGenerationOptions {
		distributeServicesOverAllLinks, xLocationsWithXDemand, useDemandFromUsedCarrierFile, distributeShipmentsOverAllLinks
	};

	public static void main(String[] args)
			throws RuntimeException, ExecutionException, InterruptedException, IOException {

		networkChoice selectedNetwork = null;
		carrierInputOptions selectedCarrierInputOption = null;
		vehicleInputOptions selectedVehicleInputOption = null;
		demandGenerationOptions selectedDemandGenerationOption = null;

		String shapeFileLocation;

		// create and prepare MATSim config
		String outputLocation = "output/demandGeneration/Test1";
		int lastMATSimIteration = 0;
		String coordinateSystem = TransformationFactory.GK4;
		Config config = prepareConfig(lastMATSimIteration, outputLocation, coordinateSystem);

		log.info("Starting class to create a freight scenario");

		// select network configurations
		selectedNetwork = networkChoice.grid9x9;
		String networkPathOfOtherNetwork = "";
		boolean usingNetworkChangeEvents = false;
		String networkChangeEventsFilePath = "";
		setNetworkAndNetworkChangeEvents(config, selectedNetwork, networkPathOfOtherNetwork, usingNetworkChangeEvents,
				networkChangeEventsFilePath);

		// load or create carrier
		selectedCarrierInputOption = carrierInputOptions.readCarrierFile;
		String carriersFileLocation = "scenarios/demandGeneration/testInput/carrier_grid_noDemand.xml";
		prepareCarrier(config, selectedCarrierInputOption, carriersFileLocation);

		// load or create carrierVehicle
		selectedVehicleInputOption = vehicleInputOptions.readVehicleFile;
		String vehicleTypesFileLocation = "scenarios/demandGeneration/testInput/vehicleTypes_default.xml";
		prepareVehicles(config, selectedVehicleInputOption, vehicleTypesFileLocation);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		FreightUtils.loadCarriersAccordingToFreightConfig(scenario);

		// create the demand
		// TODO also possibilty to add demand for carrier with existings jobs??
		// TODO add services with no demand and only serviceTime

		int demandToDistribute = 500;
		double serviceTimePerDemandUnit = 300;
		List<String> deliveryLocations = Arrays.asList("");
		boolean chooseNearestDeliveryLocation = true;
		selectedDemandGenerationOption = demandGenerationOptions.distributeServicesOverAllLinks;
		createDemand(selectedDemandGenerationOption, scenario, demandToDistribute, serviceTimePerDemandUnit);

		// prepare the VRP and get a solution

		Controler controler = prepareControler(scenario);

		int nuOfJspritIteration = 1;
		boolean usingRangeRestriction = false;
		runJsprit(controler, nuOfJspritIteration, usingRangeRestriction);

		// run MATSim
		controler.run();

		// analyze results
		String[] argsAnalysis = { config.controler().getOutputDirectory() };
		FreightAnalyse.main(argsAnalysis);
		// TODO

		log.info("Finished");
	}

	private static void createDemand(demandGenerationOptions selectedDemandGenerationOption, Scenario scenario,
			int demandToDistribute, double serviceTimePerDemandUnit) {

		switch (selectedDemandGenerationOption) {
		case distributeServicesOverAllLinks:
			createServicesWithStaticDemandOverAllLinks(scenario, demandToDistribute, serviceTimePerDemandUnit);
			break;
		case distributeShipmentsOverAllLinks:
			createServicesWithStaticDemandOverAllLinks(scenario, demandToDistribute, serviceTimePerDemandUnit);
			break;
		case xLocationsWithXDemand:
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
				throw new RuntimeException("No jobs for the carrier selected");
			break;
		default:
			break;
		}
	}

	private static void createServicesWithStaticDemandOverAllLinks(Scenario scenario, int demandToDistribute,
			double serviceTimePerDemandUnit) {
		int countOfLinks = 1;
		int distributedDemand = 0;
		double roundingError = 0;
		double sumOfLinkLenght = 0;

		for (Link link : scenario.getNetwork().getLinks().values()) {
			sumOfLinkLenght = sumOfLinkLenght + link.getLength();
		}
		if (scenario.getNetwork().getLinks().size() > demandToDistribute) {
			for (int i = 0; i < demandToDistribute; i++) {
				Random rand = new Random();
				Link link = scenario.getNetwork().getLinks().values().stream()
						.skip(rand.nextInt(scenario.getNetwork().getLinks().size())).findFirst().get();
				
				double serviceTime = serviceTimePerDemandUnit * 1;
				CarrierService thisService = CarrierService.Builder
						.newInstance(Id.create("Service_" + link.getId(), CarrierService.class), link.getId())
						.setCapacityDemand(1).setServiceDuration(serviceTime)
						.setServiceStartTimeWindow(TimeWindow.newInstance(6 * 3600, 14 * 3600)).build();
				FreightUtils.getCarriers(scenario).getCarriers().values().iterator().next().getServices()
						.put(thisService.getId(), thisService);
			}
		} else {

			for (Link link : scenario.getNetwork().getLinks().values()) {
				int demandForThisLink;
				if (countOfLinks == scenario.getNetwork().getLinks().size()) {
					demandForThisLink = demandToDistribute - distributedDemand;

				} else {
					demandForThisLink = (int) Math.ceil(link.getLength() / sumOfLinkLenght * demandToDistribute);
					roundingError = roundingError
							+ (demandForThisLink - (link.getLength() / sumOfLinkLenght * demandToDistribute));
					if (roundingError > 1) {
						demandForThisLink = demandForThisLink - 1;
						roundingError = roundingError - 1;
					}
					countOfLinks++;
				}
				double serviceTime = serviceTimePerDemandUnit * demandForThisLink;
				if (demandToDistribute > 0 && demandForThisLink > 0) {
					CarrierService thisService = CarrierService.Builder
							.newInstance(Id.create("Service_" + link.getId(), CarrierService.class), link.getId())
							.setCapacityDemand(demandForThisLink).setServiceDuration(serviceTime)
							.setServiceStartTimeWindow(TimeWindow.newInstance(6 * 3600, 14 * 3600)).build();
					FreightUtils.getCarriers(scenario).getCarriers().values().iterator().next().getServices()
							.put(thisService.getId(), thisService);
				} else if (demandToDistribute == 0) {
					CarrierService thisService = CarrierService.Builder
							.newInstance(Id.create("Service_" + link.getId(), CarrierService.class), link.getId())
							.setServiceDuration(serviceTime)
							.setServiceStartTimeWindow(TimeWindow.newInstance(6 * 3600, 14 * 3600)).build();
					FreightUtils.getCarriers(scenario).getCarriers().values().iterator().next().getServices()
							.put(thisService.getId(), thisService);
				}
				distributedDemand = distributedDemand + demandForThisLink;
			}
		}
	}

	/**
	 * @param config
	 * @param networkChoice
	 * @param networkPathOfOtherNetwork
	 * @param usingNetworkChangeEvents
	 * @param networkChangeEventsFilePath
	 * @throws RuntimeException
	 */
	private static void setNetworkAndNetworkChangeEvents(Config config, networkChoice networkChoice,
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

	private static void prepareCarrier(Config config, carrierInputOptions selectedVehicleInputOption,
			String carriersFileLocation) {

		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
		switch (selectedVehicleInputOption) {
		case createNewCarrier:
			// TODO
			break;
		case readCarrierFile:
			if (carriersFileLocation == "")
				throw new RuntimeException("No path to the carrier file selected");
			else {
				freightConfigGroup.setCarriersFile(carriersFileLocation);
				log.info("Get carriers from: " + carriersFileLocation);
			}
			break;
		case readCarrierFileIncludingJobs:
			if (carriersFileLocation == "")
				throw new RuntimeException("No path to the carrier file including the jobs selected");
			else {
				freightConfigGroup.setCarriersFile(carriersFileLocation);
				log.info("Get carriers from: " + carriersFileLocation);
			}

			break;
		default:
			throw new RuntimeException("no methed to create or read carrier selected.");
		}
	}

	private static void prepareVehicles(Config config, vehicleInputOptions selectedVehicleInputOption,
			String vehicleTypesFileLocation) {

		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
		switch (selectedVehicleInputOption) {
		case createNewVehicles:
			// TODO
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

	private static Controler prepareControler(Scenario scenario) {
		Controler controler = new Controler(scenario);

		Freight.configure(controler);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new CarrierModule());
//                bind(CarrierPlanStrategyManagerFactory.class).toInstance( null );
//                bind(CarrierScoringFunctionFactory.class).toInstance(null );
			}
		});
		return controler;
	}

	private static void runJsprit(Controler controler, int nuOfJspritIteration, boolean usingRangeRestriction)
			throws ExecutionException, InterruptedException {

		if (usingRangeRestriction) {
			FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(controler.getConfig(),
					FreightConfigGroup.class);
			freightConfigGroup.setUseDistanceConstraintForTourPlanning(
					FreightConfigGroup.UseDistanceConstraintForTourPlanning.basedOnEnergyConsumption);
		}

		NetworkBasedTransportCosts.Builder netBuilder = NetworkBasedTransportCosts.Builder.newInstance(
				controler.getScenario().getNetwork(),
				FreightUtils.getCarrierVehicleTypes(controler.getScenario()).getVehicleTypes().values());
		final NetworkBasedTransportCosts netBasedCosts = netBuilder.build();

		Carriers carriers = FreightUtils.getCarriers(controler.getScenario());

		HashMap<Id<Carrier>, Integer> carrierActivityCounterMap = new HashMap<>();

		// Fill carrierActivityCounterMap -> basis for sorting the carriers by number of
		// activities before solving in parallel
		for (Carrier carrier : carriers.getCarriers().values()) {
			carrierActivityCounterMap.put(carrier.getId(),
					carrierActivityCounterMap.getOrDefault(carrier.getId(), 0) + carrier.getServices().size());
			carrierActivityCounterMap.put(carrier.getId(),
					carrierActivityCounterMap.getOrDefault(carrier.getId(), 0) + carrier.getShipments().size());
		}

		HashMap<Id<Carrier>, Integer> sortedMap = carrierActivityCounterMap.entrySet().stream()
				.sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));

		ArrayList<Id<Carrier>> tempList = new ArrayList<>(sortedMap.keySet());
		ForkJoinPool forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
		forkJoinPool.submit(() -> tempList.parallelStream().forEach(carrierId -> {
			Carrier carrier = carriers.getCarriers().get(carrierId);

			double start = System.currentTimeMillis();
			int serviceCount = carrier.getServices().size();
			log.info("start tour planning for " + carrier.getId() + " which has " + serviceCount + " services");

//    for (Carrier carrier : carriers.getCarriers().values()){
			// Carrier carrier =
			// carriers.getCarriers().get(Id.create("kaiser_VERBRAUCHERMARKT_FRISCHE",
			// Carrier.class)); //only for tests

			// currently with try/catch, because CarrierUtils.getJspritIterations will throw
			// an exception if value is not present. Will fix it on MATSim.
			// TODO maybe a future CarrierUtils functionality: Overwrite/set all
			// nuOfJspritIterations. maybe depending on enum (overwriteAll, setNotExisiting,
			// none) ?, KMT Nov2019
			try {
				if (CarrierUtils.getJspritIterations(carrier) <= 0) {
					log.warn(
							"Received negative number of jsprit iterations. This is invalid -> Setting number of jsprit iterations for carrier: "
									+ carrier.getId() + " to " + nuOfJspritIteration);
					CarrierUtils.setJspritIterations(carrier, nuOfJspritIteration);
				} else {
					log.warn("Overwriting the number of jsprit iterations for carrier: " + carrier.getId()
							+ ". Value was before " + CarrierUtils.getJspritIterations(carrier) + "and is now "
							+ nuOfJspritIteration);
					CarrierUtils.setJspritIterations(carrier, nuOfJspritIteration);
				}
			} catch (Exception e) {
				log.warn("Setting (missing) number of jsprit iterations for carrier: " + carrier.getId() + " to "
						+ nuOfJspritIteration);
				CarrierUtils.setJspritIterations(carrier, nuOfJspritIteration);
			}

			VehicleRoutingProblem vrp = MatsimJspritFactory
					.createRoutingProblemBuilder(carrier, controler.getScenario().getNetwork())
					.setRoutingCost(netBasedCosts).build();

			log.warn("Ignore the algorithms file for jsprit and use an algorithm out of the box.");
			Scenario scenario = controler.getScenario();
			FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(controler.getConfig(),
					FreightConfigGroup.class);
			VehicleRoutingAlgorithm vra = MatsimJspritFactory.loadOrCreateVehicleRoutingAlgorithm(scenario,
					freightConfigGroup, netBasedCosts, vrp);
			vra.getAlgorithmListeners().addListener(new StopWatch(), VehicleRoutingAlgorithmListeners.Priority.HIGH);
			vra.setMaxIterations(CarrierUtils.getJspritIterations(carrier));
			VehicleRoutingProblemSolution solution = Solutions.bestOf(vra.searchSolutions());

			log.info("tour planning for carrier " + carrier.getId() + " took "
					+ (System.currentTimeMillis() - start) / 1000 + " seconds.");

			CarrierPlan newPlan = MatsimJspritFactory.createPlan(carrier, solution);

			log.info("routing plan for carrier " + carrier.getId());
			NetworkRouter.routePlan(newPlan, netBasedCosts);
			log.info("routing for carrier " + carrier.getId() + " finished. Tour planning plus routing took "
					+ (System.currentTimeMillis() - start) / 1000 + " seconds.");

			carrier.setSelectedPlan(newPlan);
		})).get();
	}

}
