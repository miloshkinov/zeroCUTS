/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.vsp.freight.food;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;
import org.matsim.freight.carriers.analysis.RunFreightAnalysisEventBased;
import org.matsim.freight.carriers.controler.CarrierModule;
import org.matsim.freight.carriers.controler.CarrierScoringFunctionFactory;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.VehicleUtils;
import picocli.CommandLine;

import java.nio.file.Path;

@CommandLine.Command(name = "solve-food-scenarios", description = "Solves the food scenarios", showDefaultValues = true)

class RunFood implements MATSimAppCommand {

	static final Logger log = LogManager.getLogger(RunFood.class);

	@CommandLine.Option(names = "--carriersFilePath", description = "Path to the carriers file.", required = true, defaultValue = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/input_2024/I-Base_carrierLEH_v2_withFleet_Shipment_OneTW_PickupTime.xml")
	private static Path carriersFilePath;

	@CommandLine.Option(names = "--vehicleTypesFilePath", description = "Path to the vehicleTypes file.", required = true, defaultValue = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/input_2024/vehicleTypes_Food_2024.xml")
	private static Path vehicleTypesFilePath;

	@CommandLine.Option(names = "--nuOfJspritIteration", description = "Sets the number of jsprit iterations.", required = true, defaultValue = "1")
	private static int nuOfJspritIteration;

	@CommandLine.Option(names = "--networkChangeEventsFile", description = "Path to the networkChangeEvents file.", defaultValue = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/input_2024/networkChangeEvents_Berlin_V6.0-10pct.xml.gz")
	private static String networkChangeEventsFileLocation;

	@CommandLine.Option(names = "--outputLocation", description = "Path to the output location.", required = true, defaultValue = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/input_2024/test/output")
	private static String outputLocation;

	@CommandLine.Option(names = "--networkPath", description = "Path to the network.", required = true, defaultValue = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v6.0/input/berlin-v6.0-network.xml.gz")
	private static String networkPath;

	@CommandLine.Option(names = "--useDistanceConstraint", description = "Use distance constraint for tour planning.")
	private static boolean useDistanceConstraint;

	@CommandLine.Option(names = "--calculateVariableConsumptionCosts", description = "Recalculate variable costs based on the given.")
	private static boolean calculateVariableConsumptionCosts;

	@CommandLine.Option(names = "--fuelCosts_EUR_per_l", description = "Fuel costs in EUR per liter.", required = true, defaultValue = "1.23") //TODO check: vwlich oder gewerbl.??
	private static double fuelCosts_EUR_per_l;

	@CommandLine.Option(names = "--energyCosts_EUR_per_kWh", description = "Energy costs in EUR per kWh.", required = true, defaultValue = "0.2216") //TODO check: vwlich oder gewerbl.??
	private static double energyCosts_EUR_per_kWh;

	public static void main(String[] args) {
		System.exit(new CommandLine(new RunFood()).execute(args));
	}

	@Override
	public Integer call() throws Exception {

//		calculateVariableConsumptionCosts = true;

		Config config = prepareConfig();
		Scenario scenario = prepareScenario(config);

		if (calculateVariableConsumptionCosts) {
			log.warn("We assume that the variable costs in the vehicleTypes don't contain fuel and energy costs.");
			log.warn("Recalculating variable costs based on the given fuel and energy costs.");
			log.warn("Fuel costs: {} EUR per liter.", fuelCosts_EUR_per_l);
			log.warn("Energy costs: {} EUR per kWh.", energyCosts_EUR_per_kWh);
			calculateVariableConsumptionCosts(scenario, fuelCosts_EUR_per_l, energyCosts_EUR_per_kWh);
			outputLocation = outputLocation + "_fuel" + fuelCosts_EUR_per_l + "_energy" + energyCosts_EUR_per_kWh + "/";
			config.controller().setOutputDirectory(outputLocation);
		}

		Controler controler = prepareControler(scenario);

		CarriersUtils.runJsprit(scenario);

		//The VSP default settings are designed for person transport simulation. After talking to Kai, they will be set to WARN here. Kai MT may'23
		controler.getConfig().vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
		controler.run();

		final String outputPath = controler.getControlerIO().getOutputPath();
		RunFreightAnalysisEventBased freightAnalysis = new RunFreightAnalysisEventBased(outputPath + "/", outputPath + "/Analysis/",
				config.global().getCoordinateSystem());
		freightAnalysis.runAnalysis();

		return 0;
	}

	/**
	 * Recalculates the variable costs based on the given fuel and energy costs.
	 * Assumes that the variable costs in the vehicleTypes don't contain fuel and energy costs.
	 *
	 * @param scenario             the scenario
	 * @param fuelCostsEurPerL     fuel costs in EUR per liter
	 * @param energyCostsEurPerKWh energy costs in EUR per kWh
	 */
	private void calculateVariableConsumptionCosts(Scenario scenario, double fuelCostsEurPerL, double energyCostsEurPerKWh) {
		CarriersUtils.getCarrierVehicleTypes(scenario).getVehicleTypes().values().forEach(vehicleType -> {
			EngineInformation engineInformation = vehicleType.getEngineInformation();
			String engineType = VehicleUtils.getHbefaTechnology(engineInformation);
			double basicVariableCostsPerMeter = vehicleType.getCostInformation().getCostsPerMeter(); // existing variable costs based on Kostensätze file
			double energieCostsPerMeter;
			if (engineType.equals("electricity")){
				double energyConsumptionKWhPerMeter = VehicleUtils.getEnergyConsumptionKWhPerMeter(engineInformation);
				energieCostsPerMeter = energyConsumptionKWhPerMeter * energyCostsEurPerKWh;

			} else if (engineType.equals("diesel")) {
				double fuelConsumptionLitersPerMeter = VehicleUtils.getFuelConsumption(vehicleType);
				energieCostsPerMeter = fuelConsumptionLitersPerMeter * fuelCostsEurPerL;

			} else
				throw new IllegalArgumentException("EngineType not supported: " + engineType);

			double newVariableCosts = basicVariableCostsPerMeter + energieCostsPerMeter;
			vehicleType.getCostInformation().setCostsPerMeter(newVariableCosts);
		});
	}


	private static Config prepareConfig() {
//        String algorithmFileLocation = args[2]; //TODO: Read in Algorithm -> Put into freightCarriersConfigGroup?

		Config config = ConfigUtils.createConfig();
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.global().setRandomSeed(4177);
		config.global().setCoordinateSystem("EPSG:25832");
		config.controller().setLastIteration(0);
		config.controller().setOutputDirectory(outputLocation);

		config.network().setInputFile(networkPath);

		if (networkChangeEventsFileLocation == null || networkChangeEventsFileLocation.isEmpty()){
			log.info("Setting networkChangeEventsInput file: " + networkChangeEventsFileLocation);
			config.network().setTimeVariantNetwork(true);
			config.network().setChangeEventsInputFile(networkChangeEventsFileLocation);
		}

		config.plans().setActivityDurationInterpretation(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration );
		//freight configstuff
		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightCarriersConfigGroup.setCarriersFile(carriersFilePath.toString());
		freightCarriersConfigGroup.setCarriersVehicleTypesFile(vehicleTypesFilePath.toString());
		freightCarriersConfigGroup.setTravelTimeSliceWidth(1800);
		freightCarriersConfigGroup.setTimeWindowHandling(FreightCarriersConfigGroup.TimeWindowHandling.enforceBeginnings);

		if(useDistanceConstraint) {
			freightCarriersConfigGroup.setUseDistanceConstraintForTourPlanning(FreightCarriersConfigGroup.UseDistanceConstraintForTourPlanning.basedOnEnergyConsumption);
		}

		return config;
	}

	private static Scenario prepareScenario(Config config) {
		Scenario scenario = ScenarioUtils.loadScenario(config);

		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);
		CarriersUtils.getCarriers(scenario).getCarriers().values().forEach(carrier -> {
			CarriersUtils.setJspritIterations(carrier, nuOfJspritIteration);
			});
		return scenario;
	}
	private static Controler prepareControler(Scenario scenario) {
		Controler controller = new Controler(scenario);

		controller.addOverridingModule(new CarrierModule());

		controller.addOverridingModule(new AbstractModule() {
			@Override public void install() {
				bind(CarrierScoringFunctionFactory.class).toInstance(new CarrierScoringFunctionFactory_KeepScore());
			}
		});

		return controller;
	}

	private static class CarrierScoringFunctionFactory_KeepScore implements CarrierScoringFunctionFactory {
		@Override public ScoringFunction createScoringFunction(Carrier carrier ){
			return new ScoringFunction(){
				@Override public void handleActivity( Activity activity ){
				}
				@Override public void handleLeg( Leg leg ){
				}
				@Override public void agentStuck( double time ){
				}
				@Override public void addMoney( double amount ){
				}
				@Override public void addScore( double amount ){
				}
				@Override public void finish(){
				}
				@Override public double getScore(){
					return CarriersUtils.getJspritScore(carrier.getSelectedPlan()); //2nd Quickfix: Keep the current score -> which ist normally the score from jsprit. -> Better safe JspritScore as own value.
//					return Double.MIN_VALUE; // 1st Quickfix, to have a "double" value for xsd (instead of neg.-Infinity).
//					return Double.NEGATIVE_INFINITY; // Default from KN -> causes errors with reading in carrierFile because Java writes "Infinity", while XSD needs "INF"
				}
				@Override public void handleEvent( Event event ){
				}
			};
		}
	}
}
