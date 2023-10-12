/*
  * *********************************************************************** *
  * project: org.matsim.*
  * *********************************************************************** *
  *                                                                         *
  * copyright       : (C) 2020 by the members listed in the COPYING,        *
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
package org.matsim.vsp.freight;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.freight.carriers.FreightConfigGroup;
import org.matsim.freight.carriers.carrier.*;
import org.matsim.freight.carriers.controler.CarrierModule;
import org.matsim.freight.carriers.controler.CarrierScoringFunctionFactory;
import org.matsim.freight.carriers.controler.CarrierStrategyManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.consistency.VspConfigConsistencyCheckerImpl;
import org.matsim.core.config.groups.ControllerConfigGroup.CompressionType;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.controler.*;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.freight.carriers.controler.CarrierControlerUtils;
import org.matsim.vehicles.VehicleType;
import org.osgeo.proj4j.UnsupportedParameterException;

import javax.management.InvalidAttributeValueException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * This is a short an easy version to run MATSim freight scenarios .
 * Optional it is possible to run MATSim after tour planning.
 * @author kturner
 */
public class RunFreightOnlyMatsim {

	private static final Logger log = LogManager.getLogger(RunFreightOnlyMatsim.class);
	private static final Level loggingLevel = Level.INFO; 		//Set to info to avoid all Debug-Messages, e.g. from VehicleRountingAlgorithm, but can be set to other values if needed. KMT feb/18. 

	private enum CostsModififier {av, avFix110pct, avDist110pct, avVehCapUp}
	private final static CostsModififier costsModififier = null;

	//Beginn Namesdefinition KT Für Berlin-Szenario 
	private static final String INPUT_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/08_ICEVBEV_NwCE_BVWP_2000it_Tax300/";

	private static final String OUTPUT_DIR = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/newMATSimRun/08_ICEVBEV_NwCE_BVWP_2000it_Tax300/" ;
	private static final String LOG_DIR = OUTPUT_DIR + "Logs/";

	//Dateinamen
	private static final String NETFILE_NAME = "output_network.xml.gz" ;
	private static final String CARRIERFILE_NAME = "output_carriers2.xml.gz";
	private static final String VEHTYPEFILE_NAME = "output_vehicleTypes.xml" ;
	private static final String NWCEFILE_NAME = "output_change_events.xml.gz";

	private static final String NETFILE = INPUT_DIR + NETFILE_NAME ;
	private static final String VEHTYPEFILE = INPUT_DIR + VEHTYPEFILE_NAME;
	private static final String CARRIERFILE = INPUT_DIR + CARRIERFILE_NAME;

	private static final String NETWORKCHANGEEVENTFILE = INPUT_DIR + NWCEFILE_NAME;

	// Einstellungen für den Run	
	private static final boolean runMatsim = true;	 //when false only jsprit run will be performed
	private static final int LAST_MATSIM_ITERATION = 0;  //only one iteration for writing events.
	private static final int MAX_JSPRIT_ITERATION = 1;

	private static Config config;

	public static void main(String[] args) throws IOException, InvalidAttributeValueException {
		LogManager.getRootLogger().atLevel(loggingLevel);
		OutputDirectoryLogging.initLoggingWithOutputDirectory(LOG_DIR);

		log.info("#### Starting Run: ");

		// ### config stuff: ###	
		config = createConfig();

		// ### scenario stuff: ###
		Scenario scenario = ScenarioUtils.loadScenario(config);

		//Building the Carriers with jsprit, incl jspritOutput KT 03.12.2014
		jspritRun();

		if (runMatsim){
			matsimRun(scenario);	//final MATSim configurations and start of the MATSim-Run
		}

		writeRunInfo();	

		log.info("#### End of all runs ####");
		OutputDirectoryLogging.closeOutputDirLogging(); 

		System.out.println("#### Finished ####");
	}

	private static Config createConfig() {
		Config config = ConfigUtils.createConfig() ;

		config.controller().setOutputDirectory(OUTPUT_DIR);

		// (the directory structure is needed for jsprit output, which is before the
		// controler starts. Maybe there is a better alternative ...)
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		new OutputDirectoryHierarchy(config.controller().getOutputDirectory(), config.controller().getRunId(), config.controller().getOverwriteFileSetting(), CompressionType.gzip);
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);

