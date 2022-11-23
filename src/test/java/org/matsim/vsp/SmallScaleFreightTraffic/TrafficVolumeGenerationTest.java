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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vsp.SmallScaleFreightTraffic.TrafficVolumeGeneration.TrafficVolumeKey;
import org.opengis.feature.simple.SimpleFeature;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;

/**
 * @author Ricardo
 *
 */
public class TrafficVolumeGenerationTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void trafficVolumeGenerationBusinessTraffic() throws IOException {

		HashMap<String, ArrayList<String>> landuseCategoriesAndDataConnection = new HashMap<String, ArrayList<String>>();
		HashMap<String, HashMap<String, ArrayList<SimpleFeature>>> buildingsPerZone = new HashMap<>();
		
		Path output = Path.of(utils.getOutputDirectory());
		new File(output.resolve("caculatedData").toString()).mkdir();
		Path inputDataDirectory = Path.of(utils.getPackageInputDirectory());
		String usedLanduseConfiguration = "useExistingDataDistribution";
		Path shapeFileLandusePath = inputDataDirectory.resolve("testShape/testLanduse.shp");
		Path shapeFileZonePath = inputDataDirectory.resolve("testShape/testZones.shp");
		Path shapeFileBuildingsPath = inputDataDirectory.resolve("testShape/testBuildings.shp");

	
		HashMap<String, Object2DoubleMap<String>> resultingDataPerZone = LanduseBuildingAnalysis
				.createInputDataDistribution(output, landuseCategoriesAndDataConnection,
						inputDataDirectory.resolve("scenarios/testScenario"), usedLanduseConfiguration,
						shapeFileLandusePath, shapeFileZonePath, shapeFileBuildingsPath, buildingsPerZone);
		
		
		String usedTrafficType = "businessTraffic";
		double sample = 1.;
		ArrayList<String> modesORvehTypes = new ArrayList<String>(
				Arrays.asList("total"));
		TrafficVolumeGeneration.setInputParamters(usedTrafficType);

		HashMap<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_start = TrafficVolumeGeneration
				.createTrafficVolume_start(resultingDataPerZone, output, sample, modesORvehTypes, usedTrafficType);
		HashMap<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_stop = TrafficVolumeGeneration
				.createTrafficVolume_stop(resultingDataPerZone, output, sample, modesORvehTypes, usedTrafficType);
		
		Assert.assertEquals(3, trafficVolumePerTypeAndZone_start.size());
		Assert.assertEquals(3, trafficVolumePerTypeAndZone_stop.size());
		
