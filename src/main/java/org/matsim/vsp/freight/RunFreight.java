/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.vsp.freight;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.algorithm.listener.VehicleRoutingAlgorithmListeners.Priority;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.jsprit.analysis.toolbox.StopWatch;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.*;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierPlan;
import org.matsim.freight.carriers.CarrierPlanWriter;
import org.matsim.freight.carriers.CarrierPlanXmlReader;
import org.matsim.freight.carriers.CarrierService;
import org.matsim.freight.carriers.CarrierVehicleTypeReader;
import org.matsim.freight.carriers.CarrierVehicleTypeWriter;
import org.matsim.freight.carriers.CarrierVehicleTypes;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.carriers.ScheduledTour;
import org.matsim.freight.carriers.Tour.ServiceActivity;
import org.matsim.freight.carriers.Tour.TourElement;
import org.matsim.freight.carriers.controller.CarrierControllerUtils;
import org.matsim.freight.carriers.controller.CarrierModule;
import org.matsim.freight.carriers.controller.CarrierScoringFunctionFactory;
import org.matsim.freight.carriers.controller.CarrierStrategyManager;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.jsprit.MatsimJspritFactory;
import org.matsim.freight.carriers.jsprit.NetworkBasedTransportCosts;
import org.matsim.freight.carriers.jsprit.NetworkBasedTransportCosts.Builder;
import org.matsim.freight.carriers.jsprit.NetworkRouter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.consistency.VspConfigConsistencyCheckerImpl;
import org.matsim.core.config.groups.ControllerConfigGroup.CompressionType;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.vehicles.VehicleType;
import org.osgeo.proj4j.UnsupportedParameterException;

/**
 * This is a short an easy version to run MATSim freight scenarios .
 * Optional it is possible to run MATSim after tour planning.
 * @author kturner
 */
public class RunFreight {

	private static final Logger log = LogManager.getLogger(RunFreight.class);
	private static final Level loggingLevel = Level.INFO; 		//Set to info to avoid all Debug-Messages, e.g. from VehicleRoutingAlgorithm, but can be set to other values if needed. KMT feb/18.

	private enum CostsModifier {av, avFix110pct, avDist110pct, avVehCapUp}
	private final static CostsModifier costsModifier = null;

	//Beginn Namesdefinition KT für Berlin-Szenario
	private static final String INPUT_DIR = "scenarios/BerlinFood/";

	private static final String OUTPUT_DIR = "scenarios/BerlinFood/output/Base_ServiceBased/" ;
	private static final String LOG_DIR = "scenarios/BerlinFood/output/Base_ServiceBased_Logs/" ;

	//Dateinamen
	private static final String NETFILE_NAME = "network.xml.gz" ;
	private static final String CARRIERFILE_NAME = "carrierLEH_v2_withFleet_depot.xml";
	private static final String VEHTYPEFILE_NAME = "vehicleTypes.xml" ;

	private static final String NETFILE = INPUT_DIR + NETFILE_NAME ;
	private static final String VEHTYPEFILE = INPUT_DIR + VEHTYPEFILE_NAME;
	private static final String CARRIERFILE = INPUT_DIR + CARRIERFILE_NAME;
	//	private static final String ALGORITHMFILE = INPUT_DIR + ALGORITHMFILE_NAME;

	// Einstellungen für den Run	
	private static final boolean runMatsim = true;	 //when false only jsprit run will be performed
	private static final int LAST_MATSIM_ITERATION = 0;  //only one iteration for writing events.
	private static final int MAX_JSPRIT_ITERATION = 1;

	private static Config config;

	public static void main(String[] args) throws IOException {
		LogManager.getRootLogger().atLevel(loggingLevel);
		OutputDirectoryLogging.initLoggingWithOutputDirectory(LOG_DIR);

		log.info("#### Starting Run: ");

		// ### config stuff: ###	
		config = createConfig(args);

		// ### scenario stuff: ###
		Scenario scenario = ScenarioUtils.loadScenario(config);

		//Building the Carriers with jsprit, incl jspritOutput KT 03.12.2014
		Carriers carriers = jspritRun(config, scenario.getNetwork());

		if (runMatsim){
			matsimRun(scenario);	//final MATSim configurations and start of the MATSim-Run
		}
		writeAdditionalRunOutput(config, carriers);	//write some final Output

		writeRunInfo();	

		log.info("#### End of all runs ####");
		OutputDirectoryLogging.closeOutputDirLogging(); 

		System.out.println("#### Finished ####");
	}

