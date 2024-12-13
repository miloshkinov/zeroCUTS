package org.matsim.vsp.freight.food.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.freight.carriers.*;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;

class ConvertCarriersToOpenBerlin {
	
	private static final Logger log = LogManager.getLogger(ConvertCarriersToOpenBerlin.class);

	private Map<Id<Link>,Id<Link>> handledOldLinksToNewLink = new HashMap<>();

	public static void main(String[] args){
		new ConvertCarriersToOpenBerlin().run();
	}

	private  void run(){
		//should be referenced in GK 4 after having a look at the net in via
		String inputNewNetwork = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";
		String inputOldNetwork = "../tubCloud/Shared/vsp_zerocuts/scenarios/archive/Fracht_LEH-Shipments-Berlin_oneTW/input/network.xml";

		String inputOldCarriers = "../tubCloud/Shared/vsp_zerocuts/scenarios/archive/Fracht_LEH-Shipments-Berlin_oneTW/input/I-Base_carrierLEH_v2_withFleet_Shipment_OneTW.xml";
		String outputNewCarriers = "../tubCloud/Shared/vsp_zerocuts/scenarios/Fracht_LEH_OpenBln_oneTW/input/I-Base_carrierLEH_v2_withFleet_Shipment_OneTW_PickupTime.xml";

		Network openBerlinNet = NetworkUtils.readNetwork(inputNewNetwork);
		Network oldNetwork = NetworkUtils.readNetwork(inputOldNetwork); 		//should be referenced in GK 4 after having a look at the net in via

		Carriers oldCarriers = new Carriers();
		CarrierPlanXmlReader carrierReader = new CarrierPlanXmlReader(oldCarriers, null);
		carrierReader.readFile(inputOldCarriers);

		Network newNetworkFiltered = filterNetwork(openBerlinNet);

		Carriers newCarriers = new Carriers();
		for(Carrier oldCarrier: oldCarriers.getCarriers().values()){
			Carrier newCarrier = CarriersUtils.createCarrier(oldCarrier.getId());

			convertCarrierCapabilities(oldNetwork, newNetworkFiltered, oldCarrier, newCarrier);
			convertCarrierServices(oldNetwork, newNetworkFiltered, oldCarrier, newCarrier);
			convertCarrierShipments(oldNetwork, newNetworkFiltered, oldCarrier, newCarrier);
			convertCarrierAttributes(oldCarrier, newCarrier);
			convertCarrierPlans(oldCarrier);

			newCarriers.addCarrier(newCarrier);
		}

		someChecks(oldCarriers, newCarriers);

		new CarrierPlanWriter(newCarriers).write(outputNewCarriers);
//		new CarrierPlanXmlWriterV2(newCarriers).write(outputNewCarriers);

		log.info("#### Done ####");
	}

	private Network filterNetwork(Network openBerlinNet) {
		NetworkFilterManager mng = new NetworkFilterManager(openBerlinNet, null);

		//allowedMode is at least car
		mng.addLinkFilter(link -> link.getAllowedModes().contains("car"));

		//no motorways
		mng.addLinkFilter(link -> !link.getAttributes().getAttribute("type").equals("motorway"));

		//no motorway-links
		mng.addLinkFilter(link -> !link.getAttributes().getAttribute("type").equals("motorway_link"));
		return mng.applyFilters();
	}

	private void convertCarrierCapabilities(Network oldNetwork, Network newNetworkFiltered, Carrier oldCarrier, Carrier newCarrier) {
		CarrierCapabilities cc = oldCarrier.getCarrierCapabilities();
		newCarrier.getCarrierCapabilities().setFleetSize(cc.getFleetSize());
		for (CarrierVehicle carrierVehicle: cc.getCarrierVehicles().values()) {
			Id<Link> newLinkId;
			newLinkId = getNewLinkId(oldNetwork, newNetworkFiltered, carrierVehicle.getLinkId());
			//Adapt VehicleId to new Location
			String oldVehicleString = carrierVehicle.getId().toString();
			Id<Vehicle> newVehicleId = Id.createVehicleId(oldVehicleString.substring(0,oldVehicleString.lastIndexOf("_")+1)+newLinkId.toString());
			CarrierVehicle newCarrierVehicle = CarrierVehicle.Builder.newInstance(newVehicleId, newLinkId, carrierVehicle.getType())
					.setEarliestStart(carrierVehicle.getEarliestStartTime())
					.setLatestEnd(carrierVehicle.getLatestEndTime())
					.build();
			CarriersUtils.addCarrierVehicle(newCarrier, newCarrierVehicle);
		}
	}

