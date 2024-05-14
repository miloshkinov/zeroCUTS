# Set working directory and load necessary packages (assuming it hasn't changed)
setwd("C:/Users/Ricardo/git/shared-svn/projects/freight/studies/WP51_EmissionsFood/output")

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

# Function to create a bar plot for vehicles
create_bar_plot_vehicles <- function(df_base, df_policy, title) {
  plot_ly() %>%
    add_bars(x = ~df_base$vehicleCategory, y = df_base$nuOfVehicles, name = 'Base Case', type = 'bar') %>%
    add_bars(x = ~df_policy$vehicleCategory, y = df_policy$nuOfVehicles, name = 'Policy Case', type = 'bar') %>%
    layout(xaxis = list(title = 'Vehicle Type'), yaxis = list(title = 'Number of Vehicles')) %>%
    layout(title = title)
}

# Function to create a bar plot for costs
create_bar_plot_costs <- function(df_base, df_policy, title) {
  plot_ly() %>%
    add_bars(x = ~df_base$vehicleCategory, y = df_base$totalCosts.EUR., name = 'Base Case', type = 'bar') %>%
    add_bars(x = ~df_policy$vehicleCategory, y = df_policy$totalCosts.EUR., name = 'Policy Case', type = 'bar') %>%
    layout(xaxis = list(title = 'Vehicle Type'), yaxis = list(title = 'Total Costs (EUR)')) %>%
    layout(title = title)
}

# Function to create a bar plot for distances
create_bar_plot_distances <- function(df_base, df_policy, title) {
  plot_ly() %>%
    add_bars(x = ~df_base$vehicleCategory, y = df_base$SumOfTravelDistances.km, name = 'Base Case', type = 'bar') %>%
    add_bars(x = ~df_policy$vehicleCategory, y = df_policy$SumOfTravelDistances.km, name = 'Policy Case', type = 'bar') %>%
    layout(xaxis = list(title = 'Vehicle Type'), yaxis = list(title = 'Traveles Distances (km)')) %>%
    layout(title = title)
}
#function to create categories
create_vehicle_categories <- function(data) {
  data <- data %>%
    mutate(vehicleCategory = ifelse(vehicleTypeId %in% c("light8t", "light8t_frozen"), yes = "light8t",
                                    no = ifelse(vehicleTypeId %in% c("light8t_electro_Mitsubishi", "light8t_electro_Quantron", "light8t_frozen_electro_Mitsubishi", "light8t_frozen_electro_Quantron"), yes = "light8t_electro",
                                                no = ifelse(vehicleTypeId %in% c("medium18t", "medium18t_frozen"), yes = "medium18t",
                                                            no = ifelse(vehicleTypeId %in% c("medium18t_electro_Renault", "medium18t_electro_Volvo"), yes = "medium18t_electro", 
                                                                        no = ifelse(vehicleTypeId %in% c("heavy26t", "heavy26t_frozen"), yes = "heavy26t", 
                                                                                    no = ifelse(vehicleTypeId %in% c("heavy26t_electro_Daimler", "heavy26t_electro_Renault", "heavy26t_frozen_electro_Daimler", "heavy26t_frozen_electro_Renault"), yes = "heavy26t_electro", 
                                                                                                no = ifelse(vehicleTypeId %in% c("heavy40t_electro_Daimler", "heavy40t_electro_Scania", "heavy40t_frozen_electro_Daimler", "heavy40t_frozen_electro_Scania"), yes = "heavy40t_electro", 
                                                                                                            no = ifelse(vehicleTypeId %in% c("heavy40t", "heavy40t_frozen"), yes = "heavy40t", 
                                                                                                                        no = as.character(vehicleTypeId))))))))))
  # Add a category for the sum of all vehicle types
  data <- data %>%
    mutate(sum = ifelse(vehicleCategory %in% c("light8t", "light8t_electro","medium18t", "medium18t_electro",
                                                           "heavy26t", "heavy26t_electro", "heavy40t", "heavy40t_electro"), 
                                    yes = "Sum", no = as.character(vehicleCategory)))
  
}
# Read data for the base case
df <- read.delim("Base_10000/Analysis/TimeDistance_perVehicleType.tsv")
df_tours <- read.delim("Base_10000/Analysis/TimeDistance_perVehicle.tsv")

# Read data for the policy case
df_policy <- read.delim("EV_only_10000/Analysis/TimeDistance_perVehicleType.tsv")
df_tours_policy <- read.delim("EV_only_10000/Analysis/TimeDistance_perVehicle.tsv")

