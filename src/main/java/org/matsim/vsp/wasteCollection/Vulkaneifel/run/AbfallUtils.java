package org.matsim.vsp.wasteCollection.Vulkaneifel.run;
import com.opencsv.exceptions.CsvException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
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

    static String carrierVehicleTypesFilePath = "scenarios/wasteCollection/Vulkaneifel/vehicles/vehicleTypes_new.xml";

    static Config prepareConfig(String output, String networkPath) {
        Config config = ConfigUtils.createConfig();
        config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
        config.global().setCoordinateSystem("EPSG:25832");
        config.controller().setOutputDirectory(output);
        config.controller().setLastIteration(0); //only one MATSim iteration
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

        FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
        log.info("Read carrier vehicle types from: {}", carrierVehicleTypesFilePath);
        freightCarriersConfigGroup.setCarriersVehicleTypesFile(carrierVehicleTypesFilePath);

        log.info("Read network from: {}", networkPath);
        config.network().setInputFile(networkPath);

        new OutputDirectoryHierarchy(config.controller().getOutputDirectory(), config.controller().getRunId(),
                config.controller().getOverwriteFileSetting(), ControllerConfigGroup.CompressionType.gzip);
        new File(Path.of(config.controller().getOutputDirectory()).toString()).mkdir();
        return config;
    }

    static void createCarriers(Carriers carriers, int jspritIterations) {
        Carrier carrier = CarriersUtils.createCarrier(Id.create("eifelWasteCollection", Carrier.class));

        log.info("Set jsprit iterations to: {} for carrier: {}", jspritIterations, carrier.getId().toString());
        CarriersUtils.setJspritIterations(carrier, jspritIterations);
        carriers.addCarrier(carrier);
    }

    static void createFreightVehiclesSingleType(Scenario scenario, RunWasteCollectionVulkaneifel.VehicleFleet vehicleFleet) {
        CarrierCapabilities carrierCapabilities = CarrierCapabilities.Builder.newInstance().setFleetSize(
                CarrierCapabilities.FleetSize.INFINITE).build();
        String[] possibleVehicleTypes;
        switch (vehicleFleet) {
            case diesel_vehicle -> possibleVehicleTypes = new String[]{"diesel_vehicle"};
            case EV_small_battery -> possibleVehicleTypes = new String[]{"EV_small_battery"};
            case EV_medium_battery -> possibleVehicleTypes = new String[]{"EV_medium_battery"};
            case EVMix -> possibleVehicleTypes = new String[]{"EV_small_battery", "EV_medium_battery"};
            case MixAll -> possibleVehicleTypes = new String[]{"EV_small_battery", "EV_medium_battery", "diesel_vehicle"};
            default -> throw new IllegalStateException("Unexpected value: " + vehicleFleet);
        }
        for (String possibleVehicleType : possibleVehicleTypes){
            VehicleType thisType = CarriersUtils.getCarrierVehicleTypes(scenario).getVehicleTypes().get(Id.create(possibleVehicleType, VehicleType.class));
            List<String> vehicleDepots = List.of(linkDepotPruem); // List of starting Depots of the vehicles
            for (Carrier thisCarrier : CarriersUtils.addOrGetCarriers(scenario).getCarriers().values()) {
                for (String singleDepot : vehicleDepots) {
                    int vehicleStartTime = 6 * 3600; //in Sekunden
                    int vehicleEndTime = 15 * 3600;  //in Sekunden


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

    }


    // V4 for scenario with both EVs
    static void  createJobs(Scenario scenario, Network subnetworkCar, ShpOptions.Index shpIndex, String gemeindeName, Population population, int volumePer4Persons, int counter) {

        Carrier carrier = CarriersUtils.addOrGetCarriers(scenario).getCarriers().values().iterator().next(); //because we only have one carrier
        Set<Id<Person>> processedPersons = new HashSet<>();
        String destinationLink = "370587200013f"; //Mülldeponie EVZ Walsdorf
        //int volume = 200; // Müllmenge pro Haushalt in L


        double pickupTimeBetweenShipments = 15; // Zwischenfahrzeit, wenn Shipment in der selben Straße/Link liegt
        double pickupServiceTime = 19; // entnommen aus Studie von Umweltbundesamt
        TimeWindow pickupTimeWindow = TimeWindow.newInstance(6 * 3600, 15 * 3600);
        //double deliveryServiceTime = 2; //delivery time per shipment at the dump
        TimeWindow deliveryTimeWindow = TimeWindow.newInstance(6 * 3600, 15 * 3600);

        //int counter = 0;
        int payload = 11000; //average payload
        int shipment_amount = payload / volumePer4Persons;
        double deliveryServiceTime = (60*45) / (double) shipment_amount; // changed to 45min breaks, 17.0.... AVG of both times for EV medium and EV small vehicles 16.29, 17.91, => 17.1; different deliverytimes result inconsistent results,
        //double deliveryServiceTime = 17.1 ;

        for (Person person : population.getPersons().values()) {
            if (processedPersons.contains(person.getId())) {
                continue; // Skip if the person has already been processed
            }
            for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
                if (planElement instanceof Activity activity) {
                    if (activity.getType().contains("home")) {
                        Coord coord = activity.getCoord();

                        if (Objects.equals(shpIndex.query(coord), gemeindeName)) {

//                            log.info("PersonID: " + person.getId().toString() + " counter: " + counter + " LinkID: " + activity.getLinkId().toString());
                            Id<Link> demandLinkId = NetworkUtils.getNearestLinkExactly(subnetworkCar, coord).getId(); //Ort der Sammlung des Mülls

                            if (carrier.getShipments().containsKey(Id.create("wasteCollection_" + demandLinkId.toString(), CarrierShipment.class))) {

                                CarrierShipment oldShipment = carrier.getShipments().get(Id.create("wasteCollection_" + demandLinkId.toString(), CarrierShipment.class));
                                double oldPickupServiceTime = oldShipment.getPickupServiceTime();
                                double oldDeliveryServiceTime = oldShipment.getDeliveryServiceTime();
                                int oldVolume = oldShipment.getSize();

                                double newPickupServiceTime = oldPickupServiceTime + pickupServiceTime + pickupTimeBetweenShipments;
                                double newDeliveryServiceTime = oldDeliveryServiceTime + deliveryServiceTime;
                                int newVolume = oldVolume + volumePer4Persons;
                                CarrierShipment thisShipment = CarrierShipment.Builder.newInstance(Id.create("wasteCollection_" + demandLinkId.toString(), CarrierShipment.class), demandLinkId, Id.createLinkId(destinationLink),
                                        newVolume).setPickupServiceTime(newPickupServiceTime).setPickupTimeWindow(pickupTimeWindow).setDeliveryServiceTime(newDeliveryServiceTime).setDeliveryTimeWindow(deliveryTimeWindow).build();
                                CarriersUtils.addShipment(carrier, thisShipment);

                                log.info("created----PersonID: {} counter: {} LinkID: {}_multiple", person.getId().toString(), counter,
                                        demandLinkId.toString());

                            } else {
                                log.info("created----PersonID: {} counter: {} LinkID: {}", person.getId().toString(), counter,
                                        demandLinkId.toString());
                                CarrierShipment thisShipment = CarrierShipment.Builder.newInstance(Id.create("wasteCollection_" + demandLinkId.toString(), CarrierShipment.class), demandLinkId, Id.createLinkId(destinationLink),
                                        volumePer4Persons).setPickupServiceTime(pickupServiceTime).setPickupTimeWindow(pickupTimeWindow).setDeliveryServiceTime(deliveryServiceTime).setDeliveryTimeWindow(deliveryTimeWindow).build();
                                CarriersUtils.addShipment(carrier, thisShipment);
                            }
                            processedPersons.add(person.getId());
                            counter++;
                            break; // Exit the inner loop after processing the first home activity to ensure we do not create multiple shipments for the same person
                        }
                    }
                }
            }
        }

    }

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
