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
 * @author Ricardo
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
	 * Creates the traffic volume (start) for each zone separated in the 3 modes and
	 * the 5 purposes.
	 * 
	 * @param resultingDataPerZone
	 * @param sample 
	 * @param inputDataDirectory 
	 * @param output 
	 * @param usedModeDifferentiation 
	 * @return
	 * @throws MalformedURLException
	 */
	static HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> createTrafficVolume_start(
			HashMap<String, Object2DoubleMap<String>> resultingDataPerZone, Path output, Path inputDataDirectory, double sample, ArrayList<String> modes) throws MalformedURLException {

		HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerTypeAndZone_start = new HashMap<String, HashMap<String, Object2DoubleMap<Integer>>>();
		calculateTrafficVolumePerZone(trafficVolumePerTypeAndZone_start, resultingDataPerZone, "start", modes);
		Path outputFileStart = output.resolve("caculatedData")
				.resolve("TrafficVolume_startPerZone_" + (int) (sample * 100) + "pt.csv");
		writeCSVWithTrafficVolumeperZoneAndModes(trafficVolumePerTypeAndZone_start, outputFileStart, sample);
		log.info("Write traffic volume for start trips per zone in CSV: " + outputFileStart);
		return trafficVolumePerTypeAndZone_start;
	}

	/**
	 * Creates the traffic volume (stop) for each zone separated in the 3 modes and
	 * the 5 purposes.
	 * 
	 * @param resultingDataPerZone
	 * @param sample 
	 * @param inputDataDirectory 
	 * @param output 
	 * @param usedModeDifferentiation 
	 * @return
	 * @throws MalformedURLException
	 */
	static HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> createTrafficVolume_stop(
			HashMap<String, Object2DoubleMap<String>> resultingDataPerZone, Path output, Path inputDataDirectory, double sample, ArrayList<String> modes) throws MalformedURLException {

		HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerTypeAndZone_stop = new HashMap<String, HashMap<String, Object2DoubleMap<Integer>>>();
		calculateTrafficVolumePerZone(trafficVolumePerTypeAndZone_stop, resultingDataPerZone, "stop", modes);
		Path outputFileStop = output.resolve("caculatedData")
				.resolve("TrafficVolume_stopPerZone_" + (int) (sample * 100) + "pt.csv");
		writeCSVWithTrafficVolumeperZoneAndModes(trafficVolumePerTypeAndZone_stop, outputFileStop, sample);
		log.info("Write traffic volume for stop trips per zone in CSV: " + outputFileStop);
		return trafficVolumePerTypeAndZone_stop;
	}
	/**
	 * Calculates the traffic volume for each zone and purpose.
	 * 
	 * @param trafficVolumePerZone
	 * @param resultingDataPerZone
	 * @param volumeType
	 * @param modes 
	 * @return
	 */
	private static HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> calculateTrafficVolumePerZone(
			HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerZone,
			HashMap<String, Object2DoubleMap<String>> resultingDataPerZone, String volumeType, ArrayList<String> modes) {

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
			for (String mode : modes) {
				Object2DoubleMap<Integer> trafficValuesPerPurpose = new Object2DoubleOpenHashMap<>();
				for (Integer purpose : generationRates.keySet()) {

					if (resultingDataPerZone.get(zoneId).isEmpty())
						trafficValuesPerPurpose.merge(purpose, 0., Double::sum);
					else
						for (String category : resultingDataPerZone.get(zoneId).keySet()) {
							double commitmentFactor;
							if (mode.equals("total"))
								commitmentFactor = 1;
							else
								commitmentFactor = commitmentRates.get(purpose + "_" + mode).get(category);
							double generationFactor = generationRates.get(purpose).get(category);
							double newValue = resultingDataPerZone.get(zoneId).getDouble(category) * generationFactor
									* commitmentFactor;
							trafficValuesPerPurpose.merge(purpose, newValue, Double::sum);
						}
					trafficValuesPerPurpose.replace(purpose, trafficValuesPerPurpose.getDouble(purpose)); // notwendig?
				}
				valuesForZone.put(mode, trafficValuesPerPurpose);
			}
			trafficVolumePerZone.put(zoneId, valuesForZone);
		}
		return trafficVolumePerZone;
	}
	
	/**
	 * @param trafficVolumePerTypeAndZone
	 * @param outputFileInInputFolder
	 * @param sample 
	 * @throws MalformedURLException
	 */
	private static void writeCSVWithTrafficVolumeperZoneAndModes(
			HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerTypeAndZone,
			Path outputFileInInputFolder, double sample) throws MalformedURLException {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFileInInputFolder.toUri().toURL(),
				StandardCharsets.UTF_8, true);
		try {
			String[] header = new String[] { "areaID", "mode", "1", "2", "3", "4", "5" };
			JOIN.appendTo(writer, header);
			writer.write("\n");
			for (String zoneID : trafficVolumePerTypeAndZone.keySet()) {
				for (String mode : trafficVolumePerTypeAndZone.get(zoneID).keySet()) {
					List<String> row = new ArrayList<>();
					row.add(zoneID);
					row.add(mode);
					Integer count = 1;
					while (count < 6) {
						row.add(String.valueOf(Math
								.round(trafficVolumePerTypeAndZone.get(zoneID).get(mode).getDouble(count) * sample)));
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
	static void loadInputParamters(Path inputDataDirectory) throws IOException {

		// Read generation rates for start potentials
		Path generationRatesStartPath = inputDataDirectory.resolve("parameters").resolve("generationRates_start.csv");
		generationRatesStart = readGenerationRates(generationRatesStartPath);
		log.info("Read generations rates (start)");

		// Read generation rates for stop potentials
		Path generationRatesStopPath = inputDataDirectory.resolve("parameters").resolve("generationRates_stop.csv");
		generationRatesStop = readGenerationRates(generationRatesStopPath);
		log.info("Read generations rates (stop)");

		// read commitment rates for start potentials
		Path commitmentRatesStartPath = inputDataDirectory.resolve("parameters").resolve("commitmentRates_start.csv");
		commitmentRatesStart = readCommitmentRates(commitmentRatesStartPath);
		log.info("Read commitment rates (start)");

		// read commitment rates for stop potentials
		Path commitmentRatesStopPath = inputDataDirectory.resolve("parameters").resolve("commitmentRates_stop.csv");
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
	private static HashMap<Integer, HashMap<String, Double>> readGenerationRates(Path generationRatesPath) throws IOException {
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
	 * Reads the data for the commitment rates. For modes: pt = public transport; it
	 * = individual traffic; op = elective (wahlfrei)
	 * 
	 * @param generationRatesPath
	 * @return
	 * @throws IOException
	 */
	private static HashMap<String, HashMap<String, Double>> readCommitmentRates(Path commitmentRatesPath) throws IOException {
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
