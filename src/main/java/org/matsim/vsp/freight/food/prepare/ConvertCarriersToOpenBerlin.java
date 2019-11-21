package org.matsim.vsp.freight.food.prepare;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReader;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierUtils;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.utils.objectattributes.attributable.Attributes;

class ConvertCarriersToOpenBerlin {
	
	private static final Logger log = Logger.getLogger(ConvertCarriersToOpenBerlin.class);
	
    private final String inputOldNet = "../../tubCloud/Shared/vsp_zerocuts/scenarios/Fracht_LEH-Shipments-Berlin/Input/network.xml";
    private final String inputNewNetwork = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz"; 

    private final String inputOldCarriers = "../../tubCloud/Shared/vsp_zerocuts/scenarios/Fracht_LEH-Shipments-Berlin/Input/I-Base_carrierLEH_v2_withFleet_Shipment.xml";
    private final String outputNewCarriers = "../../tubCloud/Shared/vsp_zerocuts/scenarios/Fracht_LEH_OpenBerlin/Input/I-Base_carrierLEH_v2_withFleet_Shipment.xml";

	private Map<Id<Link>,Id<Link>> handledOldLinksToNewLink = new HashMap<>();

	public static void main(String[] args) {
		new ConvertCarriersToOpenBerlin().run();
	}

	private  void run(){
		//TODO Konvertierung in einzelen Methoden auslagern(?)

		//should be referenced in GK 4 after having a look at the net in via
		Network openBerlinNet = NetworkUtils.readNetwork(inputNewNetwork);
		Network oldNetwork = NetworkUtils.readNetwork(inputOldNet); 		//should be referenced in GK 4 after having a look at the net in via

		Carriers oldCarriers = new Carriers();
		CarrierPlanXmlReader carrierReader = new CarrierPlanXmlReader(oldCarriers);
		carrierReader.readFile(inputOldCarriers);

		//TODO add filter speed < 60 km/h -> nicht auf Autobahnen // vielleicht besser auf Link-eingeschaft.
		NetworkFilterManager mng = new NetworkFilterManager(openBerlinNet);
		mng.addLinkFilter(l -> {
			return l.getAllowedModes().contains("car");
		});
		Network newNetworkFiltered = mng.applyFilters();
		
		Carriers newCarriers = new Carriers();

		for(Carrier oldCarrier: oldCarriers.getCarriers().values()){
			Carrier newCarrier = CarrierUtils.createCarrier(oldCarrier.getId());
			
			//Capabilities
			CarrierCapabilities cc = oldCarrier.getCarrierCapabilities();
			newCarrier.getCarrierCapabilities().setFleetSize(cc.getFleetSize());
			for (CarrierVehicle carrierVehicle: cc.getCarrierVehicles().values()) {
				Id<Link> newLinkId;
				newLinkId = getNewLinkId(oldNetwork, newNetworkFiltered, carrierVehicle.getLocation());
				CarrierVehicle newCarrierVehicle = CarrierVehicle.Builder.newInstance(carrierVehicle.getId(), newLinkId)
						.setTypeId(carrierVehicle.getVehicleTypeId())
						.setEarliestStart(carrierVehicle.getEarliestStartTime())
						.setLatestEnd(carrierVehicle.getLatestEndTime())
						.build();
				CarrierUtils.addCarrierVehicle(newCarrier, newCarrierVehicle);
				//TODO Attributes
			}
			
			//Services
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
				CarrierUtils.addService(newCarrier, newService);
				}
			
			//Shipments
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
				CarrierUtils.addShipment(newCarrier, newShipment);
			}
			
			//Carrier attributes
			Attributes attributes = oldCarrier.getAttributes();
			for (String attKey : attributes.getAsMap().keySet()){
				newCarrier.getAttributes().putAttribute(attKey, attributes.getAttribute(attKey));
			}

			if (oldCarrier.getPlans().size() > 0) {
				log.warn("Please note, that the existing tours will not be available in the converted file");
			}
			
			newCarriers.addCarrier(newCarrier);
		}


		new CarrierPlanXmlWriterV2(newCarriers).write(outputNewCarriers);

		//TODO: Überprüfe ob Anzahl Vehicle, Services und Shipments korrekt ist.
		log.info("#### Done ####");
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

