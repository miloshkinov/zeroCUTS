package org.matsim.vsp.freight.food;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierPlan;
import org.matsim.freight.carriers.CarrierPlanWriter;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;
import org.matsim.freight.carriers.ScheduledTour;
import org.matsim.freight.carriers.Tour;
import org.matsim.freight.carriers.analysis.RunFreightAnalysisEventBased;
import org.matsim.freight.carriers.controler.CarrierModule;

/**
 * Diese Klasse soll den Output von "alten" runs derart updaten,
 * dass nun die heutigen Event-based Analysen genutzt werden können.
 * D.h. insbesondere, dass die ganzen Carrier-Events nachgeworfen werden.
 * @author Kai Martins-Turner (kturner)
 */
public class UpdateRunOutputs {

  public static void main(String[] args) throws InterruptedException, ExecutionException {
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
        "foodRetailing_with_rangeConstraint/21_ICEVBEV_NwCE_BVWP_10000it_DCoff_noTax/",
        "foodRetailing_with_rangeConstraint/22_ICEVBEV_NwCE_BVWP_10000it_DCoff_Tax25/",
        "foodRetailing_with_rangeConstraint/23_ICEVBEV_NwCE_BVWP_10000it_DCoff_Tax50/",
        "foodRetailing_with_rangeConstraint/24_ICEVBEV_NwCE_BVWP_10000it_DCoff_Tax100/",
        "foodRetailing_with_rangeConstraint/25_ICEVBEV_NwCE_BVWP_10000it_DCoff_Tax150/",
        "foodRetailing_with_rangeConstraint/26_ICEVBEV_NwCE_BVWP_10000it_DCoff_Tax200/",
        "foodRetailing_with_rangeConstraint/27_ICEVBEV_NwCE_BVWP_10000it_DCoff_Tax250/",
        "foodRetailing_with_rangeConstraint/28_ICEVBEV_NwCE_BVWP_10000it_DCoff_Tax300/"
    )   ;
    for (String runDir : listOfRuns) {
        run(args, runDir);
    }
  }
  public static void run( String[] args, String runDir )
      throws InterruptedException, ExecutionException {

    // Path to public repo:
    String pathToInput = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/freight/" + runDir;
    // ### config stuff: ###
    Config config;
    if ( args==null || args.length==0 || args[0]==null ){
      config = ConfigUtils.createConfig();
      config.network().setInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/output-berlinv5.5/berlin-v5.5.3-10pct.output_network.xml.gz");
      config.controller().setOutputDirectory( "../shared-svn/projects/freight/studies/UpdateEventsfromEarlierStudies/" + runDir );
      config.controller().setLastIteration( 0 );  // no iterations; for iterations see RunFreightWithIterationsExample.  kai, jan'23
      config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
      config.global().setCoordinateSystem("EPSG:31468");

      FreightCarriersConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule( config, FreightCarriersConfigGroup.class );
      freightConfigGroup.setCarriersFile(  pathToInput + "output_carriers.xml.gz" );
      freightConfigGroup.setCarriersVehicleTypesFile( pathToInput + "output_vehicleTypes.xml.gz" );
    } else {
      config = ConfigUtils.loadConfig( args, new FreightCarriersConfigGroup() );
    }

    // load scenario (this is not loading the freight material):
    Scenario scenario = ScenarioUtils.loadScenario( config );

    //load carriers according to freight config
    CarriersUtils.loadCarriersAccordingToFreightConfig( scenario );

    // output before jsprit runDir (not necessary)
//    new CarrierPlanWriter(CarriersUtils.getCarriers( scenario )).write( "output/unmodifiedCarriers.xml" ) ;
    // (this will go into the standard "output" directory.  note that this may be removed if this is also used as the configured output dir.)

    for (Carrier carrier : CarriersUtils.addOrGetCarriers(scenario).getCarriers().values()) {
      final CarrierPlan selectedPlan = carrier.getSelectedPlan();

      //Add meaningful TourIds.
      int tourIdIndex = 1;
      Collection<ScheduledTour> updatedScheduledTours = new LinkedList<>();
      for (ScheduledTour scheduledTour : selectedPlan.getScheduledTours().stream().toList()) {
        updatedScheduledTours.add(ScheduledTour.newInstance(
            scheduledTour.getTour().duplicateWithNewId(Id.create(tourIdIndex, Tour.class)),
            scheduledTour.getVehicle(),
            scheduledTour.getDeparture() ));
        tourIdIndex++;
      }
      selectedPlan.getScheduledTours().clear();
      selectedPlan.getScheduledTours().addAll(updatedScheduledTours);

      //put score into JspritScore and Set MATSimscore to -INF.
      selectedPlan.setJspritScore(selectedPlan.getScore());
      selectedPlan.setScore(Double.NEGATIVE_INFINITY);
    }

//    new CarrierPlanWriter(CarriersUtils.getCarriers( scenario )).write( "output/updatedCarriersWithTourIds.xml" );

    // ## MATSim configuration:  ##
    final Controler controler = new Controler( scenario ) ;
    controler.addOverridingModule(new CarrierModule() );


    // ## Start of the MATSim-Run: ##
    controler.run();

    var analysis = new RunFreightAnalysisEventBased(config.controller().getOutputDirectory() , config.controller().getOutputDirectory()+"Analysis", "EPSG:31468");
    try {
      analysis.runAnalysis();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
