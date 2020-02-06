package org.matsim.vsp.DistanceConstraint;

import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.controler.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.controler.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts.Builder;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.contrib.freight.usecases.chessboard.CarrierScoringFunctionFactoryImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup.CompressionType;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.GenericStrategyManager;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit.Strategy;
import com.graphhopper.jsprit.core.algorithm.state.StateId;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.constraint.ConstraintManager;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.jsprit.core.util.VehicleRoutingTransportCostsMatrix;

class TestRunDistanceConstraintUtils {

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
	static Config prepareConfig(Config config, int lastMATSimIteration) {
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		new OutputDirectoryHierarchy(config.controler().getOutputDirectory(), config.controler().getRunId(),
				config.controler().getOverwriteFileSetting(), CompressionType.gzip);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);

		config.controler().setLastIteration(lastMATSimIteration);
		config.global().setRandomSeed(4177);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);

		return config;
	}

	

	/**
	 * Solves with jsprit and gives a xml output of the plans and a plot of the
	 * solution. Because of using the distance constraint it is necessary to create
	 * a cost matrix before solving the vrp with jsprit. The jsprit algorithm solves
	 * a solution for every created carrier separately.
	 * 
	 * @param
	 */
	static void solveWithJsprit(Scenario scenario, Carriers carriers, int jspritIteration,
			CarrierVehicleTypes vehicleTypes) {

		// Netzwerk integrieren und Kosten für jsprit
		Network network = scenario.getNetwork();
		Builder netBuilder = NetworkBasedTransportCosts.Builder.newInstance(network,
				vehicleTypes.getVehicleTypes().values());
		final NetworkBasedTransportCosts netBasedCosts = netBuilder.build();

		for (Carrier singleCarrier : carriers.getCarriers().values()) {
			
		
		netBuilder.setTimeSliceWidth(1800);

		VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(singleCarrier,
				network);
		vrpBuilder.setRoutingCost(netBasedCosts);
		// VehicleRoutingProblem problem = vrpBuilder.build();

		VehicleRoutingTransportCostsMatrix distanceMatrix = DistanceConstraintUtils.createMatrix(vrpBuilder, singleCarrier,
				network, netBuilder);

		VehicleRoutingProblem problem = vrpBuilder.build();

		StateManager stateManager = new StateManager(problem);

		StateId distanceStateId = stateManager.createStateId("distance");

		stateManager.addStateUpdater(new DistanceUpdater(distanceStateId, stateManager, distanceMatrix));

		ConstraintManager constraintManager = new ConstraintManager(problem, stateManager);
		constraintManager.addConstraint(
				new DistanceConstraint(distanceStateId, stateManager, distanceMatrix, vehicleTypes),
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
		}
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