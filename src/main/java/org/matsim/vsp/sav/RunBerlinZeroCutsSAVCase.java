/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.vsp.sav;

import static org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType.FastAStarLandmarks;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.run.MultiModeTaxiConfigGroup;
import org.matsim.contrib.taxi.run.MultiModeTaxiModule;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

/**
 * This class runs an example robotaxi scenario including fares. The simulation runs for 10 iterations, this takes
 * quite a bit time (25 minutes or so). You may switch on OTFVis visualisation in the main method below. The scenario
 * should run out of the box without any additional files. If required, you may find all input files in the resource
 * path or in the jar maven has downloaded). There are two vehicle files: 2000 vehicles and 5000, which may be set in
 * the config. Different fleet sizes can be created using
 */
public class RunBerlinZeroCutsSAVCase {

    public static void main(String[] args) {
        String configFile = "scenarios/avscenario/berlin-v5.2-1pct-sav.config.xml";
        RunBerlinZeroCutsSAVCase.run(configFile, false);
    }

    public static void run(String configFile, boolean otfvis) {
		Config config = ConfigUtils.loadConfig(configFile, new DvrpConfigGroup(), new OTFVisConfigGroup(),
				new MultiModeTaxiConfigGroup());
        enrichConfig(config);
        createControler(config, otfvis).run();
    }

    public static Controler createControler(Config config, boolean otfvis) {
        config.checkConsistency();
        String mode = TaxiConfigGroup.getSingleModeTaxiConfig(config).getMode();

        Scenario scenario = ScenarioUtils.loadScenario(config);

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new DvrpModule());
		controler.addOverridingModule(new MultiModeTaxiModule());
        controler.configureQSimComponents(DvrpQSimComponents.activateModes(mode));

        if (otfvis) {
            controler.addOverridingModule(new OTFVisLiveModule());
        }
        controler.addOverridingModule(new SwissRailRaptorModule());
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addTravelTimeBinding(TransportMode.ride).to(networkTravelTime());
                addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());
            }
        });


        return controler;
    }

    public static void enrichConfig(Config config) {

        config.controler().setRoutingAlgorithmType(FastAStarLandmarks);
        config.subtourModeChoice().setProbaForRandomSingleTripMode(0.5);
        config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
        // vsp defaults
//        config.plansCalcRoute().setInsertingAccessEgressWalk(true);


        config.qsim().setUsingTravelTimeCheckInTeleportation(true);
        config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.kinematicWaves);
        final ActivityParams paramst = new ActivityParams("taxi interaction");
        paramst.setScoringThisActivityAtAll(false);
        paramst.setTypicalDuration(1);
        config.planCalcScore().addActivityParams(paramst);

        // activities:
        for (long ii = 600; ii <= 97200; ii += 600) {
            final ActivityParams params = new ActivityParams("home_" + ii + ".0");
            params.setTypicalDuration(ii);
            config.planCalcScore().addActivityParams(params);
        }
        for (long ii = 600; ii <= 97200; ii += 600) {
            final ActivityParams params = new ActivityParams("work_" + ii + ".0");
            params.setTypicalDuration(ii);
            params.setOpeningTime(6. * 3600.);
            params.setClosingTime(20. * 3600.);
            config.planCalcScore().addActivityParams(params);
        }
        for (long ii = 600; ii <= 97200; ii += 600) {
            final ActivityParams params = new ActivityParams("leisure_" + ii + ".0");
            params.setTypicalDuration(ii);
            params.setOpeningTime(9. * 3600.);
            params.setClosingTime(27. * 3600.);
            config.planCalcScore().addActivityParams(params);
        }
        for (long ii = 600; ii <= 97200; ii += 600) {
            final ActivityParams params = new ActivityParams("shopping_" + ii + ".0");
            params.setTypicalDuration(ii);
            params.setOpeningTime(8. * 3600.);
            params.setClosingTime(20. * 3600.);
            config.planCalcScore().addActivityParams(params);
        }
        for (long ii = 600; ii <= 97200; ii += 600) {
            final ActivityParams params = new ActivityParams("other_" + ii + ".0");
            params.setTypicalDuration(ii);
            config.planCalcScore().addActivityParams(params);
        }
        {
            final ActivityParams params = new ActivityParams("freight");
            params.setTypicalDuration(12. * 3600.);
            config.planCalcScore().addActivityParams(params);
        }
    }
}
