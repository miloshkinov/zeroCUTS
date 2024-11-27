package org.matsim.vsp.wasteCollection.Vulkaneifel.run;
import com.opencsv.exceptions.CsvException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.freight.carriers.*;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReaderBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.FileInputStream;
import java.nio.file.Path;

public class AbfallUtils {
    private static final Logger log = LogManager.getLogger(AbfallUtils.class);

    static String linkDepotPruem = "1071599120001r";
    static String linkDepotBitburg = "2709393010002r-2733217030002r-4631046070013r-4631046080001r";
    static String linkDepotNickenich = "3243409100006r-4858638440008r-4858649690002r";
    static String linkDepotPlaidt = "5574439310007f";
    static String linkDepotHetzerath = "3118011660035f";

    static String existingCarriersPath;
    static String carrierVehicleTypesFilePath = "scenarios/wasteCollection/Vulkaneifel/vehicles/vehicleTypes_new.xml";
    static String networkPath = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/vulkaneifel/v1.1/input/vulkaneifel-v1.1-network.xml.gz";
    static int jspritIterations; //die Jsprit müssen immer min 1 sein, sonst wird keine Route berechnet



    public static Config prepareConfig(String output) { //planpath removed, because its not needed
        Config config = ConfigUtils.createConfig();
        //config.plans().setInputFile(planPath);
        config.global().setCoordinateSystem("EPSG:25832");
        config.controller().setOutputDirectory(output);
        config.controller().setLastIteration(0); //only one MATSim iteration
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

        FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
        log.info("Read carrier vehicle types from: {}", carrierVehicleTypesFilePath);
        freightCarriersConfigGroup.setCarriersVehicleTypesFile(carrierVehicleTypesFilePath);

        log.info("Read network from: {}", networkPath);
        config.network().setInputFile(networkPath);
        //log.info("Set output directory to: " + output);
        //config.controller().setOutputDirectory(output.toString());
        new OutputDirectoryHierarchy(config.controller().getOutputDirectory(), config.controller().getRunId(),
                config.controller().getOverwriteFileSetting(), ControllerConfigGroup.CompressionType.gzip);
        assert (new File(Path.of(config.controller().getOutputDirectory()).toString()).mkdir());
        return config;
    }

    public static void createCarriers(Carriers carriers, int jspritIterations) {
        Carrier carrierShipments = CarriersUtils.createCarrier(Id.create("carrierShipments", Carrier.class));
        carriers.addCarrier(carrierShipments);

        for (Carrier thisCarrier : carriers.getCarriers().values()) {
            log.info("Set jsprit iterations to: {} for carrier: {}", jspritIterations, thisCarrier.getId().toString());
            CarriersUtils.setJspritIterations(thisCarrier, jspritIterations);
        }
    }


