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

package org.matsim.vsp.freight.food.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.Gbl;
import org.matsim.freight.carriers.*;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AddAdditionalBEVTrucksToCarriers {
	private static final Logger log = LogManager.getLogger(AddAdditionalBEVTrucksToCarriers.class);

	private static final String INPUT_Carrier_File = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/input_2024/I-Base_carrierLEH_v2_withFleet_Shipment_OneTW_PickupTime.xml";
	private static final String OUTPUT_Carrier_File = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/input_2024/I-Base_carrierLEH_v2_withFleet_Shipment_OneTW_PickupTime_ICEVandBEV.xml" ;
	private static final String vehicleTypes_File = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/input_2024/vehicleTypes_Food_2024.xml";

	private static final List<String> newVehicleTypes_8t = Arrays.asList("Mitsubishi", "Quantron");
	private static final List<String> newVehicleTypes_18t = Arrays.asList("Renault", "Volvo");
	private static final List<String> newVehicleTypes_26t = Arrays.asList("Renault", "Daimler");
	private static final List<String> newVehicleTypes_40t = Arrays.asList("Daimler", "Scania");

	public static void main(String[] args) {
		Carriers carriers = new Carriers();
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(vehicleTypes).readFile(vehicleTypes_File);
		new CarrierPlanXmlReader(carriers, vehicleTypes).readFile(INPUT_Carrier_File); //re: perhaps add vehicleTypes here

		for (Carrier carrier : carriers.getCarriers().values()){
			Map<Id<Vehicle>, CarrierVehicle> carrierVehicles = carrier.getCarrierCapabilities().getCarrierVehicles();

			int numberOfVehiclesBefore = carrierVehicles.size();
			ArrayList<CarrierVehicle> vehiclesToAdd = new ArrayList<>();

			for (CarrierVehicle carrierVehicle : carrierVehicles.values()) {
				String vehicleIdString = carrierVehicle.getId().toString();
				List<String> newVehicleTypes;
				if (vehicleIdString.contains("light8t_"))
					newVehicleTypes = newVehicleTypes_8t;
				else if (vehicleIdString.contains("medium18t_"))
					newVehicleTypes = newVehicleTypes_18t;
				else if (vehicleIdString.contains("heavy26t_"))
					newVehicleTypes = newVehicleTypes_26t;
				else if (vehicleIdString.contains("heavy40t_"))
					newVehicleTypes = newVehicleTypes_40t;
				else
					throw new RuntimeException("Vehicle type not found");

				createNewVehicle(carrierVehicle, vehiclesToAdd, vehicleIdString, newVehicleTypes);
			}

			for (var vehicleToAdd : vehiclesToAdd) {
				CarriersUtils.addCarrierVehicle(carrier, vehicleToAdd);
			}

			Gbl.assertIf(3 * numberOfVehiclesBefore == carrier.getCarrierCapabilities().getCarrierVehicles().size()); //Nachher müssen doppelt so viele Fahrzeuge drin sein
		}

		new CarrierPlanWriter(carriers).write(OUTPUT_Carrier_File);
		System.out.println("### Done ###");
	}

	private static void createNewVehicle(CarrierVehicle carrierVehicle, ArrayList<CarrierVehicle> vehiclesToAdd, String vehicleIdString,
										 List<String> newVehicleTypes) {
		for (String newVehicleType : newVehicleTypes) {
			String vehicleId2String;
			final String searchString = "frozen";
			if (vehicleIdString.contains(searchString)) {
				var index = vehicleIdString.indexOf(searchString) + searchString.length();
				vehicleId2String = vehicleIdString.substring(0, index) + "_electro_" + newVehicleType + vehicleIdString.substring(index);
			} else {
				var index = vehicleIdString.indexOf("_");
				vehicleId2String = vehicleIdString.substring(0, index) + "_electro_" + newVehicleType + vehicleIdString.substring(index);
			}
			Id<Vehicle> vehicleId2 = Id.createVehicleId(vehicleId2String);


			Id<VehicleType> vehicleTypeId2 = Id.create(carrierVehicle.getVehicleTypeId() + "_electro_" + newVehicleType, VehicleType.class);

			VehicleType vehicleType = carrierVehicle.getType();
			VehicleType vehicleType_new = VehicleUtils.createVehicleType(vehicleTypeId2);
			VehicleUtils.copyFromTo(vehicleType_new, vehicleType);

			CarrierVehicle additionalCarrierVehicle =
					CarrierVehicle.Builder.newInstance(vehicleId2, carrierVehicle.getLinkId(), vehicleType_new)
							.setEarliestStart(carrierVehicle.getEarliestStartTime())
							.setLatestEnd(carrierVehicle.getLatestEndTime())
							.build();

			vehiclesToAdd.add(additionalCarrierVehicle);
		}
	}
}
