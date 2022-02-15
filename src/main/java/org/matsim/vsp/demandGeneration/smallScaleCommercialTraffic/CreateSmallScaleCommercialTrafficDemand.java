package org.matsim.vsp.demandGeneration.smallScaleCommercialTraffic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.LanduseOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;
import com.google.common.base.Joiner;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import picocli.CommandLine;

/**
 * @author Ricardo Ewert
 *
 */
@CommandLine.Command(name = "generate-business-passenger-traffic", description = "Generate business passenger traffic model", showDefaultValues = true)
public class CreateSmallScaleCommercialTrafficDemand implements Callable<Integer> {

	private static final Logger log = LogManager.getLogger(CreateSmallScaleCommercialTrafficDemand.class);
	private static final Joiner JOIN = Joiner.on("\t");
	private static HashMap<Integer, HashMap<String, Double>> generationRatesStart = new HashMap<Integer, HashMap<String, Double>>();
	private static HashMap<Integer, HashMap<String, Double>> generationRatesStop = new HashMap<Integer, HashMap<String, Double>>();
	private static HashMap<String, HashMap<String, Double>> commitmentRatesStart = new HashMap<String, HashMap<String, Double>>();
	private static HashMap<String, HashMap<String, Double>> commitmentRatesStop = new HashMap<String, HashMap<String, Double>>();

	@CommandLine.Parameters(arity = "1", paramLabel = "INPUT", description = "Path to the freight data directory", defaultValue = "../public-svn/matsim/scenarios/countries/de/small-scale-commercial-traffic/input")
	private Path rawDataDirectory;

//	    @CommandLine.Option(names = "--network", description = "Path to desired network file", required = true)
//	    private Path networkPath;

	@CommandLine.Option(names = "--sample", defaultValue = "1", description = "Scaling factor of the freight traffic (0, 1)", required = true)
	private double sample;

	@CommandLine.Option(names = "--output", description = "Path to output population", required = true, defaultValue = "output/BusinessPassengerTraffic/")
	private Path output;

	@CommandLine.Mixin
	private LanduseOptions landuse = new LanduseOptions();

	@CommandLine.Mixin
	private CrsOptions crs = new CrsOptions();

	private enum landuseConfiguration {
		useOnlyOSMLanduse, useOSMBuildingsAndLanduse, useExistingDataDistribution
	}

	@CommandLine.Option(names = "--landuseConfiguration", defaultValue = "useExistingDataDistribution", description = "Set option of used OSM data. Options: useOnlyOSMLanduse, useOSMBuildingsAndLanduse")
	private landuseConfiguration usedLanduseConfiguration;
//	    private final SplittableRandom rnd = new SplittableRandom(4711);

	public static void main(String[] args) {
		System.exit(new CommandLine(new CreateSmallScaleCommercialTrafficDemand()).execute(args));
	}

	@Override
	public Integer call() throws Exception {

//		Path shapeFileZonePath = rawDataDirectory.resolve("shp").resolve("districts")
//				.resolve("bezirksgrenzen_Berlin.shp");
		Path shapeFileZonePath = rawDataDirectory.resolve("shp").resolve("districts")
				.resolve("verkehrszellen_Berlin.shp");

		if (!Files.exists(shapeFileZonePath)) {
			log.error("Required distrcits shape file {} not found", shapeFileZonePath);
		}
		output = output.resolve(java.time.LocalDate.now().toString() + "_" + java.time.LocalTime.now().toSecondOfDay());

		// Load config, scenario and network
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem(crs.getInputCRS()); // "EPSG:4326"
		config.controler().setOutputDirectory(output.toString());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		new OutputDirectoryHierarchy(config.controler().getOutputDirectory(), config.controler().getRunId(),
				config.controler().getOverwriteFileSetting(), ControlerConfigGroup.CompressionType.gzip);
		new File(output.resolve("caculatedData").toString()).mkdir();

		Scenario scenario = ScenarioUtils.loadScenario(config);

		HashMap<String, Object2DoubleMap<String>> resultingDataPerZone = createInputDataDistribution(shapeFileZonePath);

		readInputParamters();

		HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerTypeAndZone_start = createTrafficVolume_start(
				resultingDataPerZone);
		HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerTypeAndZone_stop = createTrafficVolume_stop(
				resultingDataPerZone);

		return 0;

	}

