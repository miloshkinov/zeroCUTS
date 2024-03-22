setwd("C:/Users/Ricardo/git/shared-svn/projects/freight/studies/WP51_EmissionsFood/output/BaseCase_Food2024/Analysis")

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

# Read the TSV file
df <- read.delim("TimeDistance_perVehicleType.tsv")
df_tours <- read.delim("TimeDistance_perVehicle.tsv")

# Specify the desired order of vehicleTypeId
desired_order <- c("light8t", "light8t_frozen","medium18t", "medium18t_frozen","heavy26t", "heavy26t_frozen","heavy40t", "heavy40t_frozen")

# Convert 'vehicleTypeId' to a factor with the desired order
df$vehicleTypeId <- factor(df$vehicleTypeId, levels = desired_order)
df_tours$vehicleTypeId <- factor(df_tours$vehicleTypeId, levels = desired_order)

# Ensure both plots have the same levels
common_levels <- intersect(levels(df$vehicleTypeId), levels(df$vehicleTypeId))

# Filter the data frame to include only common levels
df <- df %>% filter(vehicleTypeId %in% common_levels)

# 1. Bar Plot for Number of Vehicles by Vehicle Type (Interactive)
bar_plot <- plot_ly(x = ~df$vehicleTypeId, y = df$nuOfVehicles, type = 'bar') %>%
  layout(xaxis = list(title = 'Vehicle Type'), yaxis = list(title = 'Number of Vehicles'))

# 3. Bar Plot for Total Costs by Vehicle Type (Interactive)
bar_plot_costs <- plot_ly(x = df$vehicleTypeId, y = df$totalCosts.EUR., type = 'bar', name = 'Total Costs') %>%
  layout(xaxis = list(title = 'Vehicle Type'), yaxis = list(title = 'Total Costs (EUR)'))

# 4. Box Plot for Traveled Distances by Vehicle Type (Interactive)
box_plot_distances <- plot_ly(data = df_tours, x = ~vehicleTypeId, y = ~travelDistance.km., 
                              type = 'box', boxpoints = "all", jitter = 0.0, pointpos = -0.0) %>%
  layout(xaxis = list(title = 'Vehicle Type'), yaxis = list(title = 'Traveled Distances (km)'))

# Display the plots separately
print(bar_plot %>% layout(title = 'Number of Vehicles by Vehicle Type'))
print(bar_plot_costs %>% layout(title = 'Total Costs by Vehicle Type'))
print(box_plot_distances %>% layout(title = 'Box Plot of Traveled Distances by Vehicle Type'))
