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
import java.util.Map;

public class AddAdditionalBEVTrucksToCarriers {
	private static final Logger log = LogManager.getLogger(AddAdditionalBEVTrucksToCarriers.class);

	private static final String INPUT_Carrier_File = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/I-Base_carrierLEH_v2_withFleet_Shipment_OneTW_PickupTime.xml";
	private static final String OUTPUT_Carrier_File = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/I-Base_carrierLEH_v2_withFleet_Shipment_OneTW_PickupTime_ICEVandBEV.xml" ;

	public static void main(String[] args) {
		Carriers carriers = new Carriers();
		new CarrierPlanXmlReader(carriers, null).readFile(INPUT_Carrier_File); //re: perhaps add vehicleTypes here

		for (Carrier carrier : carriers.getCarriers().values()){
			Map<Id<Vehicle>, CarrierVehicle> carrierVehicles = carrier.getCarrierCapabilities().getCarrierVehicles();

			int numberOfVehiclesBefore = carrierVehicles.size();
			ArrayList<CarrierVehicle> vehiclesToAdd = new ArrayList<>();

			for (CarrierVehicle carrierVehicle : carrierVehicles.values()) {
				String vehicleIdString = carrierVehicle.getId().toString();
				String vehicleId2String ;

				final String searchString = "frozen";
				if (vehicleIdString.contains(searchString)){
					var index = vehicleIdString.indexOf(searchString) + searchString.length();
					vehicleId2String = vehicleIdString.substring(0, index) + "_electro" + vehicleIdString.substring(index);
				} else {
					var index = vehicleIdString.indexOf("_");
					vehicleId2String = vehicleIdString.substring(0, index) + "_electro" + vehicleIdString.substring(index);
				}

				Id<Vehicle> vehicleId2 = Id.createVehicleId(vehicleId2String);

				Id<VehicleType> vehicleTypeId2 = Id.create(carrierVehicle.getVehicleTypeId() + "_electro", VehicleType.class);
				
				VehicleType vehicleType = carrierVehicle.getType();
				VehicleType vehicleType_new = VehicleUtils.createVehicleType(vehicleTypeId2);
				VehicleUtils.copyFromTo(vehicleType_new, vehicleType);

				CarrierVehicle additionalCarrierVehicle =
						CarrierVehicle.Builder.newInstance(vehicleId2, carrierVehicle.getLocation(), vehicleType_new)
								.setEarliestStart(carrierVehicle.getEarliestStartTime())
								.setLatestEnd(carrierVehicle.getLatestEndTime())
								.build();

				vehiclesToAdd.add(additionalCarrierVehicle);
			}

			for (var vehicleToAdd : vehiclesToAdd) {
				CarriersUtils.addCarrierVehicle(carrier, vehicleToAdd);
			}

			Gbl.assertIf(2 * numberOfVehiclesBefore == carrier.getCarrierCapabilities().getCarrierVehicles().size()); //Nachher müssen doppelt so viele Fahrzeuge drin sein
		}

		new CarrierPlanXmlWriterV2(carriers).write(OUTPUT_Carrier_File);
		System.out.println("### Done ###");
	}


}
