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

import com.graphhopper.jsprit.analysis.toolbox.StopWatch;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.algorithm.listener.VehicleRoutingAlgorithmListeners;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.Freight;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierUtils;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

class RunFood {

    static final Logger log = Logger.getLogger(RunFood.class);

    private static int nuOfJspritIteration;

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        for (String arg : args) {
            log.info( arg );
        }

        if ( args.length==0 ) {
            String inputPath = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/input/";
            args = new String[] {
                inputPath+"TwoCarrier_Shipment_OneTW_PickupTime_ICEVandBEV.xml",
                inputPath + "vehicleTypesBVWP100_DC_Tax300.xml",
                inputPath + "mdvrp_algorithmConfig_2.xml",
                "1",                                                    //only for demonstration.
                inputPath + "networkChangeEvents.xml.gz",
                "../shared-svn/projects/freight/studies/WP51_EmissionsFood/output/Demo1It",
                "true"
            };
        }

        Config config = prepareConfig( args ) ;
        Scenario scenario = prepareScenario( config ) ;
        Controler controler = prepareControler( scenario ) ;

        runJsprit(controler);

        controler.run();
    }

    private static Config prepareConfig(String[] args) {
        String carriersFileLocation = args[0];
        String vehicleTypesFileLocation = args[1];
        String algorithmFileLocation = args[2]; //TODO: Read in Algorithm -> Put into freightConfigGroup?
        nuOfJspritIteration = Integer.parseInt(args[3]);
        String networkChangeEventsFileLocation = args[4];
        String outputLocation = args[5];

        Boolean useDistanceConstraint = false;
        try {
            useDistanceConstraint = Boolean.parseBoolean(args[6]);
        } catch (Exception e) {
            log.warn("Was not able to parse the boolean for using the distance constraint. Using + " + useDistanceConstraint + " as default.");
//            e.printStackTrace();
        }


        Config config = ConfigUtils.createConfig();
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.global().setRandomSeed(4177);
        config.controler().setLastIteration(0);
        config.controler().setOutputDirectory(outputLocation);

        config.network().setInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-10pct/input/berlin-v5-network.xml.gz");

        if (networkChangeEventsFileLocation != ""){
        log.info("Setting networkChangeEventsInput file: " + networkChangeEventsFileLocation);
            config.network().setTimeVariantNetwork(true);
            config.network().setChangeEventsInputFile(networkChangeEventsFileLocation);
        }

//        config.addConfigConsistencyChecker(new VspConfigConsistencyCheckerImpl());
//        config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);

        config.plans().setActivityDurationInterpretation(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration );
        //freight configstuff
        FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
        freightConfigGroup.setCarriersFile(carriersFileLocation);
        freightConfigGroup.setCarriersVehicleTypesFile(vehicleTypesFileLocation);
        freightConfigGroup.setTravelTimeSliceWidth(1800);
        freightConfigGroup.setTimeWindowHandling(FreightConfigGroup.TimeWindowHandling.enforceBeginnings);

        if(useDistanceConstraint) {
            freightConfigGroup.setUseDistanceConstraintForTourPlanning(FreightConfigGroup.UseDistanceConstraintForTourPlanning.basedOnEnergyConsumption);
        }

        return config;
    }

    private static Scenario prepareScenario(Config config) {
        Scenario scenario = ScenarioUtils.loadScenario(config);

        FreightUtils.loadCarriersAccordingToFreightConfig(scenario);

        return scenario;
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

    private static void runJsprit(Controler controler) throws ExecutionException, InterruptedException {
            NetworkBasedTransportCosts.Builder netBuilder = NetworkBasedTransportCosts.Builder.newInstance(
                    controler.getScenario().getNetwork(), FreightUtils.getCarrierVehicleTypes(controler.getScenario()).getVehicleTypes().values() );
            final NetworkBasedTransportCosts netBasedCosts = netBuilder.build() ;

        Carriers carriers = FreightUtils.getCarriers(controler.getScenario());

        HashMap<Id<Carrier>, Integer> carrierServiceCounterMap = new HashMap<>();

        // Fill carrierServiceCounterMap
        for (Carrier carrier : carriers.getCarriers().values()) {
            carrierServiceCounterMap.put(carrier.getId(), carrier.getServices().size());
        }

        HashMap<Id<Carrier>, Integer> sortedMap = carrierServiceCounterMap.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));

        ArrayList<Id<Carrier>> tempList = new ArrayList<>(sortedMap.keySet());
        ForkJoinPool forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        forkJoinPool.submit(() -> tempList.parallelStream().forEach(carrierId -> {
            Carrier carrier = carriers.getCarriers().get(carrierId);

            double start = System.currentTimeMillis();
            int serviceCount = carrier.getServices().size();
            log.info("start tour planning for " + carrier.getId() + " which has " + serviceCount + " services");

//       for (Carrier carrier : carriers.getCarriers().values()){
        //Carrier carrier = carriers.getCarriers().get(Id.create("kaiser_VERBRAUCHERMARKT_FRISCHE", Carrier.class)); //only for tests

            //currently with try/catch, because CarrierUtils.getJspritIterations will throw an exception if value is not present. Will fix it on MATSim.
            //TODO maybe a future CarrierUtils functionality: Overwrite/set all nuOfJspritIterations. maybe depending on enum (overwriteAll, setNotExisiting, none) ?, KMT Nov2019
            try {
                if(CarrierUtils.getJspritIterations(carrier) <= 0){
                    log.warn("Received negative number of jsprit iterations. This is invalid -> Setting number of jsprit iterations for carrier: " + carrier.getId() + " to " + nuOfJspritIteration);
                    CarrierUtils.setJspritIterations(carrier, nuOfJspritIteration);
                } else {
                    log.warn("Overwriting the number of jsprit iterations for carrier: " + carrier.getId() + ". Value was before " +CarrierUtils.getJspritIterations(carrier) + "and is now " + nuOfJspritIteration);
                    CarrierUtils.setJspritIterations(carrier, nuOfJspritIteration);
                }
            } catch (Exception e) {
                log.warn("Setting (missing) number of jsprit iterations for carrier: " + carrier.getId() + " to " + nuOfJspritIteration);
                CarrierUtils.setJspritIterations(carrier, nuOfJspritIteration);
            }

            VehicleRoutingProblem vrp = MatsimJspritFactory.createRoutingProblemBuilder(carrier, controler.getScenario().getNetwork())
                   .setRoutingCost(netBasedCosts)
                   .build();

            log.warn("Ignore the algorithms file for jsprit and use an algorithm out of the box.");
            Scenario scenario = controler.getScenario();
            FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(controler.getConfig(), FreightConfigGroup.class);
            VehicleRoutingAlgorithm vra = MatsimJspritFactory.loadOrCreateVehicleRoutingAlgorithm(scenario, freightConfigGroup, netBasedCosts, vrp);
            vra.getAlgorithmListeners().addListener(new StopWatch(), VehicleRoutingAlgorithmListeners.Priority.HIGH);
            vra.setMaxIterations(CarrierUtils.getJspritIterations(carrier));
            VehicleRoutingProblemSolution solution = Solutions.bestOf(vra.searchSolutions());

            log.info("tour planning for carrier " + carrier.getId() + " took "
                    + (System.currentTimeMillis() - start) / 1000 + " seconds.");

            CarrierPlan newPlan = MatsimJspritFactory.createPlan(carrier, solution) ;

            log.info("routing plan for carrier " + carrier.getId());
            NetworkRouter.routePlan(newPlan,netBasedCosts) ;
            log.info("routing for carrier " + carrier.getId() + " finished. Tour planning plus routing took "
                    + (System.currentTimeMillis() - start) / 1000 + " seconds.");

            carrier.setSelectedPlan(newPlan) ;
        })).get();
    }
}
