package org.matsim.vsp.wasteCollection.Vulkaneifel.test;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
public class NetworkFilter {
    public static void main(String[] args) {
        // Define your state's geographic boundaries
        double minLat =5462831.423228515, minLon = 893009.2412629615, maxLat = 5520698.59141344, maxLon = 1058241.9367456622;

        // Load the entire network
        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile("C:\\Users\\phili\\IdeaProjects\\matsim-BA-Vulkaneifel\\scenarios\\equil\\vulkaneifel-v1.1-network.xml");

        // Create a new network for the filtered data
        Network stateNetwork = NetworkUtils.createNetwork();

        // Filter and add nodes and links to the state network
        network.getNodes().values().stream()
                .filter(node -> isWithinBounds(node.getCoord(), minLat, minLon, maxLat, maxLon))
                .forEach(node -> stateNetwork.addNode(node));

        network.getLinks().values().stream()
                .filter(link -> stateNetwork.getNodes().containsKey(link.getFromNode().getId()) &&
                        stateNetwork.getNodes().containsKey(link.getToNode().getId()))
                .forEach(link -> stateNetwork.addLink(link));

        // Write the state-specific network to a new file
        new NetworkWriter(stateNetwork).write("C:\\Users\\phili\\IdeaProjects\\matsim-BA-Vulkaneifel\\scenarios\\equil\\vulkaneifel-v1.1-network_filtered.xml");
    }

    private static boolean isWithinBounds(Coord coord, double minLat, double minLon, double maxLat, double maxLon) {
        return coord.getX() >= minLon && coord.getX() <= maxLon &&
                coord.getY() >= minLat && coord.getY() <= maxLat;
    }
}
