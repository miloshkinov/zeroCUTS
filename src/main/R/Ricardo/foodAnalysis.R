# Install and load necessary packages
library(scatterplot3d)
library(plotly)
library(ggplot2)
library(reshape2)

# Set the working directory to the folder containing your simulation run folders
setwd("C:/Users/Ricardo/git/zerocuts/output/food/costsVariation_withDC_V2")

# List all folders in the testFolder directory
folders <- list.dirs(path = "C:/Users/Ricardo/git/zerocuts/output/food/costsVariation_withDC_V2", full.names = FALSE, recursive = FALSE)
path_base <- "C:/Users/Ricardo/git/zerocuts/output/food/costsVariation_withDC_V2_base"
folders_base <- list.dirs(path = "C:/Users/Ricardo/git/zerocuts/output/food/costsVariation_withDC_V2_base", full.names = FALSE, recursive = FALSE)
# Initialize an empty dataframe to store the data
plot_data <- data.frame()
plot_data_base <- data.frame()

# Define diesel prices for each year
diesel_prices <- c("2024" = 1.55, "2030" = 1.78, "2050" = 3.2)

# Define energy prices for each year (optimistic and pessimistic)
energy_prices <- list(
  "2024" = c("optimistic" = 0.18, "pessimistic" = 0.24),
  "2030" = c("optimistic" = 0.18, "pessimistic" = 0.21),
  "2050" = c("optimistic" = 0.18, "pessimistic" = 0.21)
)

# Iterate through each folder
for (folder in folders) {
  # Extract parameters from folder name using regular expressions
  params <- strsplit(folder, "_")[[1]]
  
  # Extracting parameters from folder name
  fuel <- as.numeric(gsub("fuel", "", params[2]))
  energy <- as.numeric(gsub("energy", "", params[3]))
  
  # Path to the analysis file
  analysis_file <- file.path(folder, "Analysis", "TimeDistance_perVehicleType.tsv")
  
  # Read the analysis file
  analysis_data <- read.table(analysis_file, header = TRUE, sep = "\t")
  
  # Calculate sum of totalCosts[EUR] column
  total_costs <- sum(analysis_data$totalCosts.EUR)

  total_kilometers <- sum(analysis_data$SumOfTravelDistances.km.)
  total_kilometers_electro <- sum(analysis_data$SumOfTravelDistances.km[grepl("_electro", analysis_data$vehicleTypeId)])
  total_kilometers_diesel <- sum(analysis_data$SumOfTravelDistances.km[!grepl("_electro", analysis_data$vehicleTypeId)])

  total_vehicle_diesel <- sum(analysis_data$nuOfVehicles [!grepl("_electro", analysis_data$vehicleTypeId)])
  total_vehicle_electro <- sum(analysis_data$nuOfVehicles [grepl("_electro", analysis_data$vehicleTypeId)])

  # Append parameters and results to the dataframe
  plot_data <- rbind(plot_data, data.frame(fuel = fuel, energy = energy, costs = total_costs, total_kilometers = total_kilometers, total_kilometers_electro = total_kilometers_electro, total_kilometers_diesel = total_kilometers_diesel, total_vehicle_diesel = total_vehicle_diesel, total_vehicle_electro = total_vehicle_electro))
}

# Iterate through each folder for base
for (folder_base in folders_base) {
  # Extract parameters from folder name using regular expressions
  params <- strsplit(folder_base, "_")[[1]]

  # Extracting parameters from folder name
  fuel <- as.numeric(gsub("fuel", "", params[2]))
  energy <- as.numeric(gsub("energy", "", params[3]))

  # Path to the analysis file
  analysis_file_base <- file.path(path_base, folder_base, "Analysis", "TimeDistance_perVehicleType.tsv")

  # Read the analysis file
  analysis_data_base <- read.table(analysis_file_base, header = TRUE, sep = "\t")

  # Calculate sum of totalCosts[EUR] column
  total_costs <- sum(analysis_data_base$totalCosts.EUR)

  total_kilometers <- sum(analysis_data_base$SumOfTravelDistances.km.)
  total_kilometers_electro <- sum(analysis_data_base$SumOfTravelDistances.km[grepl("_electro", analysis_data_base$vehicleTypeId)])
  total_kilometers_diesel <- sum(analysis_data_base$SumOfTravelDistances.km[!grepl("_electro", analysis_data_base$vehicleTypeId)])

  total_vehicle_diesel <- sum(analysis_data_base$nuOfVehicles[!grepl("_electro", analysis_data_base$vehicleTypeId)])
  total_vehicle_electro <- sum(analysis_data_base$nuOfVehicles[grepl("_electro", analysis_data_base$vehicleTypeId)])

  # Append parameters and results to the dataframe
  plot_data_base <- rbind(plot_data_base, data.frame(fuel = fuel, energy = energy, costs = total_costs, total_kilometers = total_kilometers, total_kilometers_electro = total_kilometers_electro, total_kilometers_diesel = total_kilometers_diesel, total_vehicle_diesel = total_vehicle_diesel, total_vehicle_electro = total_vehicle_electro))
}
# Find the row where fuel and or energy equals the desired values
row_index_base_2024 <- which(plot_data_base$fuel == diesel_prices["2024"])
row_index_base_2030 <- which(plot_data_base$fuel == diesel_prices["2030"])
row_index_base_2050 <- which(plot_data_base$fuel == diesel_prices["2050"])

