/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package org.matsim.vsp.SmallScaleFreightTraffic;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//import org.locationtech.jts.geom.Geometry;
//import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.opengis.feature.simple.SimpleFeature;

import com.google.common.base.Joiner;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

/**
 * @author Ricardo
 *
 */
public class TripDistributionMatrix {

	private static final Logger log = LogManager.getLogger(TripDistributionMatrix.class);
	private static final Joiner JOIN = Joiner.on("\t");

	private ArrayList<String> listOfZones = new ArrayList<String>();
	private ArrayList<String> listOfModesORvehTypes = new ArrayList<String>();
	private ArrayList<Integer> listOfPurposes = new ArrayList<Integer>();
	private final List<SimpleFeature> zonesFeatures;
	private final HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolume_start;
	private final HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolume_stop;
	private final String trafficType;

	static class TripDistributionMatrixKey {
		private final String fromZone;
		private final String toZone;
		private final String modeORvehType;
		private final int purpose;
		private final String trafficType;

		public TripDistributionMatrixKey(String fromZone, String toZone, String modeORvehType, int purpose, String trafficType) {
			super();
			this.fromZone = fromZone;
			this.toZone = toZone;
			this.modeORvehType = modeORvehType;
			this.purpose = purpose;
			this.trafficType = trafficType;
		}

		public String getFromZone() {
			return fromZone;
		}

		public String getToZone() {
			return toZone;
		}

		public String getModesORvehType() {
			return modeORvehType;
		}

		public int getPurpose() {
			return purpose;
		}
		
		public String getTrafficType() {
			return trafficType;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((fromZone == null) ? 0 : fromZone.hashCode());
			long temp;
			temp = Double.doubleToLongBits(purpose);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			result = prime * result + ((toZone == null) ? 0 : toZone.hashCode());
			result = prime * result + ((modeORvehType == null) ? 0 : modeORvehType.hashCode());
			result = prime * result + ((trafficType == null) ? 0 : trafficType.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TripDistributionMatrixKey other = (TripDistributionMatrixKey) obj;
			if (fromZone == null) {
				if (other.fromZone != null)
					return false;
			} else if (!fromZone.equals(other.fromZone))
				return false;
			if (Double.doubleToLongBits(purpose) != Double.doubleToLongBits(other.purpose))
				return false;
			if (toZone == null) {
				if (other.toZone != null)
					return false;
			} else if (!toZone.equals(other.toZone))
				return false;
			if (modeORvehType == null) {
				if (other.modeORvehType != null)
					return false;
			} else if (!modeORvehType.equals(other.modeORvehType))
				return false;
			if (trafficType == null) {
				if (other.trafficType != null)
					return false;
			} else if (!trafficType.equals(other.trafficType))
				return false;
			return true;
		}
	}

	class ResistanceFunktionKey {
		private final String fromZone;
		private final String toZone;

		public ResistanceFunktionKey(String fromZone, String toZone) {
			super();
			this.fromZone = fromZone;
			this.toZone = toZone;

		}

		public String getFromZone() {
			return fromZone;
		}

		public String getToZone() {
			return toZone;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((fromZone == null) ? 0 : fromZone.hashCode());
			result = prime * result + ((toZone == null) ? 0 : toZone.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ResistanceFunktionKey other = (ResistanceFunktionKey) obj;
			if (fromZone == null) {
				if (other.fromZone != null)
					return false;
			} else if (!fromZone.equals(other.fromZone))
				return false;
			if (toZone == null) {
				if (other.toZone != null)
					return false;
			} else if (!toZone.equals(other.toZone))
				return false;
			return true;
		}
	}

	static class GravityConstantKey {
		private final String fromZone;
		private final String modeORvehType;
		private final int purpose;

		public GravityConstantKey(String fromZone, String mode, int purpose) {
			super();
			this.fromZone = fromZone;
			this.modeORvehType = mode;
			this.purpose = purpose;
		}

		public String getFromZone() {
			return fromZone;
		}

		public String getModeORvehType() {
			return modeORvehType;
		}