	private static Config createConfig(String[] args) {
		Config config = ConfigUtils.createConfig() ;

		if ((args == null) || (args.length == 0)) {
			config.controller().setOutputDirectory(OUTPUT_DIR);
		} else {
			System.out.println( "args[0]:" + args[0] );
			config.controller().setOutputDirectory( args[0]+"/" );
		}

		// (the directory structure is needed for jsprit output, which is before the
		// controller starts. Maybe there is a better alternative ...)
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		new OutputDirectoryHierarchy(config.controller().getOutputDirectory(), config.controller().getRunId(), config.controller().getOverwriteFileSetting(), CompressionType.gzip);
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		
		config.controller().setLastIteration(LAST_MATSIM_ITERATION);
		config.network().setInputFile(NETFILE);

		//Damit nicht alle um Mitternacht losfahren
		config.plans().setActivityDurationInterpretation(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration ); 

		//Some config stuff to comply to vsp-defaults even there is currently only 1 MATSim iteration and 
		//therefore no need for e.g. a strategy! KMT jan/18
		config.scoring().setFractionOfIterationsToStartScoreMSA(0.8);
		config.plans().setRemovingUnneccessaryPlanAttributes(true);
		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);
		//		config.qsim().setUsePersonIdForMissingVehicleId(false);		//TODO: Doesn't work here yet: "java.lang.IllegalStateException: NetworkRoute without a specified vehicle id." KMT jan/18
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
		ControllerUtils.checkConfigConsistencyAndWriteToLog(config, "dump");

