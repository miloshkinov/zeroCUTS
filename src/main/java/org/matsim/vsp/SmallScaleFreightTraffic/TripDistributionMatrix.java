package org.matsim.vsp.SmallScaleFreightTraffic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.application.options.ShpOptions;
import org.opengis.feature.simple.SimpleFeature;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

/**
 * @author Ricardo
 *
 */
public class TripDistributionMatrix {

	private ArrayList<String> listOfZones = new ArrayList<String>();
	private ArrayList<String> listOfModes = new ArrayList<String>();
	private ArrayList<Integer> listOfPurposes = new ArrayList<Integer>();
	private final List<SimpleFeature> zonesFeatures;
	private final HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerTypeAndZone_start;
	private final HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerTypeAndZone_stop;
	private final double sample;

	static class TripDistributionMatrixKey {
		private final String fromZone;
		private final String toZone;
		private final String mode;
		private final int purpose;

		public TripDistributionMatrixKey(String fromZone, String toZone, String mode, int purpose) {
			super();
			this.fromZone = fromZone;
			this.toZone = toZone;
			this.mode = mode;
			this.purpose = purpose;
		}

		public String getFromZone() {
			return fromZone;
		}

		public String getToZone() {
			return toZone;
		}

		public String getMode() {
			return mode;
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
			result = prime * result + ((toZone == null) ? 0 : toZone.hashCode());
			result = prime * result + ((mode == null) ? 0 : mode.hashCode());
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
			if (mode == null) {
				if (other.mode != null)
					return false;
			} else if (!mode.equals(other.mode))
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
		private final String mode;
		private final int purpose;

		public GravityConstantKey(String fromZone, String mode, int purpose) {
			super();
			this.fromZone = fromZone;
			this.mode = mode;
			this.purpose = purpose;
		}

		public String getFromZone() {
			return fromZone;
		}

		public String getMode() {
			return mode;
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
			result = prime * result + ((mode == null) ? 0 : mode.hashCode());
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
			if (mode == null) {
				if (other.mode != null)
					return false;
			} else if (!mode.equals(other.mode))
				return false;
			return true;
		}
	}

	public static class Builder {

		private final List<SimpleFeature> zonesFeatures;
		private final HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerTypeAndZone_start;
		private final HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerTypeAndZone_stop;
		private final double sample;

		public static Builder newInstance(ShpOptions shpZones,
				HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerTypeAndZone_start,
				HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerTypeAndZone_stop,
				double sample) {
			return new Builder(shpZones, trafficVolumePerTypeAndZone_start, trafficVolumePerTypeAndZone_stop, sample);
		}

		private Builder(ShpOptions shpZones,
				HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerTypeAndZone_start,
				HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerTypeAndZone_stop,
				double sample) {
			super();
			this.zonesFeatures = shpZones.readFeatures();
			this.trafficVolumePerTypeAndZone_start = trafficVolumePerTypeAndZone_start;
			this.trafficVolumePerTypeAndZone_stop = trafficVolumePerTypeAndZone_stop;
			this.sample = sample;
		}

		public TripDistributionMatrix build() {
			return new TripDistributionMatrix(this);
		}
	}

	private TripDistributionMatrix(Builder builder) {
		zonesFeatures = builder.zonesFeatures;
		trafficVolumePerTypeAndZone_start = builder.trafficVolumePerTypeAndZone_start;
		trafficVolumePerTypeAndZone_stop = builder.trafficVolumePerTypeAndZone_stop;
		sample = builder.sample;
	}

	private final ConcurrentHashMap<TripDistributionMatrixKey, Integer> matrixCache = new ConcurrentHashMap<TripDistributionMatrixKey, Integer>();
	private final ConcurrentHashMap<ResistanceFunktionKey, Double> resistanceFunktionCache = new ConcurrentHashMap<ResistanceFunktionKey, Double>();
	private final ConcurrentHashMap<GravityConstantKey, Double> gravityConstantACache = new ConcurrentHashMap<GravityConstantKey, Double>();
//	private final ConcurrentHashMap<String, Double> gravityConstantBCache = new ConcurrentHashMap<String, Double>();
	private final ConcurrentHashMap<String, Object2DoubleMap<String>> roundingError = new ConcurrentHashMap<>();
	private int createdVolume = 0;

	void setTripDistributionValue(String startZone, String stopZone, String mode, Integer purpose) {
		double volumeStart = trafficVolumePerTypeAndZone_start.get(startZone).get(mode).getDouble(purpose);
		double volumeStop = trafficVolumePerTypeAndZone_stop.get(stopZone).get(mode).getDouble(purpose);
		double resistanceValue = getResistanceFunktionValue(startZone, stopZone);
		double gravityConstantA = getGravityConstant(startZone, trafficVolumePerTypeAndZone_stop, mode, purpose);
		roundingError.computeIfAbsent(startZone, (k) -> new Object2DoubleOpenHashMap<>());
		/*
		 * gravity model Anpassungen: Faktor anpassen, z.B. reale Reisezeiten im Netz,
		 * auch besonders für ÖV Bisher: Gravity model mit fixem Quellverkehr
		 */
		double volume = gravityConstantA * volumeStart * volumeStop * resistanceValue;
		double sampledVolume = sample * volume;
		int roundedSampledVolume = (int) Math.floor(sampledVolume);
		double certainRoundingError = sampledVolume - roundedSampledVolume;
		roundingError.get(startZone).merge((mode + "_" + purpose), certainRoundingError, Double::sum);
		if (roundingError.get(startZone).getDouble((mode + "_" + purpose)) >= 1) {
			roundedSampledVolume++;
			roundingError.get(startZone).merge((mode + "_" + purpose), -1, Double::sum);
		} // TODO eventuell methodik für den letzten error rest am Ende
		TripDistributionMatrixKey matrixKey = makeKey(startZone, stopZone, mode, purpose);
		createdVolume = createdVolume + roundedSampledVolume;
		matrixCache.put(matrixKey, roundedSampledVolume);
	}

