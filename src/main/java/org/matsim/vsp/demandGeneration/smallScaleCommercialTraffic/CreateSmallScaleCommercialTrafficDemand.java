package org.matsim.vsp.demandGeneration.smallScaleCommercialTraffic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
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
 * 
 * Author: Ricardo Ewert
 */
@CommandLine.Command(name = "generate-german-freight-trips", description = "Generate german wide freight population", showDefaultValues = true)
public class CreateSmallScaleCommercialTrafficDemand implements Callable<Integer> {

	private static final Logger log = LogManager.getLogger(CreateSmallScaleCommercialTrafficDemand.class);
	private static final Joiner JOIN = Joiner.on("\t");

	@CommandLine.Parameters(arity = "1", paramLabel = "INPUT", description = "Path to the freight data directory", defaultValue = "../public-svn/matsim/scenarios/countries/de/small-scale-commercial-traffic/input")
	private Path rawDataDirectory;

//	    @CommandLine.Option(names = "--network", description = "Path to desired network file", required = true)
//	    private Path networkPath;

	@CommandLine.Option(names = "--sample", defaultValue = "1", description = "Scaling factor of the freight traffic (0, 1)", required = true)
	private double sample;

	@CommandLine.Option(names = "--output", description = "Path to output population", required = true, defaultValue = "output/SmallScaleBusiness/")
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

		Path shapeFileZonePath = rawDataDirectory.resolve("shp").resolve("districts")
				.resolve("bezirksgrenzen_Berlin.shp");
//		Path shapeFileZonePath = rawDataDirectory.resolve("shp").resolve("districts")
//				.resolve("verkehrszellen_Berlin.shp");

		if (!Files.exists(shapeFileZonePath)) {
			log.error("Required distrcits shape file {} not found", shapeFileZonePath);
		}

		// Load config, scenario and network
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem(crs.getInputCRS()); // "EPSG:4326"
		Scenario scenario = ScenarioUtils.loadScenario(config);

