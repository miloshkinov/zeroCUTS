/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.vsp.emissions;

import java.util.HashMap;
import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.analysis.time.TimeBinMap;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.analysis.EmissionsByPollutant;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

/**
 * Collects Warm- and Cold-Emission-Events and returns them either
 * by time bin and link-id, or only by link-id.
 */
public class EmissionsPerVehicleEventHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler {

  private static final Id<Vehicle> FREIGHT_REWE_VERBRAUCHERMARKT_TROCKEN_VEH_MEDIUM_18_T_ELECTRO_160444_1 = Id.createVehicleId(
      "freight_rewe_VERBRAUCHERMARKT_TROCKEN_veh_medium18t_electro_160444_1");
  //TODO: Use EmissionByPollutant?
    private final Map<Id<Vehicle>, Map<Pollutant, Double>> vehicle2pollutants = new HashMap<>();
//  private final Map<Id<VehicleType>, Map<Pollutant, Double>> vehicleType2pollutants = new HashMap<>();

	/**
	 * Drop events after end time.
	 */

    public EmissionsPerVehicleEventHandler() {}


    /**
     * Yields summed link emissions
     *
     * @return Total emissions per pollutant by vehicle id
     */
    public Map<Id<Vehicle>, Map<Pollutant, Double>> getVehicle2pollutants() {
      System.out.println("#### Vehicle2Pollutant ABRUF: " +vehicle2pollutants.get(
          FREIGHT_REWE_VERBRAUCHERMARKT_TROCKEN_VEH_MEDIUM_18_T_ELECTRO_160444_1).toString() );
      return vehicle2pollutants; }

  /**
   * Yields summed link emissions
   *
   * @return Total emissions per pollutant by vehicle id
   */
//  public Map<Id<VehicleType>, Map<Pollutant, Double>> getVehicleType2pollutants() { return vehicleType2pollutants; }

    @Override
    public void reset(int iteration) {
        vehicle2pollutants.clear();
//        vehicleType2pollutants.clear();
    }

    @Override
    public void handleEvent(WarmEmissionEvent event) {
      handleEmissionEvent(event.getTime(), event.getVehicleId(), event.getWarmEmissions());
    }

    @Override
    public void handleEvent(ColdEmissionEvent event) {
        handleEmissionEvent(event.getTime(), event.getVehicleId(), event.getColdEmissions());
    }

    private void handleEmissionEvent(double time, Id<Vehicle> vehicleId, Map<Pollutant, Double> emissions) {

        //Sum up ver VehicleId
        if (vehicle2pollutants.get(vehicleId) == null) { vehicle2pollutants.put(vehicleId, emissions); }
        else {
            for (Pollutant key : emissions.keySet()) {
                vehicle2pollutants.get(vehicleId).merge(key, emissions.get(key), Double::sum);
            }
        }
        if (vehicleId.equals(FREIGHT_REWE_VERBRAUCHERMARKT_TROCKEN_VEH_MEDIUM_18_T_ELECTRO_160444_1)){
          System.out.println("### vehId: " + vehicleId + "; emissions: "+ emissions.toString());
          System.out.println("### vehicle2Pollutants " + vehicle2pollutants.get(vehicleId).toString());
        }
        if ( vehicle2pollutants.get(FREIGHT_REWE_VERBRAUCHERMARKT_TROCKEN_VEH_MEDIUM_18_T_ELECTRO_160444_1).get(Pollutant.CO) != 1.){
          System.out.println("JETZT wurde was modifiziert: " + time + "vehid: " + vehicleId + "; emissions: "+ emissions.toString());
        }
    }
}