	/**
	 * Creates a distribution of the given input data for each zone based on the
	 * used OSM data.
	 * 
	 * @param shapeFileZonePath
	 * @return
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private HashMap<String, Object2DoubleMap<String>> createInputDataDistribution(Path shapeFileZonePath)
			throws IOException, MalformedURLException {

		HashMap<String, Object2DoubleMap<String>> resultingDataPerZone = new HashMap<String, Object2DoubleMap<String>>();
		Path outputFileInOutputFolder = output.resolve("caculatedData").resolve("dataDistributionPerZone.csv");
		switch (usedLanduseConfiguration) {
		case useExistingDataDistribution:
			Path existingDataDistribution = rawDataDirectory.resolve("dataDistributionPerZone.csv");

			if (!Files.exists(existingDataDistribution)) {
				log.error("Required data per zone file {} not found", existingDataDistribution);
			}

			try (BufferedReader reader = IOUtils.getBufferedReader(existingDataDistribution.toString())) {
				CSVParser parse = CSVFormat.DEFAULT.withDelimiter('\t').withFirstRecordAsHeader().parse(reader);

				for (CSVRecord record : parse) {
					String zoneID = record.get("areaID");
					resultingDataPerZone.put(zoneID, new Object2DoubleOpenHashMap<>());
					for (int n = 1; n < parse.getHeaderMap().size(); n++) {
						resultingDataPerZone.get(zoneID).mergeDouble(parse.getHeaderNames().get(n),
								Double.valueOf(record.get(n)), Double::sum);
					}
				}
			}
			log.info("Data distribution for " + resultingDataPerZone.size() + " zones was imported from ",
					existingDataDistribution);
			Files.copy(existingDataDistribution, outputFileInOutputFolder, StandardCopyOption.COPY_ATTRIBUTES);
			break;

		default:

			log.info("New analyze for data distribution is started. The used method is: " + usedLanduseConfiguration);
			HashMap<String, Object2DoubleMap<String>> landuseCategoriesPerZone = new HashMap<String, Object2DoubleMap<String>>();
			createLanduseDistribution(shapeFileZonePath, landuseCategoriesPerZone);

			HashMap<String, HashMap<String, Integer>> investigationAreaData = new HashMap<String, HashMap<String, Integer>>();
			readAreaData(investigationAreaData);

			createResultingDataForLanduseInZones(landuseCategoriesPerZone, investigationAreaData, resultingDataPerZone);

			writeResultOfDataDistribution(resultingDataPerZone, outputFileInOutputFolder);
		}

		return resultingDataPerZone;
	}

	private void readInputParamters() throws IOException {

		// Read generation rates for start potentials
		Path generationRatesStartPath = rawDataDirectory.resolve("parameters").resolve("generationRates_start.csv");
		generationRatesStart = readGenerationRates(generationRatesStartPath);
		log.info("Read generations rates (start)");

		// Read generation rates for stop potentials
		Path generationRatesStopPath = rawDataDirectory.resolve("parameters").resolve("generationRates_stop.csv");
		generationRatesStop = readGenerationRates(generationRatesStopPath);
		log.info("Read generations rates (stop)");

		// read commitment rates for start potentials
		Path commitmentRatesStartPath = rawDataDirectory.resolve("parameters").resolve("commitmentRates_start.csv");
		commitmentRatesStart = readCommitmentRates(commitmentRatesStartPath);
		log.info("Read commitment rates (start)");

		// read commitment rates for stop potentials
		Path commitmentRatesStopPath = rawDataDirectory.resolve("parameters").resolve("commitmentRates_stop.csv");
		commitmentRatesStop = readCommitmentRates(commitmentRatesStopPath);
		log.info("Read commitment rates (stop)");
	}

	/**
	 * Creates the traffic volume (start) for each zone separated in the 3 modes and
	 * the 5 purposes.
	 * 
	 * @param resultingDataPerZone
	 * @return
	 * @throws MalformedURLException
	 */
	private HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> createTrafficVolume_start(
			HashMap<String, Object2DoubleMap<String>> resultingDataPerZone) throws MalformedURLException {

		HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerTypeAndZone_start = new HashMap<String, HashMap<String, Object2DoubleMap<Integer>>>();
		calculateTrafficVolumePerZone(trafficVolumePerTypeAndZone_start, resultingDataPerZone, "start");
		Path outputFileStart = output.resolve("caculatedData").resolve("TrafficVolume_startPerZone.csv");
		writeCSVWithCategoryHeader(resultingDataPerZone, outputFileStart);
		log.info("Write traffic volume for start trips per zone in CSV: " + outputFileStart);
		return trafficVolumePerTypeAndZone_start;
	}

