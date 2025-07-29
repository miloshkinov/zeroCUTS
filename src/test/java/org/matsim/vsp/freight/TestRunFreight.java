/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.vsp.freight;

import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author kturner
 *
 */
public class TestRunFreight {
	
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils() ;

	/**
	 * First very simple Test method for {@link org.matsim.vsp.freight.RunFreight#main(java.lang.String[])}.
	 * Only checks if it runs without exception.
	 * TODO: Make possible to set max jsprit iteration to 1 -> make sure that test can run in acceptable test
	 * TODO: Change InputFiles to small chessboard scenario.
	 */
	@Test
	public void testMain() {
		try {
			RunFreight.main(null);
		} catch ( Exception ee ) {
            LogManager.getLogger(this.getClass()).fatal("there was an exception: \n{}", String.valueOf(ee));
			// if one catches an exception, then one needs to explicitly fail the test:
			Assertions.fail();
		}
	}
}
