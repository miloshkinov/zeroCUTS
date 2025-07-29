library(lubridate)
library(tidyverse)
library(ggplot2)
library(dplyr)
setwd("C:/Users/erica/shared/shared-svn/studies/countries/de/KiD_2002/Daten")
Fahrten <- read.csv2("KiD_2002_(Einzel)Fahrten-Datei.txt", header = TRUE, sep = "\t", dec = ".")
Ketten <- read.csv2("KiD_2002_Fahrtenketten-Datei.txt", header = TRUE, sep = "\t", dec = ".")
Fahrzeug <- read.csv2("KiD_2002_Fahrzeug-Datei.txt", header = TRUE, sep = "\t", dec = ".")
#View(Fahrten_mit_Wirtschaftszweig)
#head(Fahrten_mit_Wirtschaftszweig, 10)

filter_Wirtschaftszweig <- c("all")
filter_Wirtschaftszweig <- c("A", "B")                                    # Beschäftigte Primärer Sektor
filter_Wirtschaftszweig <- c("F")                                         # Beschäftigte Bau
filter_Wirtschaftszweig <- c("C", "D", "E" )                              # Beschäftigte Sekundärer Sektor (ohne Bau)
filter_Wirtschaftszweig <- c("G")                                         # Beschäftigte Handel
filter_Wirtschaftszweig <- c("I")                                         # Beschäftigte Verkehr und Nachrichten
filter_Wirtschaftszweig <- c("H", "J", "K", "L", "M", "N", "O", "Q")      # Beschäftigte Tertiärer Sektor
filter_Wirtschaftszweig <- c("P")                                         # privater Verkehr

filter_verkehrsmodell <- c("all")
filter_verkehrsmodell <- c("2", "3", "4")                                 # commercialPersonTraffic
filter_verkehrsmodell <- c("1")                                           # goodsTraffic

filter_vehicleTypes <- c(1,2)                                             # Kraftrad + PKW
filter_vehicleTypes <- c(3, 4, 5)                                         # LKW bis  3,5 t Nutzlast,über 3,5 t Nutzlast und Sattelzugmaschinen
filter_vehicleTypes <- c()

filter_Gesamtgewicht <- c("all")

filter_Gesamtgewicht <- c(0, 2800)                                        # vehTyp1
createStopDurations(Ketten, Fahrzeug, Fahrten, filter_Wirtschaftszweig, filter_vehicleTypes, filter_verkehrsmodell, filter_Gesamtgewicht, bins)
filter_Gesamtgewicht <- c(2800, 3500)                                     # vehTyp2
createStopDurations(Ketten, Fahrzeug, Fahrten, filter_Wirtschaftszweig, filter_vehicleTypes, filter_verkehrsmodell, filter_Gesamtgewicht, bins)
filter_Gesamtgewicht <- c(3500, 7500)                                     # vehTyp3
createStopDurations(Ketten, Fahrzeug, Fahrten, filter_Wirtschaftszweig, filter_vehicleTypes, filter_verkehrsmodell, filter_Gesamtgewicht, bins)
filter_Gesamtgewicht <- c(7500, 12000)                                    # vehTyp4
createStopDurations(Ketten, Fahrzeug, Fahrten, filter_Wirtschaftszweig, filter_vehicleTypes, filter_verkehrsmodell, filter_Gesamtgewicht, bins)
filter_Gesamtgewicht <- c(12000, 100000)                                  # vehTyp5
createStopDurations(Ketten, Fahrzeug, Fahrten, filter_Wirtschaftszweig, filter_vehicleTypes, filter_verkehrsmodell, filter_Gesamtgewicht, bins)

bins <- c(0,30,60,90,120,180,240,300,360,420,480,540,600,720,840,Inf)
bins <- c(0,10,20,30,40,50,60,75,90,105,120,150,180,240,300,420,540,660,780,900,Inf)
bins <- c(0,60,120,180,240,300,360,420,480,540,600,660,720,780,840,Inf)

