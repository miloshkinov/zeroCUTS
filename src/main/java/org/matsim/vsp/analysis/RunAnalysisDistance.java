package org.matsim.vsp.analysis;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.vehicles.Vehicle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static org.matsim.application.ApplicationUtils.globFile;

public class RunAnalysisDistance {

	public static void main(String[] args) {

//		String outputDirectory = "output/goodsTraffic/";
		String outputDirectory = "../matsim-berlin/output/commercialPersonTraffic/";
//		String outputDirectory = "../zerocuts/output/BusinessPassengerTraffic/berlin/calibration/bothTypes_1pct_2022-12-14_41270_0.001/";
//		String rundID = "goodsTraffic";
		String rundID = "commercialPersonTraffic";


		String analysisOutputDirectory = outputDirectory + "/analysis/";
		File dir = new File(analysisOutputDirectory);
		if (!dir.exists()) {
			dir.mkdir();
		}
		final String general_resultsOutputFile = analysisOutputDirectory + rundID + ".general_results_distance.csv";
		final String eventsFile = globFile(Path.of(outputDirectory), rundID, "output_events");
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(String.valueOf(globFile(Path.of(outputDirectory), rundID, "network")));
		config.global().setCoordinateSystem(TransformationFactory.DHDN_GK4);
		config.plans().setInputFile(String.valueOf(globFile(Path.of(outputDirectory), rundID, "plans.xml")));
		config.eventsManager().setNumberOfThreads(null);
		config.eventsManager().setEstimatedNumberOfEvents(null);
		config.global().setNumberOfThreads(4);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		EventsManager eventsManager = EventsUtils.createEventsManager();

		AbstractModule module = new AbstractModule() {
			@Override
			public void install() {
				bind(Scenario.class).toInstance(scenario);
				bind(EventsManager.class).toInstance(eventsManager);
				bind(EmissionModule.class);
			}
		};

		// link events handler
		CommercialAnalysisEventHandler commercialAnalysisEventHandler = new CommercialAnalysisEventHandler(scenario);

		eventsManager.addHandler(commercialAnalysisEventHandler);

		eventsManager.initProcessing();
		new MatsimEventsReader(eventsManager).readFile(eventsFile);

		createGeneralResults(scenario, general_resultsOutputFile, commercialAnalysisEventHandler);

	}
	private static void createGeneralResults(Scenario scenario, String generalResultsOutputFile,
											 CommercialAnalysisEventHandler commercialAnalysisEventHandler) {
		File tourDataFile = new File(generalResultsOutputFile);
		Object2DoubleMap<String> overviewData = new Object2DoubleOpenHashMap<>();
		Map<Id<Vehicle>, Object2DoubleMap<String>> tourInformation = commercialAnalysisEventHandler.getTourInformation();
		overviewData.put("numberOfTours", tourInformation.keySet().size());

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(tourDataFile));
			bw.write(
					"vehicle;vehicleType;drivenDistance;CO2_TOTAL");
			bw.newLine();
			for (Id<Vehicle> vehcileId : tourInformation.keySet()) {
				String vehicleType = scenario.getVehicles().getVehicles().get(vehcileId).getType().getId().toString();
				Object2DoubleMap<String> tourData = tourInformation.get(vehcileId);
				bw.write(vehcileId.toString() + ";" + vehicleType + ";" + tourData.getDouble("drivenDistance")
				);
				bw.newLine();

				//sum data for overview
				overviewData.mergeDouble("drivenDistance", tourData.getDouble("drivenDistance"), Double::sum);
			}
			bw.write((int) overviewData.getDouble("numberOfTours") + ";" + overviewData.getDouble("drivenDistance")
			);

			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
