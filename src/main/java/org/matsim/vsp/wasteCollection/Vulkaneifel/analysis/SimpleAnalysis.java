package org.matsim.vsp.wasteCollection.Vulkaneifel.analysis;

import org.matsim.core.events.EventsUtils;
import org.matsim.freight.carriers.CarriersUtils;

public class SimpleAnalysis {

    public static void main(String[] args){


        String[] Iteration = { "10000"}; //"100", "1000", "10000","1","10","100", "1000",
        String[] VehicleTypes = {"diesel_vehicle","EV_small_battery"}; //"diesel_vehicle" ,"EV_medium_battery"
        String filename = "C:\\Users\\phili\\IdeaProjects\\matsim-BA-Vulkaneifel\\output\\RunAbfall_Output_test_155_constraint\\Mi_G\\Iterations";

        for(String i : Iteration){
            for(String VehicleType : VehicleTypes){
                String filename_new = filename + i + "_" + VehicleType + "\\output_events.xml.gz";
                System.out.println("Iteration: " + i + " Vehicletype: "+ VehicleType + "\n");

                var handler = new DumpEventHandler();
                var manager = EventsUtils.createEventsManager();
                manager.addHandler(handler);

                EventsUtils.readEvents(manager, filename_new);

            }
        }


    }
}
