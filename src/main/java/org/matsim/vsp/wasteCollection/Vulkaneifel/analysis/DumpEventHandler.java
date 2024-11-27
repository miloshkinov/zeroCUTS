package org.matsim.vsp.wasteCollection.Vulkaneifel.analysis;

import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.freight.carriers.events.CarrierShipmentDeliveryEndEvent;
import org.matsim.freight.carriers.events.CarrierShipmentDeliveryStartEvent;

import org.matsim.freight.carriers.events.CarrierTourEndEvent;
import org.matsim.freight.carriers.events.CarrierTourStartEvent;
import org.matsim.freight.carriers.events.eventhandler.CarrierShipmentDeliveryEndEventHandler;
import org.matsim.freight.carriers.events.eventhandler.CarrierShipmentDeliveryStartEventHandler;
import org.matsim.freight.carriers.events.eventhandler.CarrierTourEndEventHandler;
import org.matsim.freight.carriers.events.eventhandler.CarrierTourStartEventHandler;

import java.util.HashMap;
import java.util.Map;

public class DumpEventHandler implements ActivityStartEventHandler, ActivityEndEventHandler {

    int counter = 0;
    Map<String, String> Start = new HashMap<>();
    Map<String, Integer> Start2 = new HashMap<>();

    Map<String, String> End = new HashMap<>();
    Map<String, Integer> End2 = new HashMap<>();
    @Override
    public void handleEvent(ActivityEndEvent activityEndEvent) {
        if(activityEndEvent.getActType().contains("delivery")) {
            int totalSeconds = ((int) activityEndEvent.getTime());
            int hours = totalSeconds / 3600;
            int minutes = (totalSeconds % 3600) / 60;
            int seconds = totalSeconds % 60;

            String time = String.format("%02d:%02d:%02d", hours, minutes, seconds);


            if (counter == 618) {
                End.forEach((key, value) -> System.out.println(key + "   " + value + " Dump_End_time"));
                System.out.println("");
                counter++;
                /*for(String key: Start2.keySet()){
                    int starttime = Start2.get(key);
                    int endtime = End2.get(key);
                    int duration = endtime - starttime;

                System.out.println("Vehicle: " + key + " Duration: " + duration + " s");
                }*/

            }

            if (End.containsKey(activityEndEvent.getPersonId().toString())) {
                if (End2.get(activityEndEvent.getPersonId().toString()) + 3600 < activityEndEvent.getTime()) {
                    End.put(activityEndEvent.getPersonId().toString() + "_2nd_Dump", time);
                    End2.put(activityEndEvent.getPersonId().toString() + "_2nd_Dump", (int) activityEndEvent.getTime());
                    if (End2.containsKey(activityEndEvent.getPersonId().toString() + "_2nd_Dump")) {
                        End.put(activityEndEvent.getPersonId().toString() + "_2nd_Dump", time);
                        End2.put(activityEndEvent.getPersonId().toString() + "_2nd_Dump", (int) activityEndEvent.getTime());
                    } else {
                    }
                } else {
                    End.put(activityEndEvent.getPersonId().toString(), time);
                    End2.put(activityEndEvent.getPersonId().toString(), (int) activityEndEvent.getTime());
                }
            } else {
                End.put(activityEndEvent.getPersonId().toString(), time);
                End2.put(activityEndEvent.getPersonId().toString(), (int) activityEndEvent.getTime());
            }
        }
    }


    @Override
    public void handleEvent(ActivityStartEvent activityStartEvent) {
        if(activityStartEvent.getActType().contains("delivery")){
            int totalSeconds = ((int) activityStartEvent.getTime());
            int hours = totalSeconds / 3600;
            int minutes = (totalSeconds % 3600) / 60;
            int seconds = totalSeconds % 60;

            String time = String.format("%02d:%02d:%02d", hours, minutes, seconds);
            counter++;
            //System.out.println("StartTime: "+ time  + " Dump: " + activityStartEvent.getLinkId() + " Vehicle: "+ activityStartEvent.getPersonId());

            if(Start.containsKey(activityStartEvent.getPersonId().toString())){
                 if(Start2.get(activityStartEvent.getPersonId().toString()) + 3700 < (int) activityStartEvent.getTime()){
                     if(Start2.containsKey(activityStartEvent.getPersonId().toString()+ "_2nd_Dump")) {
                         if(Start2.get(activityStartEvent.getPersonId().toString()+ "_2nd_Dump") + 3700 < (int) activityStartEvent.getTime()){
                             Start.put(activityStartEvent.getPersonId().toString() + "_3rd_Dump", time);
                             Start2.put(activityStartEvent.getPersonId().toString() + "_3rd_Dump", (int) activityStartEvent.getTime());
                         }
                     }else {
                         Start.put(activityStartEvent.getPersonId().toString() + "_2nd_Dump", time);
                         Start2.put(activityStartEvent.getPersonId().toString() + "_2nd_Dump", (int) activityStartEvent.getTime());
                     }
                 }
            } else {
                Start.put(activityStartEvent.getPersonId().toString(), time);
                Start2.put(activityStartEvent.getPersonId().toString(), (int) activityStartEvent.getTime());
            }
            if(counter > 617) {

                System.out.println("\nDump Arrival Time");
                Start.forEach((key, value) -> System.out.println(key + "   " + value));
                System.out.println("___________________ " + counter + "\nDump Departure Time");
            }
        }

    }


}

