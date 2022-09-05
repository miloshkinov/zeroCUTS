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
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import picocli.CommandLine;

/**
 * @author Ricardo Ewert
 *
 */
@CommandLine.Command(name = "generate-business-passenger-traffic", description = "Generate business passenger traffic model", showDefaultValues = true)

public class RunMATSimCommercialTraffic implements Callable<Integer> {

	private static final Logger log = LogManager.getLogger(RunMATSimCommercialTraffic.class);

	@CommandLine.Option(names = "INPUT", description = "Path to the config file.", defaultValue = "../public-svn/matsim/scenarios/countries/de/berlin/projects/zerocuts/small-scale-commercial-traffic/input/scenarios/5pct_bothTypes/")
	private static Path inputPath;

	@CommandLine.Option(names = "--output", description = "Path to output folder", required = true, defaultValue = "output/BusinessPassengerTraffic_MATSim/")
	private Path output;

	@CommandLine.Option(names = "--scale", description = "Scale of the input.", required = true, defaultValue = "0.05")
	private double inputScale;

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
		config.vehicles().setVehiclesFile("berlin_bothTypes_5pct_allVehicles.xml.gz");
		config.plans().setInputFile("berlin_bothTypes_5pct_plans.xml.gz");

		Scenario scenario = ScenarioUtils.loadScenario(config);
		createActivityParams(scenario);

		Controler controler = prepareControler(scenario);

		controler.addOverridingModule(new CadytsCarModule());

		// include cadyts into the plan scoring (this will add the cadyts corrections to
		// the scores):
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

	/** Create Activity parameter for planCalcScore.
	 * @param scenario
	 */
	private void createActivityParams(Scenario scenario) {
		Population population = scenario.getPopulation();
		Config config = scenario.getConfig();
		int i = 0;
		for (Person person : population.getPersons().values()) {
			double tourStartTime = 0;
			for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
				if (planElement instanceof Activity) {
					i++;
					String newTypeName = ((Activity) planElement).getType().toString() + "_" + i;
					((Activity) planElement).setType(newTypeName);
					if (newTypeName.contains("service"))
						config.planCalcScore()
								.addActivityParams(new ActivityParams(newTypeName)
										.setTypicalDuration(((Activity) planElement).getMaximumDuration().seconds())
										.setOpeningTime(tourStartTime).setClosingTime(tourStartTime + 8. * 3600.));
					if (newTypeName.contains("start")) {
						tourStartTime = ((Activity) planElement).getEndTime().seconds();
						config.planCalcScore().addActivityParams(
								new ActivityParams(newTypeName).setTypicalDuration(1).setOpeningTime(6. * 3600.)
										.setClosingTime(20. * 3600.).setLatestStartTime(tourStartTime)
										.setEarliestEndTime(6. * 3600.).setTypicalDuration(5 * 60));
					}
					if (newTypeName.contains("end")) {
						double tourEndTime = tourStartTime + 9 * 3600;
						if (tourEndTime > 24 * 3600)
							tourEndTime = 24 * 3600;
						config.planCalcScore()
								.addActivityParams(new ActivityParams(newTypeName).setTypicalDuration(1)
										.setOpeningTime(tourStartTime + 6 * 3600).setClosingTime(tourEndTime)
										.setLatestStartTime(tourEndTime).setTypicalDuration(5 * 60));
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
