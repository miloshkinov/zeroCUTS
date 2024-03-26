##Kopie von Ricardo. Muss dann noch angepasst werden. KMT März 24

#setwd("/Users/kturner/git-and-svn/shared-svn/projects/freight/studies/UpdateEventsfromEarlierStudies/foodRetailing_wo_rangeConstraint/71_ICEVBEV_NwCE_BVWP_10000it_DCoff_noTax/analysis")
EFood <- FALSE


#####E-Food: Alle Fzg-Typen sind Electro und müssen so gesetzt werden.
#setwd("/Users/kturner/git-and-svn/shared-svn/projects/freight/studies/UpdateEventsfromEarlierStudies/Food_ETrucks/Base_NwCE_BVWP_Pickup_10000it/analysis")
setwd("/Users/kturner/git-and-svn/shared-svn/projects/freight/studies/UpdateEventsfromEarlierStudies/Food_ETrucks/CaseA_E160_NwCE_BVWP_Pickup_10000it/analysis")
EFood <- TRUE

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

# Read the CSV file
df_org <- read.csv2("TimeDistance_perVehicleType.csv")
df_tours_org <- read.csv2("TimeDistance_perVehicle.csv")

# Specify the desired order of vehicleTypeId
desired_order <- c("7.5t", "7.5t_electro","18t", "18t_electro","26t", "26t_electro","40t", "40t_electro")

#function to create categories
create_vehicle_categories <- function(data) {
  data <- data %>%
    mutate(vehicleCategory = ifelse(vehicleTypeId %in% c("light8t", "light8t_frozen"), yes = "7.5t",
                               no = ifelse(vehicleTypeId %in% c("light8t_electro", "light8t_frozen_electro"), yes = "7.5t_electro",
                                  no = ifelse(vehicleTypeId %in% c("medium18t", "medium18t_frozen"), yes = "18t",
                                     no = ifelse(vehicleTypeId %in% c("medium18t_electro", "medium18t_electro"), yes = "18t_electro", 
                                        no = ifelse(vehicleTypeId %in% c("heavy26t", "heavy26t_frozen"), yes = "26t", 
                                           no = ifelse(vehicleTypeId %in% c("heavy26t_electro",  "heavy26t_frozen_electro"), yes = "26t_electro", 
                                              no = ifelse(vehicleTypeId %in% c("heavy40t", "heavy40t_frozen"), yes = "40t", 
                                                 no = ifelse(vehicleTypeId %in% c("heavy40t_electro", "heavy40t_frozen_electro"), yes = "40t_electro", 
                                                    no = as.character(vehicleTypeId))))))))))
}

## Ergänze alle vehicleTypeIds um "_electro"
make_all_vehicles_electric <- function(data) {
  data <- data %>%
    mutate(vehicleTypeId=paste0(vehicleTypeId,"_electro"))
}

df <- df_org # Erstmal nur eine Kopie davon in der dann gearbeitet wird.
df_tours <- select(df_tours_org, "vehicleId","vehicleTypeId", "travelDistance.km.", "tourDuration.h.")

## Im EFood-Scenario waren die E-Fahrzeuge in den Policy-Cases NICHT als solche getaggt -> Mache sie hier zu E-Fahrzeugen.
if (EFood == TRUE){
  df_tours <- make_all_vehicles_electric(df_tours)
  df <- make_all_vehicles_electric(df)
}


## Ergänze eine Spalte mit den VehicleCategories
df <- create_vehicle_categories(df)
df_tours <- create_vehicle_categories(df_tours)


## Füge Dummy-Werte hinzu, damit jede Spalte auf jeden Fall einen Wert hat und mit geplottet wird
## ACHTUNG: Damit nun keine Rechnungen mehr auf dem Datensatz machen!
addDummyValues <- function(data) {
  vehTypesInData <- unique(select(data, matches("vehicleCategory")))  ## Alle VehicleTypes als Spaltenvektor
  for(myVar in desired_order){
    if (nrow(filter(vehTypesInData, vehicleCategory == myVar)) == 0){ ##Wenn Anzahl  == 0 (Also nicht vorhanden)
      data <- rbind(data, list("DUMMY_VEH", "DUMMY_TYPE", -999, -999, myVar))  ##Dann packe Dummy-WErt dazu
    }
  }
  return(data)
}

