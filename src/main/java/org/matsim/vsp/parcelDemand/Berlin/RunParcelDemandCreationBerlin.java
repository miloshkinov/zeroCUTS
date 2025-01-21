package org.matsim.vsp.parcelDemand.Berlin;

import org.matsim.application.MATSimAppCommand;
import org.matsim.freightDemandGeneration.DemandGenerationSpecification;
import org.matsim.freightDemandGeneration.DemandGenerationSpecificationForParcelDelivery;
import org.matsim.freightDemandGeneration.FreightDemandGeneration;
import picocli.CommandLine;

import java.nio.file.Path;

@CommandLine.Command(name = "generate-scenario-ParcelDemand-Berlin", showDefaultValues = true)
public class RunParcelDemandCreationBerlin implements MATSimAppCommand {
    @CommandLine.Option(names = "--carrierOption", description = "Set the choice of getting/creating carrier. Options: readCarrierFile, createCarriersFromCSV, addCSVDataToExistingCarrierFileData")
    private String selectedCarrierInputOption;

    @CommandLine.Option(names = "--demandOption", description = "Select the option of demand generation. Options: useDemandFromCarrierFile, createDemandFromCSV, createDemandFromCSVAndUsePopulation")
    private String selectedDemandGenerationOption;

    @CommandLine.Option(names = "--populationOption", description = "Select the option of using the population. Options: useHolePopulation, usePopulationInShape, useNoPopulation")
    private String selectedPopulationOption;

    @CommandLine.Option(names = "--populationSamplingOption", description = "Select the option of sampling if using a population. Options: createMoreLocations, increaseDemandOnLocation, noPopulationSampling")
    private String selectedPopulationSamplingOption;

    @CommandLine.Option(names = "--VRPSolutionsOption", description = "Select the option of solving the VRP. Options: runJspritAndMATSim, runJspritAndMATSimWithDistanceConstraint, runJsprit, runJspritWithDistanceConstraint, createNoSolutionAndOnlyWriteCarrierFile")
    private String selectedSolution;

    @CommandLine.Option(names = "--output", description = "Path to output folder", defaultValue = "output/")
    private Path output;

    @CommandLine.Option(names = "--carrierVehicleFileLocation", description = "Path to carrierVehicleFileLocation")
    private Path vehicleFilePath;

    @CommandLine.Option(names = "--inputCarrierCSV", description = "Path to inputCarrierCSV")
    private Path carrierCSVLocation;

    @CommandLine.Option(names = "--inputDemandCSV", description = "Path to inputDemandCSV")
    private Path demandCSVLocation;

    @CommandLine.Option(names = "--shapeFileLocation", description = "Path to shapeFileLocation")
    private Path shapeFilePath;

    @CommandLine.Option(names = "--populationLocation", description = "Path to population")
    private String populationLocation;

    @CommandLine.Option(names = "--network", description = "Path to network.")
    private String network;

    @CommandLine.Option(names = "--networkCRS", description = "Network CRS.")
    private String networkCRS;

    @CommandLine.Option(names = "--shapeCategory", description = "Column name in the shape file for the data connection in the csv files")
    private String shapeCategory;

    @CommandLine.Option(names = "--combineSimilarJobs", defaultValue = "false", description = "Select the option if created jobs of the same carrier with same location and time will be combined. Options: true, false", required = true)
    private String combineSimilarJobs;

    @CommandLine.Option(names = "--defaultJspritIterations", description = "Set the default number of jsprit iterations.", defaultValue = "1")
    private int defaultJspritIterations;

    @CommandLine.Option(names = "--packagesPerPerson", description = "Number of parcels per person", defaultValue = "0.14")
    private Double packagesPerPerson;

    @CommandLine.Option(names = "--packagesPerRecipient", description = "Number of parcels per recipient", defaultValue = "1.6")
    private Double packagesPerRecipient;

    public static void main(String[] args) {
        System.exit(new CommandLine(new RunParcelDemandCreationBerlin()).execute(args));
    }

    @Override
    public Integer call() {

        DemandGenerationSpecification demandGenerationSpecificationForParcelDelivery = new DemandGenerationSpecificationForParcelDelivery(
                packagesPerPerson, packagesPerRecipient, true);

        if (carrierCSVLocation == null) {
            output = Path.of("output/ExampleParcelDelivery");
            selectedCarrierInputOption = "createCarriersFromCSV";
            selectedDemandGenerationOption = "createDemandFromCSVAndUsePopulation";
            selectedPopulationOption = "usePopulationInShape";
            selectedPopulationSamplingOption = "noPopulationSampling";
            selectedSolution = "runJspritAndMATSim";
            vehicleFilePath = Path.of("scenarios/parcelDemand/DHL_vehicleTypes.xml");
            carrierCSVLocation = Path.of("scenarios/parcelDemand/Berlin/DHL_CarrierCSV_Berlin_small.csv");
            demandCSVLocation = Path.of("scenarios/parcelDemand/Berlin/DHL_DemandCSV_Berlin_small.csv");
            shapeFilePath = Path.of("scenarios/parcelDemand/Berlin/PLZ_Gebiete_Berlin/PLZ_Gebiete_Berlin.shp");
            populationLocation = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v6.3/input/berlin-v6.3-1pct.plans.xml.gz";
//				populationLocation = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v6.3/input/berlin-only-v6.3-100pct.plans_NOT-fully-calibrated.xml.gz";
            network = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v6.3/input/berlin-v6.3-network.xml.gz";
            networkCRS = "EPSG:25832";
            shapeCategory = "plz";
            defaultJspritIterations = 3;
        }
        new FreightDemandGeneration(demandGenerationSpecificationForParcelDelivery).execute(
                "--output", output.toString(),
                "--carrierOption", selectedCarrierInputOption,
                "--demandOption", selectedDemandGenerationOption,
                "--populationOption", selectedPopulationOption,
                "--populationSamplingOption", selectedPopulationSamplingOption,
                "--VRPSolutionsOption", selectedSolution,
                "--combineSimilarJobs", combineSimilarJobs,
                "--carrierFileLocation", "",
                "--carrierVehicleFileLocation", vehicleFilePath.toString(),
                "--shapeFileLocation", shapeFilePath.toString(),
                "--shapeCRS", "EPSG:25832",
                "--populationFileLocation", populationLocation,
                "--populationCRS", "EPSG:25832",
                "--network", network,
                "--networkCRS", networkCRS,
                "--networkChangeEvents", "",
                "--shapeCategory", shapeCategory,
                "--inputCarrierCSV", carrierCSVLocation.toString(),
                "--inputDemandCSV", demandCSVLocation.toString(),
                "--populationSample", "0.01",
                "--populationSamplingTo", "0.01",
                "--defaultJspritIterations", String.valueOf(defaultJspritIterations));

        return 0;
    }
}
