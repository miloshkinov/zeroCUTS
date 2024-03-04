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

import java.util.LinkedHashMap;
import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.analysis.EmissionsByPollutant;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.vehicles.Vehicle;

/**
 * Collects Warm- and Cold-Emission-Events and returns them either
 * by time bin and link-id, or only by link-id.
 */
public class EmissionsPerVehicleEventHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler {

  private static final Id<Vehicle> FREIGHT_REWE_VERBRAUCHERMARKT_TROCKEN_VEH_MEDIUM_18_T_ELECTRO_160444_1 =
      Id.createVehicleId("freight_rewe_VERBRAUCHERMARKT_TROCKEN_veh_medium18t_electro_160444_1");

    private final Map<Id<Vehicle>, EmissionsByPollutant> vehicle2pollutants = new LinkedHashMap<>();
//  private final Map<Id<VehicleType>, Map<Pollutant, Double>> vehicleType2pollutants = new HashMap<>();

  private Double tempValue = 0.;

	/**
	 * Drop events after end time.
	 */

    public EmissionsPerVehicleEventHandler() {}


    /**
     * Yields summed link emissions
     *
     * @return Total emissions per pollutant by vehicle id
     */
    public Map<Id<Vehicle>, EmissionsByPollutant> getVehicle2pollutants() {
      System.out.println("#### Vehicle2Pollutant ABRUF: " +vehicle2pollutants.get(
          FREIGHT_REWE_VERBRAUCHERMARKT_TROCKEN_VEH_MEDIUM_18_T_ELECTRO_160444_1).getEmissions().toString() );
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
      handleEmissionEvent( event.getVehicleId(), event.getWarmEmissions(), event);
    }

    @Override
    public void handleEvent(ColdEmissionEvent event) {
        handleEmissionEvent(event.getVehicleId(), event.getColdEmissions(), event);
    }

    private void handleEmissionEvent(Id<Vehicle> vehicleId, Map<Pollutant, Double> emissions, Event event) {

      EmissionsByPollutant emissionsByPollutant = new EmissionsByPollutant(emissions);

      //Sum up ver VehicleId
        if (vehicle2pollutants.get(vehicleId) == null) {
          vehicle2pollutants.put(vehicleId, emissionsByPollutant); }
        else {
//            for (Pollutant key : emissions.keySet()) {
                vehicle2pollutants.get(vehicleId).addEmissions(emissions);
//            }
        }
//        if (vehicleId.toString().equals(FREIGHT_REWE_VERBRAUCHERMARKT_TROCKEN_VEH_MEDIUM_18_T_ELECTRO_160444_1)){
//          System.out.println("### vehId: " + vehicleId + "; emissions: "+ emissions.toString());
//          System.out.println("### vehicle2Pollutants " + vehicle2pollutants.get(vehicleId.toString()).toString());
//        }

        if ( vehicle2pollutants.get(FREIGHT_REWE_VERBRAUCHERMARKT_TROCKEN_VEH_MEDIUM_18_T_ELECTRO_160444_1).getEmission(Pollutant.CO) != tempValue.doubleValue()){
          System.out.println("JETZT wurde was modifiziert: "  + "vehid: " + vehicleId + "; emissions: "+ emissions.toString());
          System.out.println("EVENT:" + event.toString());
          tempValue = vehicle2pollutants.get(FREIGHT_REWE_VERBRAUCHERMARKT_TROCKEN_VEH_MEDIUM_18_T_ELECTRO_160444_1).getEmission(Pollutant.CO);
        }
    }
}