	Integer getTripDistributionValue(String startZone, String stopZone, String mode, Integer purpose) {
		TripDistributionMatrixKey matrixKey = makeKey(startZone, stopZone, mode, purpose);
		return matrixCache.get(matrixKey);
	}

	/**
	 * Creates a map of the values of the resistance function between two zones.
	 * 
	 * @param stopZone2
	 * @param startZone2
	 * 
	 * @param zonesFeatures
	 * @return
	 */
	private Double getResistanceFunktionValue(String startZone, String stopZone) {
		if (!resistanceFunktionCache.containsKey(makeResistanceFunktionKey(startZone, stopZone)))
			for (SimpleFeature startZoneFeature : zonesFeatures) {
				String zone1 = String.valueOf(startZoneFeature.getAttribute("gml_id"));
				if (!startZone.equals(zone1))
					continue;
				for (SimpleFeature stopZoneFeature : zonesFeatures) {
					String zone2 = String.valueOf(stopZoneFeature.getAttribute("gml_id"));
					if (!stopZone.equals(zone2))
						continue;
					double distance = Double.MAX_VALUE;

					if (zone1.equals(zone2))
						distance = 0;
					else {
						Point geometryStartZone = ((Geometry) startZoneFeature.getDefaultGeometry()).getCentroid();
						Point geometryStopZone = ((Geometry) stopZoneFeature.getDefaultGeometry()).getCentroid();

						distance = geometryStartZone.distance(geometryStopZone);
					}
					double resistanceFunktionResult = Math.exp(-distance);
					resistanceFunktionCache.put(makeResistanceFunktionKey(zone1, zone2), resistanceFunktionResult);
				}
			}
		return resistanceFunktionCache.get(makeResistanceFunktionKey(startZone, stopZone));
	}

	/**
	 * Calculates the gravity constant.
	 * 
	 * @param baseZone
	 * @param trafficVolumePerTypeAndZone
	 * @param resistanceValue
	 * @param mode
	 * @param purpose
	 * @return
	 */
	private double getGravityConstant(String baseZone,
			HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerTypeAndZone, String mode,
			Integer purpose) {

		GravityConstantKey gravityKey = makeGravityKey(baseZone, mode, purpose);
		if (!gravityConstantACache.containsKey(gravityKey)) {
			double sum = 0;
			for (String zone : trafficVolumePerTypeAndZone.keySet()) {
				double volume = trafficVolumePerTypeAndZone.get(zone).get(mode).getDouble(purpose);
				double resistanceValue = getResistanceFunktionValue(baseZone, zone);
				sum = sum + (volume * resistanceValue);
			}
			double getGravityCostant = 1 / sum;
			gravityConstantACache.put(gravityKey, getGravityCostant);
		}
		return gravityConstantACache.get(gravityKey);

	}

	/**
	 * Creates a key for the tripDistributionMatrix.
	 * 
	 * @param fromZone
	 * @param toZone
	 * @param mode
	 * @param purpose
	 * @return
	 */
	private TripDistributionMatrixKey makeKey(String fromZone, String toZone, String mode, int purpose) {
		return new TripDistributionMatrixKey(fromZone, toZone, mode, purpose);
	}

	/**
	 * Creates a key for the tripDistributionMatrix.
	 * 
	 * @param fromZone
	 * @param toZone
	 * @param mode
	 * @param purpose
	 * @return
	 */
	private ResistanceFunktionKey makeResistanceFunktionKey(String fromZone, String toZone) {
		return new ResistanceFunktionKey(fromZone, toZone);
	}

	/**
	 * Creates a key for the tripDistributionMatrix.
	 * 
	 * @param fromZone
	 * @param toZone
	 * @param mode
	 * @param purpose
	 * @return
	 */
	private GravityConstantKey makeGravityKey(String fromZone, String mode, int purpose) {
		return new GravityConstantKey(fromZone, mode, purpose);
	}

	/**
	 * Returns all zones being used as a start and/or stop location
	 * 
	 * @param odMatrix
	 * @return
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
	 * @param odMatrix
	 * @return
	 */
	ArrayList<String> getListOfModes() {
		if (listOfModes.isEmpty()) {
			for (TripDistributionMatrixKey key : matrixCache.keySet()) {
				if (!listOfModes.contains(key.getMode()))
					listOfModes.add(key.getMode());
			}
		}
		return listOfModes;
	}

	/**
	 * Returns all purposes being used.
	 * 
	 * @param odMatrix
	 * @return
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

	int getSumOfServicesForStartZone(String startZone, String mode, int purpose) {
		int numberOfTrips = 0;
		ArrayList<String> zones = getListOfZones();
		for (String stopZone : zones)
			numberOfTrips = numberOfTrips + matrixCache.get(makeKey(startZone, stopZone, mode, purpose));
		return numberOfTrips;
	}
}
