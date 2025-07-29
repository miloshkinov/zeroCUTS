package org.matsim.vsp.analysis;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommercialAnalysisEventHandler implements LinkLeaveEventHandler {

	private final List<Id<Vehicle>> berlinFreightAgents = new ArrayList<>();
	private final Map<Id<Vehicle>, Object2DoubleMap<String>> tourInformation = new HashMap<>();
	private final Scenario scenario;


	public CommercialAnalysisEventHandler(Scenario scenario) {
		this.scenario = scenario;
		for (Person person: scenario.getPopulation().getPersons().values()){
			if(person.getAttributes().getAttribute("tourStartArea").toString().contains("Berlin_"))
				berlinFreightAgents.add(VehicleUtils.getVehicleIds(person).values().iterator().next());
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (berlinFreightAgents.contains(event.getVehicleId()))
			tourInformation.computeIfAbsent(event.getVehicleId(), (k) -> new Object2DoubleOpenHashMap<>()).mergeDouble("drivenDistance",scenario.getNetwork().getLinks().get(event.getLinkId()).getLength(), Double::sum);
	}

	public Map<Id<Vehicle>, Object2DoubleMap<String>> getTourInformation() {
		return tourInformation;
	}
}