		public int getPurpose() {
			return purpose;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((fromZone == null) ? 0 : fromZone.hashCode());
			long temp;
			temp = Double.doubleToLongBits(purpose);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			result = prime * result + ((modeORvehType == null) ? 0 : modeORvehType.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			GravityConstantKey other = (GravityConstantKey) obj;
			if (fromZone == null) {
				if (other.fromZone != null)
					return false;
			} else if (!fromZone.equals(other.fromZone))
				return false;
			if (Double.doubleToLongBits(purpose) != Double.doubleToLongBits(other.purpose))
				return false;
			if (modeORvehType == null) {
				if (other.modeORvehType != null)
					return false;
			} else if (!modeORvehType.equals(other.modeORvehType))
				return false;
			return true;
		}
	}

	public static class Builder {

		private final List<SimpleFeature> zonesFeatures;
		private final HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolume_start;
		private final HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolume_stop;
		private final String trafficType;

		public static Builder newInstance(ShpOptions shpZones,
				HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolume_start,
				HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolume_stop,
				String trafficType) {
			return new Builder(shpZones, trafficVolume_start, trafficVolume_stop, trafficType);
		}

		private Builder(ShpOptions shpZones,
				HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolume_start,
				HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolume_stop,
				String trafficType) {
			super();
			this.zonesFeatures = shpZones.readFeatures();
			this.trafficVolume_start = trafficVolume_start;
			this.trafficVolume_stop = trafficVolume_stop;
			this.trafficType = trafficType;
		}

		public TripDistributionMatrix build() {
			return new TripDistributionMatrix(this);
		}
	}

	private TripDistributionMatrix(Builder builder) {
		zonesFeatures = builder.zonesFeatures;
		trafficVolume_start = builder.trafficVolume_start;
		trafficVolume_stop = builder.trafficVolume_stop;
		trafficType = builder.trafficType;
	}

	private final ConcurrentHashMap<TripDistributionMatrixKey, Integer> matrixCache = new ConcurrentHashMap<TripDistributionMatrixKey, Integer>();
	private final ConcurrentHashMap<ResistanceFunktionKey, Double> resistanceFunktionCache = new ConcurrentHashMap<ResistanceFunktionKey, Double>();
	private final ConcurrentHashMap<GravityConstantKey, Double> gravityConstantACache = new ConcurrentHashMap<GravityConstantKey, Double>();
//	private final ConcurrentHashMap<String, Double> gravityConstantBCache = new ConcurrentHashMap<String, Double>();
	private final ConcurrentHashMap<String, Object2DoubleMap<String>> roundingError = new ConcurrentHashMap<>();
	private NetworkBasedTransportCosts netBasedCosts = null;

	/**
	 * Calculates the traffic volume between two zones for a specific modeORvehType
	 * and purpuse.
	 * 
	 * @param startZone
	 * @param stopZone
	 * @param modeORvehType
	 * @param purpose
	 * @param trafficType
	 * @param scenario 
	 * @param regionLinksMap 
	 */
	void setTripDistributionValue(String startZone, String stopZone, String modeORvehType, Integer purpose, String trafficType, Network network, Map<String, HashMap<Id<Link>, Link>> regionLinksMap) {
		double volumeStart = trafficVolume_start.get(startZone).get(modeORvehType).getDouble(purpose);
		double volumeStop = trafficVolume_stop.get(stopZone).get(modeORvehType).getDouble(purpose);
		double resistanceValue = getResistanceFunktionValue(startZone, stopZone, network, regionLinksMap);
		double gravityConstantA = getGravityConstant(stopZone, trafficVolume_start, modeORvehType, purpose, network, regionLinksMap);
		roundingError.computeIfAbsent(stopZone, (k) -> new Object2DoubleOpenHashMap<>());

		//Bisher: Gravity model mit fixem Zielverkehr
		double volume = gravityConstantA * volumeStart * volumeStop * resistanceValue;	
		int roundedVolume = (int) Math.floor(volume);
		double certainRoundingError = (roundedVolume - volume) * -1;
		// roundingError based on stopZone, because gravity model is stopVolume fixed
		roundingError.get(stopZone).merge((modeORvehType + "_" + purpose), certainRoundingError, Double::sum);
		if (roundingError.get(stopZone).getDouble((modeORvehType + "_" + purpose)) >= 1) {
			roundedVolume++;
			roundingError.get(stopZone).merge((modeORvehType + "_" + purpose), -1, Double::sum);
		} 
		TripDistributionMatrixKey matrixKey = makeKey(startZone, stopZone, modeORvehType, purpose, trafficType);
		matrixCache.put(matrixKey, roundedVolume);
	}

