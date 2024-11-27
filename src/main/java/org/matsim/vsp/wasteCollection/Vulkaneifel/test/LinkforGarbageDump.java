package org.matsim.vsp.wasteCollection.Vulkaneifel.test;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

public class LinkforGarbageDump {

    public static void main(String[] args) {
        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile("C:\\Users\\phili\\IdeaProjects\\matsim-BA-Vulkaneifel\\scenarios\\equil\\vulkaneifel-v1.1-network.xml");

        double x = 336489.732692;
        double y = 5572815.489685;
        Coord coord = new Coord(x,y);

        String nearestLink = NetworkUtils.getNearestLinkExactly(network,coord).getId().toString();

        System.out.println(nearestLink);



    }

}
