package org.matsim.vsp.emissions;

import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.contrib.emissions.analysis.EmissionsOnLinkEventHandler;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.DetailedVsAverageLookupBehavior;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.NonScenarioVehicles;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * @author Kai Martins-Turner (kturner)
 */
public class RunFoodEmissions2024 {

  private static final Logger log = LogManager.getLogger(RunFoodEmissions2024.class);

  private final String runDirectory;
  private final String hbefaWarmFileDet;
  private final String analysisOutputDirectory;

  public static void main(String[] args) throws IOException {

    final String runDirectory = "/Users/kturner/git-and-svn/shared-svn/projects/freight/studies/UpdateEventsfromEarlierStudies/foodRetailing_wo_rangeConstraint/71_ICEVBEV_NwCE_BVWP_10000it_DCoff_noTax"; //KMT
    final String hbefaFileWarmDet = "original-input-data/HBEFA_summarized_final.csv"; //TODO: In verschlüsselte Dateien integrieren und ins public SVN laden. DAbei nochmal auf Spalten achten. mMn ist hier emConcept und Technology verdreht -.-

    RunFoodEmissions2024 analysis = new RunFoodEmissions2024(
        runDirectory,
        hbefaFileWarmDet,
        runDirectory);
    analysis.run();

  }

  public RunFoodEmissions2024(String runDirectory, String hbefaFileWarm,  String analysisOutputDirectory) {
    this.runDirectory = runDirectory;
    this.hbefaWarmFileDet = hbefaFileWarm;

    if (!analysisOutputDirectory.endsWith("/")) analysisOutputDirectory = analysisOutputDirectory + "/";
    this.analysisOutputDirectory = analysisOutputDirectory;
  }

  void run() throws IOException {

    Config config = ConfigUtils.createConfig();
    config.vehicles().setVehiclesFile(runDirectory + "/output_allVehicles.xml.gz");
    config.network().setInputFile(runDirectory + "/output_network.xml.gz");
    config.global().setCoordinateSystem("EPSG:31468");
//		config.global().setCoordinateSystem("GK4");
    config.plans().setInputFile(null);
    config.eventsManager().setNumberOfThreads(null);
    config.eventsManager().setEstimatedNumberOfEvents(null);
//    config.parallelEventHandling().setNumberOfThreads(null);
//    config.parallelEventHandling().setEstimatedNumberOfEvents(null);
    config.global().setNumberOfThreads(1);

    EmissionsConfigGroup eConfig = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);
    eConfig.setDetailedVsAverageLookupBehavior(
        DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable);
    eConfig.setHbefaTableConsistencyCheckingLevel(
        HbefaTableConsistencyCheckingLevel.none);  //KMT: Vielleicht nicht die beste Einstellung, aber das ist eine andere Baustelle ;)
//		eConfig.setAverageColdEmissionFactorsFile("https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/22823adc0ee6a0e231f35ae897f7b224a86f3a7a.enc"); //scheint nicht ganz die richtige Tabelle zu sein
    eConfig.setAverageColdEmissionFactorsFile(
        "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/r9230ru2n209r30u2fn0c9rn20n2rujkhkjhoewt84202.enc"); //daher nun ausnahmsweise doch mal als lokale Kopie, damit wir weiter kommen.
    eConfig.setDetailedColdEmissionFactorsFile(
        "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/5a297db51545335b2f7899002a1ea6c45d4511a3.enc");
    eConfig.setAverageWarmEmissionFactorsFile(
        "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/7eff8f308633df1b8ac4d06d05180dd0c5fdf577.enc");
    eConfig.setDetailedWarmEmissionFactorsFile(this.hbefaWarmFileDet);
//    eConfig.setHbefaRoadTypeSource(HbefaRoadTypeSource.fromLinkAttributes);
  eConfig.setNonScenarioVehicles(NonScenarioVehicles.ignore);


    final String eventsFile = runDirectory + "/output_events.xml.gz";

    final String emissionEventOutputFile =
        analysisOutputDirectory + "/emission.events.offline.xml.gz";
    final String linkEmissionAnalysisFile = analysisOutputDirectory  + "/emissionsPerLink.csv";
    final String linkEmissionPerMAnalysisFile = analysisOutputDirectory + "/emissionsPerLinkPerM.csv";
    final String vehicleTypeFile = analysisOutputDirectory  + "/emissionVehicleInformation.csv";
    final String vehicleEmissionAnalysisFile = analysisOutputDirectory  + "/emissionsPerVerhicle.csv";
    final String vehicleTypeEmissionAnalysisFile = analysisOutputDirectory  + "/emissionsPerVehicleType.csv";

