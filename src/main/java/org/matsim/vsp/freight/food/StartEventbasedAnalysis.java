package org.matsim.vsp.freight.food;

import org.matsim.contrib.freight.analysis.RunFreightAnalysisEventbased;

import java.io.IOException;

/**
 * @author Kai Martins-Turner (kturner)
 */
public class StartEventbasedAnalysis {

	public static void main(String[] args) throws IOException {
		final String outputPath = "/Users/kturner/git-and-svn/shared-svn/projects/freight/studies/Food_LCA-based/output/31_BVWP100_ICEV-BEV_10000it_noTax";
		RunFreightAnalysisEventbased freightAnalysis = new RunFreightAnalysisEventbased(outputPath +"/", outputPath +"/Analysis2/");
		freightAnalysis.runAnalysis();
	}
}
