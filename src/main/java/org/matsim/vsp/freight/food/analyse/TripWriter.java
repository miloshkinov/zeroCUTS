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

/**
 * 
 */
package org.matsim.vsp.freight.food.analyse;

import org.apache.log4j.Level;
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
import java.util.TreeMap;

import static org.slf4j.event.Level.DEBUG;

/**
 * @author ikaddoura , lkroeger
 *
 */
public class TripWriter {
	private static final Logger log = Logger.getLogger(TripWriter.class);

	TripEventHandler handler;
	String outputFolder;
	
	public TripWriter(TripEventHandler handler, String outputFolder) {
//		log.setLevel(Level.DEBUG);
		this.handler = handler;
		String directory = outputFolder + (outputFolder.endsWith("/") ? "" : "/");
		this.outputFolder = directory;
		
		String fileName = outputFolder;
		File file = new File(fileName);
		file.mkdirs();
	}
	
/**
 * Schreibt die Informationen (TripDistance, diastance Tour) des Carriers für jeden Trip einzeln auf.
 * TODO: TravelTime, 
 * TODO: gesamte Reisezeit (Ende "start"-act bis Beginn "end"-act)
 * @param carrierIdString
 */
	public void writeDetailedResultsSingleCarrier(String carrierIdString) {
		
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
				
				
				for (int i = 0 ; i < distancesInMeters.size() ; i++) {
					double distanceInKm = distancesInMeters.get(i)/1000;

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
	public void writeTourResultsSingleCarrier(String carrierIdString) {
		
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
					+ "TravelTime tour [h]"
			);
			bw.newLine();

			
		
//			KT:
			Map<Id<Person>, Double> personId2tourDistance = this.handler.getPersonId2TourDistances(carrierIdString);
			Map<Id<Person>, Double> personId2tourTravelTimes = this.handler.getPersonId2TravelTimes(carrierIdString);
			
			//Summe für gesammten Carrier
			Double totalTourDistanceInMeters = 0.0;
			Double totalTourTravelTimeInSeconds =0.0;
			for (Id<Person> id :personId2tourDistance.keySet()) {
				totalTourDistanceInMeters = totalTourDistanceInMeters + personId2tourDistance.get(id);
				totalTourTravelTimeInSeconds = totalTourTravelTimeInSeconds + personId2tourTravelTimes.get(id);
			}
			
			bw.write("SUMME Carrier;"
					+ totalTourDistanceInMeters/1000 + ";"
					+ totalTourTravelTimeInSeconds/3600
			);
			bw.newLine();
			
			// Werte der einzelnen Agenten
			for (Id<Person> id :personId2tourDistance.keySet()) {

				Double tourDistanceInMeters = personId2tourDistance.get(id);
				Double tourTravelTimeInSeconds = personId2tourTravelTimes.get(id);
				
				bw.write(id + ";"
						+ tourDistanceInMeters/1000 + ";"
						+ tourTravelTimeInSeconds/3600
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
	 * TODO: gesamte Reisezeit (Ende "start"-act bis Beginn "end"-act)
	 */
		public void writeResultsPerVehicleTypes() {
		
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
					"TravelTime [h]; " //+
					);
			bw.newLine();
	
		
//			KT:
			Map<Id<VehicleType>,Double> vehTypeId2TourDistances = new TreeMap<Id<VehicleType>,Double>();
			Map<Id<VehicleType>,Double> vehTypeId2TravelTimes = new TreeMap<Id<VehicleType>,Double>();
			Map<Id<VehicleType>,Integer> vehTypeId2NumberOfVehicles = new TreeMap<Id<VehicleType>,Integer>();
//			Map<Id<VehicleType>, VehicleTypeSpezificCapabilities> vehTypId2Capabilities = new TreeMap<Id<VehicleType>, VehicleTypeSpezificCapabilities>();
			
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
					Integer nuOfVeh = this.handler.getVehTypId2VehicleNumber(vehicleTypeId).get(vehicleTypeId);
//					VehicleTypeSpezificCapabilities capabilities = this.handler.getVehTypId2Capabilities().get(vehicleTypeId);
					if (distance != null) {
						vehTypeId2TourDistances.put(vehicleTypeId, distance );
					} else {
						vehTypeId2TourDistances.put(vehicleTypeId, 0.);
					}
					if (travelTime != null){
						vehTypeId2TravelTimes.put(vehicleTypeId, travelTime);
					}else {
						vehTypeId2TravelTimes.put(vehicleTypeId, 0.);
					}
					if (nuOfVeh != null){
						vehTypeId2NumberOfVehicles.put(vehicleTypeId, nuOfVeh);
					} else {
						vehTypeId2NumberOfVehicles.put(vehicleTypeId, 0);
					}
//					if (capabilities != null){
//						vehTypId2Capabilities.put(vehicleTypeId, capabilities);
//					}
				}
			}
			
			//Gesamtsumme
			Double totalDistanceInMeter = 0.0;
			Double totalTravelTimeInSeconds = 0.0;
			Integer totalNumberofVehicles = 0;
			for (Id<VehicleType> vehTypeId : vehTypeId2TourDistances.keySet()) {
				totalDistanceInMeter = totalDistanceInMeter + vehTypeId2TourDistances.get(vehTypeId);
				totalTravelTimeInSeconds = totalTravelTimeInSeconds + vehTypeId2TravelTimes.get(vehTypeId);
				totalNumberofVehicles = totalNumberofVehicles + vehTypeId2NumberOfVehicles.get(vehTypeId);
			}
			
			// Gesamtsumme
			bw.write("SUMME alle Carrier;"+ 
					totalNumberofVehicles + ";" +
					totalDistanceInMeter/1000 + ";" +
					totalTravelTimeInSeconds/3600 + ";" //+
					);
			bw.newLine();
			
			// Werte der einzelnen Fahrzeugtypen (alle Carrier)
			for (Id<VehicleType> vehTypeId : vehTypeId2TourDistances.keySet()) {

				Double tourDistanceInMeters = vehTypeId2TourDistances.get(vehTypeId);
				Double tourTravelTimeInSeconds = vehTypeId2TravelTimes.get(vehTypeId);
				Integer numberOfVehicles = vehTypeId2NumberOfVehicles.get(vehTypeId);
//				VehicleTypeSpezificCapabilities capabilites = vehTypId2Capabilities.get(vehTypeId);
				
				bw.write(vehTypeId + ";" +
						numberOfVehicles + ";" +
						tourDistanceInMeters/1000 + ";" +
						tourTravelTimeInSeconds /3600+ ";" //+
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
	public void writeTourResultsAllCarrier() {
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

//			Map<Id<VehicleType>, VehicleTypeSpezificCapabilities> vehTypId2Capabilities = new TreeMap<Id<VehicleType>, VehicleTypeSpezificCapabilities>();

			CarrierVehicleTypes vehicleTypes = this.handler.getVehicleTypes();

//			//preparation:
//			for (Id<VehicleType> vehicleTypeId : vehicleTypes.getVehicleTypes().keySet()){
//				VehicleTypeSpezificCapabilities capabilities = this.handler.getVehTypId2Capabilities().get(vehicleTypeId);
//				if (capabilities != null){
//					vehTypId2Capabilities.put(vehicleTypeId, capabilities);
//				}
//
//			}

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
