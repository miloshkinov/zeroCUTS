package org.matsim.vsp.wasteCollection.Berlin;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.facilities.*;
import org.matsim.freight.carriers.*;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class VrpSplitUtils {

    public enum clusteringStrategy {
        none, random, seeding, kClusters, centroidClusters, METIS
    }

    static String linkChessboardDepot = "j(0,7)R";
    static String linkChessboardDump = "j(0,9)R";

    //Data structure for the kClustering
    private static record Edge(CarrierShipment a, CarrierShipment b, double distance) {}

    static void createRandomCarriersChessboard(Scenario scenario, int numberOfCarriers, int numberOfIterations) {

        //Get initial capabilities and remove initial carrier
        Carriers carriers = CarriersUtils.getCarriers(scenario);
        var carrier = carriers.getCarriers().get(Id.create("Carrier_Chessboard", Carrier.class)); //<--- THIS and the following lines NEEDS CHANGING!
        String carrierName = carrier.getId().toString();
        //Making the vehicle, ASK IF THIS IS OKAY, NO THIS NEEDS TO BE IMPROVED FOR ALL CASES?
        String vehicleName = "Split Vehicle";
        double earliestStartingTime = 6 * 3600;
        double latestFinishingTime = 14 * 3600;
        CarrierVehicleTypes vehicleTypes = (CarrierVehicleTypes) scenario.getScenarioElement("carrierVehicleTypes");
        VehicleType vehicleType = vehicleTypes.getVehicleTypes().values().iterator().next();
        CarrierVehicle carrierVehicle =  CarrierVehicle.Builder
                .newInstance(Id.create(vehicleName, Vehicle.class), Id.createLinkId(linkChessboardDepot), vehicleType)
                .setEarliestStart(earliestStartingTime).setLatestEnd(latestFinishingTime).build();

        carriers.getCarriers().clear();

        //Set up the desired number of carriers
        for (int i = 1; i <= numberOfCarriers; i++){
            Carrier newCarrier = createSingleCarrier(carrierName, numberOfIterations, carrierVehicle, i);
            carriers.addCarrier(newCarrier);
            System.out.println(carriers.getCarriers().size() + " carriers created");
        }

        //Facilities and network setup
        final String FILENAME_EXPORT_FACILITIES = "input/chessboardFacilitiesRandomWithUtils.xml";
        ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities("facilities");
        Network network = scenario.getNetwork();

        //----ADDING DEPOT AND DUMP TO XML----
        //getting LinkIds
        List<Id<Link>> depotLinkIds = List.of(carrierVehicle.getLinkId());
        List<Id<Link>> dropOffLinkIds = List.of(Id.createLinkId(linkChessboardDump)); //IMPROVE THIS FOR ALL CASES!!
        //Getting node Ids from linkIds
        Id<Node> depotNodeId = NetworkUtils.getLinks(network,depotLinkIds).get(0).getToNode().getId();
        Id<Node> dropOffNodeId = NetworkUtils.getLinks(network,dropOffLinkIds).get(0).getToNode().getId();
        //Geting the node coords
        final Coord depotCoord =  NetworkUtils.getNodes(network, depotNodeId.toString()).get(0).getCoord();
        final Coord dropOffCoord =  NetworkUtils.getNodes(network, dropOffNodeId.toString()).get(0).getCoord();
        //Creating a facility ID
        final Id<ActivityFacility> depotFacilityId = Id.create("depot", ActivityFacility.class);
        final Id<ActivityFacility> dropOffFacilityId = Id.create("dropOff", ActivityFacility.class);
        //Creating the facilities
        ActivityFacility depotFacility = facilities.getFactory().createActivityFacility(depotFacilityId, depotCoord);
        ActivityFacility dropOffFacility = facilities.getFactory().createActivityFacility(dropOffFacilityId, dropOffCoord);
        //Adding the activity option
        depotFacility.addActivityOption(new ActivityOptionImpl("depot"));
        dropOffFacility.addActivityOption(new ActivityOptionImpl("dropOff"));
        //Putting the carrier attribute to view in Via later
        depotFacility.getAttributes().putAttribute("carrier", "depot");
        dropOffFacility.getAttributes().putAttribute("carrier", "dropOff");
        //adding the facilities to the secanrio
        facilities.addActivityFacility(depotFacility);
        facilities.addActivityFacility(dropOffFacility);

        //--------RANDOM AND POPULATE FACILITIES XML---------
        //picking a random seed
        Random randomSeed = new Random(1);

        //loop through all services
//        for (CarrierService service : carrier.getServices().values()) {
//
//            //Retrieve Node Id
//            System.out.println("SERVICE ID: " + service.getId() + "SERVICE LINK ID: " + service.getServiceLinkId());
//            List<Id<Link>> linkIds = List.of(service.getServiceLinkId());
//            Id<Node> nodeId = NetworkUtils.getLinks(network,linkIds).get(0).getToNode().getId();
//
//            //Retrieve Node coord and create activityfacility
//            final Coord coord =  NetworkUtils.getNodes(network, nodeId.toString()).get(0).getCoord();
//            final Id<ActivityFacility> facilityId = Id.create(service.getId(), ActivityFacility.class);
//            ActivityFacility facility = facilities.getFactory().createActivityFacility(facilityId, coord);
//
//            //Randomly assign the service to a new carrier
//            long coinFlip = randomSeed.nextInt(numberOfcarriers) + 1;
//            for (int i = 1; i <= numberOfcarriers; i++){
//                if (coinFlip == i) {
//                    service.getAttributes().putAttribute("carrier", "newCarrier" + i);
//                    CarriersUtils.addService(carriers.getCarriers().get(Id.create("Carrier" + i, Carrier.class)), service);
//                    facility.getAttributes().putAttribute("carrier", "newCarrier" + i);
//                    System.out.println("SERVICE " + service.getId().toString() + " ADDED TO CARRIER " + i);
//                }
//            }
//
//            //add activity to xml
//            facility.addActivityOption(new ActivityOptionImpl("delivery"));
//            facilities.addActivityFacility(facility);
//        }

        //loop through all shipments
        for (CarrierShipment shipment : carrier.getShipments().values()) {

            //Retrieve Node Id
            System.out.println("SHIPMENT ID: " + shipment.getId() + "SHIPMENT START LINK ID: " + shipment.getPickupLinkId());

            //Retrieve Node coord and create activityfacility
            final Coord coord = network.getLinks().get(shipment.getPickupLinkId()).getCoord();
            final Id<ActivityFacility> facilityId = Id.create(shipment.getId(), ActivityFacility.class);
            ActivityFacility facility = facilities.getFactory().createActivityFacility(facilityId, coord);

            //Randomly assign the shipment to a new carrier
            long coinFlip = randomSeed.nextInt(numberOfCarriers) + 1;
            for (int i = 1; i <= numberOfCarriers; i++){
                if (coinFlip == i) {
                    shipment.getAttributes().putAttribute("carrier", "newCarrier" + i);
                    CarriersUtils.addShipment(carriers.getCarriers().get(Id.create("Carrier" + i, Carrier.class)), shipment);
                    facility.getAttributes().putAttribute("carrier", "newCarrier" + i);
                    System.out.println("SHIPMENT " + shipment.getId().toString() + " ADDED TO CARRIER " + i);
                }
            }

            //add activity to xml
            facility.addActivityOption(new ActivityOptionImpl("delivery"));
            facilities.addActivityFacility(facility);
        }

        //write the xml
        new FacilitiesWriter(facilities).writeV1(FILENAME_EXPORT_FACILITIES);
        System.out.println("write facilities to " + FILENAME_EXPORT_FACILITIES);
        System.out.println("done");
    }

    static void splitCarriers(Scenario scenario, clusteringStrategy clusterStrategy , int numberOfShipmentsPerCarrier, int numberOfIterations, String outputLocation) throws IOException, InterruptedException {
        //Log message to check how long the clustering takes
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss,SSS");
        System.out.println(fmt.format(LocalDateTime.now()) + " Begin " + clusterStrategy + " VRP Splitting");

        //Get network and initial carriers and create a new set
        Network network = scenario.getNetwork();
        Carriers carriers = CarriersUtils.getCarriers(scenario);
        Carriers newCarriers = new Carriers();

        //Setup activities for xml
        Boolean beforeSplit = true;
        ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities("facilities");
        createXMLFacilities(network, carriers, outputLocation, beforeSplit, facilities);

        //Loop through all carriers
        for (Carrier singleCarrier : carriers.getCarriers().values()) {

            //Get Carrier Vehicle and Name
            CarrierVehicle carrierVehicle = singleCarrier.getCarrierCapabilities().getCarrierVehicles().values().iterator().next();
            String carrierName = singleCarrier.getId().toString();

            //Set up the desired number of new carriers
            int numberOfCarriers = estimateNumberOfCarriers(numberOfShipmentsPerCarrier, singleCarrier);

            //Get Clusters
            List<List<CarrierShipment>> clusters;
            switch (clusterStrategy) {
                case none -> {
                    return;
                }
                case random -> {
                    clusters = findRandomClusters(singleCarrier, numberOfCarriers, numberOfShipmentsPerCarrier);
                }
                case seeding -> {
                    clusters = findSeedingClusters2(singleCarrier, network, numberOfCarriers, carrierVehicle, numberOfShipmentsPerCarrier);
                }
                case kClusters -> {
                    clusters = findKClusters(singleCarrier, network, numberOfCarriers, numberOfShipmentsPerCarrier);
                }
                case centroidClusters -> {
                    clusters = findCentroidClusters(singleCarrier, network, numberOfCarriers, numberOfShipmentsPerCarrier);
                }
                case METIS -> {
                    clusters = findMETISClusters(singleCarrier, network, numberOfCarriers);
                }
                case null, default -> {
                    System.out.println("No Clustering Strategy Defined! Exit");
                    return;
                }
            }

            //Loop through all clusters and assign to carrier
            for (int i = 0; i < clusters.size(); i++) {
                //create new carrier for the cluster
                Carrier newCarrier = createSingleCarrier(carrierName, numberOfIterations, carrierVehicle, i+1);
                newCarriers.addCarrier(newCarrier);
                int numberOfShipments = 0;

                for (int j = 0; j < clusters.get(i).size(); j++) {
                    //assign all shipments from cluster to carrier
                    CarrierShipment shipment = clusters.get(i).get(j);
                    shipment.getAttributes().putAttribute("carrier", carrierName + (i + 1));
                    CarriersUtils.addShipment(newCarriers.getCarriers().get(Id.create(carrierName + (i + 1), Carrier.class)), shipment);
                    numberOfShipments++;
                }
                System.out.println(carrierName + (i + 1) + " : " + numberOfShipments + " Shipments");
            }
        }

        //Put new carriers into scenario
        carriers.getCarriers().clear();
        for (Carrier singleCarrier : newCarriers.getCarriers().values()) {
            carriers.addCarrier(singleCarrier);
        }

        //Timestamp for when finished and create xml facilities file to visualise results
        beforeSplit = false;
        createXMLFacilities(network, carriers, outputLocation, beforeSplit, facilities);
        System.out.println(fmt.format(LocalDateTime.now()) + " " + clusterStrategy + " VRP Splitting complete");
    }

    private static List<List<CarrierShipment>> findRandomClusters(Carrier singleCarrier, int numberOfCarriers, int numberOfShipmentsPerCarrier) {
        //The list of clusters that will be returned
        List<List<CarrierShipment>> clusters = new ArrayList<>();
        for (int i = 0; i < numberOfCarriers; i++) {
            clusters.add(new ArrayList<CarrierShipment>());
        }
        //picking a random seed
        Random randomSeed = new Random(1);
        //loop through all shipments
        for (CarrierShipment shipment : singleCarrier.getShipments().values()) {

            //Randomly assign the shipment to a new carrier that hasn't reached the max size yet
            boolean hasBeenAssigned = false;
            while (!hasBeenAssigned) {
                int coinFlip = randomSeed.nextInt(numberOfCarriers);
                if (clusters.get(coinFlip).size() <= numberOfShipmentsPerCarrier) {
                    clusters.get(coinFlip).add(shipment);
                    hasBeenAssigned = true;
                }
            }
        }
        return clusters;
    }

    private static List<List<CarrierShipment>> findSeedingClusters(Carrier singleCarrier, Network network, int numberOfCarriers, CarrierVehicle carrierVehicle, int numberOfShipmentsPerCarrier) {

        //The list of clusters that will be returned
        List<List<CarrierShipment>> clusters = new ArrayList<>();
        List<Coord> seedCoords = new ArrayList<>();
        List<Id<CarrierShipment>> seedCoordIds = new ArrayList<>();
        List<CarrierShipment> shipments = new ArrayList<>(singleCarrier.getShipments().values());

        //Precompute coordinates
        Map<CarrierShipment, Coord> coords = new HashMap<>();
        for (CarrierShipment shipment : shipments) {
            coords.put(shipment, network.getLinks().get(shipment.getPickupLinkId()).getCoord());
        }

        //Get Depot Coord
        Coord depotCoord =  network.getLinks().get(carrierVehicle.getLinkId()).getCoord();

        //Variables to track the max distances and coefficient to encourage spread out clustering
        Coord seedCoord = null;
        Id<CarrierShipment> seedId = null;
        double clusterCoefficient = 1.0; //PLAY AROUND WITH THIS!!!!!!!!!!!

        //Loop for amount of seeds required
        for (int i = 0; i < numberOfCarriers; i++) {

            //Create new cluster for each seed and reset maxDistance
            clusters.add(new ArrayList<>());
            double maxDistance = 0;

            //Find seed
            for (CarrierShipment shipment : shipments) {

                //Check if this shipment is already a seed
                if (seedCoordIds.contains(shipment.getId())) {
                    continue;
                }

                //Calculate Distance to depot if finding first seed REDO COMMENTS AND NAMING IN THIS SECTION
                double distance = Double.MAX_VALUE;
                if(seedCoords.isEmpty()) {
                    distance = NetworkUtils.getEuclideanDistance(depotCoord, coords.get(shipment));
                } else {

                    //Otherwise the distance to all other seeds
                    for (Coord coord : seedCoords) {
                        distance = Math.min(distance, NetworkUtils.getEuclideanDistance(coord, coords.get(shipment)));
                    }
                }

                //Check if it is the new max distance
                if (distance>maxDistance) {
                    maxDistance = distance;
                    seedCoord = coords.get(shipment);
                    seedId = shipment.getId();
                }
            }
            //Save seed
            System.out.println("Seed " + (i+1) + " found at Coord " + seedCoord.toString() + " with ID: " + seedId.toString());
            seedCoords.add(seedCoord);
            seedCoordIds.add(seedId);
        }

        //loop through all shipments to assign to seeds
        for (CarrierShipment shipment : shipments) {
            //If seed add directly to cluster
            boolean isSeed = false;
            for (int i = 0; i < seedCoordIds.size(); i++) {
                if (seedCoordIds.get(i) == shipment.getId()){
                    clusters.get(i).add(shipment);
                    isSeed = true;
                    System.out.println("THIS IS A SEED " +  (i + 1));
                    shipment.getAttributes().putAttribute("seed", "seed" + (i + 1));
                }
            }

            //Skip if shipment is a seed
            if (isSeed) {
                continue;
            }

            //Retrieve Pickup Node coord
            final Coord coord =  network.getLinks().get(shipment.getPickupLinkId()).getCoord();

            //Variables to track which cluster the shipment should be assigned to
            double minDistance = Double.MAX_VALUE;
            int seedNumber = 0;

            //Loop through all seeds
            for (int i = 0; i < seedCoords.size(); i++) {
                double distanceApart = NetworkUtils.getEuclideanDistance(coord, seedCoords.get(i));
                //Assign seed if cluster isn't too large
                if ((distanceApart < minDistance) && (clusters.get(i).size() < numberOfShipmentsPerCarrier)) {
                    seedNumber = i;
                    minDistance = distanceApart;
                }
            }
            //Assign to cluster
            clusters.get(seedNumber).add(shipment);
        }
        return clusters;
    }

    private static List<List<CarrierShipment>> findSeedingClusters2(Carrier singleCarrier, Network network, int numberOfCarriers, CarrierVehicle carrierVehicle, int numberOfShipmentsPerCarrier) {

        //The list of clusters that will be returned
        List<List<CarrierShipment>> clusters = new ArrayList<>();
        List<Coord> seedCoords = new ArrayList<>();
        List<Id<CarrierShipment>> seedCoordIds = new ArrayList<>();
        List<CarrierShipment> shipments = new ArrayList<>(singleCarrier.getShipments().values());

        //Precompute coordinates
        Map<CarrierShipment, Coord> coords = new HashMap<>();
        for (CarrierShipment shipment : shipments) {
            coords.put(shipment, network.getLinks().get(shipment.getPickupLinkId()).getCoord());
        }

        //Get Depot Coord
        Coord depotCoord =  network.getLinks().get(carrierVehicle.getLinkId()).getCoord();

        //Variables to track the max distances and coefficient to encourage spread out clustering
        Coord seedCoord = null;
        Id<CarrierShipment> seedId = null;
        int seedNumber = 0;

        //Loop for amount of seeds required
        for (int i = 0; i < numberOfCarriers; i++) {

            //Create new cluster for each seed and reset maxDistance
            clusters.add(new ArrayList<>());
            double maxDistance = 0;

            //Update the shipments still left
            shipments = new ArrayList<>(singleCarrier.getShipments().values());

            //Find seed
            for (CarrierShipment shipment : shipments) {

                //Check if this shipment is already a seed
                if (seedCoordIds.contains(shipment.getId())) {
                    continue;
                }

                //Calculate Distance to depot if finding first seed otherwise the seed that is the furthest from all other seeds
                double distance = 0;
                if(seedCoords.isEmpty()) {
                    distance = NetworkUtils.getEuclideanDistance(depotCoord, coords.get(shipment));
                } else {

                    //Otherwise the distance to all other seeds
                    for (Coord coord : seedCoords) {
                        distance += NetworkUtils.getEuclideanDistance(coord, coords.get(shipment));
                    }
                }

                //Check if it is the new max distance
                if (distance>maxDistance) {
                    maxDistance = distance;
                    seedCoord = coords.get(shipment);
                    seedId = shipment.getId();
                }
            }

            //Save seed
            System.out.println("Seed " + (i+1) + " found at Coord " + seedCoord + " with ID: " + seedId);
            seedCoords.add(seedCoord);
            seedCoordIds.add(seedId);
            singleCarrier.getShipments().get(seedId).getAttributes().putAttribute("seed", "seed" + (i + 1));

            //Calculate edge distances
            List<Edge> edges = new ArrayList<>();
            CarrierShipment fromSeed = singleCarrier.getShipments().get(seedId);
            for (int j = 0; j < singleCarrier.getShipments().size(); j++) {
                CarrierShipment toShipment = shipments.get(j);
                double dist = NetworkUtils.getEuclideanDistance(coords.get(fromSeed), coords.get(toShipment));
                edges.add(new Edge(fromSeed, toShipment, dist));
            }

            //Sort edges by increasing distance
            edges.sort(Comparator.comparingDouble(Edge::distance));

            //Assign nearest Shipments to cluster
            int counter = 0;
            int remainingShipments = singleCarrier.getShipments().size();
            //Don't leave very small clusters at end
            if (remainingShipments < numberOfShipmentsPerCarrier*1.3) {
                //Plus one so that the else statement in the while loop below is reached
                numberOfShipmentsPerCarrier = remainingShipments + 1;
            }
            while (clusters.get(seedNumber).size() < numberOfShipmentsPerCarrier) {
                if (counter < remainingShipments) {
                    CarrierShipment shipmentToBeClustered = edges.get(counter).b;
                    clusters.get(seedNumber).add(shipmentToBeClustered);
                    singleCarrier.getShipments().remove(shipmentToBeClustered.getId());
                    counter++;
                } else {
                    //return once all shipments ar assigned
                    return clusters;
                }
            }
            seedNumber++;
        }
        return clusters;
    }

    private static List<List<CarrierShipment>> findKClusters(Carrier singleCarrier, Network network, int numberOfCarriers, int numberOfShipmentsPerCarrier) {

        //The list of clusters that will be returned
        List<List<CarrierShipment>> clusters = new ArrayList<>();
        List<CarrierShipment> shipments = new ArrayList<>(singleCarrier.getShipments().values());
        int numberOfShipments = shipments.size();

        //Precompute coordinates and assign all shipments to a cluster
        Map<CarrierShipment, Coord> coords = new HashMap<>();
        for (CarrierShipment shipment : shipments) {
            coords.put(shipment, network.getLinks().get(shipment.getPickupLinkId()).getCoord());
            List<CarrierShipment> cluster = new ArrayList<>();
            cluster.add(shipment);
            clusters.add(cluster);
        }

        //Precompute all edge distances
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < numberOfShipments; i++) {
            for (int j = i + 1; j < numberOfShipments; j++) {
                CarrierShipment a = shipments.get(i);
                CarrierShipment b = shipments.get(j);
                double dist = NetworkUtils.getEuclideanDistance(coords.get(a), coords.get(b));
                edges.add(new Edge(a, b, dist));
            }
        }

        //Sort edges by increasing distance
        edges.sort(Comparator.comparingDouble(Edge::distance));

        //Merge clusters from nearest edge until desired number of clusters is reached
        for (Edge edge : edges) {
            CarrierShipment a = edge.a();
            CarrierShipment b = edge.b();
            int aIndex = getClusterIndex(a, clusters);
            int bIndex = getClusterIndex(b, clusters);
            //Check if the two clusters are too large
            if (clusters.get(aIndex).size() + clusters.get(bIndex).size() > numberOfShipmentsPerCarrier) {
                continue;
            }
            //Check if in same cluster otherwise merge the higher index into the lower
            if (aIndex != bIndex) {
                if (aIndex < bIndex) {
                    clusters.get(aIndex).addAll(clusters.get(bIndex));
                    clusters.remove(bIndex);
                } else {
                    clusters.get(bIndex).addAll(clusters.get(aIndex));
                    clusters.remove(aIndex);
                }
                if (clusters.size() == numberOfCarriers) {
                    break;
                }
            }
        }

        return clusters;
    }

    private static List<List<CarrierShipment>> findCentroidClusters(Carrier singleCarrier, Network network, int numberOfCarriers, int numberOfShipmentsPerCarrier) {

        //The list of clusters that will be returned
        List<CarrierShipment> shipments = new ArrayList<>(singleCarrier.getShipments().values());
        List<List<CarrierShipment>> clusters = new ArrayList<>();

        //Precompute coordinates and assign all shipments to a cluster
        Map<CarrierShipment, Coord> coords = new HashMap<>();
        for (CarrierShipment shipment : shipments) {
            coords.put(shipment, network.getLinks().get(shipment.getPickupLinkId()).getCoord());
            clusters.add(new ArrayList<>(List.of(shipment)));
        }

        // Keep merging until we have the target number of clusters
        while (clusters.size() > numberOfCarriers) {
            double minDistance = Double.MAX_VALUE;
            int aIndex = -1, bIndex = -1;

            //Find the two clusters with the smallest distance between centroids
            for (int i = 0; i < clusters.size(); i++) {
                Coord centroidA = computeCentroid(coords, clusters.get(i));
                for (int j = i + 1; j < clusters.size(); j++) {
                    Coord centroidB = computeCentroid(coords, clusters.get(j));
                    double distanceApart = NetworkUtils.getEuclideanDistance(centroidA, centroidB);

                    //Check if the two clusters are too large
                    if (clusters.get(i).size() + clusters.get(j).size() > numberOfShipmentsPerCarrier) {
                        continue;
                    }

                    //Save Index of the clusters
                    if (distanceApart < minDistance) {
                        minDistance = distanceApart;
                        aIndex = i;
                        bIndex = j;
                    }
                }
            }

            //Merge the higher index into the lower
            if (aIndex < bIndex) {
                clusters.get(aIndex).addAll(clusters.get(bIndex));
                clusters.remove(bIndex);
            } else if (aIndex > bIndex) {
                clusters.get(bIndex).addAll(clusters.get(aIndex));
                clusters.remove(aIndex);
            } else {
                // No valid merge found (e.g., size constraints prevent it)
                System.out.println("No further merges found!");
                break;
            }
        }

        return clusters;
    }

    private static List<List<CarrierShipment>> findMETISClusters(Carrier singleCarrier, Network network, int numberOfCarriers) throws IOException, InterruptedException {

        //The list of clusters that will be returned
        List<List<CarrierShipment>> clusters = new ArrayList<>();
        List<CarrierShipment> shipments = new ArrayList<>(singleCarrier.getShipments().values());
        for (int i = 0; i < numberOfCarriers; i++) {
            clusters.add(new ArrayList<>());
        }

        //Precompute coordinates
        Map<CarrierShipment, Coord> coords = new HashMap<>();
        for (CarrierShipment shipment : shipments) {
            coords.put(shipment, network.getLinks().get(shipment.getPickupLinkId()).getCoord());
        }

        //Find min and max distances
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < shipments.size(); i++) {
            for (int j = i + 1; j < shipments.size(); j++) {
                CarrierShipment a = shipments.get(i);
                CarrierShipment b = shipments.get(j);
                double dist = NetworkUtils.getEuclideanDistance(coords.get(a), coords.get(b));
                edges.add(new Edge(a, b, dist));
            }
        }

        edges.sort(Comparator.comparingDouble(Edge::distance));
        int minDistance = (int) edges.getFirst().distance;
        int maxDistance = (int) edges.getLast().distance;

        String path = "input/METIS_Graphs/Fr_" + singleCarrier.getId() + ".txt";
        File file = new File(path);

        //CHANGE THIS BEFORE CLUSTER RUN
        if (file.exists()) {
            //Write the Graph files
            try (PrintWriter pw = new PrintWriter(path)) {
                //File header set up
                int n = shipments.size();
                int m = n*(n-1)/2; // number of undirected edges
                pw.println(n + " " + m + " 1"); // "1" means weighted

                //Write to METIS style
                int above1000 = 0;
                int below0 = 0;
                for (int i = 0; i < singleCarrier.getShipments().size(); i++) {
                    Coord fromShipment = network.getLinks().get(shipments.get(i).getPickupLinkId()).getCoord();
                    StringBuilder line = new StringBuilder();
                    for (int j = 0; j < singleCarrier.getShipments().size(); j++) {
                        //Don't map to same Shipment
                        if (i == j) {
                            continue;
                        }
                        Coord toShipment = network.getLinks().get(shipments.get(j).getPickupLinkId()).getCoord();
                        //This normalises the distances to weights
    //                    int distance = (int) NetworkUtils.getEuclideanDistance(fromShipment, toShipment);
    //                    int weight = (int) 1 + (1000 - 1)*(maxDistance - distance)/(maxDistance - minDistance);
                        int weight = (int) (10000/NetworkUtils.getEuclideanDistance(fromShipment, toShipment));
                        //That is the upper bound of METIS Edge weights
                        if (weight > 1000) {
                            weight = 1000;
                            above1000++;
                        }
                        //That is the lower bound of METIS Edge weights
                        if (weight == 0) {
                            weight = 1;
                            below0++;
                        }
                        //J+1 because index starts at 1 in METIS
                        line.append(j+1).append(" ").append(weight).append(" ");
                    }
                    pw.println(line.toString().trim());
                }
                System.out.println("This many above " + above1000 + " and below " + below0);
            }

            //Run METIS
            ProcessBuilder pb = new ProcessBuilder("wsl", "gpmetis -niter=200 -ncuts=40 -ufactor=200", "/mnt/c/Users/Milo/Desktop/UniZeug/AbfallGit/" + path, "" + numberOfCarriers);
            Process process = pb.start();
            process.waitFor();
        }

        //Read the Results
        int shipmentCounter = 0;
        try (Scanner scanner = new Scanner(new File(path + ".part." + numberOfCarriers))) {
            while (scanner.hasNextInt()) {
                clusters.get(scanner.nextInt()).add(shipments.get(shipmentCounter));
                shipmentCounter++;
            }
        }

        return clusters;
    }

    //Returns which cluster the shipment is in
    private static int getClusterIndex(CarrierShipment a, List<List<CarrierShipment>> clusters) {
        int index = 0;
        for (int i = 0; i < clusters.size(); i++) {
            for (int j = 0; j < clusters.get(i).size(); j++) {
                if (a == clusters.get(i).get(j)) {
                    index = i;
                }
            }
        }
        return index;
    }

    //Computes the centroid of a cluster
    private static Coord computeCentroid (Map<CarrierShipment, Coord> coords, List<CarrierShipment> cluster) {
        double sumX = 0.0, sumY = 0.0;
        for (CarrierShipment s : cluster) {
            Coord c = coords.get(s);
            sumX += c.getX();
            sumY += c.getY();
        }
        int size = cluster.size();
        return new Coord(sumX / size, sumY / size);
    }

    //Determine number of new carriers
    private static int estimateNumberOfCarriers(int numberOfShipmentsPerCarrier, Carrier carrier) {
        int noOfCarriers = 0;
        //Float so that the round function works
        int noOfShipments = carrier.getShipments().size();
        System.out.println("NO OF SHIPMENTS: " + noOfShipments + " / NO OF SHIPMENTS PER CARRIER: " + numberOfShipmentsPerCarrier);
        //Here we add 1 because the result of the division is truncated
        noOfCarriers = (noOfShipments/numberOfShipmentsPerCarrier) + 1;
        System.out.println("NO OF CARRIERS: " + noOfCarriers);
        return noOfCarriers;
    }

    //Create a basic carrier
    private static Carrier createSingleCarrier(String carrierName, int numberOfIterations, CarrierVehicle carrierVehicle, int carrierNumber) {
        Carrier newCarrier = CarriersUtils.createCarrier(Id.create(carrierName + carrierNumber, Carrier.class));
        CarriersUtils.setJspritIterations(newCarrier, numberOfIterations);
        CarrierCapabilities carrierCapabilities = CarrierCapabilities.Builder.newInstance()
                .addVehicle(carrierVehicle).setFleetSize(CarrierCapabilities.FleetSize.INFINITE).build();
        newCarrier.setCarrierCapabilities(carrierCapabilities);

        return newCarrier;
    }

    //Create XML Facilities File
    private static void createXMLFacilities(Network network, Carriers carriers, String outputLocation, Boolean beforeSplit, ActivityFacilities facilities) {

        if (beforeSplit) {
            //Loop through all old carriers
            for (Carrier carrier : carriers.getCarriers().values()) {
                //Add Depot and Dropoff to xml
                String carrierName = carrier.getId().toString();
                //Getting LinkIds
                CarrierVehicle carrierVehicle = carrier.getCarrierCapabilities().getCarrierVehicles().values().iterator().next();
                CarrierShipment firstShipment = carrier.getShipments().values().iterator().next();
                //Getting the node coords
                final Coord depotCoord = network.getLinks().get(carrierVehicle.getLinkId()).getCoord();
                final Coord dropOffCoord = network.getLinks().get(firstShipment.getDeliveryLinkId()).getCoord();
                //Creating a facility ID
                final Id<ActivityFacility> depotFacilityId = Id.create("depot_" + carrierName, ActivityFacility.class);
                final Id<ActivityFacility> dropOffFacilityId = Id.create("dropOff_" + carrierName, ActivityFacility.class);
                //Creating the facilities
                ActivityFacility depotFacility = facilities.getFactory().createActivityFacility(depotFacilityId, depotCoord);
                ActivityFacility dropOffFacility = facilities.getFactory().createActivityFacility(dropOffFacilityId, dropOffCoord);
                //Adding the activity option
                depotFacility.addActivityOption(new ActivityOptionImpl("depot"));
                dropOffFacility.addActivityOption(new ActivityOptionImpl("dropOff"));
                //Putting the carrier attribute to view in Via later
                depotFacility.getAttributes().putAttribute("carrier", "depot_" + carrierName);
                dropOffFacility.getAttributes().putAttribute("carrier", "dropOff_" + carrierName);
                //Adding the facilities to the scenario
                facilities.addActivityFacility(depotFacility);
                facilities.addActivityFacility(dropOffFacility);
            }
        } else {
            //Loop through all new carriers
            for (Carrier carrier : carriers.getCarriers().values()) {

                //Add all Shipments
                for (CarrierShipment shipment : carrier.getShipments().values()) {

                    //Retrieve Pickup Node coord and create activityfacility
                    final Coord coord = network.getLinks().get(shipment.getPickupLinkId()).getCoord();
                    final Id<ActivityFacility> facilityId = Id.create(shipment.getId(), ActivityFacility.class);
                    ActivityFacility facility = facilities.getFactory().createActivityFacility(facilityId, coord);
                    facility.getAttributes().putAttribute("carrier", shipment.getAttributes().getAttribute("carrier").toString());
                    if (shipment.getAttributes().getAttribute("seed") != null) {
                        facility.getAttributes().putAttribute("seed", shipment.getAttributes().getAttribute("seed").toString());
                    }

                    //Add activity to xml
                    facility.addActivityOption(new ActivityOptionImpl("delivery"));
                    facilities.addActivityFacility(facility);
                }
            }

            //Write the xml
            final String FILENAME_EXPORT_FACILITIES = outputLocation + "/facilities.xml";
            new FacilitiesWriter(facilities).writeV1(FILENAME_EXPORT_FACILITIES);
            System.out.println("write facilities to " + FILENAME_EXPORT_FACILITIES);
            System.out.println("done");
        }
    }
}