row_index_optimistic_2024 <- which(plot_data$fuel == diesel_prices["2024"] & plot_data$energy == energy_prices[["2024"]][["optimistic"]])
row_index_optimistic_2030 <- which(plot_data$fuel == diesel_prices["2030"] & plot_data$energy == energy_prices[["2030"]][["optimistic"]])
row_index_optimistic_2050 <- which(plot_data$fuel == diesel_prices["2050"] & plot_data$energy == energy_prices[["2050"]][["optimistic"]])

row_index_pessimistic_2024 <- which(plot_data$fuel == diesel_prices["2024"] & plot_data$energy == energy_prices[["2024"]][["pessimistic"]])
row_index_pessimistic_2030 <- which(plot_data$fuel == diesel_prices["2030"] & plot_data$energy == energy_prices[["2030"]][["pessimistic"]])
row_index_pessimistic_2050 <- which(plot_data$fuel == diesel_prices["2050"] & plot_data$energy == energy_prices[["2050"]][["pessimistic"]])

# Extract the corresponding cost value
cost_base_2024 <- plot_data_base$costs[row_index_base_2024]
cost_base_2030 <- plot_data_base$costs[row_index_base_2030]
cost_base_2050 <- plot_data_base$costs[row_index_base_2050]

distance_diesel_base_2024 <- plot_data_base$total_kilometers_diesel[row_index_base_2024]
distance_diesel_base_2030 <- plot_data_base$total_kilometers_diesel[row_index_base_2030]
distance_diesel_base_2050 <- plot_data_base$total_kilometers_diesel[row_index_base_2050]

distance_electro_base_2024 <- plot_data_base$total_kilometers_electro[row_index_base_2024]
distance_electro_base_2030 <- plot_data_base$total_kilometers_electro[row_index_base_2030]
distance_electro_base_2050 <- plot_data_base$total_kilometers_electro[row_index_base_2050]

number_diesel_vehicle_base_2024 <- plot_data_base$total_vehicle_diesel[row_index_base_2024]
number_diesel_vehicle_base_2030 <- plot_data_base$total_vehicle_diesel[row_index_base_2030]
number_diesel_vehicle_base_2050 <- plot_data_base$total_vehicle_diesel[row_index_base_2050]

number_electro_vehicle_base_2024 <- plot_data_base$total_vehicle_electro[row_index_base_2024]
number_electro_vehicle_base_2030 <- plot_data_base$total_vehicle_electro[row_index_base_2030]
number_electro_vehicle_base_2050 <- plot_data_base$total_vehicle_electro[row_index_base_2050]

total_costs_optimistic_2024 <- plot_data$costs[row_index_optimistic_2024]
total_costs_optimistic_2030 <- plot_data$costs[row_index_optimistic_2030]
total_costs_optimistic_2050 <- plot_data$costs[row_index_optimistic_2050]

distance_diesel_optimistic_2024 <- plot_data$total_kilometers_diesel[row_index_optimistic_2024]
distance_diesel_optimistic_2030 <- plot_data$total_kilometers_diesel[row_index_optimistic_2030]
distance_diesel_optimistic_2050 <- plot_data$total_kilometers_diesel[row_index_optimistic_2050]

distance_electro_optimistic_2024 <- plot_data$total_kilometers_electro[row_index_optimistic_2024]
distance_electro_optimistic_2030 <- plot_data$total_kilometers_electro[row_index_optimistic_2030]
distance_electro_optimistic_2050 <- plot_data$total_kilometers_electro[row_index_optimistic_2050]

number_diesel_vehicle_optimistic_2024 <- plot_data$total_vehicle_diesel[row_index_optimistic_2024]
number_diesel_vehicle_optimistic_2030 <- plot_data$total_vehicle_diesel[row_index_optimistic_2030]
number_diesel_vehicle_optimistic_2050 <- plot_data$total_vehicle_diesel[row_index_optimistic_2050]