		HashMap<String, Object2DoubleMap<String>> resultingDataPerZone = createInputDataDistribution(shapeFileZonePath);
		
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
	@SuppressWarnings("deprecation")
	private HashMap<String, Object2DoubleMap<String>> createInputDataDistribution(Path shapeFileZonePath)
			throws IOException, MalformedURLException {

		HashMap<String, Object2DoubleMap<String>> resultingDataPerZone = new HashMap<String, Object2DoubleMap<String>>();

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
					for (int n = 1; n<parse.getHeaderMap().size(); n++) {
						resultingDataPerZone.get(zoneID).mergeDouble(parse.getHeaderNames().get(n), Double.valueOf(record.get(n)), Double::sum);
					}
				}
			}
			break;

		default:

			HashMap<String, Object2DoubleMap<String>> landuseCategoriesPerZone = new HashMap<String, Object2DoubleMap<String>>();
			createLanduseDistribution(shapeFileZonePath, landuseCategoriesPerZone);

			HashMap<String, HashMap<String, Integer>> investigationAreaData = new HashMap<String, HashMap<String, Integer>>();
			readAreaData(investigationAreaData);

			createResultingDataForLanduseInZones(landuseCategoriesPerZone, investigationAreaData, resultingDataPerZone);

			writeResultOfDataDistribution(resultingDataPerZone);
		}
		return resultingDataPerZone;
	}

	/**
	 * Writes a csv file with result of the distribution per zone of the input data.
	 * 
	 * @param resultingDataPerZone
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private void writeResultOfDataDistribution(HashMap<String, Object2DoubleMap<String>> resultingDataPerZone)
			throws IOException, MalformedURLException {

		Path outputFile = rawDataDirectory.resolve("dataDistributionPerZone.csv");

		if (Files.exists(outputFile)) {
			Path oldFile = Path.of(outputFile.toString().replace(".csv", "_old.csv"));
			log.warn("The result of data distribution already exists. The existing data will be moved to: " + oldFile);
			Files.deleteIfExists(oldFile);
			Files.move(outputFile, oldFile, StandardCopyOption.REPLACE_EXISTING);
		}

		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile.toUri().toURL(), StandardCharsets.UTF_8, true);
		try {
			String[] header = new String[] { "areaID", "Inhabitants", "Employee", "Employee Primary Sector",
					"Employee Construction", "Employee Secondary Sector Rest", "Employee Retail",
					"Employee Traffic/Parcels", "Employee Tertiär Rest" };
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
	 * @param landuseCategoriesPerZone
	 * @param investigationAreaData
	 * @param resultingDataPerZone
	 */
	private void createResultingDataForLanduseInZones(
			HashMap<String, Object2DoubleMap<String>> landuseCategoriesPerZone,
			HashMap<String, HashMap<String, Integer>> investigationAreaData,
			HashMap<String, Object2DoubleMap<String>> resultingDataPerZone) {
		HashMap<String, List<String>> landuseCategoriesAndDataConnection = new HashMap<String, List<String>>();
		landuseCategoriesAndDataConnection.put("Inhabitants", List.of("residential", "apartments", "dormitory",
				"dwelling_house", "house", "retirement_home", "semidetached_house"));
		landuseCategoriesAndDataConnection.put("Employee Primary Sector",
				List.of("farmyard", "farmland", "farm", "farm_auxiliary", "greenhouse"));
		landuseCategoriesAndDataConnection.put("Employee Construction", List.of("construction"));
		landuseCategoriesAndDataConnection.put("Employee Secondary Sector Rest",
				List.of("industrial", "factory", "manufacture"));
		landuseCategoriesAndDataConnection.put("Employee Retail",
				List.of("retail", "kiosk", "mall", "shop", "supermarket"));
		landuseCategoriesAndDataConnection.put("Employee Traffic/Parcels",
				List.of("commercial", "post_office", "storage", "storage_tank", "warehouse"));
		landuseCategoriesAndDataConnection.put("Employee Tertiär Rest",
				List.of("commercial", "embassy", "foundation", "government", "office", "townhall"));
		landuseCategoriesAndDataConnection.put("Employee",
				landuseCategoriesAndDataConnection.get("Employee Primary Sector"));
		landuseCategoriesAndDataConnection.put("Employee",
				landuseCategoriesAndDataConnection.get("Employee Construction"));
		landuseCategoriesAndDataConnection.put("Employee",
				landuseCategoriesAndDataConnection.get("Employee Secondary Sector Rest"));
		landuseCategoriesAndDataConnection.put("Employee", landuseCategoriesAndDataConnection.get("Employee Retail"));
		landuseCategoriesAndDataConnection.put("Employee",
				landuseCategoriesAndDataConnection.get("Employee Traffic/Parcels"));
		landuseCategoriesAndDataConnection.put("Employee",
				landuseCategoriesAndDataConnection.get("Employee Tertiär Rest"));

		for (String zoneID : landuseCategoriesPerZone.keySet()) {
			resultingDataPerZone.put(zoneID, new Object2DoubleOpenHashMap<>());
			for (String category : landuseCategoriesPerZone.get(zoneID).keySet()) {
				for (String categoryNameData : landuseCategoriesAndDataConnection.keySet()) {
					if (landuseCategoriesAndDataConnection.get(categoryNameData).contains(category))
						resultingDataPerZone.get(zoneID)
								.mergeDouble(categoryNameData,
										(landuseCategoriesPerZone.get(zoneID).getDouble(category))
												/ landuseCategoriesAndDataConnection.get(categoryNameData).size(),
										Double::sum);
				}
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
		// Check can be deleted
		Object2DoubleMap<String> checkSums = new Object2DoubleOpenHashMap<>();
		for (Object2DoubleMap<String> sumPerZone : resultingDataPerZone.values()) {
			for (String category : sumPerZone.keySet()) {
				checkSums.mergeDouble(category, sumPerZone.getDouble(category), Double::sum);
			}
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
		Path shapeFileBuildingsPath = rawDataDirectory.resolve("shp").resolve("landuse")
				.resolve("gis_osm_buildings_a_free_1.shp");
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

		double totalSquareMeters = 0;
		Object2DoubleMap<String> totalSquareMetersPerCategory = new Object2DoubleOpenHashMap<>();
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
							double area = (double) ((long) singleBuildingFeature.getAttribute("area"));
							landuseCategoriesPerZone.get(singleZone.getAttribute("gml_id")).mergeDouble(singleCategory,
									area, Double::sum);
							totalSquareMetersPerCategory.mergeDouble(singleCategory, area, Double::sum);
							totalSquareMeters = totalSquareMeters + area;
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
						totalSquareMetersPerCategory.mergeDouble((String) singleLanduseFeature.getAttribute("fclass"),
								(double) singleLanduseFeature.getAttribute("area"), Double::sum);
						totalSquareMeters = totalSquareMeters + (double) singleLanduseFeature.getAttribute("area");
						continue;
					}
				}
			}
			break;

		default:
			throw new RuntimeException("No possible option for the use of OSM data selected");
		}

		for (String landuseCategoriesForSingleZone : landuseCategoriesPerZone.keySet()) {
			for (String category : landuseCategoriesPerZone.get(landuseCategoriesForSingleZone).keySet()) {
				landuseCategoriesPerZone.get(landuseCategoriesForSingleZone).replace(category,
						landuseCategoriesPerZone.get(landuseCategoriesForSingleZone).getDouble(category),
						landuseCategoriesPerZone.get(landuseCategoriesForSingleZone).getDouble(category)
								/ totalSquareMetersPerCategory.getDouble(category));
			}
		}

		// To Delete
		Object2DoubleMap<String> checkPercentages = new Object2DoubleOpenHashMap<>();
		for (String landuseCategoriesForSingleZone : landuseCategoriesPerZone.keySet()) {
			for (String category : landuseCategoriesPerZone.get(landuseCategoriesForSingleZone).keySet()) {
				checkPercentages.mergeDouble(category,
						landuseCategoriesPerZone.get(landuseCategoriesForSingleZone).getDouble(category), Double::sum);
			}
		}
		System.out.println("Berlin total: " + totalSquareMeters / 1000000);
	}
}
