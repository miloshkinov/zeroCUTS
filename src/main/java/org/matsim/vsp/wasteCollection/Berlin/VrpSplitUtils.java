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
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class VrpSplitUtils {

    private static final Logger log = LogManager.getLogger(VrpSplitUtils.class);

    public enum clusteringStrategy {
        random, greedy, singleLink, centroids, METIS
    }

    static String linkChessboardDepot = "j(0,7)R";
    static String linkChessboardDump = "j(0,9)R";

    //Data structure for sorting edges
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
            log.info("{} carriers created", carriers.getCarriers().size());
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
            log.info("SHIPMENT ID: {}SHIPMENT START LINK ID: {}", shipment.getId(), shipment.getPickupLinkId());

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
                    log.info("SHIPMENT {} ADDED TO CARRIER {}", shipment.getId().toString(), i);
                }
            }

            //add activity to xml
            facility.addActivityOption(new ActivityOptionImpl("delivery"));
            facilities.addActivityFacility(facility);
        }

        //write the xml
        new FacilitiesWriter(facilities).writeV1(FILENAME_EXPORT_FACILITIES);
        log.info("write facilities to " + FILENAME_EXPORT_FACILITIES);
        log.info("done");
    }

    static void splitCarriers(Scenario scenario, clusteringStrategy clusterStrategy , int numberOfShipmentsPerCarrier, int numberOfIterations, String outputLocation) throws IOException, InterruptedException {
        //Log message to check how long the clustering takes
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss,SSS");
        log.info("{} Begin {} VRP Splitting", fmt.format(LocalDateTime.now()), clusterStrategy);

        //Get network and initial carriers and create a new set
        Network network = scenario.getNetwork();
        Carriers carriers = CarriersUtils.getCarriers(scenario);
        Carriers newCarriers = new Carriers();

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
                case random -> {
                    clusters = findRandomClusters(singleCarrier, numberOfCarriers, numberOfShipmentsPerCarrier);
                }
                case greedy -> {
                    clusters = findGreedyClusters(singleCarrier, network, numberOfCarriers, carrierVehicle, numberOfShipmentsPerCarrier);
                }
                case singleLink -> {
                    clusters = findSingleLinkClusters(singleCarrier, network, numberOfCarriers, numberOfShipmentsPerCarrier);
                }
                case centroids -> {
                    clusters = findCentroidsClusters(singleCarrier, network, numberOfCarriers, numberOfShipmentsPerCarrier);
                }
                case METIS -> {
                    clusters = findMETISClusters(singleCarrier, network, numberOfCarriers, numberOfShipmentsPerCarrier, outputLocation);
                }
                default -> {
                    log.info("No Clustering Strategy Defined! Exit");
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
                log.info("{}{} : {} Shipments", carrierName, i + 1, numberOfShipments);
            }
        }

        //Put new carriers into scenario
        carriers.getCarriers().clear();
        for (Carrier singleCarrier : newCarriers.getCarriers().values()) {
            carriers.addCarrier(singleCarrier);
        }

        //Timestamp for when finished and create xml facilities file to visualise results
        createXMLFacilities(network, carriers, outputLocation);
        log.info("{} {} VRP Splitting complete", fmt.format(LocalDateTime.now()), clusterStrategy);
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
                if (clusters.get(coinFlip).size() < numberOfShipmentsPerCarrier) {
                    clusters.get(coinFlip).add(shipment);
                    hasBeenAssigned = true;
                }
            }
        }
        return clusters;
    }

    private static List<List<CarrierShipment>> findGreedyClusters(Carrier singleCarrier, Network network, int numberOfCarriers, CarrierVehicle carrierVehicle, int numberOfShipmentsPerCarrier) {

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
            log.info("Seed {} found at Coord {} with ID: {}", i + 1, seedCoord, seedId);
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
                    //Return once all shipments are assigned
                    return clusters;
                }
            }
            seedNumber++;
        }
        return clusters;
    }

    private static List<List<CarrierShipment>> findSingleLinkClusters(Carrier singleCarrier, Network network, int numberOfCarriers, int numberOfShipmentsPerCarrier) {

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
            //Check if the two clusters are too large, allow larger merges towards the end of clustering
            if (clusters.get(aIndex).size() + clusters.get(bIndex).size() > numberOfShipmentsPerCarrier) {
                if (clusters.size() > numberOfCarriers*1.5) {
                    continue;
                }
                else if (clusters.get(aIndex).size() + clusters.get(bIndex).size() > numberOfShipmentsPerCarrier*1.3){
                    continue;
                }
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
                boolean noSmallClusters = checkForSmallClusters(clusters, numberOfShipmentsPerCarrier);
                if ((clusters.size() == numberOfCarriers) && noSmallClusters) {
                    break;
                }
            }
        }

        return clusters;
    }

    private static List<List<CarrierShipment>> findCentroidsClusters(Carrier singleCarrier, Network network, int numberOfCarriers, int numberOfShipmentsPerCarrier) {

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

                    //Check if the two clusters are too large
                    if (clusters.get(i).size() + clusters.get(j).size() > numberOfShipmentsPerCarrier) {
                        continue;
                    }

                    //Find distance between centroids
                    Coord centroidB = computeCentroid(coords, clusters.get(j));
                    double distanceApart = NetworkUtils.getEuclideanDistance(centroidA, centroidB);

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
                log.info("No further merges found!");
                break;
            }
        }

        return clusters;
    }

    private static List<List<CarrierShipment>> findMETISClusters(Carrier singleCarrier, Network network, int numberOfCarriers, int numberOfShipmentsPercCarrier, String outputLocation) throws IOException, InterruptedException {

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

        String path = "input/" + outputLocation + "/" + singleCarrier.getId() + ".txt";

        String outputPath = path + ".part." + numberOfCarriers;
        File file = new File(outputPath);

        //Check if file has been prepared
        if (!file.exists()) {
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
                        //This converts the distances to weights
                        int weight = (int) (10000/NetworkUtils.getEuclideanDistance(fromShipment, toShipment));

                        //That is the upper bound of METIS Edge weights
                        if (weight >= 1000) {
                            weight = 1000;
                            above1000++;
                        }
                        //That is the lower bound of METIS Edge weights
                        if (weight <= 1) {
                            weight = 1;
                            below0++;
                        }
                        //J+1 because index starts at 1 in METIS
                        line.append(j+1).append(" ").append(weight).append(" ");
                    }
                    pw.println(line.toString().trim());
                }
                log.info("This many above {} and below {}", above1000, below0);
            }

            //Run METIS
            ProcessBuilder pb = new ProcessBuilder("wsl", "gpmetis -ufactor=1000", "/mnt/c/Users/Milo/Desktop/UniZeug/AbfallGit/" + path, "" + numberOfCarriers);
            Process process = pb.start();
            process.waitFor();
        }

        //Read the Results
        int shipmentCounter = 0;
        try (Scanner scanner = new Scanner(new File(outputPath))) {
            while (scanner.hasNextInt()) {
                clusters.get(scanner.nextInt()).add(shipments.get(shipmentCounter));
                shipmentCounter++;
            }
        }

        //Clean up using centroids because METIS produces some weird results at times
        List<Coord> centroids = new ArrayList<>();
        for (List<CarrierShipment> cluster : clusters) {
            centroids.add(computeCentroid(coords, cluster));
        }
        int counter = 0;
        for (CarrierShipment shipment : shipments) {
            int clusterIndex = getClusterIndex(shipment, clusters);
            Coord coord = network.getLinks().get(shipment.getPickupLinkId()).getCoord();
            double originalDistance = NetworkUtils.getEuclideanDistance(coord, centroids.get(clusterIndex));
            int closestCentroid = clusterIndex;
            double closestDistance = Double.MAX_VALUE;
            for (int i = 0; i < centroids.size(); i++) {
                if (i == clusterIndex) continue;
                double distance = NetworkUtils.getEuclideanDistance(coord, centroids.get(i));
                if ((distance < 0.5*originalDistance) && (distance < closestDistance)) {
                    closestDistance = distance;
                    closestCentroid = i;
                }
            }

            if((closestCentroid != clusterIndex) && clusters.get(clusterIndex).size() > numberOfShipmentsPercCarrier*0.5){
                counter++;
                clusters.get(clusterIndex).remove(shipment);
                clusters.get(closestCentroid).add(shipment);
            }

        }
        log.info("number of switches: {}", counter);
        return clusters;
    }

    //Check remaining clusters for any that are too small
    private static boolean checkForSmallClusters(List<List<CarrierShipment>> clusters, int numberOfShipmentsPerCarrier) {
        for (List<CarrierShipment> cluster : clusters) {
            if (cluster.size() < numberOfShipmentsPerCarrier*0.3) {
                return false;
            }
        }
        return true;
    }

    //Returns which cluster the shipment is in
    private static int getClusterIndex(CarrierShipment a, List<List<CarrierShipment>> clusters) {
        int index = 0;
        for (int i = 0; i < clusters.size(); i++) {
            for (int j = 0; j < clusters.get(i).size(); j++) {
                if (a == clusters.get(i).get(j)) {
                    index = i;
                    return index;
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
        log.info("NO OF SHIPMENTS: {} / NO OF SHIPMENTS PER CARRIER: {}", noOfShipments, numberOfShipmentsPerCarrier);
        //Here we add 1 because the result of the division is truncated
        noOfCarriers = (noOfShipments/numberOfShipmentsPerCarrier) + 1;
        log.info("NO OF CARRIERS: {}", noOfCarriers);
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
    private static void createXMLFacilities(Network network, Carriers carriers, String outputLocation) {

        //Setup
        ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities("facilities");

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
        log.info("write facilities to {}", FILENAME_EXPORT_FACILITIES);
        log.info("done");

    }
}

