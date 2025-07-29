##Fasse die Ergebnisse aus den verschiedenen Runs zusammen
## Anzahl Fzg und vkm
## KMT Okt'24

#### Erweiterungen FZG
# Ausgabe der km in csv-DAtei ist noch offen
# - Das dann mal abgleichen mit aktueller Variante in Diss

#### Erweiterungen W2w --> basierend auf der Grundlage hier
# - basierend auf vkm je typ mit Faktoren je Fzg-Typ mutliplizieren
# - Aufs Jahr hochrechnen?
# - für die Strommixe anpassen als eigene cases.


# #setwd("C:/git-and-svn/shared-svn/projects/freight/studies/UpdateEventsfromEarlierStudies/foodRetailing_wo_rangeConstraint/71_ICEVBEV_NwCE_BVWP_10000it_DCoff_noTax/analysis")
# EFood <- FALSE
#setwd("C:/git-and-svn/shared-svn/projects/freight/studies/UpdateEventsfromEarlierStudies/foodRetailing_with_rangeConstraint/")
setwd("C:/git-and-svn/shared-svn/projects/freight/studies/UpdateEventsfromEarlierStudies/foodRetailing_wo_rangeConstraint/")
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
library(tidyr)
library(ggplot2)

# Hauptverzeichnis, in dem sich die Unterordner befinden
main_dir <- getwd()

# Pfad zum spezifischen Referenzordner
referenz_ordner <- "C:/git-and-svn/shared-svn/projects/freight/studies/UpdateEventsfromEarlierStudies/foodRetailing_wo_rangeConstraint/71a_ICEV_NwCE_BVWP_10000it_DCoff_noTax"

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

  # Füge eine Spalte "caseLang" hinzu und setze den Namen des Referenzordners
  referenzdaten_org$caseLang <- basename(referenz_ordner)

  # Speichere die Referenzdaten als erste Zeile im kombinierten DataFrame
  kombinierte_daten <- referenzdaten_org
} else {
  stop(paste("Referenzdatei nicht gefunden in:", file_path_referenz))
}