number_electro_vehicle_optimistic_2024 <- plot_data$total_vehicle_electro[row_index_optimistic_2024]
number_electro_vehicle_optimistic_2030 <- plot_data$total_vehicle_electro[row_index_optimistic_2030]
number_electro_vehicle_optimistic_2050 <- plot_data$total_vehicle_electro[row_index_optimistic_2050]

total_costs_pessimistic_2024 <- plot_data$costs[row_index_pessimistic_2024]
total_costs_pessimistic_2030 <- plot_data$costs[row_index_pessimistic_2030]
total_costs_pessimistic_2050 <- plot_data$costs[row_index_pessimistic_2050]

distance_diesel_pesimistic_2024 <- plot_data$total_kilometers_diesel[row_index_pessimistic_2024]
distance_diesel_pesimistic_2030 <- plot_data$total_kilometers_diesel[row_index_pessimistic_2030]
distance_diesel_pesimistic_2050 <- plot_data$total_kilometers_diesel[row_index_pessimistic_2050]

distance_electro_pessimistic_2024 <- plot_data$total_kilometers_electro[row_index_pessimistic_2024]
distance_electro_pessimistic_2030 <- plot_data$total_kilometers_electro[row_index_pessimistic_2030]
distance_electro_pessimistic_2050 <- plot_data$total_kilometers_electro[row_index_pessimistic_2050]

number_diesel_vehicle_pessimistic_2024 <- plot_data$total_vehicle_diesel[row_index_pessimistic_2024]
number_diesel_vehicle_pessimistic_2030 <- plot_data$total_vehicle_diesel[row_index_pessimistic_2030]
number_diesel_vehicle_pessimistic_2050 <- plot_data$total_vehicle_diesel[row_index_pessimistic_2050]

number_electro_vehicle_pessimistic_2024 <- plot_data$total_vehicle_electro[row_index_pessimistic_2024]
number_electro_vehicle_pessimistic_2030 <- plot_data$total_vehicle_electro[row_index_pessimistic_2030]
number_electro_vehicle_pessimistic_2050 <- plot_data$total_vehicle_electro[row_index_pessimistic_2050]
# Calculate costs for each scenario and year
costs_2024 <- c(cost_base_2024, total_costs_pessimistic_2024, total_costs_optimistic_2024)
costs_2030 <- c(cost_base_2030, total_costs_pessimistic_2030, total_costs_optimistic_2030)
costs_2050 <- c(cost_base_2050, total_costs_pessimistic_2050, total_costs_optimistic_2050)

distance_diesel_2024 <- c(distance_diesel_base_2024, distance_diesel_pesimistic_2024, distance_diesel_optimistic_2024)
distance_diesel_2030 <- c(distance_diesel_base_2030, distance_diesel_pesimistic_2030, distance_diesel_optimistic_2030)
distance_diesel_2050 <- c(distance_diesel_base_2050, distance_diesel_pesimistic_2050, distance_diesel_optimistic_2050)

distance_electro_2024 <- c(distance_electro_base_2024, distance_electro_pessimistic_2024, distance_electro_optimistic_2024)
distance_electro_2030 <- c(distance_electro_base_2030, distance_electro_pessimistic_2030, distance_electro_optimistic_2030)
distance_electro_2050 <- c(distance_electro_base_2050, distance_electro_pessimistic_2050, distance_electro_optimistic_2050)

number_diesel_vehicle_2024 <- c(number_diesel_vehicle_base_2024, number_diesel_vehicle_pessimistic_2024, number_diesel_vehicle_optimistic_2024)
number_diesel_vehicle_2030 <- c(number_diesel_vehicle_base_2030, number_diesel_vehicle_pessimistic_2030, number_diesel_vehicle_optimistic_2030)
number_diesel_vehicle_2050 <- c(number_diesel_vehicle_base_2050, number_diesel_vehicle_pessimistic_2050, number_diesel_vehicle_optimistic_2050)

