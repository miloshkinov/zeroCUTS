package org.matsim.vsp.freight;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.*;
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.ControllerUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.SimWrapperModule;
import org.matsim.vsp.analysis.RunLongDistanceAnalysis;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;

public class RunLongDistanceFreight implements MATSimAppCommand {

    private static final Logger log = LogManager.getLogger(RunLongDistanceFreight.class);

    @CommandLine.Option(names = "--longDistanceFreightPlans", description = "Path to the long distance freight plans file", defaultValue = "../public-svn/matsim/scenarios/countries/de/german-wide-freight/v2/german_freight.25pct.plans_GER_only.xml.gz")
    private static Path inputPlans;

    @CommandLine.Option(names = "--longDistanceFreightNetwork", description = "Path to the long distance freight network file", defaultValue = "../public-svn/matsim/scenarios/countries/de/german-wide-freight/v2/germany-europe-network_GER_only.xml.gz")
    private static Path inputNetwork;

    @CommandLine.Option(names = "--outputDirectory", description = "Path to the output directory", defaultValue = "output/longDistanceFreight")
    private static Path outputDirectory;

    @CommandLine.Option(names =  "--lastIteration", description = "Last iteration to run", defaultValue = "0")
    private static int lastIteration;

    @CommandLine.Option(names ="--endTime", description = "End time of the simulation", defaultValue = "1")
    private static int endTime;

    @CommandLine.Option(names = "--sampleSize", description = "Sample size for the simulation", defaultValue = "1")
    private static double sampleSize;

    public static void main(String[] args) {
        System.exit(new CommandLine(new RunLongDistanceFreight()).execute(args));
    }

    @Override
    public Integer call() {
        Config config = ConfigUtils.createConfig();

        config.controller().setOutputDirectory(outputDirectory.toString());
        config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.abort);
        config.global().setInsistingOnDeprecatedConfigVersion(false);
        config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);
        config.scoring().setFractionOfIterationsToStartScoreMSA(0.8);
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.defaultVehicle);
        config.network().setInputFile(inputNetwork.toString());
        config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.kinematicWaves);
        config.replanning().setFractionOfIterationsToDisableInnovation(0.8);
        config.plans().setRemovingUnneccessaryPlanAttributes(true);
        config.plans().setInputFile(inputPlans.toString());

        config.controller().setLastIteration(lastIteration);
        config.qsim().setEndTime(endTime * 3600);
        config.global().setCoordinateSystem("EPSG:25832");

        SimWrapperConfigGroup simWrapperConfigGroup = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
        simWrapperConfigGroup.sampleSize = sampleSize;

        for (String subpopulation : List.of("longDistanceFreight")) {
            config.replanning().addStrategySettings(
                    new ReplanningConfigGroup.StrategySettings()
                            .setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta)
                            .setWeight(0.85)
                            .setSubpopulation(subpopulation)
            );

            config.replanning().addStrategySettings(
                    new ReplanningConfigGroup.StrategySettings()
                            .setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute)
                            .setWeight(0.1)
                            .setSubpopulation(subpopulation)
            );
        }

        Scenario scenario = ScenarioUtils.loadScenario(config);

        createActivityParams(scenario);

        scenario.getPopulation().getPersons().values().forEach(person -> {
            PopulationUtils.putSubpopulation(person, "longDistanceFreight");
            person.getPlans().forEach(plan -> plan.getPlanElements().forEach(planElement -> {
                if (planElement instanceof Leg) {
                    ((Leg) planElement).setMode("car");
                }
            }));
        });

        Controller controller = ControllerUtils.createController(scenario);

        controller.addOverridingModule(new SimWrapperModule());

        controller.run();

        RunLongDistanceAnalysis runLongDistanceAnalysis = RunLongDistanceAnalysis.createRunLongDistanceAnalysis(outputDirectory);
        runLongDistanceAnalysis.call();

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
                    String newTypeName = ((Activity) planElement).getType();

                    if (newTypeName.contains("freight_start")) {
                        if (config.scoring().getActivityParams(newTypeName) == null) {
                            config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams(newTypeName).setTypicalDuration(1));
                        }
                        continue;
                    }
                    if (newTypeName.contains("freight_end")) {
                        if (config.scoring().getActivityParams(newTypeName) == null) {
                            config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams(newTypeName).setTypicalDuration(1));
                        }
                    }
                }
            }
        }
    }
}