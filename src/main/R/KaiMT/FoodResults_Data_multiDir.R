##Fasse ERgebnisse aus den verschiedenen Runs zusammen
## KMT Okt'24

####TODOS
# - Scenarien richtig benennen für Ausgabe in Paper/Diss
# - Filtern nach bestimmten Pollutants / Spalten, die dann ausgegeben werden sollen
# - Prüfen, dass das Umwandlung in Tabelle für LaTex gut klappt, ggf. Infos anpassen
# - ggf. Rundung der Werte
# -aufs Jahr hochrechnen???
# - Werte in kg hochrechnen
# - Werte Runden?

#### Erweiterungen FZG
# - für Anzahl Fahrzeuge nach Typ (wie schon in Ch9 der Diss)
# - für vkm travelled nach Typ (wie schon in Ch9 der Diss)
# -- Das muss dann Umrechnung nach ICEV und Fzg Größe enthalten. 
# - Dazu dann immer noch Summenspalte ICEV, BEV, Gesamt
# - Das dann mal abgleichen mit aktueller Variante in Diss


#### Erweiterungen W2w
# - basierend auf vkm je typ mit Faktoren je Fzg-Typ mutliplizieren
# - Aufs Jahr hochrechnen?
# - für die Strommixe anpassen als eigene cases.



# #setwd("C:/git-and-svn/shared-svn/projects/freight/studies/UpdateEventsfromEarlierStudies/foodRetailing_wo_rangeConstraint/71_ICEVBEV_NwCE_BVWP_10000it_DCoff_noTax/analysis")
# EFood <- FALSE
setwd("C:/git-and-svn/shared-svn/projects/freight/studies/UpdateEventsfromEarlierStudies/foodRetailing_with_rangeConstraint/")
EFood <- FALSE


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
library(tidyverse)
library(plotly)
library(gridExtra)
library(tibble)

# Hauptverzeichnis, in dem sich die Unterordner befinden
main_dir <- getwd()

# Pfad zum spezifischen Referenzordner
referenz_ordner <- "C:/git-and-svn/shared-svn/projects/freight/studies/UpdateEventsfromEarlierStudies/foodRetailing_wo_rangeConstraint/71a_ICEV_NwCE_BVWP_10000it_DCoff_noTax/"

# Liste der Unterordner im Hauptverzeichnis
subdirs <- list.dirs(main_dir, full.names = TRUE, recursive = FALSE)
subdirs <- subdirs[subdirs != referenz_ordner] #Referenzordner soll da nicht drin sein

# Initialisiere einen leeren Dataframe, um die kombinierten Daten zu speichern
kombinierte_daten <- data.frame()

# Lese die Referenzdaten aus dem spezifischen Referenzordner
file_path_referenz <- file.path(referenz_ordner, "Analysis", "1_emissions", "emissionsPerPollutant.csv")


# Überprüfe, ob die Referenzdatei existiert und lese sie ein
if (file.exists(file_path_referenz)) {
  referenzdaten_org <- read.csv(file_path_referenz, sep = ";")
  
  # Füge eine Spalte "Scenario" hinzu und setze den Namen des Referenzordners
  referenzdaten_org$Scenario <- basename(referenz_ordner)
  
  # Speichere die Referenzdaten als erste Zeile im kombinierten DataFrame
  kombinierte_daten <- referenzdaten_org
} else {
  stop(paste("Referenzdatei nicht gefunden in:", file_path_referenz))
}



# Durchlaufe alle Unterordner und lies all die Daten ein.
for (subdir in subdirs) {

  # Erstelle den genauen Pfad zur gewünschten CSV-Datei
  file_path_emissions <- file.path(subdir, "Analysis", "1_emissions", "emissionsPerPollutant.csv")
  
  
  ## Emissions einlesen
  if (file.exists(file_path_emissions)) {
    df_emissions_org <- read.csv(file_path_emissions, sep = ";")
    
    df_emissions <- df_emissions_org # Erstmal nur eine Kopie davon in der dann gearbeitet wird.
    
    # Füge eine Spalte "Scenario" hinzu und weise ihr den Namen des aktuellen Unterordners zu
    df_emissions$Scenario <- basename(subdir)
    
    
    # Füge die Daten zum kombinierten Dataframe hinzu
    kombinierte_daten <- rbind(kombinierte_daten, df_emissions)
    
    
  } else { 
    message(paste("Datei nicht gefunden in:", subdir, file_path_emissions))
    }
  
} ## For Schleife

### Etwas aufräumen:
# Entferne die Spalte "Run"
kombinierte_daten <- kombinierte_daten[, !names(kombinierte_daten) %in% c("Run")]
# Bringe die Spalte "Scenario" an die erste Stelle
kombinierte_daten <- kombinierte_daten[, c("Scenario", setdiff(names(kombinierte_daten), "Scenario"))]

head(kombinierte_daten)


### Relative Änderungen berechnen.

# Erhalte die numerischen Spalten im kombinierten DataFrame
numerische_spalten <- sapply(kombinierte_daten, is.numeric)

# Extrahiere die Referenzdaten aus dem kombinierten DataFrame
referenzdaten <- kombinierte_daten[kombinierte_daten$Scenario == basename(referenz_ordner), numerische_spalten]

# Gehe durch alle Szenarien außer dem Referenzszenario und berechne die relative Änderung
for (i in which(kombinierte_daten$Scenario != basename(referenz_ordner))) {
  for (col in names(kombinierte_daten)[numerische_spalten]) {
    abs_wert <- as.numeric(kombinierte_daten[i, col])  # Sicherstellen, dass es numerisch ist
    ref_wert <- as.numeric(referenzdaten[[col]])        # Sicherstellen, dass es numerisch ist
    
    # Überprüfen, ob abs_wert und ref_wert numerisch sind
    if (is.na(abs_wert) || is.na(ref_wert)) {
      kombinierte_daten[i, col] <- "NA"
    } else if (ref_wert == 0) { # Bei Referenzwert 0 soll das speziell gehandelt werden
      kombinierte_daten[i, col] <- paste(abs_wert, " ( -- %)", sep = "")
    } else { # Alles gut, rechne und füge hinzu.
      rel_aenderung <- round(((abs_wert - ref_wert) / ref_wert) * 100, 1)
      kombinierte_daten[i, col] <- paste(abs_wert, " (", rel_aenderung, "%)", sep = "")
    }
  }
}



write.csv(kombinierte_daten, "kombinierte_daten_Emissions.csv", row.names = FALSE)
