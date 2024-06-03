# Install and load necessary packages
library(scatterplot3d)
library(plotly)
library(ggplot2)
library(reshape2)
library(dplyr)
library(tidyr)

##### function ######
calculateAnualValues <- function (diesel_prices, energy_prices, analysis_data, plot_data_annual_costs, Scenario, working_Days_per_year, annual_carging_infrastructure_costs_EUR_per_year_and_vehicle){
  cumulated_costs <- 0
  for (Year in diesel_prices$year) {
    fuelThisYear <- subset(diesel_prices, Year == year)$price
    energyThisYear <- ifelse(Scenario == "Optimistic",subset(energy_prices, Year == year)$optimistic, subset(energy_prices, Year == year)$pessimistic)
    fixcosts_vehicles <- sum(analysis_data$fixedCosts.EUR.) * working_Days_per_year / 1000000
    fixcosts_chargingInfratructure <- annual_carging_infrastructure_costs_EUR_per_year_and_vehicle * sum(analysis_data$nuOfVehicles [grepl("_electro", analysis_data$vehicleTypeId)])  / 1000000
    varCosts_time <- sum(analysis_data$varCostsTime.EUR) * working_Days_per_year / 1000000
    varCosts_without_consumption <- sum(analysis_data$varCostsWithoutConsumption.EUR) * working_Days_per_year / 1000000
    varCosts_consumption <- sum(ifelse(grepl("_electro", analysis_data$vehicleTypeId),
                                       energyThisYear * analysis_data$Consumption,
                                       fuelThisYear * analysis_data$Consumption)) * working_Days_per_year / 1000000
    costs_sum <- fixcosts_vehicles +
      fixcosts_chargingInfratructure +
      varCosts_time +
      varCosts_without_consumption +
      varCosts_consumption
    cumulated_costs <- cumulated_costs + costs_sum

      plot_data_annual_costs <- rbind(plot_data_annual_costs,
                                    data.frame(year = Year,
                                               scenario = Scenario,
                                               diesel_price = fuelThisYear,
                                               energy_price = energyThisYear,
                                               totalCosts_mio = costs_sum,
                                               fixCosts = fixcosts_vehicles,
                                               fixCosts_chargingInfratructure = fixcosts_chargingInfratructure,
                                               varCosts_time = varCosts_time,
                                               varCosts_without_consumption = varCosts_without_consumption,
                                               varCosts_consumption = varCosts_consumption,
                                               cumulated_costs = cumulated_costs))
  }
  return(plot_data_annual_costs)
}

#Todo: nutze eine Option um dann Setting RE oder KMT zu laden.

### Settings Ricardo

# Set the working directory to the folder containing your simulation run folders
setwd("C:/Users/Ricardo/git/zerocuts/output/food/costsVariation_withDC_5000it")

# List all folders in the testFolder directory
folders <- list.dirs(path = "C:/Users/Ricardo/git/zerocuts/output/food/costsVariation_withDC_5000it", full.names = FALSE, recursive = FALSE)
path_base <- "C:/Users/Ricardo/git/zerocuts/output/food/costsVariation_withDC_base_10000it"
folders_base <- list.dirs(path = "C:/Users/Ricardo/git/zerocuts/output/food/costsVariation_withDC_base_10000it", full.names = FALSE, recursive = FALSE)

# File contains consumption information of each vehicleType
path_vehicleTypeFile <- "C:/Users/Ricardo/git/zerocuts/output/food/vehTypVariableCostsWithoutEngine.csv"
vehcileTypeFile <- read.table(path_vehicleTypeFile, header = TRUE, sep = "\t")


#### Settings KMT
# Set the working directory to the folder containing your simulation run folders
setwd("/Users/kturner/Library/CloudStorage/GoogleDrive-martins-turner@vsp.tu-berlin.de/.shortcut-targets-by-id/1ME69UR7QBzkeVgfJzUTSpxRBUWwH4GVC/vsp-projects/2023/zerocuts/EFood2024/costsVariation_mixedFleet_withDC_5000it")