	/**
	 * Gets value of the need traffic volume.
	 * 
	 * @param startZone
	 * @param stopZone
	 * @param modeORvehType
	 * @param purpose
	 * @param trafficType
	 * @return
	 */
	Integer getTripDistributionValue(String startZone, String stopZone, String modeORvehType, Integer purpose, String trafficType) {
		TripDistributionMatrixKey matrixKey = makeKey(startZone, stopZone, modeORvehType, purpose, trafficType);
		return matrixCache.get(matrixKey);
	}

	/**
	 * Calculates the values of the resistance function between two zones.
	 * 
	 * @param startZone
	 * @param stopZone
	 * @param scenario 
	 * @param regionLinksMap 
	 * @return
	 */
	private Double getResistanceFunktionValue(String startZone, String stopZone, Network network, Map<String, HashMap<Id<Link>, Link>> regionLinksMap) {
		
		if (netBasedCosts == null) {
			VehicleType vehicleType = VehicleUtils.createVehicleType(Id.create("vwCaddy", VehicleType.class));
			vehicleType.getCostInformation().setCostsPerMeter(0.00017).setCostsPerSecond(0.00948).setFixedCost(22.73);
			NetworkBasedTransportCosts.Builder netBuilder = NetworkBasedTransportCosts.Builder.newInstance(network, Arrays.asList(vehicleType));
			netBasedCosts = netBuilder.build();
		}
		if (!resistanceFunktionCache.containsKey(makeResistanceFunktionKey(startZone, stopZone)))
			for (SimpleFeature startZoneFeature : zonesFeatures) {
				String zone1 = String.valueOf(startZoneFeature.getAttribute("areaID"));
				if (!startZone.equals(zone1))
					continue;
				for (SimpleFeature stopZoneFeature : zonesFeatures) {
					String zone2 = String.valueOf(stopZoneFeature.getAttribute("areaID"));
					if (!stopZone.equals(zone2))
						continue;
					double distance = Double.MAX_VALUE;
					double travelCosts = Double.MAX_VALUE;
					if (zone1.equals(zone2)) {
						travelCosts = 0;
						distance = 0;
					}
					else {
						
//						Point geometryStartZone = ((Geometry) startZoneFeature.getDefaultGeometry()).getCentroid();
//						Point geometryStopZone = ((Geometry) stopZoneFeature.getDefaultGeometry()).getCentroid();
//
//						distance = geometryStartZone.distance(geometryStopZone);
						
						Location startLocation = Location.newInstance(regionLinksMap.get(startZone).keySet().iterator().next().toString());
						Location stopLocation = Location.newInstance(regionLinksMap.get(stopZone).keySet().iterator().next().toString());
						Vehicle exampleVehicle = getExampleVehicle(startLocation);
////						distance = netBasedCosts.getDistance(startLocation, stopLocation, 21600., exampleVehicle);
						travelCosts = netBasedCosts.getTransportCost(startLocation, stopLocation, 21600., null, exampleVehicle);
					}
					double resistanceFactor = 0.005;
//					double resistanceFunktionResult = Math.exp(-distance);
					double resistanceFunktionResult = Math.exp(-resistanceFactor*travelCosts);
//					double resistanceFunktionResult = 1 / (travelCosts*travelCosts);
					resistanceFunktionCache.put(makeResistanceFunktionKey(zone1, zone2), resistanceFunktionResult);
					resistanceFunktionCache.put(makeResistanceFunktionKey(zone2, zone1), resistanceFunktionResult);
				}
			}
		return resistanceFunktionCache.get(makeResistanceFunktionKey(startZone, stopZone));
	}