    Scenario scenario = ScenarioUtils.loadScenario(config);
    // network
    for (Link link : scenario.getNetwork().getLinks().values()) {

      double freespeed = Double.NaN;

      if (link.getFreespeed() <= 13.888889) {
        freespeed = link.getFreespeed() * 2;
        // for non motorway roads, the free speed level was reduced
      } else {
        freespeed = link.getFreespeed();
        // for motorways, the original speed levels seems ok.
      }

      if (freespeed <= 8.333333333) { //30kmh
        link.getAttributes().putAttribute("hbefa_road_type", "URB/Access/30");
      } else if (freespeed <= 11.111111111) { //40kmh
        link.getAttributes().putAttribute("hbefa_road_type", "URB/Access/40");
      } else if (freespeed <= 13.888888889) { //50kmh
        double lanes = link.getNumberOfLanes();
        if (lanes <= 1.0) {
          link.getAttributes().putAttribute("hbefa_road_type", "URB/Local/50");
        } else if (lanes <= 2.0) {
          link.getAttributes().putAttribute("hbefa_road_type", "URB/Distr/50");
        } else if (lanes > 2.0) {
          link.getAttributes().putAttribute("hbefa_road_type", "URB/Trunk-City/50");
        } else {
          throw new RuntimeException("NoOfLanes not properly defined");
        }
      } else if (freespeed <= 16.666666667) { //60kmh
        double lanes = link.getNumberOfLanes();
        if (lanes <= 1.0) {
          link.getAttributes().putAttribute("hbefa_road_type", "URB/Local/60");
        } else if (lanes <= 2.0) {
          link.getAttributes().putAttribute("hbefa_road_type", "URB/Trunk-City/60");
        } else if (lanes > 2.0) {
          link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-City/60");
        } else {
          throw new RuntimeException("NoOfLanes not properly defined");
        }
      } else if (freespeed <= 19.444444444) { //70kmh
        link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-City/70");
      } else if (freespeed <= 22.222222222) { //80kmh
        link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-Nat./80");
      } else if (freespeed > 22.222222222) { //faster
        link.getAttributes().putAttribute("hbefa_road_type", "RUR/MW/>130");
      } else {
        throw new RuntimeException("Link not considered...");
      }
    }

