package org.matsim.vsp.demandGeneration.smallScaleCommercialTraffic;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.opengis.geometry.DirectPosition;

import com.google.common.base.Joiner;
import com.opencsv.CSVWriter;

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

	@CommandLine.Option(names = "--output", description = "Path to output population", required = true, defaultValue = "output/SmallScaleBusiness/test.csv")
	private Path output;

	@CommandLine.Mixin
	private LanduseOptions landuse = new LanduseOptions();

	@CommandLine.Mixin
	private CrsOptions crs = new CrsOptions();

//	    private final SplittableRandom rnd = new SplittableRandom(4711);

	public static void main(String[] args) {
		System.exit(new CommandLine(new CreateSmallScaleCommercialTrafficDemand()).execute(args));
	}

	@Override
	public Integer call() throws Exception {

		Path shapeFileLandusePath = rawDataDirectory.resolve("shp").resolve("landuse")
				.resolve("gis_osm_landuse_a_free_1.shp");
		Path shapeFileBuildingsPath = rawDataDirectory.resolve("shp").resolve("landuse")
				.resolve("gis_osm_buildings_a_free_1");
//		Path shapeFileZonePath = rawDataDirectory.resolve("shp").resolve("districts")
//				.resolve("bezirksgrenzen_Berlin.shp");
		Path shapeFileZonePath = rawDataDirectory.resolve("shp").resolve("districts")
				.resolve("verkehrszellen_Berlin.shp");
		Path areaDataPath = rawDataDirectory.resolve("investigationAreaData.csv");

		if (!Files.exists(shapeFileLandusePath)) {
			log.error("Required landuse shape file {} not found", shapeFileLandusePath);
		}

		if (!Files.exists(shapeFileZonePath)) {
			log.error("Required distrcits shape file {} not found", shapeFileZonePath);
		}

		// Load config, scenario and network
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem(crs.getInputCRS()); // "EPSG:4326"
		Scenario scenario = ScenarioUtils.loadScenario(config);

		// TODO benötigte categorien filtern
		HashMap<String, Object2DoubleMap<String>> landuseCategoriesPerZone = new HashMap<String, Object2DoubleMap<String>>();
		createLanduseDistribution(shapeFileLandusePath, shapeFileZonePath, shapeFileBuildingsPath, landuseCategoriesPerZone);

		HashMap<String, HashMap<String, Integer>> investigationAreaData = new HashMap<String, HashMap<String, Integer>>();
		readAreaData(areaDataPath, investigationAreaData);

		HashMap<String, Object2DoubleMap<String>> resultingDataPerZone = new HashMap<String, Object2DoubleMap<String>>();

		createResultingDataForLanduseInZones(landuseCategoriesPerZone, investigationAreaData, resultingDataPerZone);

		BufferedWriter writer = IOUtils.getBufferedWriter(output.toUri().toURL(), StandardCharsets.UTF_8, true);
		try {
			String[] header = new String[] { "areaID", "Inhabitants", "Employee", "Employee Primary Sector",
					"Employee Construction", "Employee Secondary Sector Rest", "Employee Retail",
					"Employee Traffic/Parcels", "Employee Tertiär Rest" };
			JOIN.appendTo(writer, header);
			writer.write("\n");
			for (String s : resultingDataPerZone.keySet()) {
				List<String> row = new ArrayList<>();
				row.add(s);
				for (String r : header) {
					if (!r.equals("areaID")) {
						row.add(String.valueOf(resultingDataPerZone.get(s).getDouble(r)));
					}
				}
				JOIN.appendTo(writer, row);
				writer.write("\n");
			}

			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;

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
		landuseCategoriesAndDataConnection.put("Inhabitants", List.of("residential"));
		landuseCategoriesAndDataConnection.put("Employee",
				List.of("farmyard", "farmland", "industrial", "retail", "commercial"));
		landuseCategoriesAndDataConnection.put("Employee Primary Sector", List.of("farmyard", "farmland"));
		landuseCategoriesAndDataConnection.put("Employee Construction", List.of("")); // TODO
		landuseCategoriesAndDataConnection.put("Employee Secondary Sector Rest", List.of("industrial"));
		landuseCategoriesAndDataConnection.put("Employee Retail", List.of("retail"));
		landuseCategoriesAndDataConnection.put("Employee Traffic/Parcels", List.of("commercial"));
		landuseCategoriesAndDataConnection.put("Employee Tertiär Rest", List.of("commercial"));

		for (String zoneID : landuseCategoriesPerZone.keySet()) {
			resultingDataPerZone.put(zoneID, new Object2DoubleOpenHashMap<>());
			for (String category : landuseCategoriesPerZone.get(zoneID).keySet()) {
				for (String categoryNameData : landuseCategoriesAndDataConnection.keySet()) {
					if (landuseCategoriesAndDataConnection.get(categoryNameData).contains(category))
						resultingDataPerZone.get(zoneID)
								.mergeDouble(categoryNameData,
										(landuseCategoriesPerZone.get(zoneID).getDouble(category))
												/ landuseCategoriesAndDataConnection.get(categoryNameData).size(),
										Double::sum); // TODO Aufteilung commercial
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
	private void readAreaData(Path areaDataPath, HashMap<String, HashMap<String, Integer>> areaData)
			throws IOException {
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
	private void createLanduseDistribution(Path shapeFileLandusePath, Path shapeFileZonesPath, Path shapeFileBuildingPath,
			HashMap<String, Object2DoubleMap<String>> landuseCategoriesPerZone) {

		List<String> neededLanduseCategories = List.of("residential", "industrial", "commercial", "retail", "farmyard",
				"farmland");
		ShpOptions shpLanduse = new ShpOptions(shapeFileLandusePath, null, StandardCharsets.UTF_8);
		ShpOptions shpBuildings = new ShpOptions(shapeFileLandusePath, null, StandardCharsets.UTF_8);
		ShpOptions shpZones = new ShpOptions(shapeFileZonesPath, null, StandardCharsets.UTF_8);

		List<SimpleFeature> landuseFeatures = shpLanduse.readFeatures();
		List<SimpleFeature> buildingsFeatures = shpBuildings.readFeatures();
		List<SimpleFeature> zonesFeatures = shpZones.readFeatures();

		for (SimpleFeature districId : zonesFeatures) {
			Object2DoubleMap<String> landusePerCategory = new Object2DoubleOpenHashMap<>();
			landuseCategoriesPerZone.put((String) districId.getAttribute("gml_id"), landusePerCategory);
		}

		double totalSquareMeters = 0;
		Object2DoubleMap<String> totalSquareMetersPerCategory = new Object2DoubleOpenHashMap<>();

		for (SimpleFeature singleLanduseFeature : landuseFeatures) {
			if (!neededLanduseCategories.contains((String) singleLanduseFeature.getAttribute("fclass")))
				continue;
			Point centroidPointOfLandusePolygon = ((Geometry) singleLanduseFeature.getDefaultGeometry()).getCentroid();

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
