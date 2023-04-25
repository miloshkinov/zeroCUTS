package org.matsim.vsp.freightAnalysis;

import java.io.IOException;

import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.UncheckedIOException;

public class RunAnalyse {

	public static void main(String[] args) throws UncheckedIOException, IOException {
		
//		String runDir = "C:/Users/Ricardo/git/zeroCUTS/output/BusinessPassengerTraffic/leipzig/commercialTraffic_0.1pct_2023-03-03_43939_0.005/";
		String runDir = "C:/Users/Ricardo/Desktop/SoundingBoard/businessTraffic_5pct_2023-03-07_48622_0.005/";
//		String runId = "tax40CV.";
		String networkCRS = TransformationFactory.DHDN_GK4;
		String runId = "base.";

		if (runId != null)
			FreightAnalyse.main(new String[] { runDir, "true", networkCRS, runId });
		else
			FreightAnalyse.main(new String[] { runDir, "true", networkCRS});

	}

}
