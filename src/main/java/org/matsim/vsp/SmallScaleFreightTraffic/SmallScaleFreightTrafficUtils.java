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
import java.nio.file.Files;
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
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.application.options.ShpOptions;
import org.matsim.application.options.ShpOptions.Index;
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

	private static final Logger log = LogManager.getLogger(SmallScaleFreightTrafficUtils.class);
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
			Path outputFileInOutputFolder, HashMap<String, String> zoneIdNameConnection)
			throws IOException, MalformedURLException {

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

	/** Creates a population including the plans in preparation for the MATSim run.
	 * @param controler
	 * @param usedTrafficType
	 * @param sample
	 * @param output
	 * @param inputDataDirectory 
	 */
	static void createPlansBasedOnCarrierPlans(Controler controler, String usedTrafficType, double sample,
			Path output, Path inputDataDirectory) {
		Scenario scenario = controler.getScenario();
		Population population;
		if (usedTrafficType.equals("businessTraffic"))
			population = controler.getScenario().getPopulation();
		else {
			if (Files.exists(inputDataDirectory.resolve("berlin_longDistanceFreight_"+ (int) (sample * 100) +"pct.xml.gz"))) {
				log.error("Required landuse shape file {} not found", inputDataDirectory.resolve("berlin_longDistanceFreight_"+ (int) (sample * 100) +"pct.xml.gz"));
		
			population = PopulationUtils.readPopulation(inputDataDirectory.resolve("berlin_longDistanceFreight_"+ (int) (sample * 100) +"pct.xml.gz").toString());
			log.info("Number of imported tours of longDistance freight traffic: " + population.getPersons().size());
			}
			else
				population = PopulationUtils.createPopulation(controler.getConfig());
			}
		PopulationFactory popFactory = population.getFactory();

		Population populationFromCarrier = (Population) scenario.getScenarioElement("allpersons");
		for (Person person : populationFromCarrier.getPersons().values()) {

			Plan plan = popFactory.createPlan();
			String mode = null;
			String subpopulation = null;
			if (person.getId().toString().split("_")[2].equals("Business")) {
				mode = "car";
				subpopulation = "businessTraffic";
			} else if (person.getId().toString().split("_")[2].equals("Freight")) {
				mode = "freight";
				subpopulation = "freight";
			}
			final String usedMainMode = mode;
			List<PlanElement> tourElements = person.getSelectedPlan().getPlanElements();
			double tourStartTime = 0;
			for (PlanElement tourElement : tourElements) {

				if (tourElement instanceof Activity) {
					Activity service = (Activity) tourElement;
					service.setCoord(
							scenario.getNetwork().getLinks().get(service.getLinkId()).getFromNode().getCoord());
					if (!service.getType().equals("start"))
						service.setEndTimeUndefined();
					else
						tourStartTime = service.getEndTime().seconds();
					if (service.getType().equals("end"))
						service.setStartTime(tourStartTime + 8 * 3600);
					plan.addActivity(service);
				}
				if (tourElement instanceof Leg) {
					Leg legActivity = popFactory.createLeg(usedMainMode);
					plan.addLeg(legActivity);
				}
			}
			Person newPerson = popFactory.createPerson(person.getId());
			newPerson.addPlan(plan);
			population.addPerson(newPerson);
			PopulationUtils.putSubpopulation(newPerson, subpopulation);
			PopulationUtils.putPersonAttribute(newPerson, "tourStartArea", person.getId().toString().split("_")[3]);
			VehicleUtils.insertVehicleIdsIntoAttributes(newPerson, (new HashMap<String, Id<Vehicle>>() {
				{
					put(usedMainMode, (Id.createVehicleId(person.getId().toString())));
				}
			}));

		}
		PopulationUtils.writePopulation(population,
				output.toString() + "/berlin_" + usedTrafficType + "_" + (int) (sample * 100) + "pct_plans.xml.gz");
		controler.getScenario().getPopulation().getPersons().clear();
	}
}
