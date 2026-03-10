library(dplyr)
library(ggplot2)
library(readr)
library(sf)
library(lubridate)
library(stringr)
library(tidyr)

##readTrips
setwd("C:/Users/erica/shared/matsim-metropole-ruhr/")

# Liste der Simulationsläufe mit manuell zugeordnetem Widerstandswert
runs <- list(
  list(name = "commercial_10pct_0.005", resistance = 0.005),
  list(name = "commercial_10pct_testing_0.05_networkRoutes", resistance = 0.05),
  list(name = "commercial_10pct_testing_0.10_networkRoutes", resistance = 0.10),
  list(name = "commercial_10pct_testing_0.15_networkRoutes", resistance = 0.15),
  list(name = "commercial_10pct_testing_0.20_networkRoutes", resistance = 0.20),
  list(name = "commercial_10pct_testing_0.25_networkRoutes", resistance = 0.25),
  list(name = "commercial_10pct_testing_0.30_networkRoutes", resistance = 0.30),
  list(name = "commercial_10pct_testing_0.35_networkRoutes", resistance = 0.35),
  list(name = "commercial_10pct_testing_0.40_networkRoutes", resistance = 0.40),
  list(name = "commercial_10pct_testing_0.45_networkRoutes", resistance = 0.45),
  list(name = "commercial_10pct_testing_0.50_networkRoutes", resistance = 0.50),
  list(name = "commercial_10pct_testing_0.60_networkRoutes", resistance = 0.60),
  list(name = "commercial_10pct_testing_0.70_networkRoutes", resistance = 0.70),
  list(name = "commercial_10pct_testing_0.80_networkRoutes", resistance = 0.80),
  list(name = "commercial_10pct_testing_0.90_networkRoutes", resistance = 0.90),
  list(name = "commercial_10pct_testing_1.00_networkRoutes", resistance = 1.00)
)

# Funktion zur Extraktion pro Subpopulation
extract_metrics_per_subpop <- function(run) {
  folder <- paste0("scenarios/metropole-ruhr-v2024.0/output/rvr/", run$name, "/commercialTraffic_Run10pct/commercialTraffic_Run10pct.output_trips.csv.gz")
  trips <- read.csv2(folder)

  trips <- trips %>%
    filter(main_mode %in% c("car", "truck8t", "truck18t", "truck26t", "truck40t")) %>%
    mutate(
      subpopulation = case_when(
        str_detect(person, "commercialPersonTraffic") ~ "Personenwirtschaftsverkehr",
        str_detect(person, "goodsTraffic") ~ "kleinraeumiger WV",
        str_detect(person, "freight|GoodsType") ~ "Güterverkehr",
        str_detect(person, "ParcelDelivery") ~ "KEP",
        str_detect(person, "WasteCollection") ~ "Müllsammlung",
        TRUE ~ "Sonstiger"
      ),
      traveled_distance = as.numeric(gsub(",", ".", traveled_distance)),
      travel_time = as.numeric(lubridate::period_to_seconds(lubridate::hms(trav_time))) / 60  # Minuten
    )

  # Tourdaten je Person
  tour_data <- trips %>%
    group_by(person, subpopulation) %>%
    summarise(
      tour_distance_km = sum(traveled_distance, na.rm = TRUE) / 1000,
      tour_duration_min = sum(travel_time, na.rm = TRUE)
    ) %>%
    ungroup()

  # Kennzahlen pro Subpopulation
  trips %>%
    group_by(subpopulation) %>%
    summarise(
      num_agents = n_distinct(person),
      total_distance_km = sum(traveled_distance, na.rm = TRUE) / 1000,
      avg_trip_distance_km = mean(traveled_distance, na.rm = TRUE) / 1000
    ) %>%
    left_join(
      tour_data %>%
        group_by(subpopulation) %>%
        summarise(
          avg_tour_distance_km = mean(tour_distance_km, na.rm = TRUE),
          avg_tour_duration_min = mean(tour_duration_min, na.rm = TRUE)
        ),
      by = "subpopulation"
    ) %>%
    mutate(resistance = run$resistance)
}

# Daten für alle Runs sammeln
results_subpop <- bind_rows(lapply(runs, extract_metrics_per_subpop))

# Funktion zur Erstellung der Vergleichsplots
plot_metric_by_subpop <- function(data, y_var, y_label) {
  ggplot(data, aes(x = resistance, y = .data[[y_var]], color = subpopulation)) +
    geom_line() +
    geom_point(size = 2) +
    labs(
      title = y_label,
      x = "Widerstandswert",
      y = y_label,
      color = "Subpopulation"
    ) +
    theme_minimal()
}

# Plots generieren
plot_metric_by_subpop(results_subpop, "num_agents", "Anzahl der Agenten")
plot_metric_by_subpop(results_subpop, "total_distance_km", "Gesamte Distanz (km)")
plot_metric_by_subpop(results_subpop, "avg_trip_distance_km", "Durchschnittliche Triplänge (km)")
plot_metric_by_subpop(results_subpop, "avg_tour_distance_km", "Durchschnittliche Tourlänge (km)")
plot_metric_by_subpop(results_subpop, "avg_tour_duration_min", "Durchschnittliche Tourdauer (Minuten)")