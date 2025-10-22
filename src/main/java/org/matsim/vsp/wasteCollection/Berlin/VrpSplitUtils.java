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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.lang.Math.abs;




public class VrpSplitUtils {

    public enum clusteringStrategy {
        random, seeding, kClusters, METIS
    }

    static String linkChessboardDepot = "j(0,7)R";
    static String linkChessboardDump = "j(0,9)R";

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
        List<Id<Link>> dumpLinkIds = List.of(Id.createLinkId(linkChessboardDump)); //IMPROVE THIS FOR ALL CASES!!
        //Getting node Ids from linkIds
        Id<Node> depotNodeId = NetworkUtils.getLinks(network,depotLinkIds).get(0).getToNode().getId();
        Id<Node> dumpNodeId = NetworkUtils.getLinks(network,dumpLinkIds).get(0).getToNode().getId();
        //Geting the node coords
        final Coord depotCoord =  NetworkUtils.getNodes(network, depotNodeId.toString()).get(0).getCoord();
        final Coord dumpCoord =  NetworkUtils.getNodes(network, dumpNodeId.toString()).get(0).getCoord();
        //Creating a facility ID
        final Id<ActivityFacility> depotFacilityId = Id.create("depot", ActivityFacility.class);
        final Id<ActivityFacility> dumpFacilityId = Id.create("dump", ActivityFacility.class);
        //Creating the facilities
        ActivityFacility depotFacility = facilities.getFactory().createActivityFacility(depotFacilityId, depotCoord);
        ActivityFacility dumpFacility = facilities.getFactory().createActivityFacility(dumpFacilityId, dumpCoord);
        //Adding the activity option
        depotFacility.addActivityOption(new ActivityOptionImpl("depot"));
        dumpFacility.addActivityOption(new ActivityOptionImpl("dump"));
        //Putting the carrier attribute to view in Via later
        depotFacility.getAttributes().putAttribute("carrier", "depot");
        dumpFacility.getAttributes().putAttribute("carrier", "dump");
        //adding the facilities to the secanrio
        facilities.addActivityFacility(depotFacility);
        facilities.addActivityFacility(dumpFacility);

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

