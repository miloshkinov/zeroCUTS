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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.cadyts.car.CadytsCarModule;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsPlanChanger;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import picocli.CommandLine;

/**
 * @author Ricardo Ewert
 *
 */
@CommandLine.Command(name = "generate-business-passenger-traffic", description = "Generate business passenger traffic model", showDefaultValues = true)

public class RunMATSimCommercialTraffic implements Callable<Integer> {

	private static final Logger log = LogManager.getLogger(RunMATSimCommercialTraffic.class);

	@CommandLine.Option(names = "INPUT", description = "Path to the config file.", defaultValue = "../public-svn/matsim/scenarios/countries/de/berlin/projects/zerocuts/small-scale-commercial-traffic/input/leipzig/scenarios/1pct_bothTypes/")
	private static Path inputPath;

	@CommandLine.Option(names = "--scale", description = "Scale of the input.", required = true, defaultValue = "0.01")
	private double inputScale;

	@CommandLine.Option(names = "--addLongDistanceFreight", description = "If it is set to true the long distance freight will be read in from related plans file. If you need no long distance freight traffic or the traffic is already included to the plans file, you should set this to false.", required = true, defaultValue = "false")
	private boolean addLongDistanceFreight;
	
	@CommandLine.Option(names = "--cadytsCalibration", description = "If cadyts calibartion should be used.", required = true, defaultValue = "false")
	private boolean cadytsCalibration;

	public static void main(String[] args) {
		System.exit(new CommandLine(new RunMATSimCommercialTraffic()).execute(args));
	}

