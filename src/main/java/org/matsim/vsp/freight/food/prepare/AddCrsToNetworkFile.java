package org.matsim.vsp.freight.food.prepare;

import org.matsim.core.network.NetworkUtils;

/**
 * @author Kai Martins-Turner (kturner)
 */
public class AddCrsToNetworkFile {
	public static void main(String[] args) {
		var inputPath = "/Users/kturner/git-and-svn/shared-svn/projects/freight/studies/Food_LCA-based/output/00_LCA_ICEV-BEV_10it_noTax_2CarrierSmall/";

		var network = NetworkUtils.readNetwork(inputPath + "output_network.xml.gz");
		network.getAttributes().putAttribute("coordinateReferenceSystem", "EPSG:31468");
		NetworkUtils.writeNetwork(network, inputPath + "output_network2.xml.gz");

	}
}
