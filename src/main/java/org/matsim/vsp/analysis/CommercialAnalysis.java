package org.matsim.vsp.analysis;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.VspHbefaRoadTypeMapping;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.matsim.application.ApplicationUtils.globFile;
import static org.matsim.contrib.emissions.Pollutant.*;

public class CommercialAnalysis implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(CommercialAnalysis.class);
	static List<Pollutant> pollutants2Output = Arrays.asList(CO2_TOTAL, NOx, PM, PM_non_exhaust, FC);
	private final String timeformatForOutput = Time.TIMEFORMAT_SSSS;
	private final Path runDirectory;
	private final String runId;
	private final String hbefaWarmFile;
	private final String hbefaColdFile;
	private final String analysisOutputDirectory;

	public CommercialAnalysis(String runDirectory, String runId, String hbefaFileWarm, String hbefaFileCold,
								   String analysisOutputDirectory) {
		this.runDirectory = Path.of(runDirectory);
		this.runId = runId;
		this.hbefaWarmFile = hbefaFileWarm;
		this.hbefaColdFile = hbefaFileCold;

		if (!analysisOutputDirectory.endsWith("/")) analysisOutputDirectory = analysisOutputDirectory + "/";
		this.analysisOutputDirectory = analysisOutputDirectory;
	}

	public static void main(String[] args) {

		if (args.length > 0) {
			String runDirectory = args[0];
			if (!runDirectory.endsWith("/")) runDirectory = runDirectory + "/";

			final String runId = args[1];
			boolean analyseEmissions = true;
			if (args.length > 2 && args[2] != null)
				analyseEmissions = Boolean.parseBoolean(args[2]);// based on the simulation output available in this project
			final String hbefaPath = "../public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/";

			String hbefaFileWarm = hbefaPath + "7eff8f308633df1b8ac4d06d05180dd0c5fdf577.enc";
			String hbefaFileCold = hbefaPath + "ColdStart_Vehcat_2020_Average_withHGVetc.csv.enc";

			CommercialAnalysis analysis = new CommercialAnalysis(
				runDirectory,
				runId,
				hbefaFileWarm,
				hbefaFileCold,
				runDirectory + "simwrapper_analysis");
			try {
				analysis.call();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

		} else {
			throw new RuntimeException(
				"Please set the run directory path and/or password. \nCheck the class description for more details. Aborting...");
		}
	}

	public Integer call() {
		final String eventsFile = globFile(runDirectory, runId, "output_events");
		File dir = new File(analysisOutputDirectory);
		if (!dir.exists()) {
			dir.mkdir();
		}
		final String emissionEventOutputFile = analysisOutputDirectory + runId + ".emission.events.offline.xml.gz";

		final String general_resultsOutputFile = analysisOutputDirectory + runId + ".general_results_distance.csv";
		log.info("Writing general results to: {}", general_resultsOutputFile);

		log.info("Writing events to: {}", emissionEventOutputFile);
		Config config = ConfigUtils.createConfig();
		config.vehicles().setVehiclesFile(String.valueOf(globFile(runDirectory, runId, "output_allVehicles")));
		config.network().setInputFile(String.valueOf(globFile(runDirectory, runId, "network")));
		config.facilities().setInputFile(String.valueOf(globFile(runDirectory, runId, "facilities")));
		//TODO OSMHBEFAMAPPING siehe Kehlheim project
		config.global().setCoordinateSystem(TransformationFactory.DHDN_GK4);
		config.plans().setInputFile(String.valueOf(globFile(runDirectory, runId, "plans.xml")));
		config.eventsManager().setNumberOfThreads(null);
		config.eventsManager().setEstimatedNumberOfEvents(null);
		config.global().setNumberOfThreads(4);

		EmissionsConfigGroup eConfig = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);
		eConfig.setDetailedVsAverageLookupBehavior(EmissionsConfigGroup.DetailedVsAverageLookupBehavior.directlyTryAverageTable);
		eConfig.setAverageColdEmissionFactorsFile(this.hbefaColdFile);
		eConfig.setAverageWarmEmissionFactorsFile(this.hbefaWarmFile);
		eConfig.setNonScenarioVehicles(EmissionsConfigGroup.NonScenarioVehicles.ignore);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		new VspHbefaRoadTypeMapping().addHbefaMappings(scenario.getNetwork());

		EventsManager eventsManager = EventsUtils.createEventsManager();

		AbstractModule module = new AbstractModule() {
			@Override
			public void install() {
				bind(Scenario.class).toInstance(scenario);
				bind(EventsManager.class).toInstance(eventsManager);
				bind(EmissionModule.class);
			}
		};

		com.google.inject.Injector injector = Injector.createInjector(config, module);
		EventWriterXML emissionEventWriter = null;

		EmissionModule emissionModule = injector.getInstance(EmissionModule.class);

		emissionEventWriter = new EventWriterXML(emissionEventOutputFile);
		emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);

		// link events handler
		CommercialAnalysisEventHandler commercialAnalysisEventHandler = new CommercialAnalysisEventHandler(scenario);

		eventsManager.addHandler(commercialAnalysisEventHandler);
		EmissionsOnLinkHandlerCommercial emissionsOnLinkEventHandler = null;
		emissionsOnLinkEventHandler =  new EmissionsOnLinkHandlerCommercial(scenario);
		eventsManager.addHandler(emissionsOnLinkEventHandler);

		eventsManager.initProcessing();
		new MatsimEventsReader(eventsManager).readFile(eventsFile);

		log.info("-------------------------------------------------");
		log.info("Done reading the events file");
		log.info("Finish processing...");
		eventsManager.finishProcessing();
		log.info("Closing events file...");

//		emissionEventWriter.closeFile();
		log.info("Done");
		log.info("Writing (more) output...");

		createGeneralResults(scenario, general_resultsOutputFile, commercialAnalysisEventHandler, emissionsOnLinkEventHandler);

		return 0;
	}
	private void createGeneralResults(Scenario scenario, String generalResultsOutputFile,
									  CommercialAnalysisEventHandler commercialAnalysisEventHandler,
									  EmissionsOnLinkHandlerCommercial emissionsOnLinkEventHandler) {
		File tourDataFile = new File(generalResultsOutputFile);
		Object2DoubleMap<String> overviewData = new Object2DoubleOpenHashMap<>();
		Map<Id<Vehicle>, Object2DoubleMap<String>> tourInformation = commercialAnalysisEventHandler.getTourInformation();
		Map<Id<Vehicle>, Map<Pollutant, Double>> pollutantsPerVehicle = emissionsOnLinkEventHandler.getPollutantsPerVehicle();
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
						+ ";" + pollutantsPerVehicle.get(vehcileId).get(CO2_TOTAL)
				);
				bw.newLine();

				//sum data for overview
				overviewData.mergeDouble("drivenDistance", tourData.getDouble("drivenDistance"), Double::sum);
				overviewData.mergeDouble("CO2_TOTAL", pollutantsPerVehicle.get(vehcileId).get(CO2_TOTAL), Double::sum);
			}
			bw.write((int) overviewData.getDouble("numberOfTours") + ";" + overviewData.getDouble("drivenDistance")
					+ ";" + overviewData.getDouble("CO2_TOTAL")
			);

			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