# List all folders in the testFolder directory
folders <- list.dirs(path = "/Users/kturner/Library/CloudStorage/GoogleDrive-martins-turner@vsp.tu-berlin.de/.shortcut-targets-by-id/1ME69UR7QBzkeVgfJzUTSpxRBUWwH4GVC/vsp-projects/2023/zerocuts/EFood2024/costsVariation_mixedFleet_withDC_5000it", full.names = FALSE, recursive = FALSE)
path_base <- "/Users/kturner/Library/CloudStorage/GoogleDrive-martins-turner@vsp.tu-berlin.de/.shortcut-targets-by-id/1ME69UR7QBzkeVgfJzUTSpxRBUWwH4GVC/vsp-projects/2023/zerocuts/EFood2024/costsVariation_onlyICEV_10000it"
folders_base <- list.dirs(path = "/Users/kturner/Library/CloudStorage/GoogleDrive-martins-turner@vsp.tu-berlin.de/.shortcut-targets-by-id/1ME69UR7QBzkeVgfJzUTSpxRBUWwH4GVC/vsp-projects/2023/zerocuts/EFood2024/costsVariation_onlyICEV_10000it", full.names = FALSE, recursive = FALSE)

# File contains consumption information of each vehicleType
path_vehicleTypeFile <- "/Users/kturner/Library/CloudStorage/GoogleDrive-martins-turner@vsp.tu-berlin.de/.shortcut-targets-by-id/1ME69UR7QBzkeVgfJzUTSpxRBUWwH4GVC/vsp-projects/2023/zerocuts/EFood2024/vehTypVariableCostsWithoutEngine.csv"
vehcileTypeFile <- read.table(path_vehicleTypeFile, header = TRUE, sep = "\t")


### START Inhalt


# Initialize an empty dataframe to store the data
plot_data <- data.frame()
plot_data_base <- data.frame()
plot_data_annual_costs <- data.frame()

# Define the number of working days per year to calculate the annual costs
working_Days_per_year <- 250

# Define diesel prices for each year
diesel_price_assumptions <- data.frame(
  year = c(2024, 2030, 2050),
  price = c(1.55, 1.78, 3.2)
)

# Define energy prices for each year (optimistic and pessimistic)
energy_prices_assumptions <- data.frame(
  year = c(2024, 2030, 2050),
  optimistic = c(0.18, 0.18, 0.18),
  pessimistic = c(0.24, 0.21, 0.21)
)

# Set costs for charging infrastructure per year
annual_carging_infrastructure_costs_EUR_per_year_and_vehicle <- 1638

# Define the years you want to interpolate for
interpolate_years <- 2024:2050
# Use the approx() function to interpolate the diesel prices for the years in between
interpolated_prices <- approx(diesel_price_assumptions$year, diesel_price_assumptions$price, xout = interpolate_years)
# Create a data frame with the interpolated prices
diesel_prices <- data.frame(
  year = interpolated_prices$x,
  price = interpolated_prices$y
)
# Sort the data frame by year
diesel_prices <- diesel_prices[order(diesel_prices$year), ]
# Convert Year column to factor for better grouping
diesel_prices$year <- factor(diesel_prices$year)

