package org.matsim.vsp.freightAnalysis;

import java.io.IOException;

import org.matsim.core.utils.io.UncheckedIOException;

public class RunAnalyse {

	public static void main(String[] args) throws UncheckedIOException, IOException {
		
//		String runDir = "C:/Users/Ricardo/git/zeroCUTS/output/BusinessPassengerTraffic/leipzig/commercialTraffic_0.1pct_2023-03-03_43939_0.005/";
		String runDir = "C:/Users/Ricardo/Desktop/SoundingBoard/commercialTraffic_20pct_2023-03-06_44655_0.005/";
		String runId = "base.";
//		String runId = null;

		if (runId != null)
			FreightAnalyse.main(new String[] { runDir, "true", runId });
		else
			FreightAnalyse.main(new String[] { runDir, "true"});

	}

}