	private void convertCarrierServices(Network oldNetwork, Network newNetworkFiltered, Carrier oldCarrier, Carrier newCarrier) {
		for (CarrierService service : oldCarrier.getServices().values()) {
			Id<Link> newLinkId;
			newLinkId = getNewLinkId(oldNetwork, newNetworkFiltered, service.getLocationLinkId());
			CarrierService newService =
					CarrierService.Builder.newInstance(Id.create(service.getId(), CarrierService.class), newLinkId)
							.setCapacityDemand(service.getCapacityDemand())
							.setServiceDuration(service.getServiceDuration())
							.setServiceStartTimeWindow(service.getServiceStartTimeWindow())
							.build();
			for (String attKey : service.getAttributes().getAsMap().keySet()){
				newService.getAttributes().putAttribute(attKey, service.getAttributes().getAttribute(attKey));
			}
			CarriersUtils.addService(newCarrier, newService);
		}
	}

	private void convertCarrierShipments(Network oldNetwork, Network newNetworkFiltered, Carrier oldCarrier, Carrier newCarrier) {
		for (CarrierShipment shipment : oldCarrier.getShipments().values()) {
			Id<Link> newLinkIdFrom;
			Id<Link> newLinkIdTo;
			newLinkIdFrom = getNewLinkId(oldNetwork, newNetworkFiltered, shipment.getFrom());
			newLinkIdTo = getNewLinkId(oldNetwork, newNetworkFiltered, shipment.getTo());
			CarrierShipment newShipment =
					CarrierShipment.Builder.newInstance(Id.create(shipment.getId(), CarrierShipment.class),newLinkIdFrom, newLinkIdTo, shipment.getSize())
							.setPickupServiceTime(shipment.getPickupServiceTime())
							.setDeliveryServiceTime(shipment.getDeliveryServiceTime())
							.setPickupTimeWindow(shipment.getPickupTimeWindow())
							.setDeliveryTimeWindow(shipment.getDeliveryTimeWindow())
							.build();
			for (String attKey : shipment.getAttributes().getAsMap().keySet()){
				newShipment.getAttributes().putAttribute(attKey, shipment.getAttributes().getAttribute(attKey));
			}
			CarriersUtils.addShipment(newCarrier, newShipment);
		}
	}

	private void convertCarrierAttributes(Carrier oldCarrier, Carrier newCarrier) {
		Attributes attributes = oldCarrier.getAttributes();
		for (String attKey : attributes.getAsMap().keySet()){
			newCarrier.getAttributes().putAttribute(attKey, attributes.getAttribute(attKey));
		}
	}

	private void convertCarrierPlans(Carrier oldCarrier) {
		if (oldCarrier.getPlans().size() > 0) {
			log.warn("Please note, that the existing tours will not be available in the converted file");
		}
	}

	private void someChecks(Carriers oldCarriers, Carriers newCarriers) {
		if (oldCarriers.getCarriers().size() != newCarriers.getCarriers().size())
			throw new AssertionError("Number of carriers are not equal.");
		for (Id<Carrier> carrierId : oldCarriers.getCarriers().keySet()){
			if (newCarriers.getCarriers().get(carrierId) == null)
				throw new AssertionError("Carrier with Id is missing in converted Carriers: " + carrierId.toString());
			if (oldCarriers.getCarriers().get(carrierId).getCarrierCapabilities().getCarrierVehicles().size() != newCarriers.getCarriers().get(carrierId).getCarrierCapabilities().getCarrierVehicles().size())
				throw new AssertionError("Number of vehicles of carrier are not equal: " + carrierId.toString());
			if (oldCarriers.getCarriers().get(carrierId).getServices().size() != newCarriers.getCarriers().get(carrierId).getServices().size())
				throw new AssertionError("Number of services of carrier are not equal: " + carrierId.toString());
			if (oldCarriers.getCarriers().get(carrierId).getShipments().size() != newCarriers.getCarriers().get(carrierId).getShipments().size())
				throw new AssertionError("Number of shipments of carrier are not equal: " + carrierId.toString());
		}
	}

	private Id<Link> getNewLinkId(Network oldNetwork, Network newNetwork, Id<Link> oldLinkId) {
		Id<Link> newLinkId;
		if(!handledOldLinksToNewLink.containsKey(oldLinkId)) {
			Coord oldLinkCoord = oldNetwork.getLinks().get(oldLinkId).getCoord();
			newLinkId = NetworkUtils.getNearestLink(newNetwork, oldLinkCoord).getId();
			handledOldLinksToNewLink.put(oldLinkId, newLinkId);
		} else{
			newLinkId =handledOldLinksToNewLink.get(oldLinkId);
		}
		return newLinkId;
	}

}

