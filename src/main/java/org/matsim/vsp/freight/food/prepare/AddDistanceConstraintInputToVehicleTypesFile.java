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

import org.matsim.freight.carriers.carrier.CarrierVehicleTypeReader;
import org.matsim.freight.carriers.carrier.CarrierVehicleTypeWriter;
import org.matsim.freight.carriers.carrier.CarrierVehicleTypes;
import org.matsim.vehicles.*;


public class AddDistanceConstraintInputToVehicleTypesFile {

	private static final String INPUT_VehicleTypes = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/vehicleTypesBVWP100";
	private static final String OUTPUT_VehicleTypes = "../shared-svn/projects/freight/studies/WP51_EmissionsFood/vehicleTypesBVWP100_DC" ;

	private static final String CASE = "_Tax300.xml";

	private static final String INPUT_VehicleTypes_File = INPUT_VehicleTypes + CASE;
	private static final String OUTPUT_VehicleTypes_File = OUTPUT_VehicleTypes + CASE ;

	public static void main(String[] args) {
		CarrierVehicleTypes vehTypes = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader( vehTypes ).readFile(INPUT_VehicleTypes_File);

		for (VehicleType vehicleType : vehTypes.getVehicleTypes().values()) {
			if ( VehicleUtils.getFuelConsumption(vehicleType) == 0.0) {
				vehicleType.getEngineInformation().getAttributes().removeAttribute("fuelConsumptionLitersPerMeter");
			}

			EngineInformation engineInformation = vehicleType.getEngineInformation();
			if (vehicleType.getId().toString().equals("heavy40t_electro")) {
				VehicleUtils.setEnergyCapacity(engineInformation, 310.1);
				VehicleUtils.setEnergyConsumptionKWhPerMeter(engineInformation, 180.);
			}

			if (vehicleType.getId().toString().equals("heavy26t_electro") || vehicleType.getId().equals("heavy26t_frozen_electro")) {
				VehicleUtils.setEnergyCapacity(engineInformation, 200.2);
				VehicleUtils.setEnergyConsumptionKWhPerMeter(engineInformation, 150.);
			}

			if (vehicleType.getId().toString().equals("medium18t_electro")) {
				VehicleUtils.setEnergyCapacity(engineInformation, 85.4);
				VehicleUtils.setEnergyConsumptionKWhPerMeter(engineInformation, 106.);
			}

			if (vehicleType.getId().toString().equals("light8t_electro") || vehicleType.getId().equals("light8t_frozen_electro")) {
				VehicleUtils.setEnergyCapacity(engineInformation, 60.9);
				VehicleUtils.setEnergyConsumptionKWhPerMeter(engineInformation, 61.);
			}

		}

		new CarrierVehicleTypeWriter(vehTypes).write(OUTPUT_VehicleTypes_File);

		System.out.println("### Done ###");
	}
}
