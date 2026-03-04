library(dplyr)
library(stringr)
library(tidyr)
library(ggplot2)
library(plotly)
library(purrr)
library(tibble)
library(readr)

setwd("C:/Users/erica/shared/matsim-hannover/output/")
run <- "smallScaleCommercialPlans_25pct_1.1_300pCarrier"

# This script should compare the distribution of service durations from different sources (carrier XML, population XML, and CSV) for the given run.
# It extracts the durations, bins them according to Java's standard bins, and then plots the share of each bin for each source.

# ------------------------------------------------
# CONFIG: 4 Quellen (3x xml.gz, 1x csv.gz)
# type = "carrier" | "population" | "csv"
# ------------------------------------------------
sources <- tibble::tibble(
  source = c("carriers", "popBeforeIt", "popAfterIt", "activityCSV"),
  path   = c(
    file.path(run, "hannover-100pct.output_carriers_withPlans.xml.gz"),
    file.path(run, "hannover-small-scale-commercialTraffic-v1.0-100pct.xml.gz"),
    file.path(run, "hannover-100pct.output_plans.xml.gz"),
    file.path(run, "hannover-100pct.output_activities.csv.gz")   # <- ggf. anpassen
  ),
  type   = c("carrier", "population", "population", "csv")
)

# ------------------------------------------------
# Helpers
# ------------------------------------------------
to_minutes_hms <- function(x) {
  parts <- str_split(x, ":", simplify = TRUE)
  as.numeric(parts[, 1]) * 60 + as.numeric(parts[, 2]) + as.numeric(parts[, 3]) / 60
}

# time string -> seconds (supports HH:MM:SS or numeric seconds)
to_seconds_time <- function(x) {
  x <- str_trim(x)
  x[x == "" | is.na(x)] <- NA_character_

  is_hms <- str_detect(x, "^\\d{1,2}:\\d{2}:\\d{2}$")
  out <- rep(NA_real_, length(x))

  if (any(is_hms, na.rm = TRUE)) {
    parts <- str_split(x[is_hms], ":", simplify = TRUE)
    out[is_hms] <- as.numeric(parts[, 1]) * 3600 + as.numeric(parts[, 2]) * 60 + as.numeric(parts[, 3])
  }

  if (any(!is_hms, na.rm = TRUE)) {
    # numeric seconds, allow comma decimals
    out[!is_hms] <- suppressWarnings(as.numeric(str_replace(x[!is_hms], ",", ".")))
  }

  out
}

to_utf8_safe <- function(x) iconv(x, from = "", to = "UTF-8", sub = "byte")

# Java-konforme Bins (Minuten)
bins <- tibble::tibble(
  lo = c(0, 30, 60, 90, 120, 180, 240, 300, 360, 420, 480, 540, 600, 720),
  hi = c(30, 60, 90, 120, 180, 240, 300, 360, 420, 480, 540, 600, 720, 840)
) %>%
  mutate(label = paste0(lo, "-", hi, " min"))

# ------------------------------------------------
# XML.GZ extraction via regex streaming
# ------------------------------------------------
extract_durations_carrier_regex <- function(path, chunk_n = 20000) {
  con <- gzfile(path, open = "rt")
  on.exit(close(con), add = TRUE)

  out <- character(0)
  re <- 'serviceDuration="([0-9]{1,2}:[0-9]{2}:[0-9]{2})"'

  repeat {
    lines <- readLines(con, n = chunk_n, warn = FALSE)
    if (length(lines) == 0) break
    m <- str_match(lines, re)
    hits <- m[, 2]
    hits <- hits[!is.na(hits)]
    if (length(hits)) out <- c(out, hits)
  }
  out
}

extract_durations_population_regex <- function(path, chunk_n = 20000) {
  con <- gzfile(path, open = "rt")
  on.exit(close(con), add = TRUE)

  out <- character(0)

  re_plan_start <- "<plan\\b[^>]*>"
  re_selected   <- 'selected="(yes|true|1)"'
  re_plan_end   <- "</plan>"

  re_act <- '<activity\\b[^>]*\\btype="service"[^>]*\\bmax_dur="([0-9]{1,2}:[0-9]{2}:[0-9]{2})"'

  in_selected <- FALSE
  plan_depth  <- 0L

  repeat {
    lines <- readLines(con, n = chunk_n, warn = FALSE)
    if (length(lines) == 0) break

    for (ln in lines) {
      if (str_detect(ln, re_plan_start)) {
        plan_depth <- plan_depth + 1L
        if (plan_depth == 1L) in_selected <- str_detect(ln, re_selected)
      }

      if (in_selected) {
        m <- str_match(ln, re_act)
        if (!is.na(m[1, 2])) out <- c(out, m[1, 2])
      }

      if (str_detect(ln, re_plan_end) && plan_depth > 0L) {
        plan_depth <- plan_depth - 1L
        if (plan_depth == 0L) in_selected <- FALSE
      }
    }
  }

  out
}