		return config;
	}  //End createConfig

	private static Carriers jspritRun(Config config, Network network) {
		CarrierVehicleTypes vehicleTypes = createVehicleTypes();
		modifyVehicleTypes(vehicleTypes);

		Carriers carriers = createCarriers(vehicleTypes);
		generateCarrierPlans(network, carriers, vehicleTypes); // Hier erfolgt Lösung des VRPs

		checkServiceAssignment(carriers);

		//### Output nach Jsprit Iteration
		
		new CarrierPlanWriter(carriers).write( config.controller().getOutputDirectory() + "/jsprit_plannedCarriers.xml") ; //Muss in Temp, da OutputDir leer sein muss // setOverwriteFiles gibt es nicht mehr; kt 05.11.2014

		new WriteCarrierScoreInfos(carriers, new File(config.controller().getOutputDirectory() +  "/#JspritCarrierScoreInformation.txt"));

		return carriers;
	}

	private static void modifyVehicleTypes(CarrierVehicleTypes vehicleTypes) {
		for (VehicleType vt : vehicleTypes.getVehicleTypes().values()) {
			if (costsModifier != null) {
				switch (costsModifier) {
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
	 * Create and solve the VRP with jsprit
	 * 
	 * @param network
	 * @param carriers
	 * @param vehicleTypes
	 */
	private static void generateCarrierPlans(Network network, Carriers carriers, CarrierVehicleTypes vehicleTypes) {
		Builder netBuilder = NetworkBasedTransportCosts.Builder.newInstance( network, vehicleTypes.getVehicleTypes().values() );

		netBuilder.setTimeSliceWidth(1800) ; // !!!!, otherwise it will not do anything.

		final NetworkBasedTransportCosts netBasedCosts = netBuilder.build() ;
		log.debug(netBasedCosts.toString());

		for ( Carrier carrier : carriers.getCarriers().values() ) {
			VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder( carrier, network ) ;
			vrpBuilder.setRoutingCost(netBasedCosts) ;
			VehicleRoutingProblem vrp = vrpBuilder.build() ;


			VehicleRoutingAlgorithm vra = Jsprit.Builder.newInstance(vrp).setProperty(Jsprit.Parameter.THREADS, "5").buildAlgorithm();
			vra.getAlgorithmListeners().addListener(new StopWatch(), Priority.HIGH);
			//	        vra.getAlgorithmListeners().addListener(new AlgorithmSearchProgressChartListener(TEMP_DIR + RUN + runIndex + "jsprit_progress.png"));
			vra.setMaxIterations(MAX_JSPRIT_ITERATION);
			VehicleRoutingProblemSolution solution = Solutions.bestOf(vra.searchSolutions());

			//TODO: Auch option für Einlesen des Algorithmus, wenn vorhanden?
			//			VehicleRoutingAlgorithm algorithm = VehicleRoutingAlgorithms.readAndCreateAlgorithm(vrp, IOUtils.getUrlFromFileOrResource(ALGORITHMFILE));
			//			algorithm.setMaxIterations(MAX_JSPRIT_ITERATION);
			//			VehicleRoutingProblemSolution solution = Solutions.bestOf(algorithm.searchSolutions());
			CarrierPlan newPlan = MatsimJspritFactory.createPlan(solution) ;

			NetworkRouter.routePlan(newPlan,netBasedCosts) ;

			carrier.addPlan(newPlan) ;
			carrier.setSelectedPlan(newPlan) ;

			//Plot der Jsprit-Lösung
			//			Plotter plotter = new Plotter(vrp,solution);
			//			plotter.plot(config.controller().getOutputDirectory() + "/jsprit_solution_" + carrier.getId().toString() +".png", carrier.getId().toString());

			//Ausgabe der Ergebnisse auf der Console
			//SolutionPrinter.print(vrp,solution,Print.VERBOSE);

		}
	}

	/**
	 * Prüft für die Carriers, ob alle Services auch in den geplanten Touren vorkommen, d.h., ob sie auch tatsächlich geplant wurden.
	 * Falls nicht: log.warn und Ausgabe einer Datei: "#UnassignedServices.txt" mit den Service-Ids.
	 * @param carriers
	 */
	//TODO: Auch für Shipments auslegen und umbenennen. KMT feb'19
	//TODO: Funktionalität in contrib vorsehen -> CarrierControllerUtils? KMT feb'19
	private static void checkServiceAssignment(Carriers carriers) {
		for (Carrier c :carriers.getCarriers().values()){
			ArrayList<CarrierService> assignedServices = new ArrayList<>();
			ArrayList<CarrierService> multiassignedServices = new ArrayList<>();
			ArrayList<CarrierService> unassignedServices = new ArrayList<>();

            log.info("### Check service assignements of Carrier: {}", c.getId());
			//Erfasse alle einer Tour zugehörigen (→ stattfindenden) Services
			for (ScheduledTour tour : c.getSelectedPlan().getScheduledTours()){
				for (TourElement te : tour.getTour().getTourElements()){
					if (te instanceof  ServiceActivity){
						CarrierService assignedService = ((ServiceActivity) te).getService();
						if (!assignedServices.contains(assignedService)){
							assignedServices.add(assignedService);
                            log.debug("Assigned Service: {}", assignedServices);
						} else {
							multiassignedServices.add(assignedService);
                            log.warn("Service {} has already been assigned to Carrier {} -> multiple Assignment!", assignedService.getId().toString(), c.getId().toString());
						}
					}
				}
			}

			//Check, if all Services of the Carrier were assigned
			for (CarrierService service : c.getServices().values()){
				if (!assignedServices.contains(service)){
					unassignedServices.add(service);
                    log.warn("Service {} will NOT be served by Carrier {}", service.getId().toString(), c.getId().toString());
				} else {
                    log.debug("Service was assigned: {}", service.toString());
				}
			}

			//Schreibe die mehrfach eingeplanten Services in Datei
			if (!multiassignedServices.isEmpty()){
				try {
					FileWriter writer = new FileWriter(config.controller().getOutputDirectory() + "#MultiAssignedServices.txt", true);
					writer.write("#### Multi-assigned Services of Carrier: " + c.getId().toString() + System.lineSeparator());
					for (CarrierService s : multiassignedServices){
						writer.write(s.getId().toString() + System.lineSeparator());
					}
					writer.flush();
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				} 
			} else {
                log.info("No service(s)of {} were assigned to a tour more then one times.", c.getId().toString());
			}


			//Schreibe die nicht eingeplanten Services in Datei
			if (!unassignedServices.isEmpty()){
				try {
					FileWriter writer = new FileWriter(config.controller().getOutputDirectory() + "#UnassignedServices.txt", true);
					writer.write("#### Unassigned Services of Carrier: " + c.getId().toString() + System.lineSeparator());
					for (CarrierService s : unassignedServices){
						writer.write(s.getId().toString() + System.lineSeparator());
					}
					writer.flush();
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				} 
			} else {
                log.info("All service(s) of {} were assigned to at least one tour", c.getId().toString());
			}

		}//for(carriers)

	}


	/**
	 * Run the MATSim simulation
	 * 
	 * @param scenario
	 */
	private static void matsimRun(Scenario scenario) {
		final Controler controller = new Controler( scenario ) ;

		CarrierScoringFunctionFactory scoringFunctionFactory = createMyScoringFunction2(scenario);
		CarrierStrategyManager planStrategyManagerFactory =  createMyStrategymanager(); //Benötigt, da listener kein "Null" als StrategyFactory mehr erlaubt, KT 17.04.2015

		FreightCarriersConfigGroup freightConfig = ConfigUtils.addOrGetModule( scenario.getConfig(), FreightCarriersConfigGroup.class );
		freightConfig.setTimeWindowHandling(FreightCarriersConfigGroup.TimeWindowHandling.enforceBeginnings);

		CarriersUtils.addOrGetCarriers(scenario);
		CarrierModule listener = new CarrierModule();
		controller.addOverridingModule( new AbstractModule(){
			@Override
			public void install(){
				bind( CarrierScoringFunctionFactory.class ).toInstance(scoringFunctionFactory) ;
				bind( CarrierStrategyManager.class ).toInstance(planStrategyManagerFactory);
			}
		} ) ;
		controller.addOverridingModule(listener);

		//The VSP default settings are designed for person transport simulation. After talking to Kai, they will be set to WARN here. Kai MT may'23
		controller.getConfig().vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
		controller.run();
	}


	//Benötigt, da listener kein "Null" als StrategyFactory mehr erlaubt, KT 17.04.2015
	//Da keine Strategy notwendig, hier zunächst eine "leere" Factory
	private static CarrierStrategyManager createMyStrategymanager() {
		return CarrierControllerUtils.createDefaultCarrierStrategyManager();
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

	private static void writeAdditionalRunOutput(Config config, Carriers carriers) {
		// ### some final output: ###
		if (runMatsim){		//makes only sense, when MATSim run was performed KT 06.04.15
			new WriteCarrierScoreInfos(carriers, new File(OUTPUT_DIR + "#MatsimCarrierScoreInformation.txt"));
		}
		new CarrierPlanWriter(carriers).write(config.controller().getOutputDirectory() + "/output_carriers.xml");
		new CarrierPlanWriter(carriers).write(config.controller().getOutputDirectory() + "/output_carriers.xml.gz");
		new CarrierVehicleTypeWriter(CarrierVehicleTypes.getVehicleTypes(carriers)).write(config.controller().getOutputDirectory() + "/output_vehicleTypes.xml");
		new CarrierVehicleTypeWriter(CarrierVehicleTypes.getVehicleTypes(carriers)).write(config.controller().getOutputDirectory() + "/output_vehicleTypes.xml.gz");
	}

	/**
	 * Write out the information about the data provided for the simulation run.
	 */
	private static void writeRunInfo() {
		File file = new File(OUTPUT_DIR + "#RunInformation.txt");
		try {
			FileWriter writer = new FileWriter(file);  //Neuer File (überschreibt im Zweifel den alten - der jedoch nicht existieren dürfte!)
			writer.write("System date and time writing this file: " + LocalDateTime.now() + System.lineSeparator() + System.lineSeparator());

			writer.write("##Inputfiles:" + System.lineSeparator());
			writer.write("Input-Directory: " + INPUT_DIR);
			writer.write("Net: \t \t" + NETFILE_NAME + System.lineSeparator());
			writer.write("Carrier:  \t" + CARRIERFILE_NAME + System.lineSeparator());
			writer.write("VehType: \t" + VEHTYPEFILE_NAME + System.lineSeparator());
			//			writer.write("Algorithm: \t" + ALGORITHMFILE_NAME +System.getProperty("line.separator"));

			writer.write(System.lineSeparator());
			writer.write("##Run Settings:" + System.lineSeparator());
			writer.write("runMatsim: \t \t" + runMatsim + System.lineSeparator());
			writer.write("Last Matsim Iteration: \t" + LAST_MATSIM_ITERATION + System.lineSeparator());
			writer.write("Max jsprit Iteration: \t" + MAX_JSPRIT_ITERATION + System.lineSeparator());

			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Datei: " + file + " geschrieben.");
	}

}

