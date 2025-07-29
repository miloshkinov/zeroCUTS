package org.matsim.vsp.freight.food;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.freight.carriers.analysis.CarriersAnalysis;

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
			simOutputPath = "C:\\git-and-svn\\shared-svn\\projects\\freight\\studies\\UpdateEventsfromEarlierStudies\\foodRetailing_wo_rangeConstraint\\71_ICEVBEV_NwCE_BVWP_10000it_DCoff_noTax";
		} else {
			for (String arg : args) {
				log.info( arg );
			}
			simOutputPath = args[0];
		}

		if (!simOutputPath.endsWith(File.separator)) {
			simOutputPath = simOutputPath + File.separator;
		}
        log.info("Running analysis for: {}", simOutputPath);
		CarriersAnalysis freightAnalysis = new CarriersAnalysis(simOutputPath);
		freightAnalysis.runCarrierAnalysis(CarriersAnalysis.CarrierAnalysisType.carriersAndEvents);
	}
}
