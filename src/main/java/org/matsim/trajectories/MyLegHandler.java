/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
  
package org.matsim.trajectories;

import java.util.ArrayList;

import org.matsim.core.scoring.EventsToLegs.LegHandler;
import org.matsim.core.scoring.PersonExperiencedLeg;

class MyLegHandler implements LegHandler {
	
	//TODO: Umwandeln noch in Map<Person, ArrayList>
	//TODO: Wie mappen wir es auf die Fahrzeuge?
	private ArrayList<PersonExperiencedLeg> legList = new ArrayList<PersonExperiencedLeg>();

	@Override
	public void handleLeg(PersonExperiencedLeg leg) {
		legList.add(leg);

	}
	
	public void writeLegsToConsole() {
		System.out.println("Experienced legs: " + legList.toString());
		
	}
	

}
