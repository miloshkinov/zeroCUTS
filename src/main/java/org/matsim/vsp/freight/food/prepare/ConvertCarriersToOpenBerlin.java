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


	public static void main(String[] args) {
		new ConvertCarriersToOpenBerlin().run();
	}

	private  void run(){

		//should be referenced in GK 4 after having a look at the net in via

		Network openBerlinNet = NetworkUtils.readNetwork(inputNewNetwork);
		Network oldSchroederNet = NetworkUtils.readNetwork(inputOldNet); 		//should be referenced in GK 4 after having a look at the net in via

		Carriers oldCarriers = new Carriers();
		CarrierPlanXmlReader carrierReader = new CarrierPlanXmlReader(oldCarriers);
		carrierReader.readFile(inputOldCarriers);

		Map<Id<Link>,Id<Link>> handledOldLinksToNewLink = new HashMap<>();
		Map<Id<Link>, Carrier> handledNewLinksToCarrier = new HashMap<>();

		NetworkFilterManager mng = new NetworkFilterManager(openBerlinNet);
		mng.addLinkFilter(l -> {
			return l.getAllowedModes().contains("car");
		});
		Network newNetOnlyCar = mng.applyFilters();
		
		Carriers newCarriers = new Carriers();

		for(Carrier carrier: oldCarriers.getCarriers().values()){
			Carrier newCarrier = CarrierUtils.createCarrier(carrier.getId());
			
			//Capabilities TODO: implement with all details
			CarrierCapabilities cc = carrier.getCarrierCapabilities();
			log.error("CarrierCapabilities are not implemented");
			for (CarrierVehicle carrierVehicle: cc.getCarrierVehicles().values()) {
				log.error("not implemented");
			}
			
			//Services
			//we only have services in this carriers file, no shipments
			//TODO: look at this and work on it
			for (CarrierService service : carrier.getServices().values()) {
				Id<Link> newLink =handledOldLinksToNewLink.get(service.getLocationLinkId());
				Coord oldLinkC = oldSchroederNet.getLinks().get(service.getLocationLinkId()).getCoord();
				if(newLink == null) {
					newLink = NetworkUtils.getNearestLink(newNetOnlyCar, oldLinkC).getId();
					handledOldLinksToNewLink.put(service.getLocationLinkId(), newLink);
				}

				CarrierService newService =
							CarrierService.Builder.newInstance(Id.create(service.getId(), CarrierService.class), newLink)
							.setCapacityDemand(service.getCapacityDemand())
							.setServiceDuration(service.getServiceDuration())
							.setServiceStartTimeWindow(service.getServiceStartTimeWindow())
							.build();
					newCarrier.getServices().put(newService.getId(), newService);
					handledNewLinksToCarrier.put(newLink, newCarrier);
				}
			
			//Shipments TODO: implement
			for (CarrierShipment shipment : carrier.getShipments().values()) {
				log.error("not implemented");
			}
			
			//Attributes
			Attributes attributes = carrier.getAttributes();
			for (String att_Key : attributes.getAsMap().keySet()){
				newCarrier.getAttributes().putAttribute(att_Key, attributes.getAttribute(att_Key));
			}
			
			log.warn("Please note, that the tours are not available in the converted file");
			
			newCarriers.addCarrier(carrier);
		}
		
		new CarrierPlanXmlWriterV2(newCarriers).write(outputNewCarriers);

	}

}

