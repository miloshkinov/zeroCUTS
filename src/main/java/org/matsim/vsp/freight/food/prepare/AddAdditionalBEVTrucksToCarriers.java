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

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.core.gbl.Gbl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.Map;

public class AddAdditionalBEVTrucksToCarriers {
	private static final Logger log = Logger.getLogger(AddAdditionalBEVTrucksToCarriers.class);

	private static final String INPUT_Carrier_File = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/I-Base_carrierLEH_v2_withFleet_Shipment_OneTW_PickupTime.xml";
	private static final String OUTPUT_Carrier_File = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/I-Base_carrierLEH_v2_withFleet_Shipment_OneTW_PickupTime_ICEVandBEV.xml" ;

	public static void main(String[] args) {
		Carriers carriers = new Carriers();
		new CarrierPlanXmlReader(carriers).readFile(INPUT_Carrier_File);

		for (Carrier carrier : carriers.getCarriers().values()){
			Map<Id<Vehicle>, CarrierVehicle> carrierVehicles = carrier.getCarrierCapabilities().getCarrierVehicles();

			int numberOfVehiclesBefore = carrierVehicles.size();
			ArrayList<CarrierVehicle> vehiclesToAdd = new ArrayList<>();

			for (CarrierVehicle carrierVehicle : carrierVehicles.values()) {
				String vehicleIdString = carrierVehicle.getId().toString();
				var index = vehicleIdString.indexOf("_");
				String vehicleId2String = vehicleIdString.substring(0,index-1) + "_electro" + vehicleIdString.substring(index);
				Id<Vehicle> vehicleId2 = Id.createVehicleId(vehicleId2String);

				Id<VehicleType> vehicleTypeId2 = Id.create(carrierVehicle.getVehicleTypeId() + "_electro", VehicleType.class);

				CarrierVehicle additionalCarrierVehicle =
						CarrierVehicle.Builder.newInstance(vehicleId2, carrierVehicle.getLocation())
								.setEarliestStart(carrierVehicle.getEarliestStartTime())
								.setLatestEnd(carrierVehicle.getLatestEndTime())
								.setTypeId(vehicleTypeId2)
								.build();

				vehiclesToAdd.add(additionalCarrierVehicle);
			}

			for (var vehicleToAdd : vehiclesToAdd) {
				CarrierUtils.addCarrierVehicle(carrier, vehicleToAdd);
			}

			Gbl.assertIf(2 * numberOfVehiclesBefore == carrier.getCarrierCapabilities().getCarrierVehicles().size()); //Nachher müssen doppelt so viele Fahrzeuge drin sein
		}

		new CarrierPlanXmlWriterV2(carriers).write(OUTPUT_Carrier_File);
		System.out.println("### Done ###");
	}


}