createTourStartDistribution(Fahrten, Fahrzeug, filter_Wirtschaftszweig, filter_vehicleTypes, filter_verkehrsmodell, filter_Gesamtgewicht, bins)
createStopDurations(Ketten, Fahrzeug, Fahrten, filter_Wirtschaftszweig, filter_vehicleTypes, filter_verkehrsmodell, filter_Gesamtgewicht, bins)
createTourDurations(Ketten, Fahrzeug, Fahrten, filter_Wirtschaftszweig, filter_vehicleTypes, filter_verkehrsmodell, filter_Gesamtgewicht, bins)

######################################           TourStartZeit            ##############################

createTourStartDistribution <- function(Fahrten, Fahrzeug, filter_Wirtschaftszweig, filter_vehicleTypes, filter_verkehrsmodell, filter_Gesamtgewicht, bins) {
  Fahrten_mit_Wirtschaftszweig <- Fahrten %>%
    right_join(Fahrzeug, by = 'K00') %>%
    filter(F07b == 1 & F04 != "-1:-1") %>%
    mutate(stunde_start =  as.integer(format(as.POSIXct(F04, format = "%H:%M"), format = "%H"))) %>%
    select(K00,F04,stunde_start, K19b, K01, K03, F07a, K91, K92)
  
  if (!is.null(filter_vehicleTypes)){
    Fahrten_mit_Wirtschaftszweig <- Fahrten_mit_Wirtschaftszweig %>%
      filter(K01 %in% filter_vehicleTypes)
  }
  if ("all" %in% filter_Wirtschaftszweig == FALSE){
    Fahrten_mit_Wirtschaftszweig <- Fahrten_mit_Wirtschaftszweig %>% 
      filter(K19b %in% filter_Wirtschaftszweig)
  }
  
  if ("all" %in% filter_verkehrsmodell == FALSE){
    Fahrten_mit_Wirtschaftszweig <- Fahrten_mit_Wirtschaftszweig %>% 
      filter(F07a %in% filter_verkehrsmodell)
  }
  
  if ("all" %in% filter_Gesamtgewicht == FALSE){
    Fahrten_mit_Wirtschaftszweig <- Fahrten_mit_Wirtschaftszweig %>%
      filter(K03 >= as.integer(filter_Gesamtgewicht[1])) %>%
      filter(K03 < as.integer(filter_Gesamtgewicht[2]))
  }
  
  startzeiten_Beginn <- data.frame(K00 = integer(0), K19b = character(0), stunde_start = integer(0))
  for(i in seq_len(nrow(Fahrten_mit_Wirtschaftszweig))) {
    row <- Fahrten_mit_Wirtschaftszweig[i,]
    if(row$K00 %in% startzeiten_Beginn$K00){
      row_existing <- startzeiten_Beginn[startzeiten_Beginn$K00 == row$K00,]
      if (row$stunde_start < row_existing$stunde_start){
        startzeiten_Beginn[startzeiten_Beginn$K00 == row$K00,] <- row$stunde_start
      }
    }else {
      startzeiten_Beginn <- startzeiten_Beginn %>%
        add_row(K00 = row$K00, K19b = row$K19b, stunde_start = row$stunde_start)
      }
  }
  result_mean <- round(mean(startzeiten_Beginn$stunde_start), digits = 2)
  
  nameWirtschaftsklasse <- nameWirtschaftsklasse(filter_Wirtschaftszweig)
  nameVerkehrsmodell <- nameVerkehrsmodell(filter_verkehrsmodell)
  nameGesamtgewicht <- nameGesamtgewicht(filter_Gesamtgewicht)
  
#  ggplot(data = startzeiten_Beginn, aes(x = stunde_start)) +
#    geom_bar() +
#    labs (title = paste("Verteilung der Fahrtbeginn WV",nameWirtschaftsklasse),
#          x = "Anzahl Wegestarts pro Zeitintervall",
#          tag = paste("Mean: ", toString(result_mean))) +
#    theme(plot.margin = margin(1, 4, 1, 1, "lines"),
#          plot.tag.position = c(.9,.9),
#          plot.tag = element_text(hjust =0, size=15))
  
  ggplot(data = startzeiten_Beginn, aes(x = stunde_start, y = after_stat(count)/sum(after_stat(count))*100)) +
    geom_bar() +
    geom_text(stat='count', aes(label = round(after_stat(count)/sum(after_stat(count))*100, 1), vjust = -0.5)) +
    labs (title = iconv(paste("Fahrtbeginn",  nameVerkehrsmodell, nameWirtschaftsklasse, nameGesamtgewicht), from = "latin1", to = "UTF-8"),
          y = "Anteil in Prozent",
          x = "Zeitintervall",
          tag = paste("Mean: ", toString(result_mean))) +
    theme(plot.margin = margin(1, 4, 1, 1, "lines"),
          plot.tag.position = c(.9,.9),
          plot.tag = element_text(hjust =0, size=15))
}

