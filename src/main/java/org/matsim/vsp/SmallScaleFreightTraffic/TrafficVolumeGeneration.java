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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import com.google.common.base.Joiner;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

/**
 * @author Ricardo Ewert
 *
 */
public class TrafficVolumeGeneration {

	private static final Logger log = LogManager.getLogger(TrafficVolumeGeneration.class);
	private static final Joiner JOIN = Joiner.on("\t");

	private static HashMap<Integer, HashMap<String, Double>> generationRatesStart = new HashMap<Integer, HashMap<String, Double>>();
	private static HashMap<Integer, HashMap<String, Double>> generationRatesStop = new HashMap<Integer, HashMap<String, Double>>();
	private static HashMap<String, HashMap<String, Double>> commitmentRatesStart = new HashMap<String, HashMap<String, Double>>();
	private static HashMap<String, HashMap<String, Double>> commitmentRatesStop = new HashMap<String, HashMap<String, Double>>();

	/**
	 * Creates the traffic volume (start) for each zone separated in the
	 * modesORvehTypes and the purposes.
	 * 
	 * @param resultingDataPerZone
	 * @param output
	 * @param inputDataDirectory
	 * @param sample
	 * @param modesORvehTypes
	 * @return trafficVolume_start
	 * @throws MalformedURLException
	 */
	static HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> createTrafficVolume_start(
			HashMap<String, Object2DoubleMap<String>> resultingDataPerZone, Path output, Path inputDataDirectory,
			double sample, ArrayList<String> modesORvehTypes, String trafficType) throws MalformedURLException {

		HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolume_start = new HashMap<String, HashMap<String, Object2DoubleMap<Integer>>>();
		calculateTrafficVolumePerZone(trafficVolume_start, resultingDataPerZone, "start", modesORvehTypes);
		Path outputFileStart = output.resolve("caculatedData")
				.resolve("TrafficVolume_"+trafficType+"_"+"startPerZone_" + (int) (sample * 100) + "pt.csv");
		writeCSVTrafficVolume(trafficVolume_start, outputFileStart, sample);
		log.info("Write traffic volume for start trips per zone in CSV: " + outputFileStart);
		return trafficVolume_start;
	}

	/**
	 * Creates the traffic volume (stop) for each zone separated in the
	 * modesORvehTypes and the purposes.
	 * 
	 * @param resultingDataPerZone
	 * @param output
	 * @param inputDataDirectory
	 * @param sample
	 * @param modesORvehTypes
	 * @return trafficVolume_stop
	 * @throws MalformedURLException
	 */
	static HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> createTrafficVolume_stop(
			HashMap<String, Object2DoubleMap<String>> resultingDataPerZone, Path output, Path inputDataDirectory,
			double sample, ArrayList<String> modesORvehTypes, String trafficType) throws MalformedURLException {

		HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolume_stop = new HashMap<String, HashMap<String, Object2DoubleMap<Integer>>>();
		calculateTrafficVolumePerZone(trafficVolume_stop, resultingDataPerZone, "stop", modesORvehTypes);
		Path outputFileStop = output.resolve("caculatedData")
				.resolve("TrafficVolume_"+trafficType+"_"+"stopPerZone_" + (int) (sample * 100) + "pt.csv");
		writeCSVTrafficVolume(trafficVolume_stop, outputFileStop, sample);
		log.info("Write traffic volume for stop trips per zone in CSV: " + outputFileStop);
		return trafficVolume_stop;
	}

	/**
	 * Calculates the traffic volume for each zone and purpose.
	 * 
	 * @param trafficVolume
	 * @param resultingDataPerZone
	 * @param volumeType
	 * @param modesORvehTypes
	 * @return trafficVolume
	 */
	private static HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> calculateTrafficVolumePerZone(
			HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolume,
			HashMap<String, Object2DoubleMap<String>> resultingDataPerZone, String volumeType,
			ArrayList<String> modesORvehTypes) {

		HashMap<Integer, HashMap<String, Double>> generationRates = new HashMap<Integer, HashMap<String, Double>>();
		HashMap<String, HashMap<String, Double>> commitmentRates = new HashMap<String, HashMap<String, Double>>();

		if (volumeType.equals("start")) {
			generationRates = generationRatesStart;
			commitmentRates = commitmentRatesStart;
		} else if (volumeType.equals("stop")) {
			generationRates = generationRatesStop;
			commitmentRates = commitmentRatesStop;
		} else
			throw new RuntimeException("No generation and commitment rates selected. Please check!");

		for (String zoneId : resultingDataPerZone.keySet()) {
			HashMap<String, Object2DoubleMap<Integer>> valuesForZone = new HashMap<String, Object2DoubleMap<Integer>>();
			for (String modeORvehType : modesORvehTypes) {
				Object2DoubleMap<Integer> trafficValuesPerPurpose = new Object2DoubleOpenHashMap<>();
				for (Integer purpose : generationRates.keySet()) {

					if (resultingDataPerZone.get(zoneId).isEmpty())
						trafficValuesPerPurpose.merge(purpose, 0., Double::sum);
					else
						for (String category : resultingDataPerZone.get(zoneId).keySet()) {
							double commitmentFactor;
							if (modeORvehType.equals("total"))
								commitmentFactor = 1;
							else
								commitmentFactor = commitmentRates
										.get(purpose + "_" + modeORvehType.substring(modeORvehType.length() - 1))
										.get(category);
							double generationFactor = generationRates.get(purpose).get(category);
							double newValue = resultingDataPerZone.get(zoneId).getDouble(category) * generationFactor
									* commitmentFactor;
							trafficValuesPerPurpose.merge(purpose, newValue, Double::sum);
						}
					trafficValuesPerPurpose.replace(purpose, trafficValuesPerPurpose.getDouble(purpose)); // notwendig?
				}
				valuesForZone.put(modeORvehType, trafficValuesPerPurpose);
			}
			trafficVolume.put(zoneId, valuesForZone);
		}
		return trafficVolume;
	}