number_electro_vehicle_2024 <- c(number_electro_vehicle_base_2024, number_electro_vehicle_pessimistic_2024, number_electro_vehicle_optimistic_2024)
number_electro_vehicle_2030 <- c(number_electro_vehicle_base_2030, number_electro_vehicle_pessimistic_2030, number_electro_vehicle_optimistic_2030)
number_electro_vehicle_2050 <- c(number_electro_vehicle_base_2050, number_electro_vehicle_pessimistic_2050, number_electro_vehicle_optimistic_2050)
# Create a dataframe
scenario_data <- data.frame(
  Year = rep(c(2024, 2030, 2050), each = 3),
  Scenario = rep(c("Base Case", "Pessimistic", "Optimistic"), times = 3),
  Costs = c(costs_2024, costs_2030, costs_2050),
  distance_diesel = c(distance_diesel_2024, distance_diesel_2030, distance_diesel_2050),
  distance_electro = c(distance_electro_2024, distance_electro_2030, distance_electro_2050),
  number_diesel_vehicle = c(number_diesel_vehicle_2024, number_diesel_vehicle_2030, number_diesel_vehicle_2050),
  number_electro_vehicle = c(number_electro_vehicle_2024, number_electro_vehicle_2030, number_electro_vehicle_2050)
)
# Reorder the scenarios in the costs_data dataframe
scenario_data$Scenario <- factor(scenario_data$Scenario, levels = c("Base Case", "Pessimistic", "Optimistic"))

# Convert Year column to factor for better grouping
scenario_data$Year <- factor(scenario_data$Year)

# Melt the data to long format
melted_distances <- reshape2::melt(scenario_data, id.vars = c("Year", "Scenario"), measure.vars = c("distance_electro", "distance_diesel"))
melted_vehicles <- reshape2::melt(scenario_data, id.vars = c("Year", "Scenario"), measure.vars = c("number_diesel_vehicle", "number_electro_vehicle"))

# Define custom colors for each variable
custom_colors_Distance <- c("distance_electro" = "green",
                            "distance_diesel" = "red")
custom_colors_Vehicles <- c("number_electro_vehicle" = "green",
                            "number_diesel_vehicle" = "red")
# Plot to compare the driven distance for the different scenarios and years
ggplot(melted_distances, aes(x = Scenario, y = value, fill = variable)) +
  geom_bar(stat = 'identity', position = 'stack') +
  scale_fill_manual(values = custom_colors_Distance) +  # Set custom colors
  facet_grid(~Year) +
  ggtitle("Driven distance Comparison for the Different Scenarios and Years") +
  xlab("Scenarios") +
  ylab("Driven Distance (km)") +
  labs(fill = "Engine Type")

# Plot to compare the number of vehicles for the different scenarios and years
ggplot(melted_vehicles, aes(x = Scenario, y = value, fill = variable)) +
  geom_bar(stat = 'identity', position = 'stack') +
  scale_fill_manual(values = custom_colors_Vehicles) +  # Set custom colors
  facet_grid(~Year) +
  ggtitle("Fleet comparision for the Different Scenarios and Years") +
  xlab("Scenarios") +
  ylab("Number of vehicles") +
  labs(fill = "Engine Type")

# Define colors for the different scenarios
scenario_colors <- c("Base Case" = "grey", "Pessimistic" = "orange", "Optimistic" = "green")

# Plot the costs comparison for the different scenarios and years
plot_ly(scenario_data, x = ~factor(Year), y = ~Costs, color = ~Scenario, type = "bar", colors = scenario_colors) %>%
  layout(title = "Costs Comparison for Different Scenarios and Years",
         xaxis = list(title = "Year"),
         yaxis = list(title = "Costs"),
         barmode = "group",
         # Set the desired order of scenarios
         legend = list(traceorder = "normal"))

# Create 3D scatter plot for different fuel and energy costs
scatterplot3d(plot_data$fuel, plot_data$energy, plot_data$costs,
              main = "Total Costs vs. Fuel and Energy",
              xlab = "Fuel",
              ylab = "Energy",
              zlab = "Total Costs",
              color = "blue",
              pch = 16)

# Create interactive 3D scatter plot for different fuel and energy costs
plot_ly(plot_data, x = ~fuel, y = ~energy, z = ~costs, type = "scatter3d",
        mode = "markers", marker = list(size = 5)) %>%
  layout(scene = list(xaxis = list(title = "Fuel Costs (EUR/l)"),
                      yaxis = list(title = "Energy Costs (EUR/kWh)"),
                      zaxis = list(title = "Costs (EUR)"),
                      camera = list(eye = list(x = 1.2, y = 1.2, z = 1.2))))

# Create Plot to compare the total costs for different fuel and energy costs
plot_ly(plot_data, x = ~fuel, y = ~costs, color = ~factor(energy), type = "scatter", mode = "lines") %>%
  add_trace(name = "Title") %>%
  layout(title = "Total Costs vs. Fuel and Energy",
         xaxis = list(title = "Fuel price (EUR/l)"),
         yaxis = list(title = "Total Costs"),
         colorway = c("blue", "red"),  # Optional: define color palette
         showlegend = TRUE,  # Optional: show legend
         legend = list(title = list(text = 'Energy price (EUR/kWh)')))

