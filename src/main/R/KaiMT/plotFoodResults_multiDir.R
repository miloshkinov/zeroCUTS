##Erstelle die Violinen-Plota für die Fahrweiten in den Food-Sceanrien KMT März 24
##Hier die Verison, die die Plots aus diversen Unterordnern nimmt und dann zusammen betrachtet.

# #setwd("C:/git-and-svn/shared-svn/projects/freight/studies/UpdateEventsfromEarlierStudies/foodRetailing_wo_rangeConstraint/71_ICEVBEV_NwCE_BVWP_10000it_DCoff_noTax/analysis")
# EFood <- FALSE
setwd("C:/git-and-svn/shared-svn/projects/freight/studies/UpdateEventsfromEarlierStudies/foodRetailing_wo_rangeConstraint/")
EFood <- FALSE


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
library(tibble)

# Hauptverzeichnis, in dem sich die Unterordner befinden
main_dir <- getwd()

# Liste der Unterordner im Hauptverzeichnis
subdirs <- list.dirs(main_dir, full.names = TRUE, recursive = FALSE)

# Erstelle ein leeres tibble zur Speicherung der Plots
all_plots_DistViolin <- tibble(
  subdir = character(),  # Speicherort für die Unterordnernamen
  plot = list()          # Speicherort für die Plots
)
all_plots_DistViolin_gg <- tibble(
  subdir = character(),  # Speicherort für die Unterordnernamen
  plot = list()          # Speicherort für die Plots
)

