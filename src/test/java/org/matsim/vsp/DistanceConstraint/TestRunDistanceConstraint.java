package org.matsim.vsp.DistanceConstraint;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierUtils;
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

	public static void main(String[] args) throws IOException {
		
		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory("output/original_Chessboard/Version4");
		config.network().setInputFile(original_Chessboard);
		config = TestRunDistanceConstraintUtils.prepareConfig(config, 0);		
		
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Carriers carriers = new Carriers();
		
		Carrier myTestCarrier = CarrierUtils.createCarrier( Id.create("myCarrier", Carrier.class) );
		boolean electricCar = true;
		boolean addAdditionalVehicle = true;
		CarrierVehicleTypes vehicleTypes = TestRunDistanceConstraintUtils.createAndAddVehicles(electricCar, addAdditionalVehicle); 
		Multimap<String, Double[]> batteryConstraints = DistanceConstraintUtils.createVehilceTypeBatteryConstraints(vehicleTypes); 
		
		//Shipment 1
		CarrierShipment shipment1 = CarrierShipment.Builder
				.newInstance(Id.create("Shipment1", CarrierShipment.class), Id.createLinkId("i(1,8)"),
						Id.createLinkId("j(3,8)"), 40)
				.setDeliveryServiceTime(20).setDeliveryTimeWindow(TimeWindow.newInstance(0 * 3600, 1 * 3600))
				.setPickupServiceTime(20).setPickupTimeWindow(TimeWindow.newInstance(0 * 3600, 0.2 * 3600))
				.build();
		CarrierUtils.addShipment(myTestCarrier, shipment1);
		
		//Shipment 2
		CarrierShipment shipment2 = CarrierShipment.Builder
				.newInstance(Id.create("Shipment2", CarrierShipment.class), Id.createLinkId("i(1,8)"),
						Id.createLinkId("j(0,3)R"), 40)
				.setDeliveryServiceTime(30).setDeliveryTimeWindow(TimeWindow.newInstance(0 * 3600, 1 * 3600))
				.setPickupServiceTime(30).setPickupTimeWindow(TimeWindow.newInstance(0 * 3600, 0.2 * 3600))
				.build();
		CarrierUtils.addShipment(myTestCarrier, shipment2);
		
		CarrierShipment shipment3 = CarrierShipment.Builder
				.newInstance(Id.create("Shipment3", CarrierShipment.class), Id.createLinkId("i(1,8)"),
						Id.createLinkId("j(9,2)"), 40)
				.setDeliveryServiceTime(30).setDeliveryTimeWindow(TimeWindow.newInstance(0 * 3600, 1 * 3600))
				.setPickupServiceTime(30).setPickupTimeWindow(TimeWindow.newInstance(0 * 3600, 0.2 * 3600))
				.build();
		CarrierUtils.addShipment(myTestCarrier, shipment3);
		
		carriers.addCarrier(myTestCarrier);
		
		FleetSize fleetSize = FleetSize.INFINITE;
		TestRunDistanceConstraintUtils.createCarriers(carriers, fleetSize, myTestCarrier, scenario, vehicleTypes);
		
		int jspritIterations = 100;
		TestRunDistanceConstraintUtils.solveWithJsprit(scenario, carriers, myTestCarrier, jspritIterations, vehicleTypes, batteryConstraints);
		final Controler controler = new Controler(scenario);
		
		TestRunDistanceConstraintUtils.scoringAndManagerFactory(scenario, carriers, controler);
		controler.run();
		new CarrierPlanXmlWriterV2(carriers)
		.write(scenario.getConfig().controler().getOutputDirectory() + "/output_CarrierPlans.xml");
//		TourAnalyseTry.creatingResultFiles(scenario, carriers, jspritIterations,
//				vehicleTypes, batteryConstraints);


	}

}