		for (String zone : resultingDataPerZone.keySet()) {
			TrafficVolumeKey trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey(zone, modesORvehTypes.get(0));
			Assert.assertTrue(trafficVolumePerTypeAndZone_start.containsKey(trafficVolumeKey));
			Assert.assertTrue(trafficVolumePerTypeAndZone_stop.containsKey(trafficVolumeKey));
		}
		TrafficVolumeKey trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("testArea1_area1", modesORvehTypes.get(0));
		Assert.assertEquals(30, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals(124, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(277, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assert.assertEquals(175, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assert.assertEquals(250, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals(10, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals(105, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(426, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assert.assertEquals(121, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assert.assertEquals(65, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
		
		trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("testArea1_area2", modesORvehTypes.get(0));
		Assert.assertEquals(30, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals(211, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(514, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assert.assertEquals(441, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assert.assertEquals(630, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals(10, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals(202, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(859, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assert.assertEquals(246, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assert.assertEquals(102, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
		
		trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("testArea2_area3", modesORvehTypes.get(0));
		Assert.assertEquals(6, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals(34, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(79, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assert.assertEquals(62, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assert.assertEquals(88, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals(2, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals(31, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(128, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assert.assertEquals(37, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assert.assertEquals(17, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
		
		
		//test with different sample
		sample = 0.25;
		trafficVolumePerTypeAndZone_start = TrafficVolumeGeneration
				.createTrafficVolume_start(resultingDataPerZone, output, sample, modesORvehTypes, usedTrafficType);
		trafficVolumePerTypeAndZone_stop = TrafficVolumeGeneration
				.createTrafficVolume_stop(resultingDataPerZone, output, sample, modesORvehTypes, usedTrafficType);
		
		Assert.assertEquals(3, trafficVolumePerTypeAndZone_start.size());
		Assert.assertEquals(3, trafficVolumePerTypeAndZone_stop.size());
		
		trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("testArea1_area1", modesORvehTypes.get(0));
		Assert.assertEquals(7, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals(31, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(69, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assert.assertEquals(44, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assert.assertEquals(63, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals(3, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals(26, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(106, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assert.assertEquals(30, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assert.assertEquals(16, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
		
		trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("testArea1_area2", modesORvehTypes.get(0));
		Assert.assertEquals(7, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals(53, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(129, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assert.assertEquals(110, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assert.assertEquals(158, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals(3, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals(50, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(215, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assert.assertEquals(61, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assert.assertEquals(25, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
		
		trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("testArea2_area3", modesORvehTypes.get(0));
		Assert.assertEquals(1, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals(8, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(20, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assert.assertEquals(15, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assert.assertEquals(22, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals(1, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals(8, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals(32, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);
		Assert.assertEquals(9, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);
		Assert.assertEquals(4, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);
	}
	
	@Test
	public void trafficVolumeGenerationFreightTraffic() throws IOException {

		HashMap<String, ArrayList<String>> landuseCategoriesAndDataConnection = new HashMap<String, ArrayList<String>>();
		HashMap<String, HashMap<String, ArrayList<SimpleFeature>>> buildingsPerZone = new HashMap<>();
		
		Path output = Path.of(utils.getOutputDirectory());
		new File(output.resolve("caculatedData").toString()).mkdir();
		Path inputDataDirectory = Path.of(utils.getPackageInputDirectory());
		String usedLanduseConfiguration = "useExistingDataDistribution";
		Path shapeFileLandusePath = inputDataDirectory.resolve("testShape/testLanduse.shp");
		Path shapeFileZonePath = inputDataDirectory.resolve("testShape/testZones.shp");
		Path shapeFileBuildingsPath = inputDataDirectory.resolve("testShape/testBuildings.shp");

	
		HashMap<String, Object2DoubleMap<String>> resultingDataPerZone = LanduseBuildingAnalysis
				.createInputDataDistribution(output, landuseCategoriesAndDataConnection,
						inputDataDirectory.resolve("scenarios/testScenario"), usedLanduseConfiguration,
						shapeFileLandusePath, shapeFileZonePath, shapeFileBuildingsPath, buildingsPerZone);
		
		
		String usedTrafficType = "freightTraffic";
		double sample = 1.;
		ArrayList<String> modesORvehTypes = new ArrayList<String>(
				Arrays.asList("vehTyp1", "vehTyp2", "vehTyp3", "vehTyp4", "vehTyp5"));
		TrafficVolumeGeneration.setInputParamters(usedTrafficType);

		HashMap<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_start = TrafficVolumeGeneration
				.createTrafficVolume_start(resultingDataPerZone, output, sample, modesORvehTypes, usedTrafficType);
		HashMap<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_stop = TrafficVolumeGeneration
				.createTrafficVolume_stop(resultingDataPerZone, output, sample, modesORvehTypes, usedTrafficType);
		
		Assert.assertEquals(15, trafficVolumePerTypeAndZone_start.size());
		Assert.assertEquals(15, trafficVolumePerTypeAndZone_stop.size());
		
		for (String zone : resultingDataPerZone.keySet()) {
			for (String modesORvehType : modesORvehTypes) {
				TrafficVolumeKey trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey(zone, modesORvehType);
				Assert.assertTrue(trafficVolumePerTypeAndZone_start.containsKey(trafficVolumeKey));
				Assert.assertTrue(trafficVolumePerTypeAndZone_stop.containsKey(trafficVolumeKey));
			}
		}
		
		// test for "testArea1_area1"
		HashMap<Integer, Double> estimatesStart = new HashMap<>();
		estimatesStart.put(1, 12.);
		estimatesStart.put(2, 30.);
		estimatesStart.put(3, 205.);
		estimatesStart.put(4, 174.);
		estimatesStart.put(5, 117.);
		estimatesStart.put(6, 36.);
		
		HashMap<Integer, Double> estimatesStop = new HashMap<>();
		estimatesStop.put(1, 15.);
		estimatesStop.put(2, 36.);
		estimatesStop.put(3, 139.);
		estimatesStop.put(4, 300.);
		estimatesStop.put(5, 32.);
		estimatesStop.put(6, 31.);
		for (int i = 1; i < 7; i++) {
			double sumStart = 0;
			double sumStop = 0;
			for (String modeORvehType : modesORvehTypes) {
				TrafficVolumeKey trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("testArea1_area1", modeORvehType);
				sumStart += trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(i);
				sumStop += trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(i);
				if (modeORvehType.equals("vehTyp1")) {
					Assert.assertEquals(5, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
					Assert.assertEquals(16, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(101, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(36, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(33, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(0, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);
					
					Assert.assertEquals(5, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
					Assert.assertEquals(17, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(73, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(54, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(10, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(0, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);
				}
				if (modeORvehType.equals("vehTyp2")) {
					Assert.assertEquals(1, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
					Assert.assertEquals(3, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(21, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(11, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(23, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(10, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);	
					
					Assert.assertEquals(2, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
					Assert.assertEquals(3, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(13, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(20, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(7, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(11, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);
				}
				if (modeORvehType.equals("vehTyp3")) {
					Assert.assertEquals(3, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
					Assert.assertEquals(6, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(44, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(42, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(28, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(23, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);
					
					Assert.assertEquals(4, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
					Assert.assertEquals(8, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(28, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(73, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(6, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(15, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);
				}
				if (modeORvehType.equals("vehTyp4")) {
					Assert.assertEquals(1, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
					Assert.assertEquals(1, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(10, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(13, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(2, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(3, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);
					
					Assert.assertEquals(0, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
					Assert.assertEquals(2, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(5, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(20, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(1, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(5, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);
				}
				if (modeORvehType.equals("vehTyp5")) {
					Assert.assertEquals(2, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
					Assert.assertEquals(4, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(29, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(72, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(31, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(0, trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);
					
					Assert.assertEquals(4, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(1), MatsimTestUtils.EPSILON);
					Assert.assertEquals(6, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(2), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(20, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(3), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(133, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(4), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(8, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(5), MatsimTestUtils.EPSILON);	
					Assert.assertEquals(0, trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(6), MatsimTestUtils.EPSILON);
				}
			}
			Assert.assertEquals(estimatesStart.get(i), sumStart, MatsimTestUtils.EPSILON);
			Assert.assertEquals(estimatesStop.get(i), sumStop, MatsimTestUtils.EPSILON);
		}
		
		// test for "testArea1_area2"
		estimatesStart = new HashMap<>();
		estimatesStart.put(1, 12.);
		estimatesStart.put(2, 37.);
		estimatesStart.put(3, 201.);
		estimatesStart.put(4, 512.);
		estimatesStart.put(5, 343.);
		estimatesStart.put(6, 36.);
		
		estimatesStop = new HashMap<>();
		estimatesStop.put(1, 15.);
		estimatesStop.put(2, 40.);
		estimatesStop.put(3, 165.);
		estimatesStop.put(4, 273.);
		estimatesStop.put(5, 42.);
		estimatesStop.put(6, 41.);
		for (int i = 1; i < 7; i++) {
			double sumStart = 0;
			double sumStop = 0;
			for (String modeORvehType : modesORvehTypes) {
				TrafficVolumeKey trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("testArea1_area2", modeORvehType);
				sumStart += trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(i);
				sumStop += trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(i);
			}
			Assert.assertEquals(estimatesStart.get(i), sumStart, MatsimTestUtils.EPSILON);
			Assert.assertEquals(estimatesStop.get(i), sumStop, MatsimTestUtils.EPSILON);
		}
		
		// test for "testArea2_area23"
		estimatesStart = new HashMap<>();
		estimatesStart.put(1, 2.);
		estimatesStart.put(2, 7.);
		estimatesStart.put(3, 42.);
		estimatesStart.put(4, 69.);
		estimatesStart.put(5, 46.);
		estimatesStart.put(6, 8.);
		
		estimatesStop = new HashMap<>();
		estimatesStop.put(1, 3.);
		estimatesStop.put(2, 8.);
		estimatesStop.put(3, 30.);
		estimatesStop.put(4, 57.);
		estimatesStop.put(5, 6.);
		estimatesStop.put(6, 6.);
		for (int i = 1; i < 7; i++) {
			double sumStart = 0;
			double sumStop = 0;
			for (String modeORvehType : modesORvehTypes) {
				TrafficVolumeKey trafficVolumeKey = TrafficVolumeGeneration.makeTrafficVolumeKey("testArea2_area3", modeORvehType);
				sumStart += trafficVolumePerTypeAndZone_start.get(trafficVolumeKey).getDouble(i);
				sumStop += trafficVolumePerTypeAndZone_stop.get(trafficVolumeKey).getDouble(i);
			}
			Assert.assertEquals(estimatesStart.get(i), sumStart, MatsimTestUtils.EPSILON);
			Assert.assertEquals(estimatesStop.get(i), sumStop, MatsimTestUtils.EPSILON);
		}
		int a = 9;
	}
}
