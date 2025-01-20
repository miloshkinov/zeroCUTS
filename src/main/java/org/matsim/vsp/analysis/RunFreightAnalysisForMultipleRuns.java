package org.matsim.vsp.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.freight.carriers.analysis.CarriersAnalysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public class RunFreightAnalysisForMultipleRuns {

    static final Logger log = LogManager.getLogger(RunFreightAnalysisForMultipleRuns.class);

    public static void main(String[] args) throws IOException {

        Path folderWithDifferentRuns = Path.of("output/WasteCollectionVulkaneifel/250it_newConstraint/");

        boolean reRunAllAnalysis = false;
        File runFolder = new File(folderWithDifferentRuns.toUri());

        for (File singleRunFolder : Objects.requireNonNull(runFolder.listFiles())) {
            if (singleRunFolder.getName().equals("Analysis")) continue;

            File analysisFolder = new File(singleRunFolder, "CarriersAnalysis");
            if (!analysisFolder.exists() || reRunAllAnalysis) {
                CarriersAnalysis freightAnalysis = new CarriersAnalysis(singleRunFolder.getPath());
                freightAnalysis.runCarrierAnalysis(CarriersAnalysis.CarrierAnalysisType.carriersAndEvents);
            }
        }

    }
}
