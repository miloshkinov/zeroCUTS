# Benötigtes Paket laden
library(ggplot2)
library(dplyr)
library(scales)
library(tidyverse)
library(lubridate)
library(patchwork)


# Datei einlesen – passe den Pfad ggf. an
data <- read.csv2("C:/Users/erica/shared/matsim-metropole-ruhr/scenarios/metropole-ruhr-v2024.0/output/rvr/commercial_100pct/commercialTraffic_Run100pct/analysis/traffic/commercialTraffic_Run100pct.travelDistances_perVehicle.csv", stringsAsFactors = FALSE, fileEncoding = "UTF-8")

runFolderName <- "commercial_10pct_0.005"
data_fleet <- read.csv2(paste0("C:/Users/erica/shared/matsim-metropole-ruhr/scenarios/metropole-ruhr-v2024.0/output/rvr/", runFolderName, "/smallScaleCommercial/analysis/freight/carrierFleetAnalysis.csv"), stringsAsFactors = FALSE, fileEncoding = "UTF-8")
# Numerische Spalten umwandeln
data$distanceInKm <- as.numeric(data$distanceInKm)
data$shareOfTravelDistanceWithDepotCharging <- as.numeric(data$shareOfTravelDistanceWithDepotCharging)


data_fleet <- data_fleet %>%
  mutate(
    maxTourDuration = as.numeric(maxTourDuration)/ 60,  # in Minuten
    usedForTour = tolower(usedForTour) == "true"  # als logisch
  )

# Fahrzeugtypen zusammenfassen
data$vehicleType <- ifelse(data$vehicleType %in% c("truck40t", "heavy40t"),
                           "40-Tonner", data$vehicleType)
data$vehicleType <- ifelse(data$vehicleType %in% c("medium18t", "medium18t_parcel"),
                           "18-Tonner", data$vehicleType)
data$vehicleType <- ifelse(data$vehicleType %in% c("mercedes313", "mercedes313_parcel"),
                           "Sprinter", data$vehicleType)
data$vehicleType <- ifelse(data$vehicleType %in% c("vwCaddy"),
                           "Kleintransporter (z.B. VW Caddy)", data$vehicleType)
data$vehicleType <- ifelse(data$vehicleType %in% c("golf1.4"),
                           "PKW", data$vehicleType)
data$vehicleType <- ifelse(data$vehicleType %in% c("light8t"),
                           "8-Tonner", data$vehicleType)
data$vehicleType <- ifelse(data$vehicleType %in% c("waste_collection_diesel"),
                           "Abfallsammelfahrzeuge", data$vehicleType)
data$vehicleType <- as.factor(data$vehicleType)

# data$subpopulation <- ifelse(data$subpopulation %in% c("commercialPersonTraffic", "commercialPersonTraffic_service"),
#                              "Personenwirtschaftsverkehr", data$subpopulation)
# data$subpopulation <- ifelse(data$subpopulation %in% c("FTL_kv_trip", "FTL_trip"),
#                                 "FTL", data$subpopulation)
# data$subpopulation <- ifelse(data$subpopulation %in% c("goodsTraffic"),
#                                 "Kleinräumiger Güterverkehr", data$subpopulation)
# data$subpopulation <- ifelse(data$subpopulation %in% c("LTL_trip"),
#                                 "LTL", data$subpopulation)
# data$subpopulation <- ifelse(data$subpopulation %in% c("longDistanceFreight"),
#                                 "Transit-Güterverkehr", data$subpopulation)
# data$subpopulation <- as.factor(data$subpopulation)

data$subpopulation <- ifelse(data$subpopulation %in% c("commercialPersonTraffic", "commercialPersonTraffic_service"),
                             "Commercial Person traffic", data$subpopulation)
data$subpopulation <- ifelse(data$subpopulation %in% c("FTL_kv_trip", "FTL_trip"),
                             "FTL", data$subpopulation)
data$subpopulation <- ifelse(data$subpopulation %in% c("goodsTraffic"),
                             "Small-Scale-Freight-Traffic", data$subpopulation)
data$subpopulation <- ifelse(data$subpopulation %in% c("LTL_trip"),
                             "LTL", data$subpopulation)
data$subpopulation <- ifelse(data$subpopulation %in% c("longDistanceFreight"),
                             "Transit Freight Traffic", data$subpopulation)
data$subpopulation <- as.factor(data$subpopulation)

# Berechnung für gesamte Flotte
anteil_alle <- data %>%
  summarise(
    anteil = mean(shareOfTravelDistanceWithDepotCharging <= 1, na.rm = TRUE)
  ) %>%
  mutate(label = paste0(round(anteil * 100, 1), "%"))


