package org.matsim.vsp.freight.food.prepare;

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


import org.matsim.api.core.v01.Scenario;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;
import org.matsim.freight.carriers.carrier.CarrierPlanWriter;
import org.matsim.freight.carriers.carrier.CarrierUtils;
import org.matsim.freight.carriers.carrier.Carriers;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

import java.util.concurrent.ExecutionException;


/**
 * @see org.matsim.contrib.freight
 */
public class ConvertToShipments {

	public static void main(String[] args) throws ExecutionException, InterruptedException{

		// ### config stuff: ###

		Config config = ConfigUtils.loadConfig( IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL( "freight-chessboard-9x9" ), "config.xml" ) );

		config.plans().setInputFile( null ); // remove passenger input

		//more general settings
		config.controller().setOutputDirectory("./output/freight" );

		config.controller().setLastIteration(0 );		// yyyyyy iterations currently do not work; needs to be fixed.  (Internal discussion at end of file.)

		//freight settings
		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule( config, FreightCarriersConfigGroup.class ) ;
		freightCarriersConfigGroup.setCarriersFile( "");
		freightCarriersConfigGroup.setCarriersVehicleTypesFile( "vehicleTypes.xml");

		// load scenario (this is not loading the freight material):
		Scenario scenario = ScenarioUtils.loadScenario( config );

		//load carriers according to freight config
		CarrierUtils.loadCarriersAccordingToFreightConfig( scenario );


		// how to set the capacity of the "light" vehicle type to "1":
//		CarrierControlerUtils.getCarrierVehicleTypes( scenario ).getVehicleTypes().get( Id.create("light", VehicleType.class ) ).getCapacity().setOther( 1 );

		// output before jsprit run (not necessary)
		new CarrierPlanWriter(CarrierUtils.getCarriers( scenario )).write( "output/jsprit_unplannedCarriers.xml" ) ;
		// (this will go into the standard "output" directory.  note that this may be removed if this is also used as the configured output dir.)

		// Solving the VRP (generate carrier's tour plans)
		CarrierUtils.runJsprit( scenario );

		// Output after jsprit run (not necessary)
		new CarrierPlanWriter(CarrierUtils.getCarriers( scenario )).write( "output/jsprit_plannedCarriers.xml" ) ;
		// (this will go into the standard "output" directory.  note that this may be removed if this is also used as the configured output dir.)

		Carriers newCarriers = CarrierUtils.createShipmentVRPCarrierFromServiceVRPSolution(CarrierUtils.getCarriers( scenario ));
		new CarrierPlanWriter(newCarriers).write( "output/carriersWithShipments.xml" ) ;
	}


}