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

package org.matsim.vsp.sav;/*
 * created by jbischoff, 13.12.2018
 * This script transform all inner city Berlin car trips to SAV.
 */

import static org.matsim.core.config.ConfigUtils.createConfig;
import static org.matsim.core.scenario.ScenarioUtils.createScenario;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

public class GenerateSAVDemand {

    private static final String inPopulation = "http://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.2-1pct/input/berlin-v5.2-1pct.plans.xml.gz";
    private static final String shapeFile = "scenarios/avscenario/shp/berlin.shp";
    private static final String taxiPopulation = "scenarios/avscenario/berlin-v5.2-1pct.taxiplans.xml.gz";

    public static void main(String[] args) {
        Config config = createConfig();
        Scenario scenario = createScenario(config);
        new PopulationReader(scenario).readFile(taxiPopulation);
        scenario.getPopulation().getPersons().values().forEach(p -> removeTaxiStages(p.getSelectedPlan()));
        new PopulationWriter(scenario.getPopulation()).write(taxiPopulation);

    }


//    public static void main(String[] args) throws MalformedURLException {
//        Config config = createConfig();
//        Scenario scenario = createScenario(config);
//        Geometry berlin = readShapeFileAndExtractGeometry(shapeFile, "SCHLUESSEL").get("010113");
//        int i = 0;
//        new PopulationReader(scenario).readURL(new URL(inPopulation));
//        for (Person p : scenario.getPopulation().getPersons().values()) {
//            PersonUtils.removeUnselectedPlans(p);
//            Plan plan = p.getSelectedPlan();
//            Activity lastActivity = null;
//            Leg lastLeg = null;
//            i++;
//            if (i % 1000 == 0) {
//                System.out.println(i);
//            }
//            for (PlanElement planElement : plan.getPlanElements()) {
//                if (planElement instanceof Activity) {
//                    if (lastActivity != null) {
//                        if (berlin.contains(MGC.coord2Point(lastActivity.getCoord())) && berlin.contains(MGC.coord2Point(((Activity) planElement).getCoord()))) {
//                            if (lastLeg.getMode().equals(TransportMode.car)) {
//                                lastLeg.setMode(TransportMode.taxi);
//                                lastLeg.setRoute(null);
//                                lastActivity.setType("taxi interaction");
//                                ((Activity) planElement).setType("taxi interaction");
//                            }
//                        }
//                    }
//                    lastActivity = (Activity) planElement;
//                } else if (planElement instanceof Leg) {
//                    lastLeg = (Leg) planElement;
//                }
//            }
//            removeTaxiStages(plan);
//
//        }
//
//        new PopulationWriter(scenario.getPopulation()).write(taxiPopulation);
//
//
//    }


    public static Map<String, Geometry> readShapeFileAndExtractGeometry(String filename, String key) {

        Map<String, Geometry> geometry = new TreeMap<>();
        for (SimpleFeature ft : ShapeFileReader.getAllFeatures(filename)) {

            GeometryFactory geometryFactory = new GeometryFactory();
            WKTReader wktReader = new WKTReader(geometryFactory);

            try {
                Geometry geo = wktReader.read((ft.getAttribute("the_geom")).toString());
                String lor = ft.getAttribute(key).toString();
                geometry.put(lor, geo);
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }


        }
        return geometry;
    }

    static void removeTaxiStages(final Plan plan) {
        MainModeIdentifier mainModeIdentifier = new MainModeIdentifierImpl();
        StageActivityTypes stageActivityTypes = new StageActivityTypesImpl("car interaction", "taxi interaction", "pt interaction", "ride interaction", "freight interaction");

        final List<PlanElement> planElements = plan.getPlanElements();
        final List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan, stageActivityTypes);

        for (TripStructureUtils.Trip trip : trips) {
            final List<PlanElement> fullTrip =
                    planElements.subList(
                            planElements.indexOf(trip.getOriginActivity()) + 1,
                            planElements.indexOf(trip.getDestinationActivity()));
            final String mode = mainModeIdentifier.identifyMainMode(fullTrip);
            if (mode.equals(TransportMode.taxi)) {
                fullTrip.clear();
                fullTrip.add(PopulationUtils.createLeg(mode));
            }

        }
    }
}