	/**
	 * Creates the traffic volume (stop) for each zone separated in the 3 modes and
	 * the 5 purposes.
	 * 
	 * @param resultingDataPerZone
	 * @return
	 * @throws MalformedURLException
	 */
	private HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> createTrafficVolume_stop(
			HashMap<String, Object2DoubleMap<String>> resultingDataPerZone) throws MalformedURLException {

		HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerTypeAndZone_stop = new HashMap<String, HashMap<String, Object2DoubleMap<Integer>>>();
		calculateTrafficVolumePerZone(trafficVolumePerTypeAndZone_stop, resultingDataPerZone, "stop");
		Path outputFileStop = output.resolve("caculatedData").resolve("TrafficVolume_stopPerZone.csv");
		writeCSVWithCategoryHeader(resultingDataPerZone, outputFileStop);
		log.info("Write traffic volume for stop trips per zone in CSV: " + outputFileStop);
		return trafficVolumePerTypeAndZone_stop;
	}

	/**
	 * Method create the percentage for each land use category in each zone based on
	 * the sum of this category in all zones of the zone shape file
	 * 
	 * @param shapeFileLandusePath     Path to shape file with the land use
	 *                                 information
	 * @param shapeFileZonesPath       Path to shape file with the zone information
	 * @param landuseCategoriesPerZone
	 * 
	 */
	private void createLanduseDistribution(Path shapeFileZonesPath,
			HashMap<String, Object2DoubleMap<String>> landuseCategoriesPerZone) {

		List<String> neededLanduseCategories = List.of("residential", "industrial", "commercial", "retail", "farmyard",
				"farmland", "construction");
		Path shapeFileLandusePath = rawDataDirectory.resolve("shp").resolve("landuse")
				.resolve("gis_osm_landuse_a_free_1.shp");
		// Path shapeFileBuildingsPath =
		// rawDataDirectory.resolve("shp").resolve("landuse")
		// .resolve("gis_osm_buildings_a_free_1.shp");
		Path shapeFileBuildingsPath = rawDataDirectory.resolve("shp").resolve("landuse")
				.resolve("allBuildingsWithLevels.shp");
//		Path shapeFileBuildingsPath = rawDataDirectory.resolve("shp").resolve("landuse").resolve("buildingSample.shp");

		if (!Files.exists(shapeFileLandusePath)) {
			log.error("Required landuse shape file {} not found", shapeFileLandusePath);
		}
		if (!Files.exists(shapeFileBuildingsPath)) {
			log.error("Required OSM buildings shape file {} not found", shapeFileBuildingsPath);
		}
		ShpOptions shpLanduse = new ShpOptions(shapeFileLandusePath, null, StandardCharsets.UTF_8);
		ShpOptions shpZones = new ShpOptions(shapeFileZonesPath, null, StandardCharsets.UTF_8);

		List<SimpleFeature> landuseFeatures = shpLanduse.readFeatures();
		List<SimpleFeature> zonesFeatures = shpZones.readFeatures();

		for (SimpleFeature districId : zonesFeatures) {
			Object2DoubleMap<String> landusePerCategory = new Object2DoubleOpenHashMap<>();
			landuseCategoriesPerZone.put((String) districId.getAttribute("gml_id"), landusePerCategory);
		}

		int countOSMObjects = 0;

		switch (usedLanduseConfiguration) {
		case useOSMBuildingsAndLanduse:

			List<String> neededBuildingCategories = List.of("residential", "apartments", "dormitory", "dwelling_house",
					"house", "retirement_home", "semidetached_house", "farm", "farm_auxiliary", "greenhouse",
					"construction", "industrial", "factory", "manufacture", "retail", "kiosk", "mall", "shop",
					"supermarket", "commercial", "post_office", "storage", "storage_tank", "warehouse", "embassy",
					"foundation", "government", "office", "townhall");
			ShpOptions shpBuildings = new ShpOptions(shapeFileBuildingsPath, null, StandardCharsets.UTF_8);
			List<SimpleFeature> buildingsFeatures = shpBuildings.readFeatures();
			for (SimpleFeature singleBuildingFeature : buildingsFeatures) {
				countOSMObjects++;
				if (countOSMObjects % 10000 == 0)
					log.info("Investigate Building " + countOSMObjects + " of " + buildingsFeatures.size()
							+ " buildings: " + Math.round((double) countOSMObjects / buildingsFeatures.size() * 100)
							+ " %");
				String buildingType = String.valueOf(singleBuildingFeature.getAttribute("type"));

				List<String> buildingCategories = new ArrayList<String>();
				Point centroidPointOfBuildingPolygon = null;
				boolean neededType = false;

				if (!buildingType.equals("")) {
					for (String categoryName : neededBuildingCategories) {
						if (buildingType.contains(categoryName)) {
							buildingCategories.add(categoryName);
							neededType = true;
						}
					}
				} else {
					centroidPointOfBuildingPolygon = ((Geometry) singleBuildingFeature.getDefaultGeometry())
							.getCentroid();
					for (SimpleFeature singleLanduseFeature : landuseFeatures) {
						if (!neededLanduseCategories.contains((String) singleLanduseFeature.getAttribute("fclass")))
							continue;
						if (!neededType && ((Geometry) singleLanduseFeature.getDefaultGeometry())
								.contains(centroidPointOfBuildingPolygon)) {
							buildingCategories.add((String) singleLanduseFeature.getAttribute("fclass"));
							neededType = true;
						}
					}
				}
				if (!neededType)
					continue;

				if (centroidPointOfBuildingPolygon == null)
					centroidPointOfBuildingPolygon = ((Geometry) singleBuildingFeature.getDefaultGeometry())
							.getCentroid();

				for (SimpleFeature singleZone : zonesFeatures) {
					if (((Geometry) singleZone.getDefaultGeometry()).contains(centroidPointOfBuildingPolygon)) {
						for (String singleCategory : buildingCategories) {
							int buildingLevels = 0;
							if (singleBuildingFeature.getAttribute("levels") == null)
								buildingLevels = 1;
							else
								buildingLevels = (int) (long) singleBuildingFeature.getAttribute("levels");
							double area = (double) ((long) singleBuildingFeature.getAttribute("area")) * buildingLevels;

							landuseCategoriesPerZone.get(singleZone.getAttribute("gml_id")).mergeDouble(singleCategory,
									area, Double::sum);
						}
						break;
					}
				}
			}
			break;
		case useOnlyOSMLanduse:
			for (SimpleFeature singleLanduseFeature : landuseFeatures) {
				if (!neededLanduseCategories.contains((String) singleLanduseFeature.getAttribute("fclass")))
					continue;
				Point centroidPointOfLandusePolygon = ((Geometry) singleLanduseFeature.getDefaultGeometry())
						.getCentroid();

				for (SimpleFeature singleZone : zonesFeatures) {
					if (((Geometry) singleZone.getDefaultGeometry()).contains(centroidPointOfLandusePolygon)) {
						landuseCategoriesPerZone.get(singleZone.getAttribute("gml_id")).mergeDouble(
								(String) singleLanduseFeature.getAttribute("fclass"),
								(double) singleLanduseFeature.getAttribute("area"), Double::sum);
						continue;
					}
				}
			}
			break;

		default:
			throw new RuntimeException("No possible option for the use of OSM data selected");
		}
	}

