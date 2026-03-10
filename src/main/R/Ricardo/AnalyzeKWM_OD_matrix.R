library(sf)
library(tidyverse)
library(ggplot2)
library(readr)
library(ggthemes)
library(dplyr)
library(glue)


setwd("C:/Users/erica/shared/")
zonen <- st_read("shared-svn/projects/rvr-metropole-ruhr/data/commercialTraffic/osm/zones_v2.0_25832.shp")
folder <- "commercial_10pct_0.005"
folder <- "commercial_100pct"
folder <- "commercial_0.1pct"
folder <- "commercial_10pct_3.0"
od_matrix_purpose1 <- read_delim(file.path("matsim-metropole-ruhr/scenarios/metropole-ruhr-v2024.0/output/rvr", folder,"smallScaleCommercial/calculatedData/odMatrix_commercialPersonTraffic_total_purpose1.csv"),
                                 delim = "\t",
                                 locale = locale(decimal_mark = "."))
od_matrix_purpose2 <- read_delim(file.path("matsim-metropole-ruhr/scenarios/metropole-ruhr-v2024.0/output/rvr", folder,"smallScaleCommercial/calculatedData/odMatrix_commercialPersonTraffic_total_purpose2.csv"),
                                 delim = "\t",
                                 locale = locale(decimal_mark = "."))
od_matrix_purpose3 <- read_delim(file.path("matsim-metropole-ruhr/scenarios/metropole-ruhr-v2024.0/output/rvr", folder,"smallScaleCommercial/calculatedData/odMatrix_commercialPersonTraffic_total_purpose3.csv"),
                    delim = "\t",
                    locale = locale(decimal_mark = "."))
od_matrix_purpose4 <- read_delim(file.path("matsim-metropole-ruhr/scenarios/metropole-ruhr-v2024.0/output/rvr", folder,"smallScaleCommercial/calculatedData/odMatrix_commercialPersonTraffic_total_purpose4.csv"),
                                 delim = "\t",
                                 locale = locale(decimal_mark = "."))
od_matrix_purpose5 <- read_delim(file.path("matsim-metropole-ruhr/scenarios/metropole-ruhr-v2024.0/output/rvr", folder,"smallScaleCommercial/calculatedData/odMatrix_commercialPersonTraffic_total_purpose5.csv"),
                                 delim = "\t",
                                 locale = locale(decimal_mark = "."))

# Funktion zur Umwandlung einer OD-Matrix ins Long-Format
# Funktion: OD-Matrix ins Long-Format bringen
pivot_od_long <- function(df) {
  df %>%
    pivot_longer(-`O/D`, names_to = "ziel", values_to = "anzahl") %>%
    rename(quell = `O/D`)
}

# OD-Matrizen einzeln transformieren
od_long1 <- pivot_od_long(od_matrix_purpose1)
od_long2 <- pivot_od_long(od_matrix_purpose2)
od_long3 <- pivot_od_long(od_matrix_purpose3)
od_long4 <- pivot_od_long(od_matrix_purpose4)
od_long5 <- pivot_od_long(od_matrix_purpose5)

# Zusammenführen aller OD-Daten
od_combined <- bind_rows(od_long1, od_long2, od_long3, od_long4, od_long5)
# Gruppieren und Umwandeln der Anzahl (in numerisch)
od_long <- od_combined %>%
  mutate(anzahl = as.numeric(anzahl)) %>%
  group_by(quell, ziel)

# Optional: Nur Einträge mit Bewegung (> 1)
# (Wird hier aber nicht weiterverwendet – evtl. löschen oder in die Analyse einbauen)
#od_long %>%
#  summarise(anzahl = sum(anzahl), .groups = "drop") %>%
#  filter(anzahl > 1)

# Geometrieattribut erweitern: Fläche, Schwerpunkt (x/y)
zonen <- zonen %>%
  mutate(
    flaeche_km2 = as.numeric(st_area(geometry)) / 1e6,
    centroid = st_centroid(geometry),
    x = st_coordinates(centroid)[, 1],
    y = st_coordinates(centroid)[, 2]
  )

# Tabelle mit Zonenzentren und Fläche zur Weiterverwendung (ohne Geometrie)
zonen_attribs <- zonen %>%
  st_drop_geometry() %>%
  select(schluessel, x, y, flaeche_km2)