#add categories
df <- create_vehicle_categories(df)
df_tours <- create_vehicle_categories(df_tours)
df_policy <- create_vehicle_categories(df_policy)
df_tours_policy <- create_vehicle_categories(df_tours_policy)

# Specify the desired order of vehicleTypeId
desired_order <- c("light8t", "light8t_electro","medium18t", "medium18t_electro",
                   "heavy26t", "heavy26t_electro", "heavy40t", "heavy40t_electro")

# Convert 'vehicleTypeId' to a factor with the desired order for both base and policy cases
df$vehicleCategory <- factor(df$vehicleCategory, levels = desired_order)
df_tours$vehicleCategory <- factor(df_tours$vehicleCategory, levels = desired_order)
df_policy$vehicleCategory <- factor(df_policy$vehicleCategory, levels = desired_order)
df_tours_policy$vehicleCategory <- factor(df_tours_policy$vehicleCategory, levels = desired_order)

# Ensure both plots have the same levels
common_levels <- intersect(levels(df$vehicleCategory), levels(df$vehicleCategory))

# Filter the data frame to include only common levels
#df <- df %>% filter(vehicleTypeId %in% common_levels)

# 1. Bar Plot for Number of Vehicles by Vehicle Type (Interactive)
combined_bar_plot_vehicles <- create_bar_plot_vehicles(df, df_policy, 'Number of Vehicles by Vehicle Type')


# 2. Bar Plot for Total Costs by Vehicle Type (Interactive)
combined_bar_plot_costs <- create_bar_plot_costs(df, df_policy, 'Total Costs by Vehicle Type')

# 3. Bar Plot for Travel Distacnes by Vehicle Type (Interactive)
combined_bar_plot_distances <- create_bar_plot_distances(df, df_policy, 'Total Traveled Distances by Vehicle Type')

# 4. Box Plot for Traveled Distances by Vehicle Type (Interactive)
box_plot_distances <- plot_ly(data = df_tours, x = ~vehicleCategory, y = ~travelDistance.km., 
                              type = 'box', boxpoints = "all", jitter = 0.0, pointpos = -0.0, colors = ) %>%
  layout(xaxis = list(title = 'Vehicle Type'), yaxis = list(title = 'Traveled Distances (km)'))
box_plot_distances_policy <- plot_ly(data = df_tours_policy, x = ~vehicleCategory, y = ~travelDistance.km., 
                              type = 'box', boxpoints = "all", jitter = 0.0, pointpos = -0.0) %>%
  layout(xaxis = list(title = 'Vehicle Type'), yaxis = list(title = 'Traveled Distances (km)'))

# Merge the base case and policy case data frames
combined_df_tours <- bind_rows(
  mutate(df_tours, Case = "Base"),
  mutate(df_tours_policy, Case = "Policy")
)

# Create a box plot for traveled distances by vehicle type for both cases
combined_box_plot_distances <- plot_ly(data = combined_df_tours, x = ~vehicleCategory, y = ~travelDistance.km., color = ~Case,
                                       type = 'box', boxpoints = "all", jitter = 0.0, pointpos = -0.0, colors = "Set1") %>%
  layout(xaxis = list(title = 'Vehicle Type'), yaxis = list(title = 'Traveled Distances (km)'),
         title = 'Box Plot of Traveled Distances by Vehicle Type - Base vs. Policy') %>%
  layout(font = list(size = 16))

print(combined_bar_plot_vehicles)
print(combined_bar_plot_costs)
print(combined_bar_plot_distances)
print(box_plot_distances %>% layout(title = 'Box Plot of Traveled Distances by Vehicle Type - Base'))
print(box_plot_distances_policy %>% layout(title = 'Box Plot of Traveled Distances by Vehicle Type - EV'))
print(combined_box_plot_distances)


# Create a box plot for traveled distances by vehicle type for both cases
combined_box_plot_distances <- plot_ly(data = combined_df_tours, x = ~vehicleCategory, y = ~travelDistance.km.,
                                       type = 'box', boxpoints = "all", jitter = 0.0, pointpos = -0.0) %>%
  add_boxplot(data = filter(combined_df_tours, Case == "Base"), color = I("Set2"), colo width = 0.5, name = "Base Case") %>%
  add_boxplot(data = filter(combined_df_tours, Case == "Policy"), color = I("Set1"), width = 0.5, name = "Policy Case") %>%
  layout(xaxis = list(title = 'Vehicle Type'), yaxis = list(title = 'Traveled Distances (km)'),
         title = 'Box Plot of Traveled Distances by Vehicle Type - Base vs. Policy')

# Display the combined box plot
print(combined_box_plot_distances)
