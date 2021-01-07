/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.vsp.demandGeneration;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.vehicles.VehicleType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * @author kturner based on ikaddoura , lkroeger
 *
 */
/*package-private*/ class TripWriter {
	private static final Logger log = Logger.getLogger(TripWriter.class);

	TripEventHandler handler;
	String outputFolder;

	public TripWriter(TripEventHandler handler, String outputFolder) {
//		log.setLevel(Level.DEBUG);
		this.handler = handler;
		this.outputFolder = outputFolder + (outputFolder.endsWith("/") ? "" : "/");

		File file = new File(outputFolder);
		file.mkdirs();
	}

	/**
	 * Schreibt die Informationen (TripDistance, distance Tour) des Carriers für jeden Trip einzeln auf.
	 * TODO: TravelTime,
	 * TODO: gesamte Reisezeit (Ende "start"-act bis Beginn "end"-act)
	 * @param carrierIdString
	 */
	/*package-private*/ void writeDetailedResultsSingleCarrier(String carrierIdString) {

		String fileName = this.outputFolder + "trip_infos_" + carrierIdString + ".csv";
		File file = new File(fileName);

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(fileName);
			bw.newLine();
			bw.write("____________________________________________________________________________");
			bw.newLine();

			bw.write("person Id;"
					+ "distance trip [km];"

			);
			bw.newLine();

			Map<Id<Person>,List<Double>> personId2listOfDistances = this.handler.getPersonId2listOfDistances(carrierIdString);

			Map<Id<Person>, Double> personId2tourDistance = this.handler.getPersonId2TourDistances(carrierIdString);

			for (Id<Person> id :personId2listOfDistances.keySet()) {
				List<Double> distancesInMeters = personId2listOfDistances.get(id);


				for (Double distancesInMeter : distancesInMeters) {
					double distanceInKm = distancesInMeter / 1000;

					bw.write(id + ";"
							+ distanceInKm + ";"
					);
					bw.newLine();
				}
			}

			log.info("Output written to " + fileName);
			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Schreibt die Informationen (tour distance, tour travelTime) des Carriers für jede Tour (= jedes Fzg) einzeln auf.
	 * TODO: gesamte Reisezeit (Ende "start"-act bis Beginn "end"-act)
	 * @param carrierIdString
	 */
	/*package-private*/ void writeTourResultsSingleCarrier(String carrierIdString) {

		String fileName = this.outputFolder + "tour_infos_" + carrierIdString + ".csv";
		File file = new File(fileName);

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(fileName);
			bw.newLine();
			bw.write("____________________________________________________________________________");
			bw.newLine();

			bw.write("person Id;"
					+ "distance tour [km] ; "
					+ "TravelTime tour [h] ;"
					+ "ActivityTime tour [h] ;"
					+ "Amount of Services ;"
					+ "Amount of Shipments ;"
					+ "Handled Demand"
			);
			bw.newLine();

//			KT:
			Map<Id<Person>, Double> personId2tourDistance = this.handler.getPersonId2TourDistances(carrierIdString);
			Map<Id<Person>, Double> personId2tourTravelTimes = this.handler.getPersonId2TravelTimes(carrierIdString);
			Map<Id<Person>, Double> personId2tourActivityDurations = this.handler.getPersonId2SumOfActivityDurations(carrierIdString);
			Map<Id<Person>, Integer> personId2tourAmountServices = this.handler.getPersonId2TourServices(carrierIdString);
			Map<Id<Person>, Integer> personId2tourAmountShipments = this.handler.getPersonId2TourShipments(carrierIdString);
//			Map<Id<Person>, Integer> personId2tourHandeledDemand = this.handler.getPersonId2TourHandledDemand(carrierIdString);

			//Summe für gesammten Carrier
			double totalTourDistanceInMeters = 0.0;
			double totalTourTravelTimeInSeconds =0.0;
			double totalTourActivityDurationInSeconds =0.0;
			int totalAmountOfServices =0;
			int totalAmountOfShipments =0;
			int totalAmountOfHandeledDemand =0;

			for (Id<Person> id :personId2tourDistance.keySet()) {
				totalTourDistanceInMeters = totalTourDistanceInMeters + personId2tourDistance.get(id);
				totalTourTravelTimeInSeconds = totalTourTravelTimeInSeconds + personId2tourTravelTimes.get(id);
				totalTourActivityDurationInSeconds = totalTourActivityDurationInSeconds + personId2tourActivityDurations.get(id);
				if (!personId2tourAmountServices.isEmpty())
					totalAmountOfServices = totalAmountOfServices + personId2tourAmountServices.get(id);
				if (!personId2tourAmountShipments.isEmpty())
					totalAmountOfShipments = totalAmountOfShipments + personId2tourAmountShipments.get(id);
//				totalAmountOfHandeledDemand = totalAmountOfHandeledDemand + personId2tourHandeledDemand.get(id);
			}

			bw.write("SUMME Carrier;"
					+ totalTourDistanceInMeters/1000 + ";"
					+ totalTourTravelTimeInSeconds/3600 + ";"
					+ totalTourActivityDurationInSeconds/3600 +";"
					+ totalAmountOfServices +";"
					+ totalAmountOfShipments +";"
					+ "TODO"//totalAmountOfHandeledDemand
			);
			bw.newLine();

			// Werte der einzelnen Agenten
			for (Id<Person> id :personId2tourDistance.keySet()) {

				Double tourDistanceInMeters = personId2tourDistance.get(id);
				Double tourTravelTimeInSeconds = personId2tourTravelTimes.get(id);
				Double tourActivityDurationInSeconds = personId2tourActivityDurations.get(id);
				Integer tourAmountOfServices = personId2tourAmountServices.get(id);
				Integer tourAmountOfShipments = personId2tourAmountShipments.get(id);
//				Integer tourAmountOfHandledDemand = personId2tourHandeledDemand.get(id);

				bw.write(id + ";"
						+ tourDistanceInMeters/1000 + ";"
						+ tourTravelTimeInSeconds/3600 + ";"
						+ tourActivityDurationInSeconds/3600 +";"
						+ totalAmountOfServices +";"
						+ totalAmountOfShipments +";"
						+ "TODO"
				);
				bw.newLine();

			}

			log.info("Output written to " + fileName);
			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Schreibt die Informationen (#Fahrzeuge, distance, travelTime (Fahrzeit), FuelConsumption, CO2-Emissionen) 
	 * des Carriers für jeden FahrzeugTyp einzeln auf und bildet auch Gesamtsumme.
	 */
	/*package-private*/ void writeResultsPerVehicleTypes() {

		String fileName = this.outputFolder + "total_infos_per_vehicleType.csv";
		File file = new File(fileName);

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(fileName);
			bw.newLine();
			bw.write("____________________________________________________________________________");
			bw.newLine();


			bw.write("vehType Id;" +
					"#ofVehicles;" +
					"distance [km];" +
					"TravelTime [h]; " +
					"ActivityDuration [h];" +
					"DurationFromStartToEndofTour [h]"
			);
			bw.newLine();


//			KT:
			Map<Id<VehicleType>,Double> vehTypeId2TourDistances = new TreeMap<>();
			Map<Id<VehicleType>,Double> vehTypeId2TravelTimes = new TreeMap<>();
			Map<Id<VehicleType>,Double> vehTypeId2ActivityDurations = new TreeMap<>();
			Map<Id<VehicleType>,Double> vehTypeId2DurationsStartToEnd = new TreeMap<>();
			Map<Id<VehicleType>,Integer> vehTypeId2NumberOfVehicles = new TreeMap<>();

			//Vorbereitung: Nur Aufnehmen, wenn nicht null;
			CarrierVehicleTypes vehicleTypes = this.handler.getVehicleTypes();
			for (Id<VehicleType> vehicleTypeId : vehicleTypes.getVehicleTypes().keySet()){
				log.warn("handle vehicleType:" + vehicleTypeId);
				if (vehTypeId2TourDistances.containsKey(vehicleTypeId)) {
					log.fatal("vehicleType wurde bereits behandelt:" + vehicleTypeId.toString(), new RuntimeException());
				} else { //TODO: umschreiben, dass nur die Werte bestimmt werden... oder man die Map einmal bestimmt.
					log.debug(vehicleTypeId + " added mit Entfernung " +  this.handler.getVehTypId2TourDistances(vehicleTypeId).get(vehicleTypeId));
					Double distance = this.handler.getVehTypId2TourDistances(vehicleTypeId).get(vehicleTypeId);
					Double travelTime = this.handler.getVehTypId2TravelTimes(vehicleTypeId).get(vehicleTypeId);
					Double activityDuration = this.handler.getVehTypId2ActivityDurations(vehicleTypeId).get(vehicleTypeId);
					Double durationStartToEnde = this.handler.getVehTypId2DurationsStartToEnd(vehicleTypeId).get(vehicleTypeId);
					Integer nuOfVeh = this.handler.getVehTypId2VehicleNumber(vehicleTypeId).get(vehicleTypeId);

					vehTypeId2TourDistances.put(vehicleTypeId, Objects.requireNonNullElse(distance, 0.));
					vehTypeId2TravelTimes.put(vehicleTypeId, Objects.requireNonNullElse(travelTime, 0.));
					vehTypeId2ActivityDurations.put(vehicleTypeId, Objects.requireNonNullElse(activityDuration, 0.));
					vehTypeId2NumberOfVehicles.put(vehicleTypeId, Objects.requireNonNullElse(nuOfVeh, 0));
					vehTypeId2DurationsStartToEnd.put(vehicleTypeId, Objects.requireNonNullElse(durationStartToEnde, 0.));
				}
			}

			//Gesamtsumme
			double totalDistanceInMeter = 0.0;
			double totalTravelTimeInSeconds = 0.0;
			int totalNumberofVehicles = 0;
			double totalActivityDurationsInSeconds = 0.0;
			double totalDurationsStartToEndInSeconds = 0.0;
			for (Id<VehicleType> vehTypeId : vehTypeId2TourDistances.keySet()) {
				totalDistanceInMeter = totalDistanceInMeter + vehTypeId2TourDistances.get(vehTypeId);
				totalTravelTimeInSeconds = totalTravelTimeInSeconds + vehTypeId2TravelTimes.get(vehTypeId);
				totalNumberofVehicles = totalNumberofVehicles + vehTypeId2NumberOfVehicles.get(vehTypeId);
				totalActivityDurationsInSeconds = totalActivityDurationsInSeconds + vehTypeId2ActivityDurations.get(vehTypeId);
				totalDurationsStartToEndInSeconds = totalDurationsStartToEndInSeconds + vehTypeId2DurationsStartToEnd.get(vehTypeId);
			}

			// Gesamtsumme
			bw.write("SUMME alle Carrier;"+
					totalNumberofVehicles + ";" +
					totalDistanceInMeter/1000 + ";" +
					totalTravelTimeInSeconds/3600 + ";" +
					totalActivityDurationsInSeconds/3600 + ";" +
					totalDurationsStartToEndInSeconds/3600 + ";"
			);
			bw.newLine();

			// Werte der einzelnen Fahrzeugtypen (alle Carrier)
			for (Id<VehicleType> vehTypeId : vehTypeId2TourDistances.keySet()) {

				//				VehicleTypeSpezificCapabilities capabilites = vehTypId2Capabilities.get(vehTypeId);

				bw.write(vehTypeId + ";" +
						vehTypeId2NumberOfVehicles.get(vehTypeId) + ";" +
						vehTypeId2TourDistances.get(vehTypeId) /1000 + ";" +
						vehTypeId2TravelTimes.get(vehTypeId) /3600+ ";" +
						vehTypeId2ActivityDurations.get(vehTypeId) / 3600 +";" +
						vehTypeId2DurationsStartToEnd.get(vehTypeId) / 3600 +";"
				);
				bw.newLine();

			}

			bw.newLine();

			log.info("Output written to " + fileName);
			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Writes out the information of all tours in one file. One line per tour
	 */
	/*package-private*/ void writeTourResultsAllCarrier() {
		String fileName = this.outputFolder + "total_tour_infos_per_vehicleType.csv";
		File file = new File(fileName);

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(fileName);
			bw.newLine();
			bw.write("____________________________________________________________________________");
			bw.newLine();

			bw.write("personId; " +
					"vehType Id;" +
					"distance [km] ;" +
					"TravelTime [h];" //+
			);
			bw.newLine();

			//			KT:
			Map<Id<Person>, Double> personId2tourDistance = this.handler.getPersonId2TourDistances();
			Map<Id<Person>, Double> personId2tourTravelTimes = this.handler.getPersonId2TravelTimes();

			CarrierVehicleTypes vehicleTypes = this.handler.getVehicleTypes();

			//write results:
			for (Id<Person> personId : personId2tourDistance.keySet()) {
				log.debug("PersonId: " + personId);

				Double tourDistanceMeter = personId2tourDistance.get(personId);
				Double tourTravelTimeSec = personId2tourTravelTimes.get(personId);

				Id<VehicleType> vehTypeId = null;

				for (Id<VehicleType> vehTypeIdsAvail : vehicleTypes.getVehicleTypes().keySet()) {
					log.debug("Trying if VehicleTypeId is matching: " + vehTypeIdsAvail.toString());
					if(personId.toString().contains("_"+vehTypeIdsAvail.toString()+"_")){
						if (vehTypeIdsAvail.toString().contains("frozen") == personId.toString().contains("frozen")) { //keine doppelte Erfassung der "frozen" bei den nicht-"frozen"...
							if (vehTypeIdsAvail.toString().contains("electro") == personId.toString().contains("electro")) {//keine doppelte Erfassung der "electro" bei den nicht-"electro"...
								vehTypeId = vehTypeIdsAvail;
								log.debug("vehicletypeId was set to: " +vehTypeId);
							}
						}
					}
				}

				if (vehTypeId == null) {
					log.error("Vehicle type for person not defined: " + personId);
				}

				bw.write(personId + ";" +
						vehTypeId + ";" +
						tourDistanceMeter/1000 + ";" +  //km
						tourTravelTimeSec/3600 + ";" //+ 	//h
				);
				bw.newLine();

			}

			log.info("Output written to " + fileName);
			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}






}
