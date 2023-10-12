package org.matsim.vsp.freight.food;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.freight.carriers.analysis.analysis.RunFreightAnalysisEventBased;


import java.io.File;
import java.io.IOException;

/**
 * @author Kai Martins-Turner (kturner)
 */
public class StartEventbasedAnalysis {

	static final Logger log = LogManager.getLogger(StartEventbasedAnalysis.class);

	public static void main(String[] args) throws IOException {
		String simOutputPath;

		if ( args.length==0 ) {
			simOutputPath = "/Users/kturner/git-and-svn/shared-svn/projects/freight/studies/Food_LCA-based/output/01_LCA_ICEV_10000it_noTax";
		} else {
			for (String arg : args) {
				log.info( arg );
			}
			simOutputPath = args[0];
		}

		if (!simOutputPath.endsWith(File.separator)) {
			simOutputPath = simOutputPath + File.separator;
		}
		log.info("Running analysis for: " + simOutputPath);
		RunFreightAnalysisEventBased freightAnalysis = new RunFreightAnalysisEventBased(simOutputPath, simOutputPath +"Analysis"+File.separator, null);
		freightAnalysis.runAnalysis();
	}
}
