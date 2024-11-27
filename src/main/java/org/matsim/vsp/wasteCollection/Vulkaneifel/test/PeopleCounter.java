package org.matsim.vsp.wasteCollection.Vulkaneifel.test;

import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.vsp.wasteCollection.Vulkaneifel.run.Schedule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.matsim.vsp.wasteCollection.Vulkaneifel.run.AbfallUtils.readCsvFile;


public class PeopleCounter {

    public static void main(String[] args) throws IOException {

        String planPath = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/vulkaneifel/v1.1/input/vulkaneifel-v1.1-25pct.plans.xml.gz";
        String shapeFilePath = "scenarios/equil/Shp_files_for_Vulkaneifel/Vulkaneifel_ALL_Gemeinde.shp";
        ShpOptions shpOptions = new ShpOptions(shapeFilePath, null, null);
        List<SimpleFeature> features = shpOptions.readFeatures();
//        var features = ShapeFileReader.getAllFeatures(shapeFilePath);
        String csvFilePath = "scenarios/equil/Abfalltermine_Vulkaneifel_BA.csv";
        List<Schedule> schedules = readCsvFile(csvFilePath); // Abfallsammlungtermine der jeweiligen Gemeinden und Städten
        for (Schedule schedule : schedules) {
            System.out.println("Name: " + schedule.getName() + ", Day: " + schedule.getDay() + ", Week: " + schedule.getWeek());
        }


        int volume = 50; // Müllmenge pro Haushalt in Kg
        int jspritIterations = 1; //jspritIteration default is 1 (MUST BE AT LEAST 1)
        int Gemeinde = 0;
        //int counter = 0;
        String[] week = {"Mo","Di","Mi","Do","Fr"}; // Mo Di Mi Do Fr
        String[] KW = {"U","G"};
        String[] VehicleTypes = {"EV_small_battery","diesel_vehicle","EV_medium_battery"};
        String choosenVehicleType = VehicleTypes[0];
        List<Integer> peoplecounter = new ArrayList<Integer>();
        List<Geometry> geometries = new ArrayList<>();

        //Config config = AbfallUtils.prepareConfig(output_type);
        //Scenario scenario = ScenarioUtils.loadScenario(config);
        for(String KWtype : KW) {
            // for(String choosentype: VehicleTypes){
            for (String day : week) {

                for (Schedule schedule : schedules) {
                    if (schedule.getDay().equals(day) & schedule.getWeek().equals(KWtype)) {
                        System.out.println("Name: " + schedule.getName());
                        Gemeinde++;

                        geometries.addAll(features.stream()
                                .filter(simpleFeature -> simpleFeature.getAttribute("GEN").equals(schedule.getName()))
                                .map(simpleFeature -> (Geometry) simpleFeature.getDefaultGeometry())
                                .toList());
                    }

                }
            }
        }

        List<Id<Person>> NotInVulkaneifel = new ArrayList<>();
        List<String> NotInVulkaneifel_link = new ArrayList<>();
        List<Id<Person>> InVulkaneifel = new ArrayList<>();
        int counter = 0;
        int counter2 = 0;
        Population population = PopulationUtils.readPopulation(planPath);
        for(Geometry gemeindeGeometries : geometries) {
            for (Person person : population.getPersons().values()) {
                for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
                    if (planElement instanceof Activity activity) {
                        if (activity.getType().contains("home")) {
                            var coord = activity.getCoord();
                            var geotoolsPoint = MGC.coord2Point(coord);

                            if (gemeindeGeometries.contains(geotoolsPoint)) {

                                if (InVulkaneifel.contains(person.getId())){
                                } else {
                                    counter++;
                                    //System.out.println("PersonID: " + person.getId().toString() + " counter: " + counter + " LinkID: " + activity.getLinkId().toString());
                                    InVulkaneifel.add(person.getId());
                                }
                            }
                        }
                    }
                }
            }
        }
                //System.out.println(Collections.max(peoplecounter));
                System.out.println("Gemeinde: " + Gemeinde);



        //System.out.println("Populationsize2: " + pop);
        //System.out.println(PopulationUtils.readPopulation(planPath).getPersons().values());


       /* System.out.println("in Vulkaneifel?");
        for(String text : InVulkaneifel){
            System.out.println(text);
        }*/



        for (Person person : population.getPersons().values()) {
            for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
                if (planElement instanceof Activity activity) {
                    if (activity.getType().contains("home")) {

                        if(!InVulkaneifel.contains(person.getId())) {
                            if(NotInVulkaneifel.contains(person.getId())){}
                            else {
                                counter2++;
                               // System.out.println("PersonID: " + person.getId().toString() + " counter: " + counter2 + " LinkID: " + activity.getLinkId().toString());
                                NotInVulkaneifel.add(person.getId());
                                if(NotInVulkaneifel_link.contains(activity.getLinkId().toString())){

                                }else {
                                    NotInVulkaneifel_link.add("PersonID: " + person.getId().toString() + " LinkID: " + activity.getLinkId().toString() + " LinkCoord: "+ activity.getCoord().toString());
                                }

                            }
                        }
                    }
                }
            }
        }




        System.out.println("in: " +counter);
        System.out.println("out: " +counter2);
        int result = counter + counter2;
        System.out.println("Total: " + result);
        System.out.println("Populationsize: "+ PopulationUtils.readPopulation(planPath).getPersons().size());
        //System.out.println(peoplecounter);
        //System.out.println("max: "+Collections.max(peoplecounter));
        System.out.println("size: " + NotInVulkaneifel_link.size());
        System.out.println("Links not in Vulkaneifel?");
        for(String text : NotInVulkaneifel_link){
            System.out.println(text);
        }

    }
}
