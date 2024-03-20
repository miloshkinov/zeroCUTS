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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
  private static final Logger log = LogManager.getLogger( EmissionsPerVehicleEventHandler.class );

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
//      System.out.println("#### Vehicle2Pollutant ABRUF: " +vehicle2pollutants.get(
//          FREIGHT_REWE_VERBRAUCHERMARKT_TROCKEN_VEH_MEDIUM_18_T_ELECTRO_160444_1).getEmissions().toString() );
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
    var newMap = new HashMap<Pollutant, Double>();
      for (Entry<Pollutant, Double> pollutantDoubleEntry : emissions.entrySet()) {
        newMap.put(pollutantDoubleEntry.getKey(), (Double) pollutantDoubleEntry.getValue().doubleValue());
      }

      EmissionsByPollutant emissionsByPollutant = new EmissionsByPollutant(newMap);

//      log.warn( "vehicleId=" + vehicleId );
//      log.warn( "emissions=" + emissions) ;
//      log.warn( "emissionsByPollutant=" + emissionsByPollutant ) ;

      //Sum up ver VehicleId
        if (vehicle2pollutants.get(vehicleId) == null) {
          vehicle2pollutants.put(vehicleId, emissionsByPollutant); }
        else {
                var current = vehicle2pollutants.get(vehicleId);
                var currentCo  = current.getEmission(Pollutant.CO);
                var electroCo = vehicle2pollutants.get(FREIGHT_REWE_VERBRAUCHERMARKT_TROCKEN_VEH_MEDIUM_18_T_ELECTRO_160444_1).getEmission(Pollutant.CO);
          if(vehicleId.equals(Id.createVehicleId("freight_rewe_SUPERMARKT_TROCKEN_veh_heavy26t_160444_5"))){
            System.out.println("current CO: " + currentCo + " ++ electro CO: " + electroCo);
          }
                current.addEmissions(emissions);
        }


        if ( vehicle2pollutants.get(FREIGHT_REWE_VERBRAUCHERMARKT_TROCKEN_VEH_MEDIUM_18_T_ELECTRO_160444_1).getEmission(Pollutant.CO) != tempValue.doubleValue()){
          System.out.println("JETZT wurde was modifiziert: "  + "vehid: " + vehicleId);
          System.out.println("EMISSIONS: "+ emissions.toString());
          System.out.println("EVENT:" + event.toString());
          System.out.println("---");
          tempValue = vehicle2pollutants.get(FREIGHT_REWE_VERBRAUCHERMARKT_TROCKEN_VEH_MEDIUM_18_T_ELECTRO_160444_1).getEmission(Pollutant.CO);
        }
    }
}