	/**
	 * Reads the input data for certain areas from the csv file.
	 * 
	 * @param areaDataPath
	 * @param areaData
	 * @throws IOException
	 */
	private void readAreaData(HashMap<String, HashMap<String, Integer>> areaData) throws IOException {

		Path areaDataPath = rawDataDirectory.resolve("investigationAreaData.csv");
		if (!Files.exists(areaDataPath)) {
			log.error("Required input data file {} not found", areaDataPath);
		}
		try (CSVParser parser = new CSVParser(Files.newBufferedReader(areaDataPath),
				CSVFormat.TDF.withFirstRecordAsHeader())) {

			for (CSVRecord record : parser) {
				HashMap<String, Integer> lookUpTable = new HashMap<>();
				for (String csvRecord : parser.getHeaderMap().keySet()) {
					if (parser.getHeaderMap().get(csvRecord) > 0)
						lookUpTable.put(csvRecord, Integer.valueOf(record.get(csvRecord)));
				}
				areaData.put(record.get("Area"), lookUpTable);
			}
		}
	}

	/**
	 * @param landuseCategoriesPerZone
	 * @param investigationAreaData
	 * @param resultingDataPerZone
	 */
	private void createResultingDataForLanduseInZones(
			HashMap<String, Object2DoubleMap<String>> landuseCategoriesPerZone,
			HashMap<String, HashMap<String, Integer>> investigationAreaData,
			HashMap<String, Object2DoubleMap<String>> resultingDataPerZone) {
		
		HashMap<String, ArrayList<String>> landuseCategoriesAndDataConnection = new HashMap<String, ArrayList<String>>();
		landuseCategoriesAndDataConnection.put("Inhabitants", new ArrayList<String>(Arrays.asList("residential",
				"apartments", "dormitory", "dwelling_house", "house", "retirement_home", "semidetached_house")));
		landuseCategoriesAndDataConnection.put("Employee Primary Sector",
				new ArrayList<String>(Arrays.asList("farmyard", "farmland", "farm", "farm_auxiliary", "greenhouse")));
		landuseCategoriesAndDataConnection.put("Employee Construction",
				new ArrayList<String>(Arrays.asList("construction")));
		landuseCategoriesAndDataConnection.put("Employee Secondary Sector Rest",
				new ArrayList<String>(Arrays.asList("industrial", "factory", "manufacture")));
		landuseCategoriesAndDataConnection.put("Employee Retail",
				new ArrayList<String>(Arrays.asList("retail", "kiosk", "mall", "shop", "supermarket")));
		landuseCategoriesAndDataConnection.put("Employee Traffic/Parcels", new ArrayList<String>(
				Arrays.asList("commercial", "post_office", "storage", "storage_tank", "warehouse")));
		landuseCategoriesAndDataConnection.put("Employee Tertiary Sector Rest", new ArrayList<String>(
				Arrays.asList("commercial", "embassy", "foundation", "government", "office", "townhall")));
		landuseCategoriesAndDataConnection.put("Employee", new ArrayList<String>());
		landuseCategoriesAndDataConnection.get("Employee")
				.addAll(landuseCategoriesAndDataConnection.get("Employee Primary Sector"));
		landuseCategoriesAndDataConnection.get("Employee")
				.addAll(landuseCategoriesAndDataConnection.get("Employee Construction"));
		landuseCategoriesAndDataConnection.get("Employee")
				.addAll(landuseCategoriesAndDataConnection.get("Employee Secondary Sector Rest"));
		landuseCategoriesAndDataConnection.get("Employee")
				.addAll(landuseCategoriesAndDataConnection.get("Employee Retail"));
		landuseCategoriesAndDataConnection.get("Employee")
				.addAll(landuseCategoriesAndDataConnection.get("Employee Traffic/Parcels"));
		landuseCategoriesAndDataConnection.get("Employee")
				.addAll(landuseCategoriesAndDataConnection.get("Employee Tertiary Sector Rest"));

		Object2DoubleMap<String> totalSquareMetersPerCategory = new Object2DoubleOpenHashMap<>();

		for (String zoneID : landuseCategoriesPerZone.keySet()) {
			resultingDataPerZone.put(zoneID, new Object2DoubleOpenHashMap<>());
			for (String category : landuseCategoriesPerZone.get(zoneID).keySet()) {
				for (String categoryNameData : landuseCategoriesAndDataConnection.keySet()) {
					if (landuseCategoriesAndDataConnection.get(categoryNameData).contains(category)) {
						double addiotionalArea = landuseCategoriesPerZone.get(zoneID).getDouble(category);
						resultingDataPerZone.get(zoneID).mergeDouble(categoryNameData, addiotionalArea, Double::sum);
						totalSquareMetersPerCategory.mergeDouble(categoryNameData, addiotionalArea, Double::sum);
					}
				}
			}
		}

		for (String zoneID : resultingDataPerZone.keySet()) {
			for (String category : resultingDataPerZone.get(zoneID).keySet()) {
				resultingDataPerZone.get(zoneID).replace(category, resultingDataPerZone.get(zoneID).getDouble(category),
						resultingDataPerZone.get(zoneID).getDouble(category)
								/ totalSquareMetersPerCategory.getDouble(category));
			}
		}

		Object2DoubleMap<String> checkPercentages = new Object2DoubleOpenHashMap<>();

		for (String landuseCategoriesForSingleZone : resultingDataPerZone.keySet()) {
			for (String category : resultingDataPerZone.get(landuseCategoriesForSingleZone).keySet()) {
				checkPercentages.mergeDouble(category,
						resultingDataPerZone.get(landuseCategoriesForSingleZone).getDouble(category), Double::sum);
			}
		}

		for (String zoneId : resultingDataPerZone.keySet()) {
			for (String categoryNameData : resultingDataPerZone.get(zoneId).keySet()) {
				double percentageValue = resultingDataPerZone.get(zoneId).getDouble(categoryNameData);
				int resultingNumberPerCategory = (int) Math
						.round(percentageValue * investigationAreaData.get("Berlin").get(categoryNameData));
				resultingDataPerZone.get(zoneId).replace(categoryNameData, percentageValue, resultingNumberPerCategory);
			}
		}
	}