######################################           Aufenthaltsdauern            ##############################

createStopDurations <- function(Ketten, Fahrzeug, Fahrten, filter_Wirtschaftszweig, filter_vehicleTypes, filter_verkehrsmodell, filter_Gesamtgewicht, bins) {
  
  #Filter Fahrzwecke. Wenn in Tour min. Fahrt den Zweck "1" Transport von Gütern hat, wird die Tour zum GoodsTransport gezählt
  fahrzeug_Fahrzweck <- data.frame(K00 = integer(0), F07a = integer(0))
  Fahrten_dienstlich <- Fahrten %>%
    filter(F07b == 1)
  for(i in seq_len(nrow(Fahrten_dienstlich))) {
    row <- Fahrten_dienstlich[i,]
    if (row$K00 %in% fahrzeug_Fahrzweck$K00){
      if(row$F07a == 1){
        fahrzeug_Fahrzweck[fahrzeug_Fahrzweck$K00 == row$K00,]$F07a <- row$F07a
      }
    }
    else{
      fahrzeug_Fahrzweck <- fahrzeug_Fahrzweck %>%
        add_row(K00 = row$K00, F07a = row$F07a)
    }
  }
  
  Ketten_Aufenthaltsdauer <- Ketten %>%
    right_join(fahrzeug_Fahrzweck, by= 'K00') %>%
    right_join(Fahrzeug, by = 'K00') %>%
    filter(T07 == 1) %>%
    filter(T03 > 0) %>%
    mutate(Anzahl_Stops = as.integer(T06 - 1)) %>%
    mutate(Aufenthalt_pro_Stop = as.integer(round(T03 / Anzahl_Stops, digits = 0))) %>%
    filter(Aufenthalt_pro_Stop != 0) %>%
    select(K00, Aufenthalt_pro_Stop, Anzahl_Stops, K01, K03, K04, H01, T03, T06, F07a)
  
  
  if (!is.null(filter_vehicleTypes)){  
    Ketten_Aufenthaltsdauer <- Ketten_Aufenthaltsdauer %>%
      filter(K01 %in% filter_vehicleTypes)
  }
  
  if ("all" %in% filter_Wirtschaftszweig == FALSE){
    Ketten_Aufenthaltsdauer <- Ketten_Aufenthaltsdauer %>% 
      filter(H01 %in% filter_Wirtschaftszweig)
  }
  
  if ("all" %in% filter_verkehrsmodell == FALSE){
    Ketten_Aufenthaltsdauer <- Ketten_Aufenthaltsdauer %>% 
      filter(F07a %in% filter_verkehrsmodell)
  }
  
  if ("all" %in% filter_Gesamtgewicht == FALSE){
    Ketten_Aufenthaltsdauer <- Ketten_Aufenthaltsdauer %>% 
      filter(K03 >= as.integer(filter_Gesamtgewicht[1])) %>%
      filter(K03 < as.integer(filter_Gesamtgewicht[2]))
  }
  
  Stopp_Dauern <- data.frame(StopDauer = integer(0), Nutzlast = integer(0))
  for(i in seq_len(nrow(Ketten_Aufenthaltsdauer))) {
    row <- Ketten_Aufenthaltsdauer[i,]
    for(i in 1:row$Anzahl_Stops) {
      Stopp_Dauern <- Stopp_Dauern %>%
        add_row(StopDauer = row$Aufenthalt_pro_Stop, Nutzlast = row$K03)
    }
  }

  result_mean <- round(mean(Stopp_Dauern$StopDauer), digits = 2)
  
  Stopp_Dauern$bins <- cut(Stopp_Dauern$StopDauer,breaks = bins,include.lowest = TRUE)
  Stopp_Dauern$binsNutzlast <- cut(Stopp_Dauern$Nutzlast,breaks = c(0,2800,3500,7500,12000,100000),include.lowest = TRUE)
  nameWirtschaftsklasse <- nameWirtschaftsklasse(filter_Wirtschaftszweig)
  nameVerkehrsmodell <- nameVerkehrsmodell(filter_verkehrsmodell)
  nameGesamtgewicht <- nameGesamtgewicht(filter_Gesamtgewicht)
  
#  ggplot(data = Ketten_Aufenthaltsdauer, aes(x = bins)) +
#    geom_bar() +
#    labs (title = paste("Verteilung der Stopp-Dauern ", nameWirtschaftsklasse),
#          x = "Anzahl Wegestarts pro Zeitintervall in Minuten",
#          tag = paste("Mean: ", toString(result_mean))) +
#    theme(plot.margin = margin(1, 4, 1, 1, "lines"),
#          plot.tag.position = c(.9,.9),
#          plot.tag = element_text(hjust =0, size=15))
  
  ggplot(data = Stopp_Dauern, aes(x = bins, y = after_stat(count)/sum(after_stat(count))*100)) +
    geom_bar() +
    #geom_bar(aes(fill = binsNutzlast), position="dodge", labels()) +
    #theme(legend.position = "bottom") +
   # geom_col(position = "dodge") +
    geom_text(stat='count', aes(label = round(after_stat(count)/sum(after_stat(count))*100, 1), vjust = -0.5)) +
    labs (title = iconv(paste("Stopp-Dauern im", nameVerkehrsmodell, nameWirtschaftsklasse, nameGesamtgewicht), from = "latin1", to = "UTF-8"),
          y = "Anteil in Prozent",
          x = "Zeitintervall in Minuten",
          tag = paste("Mean: ", toString(result_mean), "min")) +
    theme(plot.margin = margin(1, 4, 1, 1, "lines"),
          plot.tag.position = c(.9,.9),
          plot.tag = element_text(hjust =0, size=15))
}

