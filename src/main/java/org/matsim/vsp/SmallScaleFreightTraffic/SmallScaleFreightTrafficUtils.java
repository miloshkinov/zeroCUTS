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

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.application.options.ShpOptions;
import org.matsim.application.options.ShpOptions.Index;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import com.google.common.base.Joiner;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;

/**
 * Utils for the SmallScaleFreightTraffic
 * 
 * @author Ricardo Ewert
 *
 */
public class SmallScaleFreightTrafficUtils {

	private static final Logger log = LogManager.getLogger(LanduseBuildingAnalysis.class);
	private static final Joiner JOIN = Joiner.on("\t");

	/**
	 * Creates and return the Index of the zones shape.
	 * 
	 * @param shapeFileZonePath
	 * @return indexZones
	 */
	static Index getIndexZones(Path shapeFileZonePath) {

		ShpOptions shpZones = new ShpOptions(shapeFileZonePath, "EPSG:4326", StandardCharsets.UTF_8);
		Index indexZones = shpZones.createIndex("EPSG:4326", "id");
		return indexZones;
	}

	/**
	 * Creates and return the Index of the landuse shape.
	 * 
	 * @param shapeFileLandusePath
	 * @return indexLanduse
	 */
	static Index getIndexLanduse(Path shapeFileLandusePath) {

		ShpOptions shpLanduse = new ShpOptions(shapeFileLandusePath, "EPSG:4326", StandardCharsets.UTF_8);
		Index indexLanduse = shpLanduse.createIndex("EPSG:4326", "fclass");
		return indexLanduse;
	}

	/**
	 * Writes a csv file with result of the distribution per zone of the input data.
	 * 
	 * @param resultingDataPerZone
	 * @param outputFileInOutputFolder
	 * @param zoneIdNameConnection 
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	static void writeResultOfDataDistribution(HashMap<String, Object2DoubleMap<String>> resultingDataPerZone,
			Path outputFileInOutputFolder, HashMap<String, String> zoneIdNameConnection) throws IOException, MalformedURLException {

		writeCSVWithCategoryHeader(resultingDataPerZone, outputFileInOutputFolder, zoneIdNameConnection);
		log.info("The data distribution is finished and written to: " + outputFileInOutputFolder);
	}

	/**
	 * Writer of data distribution data.
	 * 
	 * @param resultingDataPerZone
	 * @param outputFileInInputFolder
	 * @param zoneIdNameConnection 
	 * @throws MalformedURLException
	 */
	private static void writeCSVWithCategoryHeader(HashMap<String, Object2DoubleMap<String>> resultingDataPerZone,
			Path outputFileInInputFolder, HashMap<String, String> zoneIdNameConnection) throws MalformedURLException {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFileInInputFolder.toUri().toURL(),
				StandardCharsets.UTF_8, true);
		try {
			String[] header = new String[] { "areaID", "areaName", "Inhabitants", "Employee", "Employee Primary Sector",
					"Employee Construction", "Employee Secondary Sector Rest", "Employee Retail",
					"Employee Traffic/Parcels", "Employee Tertiary Sector Rest" };
			JOIN.appendTo(writer, header);
			writer.write("\n");
			for (String zone : resultingDataPerZone.keySet()) {
				List<String> row = new ArrayList<>();
				row.add(zone);
				row.add(zoneIdNameConnection.get(zone));
				for (String category : header) {
					if (!category.equals("areaID") && !category.equals("areaName"))
						row.add(String.valueOf((int) Math.round(resultingDataPerZone.get(zone).getDouble(category))));
				}
				JOIN.appendTo(writer, row);
				writer.write("\n");
			}

			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	static void createPlansBasedOnCarrierPlans(Controler controler, String usedTrafficType, double sample, Path output) {
		Scenario scenario = controler.getScenario();
		Carriers carriers = FreightUtils.getCarriers(scenario);
		Population population = controler.getScenario().getPopulation();
		
		PopulationFactory popFactory = population.getFactory();
		for (Carrier carrier : carriers.getCarriers().values()) {
			for (ScheduledTour tour : carrier.getSelectedPlan().getScheduledTours()) {
				Person person = popFactory.createPerson(Id.create(tour.getVehicle().getId(), Person.class));
				Plan plan = popFactory.createPlan();
				Activity startActivity = popFactory.createActivityFromCoord("freight", scenario.getNetwork().getLinks().get(tour.getTour().getStartLinkId()).getFromNode().getCoord());
				startActivity.setStartTime(tour.getDeparture());
				startActivity.setEndTime(tour.getDeparture());
				startActivity.setMaximumDuration(0);
				plan.addActivity(startActivity);
				List<TourElement> tourElements = tour.getTour().getTourElements();
				for (TourElement tourElement : tourElements) {
					
					if (tourElement instanceof Tour.ServiceActivity) {
						Tour.ServiceActivity service = (Tour.ServiceActivity) tourElement;
						Activity serviceActivity = popFactory.createActivityFromCoord("freight", scenario.getNetwork().getLinks().get(service.getLocation()).getFromNode().getCoord());
						serviceActivity.setMaximumDuration(service.getService().getServiceDuration());
						plan.addActivity(serviceActivity);
					}
					if (tourElement instanceof Tour.Leg) {
						Leg legActivity = popFactory.createLeg("freight");
						plan.addLeg(legActivity);
					}
				}
				Activity endActivity = popFactory.createActivityFromCoord("freight",scenario.getNetwork().getLinks().get(tour.getTour().getEndLinkId()).getFromNode().getCoord());
				endActivity.setMaximumDuration(0);
				endActivity.setStartTime(tour.getDeparture()+8*3600);
				plan.addActivity(endActivity);
				person.addPlan(plan);
				population.addPerson(person);
				PopulationUtils.putSubpopulation(person, carrier.getId().toString().split("_")[1]);
				PopulationUtils.putPersonAttribute(person, "type", usedTrafficType.replace("Traffic", ""));
				VehicleUtils.insertVehicleIdsIntoAttributes(person, (new HashMap<String, Id<Vehicle>>(){{put("freight", tour.getVehicle().getId());}}));
			}
		}
		PopulationUtils.writePopulation(population, output.toString() + "/berlin_"+usedTrafficType+"_"+(int)(sample*100)+"pct_plans.xml.gz");
		controler.getScenario().getPopulation().getPersons().clear();
	}
}
