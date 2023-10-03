package org.matsim.vsp.SmallScaleCommercialTraffic;

import org.matsim.smallScaleCommercialTrafficGeneration.GenerateSmallScaleCommercialTrafficDemand;

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
        commercialPersonTraffic, goodsTraffic, completeSmallScaleCommercialTraffic
    }
    public static void main(String[] args) throws IOException {

        Model selectedModel = Model.test;
        String selectedCreationOption = String.valueOf(CreationOption.createNewCarrierFile);
        String selectedLanduseConfiguration = String.valueOf(LanduseConfiguration.useOSMBuildingsAndLanduse);
        String selectedTrafficType = String.valueOf(TrafficType.completeSmallScaleCommercialTraffic);

        String sampleSize = "0.001";
        String jspritIterations = "1";
        boolean includeExistingModels = false;
        String resistanceFactor = "0.005";

        String configPath;
        String zoneShapeFileName;
        String buildingsShapeFileName = null;
        String landuseShapeFileName;
        String shapeCRS;
        String numberOfPlanVariantsPerAgent = "3";

        String nameOutputPopulation = "testPopulation.xml.gz";
        String pathOutput = "output/testOutput/";
        switch (selectedModel){

            case berlin, berlin_sample -> {
                configPath ="../public-svn/matsim/scenarios/countries/de/berlin/projects/zerocuts/small-scale-commercial-traffic/input/berlin/";
                zoneShapeFileName = "/shp/berlinBrandenburg_Zones_VKZ_4326.shp";
                switch (selectedModel){
                    case berlin -> buildingsShapeFileName = "/shp/buildings_BerlinBrandenburg_4326.shp";
                    case berlin_sample -> buildingsShapeFileName = "/shp/buildings_sample_BerlinBrandenburg_4326.shp";
                }
                landuseShapeFileName = "/shp/berlinBrandenburg_landuse_4326.shp";
                shapeCRS = "EPSG:4326";
                pathOutput = "";
            }
            case matsim_berlin -> {
                configPath ="../public-svn/matsim/scenarios/countries/de/berlin/projects/zerocuts/small-scale-commercial-traffic/input/berlin/";
                zoneShapeFileName = "/shp/berlinBrandenburg_Zones_VKZ_4326.shp";
                switch (selectedModel){
                    case berlin -> buildingsShapeFileName = "/shp/buildings_BerlinBrandenburg_4326.shp";
                    case berlin_sample -> buildingsShapeFileName = "/shp/buildings_sample_BerlinBrandenburg_4326.shp";
                }
                landuseShapeFileName = "/shp/berlinBrandenburg_landuse_4326.shp";
                shapeCRS = "EPSG:4326";
            }
            case leipzig -> {
                configPath = "../public-svn/matsim/scenarios/countries/de/berlin/projects/zerocuts/small-scale-commercial-traffic/input/leipzig/";
                zoneShapeFileName = "/shp/leipzig_zones_25832.shp";
                buildingsShapeFileName = "/shp/leipzig_buildings_25832.shp";
                landuseShapeFileName = "/shp/leipzig_landuse_25832.shp";
                shapeCRS = "EPSG:25832";
            }
            case vulkaneifel -> {
                configPath ="../matsim-vulkaneifel/input/commercialTraffic/config_demand.xml";
                zoneShapeFileName = "../public-svn/matsim/scenarios/countries/de/vulkaneifel/v1.2/input/shp/zones_vulkaneifel_commercialTraffic_25832.shp";
                buildingsShapeFileName = "../public-svn/matsim/scenarios/countries/de/vulkaneifel/v1.2/input/shp/buildings_vulkaneifel_25832.shp";
                landuseShapeFileName = "../public-svn/matsim/scenarios/countries/de/vulkaneifel/v1.2/input/shp/landuse_vulkaneifel_25832.shp";
                shapeCRS = "EPSG:25832";
                pathOutput = "";
            }
            case rvr -> {
                configPath ="../public-svn/matsim/scenarios/countries/de/berlin/projects/zerocuts/small-scale-commercial-traffic/input/rvr/";
                zoneShapeFileName = "";
                buildingsShapeFileName = "";
                landuseShapeFileName = "";
                shapeCRS = "";
            }
            case test -> {
                sampleSize = "0.25";
                configPath = "test/input/org/matsim/vsp/SmallScaleCommercialTraffic/config_demand.xml";
                zoneShapeFileName = "test/input/org/matsim/vsp/SmallScaleCommercialTraffic/shp/testZones.shp";
                buildingsShapeFileName = "test/input/org/matsim/vsp/SmallScaleCommercialTraffic/shp/testBuildings.shp";
                landuseShapeFileName = "test/input/org/matsim/vsp/SmallScaleCommercialTraffic/shp/testLanduse.shp";
                shapeCRS = "EPSG:4326";
                pathOutput = "output/testOutput/";
            }
            default -> throw new IllegalStateException("Unexpected value: " + selectedModel);
        }

        if (includeExistingModels) {
            new GenerateSmallScaleCommercialTrafficDemand().execute(
                    configPath,                    "--sample", sampleSize,
                    "--jspritIterations", jspritIterations,
                    "--creationOption", selectedCreationOption,
                    "--landuseConfiguration", selectedLanduseConfiguration,
                    "--smallScaleCommercialTrafficType", selectedTrafficType,
                    "--includeExistingModels",
                    "--zoneShapeFileName", zoneShapeFileName,
                    "--buildingsShapeFileName", buildingsShapeFileName,
                    "--landuseShapeFileName", landuseShapeFileName,
                    "--shapeCRS", shapeCRS,
                    "--resistanceFactor", resistanceFactor
            );
        }
        else {
            new GenerateSmallScaleCommercialTrafficDemand().execute(
                    configPath,
                    "--sample", sampleSize,
                    "--jspritIterations", jspritIterations,
                    "--creationOption", selectedCreationOption,
                    "--landuseConfiguration", selectedLanduseConfiguration,
                    "--smallScaleCommercialTrafficType", selectedTrafficType,
                    "--zoneShapeFileName", zoneShapeFileName,
                    "--buildingsShapeFileName", buildingsShapeFileName,
                    "--landuseShapeFileName", landuseShapeFileName,
                    "--shapeCRS", shapeCRS,
                    "--resistanceFactor", resistanceFactor,
                    "--pathOutput", pathOutput,
                    "--numberOfPlanVariantsPerAgent", numberOfPlanVariantsPerAgent
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