# Durchlaufe alle Unterordner und erstelle einen Plot für jeden Datensatz
for (subdir in subdirs) {

  # Erstelle den genauen Pfad zur gewünschten CSV-Datei
  file_path_VehType <- file.path(subdir, "Analysis", "TimeDistance_perVehicleType.tsv")
  file_path_veh <- file.path(subdir, "Analysis", "TimeDistance_perVehicle.tsv")
  # Read the CSV file
  # Überprüfe, ob die Datei existiert
  if (file.exists(file_path_VehType)) {
    df_org <- read.delim2(file_path_VehType)

    
    if (file.exists(file_path_veh)) { 
        df_tours_org <- read.delim2(file_path_veh)
        
        # Specify the desired order of vehicleTypeId
        desired_order <- c("7.5t", "7.5t_electro","18t", "18t_electro","26t", "26t_electro","40t", "40t_electro")
        
        #function to create categories
        create_vehicle_categories <- function(data) {
          data <- data %>%
            mutate(vehicleCategory = ifelse(vehicleTypeId %in% c("light8t", "light8t_frozen"), yes = "7.5t",
                                            no = ifelse(vehicleTypeId %in% c("light8t_electro", "light8t_frozen_electro", "light8t_electro_large_Quantron", "light8t_frozen_electro_large_Quantron", "light8t_electro_small_Mitsubishi", "light8t_frozen_electro_small_Mitsubishi"), yes = "7.5t_electro",
                                                        no = ifelse(vehicleTypeId %in% c("medium18t", "medium18t_frozen"), yes = "18t",
                                                                    no = ifelse(vehicleTypeId %in% c("medium18t_electro", "medium18t_electro", "medium18t_electro_large_Volvo", "medium18t_electro_small_Renault"), yes = "18t_electro", 
                                                                                no = ifelse(vehicleTypeId %in% c("heavy26t", "heavy26t_frozen"), yes = "26t", 
                                                                                            no = ifelse(vehicleTypeId %in% c("heavy26t_electro",  "heavy26t_frozen_electro", "heavy26t_electro_large_Daimler", "heavy26t_frozen_electro_large_Daimler", "heavy26t_electro_small_Volvo", "heavy26t_frozen_electro_small_Volvo"), yes = "26t_electro", 
                                                                                                        no = ifelse(vehicleTypeId %in% c("heavy40t", "heavy40t_frozen"), yes = "40t", 
                                                                                                                    no = ifelse(vehicleTypeId %in% c("heavy40t_electro", "heavy40t_frozen_electro", "heavy40t_electro_large_Scania", "heavy40t_electro_small_Daimler"), yes = "40t_electro", 
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
        max_y_km <- ceiling((max(df_tours$travelDistance.km.)+100)/100)*100-1
        
        ##Temporärer Versuch mit 2. Datensatz/Trace .. hat nicht geklappt.
        #dummyDf <- data.frame(vehicleCategory=desired_order)
        #dummyDf <- mutate(dummyDf, dummyVal=-9999.)
        #dummyDf$vehicleCategory <- factor(dummyDf$vehicleCategory, levels = desired_order)
        
        box_plot_distances <- plot_ly(data = df_tours, x = ~vehicleCategory, y = ~travelDistance.km., 
                                      type = 'box', boxpoints = "all", jitter = 0.5, pointpos = -1.0) %>%
          layout(xaxis = list(title = 'Category'), yaxis = list(title = 'Traveled Distances (km)', range = list(0.,max_y_km)))
        
        # 4b Violin- Plot Distances by Vehicle Type (Interactive)
        ##TODO: Farb-Schema festlegen ODER Grouped ViolinePlot??
        
        colorsKMT8 <- c("#1c18a0", "#9013fe" , "#1e54b6", "#760e95", "#3c71d9", "#aa108e", "#1f90cc","#DF0174")
        colorsAna <- c("#1c18a0", "#1e54b6", "#1f90cc", "#3c71d9", "#9013fe", "#760e95", "#aa108e", "c40d1e", "#a40c2e", "#5e082c","#4e0c49","#3d1066")
        
        xValue <- df_tours$vehicleCategory
        yValue <- df_tours$travelDistance.kmö
        
        ### plotly ####
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
            xaxis = list(title = 'Vehicle Type'),
            yaxis = list(title = 'Tour Distance (km)',  range = list(-45.,max_y_km)),
            #Aktuell noch ziemlich hässliche Farbpalette, aber sie Funktioniert, dass alle Diesel Rot und alle E-Fzg Grün sind.
            #colorway = c("red", "green","red", "green","red", "green","red", "green"),
            #colorway = colorsAna,
            colorway = colorsKMT8,
            showlegend = FALSE,
            title = basename(subdir)
          )

        # # Display the plots separately
        print(violin_plot_distances)
        readline(prompt = "Drücken Sie [ENTER], um fortzufahren...")
        
        # Optional: Plot als PNG speichern
        # ggsave(filename = "violin_plot_distances.png", plot = violin_plot_distances_gg, width = 10, height = 5)
        #filename <- paste(basename(subdir),".png")
        #save_image(violin_plot_distances, filename)
        
        # Füge den Plot und den Subdir-Namen dem Tibble hinzu
        all_plots_DistViolin <- all_plots_DistViolin  %>% add_row(subdir = basename(subdir), plot = list(violin_plot_distances))
        ###ENDE Plotly ####

        
        # ####TEST mit ggplot
        # # Erstellen des Violin-Plots mit ggplot2
        # violin_plot_distances_gg <- ggplot(df_tours, aes(x = vehicleCategory, y = travelDistance.km., fill = vehicleCategory)) +
        #   geom_violin(trim = FALSE, width = 1) + 
        #   geom_boxplot(width = 0.1, outlier.shape = NA) + 
        #   geom_jitter(position = position_jitter(width = 0.2), size = 1.5, alpha = 0.6) +
        #   scale_y_continuous(limits = c(-45, max_y_km)) +
        #   scale_fill_manual(values = colorsKMT8) +  # Anpassung der Farben
        #   labs(title = basename(subdir), x = "Vehicle Type", y = "Tour Distance (km)") +
        #   theme_minimal() + 
        #   theme(legend.position = "none", 
        #         plot.title = element_text(hjust = 0.5, size = 16))
        # 
        # # Plot anzeigen
        #  print(violin_plot_distances_gg)
        # 
        # # Optional: Plot als PNG speichern
        # # ggsave(filename = "violin_plot_distances.png", plot = violin_plot_distances_gg, width = 10, height = 5)
        # 
        #  
        #  # Füge den Plot und den Subdir-Namen dem Tibble hinzu
        #  all_plots_DistViolin_gg <- all_plots_DistViolin_gg %>% add_row(subdir = basename(subdir), plot = list(violin_plot_distances_gg))
        #  
        #  
        #  #### Ende ggplot
    } 
    else  {  message(paste("Datei nicht gefunden in:", subdir, file_path_veh))} } 
    
  else { message(paste("Datei nicht gefunden in:", subdir, file_path_VehType))}
  
}

###plotly###
# Test: Durchlaufen der Tibble und Anzeigen der Plots
for (i in seq_len(nrow(all_plots_DistViolin))) {
  print(all_plots_DistViolin$plot[[i]])

}


# Kombiniere die plotly-Plots zu einem einzigen Subplot
combined_plot <- subplot(all_plots_DistViolin$plot, nrows = length(all_plots_DistViolin$plot) %/% 2 + length(all_plots_DistViolin$plot) %% 2, shareX = TRUE, shareY = TRUE)

# Zeige den kombinierten Plot an
combined_plot


# ###ggplot###
# # Test: Durchlaufen der Tibble und Anzeigen der Plots
# for (i in seq_len(nrow(all_plots_DistViolin_gg))) {
#   print(all_plots_DistViolin_gg$plot[[i]])
# }
# 
# # Kombiniere die Plots zu einem einzigen Subplot
# combined_plot_gg <- wrap_plots(all_plots_DistViolin_gg$plot, ncol = 2)
# 
# # Zeige den kombinierten Plot an
# print(combined_plot_gg)
# 
# # Optional: Speichern des kombinierten Plots
# ggsave(filename = "combined_violin_plots_gg.png", plot = combined_plot_gg, width = 20, height = 10)
# 

