package org.matsim.vsp.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmissionsOnLinkHandlerCommercial implements WarmEmissionEventHandler, ColdEmissionEventHandler {

	private final Map<Id<Link>, Map<Pollutant, Double>> link2pollutants = new HashMap<>();
	private final Map<Id<Vehicle>, Map<Pollutant, Double>> pollutantsPerVehicle = new HashMap<>();
	private final List<Id<Vehicle>> berlinFreightAgents = new ArrayList<>();
	private final Scenario scenario;


	public EmissionsOnLinkHandlerCommercial(Scenario scenario) {
		this.scenario = scenario;
		for (Person person: scenario.getPopulation().getPersons().values()){
			if(person.getAttributes().getAttribute("tourStartArea").toString().contains("Berlin_"))
				berlinFreightAgents.add(VehicleUtils.getVehicleIds(person).values().iterator().next());
		}
	}

	@Override
	public void reset(int iteration) {
		link2pollutants.clear();
		pollutantsPerVehicle.clear();
	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		if (berlinFreightAgents.contains(event.getVehicleId()))
			handleEmissionEvent(event.getTime(), event.getLinkId(), event.getWarmEmissions(), event.getVehicleId() );
	}

	@Override
	public void handleEvent(ColdEmissionEvent event) {
		if (berlinFreightAgents.contains(event.getVehicleId()))
			handleEmissionEvent(event.getTime(), event.getLinkId(), event.getColdEmissions(), event.getVehicleId());
	}


	private void handleEmissionEvent(double time, Id<Link> linkId, Map<Pollutant, Double> emissions,
									 Id<Vehicle> vehicleId) {
		for (Pollutant pollutant : emissions.keySet()) {
			if (emissions.get(pollutant) != 0) {
				pollutantsPerVehicle.computeIfAbsent(vehicleId, (k) -> new HashMap<>()).merge(pollutant, emissions.get(pollutant), Double::sum);
				link2pollutants.computeIfAbsent(linkId, (k) -> new HashMap<>()).merge(pollutant, emissions.get(pollutant), Double::sum);
			}
		}

	}

	public Map<Id<Link>, Map<Pollutant, Double>> getLink2pollutants() {
		return link2pollutants;
	}
	public Map<Id<Vehicle>, Map<Pollutant, Double>> getPollutantsPerVehicle() {
		return pollutantsPerVehicle;
	}
}