	/**
	 * Writes a csv file with result of the distribution per zone of the input data.
	 * 
	 * @param resultingDataPerZone
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private void writeResultOfDataDistribution(HashMap<String, Object2DoubleMap<String>> resultingDataPerZone,
			Path outputFileInOutputFolder) throws IOException, MalformedURLException {

		Path outputFileInInputFolder = rawDataDirectory.resolve("dataDistributionPerZone.csv");

		if (Files.exists(outputFileInInputFolder)) {
			Path oldFile = Path.of(outputFileInInputFolder.toString().replace(".csv", "_old.csv"));
			log.warn("The result of data distribution already exists. The existing data will be moved to: " + oldFile);
			Files.deleteIfExists(oldFile);
			Files.move(outputFileInInputFolder, oldFile, StandardCopyOption.REPLACE_EXISTING);
		}

		writeCSVWithCategoryHeader(resultingDataPerZone, outputFileInInputFolder);
		log.info("The data distribution is finished and written to: " + outputFileInInputFolder);
		Files.copy(outputFileInInputFolder, outputFileInOutputFolder, StandardCopyOption.COPY_ATTRIBUTES);
	}

	/**
	 * @param resultingDataPerZone
	 * @param outputFileInInputFolder
	 * @throws MalformedURLException
	 */
	private void writeCSVWithCategoryHeader(HashMap<String, Object2DoubleMap<String>> resultingDataPerZone,
			Path outputFileInInputFolder) throws MalformedURLException {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFileInInputFolder.toUri().toURL(),
				StandardCharsets.UTF_8, true);
		try {
			String[] header = new String[] { "areaID", "Inhabitants", "Employee", "Employee Primary Sector",
					"Employee Construction", "Employee Secondary Sector Rest", "Employee Retail",
					"Employee Traffic/Parcels", "Employee Tertiary Sector Rest" };
			JOIN.appendTo(writer, header);
			writer.write("\n");
			for (String zone : resultingDataPerZone.keySet()) {
				List<String> row = new ArrayList<>();
				row.add(zone);
				for (String category : header) {
					if (!category.equals("areaID")) {
						row.add(String.valueOf((int) resultingDataPerZone.get(zone).getDouble(category)));
					}
				}
				JOIN.appendTo(writer, row);
				writer.write("\n");
			}

			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reads the data for the generation rates.
	 * 
	 * @param generationRatesPath
	 * @return
	 * @throws IOException
	 */
	private HashMap<Integer, HashMap<String, Double>> readGenerationRates(Path generationRatesPath) throws IOException {
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
	private HashMap<String, HashMap<String, Double>> readCommitmentRates(Path commitmentRatesPath) throws IOException {
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

	/**
	 * Calculates the traffic volume for each zone and purpose.
	 * 
	 * @param trafficVolumePerZone
	 * @param resultingDataPerZone
	 * @param volumeType
	 * @return
	 */
	private HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> calculateTrafficVolumePerZone(
			HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerZone,
			HashMap<String, Object2DoubleMap<String>> resultingDataPerZone, String volumeType) {

		HashMap<Integer, HashMap<String, Double>> generationRates = new HashMap<Integer, HashMap<String, Double>>();
		HashMap<String, HashMap<String, Double>> commitmentRates = new HashMap<String, HashMap<String, Double>>();

		ArrayList<String> modes = new ArrayList<String>(Arrays.asList("pt", "it", "op"));

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
					for (String category : resultingDataPerZone.get(zoneId).keySet()) {
						double generationFactor = generationRates.get(purpose).get(category);
						double commitmentFactor = commitmentRates.get(purpose + "_" + mode).get(category);
						double newValue = resultingDataPerZone.get(zoneId).getDouble(category) * generationFactor
								* commitmentFactor;
						trafficValuesPerPurpose.merge(purpose, newValue, Double::sum);
					}
				}
				valuesForZone.put(mode, trafficValuesPerPurpose);
			}
			trafficVolumePerZone.put(zoneId, valuesForZone);
		}
		return trafficVolumePerZone;
	}
}
