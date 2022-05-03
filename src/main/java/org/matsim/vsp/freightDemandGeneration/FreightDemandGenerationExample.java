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
package org.matsim.vsp.freightDemandGeneration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.freightDemandGeneration.FreightDemandGeneration;
import org.matsim.vsp.freightAnalysis.FreightAnalyse;

public class FreightDemandGenerationExample {

		public static void main(String[] args) throws UncheckedIOException, IOException {
		Path output = Path.of("output/demandGeneration/");
		Path vehicleFilePath = Path.of("../public-svn/matsim/scenarios/countries/de/freight-demand-generation/input_example/vehicleTypes_default.xml");
		Path carrierCSVLocation = Path.of("../public-svn/matsim/scenarios/countries/de/freight-demand-generation/input_example/exampleCarrier.csv");
		Path demandCSVLocation = Path.of("../public-svn/matsim/scenarios/countries/de/freight-demand-generation/input_example/exampleDemand.csv");
		String network = "../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";
		Path population = Path.of("../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-1pct/input/berlin-v5.5-1pct.plans.xml.gz");
		Path shapeFilePath = Path.of("../public-svn/matsim/scenarios/countries/de/freight-demand-generation/input_example/shp/Berlin_Ortsteile.shp");
		new FreightDemandGeneration().execute(
				"--output", output.toString(),
				"--carrierOption", "createCarriersFromCSV",
				"--demandOption", "createDemandFromCSV",
				"--populationOption", "useNoPopulation",
				"--populationSamplingOption", "createMoreLocations",
				"--VRPSolutionsOption", "runJspritAndMATSim",
				"--combineSimilarJobs", "false",
				"--carrierFileLocation", "",
				"--carrierVehicleFileLocation", vehicleFilePath.toString(),
				"--shapeFileLocation", shapeFilePath.toString(),
				"--shapeCRS", "EPSG:3857",
				"--populationFileLocation", population.toString(),
				"--network", network,
				"--networkCRS", "EPSG:31468",
				"--networkChangeEvents", "",
				"--inputCarrierCSV", carrierCSVLocation.toString(),
				"--inputDemandCSV", demandCSVLocation.toString(),
				"--populationSample", "0.5",
				"--populationSamplingTo", "1.0",
				"--populationCRS", "DHDN_GK4",
				"--defaultJspriIterations", "3"
		);
		List<File> fileData = new ArrayList<>();
		for (File file : Objects.requireNonNull(output.toFile().listFiles())) {
			fileData.add(file);
		}
		Collections.sort(fileData);
		File lastFile = fileData.get(fileData.size()-1);
		String[] argsAnalysis = { lastFile.toString()};
		FreightAnalyse.main(argsAnalysis);
	}}

