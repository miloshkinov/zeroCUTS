package org.matsim.vsp.wasteCollection.Berlin;

import com.graphhopper.jsprit.core.problem.job.Shipment;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.facilities.*;
import org.matsim.freight.carriers.*;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.*;

import static org.matsim.vsp.freight.CarrierScoringFunctionFactoryImpl_KT.scenario;

public class VrpSplitUtils {

    static String linkChessboardDepot = "j(0,7)R";
    static String linkChessboardDump = "j(0,9)R";

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
            List<Id<Link>> linkIds = List.of(shipment.getPickupLinkId());
            Id<Node> nodeId = NetworkUtils.getLinks(network,linkIds).get(0).getToNode().getId();

            //Retrieve Node coord and create activityfacility
            final Coord coord =  NetworkUtils.getNodes(network, nodeId.toString()).get(0).getCoord();
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

    static void splitCarriers(Scenario scenario, Run_Abfall.clusteringStrategy clusterStrategy , int numberOfCarriers, int numberOfIterations, String runName) {

        //Get network and initial carriers and create a new set
        Network network = scenario.getNetwork();
        Carriers carriers = CarriersUtils.getCarriers(scenario);
        Carriers newCarriers = new Carriers();

        //Loop through all carriers
        for (Carrier singleCarrier : carriers.getCarriers().values()) {

            //Get Carrier Vehicle and Name NEEDS TO BE FIXED FOR CARRIERS WITH MORE THAN 1 VEHICLE!!!!!
            CarrierVehicle carrierVehicle = singleCarrier.getCarrierCapabilities().getCarrierVehicles().values().iterator().next();
            String carrierName = singleCarrier.getId().toString();

            //Set up the desired number of new carriers
            for (int i = 1; i <= numberOfCarriers; i++) {
                Carrier newCarrier = createSingleCarrier(carrierName, numberOfIterations, carrierVehicle, i);
                newCarriers.addCarrier(newCarrier);
                System.out.println(newCarriers.getCarriers().size() + " carriers created");
            }

            //Get Clusters
            List<List<CarrierShipment>> clusters;
            switch (clusterStrategy) {
                case random -> {
                    clusters = findRandomClusters(singleCarrier, network, numberOfCarriers);
                }
                case seeding -> {
                    clusters = findSeedingClusters(singleCarrier, network, numberOfCarriers, carrierVehicle);
                }
                case kClusters -> {
                    clusters = findKClusters(singleCarrier, network, numberOfCarriers);
                }
                case null, default -> {
                    System.out.println("No Clustering Strategy Defined! Exit");
                    return;
                }
            }

            //loop through all clusters and assign to carrier
            for (int i = 0; i < clusters.size(); i++) {
                for (int j = 0; j < clusters.get(i).size(); j++) {
                    CarrierShipment shipment = clusters.get(i).get(j);
                    shipment.getAttributes().putAttribute("carrier", carrierName + (i + 1));
                    System.out.println("SHIPMENT " + shipment.getId().toString() + " ADDED TO " + carrierName + (i + 1));
                    CarriersUtils.addShipment(newCarriers.getCarriers().get(Id.create(carrierName + (i + 1), Carrier.class)), shipment);
                }
            }
        }

        //Put new carriers into scenario
        carriers.getCarriers().clear();
        for (Carrier singleCarrier : newCarriers.getCarriers().values()) {
            carriers.addCarrier(singleCarrier);
        }

        //create xml facilities file to visualise results
        createXMLFacilities(network, carriers, runName);

        System.out.println(clusterStrategy.toString() + " VRP Splitting complete");
    }

    private static List<List<CarrierShipment>> findRandomClusters(Carrier singleCarrier, Network network, int numberOfCarriers) {
        //The list of clusters that will be returned
        List<List<CarrierShipment>> clusters = new ArrayList<>();
        //picking a random seed
        Random randomSeed = new Random(1);
        //loop through all shipments
        for (CarrierShipment shipment : singleCarrier.getShipments().values()) {

            //Randomly assign the shipment to a new carrier
            long coinFlip = randomSeed.nextInt(numberOfCarriers);
            for (int i = 1; i <= numberOfCarriers; i++){
                if (coinFlip == i) {
                    clusters.get(i).add(shipment);
                }
            }
        }
        return clusters;
    }