# Use the approx() function to interpolate the optimistic energy prices for the years in between
interpolated_optimistic_prices <- approx(energy_prices_assumptions$year, energy_prices_assumptions$optimistic, xout = interpolate_years)
# Use the approx() function to interpolate the pessimistic energy prices for the years in between
interpolated_pessimistic_prices <- approx(energy_prices_assumptions$year, energy_prices_assumptions$pessimistic, xout = interpolate_years)
# Create a data frame with the interpolated prices
energy_prices <- data.frame(
  year = interpolated_optimistic_prices$x,
  optimistic = interpolated_optimistic_prices$y,
  pessimistic = interpolated_pessimistic_prices$y
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

  analysis_data <- merge(analysis_data, vehcileTypeFile, by = "vehicleTypeId")

  analysis_data$varCostsWithoutConsumption.EUR <- analysis_data$costsWithoutConsumption.EUR.m. * analysis_data$SumOfTravelDistances.m.
  # analysis_data$varCostsConsumption.EUR <- ifelse(grepl("_electro", analysis_data$vehicleTypeId),
  #                                                 energy * analysis_data$SumOfTravelDistances.m. * analysis_data$energyConsumptionPerMeter,
  #                                                 fuel * analysis_data$SumOfTravelDistances.m. * analysis_data$energyConsumptionPerMeter)
  analysis_data$Consumption <- analysis_data$SumOfTravelDistances.m. * analysis_data$energyConsumptionPerMeter
  if (fuel == 1.55 && energy == 0.24) {
    # year 2024 with pessimitic energy prices
    Scenario <- "Pessimistic"
    plot_data_annual_costs <- calculateAnualValues(diesel_prices, energy_prices, analysis_data, plot_data_annual_costs, Scenario, working_Days_per_year, annual_carging_infrastructure_costs_EUR_per_year_and_vehicle)
  }
  if (fuel == 1.55 && energy == 0.18) {
    # year 2024 with optimistic energy prices
    Scenario <- "Optimistic"
    plot_data_annual_costs <- calculateAnualValues(diesel_prices, energy_prices, analysis_data, plot_data_annual_costs, Scenario, working_Days_per_year, annual_carging_infrastructure_costs_EUR_per_year_and_vehicle)
  }

  # Calculate sum of totalCosts[EUR] column
  total_costs <- sum(analysis_data$totalCosts.EUR)
  total_fixCosts <- sum(analysis_data$fixedCosts.EUR.)
  total_varCosts_time <- sum(analysis_data$varCostsTime.EUR)
  total_varCosts_distance <- sum(analysis_data$varCostsDist.EUR)

  total_kilometers <- sum(analysis_data$SumOfTravelDistances.km.)
  total_kilometers_electro <- sum(analysis_data$SumOfTravelDistances.km[grepl("_electro", analysis_data$vehicleTypeId)])
  total_kilometers_diesel <- sum(analysis_data$SumOfTravelDistances.km[!grepl("_electro", analysis_data$vehicleTypeId)])

  total_vehicle_diesel <- sum(analysis_data$nuOfVehicles [!grepl("_electro", analysis_data$vehicleTypeId)])
  total_vehicle_electro <- sum(analysis_data$nuOfVehicles [grepl("_electro", analysis_data$vehicleTypeId)])

  # Append parameters and results to the dataframe
  plot_data <- rbind(plot_data, data.frame(fuel = fuel, energy = energy, costs = total_costs, fixCosts = total_fixCosts, variableCosts_time = total_varCosts_time, variableCosts_dist = total_varCosts_distance, total_kilometers = total_kilometers, total_kilometers_electro = total_kilometers_electro, total_kilometers_diesel = total_kilometers_diesel, total_vehicle_diesel = total_vehicle_diesel, total_vehicle_electro = total_vehicle_electro))
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

  analysis_data_base <- merge(analysis_data_base, vehcileTypeFile, by = "vehicleTypeId")

  analysis_data_base$varCostsWithoutConsumption.EUR <- analysis_data_base$costsWithoutConsumption.EUR.m. * analysis_data_base$SumOfTravelDistances.m.

  analysis_data_base$Consumption <- analysis_data_base$SumOfTravelDistances.m. * analysis_data_base$energyConsumptionPerMeter
  Scenario <- "Base Case"
  if (!(Scenario %in% plot_data_annual_costs$Scenario) && fuel == 1.55) {
      # year 2024 with pessimitic energy prices
    plot_data_annual_costs <- calculateAnualValues(diesel_prices, energy_prices, analysis_data_base, plot_data_annual_costs, Scenario, working_Days_per_year, annual_carging_infrastructure_costs_EUR_per_year_and_vehicle)
  }
  # Calculate sum of totalCosts[EUR] column
  total_costs <- sum(analysis_data_base$totalCosts.EUR)
  total_fixCosts <- sum(analysis_data_base$fixedCosts.EUR.)
  total_varCosts_time <- sum(analysis_data_base$varCostsTime.EUR)
  total_varCosts_distance <- sum(analysis_data_base$varCostsDist.EUR)

  total_kilometers <- sum(analysis_data_base$SumOfTravelDistances.km.)
  total_kilometers_electro <- sum(analysis_data_base$SumOfTravelDistances.km[grepl("_electro", analysis_data_base$vehicleTypeId)])
  total_kilometers_diesel <- sum(analysis_data_base$SumOfTravelDistances.km[!grepl("_electro", analysis_data_base$vehicleTypeId)])

  total_vehicle_diesel <- sum(analysis_data_base$nuOfVehicles[!grepl("_electro", analysis_data_base$vehicleTypeId)])
  total_vehicle_electro <- sum(analysis_data_base$nuOfVehicles[grepl("_electro", analysis_data_base$vehicleTypeId)])

  # Append parameters and results to the dataframe
  plot_data_base <- rbind(plot_data_base, data.frame(fuel = fuel, energy = energy, costs = total_costs, fixCosts = total_fixCosts, variableCosts_time = total_varCosts_time, variableCosts_dist = total_varCosts_distance, total_kilometers = total_kilometers, total_kilometers_electro = total_kilometers_electro, total_kilometers_diesel = total_kilometers_diesel, total_vehicle_diesel = total_vehicle_diesel, total_vehicle_electro = total_vehicle_electro))
}
# Find the row where fuel and or energy equals the desired values
row_index_base_2024 <- which(plot_data_base$fuel == subset(diesel_prices, year == 2024)$price)
row_index_base_2030 <- which(plot_data_base$fuel == subset(diesel_prices, year == 2030)$price)
row_index_base_2050 <- which(plot_data_base$fuel == subset(diesel_prices, year == 2050)$price)

