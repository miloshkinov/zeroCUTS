##Fasse die Ergebnisse aus den verschiedenen Runs zusammen
## Anzahl Fzg und vkm, und Kostenteile

#### Erweiterungen FZG
# Ausgabe der km in csv-DAtei ist noch offen
# - Das dann mal abgleichen mit aktueller Variante in Diss

#### Erweiterungen W2w --> basierend auf der Grundlage hier
# - basierend auf vkm je typ mit Faktoren je Fzg-Typ mutliplizieren
# - Aufs Jahr hochrechnen?
# - für die Strommixe anpassen als eigene cases.


setwd("C:/git-and-svn/tubcloud/kturner/Arbeit/50 ClusterRuns KMT/LSPBase/output/runLSP_Base/Carriers")


# Install and load necessary packages
if (!requireNamespace("tidyverse", quietly = TRUE)) {
  install.packages("tidyverse")
}
if (!requireNamespace("plotly", quietly = TRUE)) {
  install.packages("plotly")
}
if (!requireNamespace("gridExtra", quietly = TRUE)) {
  install.packages("gridExtra")
}
if (!requireNamespace("xtable", quietly = TRUE)) {
  install.packages("xtable")
}
library(tidyverse)
library(plotly)
library(gridExtra)
library(tibble)
library(tidyr)
library(ggplot2)
library(xtable)

# Hauptverzeichnis, in dem sich die Unterordner befinden
main_dir <- getwd()

# # Pfad zum spezifischen Referenzordner
# referenz_ordner <- "C:/git-and-svn/shared-svn/projects/freight/studies/UpdateEventsfromEarlierStudies/foodRetailing_wo_rangeConstraint/71a_ICEV_NwCE_BVWP_10000it_DCoff_noTax"

# Liste der Unterordner im Hauptverzeichnis
subdirs <- list.dirs(main_dir, full.names = TRUE, recursive = FALSE)
# subdirs <- subdirs[subdirs != referenz_ordner] #Referenzordner soll da nicht drin sein

# Initialisiere einen leeren Dataframe, um die kombinierten Daten zu speichern
kombinierte_daten <- data.frame()


# Durchlaufe alle Unterordner und lies all die Daten ein.
for (subdir in subdirs) {

  # Erstelle den genauen Pfad zur gewünschten CSV-Datei
  file_path_datei <- file.path(subdir, "analysis", "freight", "TimeDistance_perCarrier.tsv")

  ## Daten einlesen
  if (file.exists(file_path_datei)) {
    df_carriers_org <- read_delim(file_path_datei, show_col_types = FALSE)
    df_carriers <- df_carriers_org # Erstmal nur eine Kopie davon in der dann gearbeitet wird.

    # Füge eine Spalte "caseLang" hinzu und weise ihr den Namen des aktuellen Unterordners zu
    df_carriers$caseLang <- basename(subdir)

    # Füge die Daten zum kombinierten Dataframe hinzu
    kombinierte_daten <- rbind(kombinierte_daten, df_carriers)
  } else {
    message(paste("Datei nicht gefunden in:", subdir, file_path_datei))
  }

} ## For Schleife

# Entferne Leerzeichen am Anfang und Ende der Spaltennamen (scheint hier u.a. bei den vehiclTypeId so gewesen zu sein)
names(kombinierte_daten) <- trimws(names(kombinierte_daten))

### Etwas aufräumen:

# Bringe die Spalte "caseLang" an die erste Stelle
kombinierte_daten <- kombinierte_daten[, c("caseLang", setdiff(names(kombinierte_daten), "caseLang"))]

# Erstelle die neue Spalte "case": setze "Base", wenn es das Referenzszenario ist, sonst den Teil nach dem letzten Unterstrich
kombinierte_daten$case <- sub(".*_jsprit", "", kombinierte_daten$caseLang)  # Extrahiert den Teil nach dem jsprit

# Bringe die Spalte "case" an die zweite Stelle
kombinierte_daten <- kombinierte_daten[, c("caseLang", "case", setdiff(names(kombinierte_daten), c("caseLang", "case")))]

head(kombinierte_daten)

# Beispiel: Tabelle aufbereiten
auswertung <- kombinierte_daten %>%
  select(carrierId,
          case,
          nuOfVehicles = nuOfTours,
         `travelDistances[km]`,
         `travelTimes[h]`,
         `fixedCosts[EUR]`,
         `varCostsTime[EUR]`,
         `varCostsDist[EUR]`,
         `totalCosts[EUR]`,
  )

#Umformatieren
auswertung <- auswertung %>%mutate(
  nuOfVehicles = as.integer(round(nuOfVehicles, 0)),
  `travelDistances[km]` = as.integer(round(`travelDistances[km]`, 0)),
  `travelTimes[h]` = round(`travelTimes[h]`, 2),
  across(ends_with("[EUR]"),  ~ as.integer(round(.x, 0))),
  caseNum = as.numeric(gsub("[^0-9]", "", case))  # extrahiert Zahl aus "case" und speichert es als Zahl in Hilfsspalte caseNum ab.
) %>%
  arrange(carrierId, caseNum) %>% #Sortiert nach Carrier und caseNum
  select(-caseNum)   # Hilfsspalte wieder entfernen

### alle Nummern in \num{} einpacken
numify <- function(df) {
  df[] <- lapply(df, function(x) if(is.numeric(x)) paste0("\\num{", x, "}") else x)
  df
}

latex_table <- xtable(numify(auswertung))

print(latex_table, type="latex",include.rownames = FALSE, sanitize.text.function = identity)