     void createFreightVehicles(Scenario scenario, CarrierVehicleTypes carrierVehicleTypes) {
        CarrierCapabilities carrierCapabilities = CarrierCapabilities.Builder.newInstance().setFleetSize(
                CarrierCapabilities.FleetSize.INFINITE).build(); //Finite nur die Anzahl die vorher angibt, INFINITE optimale anzahl

        List<String> vehicleDepots = List.of(linkDepotPruem,linkDepotBitburg, linkDepotHetzerath, linkDepotNickenich, linkDepotPlaidt); // List of starting Depots of the vehicles
        for (Carrier thisCarrier : CarriersUtils.addOrGetCarriers(scenario).getCarriers().values()) {
            for (String singleDepot : vehicleDepots) {
                int vehicleStartTime = 6 * 3600; //in Sekunden
                int vehicleEndTime = (int) (15 * 3600); //in Sekunden
                for (VehicleType thisVehicleType : carrierVehicleTypes.getVehicleTypes().values()) {
                        VehicleType thisType = carrierVehicleTypes.getVehicleTypes().get(thisVehicleType.getId());

                        CarrierVehicle newCarrierVehicle = CarrierVehicle.Builder.newInstance(
                                Id.create(thisCarrier.getId().toString() + "_" + (carrierCapabilities.getCarrierVehicles().size() + 1), Vehicle.class),
                                Id.createLinkId(singleDepot), thisType).setEarliestStart(vehicleStartTime).setLatestEnd(vehicleEndTime).build();
                        carrierCapabilities.getCarrierVehicles().put(newCarrierVehicle.getId(), newCarrierVehicle);
                        if (!carrierCapabilities.getVehicleTypes().contains(thisType))
                            carrierCapabilities.getVehicleTypes().add(thisType);

                }
            }
            thisCarrier.setCarrierCapabilities(carrierCapabilities);
        }
    }
    // not most elegant method but it works ;)
    static void createFreightVehicles_OnlyEVs(Scenario scenario, CarrierVehicleTypes carrierVehicleTypes) {
        CarrierCapabilities carrierCapabilities = CarrierCapabilities.Builder.newInstance().setFleetSize(
                CarrierCapabilities.FleetSize.INFINITE).build(); //Finite nur die Anzahl die vorher angibt, INFINITE optimale anzahl

        Id<VehicleType> vehicleTypeId = Id.create("diesel_vehicle", VehicleType.class);
        VehicleType dieselType = carrierVehicleTypes.getVehicleTypes().get(vehicleTypeId);


        List<String> vehicleDepots = List.of(linkDepotPruem); // List of starting Depots of the vehicles
        for (Carrier thisCarrier : CarriersUtils.addOrGetCarriers(scenario).getCarriers().values()) {
            for (String singleDepot : vehicleDepots) {
                int vehicleStartTime = 6 * 3600; //in Sekunden
                int vehicleEndTime = (int) (15 * 3600); //in Sekunden
                for (VehicleType thisVehicleType : carrierVehicleTypes.getVehicleTypes().values()) {
                    VehicleType thisType = carrierVehicleTypes.getVehicleTypes().get(thisVehicleType.getId());
                    if(thisType.equals(dieselType)) {
                        // skip
                    }else{
                        CarrierVehicle newCarrierVehicle = CarrierVehicle.Builder.newInstance(
                                Id.create(thisCarrier.getId().toString() + "_" + (carrierCapabilities.getCarrierVehicles().size() + 1), Vehicle.class),
                                Id.createLinkId(singleDepot), thisType).setEarliestStart(vehicleStartTime).setLatestEnd(vehicleEndTime).build();
                        carrierCapabilities.getCarrierVehicles().put(newCarrierVehicle.getId(), newCarrierVehicle);
                        if (!carrierCapabilities.getVehicleTypes().contains(thisType))
                            carrierCapabilities.getVehicleTypes().add(thisType);
                    }
                }
            }
            thisCarrier.setCarrierCapabilities(carrierCapabilities);
        }
    }


