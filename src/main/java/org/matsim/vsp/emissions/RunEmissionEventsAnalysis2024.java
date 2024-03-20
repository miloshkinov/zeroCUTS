package org.matsim.vsp.emissions;

import java.io.File;
import java.io.IOException;
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

    String runDirectory;
    if (args.length == 0) {
      runDirectory = "/Users/kturner/git-and-svn/shared-svn/projects/freight/studies/UpdateEventsfromEarlierStudies/foodRetailing_wo_rangeConstraint/71_ICEVBEV_NwCE_BVWP_10000it_DCoff_noTax"; //KMT
    } else {
      runDirectory = args[0];
    }
    RunEmissionEventsAnalysis2024 analysis = new RunEmissionEventsAnalysis2024(
        runDirectory,
        runDirectory+"/emissionsAnalysis/");
    analysis.run();

  }

  public RunEmissionEventsAnalysis2024(String runDirectory, String analysisOutputDirectory) {
    this.runDirectory = runDirectory;

    if (!analysisOutputDirectory.endsWith("/")) analysisOutputDirectory = analysisOutputDirectory + "/";
    this.analysisOutputDirectory = analysisOutputDirectory;
    new File(this.analysisOutputDirectory).mkdir();
  }

  void run() throws IOException {

    Config config = ConfigUtils.createConfig();
    config.vehicles().setVehiclesFile(runDirectory + "/output_allVehicles.xml.gz");
    config.network().setInputFile(runDirectory + "/output_network.xml.gz");
    config.global().setCoordinateSystem("EPSG:31468");

    final String emissionEventsFile =  runDirectory + "/emission.events.offline2.xml";

    final String linkEmissionAnalysisFile = analysisOutputDirectory  + "/emissionsPerLink.csv";
    final String linkEmissionPerMAnalysisFile = analysisOutputDirectory + "/emissionsPerLinkPerM.csv";
//    final String vehicleTypeFile = analysisOutputDirectory  + "/emissionVehicleInformation.csv";
    final String vehicleEmissionAnalysisFile = analysisOutputDirectory  + "/emissionsPerVehicle.csv";
    final String vehicleTypeEmissionAnalysisFile = analysisOutputDirectory  + "/emissionsPerVehicleType.csv";

    Scenario scenario = ScenarioUtils.loadScenario(config);


    EventsManager eventsManager = EventsUtils.createEventsManager();


//    AbstractModule module = new AbstractModule(){
//      @Override
//      public void install(){
//        bind( Scenario.class ).toInstance( scenario );
//        bind( EventsManager.class ).toInstance( eventsManager );
//        bind( EmissionModule.class ) ;
//      }
//    };
//
//    com.google.inject.Injector injector = Injector.createInjector(config, module);

    EmissionsOnLinkEventHandler emissionsEventHandler = new EmissionsOnLinkEventHandler(48*3600.);
    eventsManager.addHandler(emissionsEventHandler);

    EmissionsPerVehicleEventHandler emissionsPerVehicleEventHandler = new EmissionsPerVehicleEventHandler();
    eventsManager.addHandler(emissionsPerVehicleEventHandler);

    eventsManager.initProcessing();
//    MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
//    matsimEventsReader.readFile(emissionEventsFile);
    EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);
    emissionReader.readFile(emissionEventsFile);
    eventsManager.finishProcessing();

    log.info("Done reading the events file.");
    log.info("Finish processing...");

    final Map<Id<Link>, Map<Pollutant, Double>> link2pollutants = emissionsEventHandler.getLink2pollutants();

    EmissionsWriterUtils.writePerLinkOutput(linkEmissionAnalysisFile, linkEmissionPerMAnalysisFile, scenario, link2pollutants);
//    EmissionsWriterUtils.writeEmissionConceptAssignmentOutput(vehicleTypeFile, scenario, emissionsEventHandler);
    EmissionsWriterUtils.writePerVehicleOutput(vehicleEmissionAnalysisFile,vehicleTypeEmissionAnalysisFile,scenario, emissionsPerVehicleEventHandler);
    EmissionsWriterUtils.writePerPollutantOutput(analysisOutputDirectory  + "/emissionsPerPollutant.csv",
        link2pollutants);


    int totalVehicles = scenario.getVehicles().getVehicles().size();
    log.info("Total number of vehicles: " + totalVehicles);


//    scenario.getVehicles().getVehicles().values().stream()
//        .map(vehicle -> vehicle.getType())
//        .collect(Collectors.groupingBy(category -> category, Collectors.counting()))
//        .entrySet()
//        .forEach(entry -> log.info("nr of " + VehicleUtils.getHbefaVehicleCategory(entry.getKey().getEngineInformation()) + " vehicles running on " + VehicleUtils.getHbefaEmissionsConcept(entry.getKey().getEngineInformation())
//            +" = " + entry.getValue() + " (equals " + ((double)entry.getValue()/(double)totalVehicles) + "% overall)"));
//

  }

}
