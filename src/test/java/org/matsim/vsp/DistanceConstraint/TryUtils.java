package org.matsim.vsp.DistanceConstraint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierUtils;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.controler.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.controler.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts.Builder;
import org.matsim.contrib.freight.usecases.chessboard.CarrierScoringFunctionFactoryImpl;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.EngineInformation.FuelType;

import com.google.common.collect.Multimap;
import com.graphhopper.jsprit.analysis.toolbox.Plotter;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.algorithm.box.SchrimpfFactory;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit.Strategy;
import com.graphhopper.jsprit.core.algorithm.state.StateId;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.constraint.ConstraintManager;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.jsprit.core.util.VehicleRoutingTransportCostsMatrix;

class TryUtils {

	static int noDelivery = 0;
	static int noShipments = 0;
	static double costsJsprit;
	static double costsMatsim;

	/**
	 * Deletes the existing output file and sets the number of the last MATSim
	 * iteration. Sets the qgs "EPSG: 3857"
	 * 
	 * @param config
	 */
	static Config prepareConfig(Config config, int lastIteration) {
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		new OutputDirectoryHierarchy(config.controler().getOutputDirectory(), config.controler().getRunId(),
				config.controler().getOverwriteFileSetting(), ControlerConfigGroup.CompressionType.gzip);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);

		config.controler().setLastIteration(lastIteration);
		config.global().setRandomSeed(4177);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.global().setCoordinateSystem("EPSG:3857");