	/**
	 * Writes the traffic volume.
	 * 
	 * @param trafficVolume
	 * @param outputFileInInputFolder
	 * @param sample
	 * @throws MalformedURLException
	 */
	private static void writeCSVTrafficVolume(HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolume,
			Path outputFileInInputFolder, double sample) throws MalformedURLException {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFileInInputFolder.toUri().toURL(),
				StandardCharsets.UTF_8, true);
		try {
			String[] header = new String[] { "areaID", "mode", "1", "2", "3", "4", "5" };
			JOIN.appendTo(writer, header);
			writer.write("\n");
			for (String zoneID : trafficVolume.keySet()) {
				for (String modeORvehType : trafficVolume.get(zoneID).keySet()) {
					List<String> row = new ArrayList<>();
					row.add(zoneID);
					row.add(modeORvehType);
					Integer count = 1;
					while (count < 6) {
						row.add(String.valueOf(
								Math.round(trafficVolume.get(zoneID).get(modeORvehType).getDouble(count) * sample)));
						count++;
					}
					JOIN.appendTo(writer, row);
					writer.write("\n");
				}
			}
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Loads the input data based on the selected trafficType.
	 * 
	 * @param inputDataDirectory
	 * @param trafficType
	 * @throws IOException
	 */
	static void loadInputParamters(Path inputDataDirectory, String trafficType) throws IOException {

		// Read generation rates for start potentials
		Path generationRatesStartPath = inputDataDirectory.resolve("parameters")
				.resolve("generationRates_start_" + trafficType + ".csv");
		generationRatesStart = readGenerationRates(generationRatesStartPath);
		log.info("Read generations rates (start)");

		// Read generation rates for stop potentials
		Path generationRatesStopPath = inputDataDirectory.resolve("parameters")
				.resolve("generationRates_stop_" + trafficType + ".csv");
		generationRatesStop = readGenerationRates(generationRatesStopPath);
		log.info("Read generations rates (stop)");

		// read commitment rates for start potentials
		Path commitmentRatesStartPath = inputDataDirectory.resolve("parameters")
				.resolve("commitmentRates_start_" + trafficType + ".csv");
		commitmentRatesStart = readCommitmentRates(commitmentRatesStartPath);
		log.info("Read commitment rates (start)");

		// read commitment rates for stop potentials
		Path commitmentRatesStopPath = inputDataDirectory.resolve("parameters")
				.resolve("commitmentRates_stop_" + trafficType + ".csv");
		commitmentRatesStop = readCommitmentRates(commitmentRatesStopPath);
		log.info("Read commitment rates (stop)");
	}

	/**
	 * Reads the data for the generation rates.
	 * 
	 * @param generationRatesPath
	 * @return
	 * @throws IOException
	 */
	private static HashMap<Integer, HashMap<String, Double>> readGenerationRates(Path generationRatesPath)
			throws IOException {
		HashMap<Integer, HashMap<String, Double>> generationRates = new HashMap<Integer, HashMap<String, Double>>();
		if (!Files.exists(generationRatesPath)) {
			log.error("Required input data file {} not found", generationRatesPath);
		}
		try (CSVParser parser = new CSVParser(Files.newBufferedReader(generationRatesPath),
				CSVFormat.TDF.withFirstRecordAsHeader())) {

			for (CSVRecord record : parser) {
				HashMap<String, Double> lookUpTable = new HashMap<>();
				for (String category : parser.getHeaderMap().keySet()) {
					if (!category.equals("purpose"))
						lookUpTable.put(category, Double.valueOf(record.get(category)));
				}
				generationRates.put(Integer.valueOf(record.get(0)), lookUpTable);
			}
		}
		return generationRates;
	}

	/**
	 * Reads the data for the commitment rates. The modes for the businessTraffic:
	 * pt = public transport; it = individual traffic; op = elective (wahlfrei)
	 * 
	 * @param generationRatesPath
	 * @return
	 * @throws IOException
	 */
	private static HashMap<String, HashMap<String, Double>> readCommitmentRates(Path commitmentRatesPath)
			throws IOException {
		HashMap<String, HashMap<String, Double>> commitmentRates = new HashMap<String, HashMap<String, Double>>();
		if (!Files.exists(commitmentRatesPath)) {
			log.error("Required input data file {} not found", commitmentRatesPath);
		}
		try (CSVParser parser = new CSVParser(Files.newBufferedReader(commitmentRatesPath),
				CSVFormat.TDF.withFirstRecordAsHeader())) {

			for (CSVRecord record : parser) {
				HashMap<String, Double> lookUpTable = new HashMap<>();
				for (String category : parser.getHeaderMap().keySet()) {
					if (!category.equals("purpose") && !category.equals("mode"))
						lookUpTable.put(category, Double.valueOf(record.get(category)));
				}
				commitmentRates.put((record.get(0) + "_" + record.get(1)), lookUpTable);
			}
		}
		return commitmentRates;
	}
}