######################################           Tour-Dauern            ##############################

createTourDurations <- function(Ketten, Fahrzeug, Fahrten, filter_Wirtschaftszweig, filter_vehicleTypes, filter_verkehrsmodell, filter_Gesamtgewicht, bins) {

  #Filter Fahrzwecke. Wenn in Tour min. Fahrt den Zweck "1" Transport von Gütern hat, wird die Tour zum GoodsTransport gezählt
  fahrzeug_Fahrzweck <- data.frame(K00 = integer(0), F07a = integer(0))
  Fahrten_dienstlich <- Fahrten %>%
    filter(F07b == 1)
  for(i in seq_len(nrow(Fahrten_dienstlich))) {
    row <- Fahrten_dienstlich[i,]
    if (row$K00 %in% fahrzeug_Fahrzweck$K00){
      if(row$F07a == 1){
        fahrzeug_Fahrzweck[fahrzeug_Fahrzweck$K00 == row$K00,]$F07a <- row$F07a
      }
    }
    else{
      fahrzeug_Fahrzweck <- fahrzeug_Fahrzweck %>%
        add_row(K00 = row$K00, F07a = row$F07a)
    }
  }  
  
  Ketten_TourDauer <- Ketten %>%
    right_join(fahrzeug_Fahrzweck, by= 'K00') %>%
    right_join(Fahrzeug, by = 'K00') %>%
    filter(T01 != -1) %>%
    filter(T07 == 1) %>%
    select(K00, K19b, K01, T04, T01, T07, F07a, K03)
  
  if (!is.null(filter_vehicleTypes)){
    Ketten_TourDauer <- Ketten_TourDauer %>%
      filter(K01 %in% filter_vehicleTypes) 
  }
  
  if ("all" %in% filter_Wirtschaftszweig == FALSE){
    Ketten_TourDauer <- Ketten_TourDauer %>% 
      filter(K19b %in% filter_Wirtschaftszweig)
  }
  
  if ("all" %in% filter_verkehrsmodell == FALSE){
    Ketten_TourDauer <- Ketten_TourDauer %>% 
      filter(F07a %in% filter_verkehrsmodell)
  }

  if ("all" %in% filter_Gesamtgewicht == FALSE){
    Ketten_TourDauer <- Ketten_TourDauer %>%
      filter(K03 >= as.integer(filter_Gesamtgewicht[1])) %>%
      filter(K03 < as.integer(filter_Gesamtgewicht[2]))
  }
  tourDauern <- data.frame(K00 = integer(0), K19b = character(0), tourStart = character(0), tourEnde = character(0))
  
  for(i in seq_len(nrow(Ketten_TourDauer))) {
    newRow <- Ketten_TourDauer[i,]
    if(newRow$K00 %in% tourDauern$K00){
      row_existing <- tourDauern[tourDauern$K00 == newRow$K00,]
      newRowTourStart <- as.POSIXct(newRow$T04, format = "%H:%M")
      newRowTourEnde <- newRowTourStart + minutes(newRow$T01)
      testrow_existingtourEnde <- row_existing$tourEnde
      existingTourEnd <- as.POSIXct(row_existing$tourEnde, format = "%H:%M")
      if (newRowTourEnde > existingTourEnd){
        tourDauern[tourDauern$K00 == newRow$K00,]$tourEnde <- format(newRowTourEnde, format = "%H:%M")
      }
    }else {
      thisTourStart <- as.POSIXct(newRow$T04, format = "%H:%M")
      thisTourEnde <- thisTourStart + minutes(newRow$T01)
      thisTourDuration <- difftime(thisTourEnde, thisTourStart, units = "mins")
      tourDauern <- tourDauern %>%
        add_row(K00 = newRow$K00,
                K19b = newRow$K19b,
                tourStart = format(thisTourStart, format = "%H:%M"),
                tourEnde = format(thisTourEnde, format = "%H:%M"))
    }
  }
  tourDauern <- tourDauern %>%
    mutate(tourDauer = as.integer(difftime(as.POSIXct(tourDauern$tourEnde, format = "%H:%M"),
                                           as.POSIXct(tourDauern$tourStart, format = "%H:%M"),
                                           units = "mins")))
  result_mean <- round(mean(tourDauern$tourDauer), digits = 2)
  
  tourDauern$bins <- cut(tourDauern$tourDauer,breaks = bins,include.lowest = TRUE)
  
  nameWirtschaftsklasse <- nameWirtschaftsklasse(filter_Wirtschaftszweig)
  nameVerkehrsmodell <- nameVerkehrsmodell(filter_verkehrsmodell)
  nameGesamtgewicht <- nameGesamtgewicht(filter_Gesamtgewicht)

#  ggplot(data = tourDauern, aes(x = bins)) +
#    geom_bar() +
#    labs (title = paste("Verteilung der Tour-Dauern ", toString(filter_Wirtschaftszweig)),
#          x = "Anzahl Wegestarts pro Zeitintervall",
#          tag = paste("Mean: ", toString(result_mean), "min")) +
#    theme(plot.margin = margin(1, 4, 1, 1, "lines"),
#          plot.tag.position = c(.9,.9),
#          plot.tag = element_text(hjust =0, size=15))

  ggplot(data = tourDauern, aes(x = bins, y = after_stat(count)/sum(after_stat(count))*100)) +
    geom_bar() +
    geom_text(stat='count', aes(label = round(after_stat(count)/sum(after_stat(count))*100, 1), vjust = -0.5)) +
    labs (title = iconv(paste("Tour-Dauern WV", nameWirtschaftsklasse, nameVerkehrsmodell, nameGesamtgewicht), from = "latin1", to = "UTF-8"),
          y = "Anteil in Prozent",
          x = "Zeitintervall",
          tag = paste("Mean: ", toString(result_mean), "min")) +
    theme(plot.margin = margin(1, 4, 1, 1, "lines"),
          plot.tag.position = c(.9,.9),
          plot.tag = element_text(hjust =0, size=15))
}