row_index_optimistic_2024 <- which(plot_data$fuel == subset(diesel_prices, year == 2024)$price & plot_data$energy == subset(energy_prices, year == 2024)[["optimistic"]])
row_index_optimistic_2030 <- which(plot_data$fuel == subset(diesel_prices, year == 2030)$price & plot_data$energy == subset(energy_prices, year == 2030)[["optimistic"]])
row_index_optimistic_2050 <- which(plot_data$fuel == subset(diesel_prices, year == 2050)$price & plot_data$energy == subset(energy_prices, year == 2050)[["optimistic"]])

row_index_pessimistic_2024 <- which(plot_data$fuel == subset(diesel_prices, year == 2024)$price & plot_data$energy == subset(energy_prices, year == 2024)[["pessimistic"]])
row_index_pessimistic_2030 <- which(plot_data$fuel == subset(diesel_prices, year == 2030)$price & plot_data$energy == subset(energy_prices, year == 2030)[["pessimistic"]])
row_index_pessimistic_2050 <- which(plot_data$fuel == subset(diesel_prices, year == 2050)$price & plot_data$energy == subset(energy_prices, year == 2050)[["pessimistic"]])

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
melted_vehicles <- reshape2::melt(scenario_data, id.vars = c("Year", "Scenario"), measure.vars = c("number_electro_vehicle", "number_diesel_vehicle"))
melted_costs <- reshape2::melt(scenario_data, id.vars = c("Year", "Scenario"), measure.vars = c("Costs"))
plot_data_annual_costs_long <- plot_data_annual_costs %>%
  pivot_longer(cols = c(fixCosts, varCosts_time, varCosts_without_consumption, varCosts_consumption),
               names_to = "cost_type", values_to = "cost_value")
# Define custom colors for each variable
custom_colors_Distance <- c("distance_electro" = "green",
                            "distance_diesel" = "red")
custom_colors_Vehicles <- c("number_electro_vehicle" = "green",
                            "number_diesel_vehicle" = "red")
custom_colors_Costs <- c("Base Case" = "grey", "Pessimistic" = "orange", "Optimistic" = "green")

custom_colors_Costs_years <- c("fixCosts" = "red", "varCosts_time" = "blue",
                         "varCosts_without_consumption" = "green", "varCosts_consumption" = "purple")

# Plot to compare the driven distance for the different scenarios and years
ggplot(melted_distances, aes(x = Scenario, y = value, fill = variable)) +
  geom_bar(stat = 'identity', position = 'stack') +
  scale_fill_manual(values = custom_colors_Distance) +  # Set custom colors
  facet_grid(~Year) +
  ggtitle("Driven distance Comparison for the Different Scenarios and Years") +
  xlab("Scenarios") +
  ylab("Driven Distance (km)") +
  labs(fill = "Engine Type") +
  theme(legend.position = "top",
        text = element_text(size = 20),
        axis.text.x = element_text(angle = 90, hjust = 1))