    static void createGeoSeedCarriers (Scenario scenario, int numberOfCarriers, int numberOfIterations, String runName) {

        //Get network and initial carriers and create a new set
        Network network = scenario.getNetwork();
        Carriers carriers = CarriersUtils.getCarriers(scenario);
        Carriers newCarriers = new Carriers();

        //Loop through all carriers
        for (Carrier singleCarrier : carriers.getCarriers().values()) {

            //Get Carrier Vehicle and name NEEDS TO BE FIXED FOR CARRIERS WITH MORE THAN 1 VEHICLE!!!!!
            CarrierVehicle carrierVehicle = singleCarrier.getCarrierCapabilities().getCarrierVehicles().values().iterator().next();
            String carrierName = singleCarrier.getId().toString();

            //Determine the Seeds
            List<Coord> geoSeedCoords = findGeoSeeds(singleCarrier, network, numberOfCarriers, carrierVehicle);

            //Set up the desired number of new carriers
            for (int i = 1; i <= numberOfCarriers; i++) {
                Carrier newCarrier = createSingleCarrier(carrierName, numberOfIterations, carrierVehicle, i);
                newCarriers.addCarrier(newCarrier);
                System.out.println(newCarriers.getCarriers().size() + " carriers created");
            }

            //loop through all shipments
            for (CarrierShipment shipment : singleCarrier.getShipments().values()) {

                //Retrieve Pickup Node Id
                //System.out.println("SHIPMENT ID: " + shipment.getId() + " ,SHIPMENT START LINK ID: " + shipment.getPickupLinkId());
                List<Id<Link>> linkIds = List.of(shipment.getPickupLinkId());
                Id<Node> nodeId = NetworkUtils.getLinks(network,linkIds).get(0).getToNode().getId();

                //Retrieve Pickup Node coord
                final Coord coord =  NetworkUtils.getNodes(network, nodeId.toString()).get(0).getCoord();

                //Variables to track which carrier the shipment should be assigned to
                double minDistance = Double.MAX_VALUE;
                int seedNumber = 0;

                //loop through all seeds
                for (int i = 1; i <= geoSeedCoords.size(); i++) {
                    // -1 because index starts at 0, don't change without thinking
                    double distanceApart = NetworkUtils.getEuclideanDistance(coord, geoSeedCoords.get(i-1));
                    if (distanceApart < minDistance) {
                        seedNumber = i;
                        minDistance = distanceApart;
                    }
                }
                //Assign to Carrier
                if (minDistance == 0){
                    System.out.println("THIS IS A SEED " +  seedNumber);
                    shipment.getAttributes().putAttribute("seed", "seed" + seedNumber);
                }
                shipment.getAttributes().putAttribute("carrier", carrierName + seedNumber);
                System.out.println("SHIPMENT " + shipment.getId().toString() + " ADDED TO " + carrierName + seedNumber);
                CarriersUtils.addShipment(newCarriers.getCarriers().get(Id.create(carrierName + seedNumber, Carrier.class)), shipment);

            }
        }
        //Put new carriers into scenario
        carriers.getCarriers().clear();
        for (Carrier singleCarrier : newCarriers.getCarriers().values()) {
            carriers.addCarrier(singleCarrier);
        }

        //create xml facilities file to visualise results
        createXMLFacilities(network, carriers, runName);

        System.out.println("Seed Cluster VRP Splitting complete");
    }

