package org.matsim.vsp.demandGeneration;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class TripEventHandler  implements ActivityStartEventHandler, ActivityEndEventHandler, LinkEnterEventHandler, PersonArrivalEventHandler {

	private final static Logger log = Logger.getLogger(TripEventHandler.class);

	private Network network;
	private CarrierVehicleTypes vehicleTypes;

	CarrierVehicleTypes getVehicleTypes() {
		return vehicleTypes;
	}


	private Map<Id<Person>,Integer> personId2currentTripNumber = new HashMap<>();
	private Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2departureTime = new HashMap<>();
	private Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2tripDistance = new HashMap<>();
	private Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2travelTime = new HashMap<>();
	private Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2amount = new HashMap<>();

	private Map<Id<Person>,Double> driverId2totalDistance = new HashMap<>();
	private Map<Id<Person>,Double> personId2ActivityDurations = new HashMap<>(); //Calculating the duration of all activities of that agent.
	private Map<Id<Person>,Double> personId2DurationFromStartToEnd = new HashMap<>(); // Calculation of duration of whole tour (from start to the end)
	private Map<Id<Person>,Integer> personId2NumberServices = new HashMap<>();	// Counts the services of a tour
	private Map<Id<Person>,Integer> personId2NumberShipments = new HashMap<>();	// Counts the shipments of a tour

	public TripEventHandler(Network network, CarrierVehicleTypes vehicleTypes) {
		this.network = network;
		this.vehicleTypes = vehicleTypes;
	}


	@Override
	public void reset(int iteration) {
		personId2currentTripNumber.clear();
		personId2tripNumber2departureTime.clear();
		personId2tripNumber2tripDistance.clear();
		personId2tripNumber2travelTime.clear();
		personId2ActivityDurations.clear();
		personId2tripNumber2amount.clear();
		driverId2totalDistance.clear();
	}

//	@Override
//	public void handleEvent(Service event) {
//		
//	}
	@Override
	public void handleEvent(LinkEnterEvent event) {
		double linkLength = this.network.getLinks().get(event.getLinkId()).getLength();

		if(driverId2totalDistance.containsKey(event.getVehicleId())){
			driverId2totalDistance.put(Id.createPersonId(event.getVehicleId()),driverId2totalDistance.get(Id.createPersonId(event.getVehicleId())) + linkLength);
		} else {
			driverId2totalDistance.put(Id.createPersonId(event.getVehicleId()),linkLength);
		}

		// updating the trip Length
		int tripNumber = personId2currentTripNumber.get(event.getVehicleId());
		double distanceBefore = personId2tripNumber2tripDistance.get(event.getVehicleId()).get(tripNumber);
		double updatedDistance = distanceBefore + linkLength;
		Map<Integer,Double> tripNumber2tripDistance = personId2tripNumber2tripDistance.get(event.getVehicleId());
		tripNumber2tripDistance.put(tripNumber, updatedDistance);
		personId2tripNumber2tripDistance.put(Id.createPersonId(event.getVehicleId()), tripNumber2tripDistance);
	}


	@Override
	public void handleEvent(ActivityStartEvent event) {
		final Id<Person> personId = event.getPersonId();
		if (event.getActType().equals("end")) {
			personId2DurationFromStartToEnd.put(personId, personId2DurationFromStartToEnd.get(personId) + event.getTime()) ; // Calculation of duration of whole tour (from start to the end)
		} else { //ignore end Event (end of the tour -> no activity, where anything happens for calculation of activity durations
			if (personId2ActivityDurations.containsKey(personId)) {
				personId2ActivityDurations.put(personId, personId2ActivityDurations.get(personId) - event.getTime());
			} else {
				personId2ActivityDurations.put(personId, -event.getTime());
			}
		}
		if (event.getActType().equals("service")) {
			if (personId2NumberServices.containsKey(personId))
				personId2NumberServices.put(personId, personId2NumberServices.get(personId)+1);
			else 
				personId2NumberServices.put(personId, 1);
		}
		if (event.getActType().equals("pickup")) {
			if (personId2NumberShipments.containsKey(personId))
				personId2NumberShipments.put(personId, personId2NumberShipments.get(personId)+1);
			else
				personId2NumberShipments.put(personId, 1);
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		final Id<Person> personId = event.getPersonId();
		if (personId2currentTripNumber.containsKey(personId)) {
			// the following trip is at least the person's second trip
			personId2currentTripNumber.put(personId, personId2currentTripNumber.get(personId) + 1);

			Map<Integer,Double> tripNumber2departureTime = personId2tripNumber2departureTime.get(personId);
			tripNumber2departureTime.put(personId2currentTripNumber.get(personId), event.getTime());
			personId2tripNumber2departureTime.put(personId, tripNumber2departureTime);

			Map<Integer,Double> tripNumber2tripDistance = personId2tripNumber2tripDistance.get(personId);
			tripNumber2tripDistance.put(personId2currentTripNumber.get(personId), 0.0);
			personId2tripNumber2tripDistance.put(personId, tripNumber2tripDistance);

			Map<Integer,Double> tripNumber2amount = personId2tripNumber2amount.get(personId);
			tripNumber2amount.put(personId2currentTripNumber.get(personId), 0.0);
			personId2tripNumber2amount.put(personId, tripNumber2amount);

		} else {
			// the following trip is the person's first trip
			personId2currentTripNumber.put(personId, 1);

			Map<Integer,Double> tripNumber2departureTime = new HashMap<>();
			tripNumber2departureTime.put(1, event.getTime());
			personId2tripNumber2departureTime.put(personId, tripNumber2departureTime);

			Map<Integer,Double> tripNumber2tripDistance = new HashMap<>();
			tripNumber2tripDistance.put(1, 0.0);
			personId2tripNumber2tripDistance.put(personId, tripNumber2tripDistance);

			Map<Integer,Double> tripNumber2amount = new HashMap<>();
			tripNumber2amount.put(1, 0.0);
			personId2tripNumber2amount.put(personId, tripNumber2amount);
		}

		if (event.getActType().equals("start")) {
			personId2DurationFromStartToEnd.put(personId, -event.getTime());
		} else {				//ignore Start Event (Start of the tour -> no activity, where anything happens)
			if (personId2ActivityDurations.containsKey(personId)) {
				personId2ActivityDurations.put(personId, personId2ActivityDurations.get(personId) + event.getTime());
			} else {
				personId2ActivityDurations.put(personId, event.getTime());
			}
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {

		Map<Integer, Double> tripNumber2travelTime;
		if (this.personId2tripNumber2travelTime.containsKey(event.getPersonId())) {
			tripNumber2travelTime = this.personId2tripNumber2travelTime.get(event.getPersonId());

		} else {
			tripNumber2travelTime = new HashMap<>();
		}

		int currentTripNumber = this.personId2currentTripNumber.get(event.getPersonId());
		tripNumber2travelTime.put(currentTripNumber, event.getTime() - this.personId2tripNumber2departureTime.get(event.getPersonId()).get(currentTripNumber));
		this.personId2tripNumber2travelTime.put(event.getPersonId(), tripNumber2travelTime);
	}

	/*package-private*/ Map<Id<Person>,List<Double>> getPersonId2listOfDistances(String carrierIdString) {
		Map<Id<Person>,List<Double>> personId2listOfDistances = new HashMap<>();
		for(Id<Person> personId: personId2tripNumber2tripDistance.keySet()){
			List<Double> distances = new ArrayList<>();
			for(int i : personId2tripNumber2tripDistance.get(personId).keySet()){
				if(personId.toString().contains("_"+carrierIdString+"_")){
					double distance = personId2tripNumber2tripDistance.get(personId).get(i);
					distances.add(distance);
				}
			}
			personId2listOfDistances.put(personId, distances);
		}
		return personId2listOfDistances;
	}


	/**
	 * Counts the amount of services of a tour for all persons (driver) of the specified carrier.
	 * @param carrierIdString
	 * @return
	 */
	/*package-private*/ Map<Id<Person>,Integer> getPersonId2TourServices(String carrierIdString) {
		Map<Id<Person>,Integer> personId2listOfTourServices = new HashMap<>();
		for(Id<Person> personId: personId2NumberServices.keySet()){
				if(personId.toString().contains("_"+carrierIdString+"_")){
					int service = personId2NumberServices.get(personId);
					if (personId2listOfTourServices.containsKey(personId)){
						personId2listOfTourServices.put(personId, personId2listOfTourServices.get(personId));
					} else {
						personId2listOfTourServices.put(personId, service);
					}
				}		
		}
		return personId2listOfTourServices;
	}
	/**
	 * Counts the number of shipments of a tour for all persons (driver) of the specified carrier.
	 * @param carrierIdString
	 * @return
	 */
	/*package-private*/ Map<Id<Person>,Integer> getPersonId2TourShipments(String carrierIdString) {
		Map<Id<Person>,Integer> personId2listOfTourShipments = new HashMap<>();
		for(Id<Person> personId: personId2NumberShipments.keySet()){
				if(personId.toString().contains("_"+carrierIdString+"_")){
					int service = personId2NumberShipments.get(personId);
					if (personId2listOfTourShipments.containsKey(personId)){
						personId2listOfTourShipments.put(personId, personId2listOfTourShipments.get(personId));
					} else {
						personId2listOfTourShipments.put(personId, service);
					}
				}		
		}
		return personId2listOfTourShipments;
	}
	/**
	 * Calculates the distance of a tour for all persons (driver) of the specified carrier.
	 * @param carrierIdString
	 * @return
	 */
	/*package-private*/ Map<Id<Person>,Double> getPersonId2TourDistances(String carrierIdString) {
		Map<Id<Person>,Double> personId2listOfTourDistances = new HashMap<>();
		for(Id<Person> personId: personId2tripNumber2tripDistance.keySet()){
			for(int i : personId2tripNumber2tripDistance.get(personId).keySet()){
				if(personId.toString().contains("_"+carrierIdString+"_")){
					double distance = personId2tripNumber2tripDistance.get(personId).get(i);
					if (personId2listOfTourDistances.containsKey(personId)){
						personId2listOfTourDistances.put(personId, personId2listOfTourDistances.get(personId) + distance);
					} else {
						personId2listOfTourDistances.put(personId, distance);
					}
				}
			}
		}
		return personId2listOfTourDistances;
	}

	/**
	 * Calculates the travel time (excl. time for activities) of a tour for all persons (driver) of the specified carrier.
	 * @param carrierIdString
	 * @return
	 */
	/*package-private*/  Map<Id<Person>, Double> getPersonId2TravelTimes(String carrierIdString) {
		Map<Id<Person>,Double> personId2listOfTravelTimes = new HashMap<>();
		for(Id<Person> personId : personId2tripNumber2travelTime.keySet()){
			for(int i : personId2tripNumber2travelTime.get(personId).keySet()){
				if(personId.toString().contains("_"+carrierIdString+"_")){
					double travelTime = personId2tripNumber2travelTime.get(personId).get(i);
					if (personId2listOfTravelTimes.containsKey(personId)){
						personId2listOfTravelTimes.put(personId, personId2listOfTravelTimes.get(personId) + travelTime);
					} else {
						personId2listOfTravelTimes.put(personId, travelTime);
					}
				}
			}
		}
		return personId2listOfTravelTimes;
	}

	/**
	 * Returns the time for activities of a tour for all persons (Driver) of the specified carrier.
	 * @return
	 */
	/*package-private*/  Map<Id<Person>, Double> getPersonId2SumOfActivityDurations(String carrierIdString) {
		Map<Id<Person>,Double> personId2SumOfActivityDurations = new HashMap<>();
		for(Id<Person> personId : personId2ActivityDurations.keySet()){
			if(personId.toString().contains("_"+carrierIdString+"_")){
				personId2SumOfActivityDurations.put(personId, personId2ActivityDurations.get(personId));
			}
		}
		return personId2SumOfActivityDurations;
	}

	/**
	 * Calculates the distance of a tour for all persons (driver) of all carrier.
	 * @return
	 */
	/*package-private*/  Map<Id<Person>,Double> getPersonId2TourDistances() {
		Map<Id<Person>,Double> personId2listOfTourDistances = new HashMap<>();
		for(Id<Person> personId: personId2tripNumber2tripDistance.keySet()){
			for(int i : personId2tripNumber2tripDistance.get(personId).keySet()){
				double distance = personId2tripNumber2tripDistance.get(personId).get(i);
				if (personId2listOfTourDistances.containsKey(personId)){
					personId2listOfTourDistances.put(personId, personId2listOfTourDistances.get(personId) + distance);
				} else {
					personId2listOfTourDistances.put(personId, distance);
				}
			}
		}
		return personId2listOfTourDistances;
	}

	/**
	 * Calculates the travel time (excl. time for activities) of a tour for all persons (Driver) of all carrier.
	 * @return
	 */
	/*package-private*/  Map<Id<Person>, Double> getPersonId2TravelTimes() {
		Map<Id<Person>,Double> personId2listOfTravelTimes = new HashMap<>();
		for(Id<Person> personId : personId2tripNumber2travelTime.keySet()){
			for(int i : personId2tripNumber2travelTime.get(personId).keySet()){
				double travelTime = personId2tripNumber2travelTime.get(personId).get(i);
				if (personId2listOfTravelTimes.containsKey(personId)){
					personId2listOfTravelTimes.put(personId, personId2listOfTravelTimes.get(personId) + travelTime);
				} else {
					personId2listOfTravelTimes.put(personId, travelTime);
				}
			}
		}
		return personId2listOfTravelTimes;
	}

	/**
	 * Returns the time for activities) of a tour for all persons (Driver) of all carrier.
	 * @return
	 */
	/*package-private*/  Map<Id<Person>, Double> getPersonId2SumOfActivityDurations() {
		return personId2ActivityDurations;
	}


	//Beachte: Personen sind die Agenten, die in ihrer ID auch den Namen ihres FEhrzeugs (und dieses bei ordentlicher Definition ihres FzgTypes enthalten)
	/*package-private*/  Map<Id<VehicleType>,Double> getVehTypId2TourDistances(Id<VehicleType> vehTypeId) {
		log.info("Calculate distances for vehicleTyp " + vehTypeId.toString());
		Map<Id<VehicleType>,Double> vehTypeId2TourDistances = new HashMap<>();
		for(Id<Person> personId: personId2tripNumber2tripDistance.keySet()){
			for(int i : personId2tripNumber2tripDistance.get(personId).keySet()){
				if(personId.toString().contains("_"+vehTypeId.toString()+"_")){
					if (vehTypeId.toString().contains("frozen") == personId.toString().contains("frozen")) { //keine doppelte Erfassung der "frozen" bei den nicht-"frozen"...
						if (vehTypeId.toString().contains("electro") == personId.toString().contains("electro")) {//keine doppelte Erfassung der "electro" bei den nicht-"electro"...
							double distance = personId2tripNumber2tripDistance.get(personId).get(i);
							if (vehTypeId2TourDistances.containsKey(vehTypeId)){
								vehTypeId2TourDistances.put(vehTypeId, vehTypeId2TourDistances.get(vehTypeId) + distance);
								log.debug("Aktuelle Distance für Person " + personId.toString() + " ; " + "_" +vehTypeId.toString() + "_" + "added: " + distance);
							} else {
								vehTypeId2TourDistances.put(vehTypeId, distance);
								log.debug("Distance für Person " + personId.toString() + " ; " + "_" +vehTypeId.toString() + "_" + "added: " + distance);
							}
						}
					}
				}
			}
		}
		return vehTypeId2TourDistances;
	}

	//Beachte: Personen sind die Agenten, die in ihrer ID auch den Namen ihres Fahrzeuges (und dieses bei ordentlicher Definition ihres FzgTypes enthalten)
	/*package-private*/  Map<Id<VehicleType>, Double> getVehTypId2TravelTimes(Id<VehicleType> vehTypeId) {
		Map<Id<VehicleType>,Double> vehTypeId2TravelTimes = new HashMap<>();
		for(Id<Person> personId : personId2tripNumber2travelTime.keySet()){
			for(int i : personId2tripNumber2travelTime.get(personId).keySet()){
				if(personId.toString().contains("_"+vehTypeId.toString()+"_")){
					if (vehTypeId.toString().contains("frozen") == personId.toString().contains("frozen")) { //keine doppelte Erfassung der "frozen" bei den nicht-"frozen"...
						if (vehTypeId.toString().contains("electro") == personId.toString().contains("electro")) {//keine doppelte Erfassung der "electro" bei den nicht-"electro"...
							double travelTime = personId2tripNumber2travelTime.get(personId).get(i);
							if (vehTypeId2TravelTimes.containsKey(vehTypeId)){
								vehTypeId2TravelTimes.put(vehTypeId, vehTypeId2TravelTimes.get(vehTypeId) + travelTime);
							} else {
								vehTypeId2TravelTimes.put(vehTypeId, travelTime);
							}
						}
					}
				}
			}
		}
		return vehTypeId2TravelTimes;
	}

	//Beachte: Personen sind die Agenten, die in ihrer ID auch den Namen ihres FEhrzeugs (und dieses bei ordentlicher Definition ihres FzgTypes enthalten)
	/*package-private*/  Map<Id<VehicleType>, Integer> getVehTypId2VehicleNumber(Id<VehicleType> vehTypeId) {
		Map<Id<VehicleType>,Integer> vehTypeId2VehicleNumber = new HashMap<>();
		for(Id<Person> personId : personId2tripNumber2travelTime.keySet()){
			if(personId.toString().contains("_"+vehTypeId.toString()+"_")){
				if (vehTypeId.toString().contains("frozen") == personId.toString().contains("frozen")) { //keine doppelte Erfassung der "frozen" bei den nicht-"frozen"...
					if (vehTypeId.toString().contains("electro") == personId.toString().contains("electro")) {//keine doppelte Erfassung der "electro" bei den nicht-"electro"...
						if (vehTypeId2VehicleNumber.containsKey(vehTypeId)){
							vehTypeId2VehicleNumber.put(vehTypeId, vehTypeId2VehicleNumber.get(vehTypeId) +1);
						} else {
							vehTypeId2VehicleNumber.put(vehTypeId, 1);
						}
					}
				}
			}
		}
		return vehTypeId2VehicleNumber;
	}

	//Beachte: Personen sind die Agenten, die in ihrer ID auch den Namen ihres Fahrzeugs (und dieses bei ordentlicher Definition ihres FzgTypes enthalten)
	/*package-private*/ Map<Id<VehicleType>, Double> getVehTypId2ActivityDurations(Id<VehicleType> vehTypeId) {
		Map<Id<VehicleType>,Double> vehTypeId2VehicleActivityDurations = new HashMap<>();
		for(Id<Person> personId : personId2ActivityDurations.keySet()){
			if (personId.toString().contains("_" + vehTypeId.toString() + "_")) {
				if (vehTypeId.toString().contains("frozen") == personId.toString().contains("frozen")) { //keine doppelte Erfassung der "frozen" bei den nicht-"frozen"...
					if (vehTypeId.toString().contains("electro") == personId.toString().contains("electro")) {//keine doppelte Erfassung der "electro" bei den nicht-"electro"...
						double activityDuration = personId2ActivityDurations.get(personId);
						if (vehTypeId2VehicleActivityDurations.containsKey(vehTypeId)) {
							vehTypeId2VehicleActivityDurations.put(vehTypeId, vehTypeId2VehicleActivityDurations.get(vehTypeId) + activityDuration);
						} else {
							vehTypeId2VehicleActivityDurations.put(vehTypeId, activityDuration);
						}
					}
				}

			}
		}
		return vehTypeId2VehicleActivityDurations;
	}

	/*package-private*/ Map<Id<VehicleType>, Double> getVehTypId2DurationsStartToEnd(Id<VehicleType> vehTypeId) {
		Map<Id<VehicleType>,Double> vehTypeId2StartToEndDuration = new HashMap<>();
		for(Id<Person> personId : personId2DurationFromStartToEnd.keySet()){
			if (personId.toString().contains("_" + vehTypeId.toString() + "_")) {
				if (vehTypeId.toString().contains("frozen") == personId.toString().contains("frozen")) { //keine doppelte Erfassung der "frozen" bei den nicht-"frozen"...
					if (vehTypeId.toString().contains("electro") == personId.toString().contains("electro")) {//keine doppelte Erfassung der "electro" bei den nicht-"electro"...
						double activityDuration = personId2DurationFromStartToEnd.get(personId);
						if (vehTypeId2StartToEndDuration.containsKey(vehTypeId)) {
							vehTypeId2StartToEndDuration.put(vehTypeId, vehTypeId2StartToEndDuration.get(vehTypeId) + activityDuration);
						} else {
							vehTypeId2StartToEndDuration.put(vehTypeId, activityDuration);
						}
					}
				}

			}
		}
		return vehTypeId2StartToEndDuration;
	}

}