# ------------------------------------------------
# CSV.GZ extraction: duration = end_time - start_time
# ------------------------------------------------
extract_durations_csv_from_times <- function(path) {
  df <- readr::read_delim(
    file = path,
    delim = ";",
    col_types = cols(.default = col_character()),
    show_col_types = FALSE,
    progress = FALSE
  )

  required <- c("activity_type", "start_time", "end_time")
  missing <- setdiff(required, names(df))
  if (length(missing) > 0) {
    stop("CSV missing columns: ", paste(missing, collapse = ", "))
  }

  df_service <- df %>%
    filter(activity_type == "service") %>%
    mutate(
      start_s = to_seconds_time(start_time),
      end_s   = to_seconds_time(end_time)
    ) %>%
    filter(!is.na(start_s), !is.na(end_s))

  # handle wrap-around (e.g. across midnight) defensively
  dur_s <- df_service$end_s - df_service$start_s
  dur_s <- ifelse(dur_s < 0, dur_s + 24 * 3600, dur_s)

  dur_s / 60
}

# ------------------------------------------------
# Bin + shares from minutes
# ------------------------------------------------
binned_shares_from_minutes <- function(dur_min, source_label) {
  dur_min <- dur_min[!is.na(dur_min)]

  bin_idx <- vapply(
    dur_min,
    function(v) which(v >= bins$lo & v < bins$hi)[1],
    integer(1)
  )
  bin_idx <- bin_idx[!is.na(bin_idx)]

  df_counts <- tibble::tibble(bin = bin_idx) %>%
    count(bin, name = "n") %>%
    right_join(tibble::tibble(bin = seq_len(nrow(bins))), by = "bin") %>%
    mutate(n = replace_na(n, 0L)) %>%
    left_join(bins %>% mutate(bin = row_number()), by = "bin") %>%
    arrange(lo)

  total <- sum(df_counts$n)

  df_counts %>%
    mutate(
      source = source_label,
      share = if (total > 0) n / total else 0,
      share_pct = 100 * share,
      label = factor(label, levels = bins$label, ordered = TRUE),
      hover = to_utf8_safe(paste0(
        "Source: ", source_label,
        "<br>Interval: ", as.character(label),
        "<br>Count: ", n,
        "<br>Share: ", sprintf("%.2f", share_pct), "%"
      ))
    )
}

# ------------------------------------------------
# Run all sources
# ------------------------------------------------
df_all <- pmap_dfr(sources, function(source, path, type) {
  if (!file.exists(path)) stop("File not found: ", path)

  if (type == "carrier") {
    dur_chr <- extract_durations_carrier_regex(path)
    dur_min <- to_minutes_hms(dur_chr)
    return(binned_shares_from_minutes(dur_min, source))
  }

  if (type == "population") {
    dur_chr <- extract_durations_population_regex(path)
    dur_min <- to_minutes_hms(dur_chr)
    return(binned_shares_from_minutes(dur_min, source))
  }

  if (type == "csv") {
    dur_min <- extract_durations_csv_from_times(path)
    return(binned_shares_from_minutes(dur_min, source))
  }

  stop("Unknown type: ", type)
})

# ------------------------------------------------
# Interactive plot
# ------------------------------------------------
p <- ggplot(df_all, aes(x = label, y = share, fill = source, text = hover)) +
  geom_col(position = position_dodge(width = 0.9)) +
  scale_y_continuous(labels = function(x) paste0(round(100 * x), "%")) +
  labs(
    title = "serviceDuration distribution (Java bins) - compare sources",
    x = "serviceDuration [min]",
    y = "Share",
    fill = "Source"
  ) +
  theme_minimal()

ggplotly(p, tooltip = "text")
