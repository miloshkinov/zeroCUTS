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
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.analysis.EmissionsByPollutant;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.vehicles.Vehicle;

/**
 * Collects Warm- and Cold-Emission-Events and returns them summed up by vehicleId.
 */
public class EmissionsPerVehicleEventHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler {

  private static final Logger log = LogManager.getLogger( EmissionsPerVehicleEventHandler.class );
  private final Map<Id<Vehicle>, EmissionsByPollutant> vehicle2pollutants = new LinkedHashMap<>();

  public EmissionsPerVehicleEventHandler() {}

  /**
   * Yields summed link emissions
   *
   * @return Total emissions per pollutant by vehicle id
   */
  public Map<Id<Vehicle>, EmissionsByPollutant> getVehicle2pollutants() {
    return vehicle2pollutants; }

  @Override
  public void reset(int iteration) {
    vehicle2pollutants.clear();
  }

  @Override
  public void handleEvent(WarmEmissionEvent event) {
    handleEmissionEvent( event.getVehicleId(), event.getWarmEmissions());
  }

  @Override
  public void handleEvent(ColdEmissionEvent event) {
    handleEmissionEvent(event.getVehicleId(), event.getColdEmissions());
  }

  private void handleEmissionEvent(Id<Vehicle> vehicleId, Map<Pollutant, Double> emissions) {
    // The following deep copy creation is needed to avoid Call-by-Reference issues when using
    // another EventHandler in parallel. KMT, PH march'24
    var newMap = new HashMap<Pollutant, Double>();
    for (Entry<Pollutant, Double> pollutantDoubleEntry : emissions.entrySet()) {
      newMap.put(pollutantDoubleEntry.getKey(), (Double) pollutantDoubleEntry.getValue().doubleValue());
    }

    EmissionsByPollutant emissionsByPollutant = new EmissionsByPollutant(newMap);

    //Sum up ver VehicleId
    if (vehicle2pollutants.get(vehicleId) == null) {
      vehicle2pollutants.put(vehicleId, emissionsByPollutant); }
    else {
      vehicle2pollutants.get(vehicleId).addEmissions(emissions);
    }
  }

}
