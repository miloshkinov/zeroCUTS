package org.matsim.vsp.SmallScaleCommercialTraffic;

import org.matsim.smallScaleCommercialTrafficGeneration.CreateSmallScaleCommercialTrafficDemand;

import java.io.IOException;

public class RunCreateSmallScaleCommercialTraffic {
    private enum Model {
        berlin, berlin_sample, matsim_berlin, leipzig, vulkaneifel, rvr, test
    }
    private enum CreationOption {
        useExistingCarrierFileWithSolution, createNewCarrierFile, useExistingCarrierFileWithoutSolution
    }

    private enum LanduseConfiguration {
        useOnlyOSMLanduse, useOSMBuildingsAndLanduse, useExistingDataDistribution
    }

    private enum TrafficType {
        businessTraffic, freightTraffic, commercialTraffic
    }
    public static void main(String[] args) throws IOException {

        Model selectedModel = Model.test;
        String selectedCreationOption = String.valueOf(CreationOption.createNewCarrierFile);
        String selectedLanduseConfiguration = String.valueOf(LanduseConfiguration.useOSMBuildingsAndLanduse);
        String selectedTrafficType = String.valueOf(TrafficType.freightTraffic);

        String sampleSize = "0.001";
        String jspritIterations = "1";
        boolean includeExistingModels = false;
        String resistanceFactor = "0.005";

        String inputDataDirectory;
        String zoneShapeFileName;
        String buildingsShapeFileName = null;
        String landuseShapeFileName;
        String shapeCRS;

        String nameOutputPopulation = "testPopulation.xml.gz";
        String pathOutput = "output/testOutput/";
        switch (selectedModel){

            case berlin, berlin_sample -> {
                inputDataDirectory ="../public-svn/matsim/scenarios/countries/de/berlin/projects/zerocuts/small-scale-commercial-traffic/input/berlin/";
                zoneShapeFileName = inputDataDirectory + "/shp/berlinBrandenburg_Zones_VKZ_4326.shp";
                switch (selectedModel){
                    case berlin -> buildingsShapeFileName = inputDataDirectory + "/shp/buildings_BerlinBrandenburg_4326";
                    case berlin_sample -> buildingsShapeFileName = inputDataDirectory + "/shp/buildings_sample_BerlinBrandenburg_4326.shp";
                }
                landuseShapeFileName = inputDataDirectory + "/shp/berlinBrandenburg_landuse_4326.shp";
                shapeCRS = "EPSG:4326";
            }
            case matsim_berlin -> {
                inputDataDirectory ="../public-svn/matsim/scenarios/countries/de/berlin/projects/zerocuts/small-scale-commercial-traffic/input/berlin/";
                zoneShapeFileName = inputDataDirectory + "/shp/berlinBrandenburg_Zones_VKZ_4326.shp";
                switch (selectedModel){
                    case berlin -> buildingsShapeFileName = inputDataDirectory + "/shp/buildings_BerlinBrandenburg_4326";
                    case berlin_sample -> buildingsShapeFileName = inputDataDirectory + "/shp/buildings_sample_BerlinBrandenburg_4326.shp";
                }
                landuseShapeFileName = inputDataDirectory + "/shp/berlinBrandenburg_landuse_4326.shp";
                shapeCRS = "EPSG:4326";
            }
            case leipzig -> {
                inputDataDirectory = "../public-svn/matsim/scenarios/countries/de/berlin/projects/zerocuts/small-scale-commercial-traffic/input/leipzig/";
                zoneShapeFileName = inputDataDirectory + "/shp/leipzig_zones_25832.shp";
                buildingsShapeFileName = inputDataDirectory + "/shp/leipzig_buildings_25832.shp";
                landuseShapeFileName = inputDataDirectory + "/shp/leipzig_landuse_25832.shp";
                shapeCRS = "EPSG:25832";
            }
            case vulkaneifel -> {
                inputDataDirectory ="../public-svn/matsim/scenarios/countries/de/berlin/projects/zerocuts/small-scale-commercial-traffic/input/vulkaneifel/";
                zoneShapeFileName = inputDataDirectory + "/shp/zones_vulkaneifel_commercialTraffic_25832.shp";
                buildingsShapeFileName = inputDataDirectory + "/shp/buildings_vulkaneifel_25832.shp";
                landuseShapeFileName = inputDataDirectory + "/shp/landuse_vulkaneifel_25832.shp";
                shapeCRS = "EPSG:25832";
            }
            case rvr -> {
                inputDataDirectory ="../public-svn/matsim/scenarios/countries/de/berlin/projects/zerocuts/small-scale-commercial-traffic/input/rvr/";
                zoneShapeFileName = "";
                buildingsShapeFileName = "";
                landuseShapeFileName = "";
                shapeCRS = "";
            }
            case test -> {
                sampleSize = "0.5";
                inputDataDirectory = "test/input/org/matsim/vsp/SmallScaleCommercialTraffic/";
                zoneShapeFileName = inputDataDirectory + "/shp/testZones.shp";
                buildingsShapeFileName = inputDataDirectory + "/shp/testBuildings.shp";
                landuseShapeFileName = inputDataDirectory + "/shp/testLanduse.shp";
                shapeCRS = "EPSG:4326";
            }
            default -> throw new IllegalStateException("Unexpected value: " + selectedModel);
        }

        if (includeExistingModels) {
            new CreateSmallScaleCommercialTrafficDemand().execute(
                    inputDataDirectory,
                    "--sample", sampleSize,
                    "--jspritIterations", jspritIterations,
                    "--creationOption", selectedCreationOption,
                    "--landuseConfiguration", selectedLanduseConfiguration,
                    "--trafficType", selectedTrafficType,
                    "--includeExistingModels",
                    "--zoneShapeFileName", zoneShapeFileName,
                    "--buildingsShapeFileName", buildingsShapeFileName,
                    "--landuseShapeFileName", landuseShapeFileName,
                    "--shapeCRS", shapeCRS,
                    "--resistanceFactor", resistanceFactor
            );
        }
        else {
            new CreateSmallScaleCommercialTrafficDemand().execute(
                    inputDataDirectory,
                    "--sample", sampleSize,
                    "--jspritIterations", jspritIterations,
                    "--creationOption", selectedCreationOption,
                    "--landuseConfiguration", selectedLanduseConfiguration,
                    "--trafficType", selectedTrafficType,
                    "--zoneShapeFileName", zoneShapeFileName,
                    "--buildingsShapeFileName", buildingsShapeFileName,
                    "--landuseShapeFileName", landuseShapeFileName,
                    "--shapeCRS", shapeCRS,
                    "--resistanceFactor", resistanceFactor
            );
        }
//        List<File> fileData = new ArrayList<>();
//        for (File file : Objects.requireNonNull(output.toFile().listFiles())) {
//            fileData.add(file);
//        }
//        Collections.sort(fileData);
//        File lastFile = fileData.get(fileData.size()-1);
//        String[] argsAnalysis = { lastFile.toString(), "true"};
//        FreightAnalyse.main(argsAnalysis);
    }
}
