
/* *********************************************************************** *
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
 * *********************************************************************** */
package org.matsim.vsp.wasteCollection;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class RunAnalyse {
	static final Logger log = Logger.getLogger(RunAnalyse.class);
	
	private static final String workingDir = "C:/Users/Ricardo/tubCloud/Shared/";

	private static final String dir_moLargeBinsDiesel100it = workingDir+ "vsp_zerocuts/scenarios/Muellentsorgung/Montag_withTraffic/diesel_Mo_largeBin_100it/";
	private static final String dir_moSmallBinsDiesel100it = workingDir+ "vsp_zerocuts/scenarios/Muellentsorgung/Montag_withTraffic/diesel_Mo_smallBin_100it/";
	private static final String dir_moLargeBinsElektro100it = workingDir+ "vsp_zerocuts/scenarios/Muellentsorgung/Montag_withTraffic/mediumEV_Mo_largeBin_100it/";
	private static final String dir_moSmallBinsElektro100it = workingDir+ "vsp_zerocuts/scenarios/Muellentsorgung/Montag_withTraffic/mediumEV_Mo_smallBin_100it/";
	private static final String dir_weLargeBinsDiesel100it = workingDir+ "vsp_zerocuts/scenarios/Muellentsorgung/Mittwoch_withTraffic/diesel_Mi_largeBin_100it/";
	private static final String dir_weSmallBinsDiesel100it = workingDir+ "vsp_zerocuts/scenarios/Muellentsorgung/Mittwoch_withTraffic/diesel_Mi_smallBin_100it/";
	private static final String dir_weLargeBinsElektro100it = workingDir+ "vsp_zerocuts/scenarios/Muellentsorgung/Mittwoch_withTraffic/mediumEV_Mi_largeBin_100it/";
	private static final String dir_weSmallBinsElektro100it = workingDir+ "vsp_zerocuts/scenarios/Muellentsorgung/Mittwoch_withTraffic/mediumEV_Mi_smallBin_100it/";

	private enum scenarioAuswahl {
		moLargeBinsDiesel100it, moLargeBinsElektro100it, moSmallBinsDiesel100it, moSmallBinsElektro100it,
		weLargeBinsDiesel100it, weLargeBinsElektro100it, weSmallBinsDiesel100it, weSmallBinsElektro100it,
	}

	public static void main(String[] args) throws IOException {

		log.info("Starting");

		scenarioAuswahl scenarioWahl = scenarioAuswahl.weLargeBinsDiesel100it;
		String inputDir;
		Map<Id<Person>, Double> personId2tourDistanceKm = new HashMap<>();
		Map<Id<Person>, Integer> personId2tourNumCollections = new HashMap<>();
		Map<Id<Person>, Double> personId2tourWasteCollectedTons = new HashMap<>();
		Map<Id<Person>, Integer> personId2tourDurations = new HashMap<>();
		Map<Id<Person>, Integer> personId2tourStartTime = new HashMap<>();
		Map<Id<Person>, Integer> personId2tourEndTime = new HashMap<>();
		Map<Id<Person>, String> personId2tourDepot = new HashMap<>();
		Map<Id<Person>, Double> personId2tourConsumption = new HashMap<>();

		switch (scenarioWahl) {
		case moLargeBinsDiesel100it:
			inputDir = dir_moLargeBinsDiesel100it;
			break;
		case moSmallBinsDiesel100it:
			inputDir = dir_moSmallBinsDiesel100it;
			break;
		case moLargeBinsElektro100it:
			inputDir = dir_moLargeBinsElektro100it;
			break;
		case moSmallBinsElektro100it:
			inputDir = dir_moSmallBinsElektro100it;
			break;
		case weLargeBinsDiesel100it:
			inputDir = dir_weLargeBinsDiesel100it;
			break;
		case weSmallBinsDiesel100it:
			inputDir = dir_weSmallBinsDiesel100it;
			break;
		case weLargeBinsElektro100it:
			inputDir = dir_weLargeBinsElektro100it;
			break;
		case weSmallBinsElektro100it:
			inputDir = dir_weSmallBinsElektro100it;
			break;
		default:
			throw new IllegalStateException("Unexpected value: " + scenarioWahl);
		}

		log.info("Running analysis for " + scenarioWahl + " : " + inputDir);

		Carriers carriers = new Carriers();
		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(carrierVehicleTypes).readFile(inputDir + "output_vehicleTypes.xml");
		new CarrierPlanXmlReader(carriers, carrierVehicleTypes) //re: perhaps add vehicleTypes here
				.readFile(new File(inputDir + "output_CarrierPlans.xml").getCanonicalPath());

		Network network = NetworkUtils.readNetwork(inputDir + "output_network.xml.gz");

		for (Carrier carrier : carriers.getCarriers().values()) {
			double distanceTourKM;
			int numCollections;
			double wasteCollectedTons;
			int tourNumber = 0;

			Collection<ScheduledTour> tours = carrier.getSelectedPlan().getScheduledTours();
			Collection<CarrierShipment> shipments = carrier.getShipments().values();
			HashMap<String, Integer> shipmentSizes = new HashMap<String, Integer>();

			for (CarrierShipment carrierShipment : shipments) {
				String shipmentId = carrierShipment.getId().toString();
				int shipmentSize = carrierShipment.getSize();
				shipmentSizes.put(shipmentId, shipmentSize);
			}
			for (ScheduledTour scheduledTour : tours) {
				distanceTourKM = 0.0;
				numCollections = 0;
				wasteCollectedTons = 0.0;
				int startTime = 10000000;
				int endTime = 0;

				List<Tour.TourElement> elements = scheduledTour.getTour().getTourElements();
				for (Tour.TourElement element : elements) {
					if (element instanceof Tour.Pickup) {
						numCollections++;
						Tour.Pickup pickupElement = (Tour.Pickup) element;
						String pickupShipmentId = pickupElement.getShipment().getId().toString();
						wasteCollectedTons = wasteCollectedTons + (double)shipmentSizes.get(pickupShipmentId)/1000;
					}
					if (element instanceof Tour.Leg) {
						Tour.Leg legElement = (Tour.Leg) element;
						if (legElement.getRoute().getDistance() != 0)
							distanceTourKM = distanceTourKM
									+ RouteUtils.calcDistance((NetworkRoute) legElement.getRoute(), 0, 0, network)
											/ 1000;
						if (startTime > legElement.getExpectedDepartureTime())
							startTime = (int) legElement.getExpectedDepartureTime();
						if (endTime < (legElement.getExpectedDepartureTime() + legElement.getExpectedTransportTime()))
							endTime = (int) (legElement.getExpectedDepartureTime()
									+ legElement.getExpectedTransportTime());
					}
				}

				Id<Person> personId = Id.create(
						carrier.getId().toString() + scheduledTour.getVehicle().getId().toString() + "_" + tourNumber,
						Person.class);
				personId2tourDistanceKm.put(personId, distanceTourKM);
				personId2tourNumCollections.put(personId, numCollections);
				personId2tourWasteCollectedTons.put(personId, wasteCollectedTons);
				personId2tourDurations.put(personId, endTime - startTime);
				personId2tourStartTime.put(personId, startTime);
				personId2tourEndTime.put(personId, endTime);
				if (scheduledTour.getTour().getStartLinkId().toString().equals("42882"))
					personId2tourDepot.put(personId, "Nordring");
				if (scheduledTour.getTour().getStartLinkId().toString().equals("71781"))
					personId2tourDepot.put(personId, "Gradestraße");
				if (scheduledTour.getTour().getStartLinkId().toString().equals("116212"))
					personId2tourDepot.put(personId, "Malmoeerstrasse");
				if (scheduledTour.getTour().getStartLinkId().toString().equals("27766"))
					personId2tourDepot.put(personId, "Forckenbeckstrasse");
				double consumption = distanceTourKM*100/100 + 1.4*wasteCollectedTons;
				personId2tourConsumption.put(personId, consumption);
				tourNumber++;
			}
		}
		writeOutput(inputDir, personId2tourDistanceKm, personId2tourNumCollections, personId2tourWasteCollectedTons,
				personId2tourDurations, personId2tourStartTime, personId2tourEndTime, personId2tourDepot, personId2tourConsumption);
		log.info("### Done.");
	}

	static void writeOutput(String directory, Map<Id<Person>, Double> personId2tourDistanceKm,
			Map<Id<Person>, Integer> personId2tourNumCollections,
			Map<Id<Person>, Double> personId2tourWasteCollectedTons, Map<Id<Person>, Integer> personId2tourDurations, Map<Id<Person>, Integer> personId2tourStartTime, Map<Id<Person>, Integer> personId2tourEndTime, Map<Id<Person>, String> personId2tourDepot, Map<Id<Person>, Double> personId2tourConsumption) {
		String outputFolder = directory + "/03_TourStatistics_new.csv";
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder);
		try {
			// Headline
			writer.write(
					"TourID\tduration hh:mm:ss\tstartTime\tendTime\tdistance (km)\t#ofDeliveries\tdelivered Volume (m3)\tconsumption (kWH)\tdepot\n");

			for (Id<Person> id : personId2tourDistanceKm.keySet()) {
				Double tourDistance = personId2tourDistanceKm.get(id);
				Integer tourNumCollections = personId2tourNumCollections.get(id);
				Double tourWasteCollected = personId2tourWasteCollectedTons.get(id);
				int tourStartTime = personId2tourStartTime.get(id);
				int tourEndTime = personId2tourEndTime.get(id);
				int duration = personId2tourDurations.get(id);
				String depot = personId2tourDepot.get(id);
				Double consumption = personId2tourConsumption.get(id);

				writer.write(id + "\t" + timeTransmission(duration) + "\t" + timeTransmission(tourStartTime) + "\t" + timeTransmission(tourEndTime) + "\t"+ tourDistance + "\t"
						+ tourNumCollections + "\t" + tourWasteCollected + "\t" + consumption + "\t" + depot);
				writer.newLine();
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		log.info("Output geschrieben");
	}

	public static String timeTransmission(int duration) {
		int stunden = (int) duration / 3600;
		int minuten = (int) (duration - stunden * 3600) / 60;
		int sekunden = duration - stunden * 3600 - minuten * 60;
		return stunden + ":" + minuten + ":" + sekunden;
	}
}