		config.controller().setLastIteration(LAST_MATSIM_ITERATION);	
		config.network().setInputFile(NETFILE);

		config.network().setChangeEventsInputFile(NETWORKCHANGEEVENTFILE);
		config.network().setTimeVariantNetwork(true);

		//Damit nicht alle um Mitternacht losfahren
		config.plans().setActivityDurationInterpretation(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration ); 

		//Some config stuff to comply to vsp-defaults even there is currently only 1 MATSim iteration and 
		//therefore no need for e.g. a strategy! KMT jan/18
		config.scoring().setFractionOfIterationsToStartScoreMSA(0.8);
		config.plans().setRemovingUnneccessaryPlanAttributes(true);
		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);
		config.qsim().setUsingTravelTimeCheckInTeleportation(true);
		config.qsim().setTrafficDynamics(TrafficDynamics.kinematicWaves);
		config.replanning().setFractionOfIterationsToDisableInnovation(0.8);

		StrategySettings stratSettings1 = new StrategySettings();
		stratSettings1.setStrategyName("ChangeExpBeta");
		stratSettings1.setWeight(0.1);
		config.replanning().addStrategySettings(stratSettings1);

		StrategySettings stratSettings2 = new StrategySettings();
		stratSettings2.setStrategyName("BestScore");
		stratSettings2.setWeight(0.9);
		config.replanning().addStrategySettings(stratSettings2);

		config.vspExperimental().setVspDefaultsCheckingLevel(VspDefaultsCheckingLevel.warn);
		config.addConfigConsistencyChecker(new VspConfigConsistencyCheckerImpl());
		ControlerUtils.checkConfigConsistencyAndWriteToLog(config, "dump");

		return config;
	}  //End createConfig

	private static void jspritRun() {
		CarrierVehicleTypes vehicleTypes = createVehicleTypes();
		modifyVehicleTypes(vehicleTypes);

		createCarriers(vehicleTypes);
	}

	private static void modifyVehicleTypes(CarrierVehicleTypes vehicleTypes) {
		for (VehicleType vt : vehicleTypes.getVehicleTypes().values()) {
			if (costsModififier != null) {
				switch (costsModififier) {
				case av:
					vt.getCostInformation().setCostsPerSecond(0.0);
					break;
				case avDist110pct:
					vt.getCostInformation().setCostsPerSecond(0.0);
					vt.getCostInformation().setCostsPerMeter(vt.getCostInformation().getCostsPerMeter() *1.1);
					break;
				case avFix110pct:
					vt.getCostInformation().setCostsPerSecond(0.0);
					vt.getCostInformation().setFixedCost(vt.getCostInformation().getFixedCosts() *1.1);
					break;
				case avVehCapUp:
					if (vt.getId().toString().endsWith("_frozen")) {
						vt.getCostInformation().setCostsPerSecond(0.0);
						vt.getCapacity().setOther(vt.getCapacity().getOther() + 14);		// more trolleys instead of the drivers cabin

					} else {
						vt.getCostInformation().setCostsPerSecond(0.0);
						vt.getCapacity().setOther(vt.getCapacity().getOther() + 2);			// two additional palates instead of the drivers cabin
					}
				default:
					log.error("Unspecified modification for carrierVehicleTypeCosts selected", new UnsupportedParameterException());
				}
			} else {
				log.info("No modification for carrierVehicleTypeCosts selected" );
			}

		}
	}

	private static Carriers createCarriers(CarrierVehicleTypes vehicleTypes) {
		Carriers carriers = new Carriers() ;
		new CarrierPlanXmlReader(carriers, vehicleTypes).readFile(CARRIERFILE) ;
		return carriers;
	}

	private static CarrierVehicleTypes createVehicleTypes() {
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
		new CarrierVehicleTypeReader(vehicleTypes).readFile(VEHTYPEFILE) ;
		return vehicleTypes;
	}


	/**
	 * Run the MATSim simulation
	 * 
	 * @param scenario
	 */
	private static void matsimRun(Scenario scenario) {
		final Controler controler = new Controler( scenario ) ;

		CarrierScoringFunctionFactory scoringFunctionFactory = createMyScoringFunction2(scenario);
		CarrierStrategyManager planStrategyManagerFactory =  createMyStrategymanager(); //Benötigt, da listener kein "Null" als StrategyFactory mehr erlaubt, KT 17.04.2015

		FreightConfigGroup freightConfig = ConfigUtils.addOrGetModule( scenario.getConfig(), FreightConfigGroup.class );
		freightConfig.setTimeWindowHandling(FreightConfigGroup.TimeWindowHandling.enforceBeginnings);

		CarrierUtils.addOrGetCarriers(scenario);
		CarrierModule listener = new CarrierModule();
		controler.addOverridingModule( new AbstractModule(){
			@Override
			public void install(){
				bind( CarrierScoringFunctionFactory.class ).toInstance(scoringFunctionFactory) ;
				bind( CarrierStrategyManager.class ).toInstance(planStrategyManagerFactory);
			}
		} ) ;
		controler.addOverridingModule(listener);
		controler.run();
	}


	//Benötigt, da listener kein "Null" als StrategyFactory mehr erlaubt, KT 17.04.2015
	//Da keine Strategy notwendig, hier zunächst eine "leere" Factory
	private static CarrierStrategyManager createMyStrategymanager() {
		return CarrierControlerUtils.createDefaultCarrierStrategyManager();
	}





	/**
	 * TODO:  Activity: Kostensatz mitgeben, damit klar ist, wo er herkommt... oder vlt geht es in dem Konstrukt doch aus den Veh-Eigenschaften?? (KT, 17.04.15)
	 * TODO: Default CarrierScoringFunctionFactoryImpl in Freight contrib hinterlegen
	 */
	private static CarrierScoringFunctionFactoryImpl_KT createMyScoringFunction2 (final Scenario scenario) {

		return new CarrierScoringFunctionFactoryImpl_KT(scenario, scenario.getConfig().controller().getOutputDirectory()) {

			public ScoringFunction createScoringFunction(final Carrier carrier){
				SumScoringFunction sumSf = new SumScoringFunction() ;

				VehicleFixCostScoring fixCost = new VehicleFixCostScoring(carrier);
				sumSf.addScoringFunction(fixCost);

				LegScoring legScoring = new LegScoring(carrier);
				sumSf.addScoringFunction(legScoring);

				//Score Activity w/o correction of waitingTime @ 1st Service. -> Waiting time before first service will be scored
				//			ActivityScoring actScoring = new ActivityScoring(carrier);
				//			sumSf.addScoringFunction(actScoring);

				//Alternativ:
				//Score Activity with correction of waitingTime @ 1st Service. -> Ignore waiting time before first service 
				ActivityScoringWithCorrection actScoring = new ActivityScoringWithCorrection(carrier);
				sumSf.addScoringFunction(actScoring);

				return sumSf;
			}
		};
	}

	/**
	 * Write out the information about the datas provided for the simulation run.
	 */
	private static void writeRunInfo() {
		File file = new File(OUTPUT_DIR + "#RunInformation.txt");
		try {
			FileWriter writer = new FileWriter(file);  //Neuer File (überschreibt im Zweifel den alten - der jedoch nicht existieren dürfte!
			writer.write("System date and time writing this file: " + LocalDateTime.now() + System.getProperty("line.separator") + System.getProperty("line.separator"));

			writer.write("##Inputfiles:" +System.getProperty("line.separator"));
			writer.write("Input-Directory: " + INPUT_DIR);
			writer.write("Net: \t \t" + NETFILE_NAME +System.getProperty("line.separator"));
			writer.write("Carrier:  \t" + CARRIERFILE_NAME +System.getProperty("line.separator"));
			writer.write("VehType: \t" + VEHTYPEFILE_NAME +System.getProperty("line.separator"));
			//			writer.write("Algorithm: \t" + ALGORITHMFILE_NAME +System.getProperty("line.separator"));

			writer.write(System.getProperty("line.separator"));
			writer.write("##Run Settings:" +System.getProperty("line.separator"));
			writer.write("runMatsim: \t \t" + runMatsim +System.getProperty("line.separator"));
			writer.write("Last Matsim Iteration: \t" + LAST_MATSIM_ITERATION +System.getProperty("line.separator"));
			writer.write("Max jsprit Iteration: \t" + MAX_JSPRIT_ITERATION +System.getProperty("line.separator"));

			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Datei: " + file + " geschrieben.");
	}

}