##################### Helper functions ##########################

nameWirtschaftsklasse <- function(filter_Wirtschaftszweig) {
  if(identical(filter_Wirtschaftszweig, c("A", "B"))){
    nameWirtschaftsklasse <- "im Primären Sektor"
  }else if(identical(filter_Wirtschaftszweig, c("F"))){
    nameWirtschaftsklasse <- "im Bau Sektor"
  }else if(identical(filter_Wirtschaftszweig, c("C", "D", "E"))){
    nameWirtschaftsklasse <- "im Sekündären Sektor (ohne Bau)"
  }else if(identical(filter_Wirtschaftszweig, c("G"))){
    nameWirtschaftsklasse <- "im Sektor Handel"
  }else if(identical(filter_Wirtschaftszweig, c("I"))){
    nameWirtschaftsklasse <- "im Sektor Verkehr und Nachrichten"
  }else if(identical(filter_Wirtschaftszweig, c("H", "J", "K", "L", "M", "N", "O", "Q"))){
    nameWirtschaftsklasse <- "im Tertiären Sektor"
  }else if(identical(filter_Wirtschaftszweig, c("all"))){
    nameWirtschaftsklasse <- "aller Sektoren"
  }else{
    nameWirtschaftsklasse <- toString(filter_Wirtschaftszweig)
  }
}

