package org.matsim.vsp.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.application.ApplicationUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.freight.carriers.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Class to analyze the possible fleet of a carrier and checks if the vehicles are used for tours.
 */
public class CarrierFleetAnalysis {
    private static final String delimiter = "\t";

    static void main() throws IOException {

//        String runFolder = "../matsim-hannover/output/smallScaleCommercialPlans/"; // Specify the path to your run folder
        String runFolder = "../matsim-hannover/output/smallScaleCommercialPlans_newApproach2_0.2/"; // Specify the path to your run folder
//        String runFolder = "../matsim-hannover/output/smallScaleCommercialPlans_old_approach/"; // Specify the path to your run folder

        String carriersFile = String.valueOf(ApplicationUtils.globFile(Path.of(runFolder),"*output_carriers_withPlans.xml.gz")); // Specify the path to your carriers file
        String vehicleTypesFile = String.valueOf(ApplicationUtils.globFile(Path.of(runFolder),"*.output_vehicles.xml.gz")); // Specify the path to your vehicle types file

        Config config = ConfigUtils.createConfig();
        FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
        freightCarriersConfigGroup.setCarriersFile(carriersFile);
        freightCarriersConfigGroup.setCarriersVehicleTypesFile(vehicleTypesFile);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);

        writeAnalysisResults(scenario, carriersFile);
    }

    private static void writeAnalysisResults(Scenario scenario, String carriersFile) throws IOException {

        Carriers carriers = CarriersUtils.getCarriers(scenario);

        try (BufferedWriter writer = IOUtils.getBufferedWriter(Path.of(carriersFile).getParent().resolve("analysis").resolve("freight").resolve("carrierFleetAnalysis.csv").toString())) {
            writer.write("vehicle" + delimiter);
            writer.write("carrier" + delimiter);
            writer.write("vehicleType" + delimiter);
            writer.write("linkId" + delimiter);
            writer.write("maxTourDuration" + delimiter);
            writer.write("usedForTour" + delimiter);
            writer.write("usedDuration");

            writer.newLine();
            for (Carrier carrier : carriers.getCarriers().values()) {
                carrier.getCarrierCapabilities().getCarrierVehicles().values().forEach(vehicle -> {
                    try {
                        writer.write(vehicle.getId().toString() + delimiter);
                        writer.write(carrier.getId() + delimiter);
                        writer.write(vehicle.getType().getId().toString() + delimiter);
                        writer.write(vehicle.getLinkId().toString() + delimiter);
                        writer.write((vehicle.getLatestEndTime() - vehicle.getEarliestStartTime()) + delimiter);
                        boolean isUsedForTour = false;
                        double tourDuration = 0.0;
                        for (ScheduledTour tour : carrier.getSelectedPlan().getScheduledTours())
                            if (tour.getVehicle().getId().equals(vehicle.getId())) {
                                double tourStartTime = tour.getDeparture();
                                double tourEndTime = 0.0;
                                if (tour.getTour().getTourElements().isEmpty()) {
                                    continue;
                                }
                                Tour.TourElement tourEnd = tour.getTour().getTourElements().getLast();
                                if (tourEnd instanceof Tour.Leg) {
                                    tourEndTime = ((Tour.Leg) tourEnd).getExpectedDepartureTime() + ((Tour.Leg) tourEnd).getExpectedTransportTime();
                                }
                                tourDuration = (tourEndTime - tourStartTime);
                                isUsedForTour = true;
                                break;
                            }
                        writer.write(isUsedForTour + delimiter);
                        writer.write(Double.toString(tourDuration));
                        writer.newLine();
                    } catch (IOException e) {
                        throw new RuntimeException("Error writing to file: " + e.getMessage(), e);
                    }
                });
            }
        }
    }
}
