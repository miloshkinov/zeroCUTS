library(tidyverse)
library(plotly)

# CSV einlesen (Pfad ggf. anpassen)
setwd("C:/Users/erica/shared/matsim-hannover/output/")
run <- "smallScaleCommercialPlans"
run <- "smallScaleCommercialPlans_newApproach2_0.2"
run <- "smallScaleCommercialPlans_old_approach"
run <- "smallScaleCommercialPlans"
run <- "smallScaleCommercialPlans"
run <- "smallScaleCommercialPlans"
run <- "smallScaleCommercialPlans"
run <- "smallScaleCommercialPlans"

createPlots(run)

createPlots <- function(run) {

  df <- read.csv2(
    file.path(run, "analysis", "freight", "carrierFleetAnalysis.csv"),
    header = TRUE,
    sep = "\t",
    dec = "."
  )


  # Bins in Minuten
  bins <- c(0, 60, 120, 180, 240, 300, 360, 420, 480, 540, 600, 660, 720, 780, 840, Inf)

  # Umrechnung Sekunden -> Minuten
  df <- df %>%
    mutate(
      usedForTour = tolower(usedForTour) == "true",  # <- FIX
      max_min = maxTourDuration / 60,
      used_min = usedDuration / 60
    )
  df_long <- bind_rows(
    df %>%
      transmute(
        value = max_min,
        type = "maxTourDuration"
      ),
    df %>%
      filter(usedForTour) %>%
      transmute(
        value = used_min,
        type = "usedDuration"
      )
  )
  df_bins <- df_long %>%
    mutate(
      bin = cut(value, breaks = bins, right = FALSE)
    ) %>%
    count(bin, type)

  ### Anteil der genutzten Tourdauer an der maximalen Tourdauer
  df_util <- df %>%
    filter(usedForTour) %>%
    mutate(
      utilization = usedDuration / maxTourDuration
    ) %>%
    filter(utilization >= 0)

  ### Bins erstellen
  util_bins <- seq(0, 1, by = 0.1)
  df_util_bins <- df_util %>%
    mutate(
      bin = cut(utilization, breaks = util_bins, right = FALSE)
    ) %>%
    count(bin)

  ###Text block bauen
  stats <- df_util %>%
    summarise(
      mean_util = mean(utilization, na.rm = TRUE),
      median_util = median(utilization, na.rm = TRUE),
      share_over_80 = mean(utilization >= 0.8, na.rm = TRUE),
      n_tours = dplyr::n()
    )

  stats_label <- stats %>%
    transmute(
      label = paste0(
        "n = ", n_tours,
        "\nMean = ", sprintf("%.2f", mean_util),
        "\nMedian = ", sprintf("%.2f", median_util),
        "\n≥ 0.80: ", sprintf("%.1f%%", 100 * share_over_80)
      )
    )


  ggplot(df_bins, aes(x = bin, y = n, fill = type)) +
    geom_col(position = "dodge", width = 0.8) +
    labs(
      title = paste("Maximale vs. genutzte Tourdauer (", run, ")", sep = ""),
      x = "Dauer [Minuten]",
      y = "Anzahl Fahrzeuge",
      fill = ""
    ) +
    scale_fill_manual(
      values = c(
        "maxTourDuration" = "steelblue",
        "usedDuration" = "darkorange"
      ),
      labels = c(
        "maxTourDuration" = "Maximale Dauer",
        "usedDuration" = "Genutzte Dauer"
      )
    ) +
    theme_minimal() +
    theme(
      legend.position = "top",
      axis.text.x = element_text(angle = 45, hjust = 1)
    )

  print(
    ggplot(df_util_bins, aes(x = bin, y = n)) +
      geom_col(fill = "darkgreen", width = 0.8) +
      labs(
        title = paste("Ausnutzung der maximalen Tourdauer (", run, ")", sep = ""),
        x = "Anteil genutzte / maximale Tourdauer",
        y = "Anzahl Touren"
      ) +
      annotate(
        "label",
        x = 1, y = Inf,
        label = stats_label,
        hjust = 0, vjust = 1,
        size = 3.5
      ) +
      theme_minimal() +
      theme(
        axis.text.x = element_text(angle = 45, hjust = 1)
      )
  )
}
