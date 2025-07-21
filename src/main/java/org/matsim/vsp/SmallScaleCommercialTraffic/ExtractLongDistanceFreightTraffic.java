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
package org.matsim.vsp.SmallScaleCommercialTraffic;

import java.io.File;
import java.nio.file.Path;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.prepare.longDistanceFreightGER.tripExtraction.ExtractRelevantFreightTrips;
import org.matsim.core.population.PopulationUtils;

/** Extracts the long distance freight traffic for Berlin/Brandenburg and samples the population to the required sample size.
 * 
 * @author Ricardo Ewert
 *
 */
public class ExtractLongDistanceFreightTraffic {

	public static void main(String[] args) throws Exception {
		
		Path germanyPlansFile = Path.of("../public-svn/matsim/scenarios/countries/de/german-wide-freight/v2/german_freight.25pct.plans.xml.gz");
//		Path networkPath = Path.of("../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz");
		Path networkPath = Path.of("../public-svn/matsim/scenarios/countries/de/german-wide-freight/v2/germany-europe-network.xml.gz");

		Path outputPath = Path.of("../zeroCUTS/output/longDistanceFreight");
		Path shpPath = Path.of("../public-svn/matsim/scenarios/countries/de/berlin/projects/zerocuts/small-scale-commercial-traffic/input/shp/berlinBrandenburg/berlinBrandenburg_4326.shp");
		String inputCRS = "EPSG:25832";
		String shpCRS = "EPSG:4326";
		String targetCRS = "EPSG:31468";
		double inputPopulationSample = 0.25;
		double samplePopulationTo = 0.1;
		
		if (samplePopulationTo > inputPopulationSample)
			throw new Exception("The sample of the input population is smaller then the expected output sample. This is not possible.");
		
		new ExtractRelevantFreightTrips().execute(
				germanyPlansFile.toString(),
				"--network", networkPath.toString(),
				"--output", outputPath.toString(),
				"--shp", shpPath.toString(),
				"--input-crs", inputCRS,
				"--target-crs", targetCRS,
				"--shp-crs", shpCRS,
				"--cut-on-boundary",
				"--tripType", "TRANSIT"
				);
		Population population = PopulationUtils.readPopulation(outputPath.toString() + "/extracted-population.xml.gz");
		for (Person person : population.getPersons().values()) {
			PopulationUtils.putSubpopulation(person, "longDistanceFreight");
		}
		PopulationUtils.sampleDown(population, samplePopulationTo/inputPopulationSample);
		PopulationUtils.writePopulation(population, outputPath + "/berlin_longDistanceFreight_"+(int)(samplePopulationTo*100)+"pct.xml.gz");
		assert (new File (outputPath + "/extracted-population.xml.gz").delete());
	}
}