		return config;
	}

	/**
	 * Creates a new vehicleType and ads this type to the CarrierVehicleTypes. Only
	 * an example vehicle (conventional and electric) with no real parameters
	 */
	static CarrierVehicleTypes createAndAddVehicles(boolean electricCar, boolean addAdditionalVehicle) {
		VehicleType newVehicleType = null;
		if (addAdditionalVehicle == true) {
			String vehicleTypeId = "MB_Econic_Diesel";
			// capacityTruck = 11500; // kg
			int capacityTruck = 20; // m3
			double maxVelocity = 100 / 3.6; // m/s
			double costPerDistanceUnit = 0.000846; // €/m
			double costPerTimeUnit = 0.0; // €/s
			double fixCosts = 999.93; // €
			EngineInformation.FuelType engineInformation = FuelType.diesel;
			double literPerMeter = 0.00067; // l/m

			if (electricCar == true) {
				vehicleTypeId = "E-Force KSF";
				capacityTruck = 40; // m3
				maxVelocity = 80 / 3.6; // m/s
				costPerDistanceUnit = 0.00055; // €/m
				costPerTimeUnit = 0.008; // €/s
				fixCosts = 70; // €
				engineInformation = FuelType.electricity;
				literPerMeter = 0.0; // l/m
			}

			newVehicleType = createGarbageTruckType(vehicleTypeId, maxVelocity, costPerDistanceUnit, costPerTimeUnit,
					fixCosts, engineInformation, literPerMeter, capacityTruck);
		}
		return adVehicleTypes(addAdditionalVehicle, newVehicleType);

	}

	/**
	 * Method creates a new CarrierVehicleType
	 * 
	 * @param maxVelocity in m/s
	 * @return the new vehicleType
	 */
	private static VehicleType createGarbageTruckType(String vehicleTypeId, double maxVelocity,
			double costPerDistanceUnit, double costPerTimeUnit, double fixCosts, FuelType engineInformation,
			double literPerMeter, int capacityTruck) {
		
//		VehicleTypeImpl thisVehicleType = VehicleTypeImpl.Builder.newInstance(vehicleTypeId).addCapacityDimension(0, capacityTruck)
//				.setMaxVelocity(maxVelocity).setCostPerDistance(costPerDistanceUnit).setProfile(vehicleTypeId)
//				.setCostPerTime(costPerTimeUnit).setFixedCost(fixCosts).build();
	
		VehicleType thisVehicleType2 = VehicleUtils.createVehicleType(Id.create(vehicleTypeId, VehicleType.class));
		thisVehicleType2.getCapacity().setOther(capacityTruck);
		thisVehicleType2.getEngineInformation().getAttributes().putAttribute("fuelType", engineInformation);
//		thisVehicleType2.getAttributes().putAttribute("fuelType", engineInformation);
		thisVehicleType2.getCostInformation().setCostsPerMeter(costPerDistanceUnit).setCostsPerSecond(costPerTimeUnit).setFixedCost(fixCosts);
		return 	thisVehicleType2;

	}

	/**
	 * Method adds all vehicleType (imported and created) to the list of
	 * vehicleTyps. Sets the maxVelocity to 120 km/h
	 * 
	 * @param
	 * @return vehicleTypes
	 */
	private static CarrierVehicleTypes adVehicleTypes(boolean addAdditionalVehicle, VehicleType newVehicleType) {
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(vehicleTypes).readFile("scenarios/vehicleTypesExample/vehicleTypesExample.xml");
		if (addAdditionalVehicle == true)
			vehicleTypes.getVehicleTypes().put(newVehicleType.getId(), newVehicleType);
		for (VehicleType singleVehicleType : vehicleTypes.getVehicleTypes().values()) {
			singleVehicleType.setMaximumVelocity(120 / 3.6);
		}
		return vehicleTypes;
	}

	/**
	 * Creates the vehicle at the depot, ads this vehicle to the carriers and sets
	 * the capabilities. Sets TimeWindow for the carriers.
	 * 
	 * @param
	 */
	static void createCarriers(Carriers carriers, FleetSize fleetSize, Carrier singleCarrier, Scenario scenario,
			CarrierVehicleTypes vehicleTypes) {
		String vehicleName = "Depot";
		double earliestStartingTime = 0 * 3600;
		double latestFinishingTime = 48 * 3600;
		List<CarrierVehicle> vehicles = new ArrayList<CarrierVehicle>();
		for (VehicleType singleVehicleType : vehicleTypes.getVehicleTypes().values()) {
			vehicles.add(createGarbageTruck(vehicleName + "-" + singleVehicleType.getId(), earliestStartingTime,
					latestFinishingTime, singleVehicleType));
		}

		// define Carriers

		defineCarriersBorusan(carriers, fleetSize, singleCarrier, vehicles, vehicleTypes);
	}

	/**
	 * Method for creating a new carrierVehicle
	 * 
	 * @param
	 * 
	 * @return new carrierVehicle at the depot
	 */
	static CarrierVehicle createGarbageTruck(String vehicleName, double earliestStartingTime,
			double latestFinishingTime, VehicleType singleVehicleType) {

		return CarrierVehicle.Builder.newInstance(Id.create(vehicleName, Vehicle.class), Id.createLinkId("i(1,8)"))
				.setEarliestStart(earliestStartingTime).setLatestEnd(latestFinishingTime)
				.setTypeId(singleVehicleType.getId()).setType(singleVehicleType).build();
	}

	/**
	 * Defines and sets the Capabilities of the Carrier, including the vehicleTypes
	 * for the carriers
	 * 
	 * @param
	 * 
	 */
	private static void defineCarriersBorusan(Carriers carriers, FleetSize fleetSize, Carrier singleCarrier,
			List<CarrierVehicle> vehicles, CarrierVehicleTypes vehicleTypes) {

		singleCarrier.setCarrierCapabilities(CarrierCapabilities.Builder.newInstance().setFleetSize(fleetSize).build());
		for (CarrierVehicle carrierVehicle : vehicles) {
			CarrierUtils.addCarrierVehicle(singleCarrier, carrierVehicle);
		}	
		singleCarrier.getCarrierCapabilities().getVehicleTypes().addAll(vehicleTypes.getVehicleTypes().values());

		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes);
	}

	/**
	 * Solves with jsprit and gives a xml output of the plans and a plot of the
	 * solution. Because of using the distance constraint it is necessary to create
	 * a cost matrix before solving the vrp with jsprit. The jsprit algorithm solves
	 * a solution for every created carrier separately.
	 * 
	 * @param
	 */
	static void solveWithJsprit(Scenario scenario, Carriers carriers, Carrier singleCarrier, int jspritIteration,
			CarrierVehicleTypes vehicleTypes, Multimap<String, Double[]> batteryConstraints) {

		// Netzwerk integrieren und Kosten für jsprit
		Network network = scenario.getNetwork();
		Builder netBuilder = NetworkBasedTransportCosts.Builder.newInstance(network,
				vehicleTypes.getVehicleTypes().values());
		final NetworkBasedTransportCosts netBasedCosts = netBuilder.build();

		netBuilder.setTimeSliceWidth(1800);

		VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(singleCarrier,
				network);
		vrpBuilder.setRoutingCost(netBasedCosts);
		// VehicleRoutingProblem problem = vrpBuilder.build();

		VehicleRoutingTransportCostsMatrix distanceMatrix = ConstraintUtilsTry.createMatrix(vrpBuilder, singleCarrier,
				network, netBuilder);

		VehicleRoutingProblem problem = vrpBuilder.build();

		StateManager stateManager = new StateManager(problem);

		StateId distanceStateId = stateManager.createStateId("distance");

		stateManager.addStateUpdater(new DistanceUpdater(distanceStateId, stateManager, distanceMatrix));

		ConstraintManager constraintManager = new ConstraintManager(problem, stateManager);
		constraintManager.addConstraint(
				new DistanceConstraint(distanceStateId, stateManager, distanceMatrix, batteryConstraints),
				ConstraintManager.Priority.CRITICAL);

		// get the algorithm out-of-the-box, search solution and get the best one.
		//
		VehicleRoutingAlgorithm algorithm = Jsprit.Builder.newInstance(problem)
				.setStateAndConstraintManager(stateManager, constraintManager)
				.setProperty(Strategy.RADIAL_REGRET.toString(), "1.").buildAlgorithm();
//			VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);
		algorithm.setMaxIterations(jspritIteration);
		Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
		VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);
		costsJsprit = costsJsprit + bestSolution.getCost();

		// Routing bestPlan to Network
		CarrierPlan carrierPlanServices = MatsimJspritFactory.createPlan(singleCarrier, bestSolution);
		NetworkRouter.routePlan(carrierPlanServices, netBasedCosts);
		singleCarrier.setSelectedPlan(carrierPlanServices);
		noDelivery = noDelivery + bestSolution.getUnassignedJobs().size();
		noShipments = problem.getJobs().size();
		// noDeliveryList = bestSolution.getUnassignedJobs();
