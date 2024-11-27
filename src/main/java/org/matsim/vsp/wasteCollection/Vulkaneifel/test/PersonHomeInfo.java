package org.matsim.vsp.wasteCollection.Vulkaneifel.test;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class PersonHomeInfo {
    private String personId;
    private Coord homeCoordinates;
    private String homeLink;

    public PersonHomeInfo(String personId, Coord homeCoordinates, String homeLink){
        this.personId = personId;
        this.homeCoordinates = homeCoordinates;
        this.homeLink = homeLink;
    }
    public String getPersonId() {
        return personId;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    public Coord getHomeCoordinates() {
        return homeCoordinates;
    }

    public void setHomeCoordinates(Coord homeCoordinates) {
        this.homeCoordinates = homeCoordinates;
    }

    public String getHomeLink() {
        return homeLink;
    }

    public void setHomeLink(String homeLink) {
        this.homeLink = homeLink;
    }
    @Override
    public String toString() {
        return "PersonHomeInfo{" +
                "personId='" + personId + '\'' +
                ", homeCoordinates=" + homeCoordinates +
                ", homeLink='" + homeLink + '\'' +
                '}';
    }
}

