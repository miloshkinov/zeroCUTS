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

public class VrpSplitUtils {

    static String linkChessboardDepot = "j(0,7)R";
    static String linkChessboardDump = "j(0,9)R";

    static String linkHaselhorstDepot = "27766";
    static String linkHaselhorstDump = "142010";

    static void createRandomCarriersChessboard(Scenario scenario, int numberOfCarriers, int numberOfIterations) {

        //Get initial capabilities and remove initial carrier
        Carriers carriers = CarriersUtils.getCarriers(scenario);
        var carrier = carriers.getCarriers().get(Id.create("Carrier_Chessboard", Carrier.class)); //<--- THIS and the following lines NEEDS CHANGING!
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
            Carrier newCarrier = createSingleCarrier(i, numberOfIterations, carrierVehicle);
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

    static void createRandomCarriers(Scenario scenario, int numberOfCarriers, int numberOfIterations) {

        //picking a random seed
        Random randomSeed = new Random(1);

        //Get network and initial carriers and create a new set
        Network network = scenario.getNetwork();
        Carriers carriers = CarriersUtils.getCarriers(scenario);
        Carriers newCarriers = new Carriers();

        //Loop through all carriers
        for (Carrier singleCarrier : carriers.getCarriers().values()) {

            //Get Carrier Vehicle NEEDS TO BE FIXED FOR CARRIERS WITH MORE THAN 1 VEHICLE!!!!!
            CarrierVehicle carrierVehicle = singleCarrier.getCarrierCapabilities().getCarrierVehicles().values().iterator().next();

            //Set up the desired number of new carriers
            for (int i = 1; i <= numberOfCarriers; i++){
                Carrier newCarrier = createSingleCarrier(i, numberOfIterations, carrierVehicle);
                newCarriers.addCarrier(newCarrier);
                System.out.println(newCarriers.getCarriers().size() + " carriers created");
            }

            //loop through all shipments
            for (CarrierShipment shipment : singleCarrier.getShipments().values()) {

                //Retrieve Pickup Node Id
                //System.out.println("SHIPMENT ID: " + shipment.getId() + "SHIPMENT START LINK ID: " + shipment.getPickupLinkId());
                List<Id<Link>> linkIds = List.of(shipment.getPickupLinkId());
                Id<Node> nodeId = NetworkUtils.getLinks(network,linkIds).get(0).getToNode().getId();

                //Retrieve Pickup Node coord
                final Coord coord =  NetworkUtils.getNodes(network, nodeId.toString()).get(0).getCoord();

                //Randomly assign the shipment to a new carrier THINK ABOUT THIS WHEN THERE ARE MORE THAN 1 OLD CARRIERS COS OF THE NEW CARRIER NAMES!!
                long coinFlip = randomSeed.nextInt(numberOfCarriers) + 1;
                for (int i = 1; i <= numberOfCarriers; i++){
                    if (coinFlip == i) {
                        shipment.getAttributes().putAttribute("carrier", "newCarrier" + i);
                        CarriersUtils.addShipment(newCarriers.getCarriers().get(Id.create("Carrier" + i, Carrier.class)), shipment);
                        System.out.println("SHIPMENT " + shipment.getId().toString() + " ADDED TO CARRIER " + i);
                    }
                }
            }
        }
        //Put new carriers into scenario
        carriers.getCarriers().clear();
        for (Carrier singleCarrier : newCarriers.getCarriers().values()) {
            carriers.addCarrier(singleCarrier);
        }

        //create xml facilities file to visualise results
        createXMLFacilities(network, carriers);

        System.out.println("Random VRP Splitting complete");
    }

    static void creatGeoSeedCarriers (Scenario scenario, int numberOfCarriers, int numberOfIterations) {

        //Get network and initial carriers and create a new set
        Network network = scenario.getNetwork();
        Carriers carriers = CarriersUtils.getCarriers(scenario);
        Carriers newCarriers = new Carriers();

        //Loop through all carriers
        for (Carrier singleCarrier : carriers.getCarriers().values()) {

            //Get Carrier Vehicle NEEDS TO BE FIXED FOR CARRIERS WITH MORE THAN 1 VEHICLE!!!!!
            CarrierVehicle carrierVehicle = singleCarrier.getCarrierCapabilities().getCarrierVehicles().values().iterator().next();

            //Determine the Seeds
            List<Coord> geoSeedCoords = findGeoSeeds(singleCarrier, network, numberOfCarriers);

            //Set up the desired number of new carriers
            for (int i = 1; i <= numberOfCarriers; i++) {
                Carrier newCarrier = createSingleCarrier(i, numberOfIterations, carrierVehicle);
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
                shipment.getAttributes().putAttribute("carrier", "newCarrier" + seedNumber);
                System.out.println("SHIPMENT " + shipment.getId().toString() + " ADDED TO CARRIER " + seedNumber);
                CarriersUtils.addShipment(newCarriers.getCarriers().get(Id.create("Carrier" + seedNumber, Carrier.class)), shipment);

            }
        }
        //Put new carriers into scenario
        carriers.getCarriers().clear();
        for (Carrier singleCarrier : newCarriers.getCarriers().values()) {
            carriers.addCarrier(singleCarrier);
        }

        //create xml facilities file to visualise results
        createXMLFacilities(network, carriers);

        System.out.println("Geocluster VRP Splitting complete");
    }

    private static List<Coord> findGeoSeeds(Carrier carrier, Network network, int numberOfCarriers) {

        //List to track coords that will be returned
        List<Coord> seedCoords = new ArrayList<>();
        List<Id<CarrierShipment>> seedCoordIds = new ArrayList<>();

        //Get Depot Coord THIS NEEDS TO BE IMPROVED
        Id<Node> depotNodeId = NetworkUtils.getLinks(network, linkHaselhorstDepot).get(0).getFromNode().getId();
        Coord depotCoord =  NetworkUtils.getNodes(network, depotNodeId.toString()).get(0).getCoord();

        //Variables to track the max distances and coefficient to encourage clustering
		double maxDistance = 0;
        Coord seedCoord = null;
        Id<CarrierShipment> seedId = null;
        double clusterCoefficient = 2.0; //PLAY AROUND WITH THIS

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

        //return arraylist
        return seedCoords;
    }

    //Create a basic carrier
    private static Carrier createSingleCarrier(int carrierNumber, int numberOfIterations, CarrierVehicle carrierVehicle) {
        Carrier newCarrier = CarriersUtils.createCarrier(Id.create("Carrier" + carrierNumber, Carrier.class));
        CarriersUtils.addCarrierVehicle(newCarrier, carrierVehicle);
        CarriersUtils.setJspritIterations(newCarrier, numberOfIterations);

        return newCarrier;
    }

    //Create XML Facilities File (IS THIS REALLY NECESSARY OR JUST A COOL FEATURE?
    private static void createXMLFacilities(Network network, Carriers carriers) {

        //Facilities and network setup
        final String FILENAME_EXPORT_FACILITIES = "input/facilitiesHaselhorstGeoSplit.xml";  //THINK OF HOW TO MAKE THIS EASIER AND LESS MANUAL
        ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities("facilities");

        //----ADDING DEPOT AND DROPOFF TO XML----
        //getting LinkIds
        List<Id<Link>> depotLinkIds = List.of(Id.createLinkId(linkHaselhorstDepot));         //IMPROVE THIS FOR ALL CASES!! COULD PUT INTO THE CARRIER FOR LOOP
        List<Id<Link>> dumpLinkIds = List.of(Id.createLinkId(linkHaselhorstDump));           //IMPROVE THIS FOR ALL CASES!!
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

        //loop through all shipments
        for (Carrier carrier : carriers.getCarriers().values()) {
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