    static void splitCarriers(Scenario scenario, clusteringStrategy clusterStrategy , int numberOfShipmentsPerCarrier, int numberOfIterations, String runName) throws IOException, InterruptedException {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss,SSS");
        System.out.println(fmt.format(LocalDateTime.now()) + " Begin " + clusterStrategy.toString() + " VRP Splitting");

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
                    clusters = findRandomClusters(singleCarrier, network, numberOfCarriers);
                }
                case seeding -> {
                    clusters = findSeedingClusters(singleCarrier, network, numberOfCarriers, carrierVehicle, numberOfShipmentsPerCarrier);
                }
                case kClusters -> {
                    clusters = findKClusters(singleCarrier, network, numberOfCarriers, numberOfShipmentsPerCarrier);
                }
                case METIS -> {
                    clusters = findMETISClusters(singleCarrier, network, numberOfCarriers);
                }
                case null, default -> {
                    System.out.println("No Clustering Strategy Defined! Exit");
                    return;
                }
            }

            //loop through all clusters and assign to carrier
            for (int i = 0; i < clusters.size(); i++) {
                //create new carrier for the cluster
                Carrier newCarrier = createSingleCarrier(carrierName, numberOfIterations, carrierVehicle, i+1);
                newCarriers.addCarrier(newCarrier);
                int numberOfShipments = 0;

                for (int j = 0; j < clusters.get(i).size(); j++) {
                    //assign all shipments from cluster to carrier
                    CarrierShipment shipment = clusters.get(i).get(j);
                    shipment.getAttributes().putAttribute("carrier", carrierName + (i + 1));
                    //System.out.println("SHIPMENT " + shipment.getId().toString() + " ADDED TO " + carrierName + (i + 1));
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

        //create xml facilities file to visualise results
        createXMLFacilities(network, carriers, runName);
        System.out.println(fmt.format(LocalDateTime.now()) + " " + clusterStrategy.toString() + " VRP Splitting complete");
    }

    private static List<List<CarrierShipment>> findRandomClusters(Carrier singleCarrier, Network network, int numberOfCarriers) {
        //The list of clusters that will be returned
        List<List<CarrierShipment>> clusters = new ArrayList<>();
        for (int i = 0; i < numberOfCarriers; i++) {
            clusters.add(new ArrayList<CarrierShipment>());
        }
        //picking a random seed
        Random randomSeed = new Random(1);
        //loop through all shipments
        for (CarrierShipment shipment : singleCarrier.getShipments().values()) {

            //Randomly assign the shipment to a new carrier
            int coinFlip = randomSeed.nextInt(numberOfCarriers);
            clusters.get(coinFlip).add(shipment);
        }
        return clusters;
    }

    private static List<List<CarrierShipment>> findSeedingClusters(Carrier carrier, Network network, int numberOfCarriers, CarrierVehicle carrierVehicle, int numberOfShipmentsPerCarrier) {

        //The list of clusters that will be returned COULD MOVE THIS DOWN
        List<List<CarrierShipment>> clusters = new ArrayList<>();
        for (int i = 0; i < numberOfCarriers; i++) {
            clusters.add(new ArrayList<CarrierShipment>());
        }
        List<Coord> seedCoords = new ArrayList<>();
        List<Id<CarrierShipment>> seedCoordIds = new ArrayList<>();

        //Get Depot Coord
        Coord depotCoord =  network.getLinks().get(carrierVehicle.getLinkId()).getCoord();

        //Variables to track the max distances and coefficient to encourage spread out clustering
		double maxDistance = 0;
        Coord seedCoord = null;
        Id<CarrierShipment> seedId = null;
        double clusterCoefficient = 2.0; //PLAY AROUND WITH THIS!!!!!!!!!!!

        //loop for amount of seeds required
        for (int i = 0; i < numberOfCarriers; i++) {

            //Find seed
            for (CarrierShipment shipment : carrier.getShipments().values()) {

                //Get Coord of Shipment
                final Coord shipmentCoord = network.getLinks().get(shipment.getPickupLinkId()).getCoord();

                //Calculate Distance to depot and all other seeds
                double distance = NetworkUtils.getEuclideanDistance(depotCoord, shipmentCoord);
                if (seedCoords != null) {
                    for (int j = 0; j < seedCoords.size(); j++) {
                        //To avoid picking same seed twice
                        if (seedCoordIds.get(j) == shipment.getId()){
                            distance = 0;
                            break;
                        }
                        distance += NetworkUtils.getEuclideanDistance(seedCoords.get(j), shipmentCoord)*clusterCoefficient;
                    }
                }

                //Check if it is the new max Distace
                if (distance>maxDistance) {
                    maxDistance = distance;
                    seedCoord = shipmentCoord;
                    seedId = shipment.getId();
                }
            }
            //Save Seed and reset max Distance
            System.out.println("Seed " + (i+1) + " found at Coord " + seedCoord.toString() + " with ID: " + seedId.toString());
            seedCoords.add(seedCoord);
            seedCoordIds.add(seedId);
            maxDistance = 0;
        }

        //loop through all shipments to assign to seeds
        for (CarrierShipment shipment : carrier.getShipments().values()) {
            //Skip if Shipment is already a seed
            for (int i = 0; i < seedCoordIds.size(); i++) {
                if (seedCoordIds.get(i) == shipment.getId()){
                    System.out.println("THIS IS A SEED " +  (i + 1));
                    shipment.getAttributes().putAttribute("seed", "seed" + (i + 1));
                }
            }

            //Retrieve Pickup Node coord
            final Coord coord =  network.getLinks().get(shipment.getPickupLinkId()).getCoord();

            //Variables to track which carrier the shipment should be assigned to
            double minDistance = Double.MAX_VALUE;
            int seedNumber = 0;

            //loop through all seeds
            for (int i = 0; i < seedCoords.size(); i++) {
                double distanceApart = NetworkUtils.getEuclideanDistance(coord, seedCoords.get(i));
                //Assign seed if cluster isn't too large
                if ((distanceApart < minDistance) && (clusters.get(i).size() < (carrier.getShipments().size()/numberOfCarriers)+1)) {
                    seedNumber = i;
                    minDistance = distanceApart;
                }
            }
            //Assign to Carrier
            clusters.get(seedNumber).add(shipment);
        }
        return clusters;
    }

    private static List<List<CarrierShipment>> findKClusters(Carrier singleCarrier, Network network, int numberOfCarriers, int numberOfShipmentsPerCarrier) {

        //The list of clusters that will be returned
        List<List<CarrierShipment>> clusters = new ArrayList<>();

        //Assign each shipment to a cluster CAN I PUT THIS FURTHER DOWN
        for (CarrierShipment shipment : singleCarrier.getShipments().values()){
            List<CarrierShipment> cluster = new ArrayList<>();
            cluster.add(shipment);
            clusters.add(cluster);
        }
        List<CarrierShipment> shipments = new ArrayList<>(singleCarrier.getShipments().values());
        int n = shipments.size();

        //Precompute coordinates
        Map<CarrierShipment, Coord> coords = new HashMap<>();
        for (CarrierShipment shipment : shipments) {
            coords.put(shipment, network.getLinks().get(shipment.getPickupLinkId()).getCoord());
        }

        //Precompute all edge distances
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                CarrierShipment a = shipments.get(i);
                CarrierShipment b = shipments.get(j);
                double dist = NetworkUtils.getEuclideanDistance(coords.get(a), coords.get(b));
                edges.add(new Edge(a, b, dist));
            }
        }

        //Sort edges by increasing distance
        edges.sort(Comparator.comparingDouble(Edge::distance));

        //Merge clusters from shortest edge until desired number of clusters is reached
        for (Edge e : edges) {
            CarrierShipment a = e.a();
            CarrierShipment b = e.b();
            int aIndex = getClusterIndex(a, clusters);
            int bIndex = getClusterIndex(b, clusters);
            //check if clusters are too large
            if (clusters.get(aIndex).size() + clusters.get(bIndex).size() > shipments.size()/numberOfCarriers + 1) {
                continue;
            }
            //check if in same cluster otherwise merge the higher index into the lower
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

    //returns which cluster the shipment is in
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

    private static List<List<CarrierShipment>> findMETISClusters(Carrier singleCarrier, Network network, int numberOfCarriers) throws IOException, InterruptedException {

        //The list of clusters that will be returned
        List<List<CarrierShipment>> clusters = new ArrayList<>();
        List<CarrierShipment> shipments = new ArrayList<>(singleCarrier.getShipments().values());
        for (int i = 0; i < numberOfCarriers; i++) {
            clusters.add(new ArrayList<>());
        }

        //Write the Graph files
        try (PrintWriter pw = new PrintWriter("input/METIS_Graphs/graphTest_" + singleCarrier.getId().toString() + ".txt")) {
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
                //We loop from the counter onward because edges shouldn't be added twice
                for (int j = 0; j < singleCarrier.getShipments().size(); j++) {
                    //Don't map to same Shipment
                    if (i == j) {
                        continue;
                    }
                    Coord toShipment = network.getLinks().get(shipments.get(j).getPickupLinkId()).getCoord();
                    int weight = (int) (10000 / NetworkUtils.getEuclideanDistance(fromShipment, toShipment));
                    //That is the upper bound of METIS Edge weights
                    if (weight > 1000) {
                        weight = 999;
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
            System.out.println("This many above " + above1000 + " and below " + below0  );
        }

        //Run METIS
        ProcessBuilder pb = new ProcessBuilder("wsl", "gpmetis", "/mnt/c/Users/Milo/Desktop/UniZeug/AbfallGit/input/METIS_Graphs/graphTest_" + singleCarrier.getId().toString() + ".txt", "" + numberOfCarriers);
        Process process = pb.start();
        process.waitFor();

        //Read the Results
        int shipmentCounter = 0;
        try (Scanner sc = new Scanner(new File("input/METIS_Graphs/graphTest_" + singleCarrier.getId().toString() + ".txt.part." + numberOfCarriers))) {
            while (sc.hasNextInt()) {
                clusters.get(sc.nextInt()).add(shipments.get(shipmentCounter));
                shipmentCounter++;
            }
        }

        return clusters;
    }

    //Determine number of new carriers
    private static int estimateNumberOfCarriers(int numberOfShipmentsPerCarrier, Carrier carrier) {
        int noOfCarriers = 0;
        //Float so that the round function works
        float noOfShipments = carrier.getShipments().size();
        System.out.println("NO OF SHIPMENTS: " + noOfShipments + " / NO OF SHIPMENTS PER CARRIER: " + numberOfShipmentsPerCarrier);
        noOfCarriers = Math.round(noOfShipments/numberOfShipmentsPerCarrier);
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
    private static void createXMLFacilities(Network network, Carriers carriers, String runName) {

        //Facilities and network setup
        final String FILENAME_EXPORT_FACILITIES = "input/" + runName + ".xml";
        ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities("facilities");

        //loop through all shipments
        for (Carrier carrier : carriers.getCarriers().values()) {
            //----ADDING DEPOT AND DROPOFF TO XML----
            String carrierName = carrier.getId().toString();
            //getting LinkIds  FIX IN CASE MULTIPLE DEPOTS OR DUMPS
            CarrierVehicle carrierVehicle = carrier.getCarrierCapabilities().getCarrierVehicles().values().iterator().next();
            CarrierShipment firstShipment = carrier.getShipments().values().iterator().next();
            //Geting the node coords
            final Coord depotCoord =  network.getLinks().get(carrierVehicle.getLinkId()).getCoord();
            final Coord dumpCoord =  network.getLinks().get(firstShipment.getDeliveryLinkId()).getCoord();
            //Creating a facility ID
            final Id<ActivityFacility> depotFacilityId = Id.create("depot_" + carrierName, ActivityFacility.class);
            final Id<ActivityFacility> dumpFacilityId = Id.create("dump_" + carrierName, ActivityFacility.class);
            //Creating the facilities
            ActivityFacility depotFacility = facilities.getFactory().createActivityFacility(depotFacilityId, depotCoord);
            ActivityFacility dumpFacility = facilities.getFactory().createActivityFacility(dumpFacilityId, dumpCoord);
            //Adding the activity option
            depotFacility.addActivityOption(new ActivityOptionImpl("depot"));
            dumpFacility.addActivityOption(new ActivityOptionImpl("dump"));
            //Putting the carrier attribute to view in Via later
            depotFacility.getAttributes().putAttribute("carrier", "depot_" + carrierName);
            dumpFacility.getAttributes().putAttribute("carrier", "dump_" + carrierName);
            //Adding the facilities to the scenario
            facilities.addActivityFacility(depotFacility);
            facilities.addActivityFacility(dumpFacility);

            //Add all Shipments
            for (CarrierShipment shipment : carrier.getShipments().values()) {

                //Retrieve Pickup Node Id
                List<Id<Link>> linkIds = List.of(shipment.getPickupLinkId());

                //Retrieve Pickup Node coord and create activityfacility
                final Coord coord = network.getLinks().get(shipment.getPickupLinkId()).getCoord();
                final Id<ActivityFacility> facilityId = Id.create(shipment.getId(), ActivityFacility.class);
                ActivityFacility facility = facilities.getFactory().createActivityFacility(facilityId, coord);
                facility.getAttributes().putAttribute("carrier", shipment.getAttributes().getAttribute("carrier").toString());
                if (shipment.getAttributes().getAttribute("seed") != null) {
                    facility.getAttributes().putAttribute("seed", shipment.getAttributes().getAttribute("seed").toString());
                }

                //add activity to xml
                facility.addActivityOption(new ActivityOptionImpl("delivery"));
                facilities.addActivityFacility(facility);
            }
        }

        //write the xml
        new FacilitiesWriter(facilities).writeV1(FILENAME_EXPORT_FACILITIES);
        System.out.println("write facilities to " + FILENAME_EXPORT_FACILITIES);
        System.out.println("done");
    }

}