    public static void createFreightVehiclesSingleType(Scenario scenario, CarrierVehicleTypes carrierVehicleTypes, String choosentype) {
        CarrierCapabilities carrierCapabilities = CarrierCapabilities.Builder.newInstance().setFleetSize(
                CarrierCapabilities.FleetSize.INFINITE).build(); //Finite nur die Anzahl die vorher angibt, INFINITE optimale anzahl

        Map<Id<VehicleType>, VehicleType> thisVehicleType = carrierVehicleTypes.getVehicleTypes();

        Id<VehicleType> vehicleTypeId = Id.create(choosentype, VehicleType.class);
        VehicleType thisType = thisVehicleType.get(vehicleTypeId);
        List<String> vehicleDepots = List.of(linkDepotPruem); // List of starting Depots of the vehicles
        for (Carrier thisCarrier : CarriersUtils.addOrGetCarriers(scenario).getCarriers().values()) {
            for (String singleDepot : vehicleDepots) {
                int vehicleStartTime = 6 * 3600; //in Sekunden
                int vehicleEndTime = (int) (15 * 3600);  //in Sekunden


                    CarrierVehicle newCarrierVehicle = CarrierVehicle.Builder.newInstance(
                            Id.create(thisCarrier.getId().toString() + "_" + (carrierCapabilities.getCarrierVehicles().size() + 1), Vehicle.class),
                            Id.createLinkId(singleDepot), thisType).setEarliestStart(vehicleStartTime).setLatestEnd(vehicleEndTime).build();
                    carrierCapabilities.getCarrierVehicles().put(newCarrierVehicle.getId(), newCarrierVehicle);
                    if (!carrierCapabilities.getVehicleTypes().contains(thisType))
                        carrierCapabilities.getVehicleTypes().add(thisType);


            }
            thisCarrier.setCarrierCapabilities(carrierCapabilities);
        }
    }
    static void createJobswithsphapes(Scenario scenario, Geometry gemeindegeometry, Population population, int volume, String choosenVehicletype,int counter) {

        Carrier carrierShipments = CarriersUtils.addOrGetCarriers(scenario).getCarriers().get(Id.create("carrierShipments", Carrier.class));

        log.info("Create Shipments at each link in Shapefile; Zum Beispiel Müllsammlung mit Transport des Mülls zu einer Deponie");
        String destinationLink = "370587200013f"; //Mülldeponie EVZ Walsdorf
        //int volume = 200; // Müllmenge pro Haushalt in L

        int payload_EV_Medium = 10500;
        int payload_EV_small_diesel = 11500;

        double pickupServiceTime = 19; //
        TimeWindow pickupTimeWindow = TimeWindow.newInstance(6 * 3600, 18 * 3600);
        //double deliveryServiceTime = 2; //delivery time per shipment at the dump
        TimeWindow deliveryTimeWindow = TimeWindow.newInstance(6 * 3600, 18 * 3600);

        //int counter = 0;

        if (choosenVehicletype.equals("EV_medium_battery")){
            int shipment_amount = payload_EV_Medium/volume;
            double deliveryServiceTime = 3600/(double)shipment_amount;

            for (Person person :population.getPersons().values()) {
                for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
                    if (planElement instanceof Activity activity) {
                        if (activity.getType().contains("home")) {
                            var coord = activity.getCoord();
                            var geotoolsPoint = MGC.coord2Point(coord);

                            if (gemeindegeometry.contains(geotoolsPoint)) {
                                counter++;
                                System.out.println("PersonID: " + person.getId().toString() + " counter: " + counter + " LinkID: " + activity.getLinkId().toString());
                                Id<Link> demandLinkId = activity.getLinkId(); //Ort der Sammlung des Mülls
                                CarrierShipment thisShipment = CarrierShipment.Builder.newInstance(Id.create("idNewShipment_"+activity.getLinkId().toString(),CarrierShipment.class), demandLinkId, Id.createLinkId(destinationLink),
                                        volume).setPickupServiceTime(pickupServiceTime).setPickupTimeWindow(pickupTimeWindow).setDeliveryServiceTime(deliveryServiceTime).setDeliveryTimeWindow(deliveryTimeWindow).build();
                                CarriersUtils.addShipment(carrierShipments, thisShipment);

                                break;
                            }
                        }
                    }
                }
            }
        } else{
            int shipment_amount = payload_EV_small_diesel/volume;
            double deliveryServiceTime = 3600/(double)shipment_amount;

            for (Person person :population.getPersons().values()) {
                for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
                    if (planElement instanceof Activity activity) {
                        if (activity.getType().contains("home")) {
                            var coord = activity.getCoord();
                            var geotoolsPoint = MGC.coord2Point(coord);

                            if (gemeindegeometry.contains(geotoolsPoint)) {
                                counter++;
                                System.out.println("PersonID: " + person.getId().toString() + " counter: " + counter + " LinkID: " + activity.getLinkId().toString());
                                Id<Link> demandLinkId = activity.getLinkId(); //Ort der Sammlung des Mülls
                                CarrierShipment thisShipment = CarrierShipment.Builder.newInstance(Id.create("idNewShipment"+activity.getLinkId().toString(),CarrierShipment.class), demandLinkId, Id.createLinkId(destinationLink),
                                        volume).setPickupServiceTime(pickupServiceTime).setPickupTimeWindow(pickupTimeWindow).setDeliveryServiceTime(deliveryServiceTime).setDeliveryTimeWindow(deliveryTimeWindow).build();
                                CarriersUtils.addShipment(carrierShipments, thisShipment);

                                break;
                            }
                        }
                    }
                }
            }
        }
    } // creates shipments but shipments with the same link are just copied and replaced

    public static int createJobswithsphapes_V3(Scenario scenario, Geometry gemeindegeometry, Population population, int volume, String choosenVehicletype, int counter) {

        Carrier carrierShipments = CarriersUtils.addOrGetCarriers(scenario).getCarriers().get(Id.create("carrierShipments", Carrier.class));
        Set<Id<Person>> processedPersons = new HashSet<>();
        log.info("Create Shipments at each link in Shapefile; Zum Beispiel Müllsammlung mit Transport des Mülls zu einer Deponie");
        String destinationLink = "370587200013f"; //Mülldeponie EVZ Walsdorf
        //int volume = 200; // Müllmenge pro Haushalt in L

        int payload_EV_Medium = 10500;
        int payload_EV_small_diesel = 11500;
        double pickupTimebetweenShipments = 15; // Zwischenfahrzeit, wenn Shipment in der selben Straße/Link liegt
        double pickupServiceTime = 19; // entnommen aus Studie von Umweltbundesamt
        TimeWindow pickupTimeWindow = TimeWindow.newInstance(6 * 3600, 15 * 3600);
        //double deliveryServiceTime = 2; //delivery time per shipment at the dump
        TimeWindow deliveryTimeWindow = TimeWindow.newInstance(6 * 3600, 15 * 3600);

        //int counter = 0;
        int shipment_amount;
        double deliveryServiceTime;
        if (choosenVehicletype.equals("EV_medium_battery")) {
            shipment_amount = payload_EV_Medium / volume;
            deliveryServiceTime = 3600 / (double) shipment_amount; //~17,91 s
        } else {
            shipment_amount = payload_EV_small_diesel / volume;
            deliveryServiceTime = 3600 / (double) shipment_amount; // ~16,29 s
        }
        for (Person person : population.getPersons().values()) {
            if (processedPersons.contains(person.getId())) {
                continue; // Skip if the person has already been processed
            }
            for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
                if (planElement instanceof Activity activity) {
                    if (activity.getType().contains("home")) {
                        var coord = activity.getCoord();
                        var geotoolsPoint = MGC.coord2Point(coord);

                        if (gemeindegeometry.contains(geotoolsPoint)) {

                            log.info("PersonID: " + person.getId().toString() + " counter: " + counter + " LinkID: " + activity.getLinkId().toString());
                            Id<Link> demandLinkId = activity.getLinkId(); //Ort der Sammlung des Mülls

                            if (carrierShipments.getShipments().containsKey(Id.create("idNewShipment_" + activity.getLinkId().toString(), CarrierShipment.class))) {

                                CarrierShipment oldShipment = carrierShipments.getShipments().get(Id.create("idNewShipment_" + activity.getLinkId().toString(), CarrierShipment.class));
                                double oldPickupServiceTime = oldShipment.getPickupServiceTime();
                                double oldDeliveryServiceTime = oldShipment.getDeliveryServiceTime();
                                int oldvolume = oldShipment.getSize();

                                oldPickupServiceTime = oldPickupServiceTime + pickupServiceTime + pickupTimebetweenShipments;
                                oldDeliveryServiceTime = oldDeliveryServiceTime + deliveryServiceTime;
                                oldvolume = oldvolume + volume;
                                CarrierShipment thisShipment = CarrierShipment.Builder.newInstance(Id.create("idNewShipment_" + activity.getLinkId().toString(), CarrierShipment.class), demandLinkId, Id.createLinkId(destinationLink),
                                        oldvolume).setPickupServiceTime(oldPickupServiceTime).setPickupTimeWindow(pickupTimeWindow).setDeliveryServiceTime(oldDeliveryServiceTime).setDeliveryTimeWindow(deliveryTimeWindow).build();
                                CarriersUtils.addShipment(carrierShipments, thisShipment);

                                log.info("created----" + "PersonID: " + person.getId().toString() + " counter: " + counter + " LinkID: " + activity.getLinkId().toString() + "_multiple");

                            } else {
                                log.info("created----" + "PersonID: " + person.getId().toString() + " counter: " + counter + " LinkID: " + activity.getLinkId().toString());
                                CarrierShipment thisShipment = CarrierShipment.Builder.newInstance(Id.create("idNewShipment_" + activity.getLinkId().toString(), CarrierShipment.class), demandLinkId, Id.createLinkId(destinationLink),
                                        volume).setPickupServiceTime(pickupServiceTime).setPickupTimeWindow(pickupTimeWindow).setDeliveryServiceTime(deliveryServiceTime).setDeliveryTimeWindow(deliveryTimeWindow).build();
                                CarriersUtils.addShipment(carrierShipments, thisShipment);
                            }
                            processedPersons.add(person.getId());
                            counter++;
                            break; // Exit the inner loop after processing the first home activity to ensure we do not create multiple shipments for the same person
                        }
                    }
                }
            }
        }
        return counter;
    } //V3 slimmer Version of V2

    // V4 for scenario with both EVs
    public static int createJobswithsphapes_V4(Scenario scenario, Geometry gemeindegeometry, Population population, int volume, int counter) {

        Carrier carrierShipments = CarriersUtils.addOrGetCarriers(scenario).getCarriers().get(Id.create("carrierShipments", Carrier.class));
        Set<Id<Person>> processedPersons = new HashSet<>();
        log.info("Create Shipments at each link in Shapefile; Zum Beispiel Müllsammlung mit Transport des Mülls zu einer Deponie");
        String destinationLink = "370587200013f"; //Mülldeponie EVZ Walsdorf
        //int volume = 200; // Müllmenge pro Haushalt in L

        //int payload_EV_Medium = 10500;
        //int payload_EV_small_diesel = 11500;
        int payload = 11000;
        double pickupTimebetweenShipments = 15; // Zwischenfahrzeit, wenn Shipment in der selben Straße/Link liegt
        double pickupServiceTime = 19; // entnommen aus Studie von Umweltbundesamt
        TimeWindow pickupTimeWindow = TimeWindow.newInstance(6 * 3600, 15 * 3600);
        //double deliveryServiceTime = 2; //delivery time per shipment at the dump
        TimeWindow deliveryTimeWindow = TimeWindow.newInstance(6 * 3600, 15 * 3600);

        //int counter = 0;
        int shipment_amount = payload / volume;
        double deliveryServiceTime = (60*45) / (double) shipment_amount; // changed to 45min breaks, 17.0.... AVG of both times for EV medium and EV small vehicles 16.29, 17.91, => 17.1; different deliverytimes result inconsistent results,
        //double deliveryServiceTime = 17.1 ;

        for (Person person : population.getPersons().values()) {
            if (processedPersons.contains(person.getId())) {
                continue; // Skip if the person has already been processed
            }
            for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
                if (planElement instanceof Activity activity) {
                    if (activity.getType().contains("home")) {
                        var coord = activity.getCoord();
                        var geotoolsPoint = MGC.coord2Point(coord);

                        if (gemeindegeometry.contains(geotoolsPoint)) {

                            log.info("PersonID: " + person.getId().toString() + " counter: " + counter + " LinkID: " + activity.getLinkId().toString());
                            Id<Link> demandLinkId = activity.getLinkId(); //Ort der Sammlung des Mülls

                            if (carrierShipments.getShipments().containsKey(Id.create("idNewShipment_" + activity.getLinkId().toString(), CarrierShipment.class))) {

                                CarrierShipment oldShipment = carrierShipments.getShipments().get(Id.create("idNewShipment_" + activity.getLinkId().toString(), CarrierShipment.class));
                                double oldPickupServiceTime = oldShipment.getPickupServiceTime();
                                double oldDeliveryServiceTime = oldShipment.getDeliveryServiceTime();
                                int oldvolume = oldShipment.getSize();

                                oldPickupServiceTime = oldPickupServiceTime + pickupServiceTime + pickupTimebetweenShipments;
                                oldDeliveryServiceTime = oldDeliveryServiceTime + deliveryServiceTime;
                                oldvolume = oldvolume + volume;
                                CarrierShipment thisShipment = CarrierShipment.Builder.newInstance(Id.create("idNewShipment_" + activity.getLinkId().toString(), CarrierShipment.class), demandLinkId, Id.createLinkId(destinationLink),
                                        oldvolume).setPickupServiceTime(oldPickupServiceTime).setPickupTimeWindow(pickupTimeWindow).setDeliveryServiceTime(oldDeliveryServiceTime).setDeliveryTimeWindow(deliveryTimeWindow).build();
                                CarriersUtils.addShipment(carrierShipments, thisShipment);

                                log.info("created----" + "PersonID: " + person.getId().toString() + " counter: " + counter + " LinkID: " + activity.getLinkId().toString() + "_multiple");

                            } else {
                                log.info("created----" + "PersonID: " + person.getId().toString() + " counter: " + counter + " LinkID: " + activity.getLinkId().toString());
                                CarrierShipment thisShipment = CarrierShipment.Builder.newInstance(Id.create("idNewShipment_" + activity.getLinkId().toString(), CarrierShipment.class), demandLinkId, Id.createLinkId(destinationLink),
                                        volume).setPickupServiceTime(pickupServiceTime).setPickupTimeWindow(pickupTimeWindow).setDeliveryServiceTime(deliveryServiceTime).setDeliveryTimeWindow(deliveryTimeWindow).build();
                                CarriersUtils.addShipment(carrierShipments, thisShipment);
                            }
                            processedPersons.add(person.getId());
                            counter++;
                            break; // Exit the inner loop after processing the first home activity to ensure we do not create multiple shipments for the same person
                        }
                    }
                }
            }
        }
        return counter;
    } //V4 slimmer Version of V2

    public static int createJobswithsphapes_V2(Scenario scenario, Geometry gemeindegeometry, Population population, int volume, String choosenVehicletype, int counter) {

        Carrier carrierShipments = CarriersUtils.addOrGetCarriers(scenario).getCarriers().get(Id.create("carrierShipments", Carrier.class));

        log.info("Create Shipments at each link in Shapefile; Zum Beispiel Müllsammlung mit Transport des Mülls zu einer Deponie");
        String destinationLink = "370587200013f"; //Mülldeponie EVZ Walsdorf
        //int volume = 200; // Müllmenge pro Haushalt in L

        int payload_EV_Medium = 10500;
        int payload_EV_small_diesel = 11500;
        double pickupTimebetweenShipments = 15; // Zwischenfahrzeit, wenn Shipment in der selben Straße/Link liegt
        double pickupServiceTime = 19; //
        TimeWindow pickupTimeWindow = TimeWindow.newInstance(6 * 3600, 18 * 3600);
        //double deliveryServiceTime = 2; //delivery time per shipment at the dump
        TimeWindow deliveryTimeWindow = TimeWindow.newInstance(6 * 3600, 18 * 3600);

        //int counter = 0;

        if (choosenVehicletype.equals("EV_medium_battery")){
            int shipment_amount = payload_EV_Medium/volume;
            double deliveryServiceTime = 3600/(double)shipment_amount;

            for (Person person :population.getPersons().values()) {
                for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
                    if (planElement instanceof Activity activity) {
                        if (activity.getType().contains("home")) {
                            var coord = activity.getCoord();
                            var geotoolsPoint = MGC.coord2Point(coord);

                            if (gemeindegeometry.contains(geotoolsPoint)) {
                                counter++;
                                log.info("PersonID: " + person.getId().toString() + " counter: " + counter + " LinkID: " + activity.getLinkId().toString());
                                Id<Link> demandLinkId = activity.getLinkId(); //Ort der Sammlung des Mülls


                                if(carrierShipments.getShipments().containsKey(Id.create("idNewShipment_" + activity.getLinkId().toString(), CarrierShipment.class))) {
                                    CarrierShipment oldShipment = carrierShipments.getShipments().get(Id.create("idNewShipment_" + activity.getLinkId().toString(), CarrierShipment.class));
                                    double oldPickupServiceTime = oldShipment.getPickupServiceTime();
                                    double oldDeliveryServiceTime = oldShipment.getDeliveryServiceTime();
                                    int oldvolume = oldShipment.getSize();

                                    oldPickupServiceTime = oldPickupServiceTime+ pickupServiceTime + pickupTimebetweenShipments;
                                    oldDeliveryServiceTime = oldDeliveryServiceTime + deliveryServiceTime;
                                    oldvolume = oldvolume + volume;
                                    CarrierShipment thisShipment = CarrierShipment.Builder.newInstance(Id.create("idNewShipment_" + activity.getLinkId().toString(), CarrierShipment.class), demandLinkId, Id.createLinkId(destinationLink),
                                            oldvolume).setPickupServiceTime(oldPickupServiceTime).setPickupTimeWindow(pickupTimeWindow).setDeliveryServiceTime(oldDeliveryServiceTime).setDeliveryTimeWindow(deliveryTimeWindow).build();
                                    CarriersUtils.addShipment(carrierShipments, thisShipment);

                                    log.info("created----" + "PersonID: " + person.getId().toString()+ " LinkID: " + activity.getLinkId().toString() + " ");
                                }else{

                                    log.info("created----" + "PersonID: " + person.getId().toString() + " counter: " + counter + " LinkID: " + activity.getLinkId().toString());
                                    CarrierShipment thisShipment = CarrierShipment.Builder.newInstance(Id.create("idNewShipment_"+activity.getLinkId().toString(),CarrierShipment.class), demandLinkId, Id.createLinkId(destinationLink),
                                            volume).setPickupServiceTime(pickupServiceTime).setPickupTimeWindow(pickupTimeWindow).setDeliveryServiceTime(deliveryServiceTime).setDeliveryTimeWindow(deliveryTimeWindow).build();
                                    CarriersUtils.addShipment(carrierShipments, thisShipment);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        } else{
            int shipment_amount = payload_EV_small_diesel/volume;
            double deliveryServiceTime = 3600/(double)shipment_amount;

            for (Person person :population.getPersons().values()) {
                for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
                    if (planElement instanceof Activity activity) {
                        if (activity.getType().contains("home")) {
                            var coord = activity.getCoord();
                            var geotoolsPoint = MGC.coord2Point(coord);

                            if (gemeindegeometry.contains(geotoolsPoint)) {
                                counter++;
                                log.info("PersonID: " + person.getId().toString() + " counter: " + counter + " LinkID: " + activity.getLinkId().toString());
                                Id<Link> demandLinkId = activity.getLinkId(); //Ort der Sammlung des Mülls

                                if(carrierShipments.getShipments().containsKey(Id.create("idNewShipment_" + activity.getLinkId().toString(), CarrierShipment.class))) {
                                    CarrierShipment oldShipment = carrierShipments.getShipments().get(Id.create("idNewShipment_" + activity.getLinkId().toString(), CarrierShipment.class));
                                    double oldPickupServiceTime = oldShipment.getPickupServiceTime();
                                    double oldDeliveryServiceTime = oldShipment.getDeliveryServiceTime();
                                    int oldvolume = oldShipment.getSize();

                                    oldPickupServiceTime = oldPickupServiceTime+ pickupServiceTime + pickupTimebetweenShipments;
                                    oldDeliveryServiceTime = oldDeliveryServiceTime + deliveryServiceTime;
                                    oldvolume = oldvolume + volume;
                                    CarrierShipment thisShipment = CarrierShipment.Builder.newInstance(Id.create("idNewShipment_" + activity.getLinkId().toString(), CarrierShipment.class), demandLinkId, Id.createLinkId(destinationLink),
                                            oldvolume).setPickupServiceTime(oldPickupServiceTime).setPickupTimeWindow(pickupTimeWindow).setDeliveryServiceTime(oldDeliveryServiceTime).setDeliveryTimeWindow(deliveryTimeWindow).build();
                                    CarriersUtils.addShipment(carrierShipments, thisShipment);

                                    log.info("created----" + "PersonID: " + person.getId().toString()+ " LinkID: " + activity.getLinkId().toString() + "_multiple");
                                }else{
                                    log.info("created----" + "PersonID: " + person.getId().toString() + " counter: " + counter + " LinkID: " + activity.getLinkId().toString());
                                    CarrierShipment thisShipment = CarrierShipment.Builder.newInstance(Id.create("idNewShipment_"+activity.getLinkId().toString(),CarrierShipment.class), demandLinkId, Id.createLinkId(destinationLink),
                                            volume).setPickupServiceTime(pickupServiceTime).setPickupTimeWindow(pickupTimeWindow).setDeliveryServiceTime(deliveryServiceTime).setDeliveryTimeWindow(deliveryTimeWindow).build();
                                    CarriersUtils.addShipment(carrierShipments, thisShipment);
                                }

                                 break; // do not forget that break, without people with multipe home activities will get multiple shipments
                            }
                        }
                    }
                }
            }
        }
        return counter;
    } //V2 summmarizes shipments with the same link into one shipment, adding volume, delivery time  pickup time
    public static List<Schedule> readCsvFile(String filePath) {
        List<Schedule> schedules = new ArrayList<>();

        try (InputStreamReader isr = new InputStreamReader(new FileInputStream(filePath), StandardCharsets.ISO_8859_1)) {
            // Configure the CSVParserBuilder with a custom separator
            var csvParser = new CSVParserBuilder().withSeparator(';').build();

            // Use CSVReaderBuilder to create a CSVReader with the custom parser
            var reader = new CSVReaderBuilder(isr)
                    .withCSVParser(csvParser)
                    .build();

            List<String[]> allRows = reader.readAll();
            // Skip header row if present, then process each row
            for (int i = 1; i < allRows.size(); i++) {
                String[] nextLine = allRows.get(i);
                // Ensure the row has at least 3 columns (for name, day, and week)
                if (nextLine.length < 3) {
                    System.out.println("Skipping a row due to insufficient data");
                    continue; // Skip this row
                }
                String name = nextLine[0];
                String day = nextLine[1];
                String week = nextLine[2];
                schedules.add(new Schedule(name, day, week));
            }
        } catch (IOException | CsvException e) {
            e.printStackTrace();
        }

        return schedules;
    }
}
