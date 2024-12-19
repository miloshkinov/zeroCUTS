package org.matsim.vsp.analysis;

public class RunAnalysis {

	public static void main(String[] args) {

//		String outputDirectory = "output/goodsTraffic/";
		String outputDirectory = "../matsim-berlin/output/commercialPersonTraffic/";
//		String outputDirectory = "../zerocuts/output/BusinessPassengerTraffic/berlin/calibration/bothTypes_1pct_2022-12-14_41270_0.001/";
//		String rundID = "goodsTraffic";
		String rundID = "commercialPersonTraffic";
		CommercialAnalysis.main(new String[]{outputDirectory, rundID, "true"});
	}
}
