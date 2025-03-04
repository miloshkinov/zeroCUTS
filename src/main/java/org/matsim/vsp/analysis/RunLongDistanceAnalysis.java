package org.matsim.vsp.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Path;
import java.util.List;

import static org.matsim.application.ApplicationUtils.globFile;

public class RunLongDistanceAnalysis implements MATSimAppCommand {

	private static final String delimiter = ",";

	@CommandLine.Option(names = "--runDirectory", description = "Path to the output directory", defaultValue = "output/longDistanceScenarios/25pct/")
	private static Path runDirectory;

    private RunLongDistanceAnalysis(Path runDirectory) {
		RunLongDistanceAnalysis.runDirectory = runDirectory;
    }

    public static void main(String[] args) {
		System.exit(new CommandLine(createRunLongDistanceAnalysis(runDirectory)).execute(args));
	}

    public static RunLongDistanceAnalysis createRunLongDistanceAnalysis(Path runDirectory) {
        return new RunLongDistanceAnalysis(runDirectory);
    }

    @Override
	public Integer call() {

		String analysisOutputDirectory = runDirectory.toString() + "/analysis/traffic/";
		File dir = new File(analysisOutputDirectory);
		if (!dir.exists()) {
			dir.mkdir();
		}
		final String existingTrafficFile = analysisOutputDirectory +"traffic_stats_by_link_daily.csv";
		final Path eventsFile = globFile(runDirectory, "*output_events*");
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(globFile(runDirectory, "*output_network*").toString());
		config.global().setCoordinateSystem("EPSG:25832");
		config.plans().setInputFile(globFile(runDirectory, "*output_plans*").toString());
		config.eventsManager().setNumberOfThreads(null);
		config.eventsManager().setEstimatedNumberOfEvents(null);
		config.global().setNumberOfThreads(4);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		EventsManager eventsManager = EventsUtils.createEventsManager();

		// link events handler
		LongDistanceAnalysisEventHandler longDistanceAnalysisEventHandler = new LongDistanceAnalysisEventHandler(scenario);

		eventsManager.addHandler(longDistanceAnalysisEventHandler);

		eventsManager.initProcessing();
		new MatsimEventsReader(eventsManager).readFile(eventsFile.toString());

		createGeneralResults(existingTrafficFile, longDistanceAnalysisEventHandler);
		return 0;
	}

	private static void createGeneralResults(String existingTrafficFile,
											 LongDistanceAnalysisEventHandler LongDistanceAnalysisEventHandler) {

		List<String> geographicalTypes = LongDistanceAnalysisEventHandler.getGeographicalTypes();
		try (BufferedReader reader = new BufferedReader(new FileReader(existingTrafficFile))) {

			try (BufferedWriter writer = new BufferedWriter(new FileWriter(existingTrafficFile.replace("_daily.csv", "_daily_longDistanceTypes.csv")))) {
				String lineReader;
				StringBuilder header = new StringBuilder(String.join(delimiter,
						"link_id"));
				for (String type : geographicalTypes) {
					header.append(delimiter + "vol_").append(type);
				}
				header.append(delimiter + "vol_all");
				header.append(delimiter + "share_transit");

				writer.write(header.toString());

				while ((lineReader = reader.readLine()) != null) {
					if (lineReader.startsWith("link_id"))
						continue;
					// Neue Spalte hinzufügen (mit Komma als Trennzeichen)
					writer.newLine();
					Id<Link> linkID = Id.createLinkId(lineReader.split(",")[0]);
					StringBuilder newLine = new StringBuilder(String.valueOf(linkID));
					for (String type : geographicalTypes) {
						newLine.append(delimiter).append(
								LongDistanceAnalysisEventHandler.getVolumesPerType(type, linkID));
					}
					newLine.append(delimiter).append(LongDistanceAnalysisEventHandler.getVolumesPerType("all", linkID));
					newLine.append(delimiter).append(Math.round(((float) LongDistanceAnalysisEventHandler.getVolumesPerType("transit",
							linkID) /
							(float)LongDistanceAnalysisEventHandler.getVolumesPerType("all", linkID)) * 10000) / 100);
					writer.write(String.valueOf(newLine));
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}