# Anteil ≤ 100 % pro Fahrzeugtyp berechnen (vehicleType)
anteil_df <- data %>%
  group_by(vehicleType) %>%
  summarise(
    anteil = mean(shareOfTravelDistanceWithDepotCharging <= 1, na.rm = TRUE),
    .groups = "drop"
  ) %>%
  mutate(label = paste0(round(anteil * 100, 1), "%"))

# Anteil ≤ 80 % pro Fahrzeugtyp (vehicleType)
anteil80_df <- data %>%
  group_by(vehicleType) %>%
  summarise(
    anteil = mean(shareOfTravelDistanceWithDepotCharging <= 0.8, na.rm = TRUE),
    .groups = "drop"
  ) %>%
  mutate(label = paste0(round(anteil * 100, 1), "%"))

# Anteil ≤ 100 % pro Fahrzeugtyp berechnen (subpopulations)
anteil_df_subpopulation <- data %>%
  group_by(subpopulation) %>%
  summarise(
    anteil = mean(shareOfTravelDistanceWithDepotCharging <= 1, na.rm = TRUE),
    .groups = "drop"
  ) %>%
  mutate(label = paste0(round(anteil * 100, 1), "%"))

# Anteil ≤ 80 % pro Fahrzeugtyp (subpopulations)
anteil80_df_subpopulation <- data %>%
  group_by(subpopulation) %>%
  summarise(
    anteil = mean(shareOfTravelDistanceWithDepotCharging <= 0.8, na.rm = TRUE),
    .groups = "drop"
  ) %>%
  mutate(label = paste0(round(anteil * 100, 1), "%"))


# Anteil ≤ 80 % für gesamte Flotte
anteil80_alle <- data %>%
  summarise(
    anteil = mean(shareOfTravelDistanceWithDepotCharging <= 0.8, na.rm = TRUE)
  ) %>%
  mutate(label = paste0(round(anteil * 100, 1), "%"))

# Hilfs-DataFrame für vertikale Linien
linien_df <- data.frame(
  xintercept = c(0.8, 1.0),
  label = c("Share of vehicles for which 80% of the battery is sufficient", "Share of vehicles for which 100% of the battery is sufficient")
)

# ggplot(data, aes(x = shareOfTravelDistanceWithDepotCharging)) +
#   geom_density(fill = "skyblue", alpha = 0.4) +
#   geom_rug(sides = "b", alpha = 0.5) +
#   # 100%-Strich + Text
#   geom_vline(xintercept = 1.0, color = "red", linetype = "dashed", size = 1) +
#   geom_vline(data = linien_df, aes(xintercept = xintercept, color = label),
#              linetype = "dashed", size = 1, show.legend = TRUE) +
#   scale_color_manual(
#     name = "Limit values",
#     values = c("Share of vehicles for which 80% of the battery is sufficient" = "darkgreen", "Share of vehicles for which 80% of the battery is sufficient" = "red")
#   ) +
#   geom_text(
#     data = anteil_alle,
#     aes(x = 1.05, y = Inf, label = label),
#     inherit.aes = FALSE,
#     hjust = 0, vjust = 2.2,
#     color = "red", size = 3.5
#   ) +
#   # 80%-Strich + Text
#   geom_vline(xintercept = 0.8, color = "darkgreen", linetype = "dashed", size = 1) +
#   geom_text(
#     data = anteil80_alle,
#     aes(x = 0.35, y = Inf, label = label),
#     inherit.aes = FALSE,
#     hjust = 0, vjust = 3.2,
#     color = "darkgreen", size = 3.5
#   ) +
#   scale_x_continuous(
#     labels = scales::percent_format(accuracy = 1),
#     limits = c(0, 7)
#   ) +
#   labs(
#     x = "Share of utilised range capacity",
#     y = "Density",
#     title = "Distance travelled in relation to the range of the electric vehicle without recharging"
#   ) +
#   theme_minimal() +
#   theme(legend.position = "bottom")


