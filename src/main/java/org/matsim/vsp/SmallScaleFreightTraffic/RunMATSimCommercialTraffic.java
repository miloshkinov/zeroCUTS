package org.matsim.vsp.SmallScaleFreightTraffic;

import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import picocli.CommandLine;

/**
 * @author Ricardo Ewert
 *
 */
@CommandLine.Command(name = "generate-business-passenger-traffic", description = "Generate business passenger traffic model", showDefaultValues = true)

public class RunMATSimCommercialTraffic implements Callable<Integer> {

	private static final Logger log = LogManager.getLogger(RunMATSimCommercialTraffic.class);
	
	@CommandLine.Option(names = "config", description = "Path to the config file.", defaultValue = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/zerocuts/small-scale-commercial-traffic/input/config.xml")
	private static URL configURL;
	
	@CommandLine.Option(names = "--output", description = "Path to output folder", required = true, defaultValue = "output/BusinessPassengerTraffic_MATSim/")
	private Path output;
	
	public static void main(String[] args) {
		System.exit(new CommandLine(new RunMATSimCommercialTraffic()).execute(args));
	}

	@Override
	public Integer call() throws Exception {
		
		Config config = ConfigUtils.loadConfig(configURL);
		output = output.resolve(java.time.LocalDate.now().toString() + "_" + java.time.LocalTime.now().toSecondOfDay());
		config.controler().setOutputDirectory(output.toString());
		new OutputDirectoryHierarchy(config.controler().getOutputDirectory(), config.controler().getRunId(),
				config.controler().getOverwriteFileSetting(), ControlerConfigGroup.CompressionType.gzip);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Controler controler = prepareControler(scenario);
		
		controler.run();
		
		return 0;
	}
	
	/**
	 * Prepares the controller.
	 * 
	 * @param scenario
	 * @return
	 */
	private Controler prepareControler(Scenario scenario) {
		Controler controler = new Controler(scenario);
		
		if (controler.getConfig().transit().isUseTransit()) {
			// use the sbb pt raptor router
			controler.addOverridingModule( new AbstractModule() {
				@Override
				public void install() {
					install( new SwissRailRaptorModule() );
				}
			} );
		}
		return controler;
	}
}
