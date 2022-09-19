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
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.PopulationUtils;
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

	@CommandLine.Option(names = "INPUT", description = "Path to the config file.", defaultValue = "../public-svn/matsim/scenarios/countries/de/berlin/projects/zerocuts/small-scale-commercial-traffic/input/scenarios/10pct_bothTypes/")
	private static Path inputPath;

	@CommandLine.Option(names = "--output", description = "Path to output folder", required = true, defaultValue = "output/BusinessPassengerTraffic_MATSim/")
	private Path output;

	@CommandLine.Option(names = "--scale", description = "Scale of the input.", required = true, defaultValue = "0.10")
	private double inputScale;

	@CommandLine.Option(names = "--addLongDistanceFreight", description = "If it is set to true the long distance freight will be read in from related plans file. If you need no long distance freight traffic or the traffic is already included to the plans file, you should set this to false.", required = true, defaultValue = "false")
	private boolean addLongDistanceFreight;

	public static void main(String[] args) {
		System.exit(new CommandLine(new RunMATSimCommercialTraffic()).execute(args));
	}

	@Override
	public Integer call() throws Exception {

		Config config = ConfigUtils.loadConfig(inputPath.resolve("config.xml").toString(), new CadytsConfigGroup());
		output = output.resolve(java.time.LocalDate.now().toString() + "_" + java.time.LocalTime.now().toSecondOfDay());
		config.controler().setOutputDirectory(output.toString());
		new OutputDirectoryHierarchy(config.controler().getOutputDirectory(), config.controler().getRunId(),
				config.controler().getOverwriteFileSetting(), ControlerConfigGroup.CompressionType.gzip);
		config.counts().setInputFile("../../counts_berlin_Lkw.xml");
		config.counts().setCountsScaleFactor(1 / inputScale);
		config.counts().setAnalyzedModes("freight");
		config.qsim().setFlowCapFactor(inputScale);
		config.qsim().setStorageCapFactor(inputScale);
		config.qsim().setPcuThresholdForFlowCapacityEasing(0.5);
		config.vehicles().setVehiclesFile("berlin_bothTypes_" + (int) (inputScale * 100) + "pct_allVehicles.xml.gz");
		config.plans().setInputFile("berlin_bothTypes_" + (int) (inputScale * 100) + "pct_plans.xml.gz");

		Scenario scenario = ScenarioUtils.loadScenario(config);
		if (addLongDistanceFreight)
			addLongDistanceFreightTraffic(scenario);
		createActivityParams(scenario);

		Controler controler = prepareControler(scenario);

		controler.addOverridingModule(new CadytsCarModule());

		// include cadyts into the plan scoring (this will add the cadyts corrections to
		// the scores)
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

	/** The long distance freight traffic will be added.
	 * @param scenario
	 */
	private void addLongDistanceFreightTraffic(Scenario scenario) {

		Population longDistanceFreightPopulation = PopulationUtils.readPopulation(
				inputPath.resolve("berlin_longDistanceFreight_" + (int) (inputScale * 100) + "pct.xml.gz").toString());

		longDistanceFreightPopulation.getPersons().values().forEach(
				person -> VehicleUtils.insertVehicleIdsIntoAttributes(person, (new HashMap<String, Id<Vehicle>>() {
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
