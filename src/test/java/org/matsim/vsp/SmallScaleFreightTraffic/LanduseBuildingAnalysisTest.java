package org.matsim.vsp.SmallScaleFreightTraffic;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.application.options.ShpOptions;
import org.matsim.application.options.ShpOptions.Index;
import org.matsim.testcases.MatsimTestUtils;
import org.opengis.feature.simple.SimpleFeature;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;

public class LanduseBuildingAnalysisTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testReadOfDataDistributionPerZoneAndBuildingAnalysis() throws IOException {
		HashMap<String, ArrayList<String>> landuseCategoriesAndDataConnection = new HashMap<String, ArrayList<String>>();
		HashMap<String, HashMap<String, ArrayList<SimpleFeature>>> buildingsPerZone = new HashMap<>();

		Path output = Path.of(utils.getOutputDirectory());
		new File(output.resolve("caculatedData").toString()).mkdir();
		Path inputDataDirectory = Path.of(utils.getPackageInputDirectory());
		String usedLanduseConfiguration = "useExistingDataDistribution";
		Path shapeFileLandusePath = inputDataDirectory.resolve("testShape/testLanduse.shp");
		Path shapeFileZonePath = inputDataDirectory.resolve("testShape/testZones.shp");
		Path shapeFileBuildingsPath = inputDataDirectory.resolve("testShape/testBuildings.shp");
		HashMap<String, Object2DoubleMap<String>> resultingDataPerZone = LanduseBuildingAnalysis.createInputDataDistribution(output, landuseCategoriesAndDataConnection,
				inputDataDirectory.resolve("scenarios/testScenario"), usedLanduseConfiguration, shapeFileLandusePath, shapeFileZonePath,
				shapeFileBuildingsPath, buildingsPerZone);
		
		Assert.assertEquals(3, resultingDataPerZone.size(), MatsimTestUtils.EPSILON);
		
		Assert.assertTrue(resultingDataPerZone.containsKey("testArea1_area1"));
		Assert.assertTrue(resultingDataPerZone.containsKey("testArea1_area2"));
		Assert.assertTrue(resultingDataPerZone.containsKey("testArea2_area3"));
		
		for (Object2DoubleMap<String> categories : resultingDataPerZone.values()) {
			int employeeSum = 0;
			Assert.assertEquals(8,categories.size(), MatsimTestUtils.EPSILON);
			Assert.assertTrue(categories.containsKey("Inhabitants"));
			Assert.assertTrue(categories.containsKey("Employee"));
			Assert.assertTrue(categories.containsKey("Employee Primary Sector"));
			Assert.assertTrue(categories.containsKey("Employee Construction"));
			Assert.assertTrue(categories.containsKey("Employee Secondary Sector Rest"));
			Assert.assertTrue(categories.containsKey("Employee Retail"));
			Assert.assertTrue(categories.containsKey("Employee Traffic/Parcels"));
			Assert.assertTrue(categories.containsKey("Employee Tertiary Sector Rest"));	
			
			employeeSum += categories.getDouble("Employee Primary Sector");
			employeeSum += categories.getDouble("Employee Construction");
			employeeSum += categories.getDouble("Employee Secondary Sector Rest");
			employeeSum += categories.getDouble("Employee Retail");
			employeeSum += categories.getDouble("Employee Traffic/Parcels");
			employeeSum += categories.getDouble("Employee Tertiary Sector Rest");
			
			Assert.assertEquals(categories.getDouble("Employee"), employeeSum, MatsimTestUtils.EPSILON);
		}

		Index indexZones = SmallScaleFreightTrafficUtils.getIndexZones(shapeFileZonePath);
		ShpOptions shpBuildings = new ShpOptions(shapeFileBuildingsPath, null, StandardCharsets.UTF_8);
		List<SimpleFeature> buildingsFeatures = shpBuildings.readFeatures();
		LanduseBuildingAnalysis.analyzeBuildingType(buildingsFeatures, buildingsPerZone, landuseCategoriesAndDataConnection,
				shapeFileLandusePath, indexZones);
		
		Assert.assertEquals(3, buildingsPerZone.size(), MatsimTestUtils.EPSILON);
		Assert.assertTrue(buildingsPerZone.containsKey("testArea1_area1"));
		Assert.assertTrue(buildingsPerZone.containsKey("testArea1_area2"));
		Assert.assertTrue(buildingsPerZone.containsKey("testArea2_area3"));
	}
}
