package org.matsim.vsp.DistanceConstraint;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReader;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierUtils;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import com.google.common.collect.Multimap;



public class TestRunDistanceConstraint {
	static final Logger log = Logger.getLogger(TestRunDistanceConstraint.class);

	private static final String original_Chessboard = "https://raw.githubusercontent.com/matsim-org/matsim/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml";
	private static final String input_vehicleTypes = "scenarios/DistanceConstraint/vehicleTypesExample.xml";
	private static final String input_carriers = "scenarios/DistanceConstraint/carriers-3_vehicles.xml";

	public static void main(String[] args) throws IOException {
		
		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory("output/original_Chessboard/Test1");
		config.network().setInputFile(original_Chessboard);
		config = TestRunDistanceConstraintUtils.prepareConfig(config, 0);		
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
//		Carrier myTestCarrier = CarrierUtils.createCarrier( Id.create("myCarrier", Carrier.class) );
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(vehicleTypes).readFile(input_vehicleTypes);
		
		Carriers carriers = new Carriers() ;
		new CarrierPlanXmlReader(carriers).readFile(input_carriers) ;

		// assign vehicle types to the carriers
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes) ;
		
		int jspritIterations = 100;
		TestRunDistanceConstraintUtils.solveWithJsprit(scenario, carriers, jspritIterations, vehicleTypes);
		final Controler controler = new Controler(scenario);
		
		TestRunDistanceConstraintUtils.scoringAndManagerFactory(scenario, carriers, controler);
		controler.run();
		new CarrierPlanXmlWriterV2(carriers)
		.write(scenario.getConfig().controler().getOutputDirectory() + "/output_CarrierPlans.xml");
//		TourAnalyseTry.creatingResultFiles(scenario, carriers, jspritIterations,
//				vehicleTypes, batteryConstraints);


	}

}