# Plot to compare the number of vehicles for the different scenarios and years
ggplot(melted_vehicles, aes(x = Scenario, y = value, fill = variable)) +
  geom_bar(stat = 'identity', position = 'stack') +
  scale_fill_manual(values = custom_colors_Vehicles) +  # Set custom colors
  facet_grid(~Year) +
  ggtitle("Fleet comparision for the Different Scenarios and Years") +
  xlab("Scenarios") +
  ylab("Number of vehicles") +
  labs(fill = "Engine Type") +
  theme(legend.position = "top",
        text = element_text(size = 20),
        axis.text.x = element_text(angle = 90, hjust = 1))

# Plot to compare the total costs for the different scenarios and years
ggplot(melted_costs, aes(x = Scenario, y = value, fill = Scenario)) +
  geom_bar(stat = 'identity', position = 'dodge') +
  scale_fill_manual(values = custom_colors_Costs) +
  facet_grid(~Year) +
  ggtitle("Costs Comparison for Different Scenarios and Years") +
  xlab("Scenarios") +
  ylab("Costs") +
  labs(fill = "Scenario") +
  theme(legend.position = "none",
        text = element_text(size = 20),
        axis.text.x = element_text(angle = 90, hjust = 1))

# Plot to compare the cumulated costs for the different scenarios and years
ggplot(plot_data_annual_costs, aes(x = year, y = totalCosts, fill = scenario)) +
  geom_bar(stat = 'identity', position = 'dodge') +
  scale_fill_manual(values = custom_colors_Costs) +
  geom_line(aes(x = year, y = cumulated_costs, group = scenario, color = scenario)) +
  ggtitle("Cumulated Costs Comparison for Different Scenarios and Years") +
  xlab("Year") +
  ylab("Cumulated Costs") +
  labs(fill = "Scenario") +
  theme(legend.position = "top",
        text = element_text(size = 20),
        axis.text.x = element_text(angle = 90, hjust = 1))+
          scale_y_continuous(
            name = "Cumulated Costs",
            sec.axis = sec_axis(~ ., name = "Cumulative Costs",
                                breaks = scales::breaks_width(1000), # You can adjust breaks according to your data
                                labels = scales::comma))


ggplot(plot_data_annual_costs_long, aes(x = year, y = cost_value, fill = cost_type)) +
  geom_bar(stat = 'identity', position = 'stack') +
  facet_wrap(~ scenario) +
  scale_fill_manual(values = custom_colors_Costs_years) +
  ggtitle("Cumulated Costs Comparison for Different Scenarios and Years") +
  xlab("Year") +
  ylab("Cumulated Costs") +
  labs(fill = "Cost Type") +
  theme(legend.position = "top",
        text = element_text(size = 20),
        axis.text.x = element_text(angle = 90, hjust = 1))
ggplot(plot_data_annual_costs_long, aes(x = as.factor(year), y = cost_value, fill = cost_type)) +
  geom_bar(stat = 'identity', position = position_dodge(width = 0.8)) +
  facet_grid(~ scenario) +
  scale_fill_manual(values = custom_colors_Costs_years) +
  ggtitle("Cumulated Costs Comparison for Different Scenarios and Years") +
  xlab("Year") +
  ylab("Cumulated Costs") +
  labs(fill = "Cost Type") +
  theme(legend.position = "top",
        text = element_text(size = 20),
        axis.text.x = element_text(angle = 90, hjust = 1))

# Plot the costs comparison for the different scenarios and years
plot_ly(scenario_data, x = ~factor(Year), y = ~Costs, color = ~Scenario, type = "bar", colors = custom_colors_Costs) %>%
  layout(title = "Costs Comparison for Different Scenarios and Years",
         xaxis = list(title = "Year"),
         yaxis = list(title = "Costs"),
         barmode = "group",
         # Set the desired order of scenarios
         legend = list(traceorder = "normal"))



#################### 3D plots ####################

# Create 3D scatter plot for different fuel and energy costs
scatterplot3d(plot_data$fuel, plot_data$energy, plot_data$costs,
              main = "Total Costs vs. Fuel and Energy",
              xlab = "Fuel Costs (EUR/l)",
              ylab = "Energy Costs (EUR/kWh)",
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

