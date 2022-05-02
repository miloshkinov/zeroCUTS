package org.matsim.vsp.SmallScaleFreightTraffic;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.options.ShpOptions;
import org.matsim.application.options.ShpOptions.Index;
import org.matsim.core.utils.io.IOUtils;

import com.google.common.base.Joiner;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;

/**
 * @author Ricardo
 *
 */
public class SmallScaleFreightTrafficUtils {
	
	private static final Logger log = LogManager.getLogger(LanduseBuildingAnalysis.class);
	private static final Joiner JOIN = Joiner.on("\t");

	
	static Index getIndexZones(Path shapeFileZonePath) {

			ShpOptions shpZones = new ShpOptions(shapeFileZonePath, "EPSG:4326", StandardCharsets.UTF_8);
			 Index indexZones = shpZones.createIndex("EPSG:4326", "gml_id");
		return indexZones;
	}

	static Index getIndexLanduse(Path shapeFileLandusePath) {

			ShpOptions shpLanduse = new ShpOptions(shapeFileLandusePath, "EPSG:4326", StandardCharsets.UTF_8);
			Index indexLanduse = shpLanduse.createIndex("EPSG:4326", "fclass");
		return indexLanduse;
	}
	
	/**
	 * Writes a csv file with result of the distribution per zone of the input data.
	 * 
	 * @param resultingDataPerZone
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	static void writeResultOfDataDistribution(HashMap<String, Object2DoubleMap<String>> resultingDataPerZone,
			Path outputFileInOutputFolder) throws IOException, MalformedURLException {

		writeCSVWithCategoryHeader(resultingDataPerZone, outputFileInOutputFolder);
		log.info("The data distribution is finished and written to: " + outputFileInOutputFolder);
	}
	
	/**
	 * @param resultingDataPerZone
	 * @param outputFileInInputFolder
	 * @throws MalformedURLException
	 */
	private static void writeCSVWithCategoryHeader(HashMap<String, Object2DoubleMap<String>> resultingDataPerZone,
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
					if (!category.equals("areaID"))
						row.add(String.valueOf((int) Math.round(resultingDataPerZone.get(zone).getDouble(category))));
				}
				JOIN.appendTo(writer, row);
				writer.write("\n");
			}

			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
