/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.examples.ExamplesUtils;

/**
 * @author nagel
 *
 */
public class RunMatsim {

	private Config config ;
	private Scenario scenario ;
	private Controler controler ;

	private boolean hasPreparedConfig = false ;
	private boolean hasPreparedScenario = false ;
	private boolean hasPreparedControler = false ;

	public static void main(String[] args) {
		new RunMatsim().run() ;
	}

	Config prepareConfig() {
		hasPreparedConfig = true ;

		ExamplesUtils.getTestScenarioURL(  )

		config = ConfigUtils.loadConfig( url ) ;

		// default modifications of config go here

		return config ;
	}

	Scenario prepareScenario() {
		if ( !hasPreparedConfig ) {
			prepareConfig() ;
			hasPreparedConfig = true ;
		}
		scenario = ScenarioUtils.loadScenario( config ) ;

		// default modifications of scenario go here

		return scenario ;
	}

	Controler prepareControler() {
		if ( !hasPreparedScenario ) {
			prepareScenario() ;
			hasPreparedScenario = true ;
		}
		controler = new Controler( scenario ) ;

		// default modifications of controler go here

		return controler ;
	}

	void run() {
		if ( !hasPreparedControler ) {
			prepareControler() ;
			hasPreparedControler = true ;
		}
		controler.run() ;
	}

}
