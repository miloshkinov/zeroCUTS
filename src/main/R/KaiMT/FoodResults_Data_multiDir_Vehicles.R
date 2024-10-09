##Fasse die Ergebnisse aus den verschiedenen Runs zusammen
## Anzahl Fzg und vkm
## KMT Okt'24

#### Erweiterungen FZG
# - für Anzahl Fahrzeuge nach Typ (wie schon in Ch9 der Diss)
# - für vkm travelled nach Typ (wie schon in Ch9 der Diss)
# - Dazu dann immer noch Summenspalte ICEV, BEV, Gesamt
# - Das dann mal abgleichen mit aktueller Variante in Diss

#### Erweiterungen W2w --> basierend auf der Grundlage hier
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
file_path_referenz <- file.path(referenz_ordner, "Analysis", "TimeDistance_perVehicleType.tsv")


# Überprüfe, ob die Referenzdatei existiert und lese sie ein
if (file.exists(file_path_referenz)) {
  referenzdaten_org <- read_delim(file_path_referenz, show_col_types = FALSE)
  
  # Füge eine Spalte "ScenarioLang" hinzu und setze den Namen des Referenzordners
  referenzdaten_org$ScenarioLang <- basename(referenz_ordner)
  
  # Speichere die Referenzdaten als erste Zeile im kombinierten DataFrame
  kombinierte_daten <- referenzdaten_org
} else {
  stop(paste("Referenzdatei nicht gefunden in:", file_path_referenz))
}



# Durchlaufe alle Unterordner und lies all die Daten ein.
for (subdir in subdirs) {

  # Erstelle den genauen Pfad zur gewünschten CSV-Datei
  file_path_datei <- file.path(subdir, "Analysis", "TimeDistance_perVehicleType.tsv")
  
  
  ## Emissions einlesen
  if (file.exists(file_path_datei)) {
    df_vehTypes_org <- read_delim(file_path_datei, show_col_types = FALSE)
    
    df_vehTypes <- df_vehTypes_org # Erstmal nur eine Kopie davon in der dann gearbeitet wird.
    
    # Füge eine Spalte "ScenarioLang" hinzu und weise ihr den Namen des aktuellen Unterordners zu
    df_vehTypes$ScenarioLang <- basename(subdir)
    
    
    # Füge die Daten zum kombinierten Dataframe hinzu
    kombinierte_daten <- rbind(kombinierte_daten, df_vehTypes)
    
    
  } else { 
    message(paste("Datei nicht gefunden in:", subdir, file_path_datei))
    }
  
} ## For Schleife

# Entferne Leerzeichen am Anfang und Ende der Spaltennamen (scheint hier u.a. bei den vehiclTypeId so gewesen zu sein)
names(kombinierte_daten) <- trimws(names(kombinierte_daten))

#function to create categories
create_vehicle_categories <- function(data) {
  data <- data %>% 
    mutate(vehicleCategory = ifelse(vehicleTypeId %in% c("light8t", "light8t_frozen"), yes = "7.5t",
                                    no = ifelse(vehicleTypeId %in% c("light8t_electro", "light8t_frozen_electro", "light8t_electro_large_Quantron", "light8t_frozen_electro_large_Quantron", "light8t_electro_small_Mitsubishi", "light8t_frozen_electro_small_Mitsubishi"), yes = "7.5t_electro",
                                                no = ifelse(vehicleTypeId %in% c("medium18t", "medium18t_frozen"), yes = "18t",
                                                            no = ifelse(vehicleTypeId %in% c("medium18t_electro", "medium18t_electro", "medium18t_electro_large_Volvo", "medium18t_electro_small_Renault"), yes = "18t_electro",
                                                                        no = ifelse(vehicleTypeId %in% c("heavy26t", "heavy26t_frozen"), yes = "26t",
                                                                                    no = ifelse(vehicleTypeId %in% c("heavy26t_electro",  "heavy26t_frozen_electro", "heavy26t_electro_large_Daimler", "heavy26t_frozen_electro_large_Daimler", "heavy26t_electro_small_Volvo", "heavy26t_frozen_electro_small_Volvo"), yes = "26t_electro",
                                                                                                no = ifelse(vehicleTypeId %in% c("heavy40t", "heavy40t_frozen"), yes = "40t",
                                                                                                            no = ifelse(vehicleTypeId %in% c("heavy40t_electro", "heavy40t_frozen_electro", "heavy40t_electro_large_Scania", "heavy40t_electro_small_Daimler"), yes = "40t_electro",
                                                                                                                        no = as.character(vehicleTypeId))))))))))
}

#Nur die Fahrzeuggröße -> d.h. egal ob elektro oder nicht. 
create_vehicle_sizeClass <- function(data) {
  data <- data %>% 
    mutate(sizeClass = sub("_.*", "", vehicleCategory))

}