	/** Corrects missing trafficVolume in the OD because of roundingErrors based on trafficVolume_stop
	 * 
	 */
	public void clearRoundingError(){

		for (String stopZone : getListOfZones()) {
			for (String modeORvehType : getListOfModesOrVehTypes()) {
				for (Integer purpose : getListOfPurposes()) {
					double trafficVolume = trafficVolume_stop.get(stopZone).get(modeORvehType).getDouble(purpose);
					int generatedTrafficVolume = getSumOfServicesForStopZone(stopZone, modeORvehType, purpose, trafficType);
					if (trafficVolume > generatedTrafficVolume) {
						if (generatedTrafficVolume == 0) {
							TripDistributionMatrixKey matrixKey = makeKey(stopZone, stopZone, modeORvehType, purpose,
									trafficType);
							matrixCache.replace(matrixKey, matrixCache.get(matrixKey) + 1);
							generatedTrafficVolume = getSumOfServicesForStopZone(stopZone, modeORvehType, purpose, trafficType);
						} else {
							ArrayList<String> shuffeldZones = new ArrayList<String>(getListOfZones());
							Collections.shuffle(shuffeldZones);
							for (String startZone : shuffeldZones) {
								TripDistributionMatrixKey matrixKey = makeKey(startZone, stopZone, modeORvehType,
										purpose, trafficType);
								if (matrixCache.get(matrixKey) > 0) {
									matrixCache.replace(matrixKey, matrixCache.get(matrixKey) + 1);
									break;
								}
							}
						}
					}
				}
			}
		}
	}
	

	/** Create example vehicle for netBasedCosts calculation.
	 * @param fromId
	 * @return
	 */
	private VehicleImpl getExampleVehicle(Location fromId) {
		return VehicleImpl.Builder.newInstance("vwCaddy").setType(
				com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl.Builder.newInstance("vwCaddy").build())
				.setStartLocation(fromId).build();
	}

	/**
	 * Calculates the gravity constant.
	 * 
	 * @param baseZone
	 * @param trafficVolume
	 * @param modeORvehType
	 * @param purpose
	 * @param scenario 
	 * @param regionLinksMap 
	 * @return gravity constant
	 */
	private double getGravityConstant(String baseZone,
			HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolume, String modeORvehType,
			Integer purpose, Network network, Map<String, HashMap<Id<Link>, Link>> regionLinksMap) {

		GravityConstantKey gravityKey = makeGravityKey(baseZone, modeORvehType, purpose);
		if (!gravityConstantACache.containsKey(gravityKey)) {
			double sum = 0;
			for (String zone : trafficVolume.keySet()) {
				double volume = trafficVolume.get(zone).get(modeORvehType).getDouble(purpose);
				double resistanceValue = getResistanceFunktionValue(baseZone, zone, network, regionLinksMap);
				sum = sum + (volume * resistanceValue);
			}
			double gravityCostant = 1 / sum;
			gravityConstantACache.put(gravityKey, gravityCostant);
		}
		return gravityConstantACache.get(gravityKey);

	}

	/**
	 * Creates a key for the tripDistributionMatrix.
	 * 
	 * @param fromZone
	 * @param toZone
	 * @param modeORvehType
	 * @param purpose
	 * @param trafficType
	 * @return TripDistributionMatrixKey
	 */
	private TripDistributionMatrixKey makeKey(String fromZone, String toZone, String modeORvehType, int purpose, String trafficType) {
		return new TripDistributionMatrixKey(fromZone, toZone, modeORvehType, purpose, trafficType);
	}

	/**
	 * Creates a key for the resistance function.
	 * 
	 * @param fromZone
	 * @param toZone
	 * @return ResistanceFunktionKey
	 */
	private ResistanceFunktionKey makeResistanceFunktionKey(String fromZone, String toZone) {
		return new ResistanceFunktionKey(fromZone, toZone);
	}

	/**
	 * Creates a key for a gravity constant.
	 * 
	 * @param fromZone
	 * @param toZone
	 * @param modeOrVehType
	 * @param purpose
	 * @return GravityConstantKey
	 */
	private GravityConstantKey makeGravityKey(String fromZone, String modeOrVehType, int purpose) {
		return new GravityConstantKey(fromZone, modeOrVehType, purpose);
	}

	/**
	 * Returns all zones being used as a start and/or stop location
	 * 
	 * @return listOfZones
	 */
	ArrayList<String> getListOfZones() {
		if (listOfZones.isEmpty())
			for (TripDistributionMatrixKey key : matrixCache.keySet()) {
				if (!listOfZones.contains(key.getFromZone()))
					listOfZones.add(key.getFromZone());
				if (!listOfZones.contains(key.getToZone()))
					listOfZones.add(key.getToZone());
			}
		return listOfZones;
	}