//		new Plotter(problem, bestSolution).plot(
//				scenario.getConfig().controler().getOutputDirectory() + "/jsprit_CarrierPlans.png", "bestSolution");

		new CarrierPlanXmlWriterV2(carriers)
				.write(scenario.getConfig().controler().getOutputDirectory() + "/jsprit_CarrierPlans.xml");

	}

	/**
	 * @param
	 */
	static void scoringAndManagerFactory(Scenario scenario, Carriers carriers, final Controler controler) {
		CarrierScoringFunctionFactory scoringFunctionFactory = createMyScoringFunction2(scenario);
		CarrierPlanStrategyManagerFactory planStrategyManagerFactory = createMyStrategymanager();

		CarrierModule listener = new CarrierModule(carriers, planStrategyManagerFactory, scoringFunctionFactory);
		controler.addOverridingModule(listener);
	}

	/**
	 * @param scenario
	 * @return
	 */
	private static CarrierScoringFunctionFactoryImpl createMyScoringFunction2(final Scenario scenario) {

		return new CarrierScoringFunctionFactoryImpl(scenario.getNetwork());
	}

	/**
	 * @return
	 */
	private static CarrierPlanStrategyManagerFactory createMyStrategymanager() {
		return new CarrierPlanStrategyManagerFactory() {
			@Override
			public GenericStrategyManager<CarrierPlan, Carrier> createStrategyManager() {
				return null;
			}
		};
	}

}