# Plot 2: Histogramm + Dichteverteilung
ggplot(data, aes(x = shareOfTravelDistanceWithDepotCharging)) +
  geom_histogram(aes(y = ..density..), bins = 75, fill = "lightblue", color = "white", alpha = 0.6) +
  geom_density(color = "darkblue", fill = "blue", alpha = 0.3) +
  geom_vline(xintercept = 1.0, color = "red", linetype = "dashed", size = 1) +
  geom_vline(data = linien_df, aes(xintercept = xintercept, color = label),
             linetype = "dashed", size = 1, show.legend = TRUE) +
  scale_color_manual(
    name = "Limit values",
    values = c("Share of vehicles for which 80% of the battery is sufficient" = "darkgreen", "Share of vehicles for which 100% of the battery is sufficient" = "red")
  ) +
  geom_text(
    data = anteil_alle,
    aes(x = 1.1, y = Inf, label = label),
    inherit.aes = FALSE,
    hjust = 0, vjust = 2.2,
    color = "red", size = 5.5
  ) +
  # 80%-Strich + Text
  geom_vline(xintercept = 0.8, color = "darkgreen", linetype = "dashed", size = 1) +
  geom_text(
    data = anteil80_alle,
    aes(x = 0.35, y = Inf, label = label),
    inherit.aes = FALSE,
    hjust = 0, vjust = 1.2,
    color = "darkgreen", size = 5.5
  ) +
  scale_x_continuous(
    labels = scales::percent_format(accuracy = 1),
    limits = c(0, 7)
  ) +
  labs(
    x = "Share of used range capacity [%]",
    y = "Density",
    title = "Distance travelled in relation to the range of the BEV without recharging"
  ) +
  theme_minimal() +
  theme(
    legend.position = "bottom",
    plot.title = element_text(size = 16, face = "bold"),        # Titelgröße
    axis.title = element_text(size = 14),                       # Achsentitel
    axis.text = element_text(size = 12),                        # Achsenbeschriftungen
    legend.title = element_text(size = 13),                     # Legendentitel
    legend.text = element_text(size = 12)                       # Legendentext
  )

# Plot 3: Verteilung der Depotladung nach Fahrzeugtyp
ggplot(data, aes(x = shareOfTravelDistanceWithDepotCharging)) +
  geom_histogram(aes(y = ..density..), bins = 30, fill = "lightblue", color = "white", alpha = 0.6) +
  geom_density(color = "darkblue", fill = "blue", alpha = 0.3) +
  geom_vline(xintercept = 1.0, color = "red", linetype = "dashed", size = 1) +
  geom_vline(data = linien_df, aes(xintercept = xintercept, color = label),
             linetype = "dashed", size = 1, show.legend = TRUE) +
  scale_color_manual(
    name = "Limit values",
    values = c("Share of vehicles for which 80% of the battery is sufficient" = "darkgreen", "Share of vehicles for which 100% of the battery is sufficient" = "red")
  ) +
  # Annotation pro vehicleType (nach rechts versetzt)
  geom_text(
    data = anteil_df,
    aes(x = 1.2, y = Inf, label = label),
    inherit.aes = FALSE,
    hjust = 0, vjust = 1.2,
    color = "red", size = 3.5
  ) +
  # 80%-Strich + Text
  geom_vline(xintercept = 0.8, color = "darkgreen", linetype = "dashed", size = 1) +
  geom_text(
    data = anteil80_df,
    aes(x = 1.2, y = Inf, label = label),
    inherit.aes = FALSE,
    hjust = 0, vjust = 3.2,
    color = "darkgreen", size = 3.5
  ) +
  facet_wrap(~ vehicleType, scales = "free_y") +
  scale_x_continuous(
    labels = scales::percent_format(accuracy = 1),
    limits = c(0, 7)
  ) +
  labs(
    x = "Anteil der genutzten Reichweitenkapazität",
    y = "Density",
    title = "Fahrstrecke im Verhältnis zur Reichweite des Elektrofahrzeugs ohne Nachladen (Unterteilung nach Fahrzeugtyp)"
  ) +
  theme_minimal() +
  theme(
    plot.margin = margin(10, 40, 10, 10),
    clip = "off"
  ) +
  theme(legend.position = "bottom")

