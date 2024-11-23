install.packages(c("ggplot2", "sf", "dplyr", "ggmap", "ggspatial", "prettymapr", "raster"))
library(ggplot2)
library(sf)
library(dplyr)
install.packages("prettymapr")
library(ggspatial)

setwd("C:/Users/erica/Downloads/Mauttabelle_2024_11_17")

# Daten einlesen (ersetzen Sie 'path_to_your_file.csv' durch den tatsächlichen Dateipfad)
data <- read.csv("2024-11-17_Mauttabelle.CSV", sep = ";", dec = ".")
data$Breite_Von <- as.numeric(data$Breite_Von)
data$Laenge_Von <- as.numeric(data$Laenge_Von)
data$Breite_Nach <- as.numeric(data$Breite_Nach)
data$Laenge_Nach <- as.numeric(data$Laenge_Nach)
# Nur die ersten 3 Zeilen der Daten verwenden
data <- data[1:4, ]
# Überprüfen Sie die Koordinaten, um sicherzustellen, dass keine ungültigen Werte vorhanden sind
summary(data[, c("Breite_Von", "Laenge_Von", "Breite_Nach", "Laenge_Nach")])

# Geometrien erstellen (Fehlerbehandlung einbauen)
edges <- data %>%
  rowwise() %>%
  mutate(
    geometry = tryCatch(
      st_sfc(st_linestring(matrix(c(Laenge_Von, Breite_Von, Laenge_Nach, Breite_Nach),
                                  ncol = 2, byrow = TRUE))),
      error = function(e) NULL  # Fehlerabfangung, falls ungültige Koordinaten vorliegen
    )
  ) %>%
  filter(!is.null(geometry)) %>%  # Ungültige Zeilen herausfiltern
  st_as_sf(crs = 4326)  # WGS84-Koordinatensystem (EPSG:4326)

# Plotten mit OpenStreetMap-Hintergrund über ggspatial
ggplot() +
  annotation_map_tile(type = "osm", zoom = 10) +  # OpenStreetMap Hintergrund
  geom_sf(data = edges, aes(geometry = geometry), color = "blue", size = 1) +
  coord_sf(crs = 4326) +  # Sicherstellen, dass wir das WGS84 CRS verwenden
  labs(title = "Strassenabschnitte mit OSM-Hintergrund", x = "Laengengrad", y = "Breitengrad") +
  theme_minimal()

ggplot(data) +
  geom_point(aes(x = Laenge_Von, y = Breite_Von), color = "blue") +
  geom_point(aes(x = Laenge_Nach, y = Breite_Nach), color = "red") +
  labs(title = "Koordinatenueberpruefung: Start und Zielpunkte") +
  theme_minimal()
