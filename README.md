# zeroCUTS - Wo liegt was?
Gitlab nutzen wir neben dem code auch für die Issues.
Dokumente und Input-Dateien werden in der TubCloud (schared/vsp-zeroCUTS) abgelegt.

Aus der Praxis heraus noch folgende Empfehlung, damit die Zugriffe auf die Inputdateien aus dem Code heraus bei allen gleich sind:
Die TubCloud sollte parallel zum git-Checkpout des Codes sein. Beispiel:
* ..../workspace (Ein belibieger Ordner auf der Festplatte, individuell verschieden)
    * /zeroCuts (git checkout dieses gitlab-repository mit dem Quellcode)
    * /tubcloud/shared/vsp-zerocuts (Projektverzeichnis aus tubcloud)

# Wir werden Events analysiert?
Ein guter Einstieg findet sich hier:
https://github.com/matsim-org/matsim-code-examples/tree/0.10.x/src/main/java/tutorial/events 
(Das gesamte Repository https://github.com/matsim-org/matsim-code-examples/ ist als Einstieg gedacht und kann daher auch gerne geclont, kopiert werden. Hier von Interesse für MPM sind v.a. die Events, weil es das ist, was VSP als Ergebnis der Verkehrssimultion liefert.)

------------

# Infos from matsim-example-project

A small example of how to use MATSim as a library.

By default, this project uses the latest (pre-)release. In order to use a different version, edit `pom.xml`.

A recommended directory structure is as follows:
* `src` for sources
* `original-input-data` for original input data (typically not in MATSim format) 
* `scenarios` for MATSim scenarios, i.e. MATSim input and output data.  A good way is the following:
  * One subdirectory for each scenario, e.g. `scenarios/mySpecialScenario01`.
  * This minimally contains a config file, a network file, and a population file.
  * Output goes one level down, e.g. `scenarios/mySpecialScenario01/output-from-a-good-run/...`.
  

Import into eclipse

    download a modern version of eclipse. This should have maven and git included by default.
    file->import->git->projects from git->clone URI and clone as specified above. It will go through a sequence of windows; it is important that you import as 'general project'.
    file->import->maven->existing maven projects

Sometimes, step 3 does not work, in particular after previously failed attempts. Sometimes, it is possible to right-click to configure->convert to maven project. If that fails, the best thing seems to remove all pieces of the failed attempt in the directory and start over.

Import into IntelliJ

... todo ...
