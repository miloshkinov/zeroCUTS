package org.matsim.vsp.emissions;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.analysis.EmissionsOnLinkEventHandler;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author Kai Martins-Turner (kturner)
 */
public class RunEmissionEventsAnalysis2024 {

  private static final Logger log = LogManager.getLogger(RunEmissionEventsAnalysis2024.class);
  private final String runDirectory;
  private final String analysisOutputDirectory;


  public static void main(String[] args) throws IOException {

//    // Update EFoods2020::
//    final String pathToRunDir = "/Users/kturner/git-and-svn/shared-svn/projects/freight/studies/UpdateEventsfromEarlierStudies/Food_ETrucks/";
//    var listOfRuns = List.of(
//        "Base_NwCE_BVWP_Pickup_10000it/",
//        "CaseA_E160_NwCE_BVWP_Pickup_10000it/",
//        "CaseB_E100_NwCE_BVWP_Pickup_10000it/"
//    )   ;


        final String pathToRunDir = "/Users/kturner/git-and-svn/shared-svn/projects/freight/studies/UpdateEventsfromEarlierStudies/";
    var listOfRuns = List.of(
        "foodRetailing_wo_rangeConstraint/71_ICEVBEV_NwCE_BVWP_10000it_DCoff_noTax/",
        "foodRetailing_wo_rangeConstraint/71a_ICEV_NwCE_BVWP_10000it_DCoff_noTax/",
        "foodRetailing_wo_rangeConstraint/72_ICEVBEV_NwCE_BVWP_10000it_DCoff_Tax25/",
        "foodRetailing_wo_rangeConstraint/73_ICEVBEV_NwCE_BVWP_10000it_DCoff_Tax50/",
        "foodRetailing_wo_rangeConstraint/74_ICEVBEV_NwCE_BVWP_10000it_DCoff_Tax100/",
        "foodRetailing_wo_rangeConstraint/75_ICEVBEV_NwCE_BVWP_10000it_DCoff_Tax150/",
        "foodRetailing_wo_rangeConstraint/76_ICEVBEV_NwCE_BVWP_10000it_DCoff_Tax200/",
        "foodRetailing_wo_rangeConstraint/77_ICEVBEV_NwCE_BVWP_10000it_DCoff_Tax250/",
        "foodRetailing_wo_rangeConstraint/78_ICEVBEV_NwCE_BVWP_10000it_DCoff_Tax300/",
        //nun Runs mit ReichweitenConstraint
        "foodRetailing_with_rangeConstraint/21_ICEVBEV_NwCE_BVWP_10000it_DC_noTax/",
        "foodRetailing_with_rangeConstraint/22_ICEVBEV_NwCE_BVWP_10000it_DC_Tax25/",
        "foodRetailing_with_rangeConstraint/23_ICEVBEV_NwCE_BVWP_10000it_DC_Tax50/",
        "foodRetailing_with_rangeConstraint/24_ICEVBEV_NwCE_BVWP_10000it_DC_Tax100/",
        "foodRetailing_with_rangeConstraint/25_ICEVBEV_NwCE_BVWP_10000it_DC_Tax150/",
        "foodRetailing_with_rangeConstraint/26_ICEVBEV_NwCE_BVWP_10000it_DC_Tax200/",
        "foodRetailing_with_rangeConstraint/27_ICEVBEV_NwCE_BVWP_10000it_DC_Tax250/",
        "foodRetailing_with_rangeConstraint/28_ICEVBEV_NwCE_BVWP_10000it_DC_Tax300/"
    )   ;


    if (args.length == 0) {
      for (String runDir : listOfRuns) {
        String runDirectory = pathToRunDir + runDir;

        RunEmissionEventsAnalysis2024 analysis = new RunEmissionEventsAnalysis2024(
            runDirectory,
            runDirectory+"/analysis/1_emissions/");
        analysis.run();
      }
//      runDirectory = "/Users/kturner/git-and-svn/shared-svn/projects/freight/studies/UpdateEventsfromEarlierStudies/foodRetailing_wo_rangeConstraint/71_ICEVBEV_NwCE_BVWP_10000it_DCoff_noTax"; //KMT
    } else {
     String runDirectory = args[0];

      RunEmissionEventsAnalysis2024 analysis = new RunEmissionEventsAnalysis2024(
          runDirectory,
          runDirectory+"/analysis/1_emissions/");
      analysis.run();
    }
  }

  public RunEmissionEventsAnalysis2024(String runDirectory, String analysisOutputDirectory) {
    this.runDirectory = runDirectory;

    if (!analysisOutputDirectory.endsWith("/")) analysisOutputDirectory = analysisOutputDirectory + "/";
    this.analysisOutputDirectory = analysisOutputDirectory;
    new File(this.analysisOutputDirectory).mkdirs();
  }

  void run() throws IOException {

    Config config = ConfigUtils.createConfig();
    config.vehicles().setVehiclesFile(runDirectory + "/output_allVehicles.xml.gz");
    config.network().setInputFile(runDirectory + "/output_network.xml.gz");
    config.global().setCoordinateSystem("EPSG:31468");

//    final String emissionEventsFile =  runDirectory + "/emission.events.offline2.xml";
    final String emissionEventsFile =  runDirectory + "/emission.events.selected2Events.xml";


    final String linkEmissionAnalysisFile = analysisOutputDirectory  + "/emissionsPerLink.csv";
    final String linkEmissionPerMAnalysisFile = analysisOutputDirectory + "/emissionsPerLinkPerM.csv";
    final String vehicleEmissionAnalysisFile = analysisOutputDirectory  + "/emissionsPerVehicle.csv";
    final String vehicleTypeEmissionAnalysisFile = analysisOutputDirectory  + "/emissionsPerVehicleType.csv";

    Scenario scenario = ScenarioUtils.loadScenario(config);

    EventsManager eventsManager = EventsUtils.createEventsManager();

    EmissionsOnLinkEventHandler emissionsEventHandler = new EmissionsOnLinkEventHandler(48*3600.);
    eventsManager.addHandler(emissionsEventHandler);

    EmissionsPerVehicleEventHandler emissionsPerVehicleEventHandler = new EmissionsPerVehicleEventHandler();
    eventsManager.addHandler(emissionsPerVehicleEventHandler);

    eventsManager.initProcessing();
    EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);
    emissionReader.readFile(emissionEventsFile);
    eventsManager.finishProcessing();

    log.info("Done reading the events file.");
    log.info("Finish processing...");

    final Map<Id<Link>, Map<Pollutant, Double>> link2pollutants = emissionsEventHandler.getLink2pollutants();

    EmissionsWriterUtils.writePerLinkOutput(linkEmissionAnalysisFile, linkEmissionPerMAnalysisFile, scenario, link2pollutants);
    EmissionsWriterUtils.writePerVehicleOutput(vehicleEmissionAnalysisFile,vehicleTypeEmissionAnalysisFile,scenario, emissionsPerVehicleEventHandler);
    EmissionsWriterUtils.writePerPollutantOutput(analysisOutputDirectory  + "/emissionsPerPollutant.csv",
        link2pollutants);


    int totalVehicles = scenario.getVehicles().getVehicles().size();
    log.info("Total number of vehicles: " + totalVehicles);

  }

}
