package org.matsim.vsp.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LongDistanceAnalysisEventHandler implements LinkLeaveEventHandler, VehicleEntersTrafficEventHandler {

    private final HashMap<Id<Vehicle>, Id<Person>> vehiclePersonConnection = new HashMap<>();
    private final HashMap<String, IdMap<Link, Integer>> linksPerType = new HashMap<>();
    private final List<String> geographicalTypes = new ArrayList<>();

    private final Scenario scenario;

    public LongDistanceAnalysisEventHandler(Scenario scenario) {
        this.scenario = scenario;
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        Person person = scenario.getPopulation().getPersons().get(vehiclePersonConnection.get(event.getVehicleId()));
        String geographicalType = (String) person.getAttributes().getAttribute("geographicalType");
        linksPerType.computeIfAbsent(geographicalType, (k) -> new IdMap<>(Link.class));
        linksPerType.computeIfAbsent("all", (k) -> new IdMap<>(Link.class));

        if (!geographicalTypes.contains(geographicalType)) {
            geographicalTypes.add(geographicalType);
        }
        linksPerType.get("all").put(event.getLinkId(), linksPerType.get("all").getOrDefault(event.getLinkId(), 0) + 1);
        linksPerType.get(geographicalType).put(event.getLinkId(), linksPerType.get(geographicalType).getOrDefault(event.getLinkId(), 0) + 1);
    }


    @Override
    public void handleEvent(VehicleEntersTrafficEvent vehicleEntersTrafficEvent) {
        vehiclePersonConnection.computeIfAbsent(vehicleEntersTrafficEvent.getVehicleId(), (k) -> vehicleEntersTrafficEvent.getPersonId());
    }

    public Integer getVolumesPerType(String geographicalType, Id<Link> linkId) {
        return this.linksPerType.get(geographicalType).getOrDefault(linkId, 0);
    }

    public List<String> getGeographicalTypes() {
        return this.geographicalTypes;
    }

    @Override
    public void reset(final int iteration) {
        this.linksPerType.clear();
    }

}
