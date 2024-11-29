package org.matsim.vsp.wasteCollection.Vulkaneifel.run;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.ControllerUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.analysis.RunFreightAnalysisEventBased;
import org.matsim.freight.carriers.controler.CarrierModule;
import org.matsim.freight.carriers.controler.CarrierScoringFunctionFactory;
import picocli.CommandLine;


import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.matsim.vsp.wasteCollection.Vulkaneifel.run.AbfallUtils.*;


/**
 * @Author Philip_Cuong_Minh_Nguyen (Bachelor thesis), modified by Ricardo Ewert
 */
public class RunWasteCollectionVulkaneifel implements MATSimAppCommand {

    static final Logger log = LogManager.getLogger(RunWasteCollectionVulkaneifel.class);

    enum VehicleFleet {
        diesel_vehicle,
        EV_small_battery,
        EV_medium_battery,
        EVMix,
        MixAll
    }

    @CommandLine.Option(names = "--planInputFile", description = "Path to the plans input file",
            defaultValue = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/vulkaneifel/v1.2/input/vulkaneifel-v1.2-25pct.plans-initial.xml.gz")
    private String planPath;

    @CommandLine.Option(names = "--outputFolder", description = "Path to the output folder",
            defaultValue = "output/WasteCollectionVulkaneifel/")
    private String output;

    @CommandLine.Option(names = "--vehicleFleet", description = "Set the possible vehicle fleet. Options: diesel_vehicle, EV_small_battery, EV_medium_battery, EVMix, MixAll",
            defaultValue = "diesel_vehicle")
    private VehicleFleet vehicleFleet;

    @CommandLine.Option(names = "--jspritIterations", description = "Number of jsprit iterations",
            defaultValue = "1")
    private int jspritIterations;

    @CommandLine.Option(names = "--weekday", description = "Weekday for waste collection: Mo, Di, Mi, Do, Fr", required = true, defaultValue = "Mo")
    private String weekday;

    @CommandLine.Option(names = "--weekRhythm", description = "Week rhythm for waste collection (even (G) or odd (U) week)", required = true, defaultValue = "G")
    private String weekRhythm;

    public static void main(String[] args) {
        System.exit(new CommandLine(new RunWasteCollectionVulkaneifel()).execute(args));
    }

    @Override
    public Integer call() throws Exception {

        String shapeFilePath = "scenarios/wasteCollection/Vulkaneifel/Shp_files_for_Vulkaneifel/Vulkaneifel_ALL_Gemeinde.shp";
        ShpOptions shpOptions = new ShpOptions(shapeFilePath, null, null);
        List<SimpleFeature> features = shpOptions.readFeatures();
        ShpOptions.Index shpIndex = shpOptions.createIndex("GEN");
        String csvFilePath = "scenarios/wasteCollection/Vulkaneifel/Abfalltermine_Vulkaneifel_BA.csv";
        List<Schedule> schedules = readCsvFile(csvFilePath); // Abfallsammlungstermine der jeweiligen Gemeinden und Städten
        for (Schedule schedule : schedules) {
            log.info("Name: {}, Day: {}, Week: {}", schedule.getName(), schedule.getDay(), schedule.getWeek());
        }


        int volumePer4Persons = 52; // Müllmenge pro Haushalt (4 Personen) in Kg; analog zu dem 25% Szenario
        int GemeindeWithCollection = 0;
        int totalCounter = 0;

        String Output_suffix = "/" + weekday + "_" + weekRhythm + "_"+vehicleFleet + "_"+ "Iterations_"+ jspritIterations ;
        output = output + Output_suffix;

        Config config = AbfallUtils.prepareConfig(output);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
        freightCarriersConfigGroup.setUseDistanceConstraintForTourPlanning(FreightCarriersConfigGroup.UseDistanceConstraintForTourPlanning.basedOnEnergyConsumption);

        CarrierVehicleTypes carrierVehicleTypes = CarriersUtils.getCarrierVehicleTypes(scenario);
        new CarrierVehicleTypeReader( carrierVehicleTypes ).readURL( IOUtils.extendUrl(scenario.getConfig().getContext(), freightCarriersConfigGroup.getCarriersVehicleTypesFile()) );
        Carriers carriers = CarriersUtils.addOrGetCarriers(scenario);

        AbfallUtils.createCarriers(carriers, jspritIterations);

        createFreightVehiclesSingleType(scenario, vehicleFleet); // just one type

        Population population = PopulationUtils.readPopulation(planPath);
        for (Schedule schedule : schedules){
            if(schedule.getDay().equals(weekday) & schedule.getWeek().equals(weekRhythm)) {
                log.info("Waste collection for {} on {} in week {}", schedule.getName(), weekday, weekRhythm);
                log.info("Name: {}", schedule.getName());
                GemeindeWithCollection++;

                createJobs(scenario, shpIndex, schedule.getName(), population, volumePer4Persons, totalCounter);
            } else {
                log.info("{} not in {} and KW {}", schedule.getName(), weekday, weekRhythm);}
        }
        log.info("GemeindeWithCollection: {}", GemeindeWithCollection);
        log.info("Counted Households: {}", totalCounter);
        CarriersUtils.writeCarriers(carriers, output + "/output_carriersNoSolution.xml.gz");

        try {
            CarriersUtils.runJsprit(scenario);
        } catch (ExecutionException e) {
            // Hier können Sie die Ausnahme spezifisch behandeln. Zum Beispiel:
            System.err.println("Ein Fehler ist während der Ausführung von runJsprit aufgetreten: " + e.getMessage());
            e.printStackTrace();  // Protokollieren des Stacktrace für Debugging-Zwecke
        } catch (InterruptedException e) {
            // Behandlung der InterruptedException
            System.err.println("Die Ausführung von runJsprit wurde unterbrochen: " + e.getMessage());
            e.printStackTrace();
            // Wiederherstellung des unterbrochenen Status
            Thread.currentThread().interrupt();
        }

        Controller controller = ControllerUtils.createController(scenario);
        controller.addOverridingModule(new CarrierModule());
        controller.addOverridingModule(new AbstractModule() {
            @Override public void install() {
                bind(CarrierScoringFunctionFactory.class).toInstance(new CarrierScoringFunctionFactory_KeepScore());
            }
        });

        controller.run();

        // kAnalysis
        log.info("Starting Analysis");
        RunFreightAnalysisEventBased freightAnalysis = new RunFreightAnalysisEventBased(output +"/", output +"/Analysis_new/", config.global().getCoordinateSystem());
        freightAnalysis.runCompleteAnalysis();
        log.info("Finished Analysis");

        return 0;
    }

    private static class CarrierScoringFunctionFactory_KeepScore implements CarrierScoringFunctionFactory {
        @Override public ScoringFunction createScoringFunction(Carrier carrier ){
            return new ScoringFunction(){
                @Override public void handleActivity( Activity activity ){
                }
                @Override public void handleLeg( Leg leg ){
                }
                @Override public void agentStuck( double time ){
                }
                @Override public void addMoney( double amount ){
                }
                @Override public void addScore( double amount ){
                }
                @Override public void finish(){
                }
                @Override public double getScore(){
                    return CarriersUtils.getJspritScore(carrier.getSelectedPlan()); //2nd Quickfix: Keep the current score -> which ist normally the score from jsprit. -> Better safe JspritScore as own value.
//					return Double.MIN_VALUE; // 1st Quickfix, to have a "double" value for xsd (instead of neg.-Infinity).
//					return Double.NEGATIVE_INFINITY; // Default from KN -> causes errors with reading in carrierFile because Java writes "Infinity", while XSD needs "INF"
                }
                @Override public void handleEvent( Event event ){
                }
            };
        }
    }
}
