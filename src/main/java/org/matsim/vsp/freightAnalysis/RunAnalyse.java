package org.matsim.vsp.freightAnalysis;

import java.io.IOException;

import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.freight.carriers.analysis.analysis.RunFreightAnalysisEventBased;

public class RunAnalyse {

	public static void main(String[] args) throws UncheckedIOException, IOException {
		
		String runDir = "C:/Users/Ricardo/git/zeroCUTS/output/BusinessPassengerTraffic/vulkaneifel/commercialPersonTraffic_2pct_2023-08-30_32900_0.005/";
//		String runDir = "C:/Users/Ricardo/Desktop/SoundingBoard/businessTraffic_5pct_2023-03-07_48622_0.005/";
//		String runId = "tax40CV.";
		String networkCRS = TransformationFactory.DHDN_GK4;
		String runId = "base.";
		enum FreightAnalysisVersion {
			eventBased, oldVersion
		}
		FreightAnalysisVersion slectedFreightAnalysisVersion = FreightAnalysisVersion.eventBased;
		switch (slectedFreightAnalysisVersion) {
			case eventBased -> {
				RunFreightAnalysisEventBased freightAnalysis = new RunFreightAnalysisEventBased(runDir + "/",
						runDir + "/EventBasedAnalysis/", "EPSG:25832");
				freightAnalysis.runAnalysis();
			}
			case oldVersion -> {
				if (runId != null)
					FreightAnalyse.main(new String[] { runDir, "true", networkCRS, runId});
				else
					FreightAnalyse.main(new String[] { runDir, "true", networkCRS});
			}
		}


	}

}
