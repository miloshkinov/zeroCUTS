### Einige Funktionen, die ich ggf. mehrfach benötige

### Relative Änderungen berechnen.
#Funktion um relativeÄnderungen zu berechnen
calcRelChanges <- function(data){
  # Erhalte die numerischen Spalten im kombinierten DataFrame
  numerische_spalten <- sapply(data, is.numeric)
  
  # Extrahiere die Referenzdaten aus dem kombinierten DataFrame
  referenzdaten <- data[data$ScenarioLang == basename(referenz_ordner), numerische_spalten]
  
  # Gehe durch alle Szenarien außer dem Referenzszenario und berechne die relative Änderung
  for (i in which(data$ScenarioLang != basename(referenz_ordner))) {
    for (col in names(data)[numerische_spalten]) {
      abs_wert <- as.numeric(data[i, col])  # Sicherstellen, dass es numerisch ist
      ref_wert <- as.numeric(referenzdaten[[col]])        # Sicherstellen, dass es numerisch ist
      
      # Überprüfen, ob abs_wert und ref_wert numerisch sind
      if (is.na(abs_wert) || is.na(ref_wert)) {
        data[i, col] <- "NA"
      } else if (ref_wert == 0) { # Bei Referenzwert 0 soll das speziell gehandelt werden
        data[i, col] <- paste(abs_wert, " ( -- %)", sep = "")
      } else { # Alles gut, rechne und füge hinzu.
        rel_aenderung <- round(((abs_wert - ref_wert) / ref_wert) * 100, 1)
        data[i, col] <- paste(abs_wert, " (", rel_aenderung, "%)", sep = "")
      }
    }
  }
  data <- data 
}

