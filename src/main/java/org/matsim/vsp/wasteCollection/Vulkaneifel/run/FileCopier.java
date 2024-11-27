package org.matsim.vsp.wasteCollection.Vulkaneifel.run;

import java.io.IOException;
import java.nio.file.*;

public class FileCopier {

    public static void main(String[] args) {
        String sourceFolder = "C:\\Users\\phili\\IdeaProjects\\matsim-BA-Vulkaneifel\\output\\RunAbfall_Output_V4_final_45minbreak_125kwhEVS_oneload\\Mi_G";
        String destinationFolder = "C:\\Users\\phili\\iCloudDrive\\Studium Wirtschaftsingenieurwesen\\MATSim_BA\\Vulkaneifel_Analysis_Data_final"; // Replace with your destination folder path

        int[] iterations = {100, 1000, 10000};
        String[] vehicleTypes = {"diesel_vehicle", "EV_small_battery", "EV_medium_battery"};
        String[] folders = {"Carrier_stats", "Load_perVehicle", "TimeDistance_perVehicle"};

        for (int iteration : iterations) {
            for (String vehicleType : vehicleTypes) {
                for (String folderName : folders) {
                    String sourceFolder2 = sourceFolder + "\\Iterations_" + iteration + "_" + vehicleType + "\\Analysis_new";

                    Path destinationPath = Paths.get(destinationFolder, folderName);
                    createDirectoriesIfNotExist(destinationPath);

                    copyAndRenameFile(sourceFolder2, destinationPath.toString(), folderName + ".tsv", folderName + "_" + iteration + "_" + vehicleType + "_V4.tsv");
                }
            }
        }
    }

    private static void copyAndRenameFile(String sourceFolder, String destinationFolder, String sourceFileName, String destinationFileName) {
        Path sourcePath = Paths.get(sourceFolder, sourceFileName);
        Path destinationPath = Paths.get(destinationFolder, destinationFileName);

        try {
            Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File " + sourceFileName + " copied to " + destinationPath);
        } catch (IOException e) {
            System.err.println("Error copying file " + sourceFileName + ": " + e.getMessage());
        }
    }

    private static void createDirectoriesIfNotExist(Path path) {
        try {
            if (Files.notExists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            System.err.println("Error creating directories: " + e.getMessage());
        }
    }
}
