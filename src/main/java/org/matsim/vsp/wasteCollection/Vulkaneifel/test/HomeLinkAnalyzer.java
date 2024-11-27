package org.matsim.vsp.wasteCollection.Vulkaneifel.test;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.MutableScenario;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
public class HomeLinkAnalyzer {
    public static void main(String[] args) {
        String plansFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/vulkaneifel/v1.1/input/vulkaneifel-v1.1-25pct.plans.xml.gz"; // Replace with the actual path to your plans.xml

        // Load the scenario
        Config config = ConfigUtils.createConfig();
        MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);

        // Read the population from the plans file
        PopulationReader populationReader = new PopulationReader(scenario);
        populationReader.readFile(plansFile);
        Population population = scenario.getPopulation();

        // Map to store link IDs and the corresponding set of person IDs
        Map<Id, Set<Id<Person>>> linkToPersonsMap = new HashMap<>();

        // Iterate over each person in the population
        for (Person person : population.getPersons().values()) {
            for (Plan plan : person.getPlans()) {
                for (Activity activity : plan.getPlanElements().stream()
                        .filter(pe -> pe instanceof Activity)
                        .map(pe -> (Activity) pe)
                        .toList()) {
                    if (activity.getType().contains("home")) {
                        Id linkId = activity.getLinkId();
                        linkToPersonsMap.putIfAbsent(linkId, new HashSet<>());
                        linkToPersonsMap.get(linkId).add(person.getId());
                    }
                }
            }
        }

        // List link IDs with multiple persons linked to them as home
        for (Map.Entry<Id, Set<Id<Person>>> entry : linkToPersonsMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                System.out.println("Link ID: " + entry.getKey() + " is linked to multiple persons: " + entry.getValue());
            }
        }
    }
}