nameVerkehrsmodell <- function(filter_verkehrsmodell) {
  if(identical(filter_verkehrsmodell, c("1"))){
    nameVerkehrsmodell <- "im kleinräumigen Güterverkehr"
  }else if(identical(filter_verkehrsmodell, c("2", "3", "4"))){
    nameVerkehrsmodell <- "im kleinräumigen Personenwirtschaftsverkehr"
  }else if(identical(filter_verkehrsmodell, c("all"))){
    nameVerkehrsmodell <- "im kleinräumigen Personenwirtschafts- und Güterverkehr"
  }else{
    nameVerkehrsmodell <- toString(filter_verkehrsmodell)
  }
}

nameGesamtgewicht <- function(filter_Gesamtgewicht) {
  if(identical(filter_Gesamtgewicht, c(-1, 0))){
    nameGesamtgewicht <- "(Pkw, Kombi, Lieferwagen (Lkw bis 2,8 t Gesamtgewicht))"
  }else if(identical(filter_Gesamtgewicht, c(0, 2800))){
    nameGesamtgewicht <- "(bis 2,8t)"
  }else if(identical(filter_Gesamtgewicht, c(2800, 3500))){
    nameGesamtgewicht <- "(2,8 - 3,5t)"
  }else if(identical(filter_Gesamtgewicht, c(3500, 7500))){
    nameGesamtgewicht <- "(3,5 - 7,5t)"
  }else if(identical(filter_Gesamtgewicht, c(7500, 12000))){
    nameGesamtgewicht <- "(7,5 - 12t)"
  }else if(identical(filter_Gesamtgewicht, c(12000, 100000))){
    nameGesamtgewicht <- "(>12t)"
  }else if(identical(filter_Gesamtgewicht, c("all"))){
    nameGesamtgewicht <- "(alle Fahrzeugtypen)"
  }else{
    nameGesamtgewicht <- parse(filter_Gesamtgewicht[1], "-", filter_Gesamtgewicht[2])
  }
}