### Finde raus, ob es ein BEV oder ICEV ist.
create_vehicle_engineType <- function(data) {
  data <- data %>% 
    mutate(engineType = ifelse(is.na(vehicleCategory) | vehicleCategory == "", 
                               NA,  # Falls vehicleCategory leer oder NA ist, setze den Wert auf NA
                               ifelse(grepl("_electro", vehicleCategory), "BEV", "ICEV"))) # Wenn "_electro" enthalten, BEV, sonst ICEV.
  
}

kombinierte_daten <- create_vehicle_categories(kombinierte_daten)
kombinierte_daten <- create_vehicle_sizeClass(kombinierte_daten)
kombinierte_daten <- create_vehicle_engineType(kombinierte_daten)

### Etwas aufräumen:

# Entferne die Spalte "Run"
kombinierte_daten <- kombinierte_daten[, !names(kombinierte_daten) %in% c("Run")]
# Bringe die Spalte "ScenarioLang" an die erste Stelle
kombinierte_daten <- kombinierte_daten[, c("ScenarioLang", setdiff(names(kombinierte_daten), "ScenarioLang"))]

# Erstelle die neue Spalte "Scenario": setze "Base Case", wenn es das Referenzszenario ist, sonst den Teil nach dem letzten Unterstrich
kombinierte_daten$Scenario <- ifelse(
  kombinierte_daten$ScenarioLang == basename(referenz_ordner), 
  "Base Case", 
  sub(".*_", "", kombinierte_daten$ScenarioLang)  # Extrahiert den Teil nach dem letzten Unterstrich
)

# Bringe die Spalte "Scenario" an die zweite Stelle
kombinierte_daten <- kombinierte_daten[, c("ScenarioLang", "Scenario", setdiff(names(kombinierte_daten), c("ScenarioLang", "Scenario")))]


head(kombinierte_daten)

# Group by Scenario, SizeClass, and engineType, then summarize the number of vehicles
summarized_data_vehicles <- kombinierte_daten %>%
  group_by(Scenario, sizeClass, engineType) %>%
  summarize(sumedValue = sum(nuOfVehicles, na.rm = TRUE)) %>%
  ungroup()

summarized_data_km <- kombinierte_daten %>%
  group_by(Scenario, sizeClass, engineType) %>%
  summarize(sumedValue = sum(`SumOfTravelDistances[km]`, na.rm = TRUE)) %>%
  ungroup()

# Function to reshape data and calculate sums
reshape_and_calculate_sums <- function(data) {
  reshaped_data <- data %>%
    unite("SizeClass_engineType", sizeClass, engineType, sep = "_") %>%
    spread(key = SizeClass_engineType, value = sumedValue, fill = 0) %>%
    mutate(
      BEV_Total = rowSums(select(., contains("_BEV")), na.rm = TRUE),
      ICEV_Total = rowSums(select(., contains("_ICEV")), na.rm = TRUE),
      Total = BEV_Total + ICEV_Total
    )
  return(reshaped_data)
}


reshaped_data_vehicles <- reshape_and_calculate_sums(summarized_data_vehicles)
reshaped_data_km <- reshape_and_calculate_sums(summarized_data_km)

# View the updated reshaped data
print(reshaped_data_vehicles)
print(reshaped_data_km)


#### Ausgabe:
# Define the desired order of scenarios
scenario_order <- c("Base Case", "noTax", "Tax25", "Tax50", "Tax100", "Tax150", "Tax200", "Tax250", "Tax300")

# Reorder the rows based on the scenario order
reshaped_data_vehicles <- reshaped_data_vehicles %>%
  mutate(Scenario = factor(Scenario, levels = scenario_order)) %>%
  arrange(Scenario)

# Define the desired order of columns
column_order <- c("Scenario", "7.5t_ICEV", "7.5t_BEV", "18t_ICEV", "18t_BEV", "26t_ICEV", "26t_BEV", "40t_ICEV", "40t_BEV", "ICEV_Total", "BEV_Total", "Total")

#Funktion zum Schreiben des Outputs
write_output_kmt <- function(output_file, dataframe) {
  cat("Summen über die jeweiligen Szenarien \n\n", file = output_file, append = TRUE)
  write.table(dataframe %>% select(all_of(column_order)), file = output_file, sep = ";", row.names = FALSE, col.names = TRUE, append = TRUE)
}

write_output_kmt("vehiclesPerType_Anzahl.csv", reshaped_data_vehicles)

# Reorder the columns based on the column order
reshaped_data_vehicles <- reshaped_data_vehicles %>%
  select(all_of(column_order))

# Write the reordered data to a CSV file
write.csv(reshaped_data_vehicles, "output.csv", row.names = FALSE)