	@Override
	public Integer call() throws Exception {
		
		Config config = ConfigUtils.loadConfig(inputPath.resolve("config.xml").toString());
		if (cadytsCalibration)
			config.addModule(new CadytsConfigGroup());
		String modelName = inputPath.getParent().getParent().getFileName().toString();
		String usedTrafficType = inputPath.getFileName().toString().split("pct_")[1];
		String sampleName = null;

		if ((inputScale * 100) % 1 == 0)
			sampleName = String.valueOf((int) (inputScale * 100));
		else
			sampleName = String.valueOf((inputScale * 100));
		config.controler().setOutputDirectory(Path.of(config.controler().getOutputDirectory()).resolve(modelName)
				.resolve(usedTrafficType.toString() + "_" + sampleName + "pct" + "_"
						+ java.time.LocalDate.now().toString() + "_" + java.time.LocalTime.now().toSecondOfDay())
				.toString() + "_run");
		log.info("Output folder is set to: " + config.controler().getOutputDirectory());
		new OutputDirectoryHierarchy(config.controler().getOutputDirectory(), config.controler().getRunId(),
				config.controler().getOverwriteFileSetting(), ControlerConfigGroup.CompressionType.gzip);
		config.counts().setCountsScaleFactor(1 / inputScale);
		config.counts().setAnalyzedModes("freight");
		config.qsim().setFlowCapFactor(inputScale);
		config.qsim().setStorageCapFactor(inputScale);
		config.qsim().setPcuThresholdForFlowCapacityEasing(0.5);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		if (addLongDistanceFreight)
			addLongDistanceFreightTraffic(scenario);
		createActivityParams(scenario);

		Controler controler = prepareControler(scenario);
		if (cadytsCalibration)
			controler.addOverridingModule(new CadytsCarModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
//		        bind(MainModeIdentifier.class).to(MainModeIdentifierImpl.class);
//		        bind(AnalysisMainModeIdentifier.class).to(MainModeIdentifier.class);
				bind(AnalysisMainModeIdentifier.class).to(TransportPlanningMainModeIdentifierRE.class);
				if (cadytsCalibration)
					addPlanStrategyBinding("planChangerCadyts").toProvider(new javax.inject.Provider<PlanStrategy>() {
						@Inject
						Scenario scenario;
						@Inject
						CadytsContext cadytsContext;

						@Override
						public PlanStrategy get() {
							return new PlanStrategyImpl.Builder((new CadytsPlanChanger(scenario, cadytsContext)))
									.build();
						}
					});
			}
		});
		// include cadyts into the plan scoring (this will add the cadyts corrections to
		// the scores)
		if (cadytsCalibration)
			controler.setScoringFunctionFactory(new ScoringFunctionFactory() {

				@Inject
				CadytsContext cadytsContext;
				@Inject
				ScoringParametersForPerson parameters;

				@Override
				public ScoringFunction createNewScoringFunction(Person person) {
					final ScoringParameters params = parameters.getScoringParameters(person);

					SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
					scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params,
							controler.getScenario().getNetwork(), config.transit().getTransitModes()));
					scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params));
					scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
					final CadytsScoring<Link> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(), config,
							cadytsContext);
					scoringFunction.setWeightOfCadytsCorrection(30. * config.planCalcScore().getBrainExpBeta());
					scoringFunctionAccumulator.addScoringFunction(scoringFunction);

					return scoringFunctionAccumulator;
				}
			});

		controler.run();

		return 0;
	}

	/**
	 * The long distance freight traffic will be added.
	 * 
	 * @param scenario
	 */
	private void addLongDistanceFreightTraffic(Scenario scenario) {

		scenario.getPopulation().getPersons().values().forEach(person -> {
			if (PopulationUtils.getSubpopulation(person).equals("longDistanceFreight")) {
				log.error("The input population already has agents for the long distance traffic. Please check!");
			}
		});
		String modelName = inputPath.getParent().getParent().getFileName().toString();
		Path longDistancePopulation = null;
		if ((inputScale * 100) % 1 == 0)
			longDistancePopulation = inputPath
					.resolve(modelName + "_longDistanceFreight_" + (int) (inputScale * 100) + "pct.xml.gz");
		else
			longDistancePopulation = inputPath
					.resolve(modelName + "_longDistanceFreight_" + (inputScale * 100) + "pct.xml.gz");

		if (!Files.exists(longDistancePopulation)) {
			log.error("Required population for the long distance freight {} not found", longDistancePopulation);
		}
		Population longDistanceFreightPopulation = PopulationUtils.readPopulation(longDistancePopulation.toString());

		longDistanceFreightPopulation.getPersons().values()
				.forEach(person -> VehicleUtils.insertVehicleIdsIntoAttributes(
						scenario.getPopulation().getPersons().get(person.getId()), (new HashMap<String, Id<Vehicle>>() {
							{
								put("freight", Id.createVehicleId(person.getId().toString()));
							}
						})));
		longDistanceFreightPopulation.getPersons().values()
				.forEach(person -> scenario.getVehicles().addVehicle(VehicleUtils.createVehicle(
						Id.createVehicleId(person.getId().toString()),
						scenario.getVehicles().getVehicleTypes().get(Id.create("medium18t", VehicleType.class)))));
		longDistanceFreightPopulation.getPersons().values()
				.forEach(person -> scenario.getPopulation().addPerson(person));

		log.info(longDistanceFreightPopulation.getPersons().size() + " Agents for the long distance freight added");
	}

	/**
	 * Create Activity parameter for planCalcScore.
	 * 
	 * @param scenario
	 */
	private void createActivityParams(Scenario scenario) {
		Population population = scenario.getPopulation();
		Config config = scenario.getConfig();
		int i = 0;
		int countPersons = 0;
		for (Person person : population.getPersons().values()) {
			countPersons++;
			if (countPersons % 1000 == 0)
				log.info("Activities for " + countPersons + " of " + population.getPersons().size()
						+ " persons generated.");
			double tourStartTime = 0;
			for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
				if (planElement instanceof Activity) {
					i++;
					String newTypeName = ((Activity) planElement).getType().toString() + "_" + i;
					((Activity) planElement).setType(newTypeName);
					if (newTypeName.contains("service")) {
						config.planCalcScore()
								.addActivityParams(new ActivityParams(newTypeName)
										.setTypicalDuration(((Activity) planElement).getMaximumDuration().seconds())
										.setOpeningTime(tourStartTime).setClosingTime(tourStartTime + 8. * 3600.));
						continue;
					}
					if (newTypeName.contains("start") && !newTypeName.contains("freight")) {
						tourStartTime = ((Activity) planElement).getEndTime().seconds();
						config.planCalcScore()
								.addActivityParams(new ActivityParams(newTypeName).setOpeningTime(6. * 3600.)
										.setClosingTime(20. * 3600.).setLatestStartTime(tourStartTime)
										.setEarliestEndTime(6. * 3600.).setTypicalDuration(5 * 60));
						continue;
					}
					if (newTypeName.contains("end") && !newTypeName.contains("freight")) {
						double tourEndTime = tourStartTime + 9 * 3600;
						if (tourEndTime > 24 * 3600)
							tourEndTime = 24 * 3600;
						config.planCalcScore()
								.addActivityParams(new ActivityParams(newTypeName)
										.setOpeningTime(tourStartTime + 6 * 3600).setClosingTime(tourEndTime)
										.setLatestStartTime(tourEndTime).setTypicalDuration(5 * 60));
						continue;
					}
					if (newTypeName.contains("freight_start")) {
						tourStartTime = ((Activity) planElement).getEndTime().seconds();
						config.planCalcScore().addActivityParams(new ActivityParams(newTypeName).setTypicalDuration(1));
						continue;
					}
					if (newTypeName.contains("freight_end")) {
						config.planCalcScore().addActivityParams(new ActivityParams(newTypeName).setTypicalDuration(1));
						continue;
					}
				}
			}
		}
	}

	/**
	 * Prepares the controller.
	 * 
	 * @param scenario
	 * @return
	 */
	private Controler prepareControler(Scenario scenario) {
		Controler controler = new Controler(scenario);

		if (controler.getConfig().transit().isUseTransit()) {
			// use the sbb pt raptor router
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					install(new SwissRailRaptorModule());
				}
			});
		}
		return controler;
	}
}