# Plot 3-2: Verteilung der Depotladung nach Modell-Typ
ggplot(data, aes(x = shareOfTravelDistanceWithDepotCharging)) +
  geom_histogram(aes(y = ..density..), bins = 30, fill = "lightblue", color = "white", alpha = 0.6) +
  geom_density(color = "darkblue", fill = "blue", alpha = 0.3) +
  geom_vline(xintercept = 1.0, color = "red", linetype = "dashed", size = 1) +
  geom_vline(data = linien_df, aes(xintercept = xintercept, color = label),
             linetype = "dashed", size = 1, show.legend = TRUE) +
  scale_color_manual(
    name = "Limit values",
    values = c("Share of vehicles for which 80% of the battery is sufficient" = "darkgreen", "Share of vehicles for which 100% of the battery is sufficient" = "red")
  ) +
  # Annotation pro vehicleType (nach rechts versetzt)
  geom_text(
    data = anteil_df_subpopulation,
    aes(x = 1.2, y = Inf, label = label),
    inherit.aes = FALSE,
    hjust = 0, vjust = 1.2,
    color = "red", size = 3.5
  ) +
  # 80%-Strich + Text
  geom_vline(xintercept = 0.8, color = "darkgreen", linetype = "dashed", size = 1) +
  geom_text(
    data = anteil80_df_subpopulation,
    aes(x = 1.2, y = Inf, label = label),
    inherit.aes = FALSE,
    hjust = 0, vjust = 3.2,
    color = "darkgreen", size = 3.5
  ) +
  facet_wrap(~ subpopulation, scales = "free_y") +
  scale_x_continuous(
    labels = scales::percent_format(accuracy = 1),
    limits = c(0, 5)
  ) +
  labs(
    x = "Share of utilised range capacity",
    y = "Denity",
    title = "Distance travelled in relation to the range of the BEV without recharging (by model type)"
  ) +
  theme_minimal() +
  theme(
    plot.margin = margin(10, 40, 10, 10),
    clip = "off",
    legend.position = "bottom",
    plot.title = element_text(size = 16, face = "bold"),
    axis.title = element_text(size = 14),
    axis.text = element_text(size = 12),
    legend.title = element_text(size = 13),
    legend.text = element_text(size = 12),
    strip.text = element_text(size = 13, face = "bold")  # Facet-Überschrift
  )



# --- PLOT 4: Kumulative Häufigkeit (ECDF) nach vehicleType ---
plot4 <- ggplot(data, aes(x = shareOfTravelDistanceWithDepotCharging)) +
  stat_ecdf(geom = "step", color = "blue", size = 1) +
  facet_wrap(~ vehicleType, scales = "free_y") +
  geom_vline(xintercept = 1.0, color = "red", linetype = "dashed", size = 1) +
  geom_vline(xintercept = 0.8, color = "darkgreen", linetype = "dashed", size = 1) +
  geom_vline(data = linien_df, aes(xintercept = xintercept, color = label),
             linetype = "dashed", size = 1, show.legend = TRUE) +
  scale_color_manual(
    name = "Limit values",
    values = c("Share of vehicles for which 80% of the battery is sufficient" = "darkgreen", "Share of vehicles for which 100% of the battery is sufficient" = "red")
  ) +
  # Text bei 80 %
  geom_text(data = anteil80_df,
            aes(x = 0.82, y = anteil, label = label),
            inherit.aes = FALSE,
            hjust = 0, vjust = -0.5,
            color = "darkgreen", size = 3.5) +
  # Text bei 100 %
  geom_text(data = anteil_df,
            aes(x = 1.02, y = anteil, label = label),
            inherit.aes = FALSE,
            hjust = 0, vjust = -0.5,
            color = "red", size = 3.5) +
  scale_x_continuous(labels = percent_format(accuracy = 1), limits = c(0, 8)) +
  #scale_y_continuous(labels = percent_format(accuracy = 1)) +
  labs(
    x = "Depotladungsanteil",
    y = "Kumulative Verteilung"
  ) +
  theme_minimal() +
  theme(legend.position = "bottom")

print(plot4)


####Plots für Fleet Analysis
# Konfigurierbare Bins (z. B. in 30-Minuten-Schritten bis 10h)
duration_bins <- seq(0, 800, by = 30)

data_fleet %>%
  mutate(duration_bin = cut(maxTourDuration, breaks = duration_bins, include.lowest = TRUE)) %>%
  count(duration_bin) %>%
  mutate(percentage = n / sum(n) * 100) %>%
  ggplot(aes(x = duration_bin, y = percentage)) +
  geom_col(fill = "steelblue") +
  labs(
    title = paste("Verteilung der maxTourDuration (alle Fahrzeuge) (Run:", runFolderName, ")"),
    x = "maxTourDuration (Minuten, Bins)",
    y = "Anteil (%)"
  ) +
  theme_minimal() +
  theme(axis.text.x = element_text(angle = 45, hjust = 1))

data_fleet %>%
  filter(usedForTour) %>%
  mutate(duration_bin = cut(maxTourDuration, breaks = duration_bins, include.lowest = TRUE)) %>%
  count(duration_bin) %>%
  mutate(percentage = n / sum(n) * 100) %>%
  ggplot(aes(x = duration_bin, y = percentage)) +
  geom_col(fill = "forestgreen") +
  labs(
    title = paste("Verteilung der maxTourDuration (nur genutzte Fahrzeuge) (Run:", runFolderName, ")"),
    x = "maxTourDuration (Minuten, Bins)",
    y = "Anteil (%)"
  ) +
  theme_minimal() +
  theme(axis.text.x = element_text(angle = 45, hjust = 1))