df_tours <- addDummyValues(df_tours)


# Convert 'vehicleCategory' to a factor with the desired order
df$vehicleCategory <- factor(df$vehicleCategory, levels = desired_order)
df_tours$vehicleCategory <- factor(df_tours$vehicleCategory, levels = desired_order)

#Runde Werte auf 2 Nachkommastellen
df_tours$travelDistance.km. <- as.numeric(df_tours$travelDistance.km.)
df_tours$travelDistance.km. <- round(df_tours$travelDistance.km.,2)
df_tours$tourDuration.h. <- round(as.numeric(df_tours$tourDuration.h.),2)

# # Ensure both plots have the same levels
# common_levels <- intersect(levels(df$vehicleCategory), levels(df$vehicleCategory))
# 
# # Filter the data frame to include only common levels
# df <- df %>% filter(vehicleCategory %in% common_levels)

# 1. Bar Plot for Number of Vehicles by Vehicle Type (Interactive)
bar_plot <- plot_ly(x = ~df$vehicleCategory, y = df$nuOfVehicles, type = 'bar') %>%
  layout(xaxis = list(title = 'Vehicle Category'), yaxis = list(title = 'Number of Vehicles'))

# 3. Bar Plot for Total Costs by Vehicle Type (Interactive)
bar_plot_costs <- plot_ly(x = df$vehicleCategory, y = df$totalCosts.EUR., type = 'bar', name = 'Total Costs') %>%
  layout(xaxis = list(title = 'Category'), yaxis = list(title = 'Total Costs (EUR)'))

### 4. Box Plot for Traveled Distances by Vehicle Type (Interactive)
## Max Reichweite auf 100km aufgerundet.
#max_y_km <- round(max(df_tours$travelDistance.km.),-2)
# Max Reichweite um 100km erhöht für Violinen-Plot und auf 100km aufgerundet.
max_y_km <- ceiling((max(df_tours$travelDistance.km.)+100)/100)*100

##Temporärer Versuch mit 2. Datensatz/Trace .. hat nicht geklappt.
#dummyDf <- data.frame(vehicleCategory=desired_order)
#dummyDf <- mutate(dummyDf, dummyVal=-9999.)
#dummyDf$vehicleCategory <- factor(dummyDf$vehicleCategory, levels = desired_order)

box_plot_distances <- plot_ly(data = df_tours, x = ~vehicleCategory, y = ~travelDistance.km., 
                              type = 'box', boxpoints = "all", jitter = 0.5, pointpos = -1.0) %>%
  layout(xaxis = list(title = 'Category'), yaxis = list(title = 'Traveled Distances (km)', range = list(0.,max_y_km)))

# 4b Violin- Plot Distances by Vehicle Type (Interactive)
##TODO: Farb-Schema festlegen ODER Grouped ViolinePlot??

violin_plot_distances <- plot_ly(#data = df_tours, 
                                 x = ~df_tours$vehicleCategory, 
                                 y = ~df_tours$travelDistance.km., 
                                 split = ~df_tours$vehicleCategory,
                                 type = 'violin',
                                 width = 1000,
                                 height = 500,
                                 box = list(visible = T),
                                 points = "all", jitter = 0.5, pointpos = -1.5) %>%
        layout(
          xaxis = list(title = 'Category'), 
          yaxis = list(title = 'Traveled Distances (km)',  range = list(-45.,max_y_km)), 
          #Aktuell noch ziemlich hässliche Farbpalette, aber sie Funktioniert, dass alle Diesel Rot und alle E-Fzg Grün sind.
          colorway = c("red", "green","red", "green","red", "green","red", "green"),
          showlegend = FALSE
          )

# Display the plots separately
print(bar_plot %>% layout(title = 'Number of Vehicles by Vehicle Category'))
print(bar_plot_costs %>% layout(title = 'Total Costs by Vehicle Category'))
print(box_plot_distances %>% layout(title = 'Traveled Distances by Vehicle Category'))
print(violin_plot_distances %>% layout(title = 'Traveled Distances by Vehicle Category'))
