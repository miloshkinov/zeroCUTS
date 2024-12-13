package org.matsim.vsp.analysis;

import org.matsim.freight.carriers.analysis.RunFreightAnalysisEventBased;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public class RunFreightAnalysisForMultipleRuns {

    public static void main(String[] args) throws IOException {

        Path folderWithDifferentRuns = Path.of("output/WasteCollectionVulkaneifel/TestNewConstraint/");
        String globalCrs = "EPSG:25832";

        File runFolder = new File(folderWithDifferentRuns.toUri());

        for (File singleRunFolder : Objects.requireNonNull(runFolder.listFiles())) {
            File analysisFolder = new File(singleRunFolder, "Analysis_new");
            if (!analysisFolder.exists()) {
                RunFreightAnalysisEventBased freightAnalysis = new RunFreightAnalysisEventBased(singleRunFolder.getPath() + "/",
                        singleRunFolder.getPath() + "/Analysis_new/",
                        globalCrs);
                freightAnalysis.runCompleteAnalysis();
            }
        }

    }
}