    // vehicles
    {
      VehicleType heavy_26tVehicleType = scenario.getVehicles().getVehicleTypes()
          .get(Id.create("heavy26t", VehicleType.class));
      VehicleType heavy26t_electricityVehicleType = scenario.getVehicles().getVehicleTypes()
          .get(Id.create("heavy26t_electro", VehicleType.class));
      VehicleType heavy26t_frozenVehicleType = scenario.getVehicles().getVehicleTypes()
          .get(Id.create("heavy26t_frozen", VehicleType.class));
      VehicleType heavy26t_frozen_electricityVehicleType = scenario.getVehicles().getVehicleTypes()
          .get(Id.create("heavy26t_frozen_electro", VehicleType.class));
      VehicleType heavy40tVehicleType = scenario.getVehicles().getVehicleTypes()
          .get(Id.create("heavy40t", VehicleType.class));
      VehicleType heavy40t_electricityVehicleType = scenario.getVehicles().getVehicleTypes()
          .get(Id.create("heavy40t_electro", VehicleType.class));
      VehicleType light8tVehicleType = scenario.getVehicles().getVehicleTypes()
          .get(Id.create("light8t", VehicleType.class));
      VehicleType light8t_electricityVehicleType = scenario.getVehicles().getVehicleTypes()
          .get(Id.create("light8t_electro", VehicleType.class));
      VehicleType light8t_frozenVehicleType = scenario.getVehicles().getVehicleTypes()
          .get(Id.create("light8t_frozen", VehicleType.class));
      VehicleType light8t_frozen_electricityVehicleType = scenario.getVehicles().getVehicleTypes()
          .get(Id.create("light8t_frozen_electro", VehicleType.class));
      VehicleType medium18tVehicleType = scenario.getVehicles().getVehicleTypes()
          .get(Id.create("medium18t", VehicleType.class));
      VehicleType medium18t_electricityVehicleType = scenario.getVehicles().getVehicleTypes()
          .get(Id.create("medium18t_electro", VehicleType.class));

      if (heavy_26tVehicleType != null) {
        EngineInformation heavy_26tEngineInformation = heavy_26tVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(heavy_26tEngineInformation,
            HbefaVehicleCategory.HEAVY_GOODS_VEHICLE.toString());
        VehicleUtils.setHbefaTechnology(heavy_26tEngineInformation, "diesel");
        VehicleUtils.setHbefaSizeClass(heavy_26tEngineInformation, "RT >20-26t");
        VehicleUtils.setHbefaEmissionsConcept(heavy_26tEngineInformation, "average");
      }

      if (heavy26t_electricityVehicleType != null) {
        EngineInformation heavy26t_electricityEngineInformation = heavy26t_electricityVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(heavy26t_electricityEngineInformation,
            HbefaVehicleCategory.HEAVY_GOODS_VEHICLE.toString());
        VehicleUtils.setHbefaTechnology(heavy26t_electricityEngineInformation, "electricity");
        VehicleUtils.setHbefaSizeClass(heavy26t_electricityEngineInformation, "RT >12t");
        VehicleUtils.setHbefaEmissionsConcept(heavy26t_electricityEngineInformation, "average");
      }

      if (heavy26t_frozenVehicleType != null) {
        EngineInformation heavy26t_frozenEngineInformation = heavy26t_frozenVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(heavy26t_frozenEngineInformation,
            HbefaVehicleCategory.HEAVY_GOODS_VEHICLE.toString());
        VehicleUtils.setHbefaTechnology(heavy26t_frozenEngineInformation, "diesel");
        VehicleUtils.setHbefaSizeClass(heavy26t_frozenEngineInformation, "RT >20-26t");
        VehicleUtils.setHbefaEmissionsConcept(heavy26t_frozenEngineInformation, "average");
      }

      if (heavy26t_frozen_electricityVehicleType != null) {
        EngineInformation heavy26t_frozen_electricityEngineInformation = heavy26t_frozen_electricityVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(heavy26t_frozen_electricityEngineInformation,
            HbefaVehicleCategory.HEAVY_GOODS_VEHICLE.toString());
        VehicleUtils.setHbefaTechnology(heavy26t_frozen_electricityEngineInformation, "electricity");
        VehicleUtils.setHbefaSizeClass(heavy26t_frozen_electricityEngineInformation, "RT >12t");
        VehicleUtils.setHbefaEmissionsConcept(heavy26t_frozen_electricityEngineInformation,
            "average");
      }

      if (heavy40tVehicleType != null) {
        EngineInformation heavy40tEngineInformation = heavy40tVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(heavy40tEngineInformation,
            HbefaVehicleCategory.HEAVY_GOODS_VEHICLE.toString());
        VehicleUtils.setHbefaTechnology(heavy40tEngineInformation, "diesel");
        VehicleUtils.setHbefaSizeClass(heavy40tEngineInformation, "RT >32t");
        VehicleUtils.setHbefaEmissionsConcept(heavy40tEngineInformation, "average");
      }

      if (heavy40t_electricityVehicleType != null) {
        EngineInformation heavy40t_electricityEngineInformation = heavy40t_electricityVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(heavy40t_electricityEngineInformation,
            HbefaVehicleCategory.HEAVY_GOODS_VEHICLE.toString());
        VehicleUtils.setHbefaTechnology(heavy40t_electricityEngineInformation, "electricity");
        VehicleUtils.setHbefaSizeClass(heavy40t_electricityEngineInformation, "RT >12t");
        VehicleUtils.setHbefaEmissionsConcept(heavy40t_electricityEngineInformation, "average");
      }

      if (light8tVehicleType != null) {
        EngineInformation light8tEngineInformation = light8tVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(light8tEngineInformation,
            HbefaVehicleCategory.HEAVY_GOODS_VEHICLE.toString());
        VehicleUtils.setHbefaTechnology(light8tEngineInformation, "diesel");
        VehicleUtils.setHbefaSizeClass(light8tEngineInformation, "RT >7.5-12t");
        VehicleUtils.setHbefaEmissionsConcept(light8tEngineInformation, "average");
      }

      if (light8t_electricityVehicleType != null) {
        EngineInformation light8t_electricityEngineInformation = light8t_electricityVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(light8t_electricityEngineInformation,
            HbefaVehicleCategory.HEAVY_GOODS_VEHICLE.toString());
        VehicleUtils.setHbefaTechnology(light8t_electricityEngineInformation, "electricity");
        VehicleUtils.setHbefaSizeClass(light8t_electricityEngineInformation, "RT >7.5-12t");
        VehicleUtils.setHbefaEmissionsConcept(light8t_electricityEngineInformation, "average");
      }

      if (light8t_frozenVehicleType != null) {
        EngineInformation light8t_frozenEngineInformation = light8t_frozenVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(light8t_frozenEngineInformation,
            HbefaVehicleCategory.HEAVY_GOODS_VEHICLE.toString());
        VehicleUtils.setHbefaTechnology(light8t_frozenEngineInformation, "diesel");
        VehicleUtils.setHbefaSizeClass(light8t_frozenEngineInformation, "RT >7.5-12t");
        VehicleUtils.setHbefaEmissionsConcept(light8t_frozenEngineInformation, "average");
      }

      if (light8t_frozen_electricityVehicleType != null) {
        EngineInformation light8t_frozen_electricityEngineInformation = light8t_frozen_electricityVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(light8t_frozen_electricityEngineInformation,
            HbefaVehicleCategory.HEAVY_GOODS_VEHICLE.toString());
        VehicleUtils.setHbefaTechnology(light8t_frozen_electricityEngineInformation, "electricity");
        VehicleUtils.setHbefaSizeClass(light8t_frozen_electricityEngineInformation, "RT >7.5-12t");
        VehicleUtils.setHbefaEmissionsConcept(light8t_frozen_electricityEngineInformation, "average");
      }

      if (medium18tVehicleType != null) {
        EngineInformation medium18tEngineInformation = medium18tVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(medium18tEngineInformation,
            HbefaVehicleCategory.HEAVY_GOODS_VEHICLE.toString());
        VehicleUtils.setHbefaTechnology(medium18tEngineInformation, "diesel");
        VehicleUtils.setHbefaSizeClass(medium18tEngineInformation, "RT >14-20t");
        VehicleUtils.setHbefaEmissionsConcept(medium18tEngineInformation, "average");
      }

      if (medium18t_electricityVehicleType != null) {
        EngineInformation medium18t_electricityEngineInformation = medium18t_electricityVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(medium18t_electricityEngineInformation,
            HbefaVehicleCategory.HEAVY_GOODS_VEHICLE.toString());
        VehicleUtils.setHbefaTechnology(medium18t_electricityEngineInformation, "electricity");
        VehicleUtils.setHbefaSizeClass(medium18t_electricityEngineInformation, "RT >12t");
        VehicleUtils.setHbefaEmissionsConcept(medium18t_electricityEngineInformation, "average");
      }
    }

