package org.matsim.vsp.wasteCollection.Vulkaneifel.analysis;

import com.google.common.base.Joiner;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class RunVulkaneifelWasteAnalysis implements MATSimAppCommand {

    static final Logger log = LogManager.getLogger(RunVulkaneifelWasteAnalysis.class);
    private static final Joiner JOIN = Joiner.on("\t");
    private final Set<BufferedWriter> headersWritten = new HashSet<>();


    @CommandLine.Option(names = "--resultsFolder", description = "Path to the results folder",
            defaultValue = "output/WasteCollectionVulkaneifel/250it_newConstraint/")
    private Path resultsFolder;

    @CommandLine.Option(names = "--outputFolder", description = "Path to the output folder",
            defaultValue = "output/WasteCollectionVulkaneifel/250it_newConstraint/Analysis/")
    private Path outputFolder;

    public static void main(String[] args) {
        System.exit(new CommandLine(new RunVulkaneifelWasteAnalysis()).execute(args));
    }

    @Override
    public Integer call() throws IOException {

        if (!Files.exists(outputFolder)) {
            new File(outputFolder.toString()).mkdirs();
        }

        File runFolder = new File(resultsFolder.toUri());
        int count = 0;

        int numberOfRuns = Objects.requireNonNull(runFolder.listFiles()).length;

        Path pathCarrierOverview = outputFolder.resolve("overview_Carriers.csv");
        Path pathVehiclesOverview = outputFolder.resolve("overview_Vehicles.csv");
        Path pathVehicleLoadsOverview = outputFolder.resolve("overview_Load_perVehicles.csv");
        Path pathVehicleTypesOverview = outputFolder.resolve("overview_perVehicleType.csv");
        if (Files.exists(pathCarrierOverview)) {
            Files.delete(pathCarrierOverview);
        }
        if (Files.exists(pathVehiclesOverview)) {
            Files.delete(pathVehiclesOverview);
        }
        if (Files.exists(pathVehicleLoadsOverview)) {
            Files.delete(pathVehicleLoadsOverview);
        }
        if (Files.exists(pathVehicleTypesOverview)) {
            Files.delete(pathVehicleTypesOverview);
        }
        BufferedWriter writerCarrier = IOUtils.getBufferedWriter(pathCarrierOverview.toString());
        BufferedWriter writerVehicles = IOUtils.getBufferedWriter(pathVehiclesOverview.toString());
        BufferedWriter writerVehicleLoads = IOUtils.getBufferedWriter(pathVehicleLoadsOverview.toString());
        BufferedWriter writerVehicleTypes = IOUtils.getBufferedWriter(pathVehicleTypesOverview.toString());

        for (File singleRunFolder : Objects.requireNonNull(runFolder.listFiles())) {
            count++;
            log.info("Run Analysis for run {} of {} runs.", count, numberOfRuns);

            if (singleRunFolder.isFile() || singleRunFolder.getName().contains("Analysis"))
                continue;
            String runName = singleRunFolder.getName();
            String weekday = runName.split("_")[0];
            String weekRhythm = runName.split("_")[1];
            String vehicleFleet = runName.split("_Iterations")[0].replace(weekday + "_" + weekRhythm + "_", "");
            int iterations = Integer.parseInt(runName.split("_Iterations_")[1]);

            File analysisFolder = new File(singleRunFolder, "Analysis_new");
            if (!analysisFolder.exists()) {
                log.error("Analysis folder does not exist for run {}", runName);
                continue;
            }

            File[] files = analysisFolder.listFiles();
            if (files == null) {
                log.error("No files in analysis folder for run {}", runName);
                continue;
            }

            for (File file : files) {
                if (file.getName().contains("Carrier_stats")) {
                    try {
                        sumAnalysisFile(writerCarrier, weekday, weekRhythm, vehicleFleet, iterations, file);
                    } catch (IOException e) {
                        log.error("Error copying Carrier_stats file for run {}", runName, e);
                    }
                } else if (file.getName().contains("Load_perVehicle")) {
                    try {
                        sumAnalysisFile(writerVehicleLoads, weekday, weekRhythm, vehicleFleet, iterations, file);
                    } catch (IOException e) {
                        log.error("Error copying Load_perVehicle file for run {}", runName, e);
                    }
                } else if (file.getName().equals("TimeDistance_perVehicle.tsv")) {
                    try {
                        sumAnalysisFile(writerVehicles, weekday, weekRhythm, vehicleFleet, iterations, file);
                    } catch (IOException e) {
                        log.error("Error copying TimeDistance_perVehicle file for run {}", runName, e);
                    }
                }
                else if (file.getName().equals("TimeDistance_perVehicleType.tsv")) {
                    try {
                        sumAnalysisFile(writerVehicleTypes, weekday, weekRhythm, vehicleFleet, iterations, file);
                    } catch (IOException e) {
                        log.error("Error copying VehicleType_stats file for run {}", runName, e);
                    }
                }
            }

        }
        writerCarrier.flush();
        writerCarrier.close();

        writerVehicles.flush();
        writerVehicles.close();

        writerVehicleLoads.flush();
        writerVehicleLoads.close();

        writerVehicleTypes.flush();
        writerVehicleTypes.close();
        return 0;
    }

    private void sumAnalysisFile(BufferedWriter writer, String weekday, String weekRhythm, String vehicleFleet,
                                 int iterations, File file) throws IOException {

        CSVParser parse = new CSVParser(Files.newBufferedReader(file.toPath()),
                CSVFormat.Builder.create(CSVFormat.TDF).setHeader().setSkipHeaderRecord(true).build());
        for (CSVRecord record : parse) {
            if (!headersWritten.contains(writer)) {
                ArrayList<String> header = new ArrayList<>();
                header.add("weekday");
                header.add("weekRhythm");
                header.add("vehicleFleet");
                header.add("iterations");
                header.addAll(record.toMap().keySet());
                JOIN.appendTo(writer, header);
                writer.newLine();
                headersWritten.add(writer); // Mark this writer as having the header written
            }
            List<String> recordList = new ArrayList<>(List.of(weekday, weekRhythm, vehicleFleet, String.valueOf(iterations)));
            recordList.addAll(record.stream().toList());
            JOIN.appendTo(writer, recordList);
            writer.newLine();
        }
    }
}