	/**
	 * Returns all modes being used.
	 * 
	 * @return listOfModesORvehTypes
	 */
	ArrayList<String> getListOfModesOrVehTypes() {
		if (listOfModesORvehTypes.isEmpty()) {
			for (TripDistributionMatrixKey key : matrixCache.keySet()) {
				if (!listOfModesORvehTypes.contains(key.getModesORvehType()))
					listOfModesORvehTypes.add(key.getModesORvehType());
			}
		}
		return listOfModesORvehTypes;
	}

	/**
	 * Returns all purposes being used.
	 * 
	 * @return listOfPurposes
	 */
	ArrayList<Integer> getListOfPurposes() {
		if (listOfPurposes.isEmpty()) {
			for (TripDistributionMatrixKey key : matrixCache.keySet()) {
				if (!listOfPurposes.contains(key.getPurpose()))
					listOfPurposes.add(key.getPurpose());
			}
		}
		return listOfPurposes;
	}

	/**
	 * @param startZone
	 * @param modeORvehType
	 * @param purpose
	 * @param trafficType
	 * @return numberOfTrips
	 */
	int getSumOfServicesForStartZone(String startZone, String modeORvehType, int purpose, String trafficType) {
		int numberOfTrips = 0;
		ArrayList<String> zones = getListOfZones();
		for (String stopZone : zones) 
			numberOfTrips = numberOfTrips + (int)Math.round(matrixCache.get(makeKey(startZone, stopZone, modeORvehType, purpose, trafficType)));
		return numberOfTrips;
	}
	/**
	 * @param stopZone
	 * @param modeORvehType
	 * @param purpose
	 * @param trafficType
	 * @return numberOfTrips
	 */
	int getSumOfServicesForStopZone(String stopZone, String modeORvehType, int purpose, String trafficType) {
		int numberOfTrips = 0;
		ArrayList<String> zones = getListOfZones();
		for (String startZone : zones) 
			numberOfTrips = numberOfTrips + (int)Math.round(matrixCache.get(makeKey(startZone, stopZone, modeORvehType, purpose, trafficType)));
		return numberOfTrips;
	}

	/**
	 * Writes every matrix for each mode and purpose.
	 * 
	 * @param output
	 * @throws UncheckedIOException
	 * @throws MalformedURLException
	 */
	void writeODMatrices(Path output, String trafficType) throws UncheckedIOException, MalformedURLException {

		ArrayList<String> usedModesORvehTypes = getListOfModesOrVehTypes();
		ArrayList<String> usedZones = getListOfZones();
		ArrayList<Integer> usedPurposes = getListOfPurposes();

		for (String modeORvehType : usedModesORvehTypes) {
			for (int purpose : usedPurposes) {

				Path outputFolder = output.resolve("caculatedData")
						.resolve("odMatrix_" + trafficType +"_" + modeORvehType + "_purpose" + purpose + ".csv");

				BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder.toUri().toURL(), StandardCharsets.UTF_8,
						true);
				try {

					List<String> headerRow = new ArrayList<>();
					headerRow.add("");
					for (int i = 0; i < usedZones.size(); i++) {
						headerRow.add(usedZones.get(i));
					}
					JOIN.appendTo(writer, headerRow);
					writer.write("\n");

					for (String startZone : usedZones) {
						List<String> row = new ArrayList<>();
						row.add(startZone);
						for (String stopZone : usedZones) {
							if (getTripDistributionValue(startZone, stopZone, modeORvehType, purpose, trafficType) == null)
								throw new RuntimeException("OD pair is missing; start: " + startZone + "; stop: "
										+ stopZone + "; modeORvehType: " + modeORvehType + "; purpuse: " + purpose + "; trafficType: " + trafficType);
							row.add(String
									.valueOf(getTripDistributionValue(startZone, stopZone, modeORvehType, purpose, trafficType)));
						}
						JOIN.appendTo(writer, row);
						writer.write("\n");
					}
					writer.close();

				} catch (IOException e) {
					e.printStackTrace();
				}
				log.info("Write OD matrix for mode " + modeORvehType + " and for purpose " + purpose + " to "
						+ outputFolder.toString());
			}
		}
	}
}
