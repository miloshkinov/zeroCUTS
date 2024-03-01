package org.matsim.vsp.emissions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.analysis.EmissionsOnLinkEventHandler;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

/**
 * @author Kai Martins-Turner (kturner)
 */
public class EmissionsWriterUtils {

  private static final Logger log = LogManager.getLogger(EmissionsWriterUtils.class);
  /**
   * Schreibt CSV Dateien raus mit Emission pro LINK - einmal als Summe für den Link -> kann genutzt
   * werden für Gesamtsumme der Emissionen. - einmal pro Meter -> für grafische Analyse
   *
   * @param linkEmissionAnalysisFile
   * @param linkEmissionPerMAnalysisFile
   * @param scenario
   * @param emissionsEventHandler
   * @throws IOException
   */
  static void writePerLinkOutput(String linkEmissionAnalysisFile,
      String linkEmissionPerMAnalysisFile, Scenario scenario,
      EmissionsOnLinkEventHandler emissionsEventHandler)
      throws IOException {
    log.info("Emission analysis completed.");
    log.info("Writing output per LINK...");

    NumberFormat nf = NumberFormat.getInstance(Locale.US);
    nf.setMaximumFractionDigits(4);
    nf.setGroupingUsed(false);

    {
      File absolutFile = new File(linkEmissionAnalysisFile);
      File perMeterFile = new File(linkEmissionPerMAnalysisFile);

      BufferedWriter absolutWriter = new BufferedWriter(new FileWriter(absolutFile));
      BufferedWriter perMeterWriter = new BufferedWriter(new FileWriter(perMeterFile));

      absolutWriter.write("linkId");
      perMeterWriter.write("linkId");

      Map<Id<Link>, Map<Pollutant, Double>> link2pollutants = emissionsEventHandler.getLink2pollutants();

      for (Pollutant pollutant : Pollutant.values()) {
        absolutWriter.write(";" + pollutant);
        perMeterWriter.write(";" + pollutant + " [g/m]");
      }

      absolutWriter.newLine();
      perMeterWriter.newLine();

      for (Id<Link> linkId : link2pollutants.keySet()) {
        absolutWriter.write(linkId.toString());
        perMeterWriter.write(linkId.toString());

        for (Pollutant pollutant : Pollutant.values()) {
          double emissionValue = 0.;
          if (link2pollutants.get(linkId).get(pollutant) != null) {
            emissionValue = link2pollutants.get(linkId).get(pollutant);
          }
          absolutWriter.write(";" + nf.format(emissionValue));

          double emissionPerM = Double.NaN;
          Link link = scenario.getNetwork().getLinks().get(linkId);
          if (link != null) {
            emissionPerM = emissionValue / link.getLength();
          }
          perMeterWriter.write(";" + nf.format(emissionPerM));

        }
        absolutWriter.newLine();
        perMeterWriter.newLine();
      }

      absolutWriter.close();
      log.info("Output written to " + linkEmissionAnalysisFile);
      perMeterWriter.close();
      log.info("Output written to " + linkEmissionPerMAnalysisFile);
    }

  }

  /**
   * Schreibt die Zuordnung von Fzg-Ids und den zugeordnenten emissions-concepts raus.
   * 
   * @param vehicleTypeFileStr
   * @param scenario
   * @param emissionsEventHandler
   * @throws IOException
   */
  static void writeEmissionConceptAssignmentOutput(String vehicleTypeFileStr,
      Scenario scenario, EmissionsOnLinkEventHandler emissionsEventHandler)
      throws IOException {
    {
      //TODO: Das ist nur zuordnugn von Fzg zu Fzg-Typen. Das brauche ich aber gar nicht.
      //TODO: Gut bzw. wichtig wären eigentlich emissions pro Fzg / FzgTyp.
      log.info("Writing assignemnt of emissionConcepts to vehicles...");
      
      File vehicleTypeFile = new File(vehicleTypeFileStr);

      BufferedWriter vehicleTypeWriter = new BufferedWriter(new FileWriter(vehicleTypeFile));

      vehicleTypeWriter.write("vehicleId;vehicleType;emissionsConcept");
      vehicleTypeWriter.newLine();

      for (Vehicle vehicle : scenario.getVehicles().getVehicles().values()) {
        String emissionsConcept = "null";
        if (vehicle.getType().getEngineInformation() != null && VehicleUtils.getHbefaEmissionsConcept(vehicle.getType().getEngineInformation()) != null) {
          emissionsConcept = VehicleUtils.getHbefaEmissionsConcept(vehicle.getType().getEngineInformation());
        }

        vehicleTypeWriter.write(vehicle.getId() + ";" + vehicle.getType().getId().toString() + ";" + emissionsConcept);
        vehicleTypeWriter.newLine();
      }

      vehicleTypeWriter.close();
      log.info("Output written to " + vehicleTypeFileStr);
    }
  }
}
