package org.matsim.vsp.SmallScaleFreightTraffic;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
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
	
	@CommandLine.Option(names = "config", description = "Path to the config file.", defaultValue = "../public-svn/matsim/scenarios/countries/de/berlin/projects/zerocuts/small-scale-commercial-traffic/input/config_new.xml")
	private static Path configURL;
	
	@CommandLine.Option(names = "--output", description = "Path to output folder", required = true, defaultValue = "output/BusinessPassengerTraffic_MATSim/")
	private Path output;
	
	public static void main(String[] args) {
		System.exit(new CommandLine(new RunMATSimCommercialTraffic()).execute(args));
	}

	@Override
	public Integer call() throws Exception {
		
		Config config = ConfigUtils.loadConfig(configURL.toString());
		output = output.resolve(java.time.LocalDate.now().toString() + "_" + java.time.LocalTime.now().toSecondOfDay());
		config.controler().setOutputDirectory(output.toString());
		new OutputDirectoryHierarchy(config.controler().getOutputDirectory(), config.controler().getRunId(),
				config.controler().getOverwriteFileSetting(), ControlerConfigGroup.CompressionType.gzip);
		config.counts().setInputFile("counts_berlin_Lkw.xml");
		config.vehicles().setVehiclesFile("berlin_bothTypes_1pct_allVehicles.xml.gz");
		config.plans().setInputFile("berlin_bothTypes_1pct_plans.xml.gz");
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		createActivityParams(scenario);
		
		Controler controler = prepareControler(scenario);
		
		controler.run();
		
		return 0;
	}
	
	private void createActivityParams(Scenario scenario) {
		Population population = scenario.getPopulation();
		Config config = scenario.getConfig();
		int i = 0;
		for (Person person : population.getPersons().values()) {
			for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
				if (planElement instanceof Activity) {
					i++;
					String newTypeName = ((Activity) planElement).getType().toString()+"_"+i;
					((Activity) planElement).setType(newTypeName);
					if (newTypeName.contains("service"))
						config.planCalcScore().addActivityParams(new ActivityParams(newTypeName).setTypicalDuration( ((Activity) planElement).getMaximumDuration().seconds()).setOpeningTime(6. * 3600. ).setClosingTime(20. * 3600. ) );
					if (newTypeName.contains("start"))
						config.planCalcScore().addActivityParams(new ActivityParams(newTypeName).setTypicalDuration(1).setOpeningTime(6. * 3600.).setClosingTime(24. * 3600.).setLatestStartTime(((Activity) planElement).getEndTime().seconds()) );
					if (newTypeName.contains("end"))
						config.planCalcScore().addActivityParams(new ActivityParams(newTypeName).setTypicalDuration(1).setOpeningTime(6. * 3600. ).setClosingTime(24. * 3600. ).setLatestStartTime(((Activity) planElement).getStartTime().seconds()));
				}
			}
		}
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
