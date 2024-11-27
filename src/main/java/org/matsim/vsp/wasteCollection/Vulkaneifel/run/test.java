package org.matsim.vsp.wasteCollection.Vulkaneifel.run;

import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.analysis.RunFreightAnalysisEventBased;
import org.matsim.freight.carriers.controler.CarrierModule;
import org.matsim.freight.carriers.controler.CarrierScoringFunctionFactory;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.matsim.vsp.wasteCollection.Vulkaneifel.run.AbfallUtils.*;

public class test { // copy of Runabfall but it does the whole analysis in 1 run

    public static void main(String[] args) throws IOException {
        String planPath = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/vulkaneifel/v1.1/input/vulkaneifel-v1.1-25pct.plans.xml.gz";
        String shapeFilePath = "scenarios/equil/Shp_files_for_Vulkaneifel/Vulkaneifel_ALL_Gemeinde.shp";
        ShpOptions shpOptions = new ShpOptions(shapeFilePath, null, null);
        List<SimpleFeature> features = shpOptions.readFeatures();
//        var features = ShapeFileReader.getAllFeatures(shapeFilePath);
        String csvFilePath = "scenarios/equil/Abfalltermine_Vulkaneifel_BA.csv";
        List<Schedule> schedules = readCsvFile(csvFilePath); // Abfallsammlungtermine der jeweiligen Gemeinden und Städten
        //String output = "C:\\Users\\phili\\iCloudDrive\\Studium Wirtschaftsingenieurwesen\\MATSim_BA\\Vulkaneifel_Data";
        String output = "output/RunAbfall_Output_V4_final_45minbreak_125Kwh_maintenance"; // _new with shortened range districtrion

        //int jspritIterations = 1;
        Population population = PopulationUtils.readPopulation(planPath);
        int volume = 52; // Müllmenge pro Haushalt in kg

        //int counter = 0; //PersonanzahlCounter
        /*
        for (Schedule schedule : schedules) {
            System.out.println("Name: " + schedule.getName() + ", Day: " + schedule.getDay() + ", Week: " + schedule.getWeek());
        }
        */
        int[] iterations = { 1000, 100 }; //100, 1000, 10000, 15000 10000,
        String[] week ={"Mi"};//{"Mo","Di","Mi","Do","Fr"}; // Mo Di Mi Do Fr
        String[] KW = {"G"}; //{"U","G"};   // U or G
        String[] VehicleTypes = {"EV_medium_battery", "EV_small_battery", "diesel_vehicle"}; //"EV_medium_battery", "diesel_vehicle","EV_small_battery",
       // String[] VehicleTypes = {"17_1s_only_EVs"}; //
        //String choosentype = VehicleTypes[];

        for(int jspritIterations : iterations) {
            for (String KWtype : KW) {
                for (String choosentype : VehicleTypes) {
                    for (String day : week) {

                        //String Output_suffix = "\\"+ day + "_" + KWtype +"_"+choosentype;
                        String Output_suffix = "/" + day + "_" + KWtype + "/"+"Iterations_" + jspritIterations + "_" + choosentype;
                        String output_type = output + Output_suffix;
                        Config config = AbfallUtils.prepareConfig(output_type);
                        //int numberOfThreads = Integer.parseInt(System.getProperty("numberOf-Threads","1"));
                        //config.global().setNumberOfThreads(numberOfThreads);
                        Scenario scenario = ScenarioUtils.loadScenario(config);
                        int Gemeinde = 0;
                        int totalcounter = 0;
                        FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
                        CarrierVehicleTypes carrierVehicleTypes = CarriersUtils.getCarrierVehicleTypes(scenario);
                        new CarrierVehicleTypeReader(carrierVehicleTypes).readURL(IOUtils.extendUrl(scenario.getConfig().getContext(), freightCarriersConfigGroup.getCarriersVehicleTypesFile()));
                        freightCarriersConfigGroup.setUseDistanceConstraintForTourPlanning(FreightCarriersConfigGroup.UseDistanceConstraintForTourPlanning.basedOnEnergyConsumption);

                        Carriers carriers = CarriersUtils.addOrGetCarriers(scenario);
                        AbfallUtils.createCarriers(carriers, jspritIterations);



                        //createFreightVehicles(scenario,carrierVehicleTypes); //all vehicletypes available
                       createFreightVehiclesSingleType(scenario, carrierVehicleTypes, choosentype); // just one type
                        //createFreightVehicles_OnlyEVs(scenario,carrierVehicleTypes);


                        for (Schedule schedule : schedules) {
                            if (schedule.getDay().equals(day) & schedule.getWeek().equals(KWtype)) {
                                System.out.println("Name: " + schedule.getName());
                                Gemeinde++;


                                var geometries = features.stream()
                                        .filter(simpleFeature -> simpleFeature.getAttribute("GEN").equals(schedule.getName()))
                                        .map(simpleFeature -> (Geometry) simpleFeature.getDefaultGeometry())
                                        .toList();
                                var gemeindeGeomtries = geometries.get(0);

                                //createJobswithsphapes(scenario, gemeindeGeomtries, population, volume, choosentype);
                                //totalcounter = createJobswithsphapes_V2(scenario,gemeindeGeomtries, population, volume, choosenVehicleType, totalcounter);
                               // totalcounter = createJobswithsphapes_V3(scenario,gemeindeGeomtries, population, volume, choosentype, totalcounter);
                                totalcounter = createJobswithsphapes_V4(scenario,gemeindeGeomtries, population, volume, totalcounter);

                            } else {
                                System.out.println(schedule.getName() + " not in " + day + " and KW " + KWtype);
                            }
                        }
                        System.out.println("Gemeinde: " + Gemeinde);
                        System.out.println("Households: " + totalcounter);
                        CarriersUtils.writeCarriers(carriers, output_type + "/output_carriersNoSolution.xml.gz");

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

                        Controler controler = new Controler(scenario);
                        controler.addOverridingModule(new CarrierModule());
                        controler.addOverridingModule(new AbstractModule() {
                            @Override
                            public void install() {
                                bind(CarrierScoringFunctionFactory.class).toInstance(new test.CarrierScoringFunctionFactory_KeepScore());
                            }
                        });

                        controler.run();

                        // kAnalysis
                        System.out.println("Starting Analysis");
                        RunFreightAnalysisEventBased freightAnalysis = new RunFreightAnalysisEventBased(output_type + "/", output_type + "/Analysis_new/", config.global().getCoordinateSystem());
                        freightAnalysis.runCompleteAnalysis();
                        System.out.println("Finished Analysis");


                    }
                }
            }

        }
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for (Thread t : threadSet) {
            System.out.println(t.getName() + " is " + (t.isDaemon() ? "daemon" : "non-daemon"));
        }

        // Forcefully exit after execution
        System.out.println("Forcing exit after controler.run() if non-daemon threads persist.");
        System.exit(0);
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
