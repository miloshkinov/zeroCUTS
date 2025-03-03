package org.matsim.vsp.parcelDemand;

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

public class RunSummarizeSimulationResults implements MATSimAppCommand {

    static final Logger log = LogManager.getLogger(RunSummarizeSimulationResults.class);
    private static final Joiner JOIN = Joiner.on("\t");
    private final Set<BufferedWriter> headersWritten = new HashSet<>();


    @CommandLine.Option(names = "--resultsFolder", description = "Path to the results folder",
            defaultValue = "output/parcelDemand/Berlin/separatedAreas/")
    private Path resultsFolder;

    @CommandLine.Option(names = "--outputFolder", description = "Path to the output folder",
            defaultValue = "output/parcelDemand/Berlin/separatedAreas/Analysis/")
    private Path outputFolder;

    public static void main(String[] args) {
        System.exit(new CommandLine(new RunSummarizeSimulationResults()).execute(args));
    }

    @Override
    public Integer call() throws IOException {

        if (!Files.exists(outputFolder)) {
            new File(outputFolder.toString()).mkdirs();
        }

        File runFolder = new File(resultsFolder.toUri());
        int count = 0;

        int numberOfRuns = Objects.requireNonNull(runFolder.listFiles()).length;

        Path pathCarrierPlanOverview = outputFolder.resolve("overview_CarriersPlans.csv");
        Path pathCarrierPlanUnplannedOverview = outputFolder.resolve("overview_CarriersPlans_unplanned.csv");
        Path pathVehiclesOverview = outputFolder.resolve("overview_Vehicles.csv");
        Path pathVehicleLoadsOverview = outputFolder.resolve("overview_Load_perVehicles.csv");
        Path pathVehicleTypesOverview = outputFolder.resolve("overview_perVehicleType.csv");
        Path pathOverviewVehiclesPerCarrier = outputFolder.resolve("overview_perCarrier.csv");

        Files.deleteIfExists(pathCarrierPlanOverview);
        Files.deleteIfExists(pathCarrierPlanUnplannedOverview);
        Files.deleteIfExists(pathVehiclesOverview);
        Files.deleteIfExists(pathVehicleLoadsOverview);
        Files.deleteIfExists(pathVehicleTypesOverview);
        Files.deleteIfExists(pathOverviewVehiclesPerCarrier);

        BufferedWriter writerCarrierPlans = IOUtils.getBufferedWriter(pathCarrierPlanOverview.toString());
        BufferedWriter writerCarrierPlans_unplanned = IOUtils.getBufferedWriter(pathCarrierPlanUnplannedOverview.toString());
        BufferedWriter writerVehicles = IOUtils.getBufferedWriter(pathVehiclesOverview.toString());
        BufferedWriter writerVehicleLoads = IOUtils.getBufferedWriter(pathVehicleLoadsOverview.toString());
        BufferedWriter writerVehicleTypes = IOUtils.getBufferedWriter(pathVehicleTypesOverview.toString());
        BufferedWriter writerVehiclesCarrier = IOUtils.getBufferedWriter(pathOverviewVehiclesPerCarrier.toString());

        for (File singleRunFolder : Objects.requireNonNull(runFolder.listFiles())) {
            count++;
            log.info("Run Analysis for run {} of {} runs.", count, numberOfRuns);

            if (singleRunFolder.isFile() || singleRunFolder.getAbsolutePath().equals(outputFolder.toAbsolutePath().toString()))
                continue;
            String deliveryArea = singleRunFolder.getName();
//            String weekday = deliveryArea.split("_")[0];
//            String weekRhythm = deliveryArea.split("_")[1];
//            String vehicleFleet = deliveryArea.split("_Iterations")[0].replace(weekday + "_" + weekRhythm + "_", "");
//            int iterations = Integer.parseInt(deliveryArea.split("_Iterations_")[1]);
            File latestRun = Arrays.stream(Objects.requireNonNull(singleRunFolder.listFiles()))
                    .filter(File::isDirectory) // Only include directories
                    .max((folder1, folder2) -> Long.compare(folder1.lastModified(), folder2.lastModified()))
                    .orElse(null); // Return null if no directories are found
            if (latestRun == null) {
                continue;
            }
            String runName = latestRun.getName();
            File analysisFolder = new File(latestRun, "CarriersAnalysis");
            if (!analysisFolder.exists()) {
                log.error("Analysis folder does not exist for run {}", deliveryArea);
                continue;
            }

            File[] files = analysisFolder.listFiles();
            if (files == null) {
                log.error("No files in analysis folder for run {}", deliveryArea);
                continue;
            }

            for (File file : files) {
                if (file.getName().contains("Carriers_stats.tsv")) {
                    try {
                        sumAnalysisFile(writerCarrierPlans, file, deliveryArea, runName);
                    } catch (IOException e) {
                        log.error("Error copying Carrier_stats file for run {}", deliveryArea, e);
                    }
                } else if (file.getName().contains("Carriers_stats_unPlanned.tsv")) {
                    try {
                        sumAnalysisFile(writerCarrierPlans_unplanned, file, deliveryArea, runName);
                    } catch (IOException e) {
                        log.error("Error copying Carrier_stats_unplanned file for run {}", deliveryArea, e);
                    }
                } else if (file.getName().contains("Load_perVehicle")) {
                    try {
                        sumAnalysisFile(writerVehicleLoads, file, deliveryArea, runName);
                    } catch (IOException e) {
                        log.error("Error copying Load_perVehicle file for run {}", deliveryArea, e);
                    }
                } else if (file.getName().equals("TimeDistance_perVehicle.tsv")) {
                    try {
                        sumAnalysisFile(writerVehicles, file, deliveryArea, runName);
                    } catch (IOException e) {
                        log.error("Error copying TimeDistance_perVehicle file for run {}", deliveryArea, e);
                    }
                } else if (file.getName().equals("TimeDistance_perVehicleType.tsv")) {
                    try {
                        sumAnalysisFile(writerVehicleTypes, file, deliveryArea, runName);
                    } catch (IOException e) {
                        log.error("Error copying VehicleType_stats file for run {}", deliveryArea, e);
                    }
                }else if (file.getName().equals("TimeDistance_perCarrier.tsv")) {
                    try {
                        sumAnalysisFile(writerVehiclesCarrier, file, deliveryArea, runName);
                    } catch (IOException e) {
                        log.error("Error copying TimeDistance_perCarrier file for run {}", deliveryArea, e);
                    }
                }
                else
                    log.warn("Unknown file name {}", file.getName());

            }

        }
        writerCarrierPlans_unplanned.flush();
        writerCarrierPlans_unplanned.close();

        writerCarrierPlans.flush();
        writerCarrierPlans.close();

        writerVehicles.flush();
        writerVehicles.close();

        writerVehicleLoads.flush();
        writerVehicleLoads.close();

        writerVehicleTypes.flush();
        writerVehicleTypes.close();

        writerVehiclesCarrier.flush();
        writerVehiclesCarrier.close();
        return 0;
    }

    private void sumAnalysisFile(BufferedWriter writer, File file, String deliveryArea, String runName) throws IOException {

        CSVParser parse = new CSVParser(Files.newBufferedReader(file.toPath()),
                CSVFormat.Builder.create(CSVFormat.TDF).setHeader().setSkipHeaderRecord(true).build());
        for (CSVRecord record : parse) {
            if (!headersWritten.contains(writer)) {
                ArrayList<String> header = new ArrayList<>();
                header.add("deliveryArea");
                header.add("runName");
//                header.add("vehicleFleet");
//                header.add("iterations");
                header.addAll(record.toMap().keySet());
                JOIN.appendTo(writer, header);
                writer.newLine();
                headersWritten.add(writer); // Mark this writer as having the header written
            }
            List<String> recordList = new ArrayList<>(List.of(deliveryArea, runName));
            recordList.addAll(record.stream().toList());
            JOIN.appendTo(writer, recordList);
            writer.newLine();
        }
    }
}