    private static List<List<CarrierShipment>> findSeedingClusters(Carrier carrier, Network network, int numberOfCarriers, CarrierVehicle carrierVehicle) {
        //The list of clusters that will be returned
        List<List<CarrierShipment>> clusters = new ArrayList<>();

        //List to track coords that will be returned
        List<Coord> seedCoords = new ArrayList<>();
        List<Id<CarrierShipment>> seedCoordIds = new ArrayList<>();

        //Get Depot Coord DISCUSS WHAT TO DO WHEN MULTIPLE VEHICLES AND/OR MULTIPLE DEPOTS!!!!!!!!!
        Id<Node> depotNodeId = NetworkUtils.getLinks(network, carrierVehicle.getLinkId().toString()).get(0).getToNode().getId();
        Coord depotCoord =  NetworkUtils.getNodes(network, depotNodeId.toString()).get(0).getCoord();

        //Variables to track the max distances and coefficient to encourage clustering
		double maxDistance = 0;
        Coord seedCoord = null;
        Id<CarrierShipment> seedId = null;
        double clusterCoefficient = 2.0; //PLAY AROUND WITH THIS!!!!!!!!!!!

        //loop for amount of seeds required
        for (int i = 0; i < numberOfCarriers; i++) {

            //Find seed
            for (CarrierShipment shipment : carrier.getShipments().values()) {

                //Get Coord of Shipment
                List<Id<Link>> linkIds = List.of(shipment.getPickupLinkId());
                Id<Node> nodeId = NetworkUtils.getLinks(network,linkIds).get(0).getToNode().getId();
                final Coord shipmentCoord =  NetworkUtils.getNodes(network, nodeId.toString()).get(0).getCoord();

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

        return clusters;
    }

    private static List<List<CarrierShipment>> findKClusters(Carrier singleCarrier, Network network, int numberOfCarriers) {
        //The list of clusters that will be returned
        List<List<CarrierShipment>> clusters = new ArrayList<>();

        //Assign each shipment to a cluster
        for (CarrierShipment shipment : singleCarrier.getShipments().values()){
            List<CarrierShipment> cluster = new ArrayList<>();
            cluster.add(shipment);
            clusters.add(cluster);
        }

        //Loop through all Shipments until number of clusters is = number of carriers
        while (clusters.size() > numberOfCarriers) {
            //Variables for keeping track of shortest Link
            double minDistance = Double.MAX_VALUE;
            int fromClusterNumber = 0;
            int toClusterNUmber = 0;
            Coord fromCoord = null;
            Coord toCoord = null;
            CarrierShipment fromShipment = null;
            CarrierShipment toShipment = null;

            //loop through all connections to find the shortest one
            for (int i = 0; i < clusters.size(); i++) {
                for (int j = 0; j < clusters.get(i).size(); j++) {
                    //Get fromCoord
                    fromShipment = clusters.get(i).get(j);
                    List<Id<Link>> fromLinkIds = List.of(fromShipment.getPickupLinkId());
                    Id<Node> fromNodeId = NetworkUtils.getLinks(network,fromLinkIds).get(0).getToNode().getId();
                    fromCoord = NetworkUtils.getNodes(network, fromNodeId.toString()).get(0).getCoord();
                    //Now Loop Again
                    for (int x = i; x < clusters.size(); x++){
                        //skip to next cluster if last Shipment in cluster
                        if (j == clusters.get(i).size() - 1){
                            continue;
                        }
                        for (int y = j + 1; y < clusters.get(i).size(); y++){
                            //get to coord
                            toShipment = clusters.get(x).get(y);
                            List<Id<Link>> linkIds = List.of(toShipment.getPickupLinkId());
                            Id<Node> toNodeId = NetworkUtils.getLinks(network,linkIds).get(0).getToNode().getId();
                            toCoord = NetworkUtils.getNodes(network, toNodeId.toString()).get(0).getCoord();
                            //check if distance is new minimum
                            double distanceApart = NetworkUtils.getEuclideanDistance(fromCoord,toCoord);
                            if (distanceApart < minDistance){
                                //remember the clusters and update minDistance
                                minDistance = distanceApart;
                                fromClusterNumber = i;
                                toClusterNUmber = x;
                            }
                        }
                    }
                }
            }
            //Merge clusters of smallest link
            clusters.get(fromClusterNumber).addAll(clusters.get(toClusterNUmber));
            clusters.remove(toClusterNUmber);
        }

        return clusters;
    }

    //Create a basic carrier
    private static Carrier createSingleCarrier(String carrierName, int numberOfIterations, CarrierVehicle carrierVehicle, int carrierNumber) {
        Carrier newCarrier = CarriersUtils.createCarrier(Id.create(carrierName + carrierNumber, Carrier.class));
        //CarriersUtils.addCarrierVehicle(newCarrier, carrierVehicle);
        CarriersUtils.setJspritIterations(newCarrier, numberOfIterations);
        CarrierCapabilities carrierCapabilities = CarrierCapabilities.Builder.newInstance()  //LOOP THROUGH ALL VEHICLE TYPES TO FIX THIS
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
            List<Id<Link>> depotLinkIds = List.of(Id.createLinkId(carrierVehicle.getLinkId()));
            CarrierShipment firstShipment = carrier.getShipments().values().iterator().next();
            List<Id<Link>> dumpLinkIds = List.of(firstShipment.getDeliveryLinkId());
            //Getting node Ids from linkIds
            Id<Node> depotNodeId = NetworkUtils.getLinks(network,depotLinkIds).get(0).getToNode().getId();
            Id<Node> dumpNodeId = NetworkUtils.getLinks(network,dumpLinkIds).get(0).getToNode().getId();
            //Geting the node coords
            final Coord depotCoord =  NetworkUtils.getNodes(network, depotNodeId.toString()).get(0).getCoord();
            final Coord dumpCoord =  NetworkUtils.getNodes(network, dumpNodeId.toString()).get(0).getCoord();
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
                //System.out.println("SHIPMENT ID: " + shipment.getId() + "SHIPMENT START LINK ID: " + shipment.getPickupLinkId());
                List<Id<Link>> linkIds = List.of(shipment.getPickupLinkId());
                Id<Node> nodeId = NetworkUtils.getLinks(network,linkIds).get(0).getToNode().getId();

                //Retrieve Pickup Node coord and create activityfacility
                final Coord coord =  NetworkUtils.getNodes(network, nodeId.toString()).get(0).getCoord();
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