# OD-Daten mit Koordinaten und Fläche verknüpfen + Entfernung berechnen
od_coords <- od_long %>%
  left_join(zonen_attribs, by = c("quell" = "schluessel")) %>%
  rename(x_start = x, y_start = y, flaeche_start = flaeche_km2) %>%
  left_join(zonen_attribs, by = c("ziel" = "schluessel")) %>%
  rename(x_end = x, y_end = y) %>%
  mutate(
    entfernung = sqrt((x_end - x_start)^2 + (y_end - y_start)^2) / 1000,
    # Innerhalb-Zonen-Relationen: Schätzradius als Distanz
    entfernung = if_else(
      quell == ziel & entfernung == 0,
      sqrt(flaeche_start / pi),  # Radius eines Kreises gleicher Fläche
      entfernung
    )
  )

# OD-Beziehungen pro Startzone zählen
anzahl_od_pro_quell <- od_coords %>%
  filter(!is.na(anzahl)) %>%
  group_by(quell) %>%
  summarise(anzahl_beziehungen = sum(anzahl), .groups = "drop")

# Durchschnittliche Entfernung (gewichtet) pro Startzone berechnen
mean_dist_per_zone <- od_coords %>%
  group_by(quell) %>%
  summarise(
    mean_dist = weighted.mean(entfernung, anzahl),
    .groups = "drop"
  ) %>%
  mutate(mean_dist = ifelse(is.nan(mean_dist), 0, mean_dist))

# Gesamtdurchschnitt über alle Zonen
gesamt_mean_dist <- od_coords %>%
  filter(!is.na(anzahl), anzahl > 0) %>%
  summarise(gesamt_mittelwert = weighted.mean(entfernung, anzahl)) %>%
  pull(gesamt_mittelwert)

# Zonen mit Metriken anreichern
zonen <- zonen %>%
  left_join(mean_dist_per_zone, by = c("schluessel" = "quell")) %>%
  left_join(anzahl_od_pro_quell, by = c("schluessel" = "quell")) %>%
  mutate(
    anzahl_beziehungen = replace_na(anzahl_beziehungen, 0),
    beziehungen_pro_km2 = anzahl_beziehungen / flaeche_km2
  )

# 1. Plot: Durchschnittliche Entfernung je Startzone
ggplot() +
  geom_sf(data = zonen, aes(fill = mean_dist), color = "white") +
  scale_fill_viridis_c(
    option = "plasma",
    na.value = "grey90",
    limits = c(0, 150),                      # Legendenbereich festlegen
    breaks = seq(0, 150, by = 20),           # Achseneinteilung
    oob = scales::squish                    # Extremwerte kappen
  ) +
  theme_map() +
  labs(
    title = paste("Ø Entfernung aller O/D-Beziehungen je Startzone (Run:", folder, ")"),
    subtitle = glue("Gesamtdurchschnitt: {round(gesamt_mean_dist, 1)} km"),
    fill = "Ø Entfernung (km)"
  )

# 2. Plot: OD-Beziehungen je km²
ggplot() +
  geom_sf(data = zonen, aes(fill = beziehungen_pro_km2), color = "white") +
  scale_fill_viridis_c(
    option = "cividis",
    na.value = "grey90",
    limits = c(0, 50),                      # Legendenbereich festlegen
    breaks = seq(0, 50, by = 20),           # Achseneinteilung
    oob = scales::squish                    # Extremwerte kappen
  ) +
  theme_map() +
  labs(
    title = paste("OD-Beziehungen je km² pro Startzone (Run:", folder, ")"),
    fill = "OD/km²"
  )


# Quellzone definieren (z. B. "51239")
startzone <- "051236"

# O/D-Daten nur für diese Startzone filtern
od_coords_filtered <- od_coords %>%
  filter(quell == startzone, anzahl > 0)

zone_mean_dist <- mean_dist_per_zone %>%
  filter(quell == startzone) %>%
  pull(mean_dist)

# Schritt 1: Zielzonen der ausgewählten Startzone extrahieren
zielzonen_keys <- od_coords_filtered %>%
  pull(ziel) %>%
  unique()

# Schritt 2: Zonendaten auf relevante Zielzonen (und ggf. Startzone) filtern
zonen_filtered <- zonen %>%
  filter(schluessel %in% c(startzone, zielzonen_keys))

# Plot nur für eine Quellzone
ggplot() +
  geom_sf(data = zonen_filtered, aes(fill = mean_dist), color = "white") +
  geom_segment(data = od_coords_filtered,
               aes(x = x_start, y = y_start,
                   xend = x_end, yend = y_end,
                   size = anzahl),
               alpha = 0.4, color = "darkred", lineend = "round") +
  scale_fill_viridis_c(option = "plasma", na.value = "grey90") +
  scale_size_continuous(range = c(0.1, 2), guide = "none") +
  theme_map() +
  labs(
    title = paste("O/D-Verbindungen von Startzone", startzone, " (Run:", folder, ")"),
    subtitle = glue("Gesamtdurchschnitt: {round(zone_mean_dist, 1)} km"),
    fill = "Ø Entfernung"
  )