# Durchlaufe alle Unterordner und lies all die Daten ein.
for (subdir in subdirs) {

  # Erstelle den genauen Pfad zur gewünschten CSV-Datei
  file_path_datei <- file.path(subdir, "Analysis", "TimeDistance_perVehicleType.tsv")

  ## Daten einlesen
  if (file.exists(file_path_datei) && file_path_datei != file_path_referenz) {
    df_vehTypes_org <- read_delim(file_path_datei, show_col_types = FALSE)

    df_vehTypes <- df_vehTypes_org # Erstmal nur eine Kopie davon in der dann gearbeitet wird.

    # Füge eine Spalte "caseLang" hinzu und weise ihr den Namen des aktuellen Unterordners zu
    df_vehTypes$caseLang <- basename(subdir)


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
# Bringe die Spalte "caseLang" an die erste Stelle
kombinierte_daten <- kombinierte_daten[, c("caseLang", setdiff(names(kombinierte_daten), "caseLang"))]

# Erstelle die neue Spalte "case": setze "Base", wenn es das Referenzszenario ist, sonst den Teil nach dem letzten Unterstrich
kombinierte_daten$case <- ifelse(
  kombinierte_daten$caseLang == basename(referenz_ordner),
  "Base",
  sub(".*_", "", kombinierte_daten$caseLang)  # Extrahiert den Teil nach dem letzten Unterstrich
)

# Bringe die Spalte "case" an die zweite Stelle
kombinierte_daten <- kombinierte_daten[, c("caseLang", "case", setdiff(names(kombinierte_daten), c("caseLang", "case")))]


head(kombinierte_daten)

# Group by case, SizeClass, and engineType, then summarize the number of vehicles
summarized_data_vehicles <- kombinierte_daten %>%
  group_by(case, sizeClass, engineType) %>%
  summarize(sumedValue = sum(nuOfVehicles, na.rm = TRUE)) %>%
  ungroup()

summarized_data_km <- kombinierte_daten %>%
  group_by(case, sizeClass, engineType) %>%
  summarize(sumedValue = sum(`SumOfTravelDistances[km]`, na.rm = TRUE)) %>%
  ungroup()

summarized_costs <- kombinierte_daten %>%
  group_by(case, sizeClass, engineType) %>%
  summarize(
    totalCostsEUR = sum(`totalCosts[EUR]`, na.rm = TRUE),
    fixedCostsEUR = sum(`fixedCosts[EUR]`, na.rm = TRUE),
    varCostsDistEUR = sum(`varCostsDist[EUR]`, na.rm = TRUE),
    varCostsTimeEUR = sum(`varCostsTime[EUR]`, na.rm = TRUE)
  ) %>%
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

# # Weil es mehere Spalten gibt die von Interesse sind muss es umgeformt werden: pivot_longer und dann zurück: pivot_wider
# reshape_and_calculate_sums_costs <- function(data) {
#   data_long <- data %>%
#     pivot_longer(
#       cols = c(totalCostsEUR, fixedCostsEUR, varCostsDistEUR, varCostsTimeEUR),
#       names_to = "costType",
#       values_to = "value"
#     ) %>%
#     unite("SizeClass_engineType", sizeClass, engineType, sep = "_") %>%
#     unite("costType_SizeClass_engineType", costType, SizeClass_engineType, sep = "_")
#
#   reshaped_data <- data_long %>%
#     pivot_wider(
#       names_from = costType_SizeClass_engineType,
#       values_from = value,
#       values_fill = 0
#     )
#
#   return(reshaped_data)
# }


reshaped_data_vehicles <- reshape_and_calculate_sums(summarized_data_vehicles)
reshaped_data_km <- reshape_and_calculate_sums(summarized_data_km)

costs_per_case <- summarized_costs %>%
  group_by(case) %>%
  summarize(
    totalCostsEUR = sum(totalCostsEUR, na.rm = TRUE),
    fixedCostsEUR = sum(fixedCostsEUR, na.rm = TRUE),
    varCostsDistEUR = sum(varCostsDistEUR, na.rm = TRUE),
    varCostsTimeEUR = sum(varCostsTimeEUR, na.rm = TRUE)
  ) #%>%
# pivot_longer(cols = c(totalCostsEUR, fixedCostsEUR, varCostsDistEUR, varCostsTimeEUR), names_to = "costType", values_to = "value")


# View the updated reshaped data
print(reshaped_data_vehicles)
print(reshaped_data_km)
print(costs_per_case)

# Round all numeric values in the reshaped_data_km data frame to whole numbers
reshaped_data_km <- reshaped_data_km %>%
  mutate(across(where(is.numeric), ~ round(.x, digits = 0)))

# Round all numeric values in the reshaped_data_costsdata frame to whole numbers
costs_per_case <- costs_per_case %>%
  mutate(across(where(is.numeric), ~ round(.x, digits = 0)))

#### Ausgabe:
# Define the desired order of cases
case_order <- c("Base", "noTax", "Tax25", "Tax50", "Tax100", "Tax150", "Tax200", "Tax250", "Tax300")

reorderRows <- function (data, order) {
  data <- data %>%
    mutate(case = factor(case, levels = order)) %>%
    arrange(case)
}

reshaped_data_vehicles <- reorderRows(reshaped_data_vehicles, case_order)
reshaped_data_km <- reorderRows(reshaped_data_km, case_order)
costs_per_case <- reorderRows(costs_per_case, case_order)


# Define the desired order of columns
column_order_types <- c("case", "7.5t_ICEV", "7.5t_BEV", "18t_ICEV", "18t_BEV", "26t_ICEV", "26t_BEV", "40t_ICEV", "40t_BEV", "ICEV_Total", "BEV_Total", "Total")
column_order_costs <- c("case", "fixedCostsEUR", "varCostsDistEUR", "varCostsTimeEUR", "totalCostsEUR")


#Funktion zum Schreiben des Outputs
write_output_table <- function(output_file, dataframe, column_order) {
  cat("Summen über die jeweiligen Szenarien \n\n", file = output_file, append = TRUE)
  write.table(
    dataframe %>% select(all_of(column_order)),
    file = output_file,
    sep = ";",
    row.names = FALSE,
    col.names = TRUE,
    append = TRUE,
    fileEncoding = "UTF-8"
  )
}

write_output_table("vehiclesPerType_Anzahl.csv", reshaped_data_vehicles, column_order_types)
write_output_table("vehiclesPerType_km.csv", reshaped_data_km, column_order_types)
write_output_table("costsPercase.csv", costs_per_case, column_order_costs )



#### Plot costs als Säulendiagramm
# Ins lange Format bringen (ohne totalCostsEUR)
costs_long <- costs_per_case %>%
  pivot_longer(
    cols = c(fixedCostsEUR, varCostsDistEUR, varCostsTimeEUR),
    names_to = "costType",
    values_to = "value"
  )

# Reihenfolge der Kostenarten für die Stapelung festlegen
costs_long$costType <- factor(
  costs_long$costType,
  # levels = c("fixedCostsEUR", "varCostsDistEUR", "varCostsTimeEUR")
  levels = c("varCostsTimeEUR", "varCostsDistEUR", "fixedCostsEUR")
)

# Schriftgrößen als Variablen definieren
axis_text_size <- 14
axis_title_size <- 14
legend_text_size <- 14
legend_title_size <- 14
plot_title_size <- 18


# Gestapeltes Säulendiagramm
p <- ggplot(costs_long, aes(x = case, y = value, fill = costType)) +
  geom_bar(stat = "identity") +
  labs(
    # title = "Daily costs per case",
    y = "costs (EUR)",
    x = "case") +
  scale_fill_manual(
    values = c("fixedCostsEUR" = "grey", "varCostsDistEUR" = "skyblue", "varCostsTimeEUR" = "orange"),
    labels = c("variable costs (time)", "variable costs (distance)", "fixed costs")
  ) +
  theme_minimal()+
  #Schriftgrößen der Achsen etc.
  theme(
    axis.text.x = element_text(size = axis_text_size),
    axis.text.y = element_text(size = axis_text_size),
    axis.title.x = element_text(size = axis_title_size),
    axis.title.y = element_text(size = axis_title_size),
    legend.text = element_text(size = legend_text_size),
    legend.title = element_text(size = legend_title_size),
    plot.title = element_text(size = plot_title_size, hjust = 0.5), # hjust =0.5 --> mittig
    legend.position = "bottom"
  )

#Anzeigen des Plots
p

# Speichere das Diagramm als PDF
ggsave("costs_per_case.pdf", plot = p, width = 8, height = 5, dpi = 300)

