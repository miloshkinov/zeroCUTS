package org.matsim.vsp.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.freight.carriers.analysis.RunFreightAnalysisEventBased;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class RunFreightAnalysisForMultipleRuns {

    static final Logger log = LogManager.getLogger(RunFreightAnalysisForMultipleRuns.class);

    public static void main(String[] args) throws IOException {

        Path folderWithDifferentRuns = Path.of("output/WasteCollectionVulkaneifel/250it_newConstraint/");
        String globalCrs = "EPSG:25832";

        boolean reRunAllAnalysis = false;
        File runFolder = new File(folderWithDifferentRuns.toUri());

        for (File singleRunFolder : Objects.requireNonNull(runFolder.listFiles())) {
            if (singleRunFolder.getName().equals("Analysis")) continue;

            File analysisFolder = new File(singleRunFolder, "Analysis_new");
            if (!analysisFolder.exists() || reRunAllAnalysis) {
                RunFreightAnalysisEventBased freightAnalysis = new RunFreightAnalysisEventBased(singleRunFolder.getPath() + "/",
                        singleRunFolder.getPath() + "/Analysis_new/",
                        globalCrs);
                freightAnalysis.runCompleteAnalysis();
            }
        }

    }
}