    // the following is copy paste from the example...

    EventsManager eventsManager = EventsUtils.createEventsManager();

    AbstractModule module = new AbstractModule(){
      @Override
      public void install(){
        bind( Scenario.class ).toInstance( scenario );
        bind( EventsManager.class ).toInstance( eventsManager );
        bind( EmissionModule.class ) ;
      }
    };

    com.google.inject.Injector injector = Injector.createInjector(config, module);

    EmissionModule emissionModule = injector.getInstance(EmissionModule.class);

    EventWriterXML emissionEventWriter = new EventWriterXML(emissionEventOutputFile);
    emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);


    EmissionsOnLinkEventHandler emissionsEventHandler = new EmissionsOnLinkEventHandler(24*3600.);
    eventsManager.addHandler(emissionsEventHandler);

    EmissionsPerVehicleEventHandler emissionsPerVehicleEventHandler = new EmissionsPerVehicleEventHandler();
    eventsManager.addHandler(emissionsPerVehicleEventHandler);

    eventsManager.initProcessing();
    MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
    matsimEventsReader.readFile(eventsFile);
    eventsManager.finishProcessing();

    log.info("Done reading the events file.");
    log.info("Finish processing...");


    EmissionsWriterUtils.writePerLinkOutput(linkEmissionAnalysisFile, linkEmissionPerMAnalysisFile, scenario, emissionsEventHandler);
    EmissionsWriterUtils.writeEmissionConceptAssignmentOutput(vehicleTypeFile, scenario, emissionsEventHandler);
    EmissionsWriterUtils.writePerVehicleOutput(vehicleEmissionAnalysisFile,vehicleTypeEmissionAnalysisFile,scenario,emissionsPerVehicleEventHandler);


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
