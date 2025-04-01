package org.matsim.vsp.freight;

import org.matsim.application.prepare.freight.tripExtraction.ExtractRelevantFreightTrips;

import java.util.ArrayList;
import java.util.List;

public class ExtractLongDistanceFreight {

    public static void main(String[] args) throws Exception {

        String longDistanceFreightPopulationName = "output/longDistancePlans/german_freight.25pct.plans_GER_only.xml.gz";

        List<String> argumentsForFreightTransitTraffic = new ArrayList<>();
        argumentsForFreightTransitTraffic.add("../public-svn/matsim/scenarios/countries/de/german-wide-freight/v2/german_freight.25pct.plans.xml.gz");
        argumentsForFreightTransitTraffic.add("--network");
        argumentsForFreightTransitTraffic.add("../public-svn/matsim/scenarios/countries/de/german-wide-freight/v2/germany-europe-network.xml.gz");
        argumentsForFreightTransitTraffic.add("--output");
        argumentsForFreightTransitTraffic.add(longDistanceFreightPopulationName);
        argumentsForFreightTransitTraffic.add("--shp");
        argumentsForFreightTransitTraffic.add("../public-svn/matsim/scenarios/countries/de/german-wide-freight/raw-data/shp/NUTS3/NUTS3_2010_DE.shp");
        argumentsForFreightTransitTraffic.add("--input-crs");
        argumentsForFreightTransitTraffic.add("EPSG:25832");
        argumentsForFreightTransitTraffic.add("--target-crs");
        argumentsForFreightTransitTraffic.add("EPSG:25832");
        argumentsForFreightTransitTraffic.add("--shp-crs");
        argumentsForFreightTransitTraffic.add("EPSG:31467");
        argumentsForFreightTransitTraffic.add("--geographicalTripType");
        argumentsForFreightTransitTraffic.add("ALL");
        argumentsForFreightTransitTraffic.add("--legMode");
        argumentsForFreightTransitTraffic.add("car");
        argumentsForFreightTransitTraffic.add("--cut-on-boundary");

        new ExtractRelevantFreightTrips().execute(argumentsForFreightTransitTraffic.toArray(new String[0]));
    }
}
