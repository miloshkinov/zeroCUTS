package org.matsim.vsp.wasteCollection.Vulkaneifel.test;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.ArrayList;
import java.util.List;

public class PersonHomeInfoExtractor {

    public static List<PersonHomeInfo> extractPersonHomeInfo(String plansFilePath) {
        // Initialize the MATSim scenario
        Config config = ConfigUtils.createConfig();
        config.plans().setInputFile(plansFilePath);
        config.global().setCoordinateSystem("EPSG:25832");
        Scenario scenario = ScenarioUtils.loadScenario(config);

        List<PersonHomeInfo> personHomeInfoList = new ArrayList<>();

        for (Person person : scenario.getPopulation().getPersons().values()) {
            for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
                if (planElement instanceof Activity) {
                    Activity activity = (Activity) planElement;
                    if (activity.getType().contains("home")) {
                        // Extract home coordinates and home link (if available)
                        Coord homeCoord = activity.getCoord();
                        String homeLink = activity.getLinkId().toString(); // Replace with actual link extraction logic if available

                        // Create and add PersonHomeInfo object
                        personHomeInfoList.add(new PersonHomeInfo(person.getId().toString(), homeCoord, homeLink));
                        break; // Assuming only one home activity per person
                    }
                }
            }
        }

        return personHomeInfoList;
    }

    public static void main(String[] args) {
        String plansFilePath = "C:\\Users\\phili\\IdeaProjects\\matsim-BA-Vulkaneifel\\scenarios\\equil\\vulkaneifel-v1.1-25pct.plans.xml";
        List<PersonHomeInfo> personHomeInfoList = extractPersonHomeInfo(plansFilePath);

        for (PersonHomeInfo personHomeInfo : personHomeInfoList) {
            System.out.println(personHomeInfo);
        }


        System.out.println(personHomeInfoList.size());


        }
    }

