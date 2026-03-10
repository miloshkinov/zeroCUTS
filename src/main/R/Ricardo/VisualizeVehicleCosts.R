library(ggplot2)
library(patchwork)

# Data
df <- data.frame(
  Vehicle = rep(c("8-tons","18-tons","26-tons","40-tons"), each=2),
  Year = rep(c(2019,2024), times=4),
  Costs = c(100675,143900, 159400,243395, 268329,304107, 358714,344271),
  Range = c(100,174, 80,395, 133,396, 172,450)
)

# set explicit order
df$Vehicle <- factor(df$Vehicle, levels = c("8-tons", "18-tons", "26-tons", "40-tons"))

scale_factor <- max(df$Costs)/max(df$Range)

# Combined plot with dual y-axis
ggplot(df, aes(x = Year, group = Vehicle)) +
  geom_line(aes(y = Costs, color = Vehicle), linewidth = 1.2) +
  geom_point(aes(y = Costs, color = Vehicle), size = 3) +
  geom_line(aes(y = Range*scale_factor, linetype = "Range",
                color = Vehicle), linewidth = 1.2, alpha = 0.9) +
  geom_point(aes(y = Range*scale_factor, color = Vehicle), size = 3, alpha = 0.9) +
  scale_y_continuous("Vehicle acquisition costs (T€) (incl. battery)",
                     labels = function(x) x/1000,
                     sec.axis = sec_axis(~./scale_factor, name = "Range (km)")
  ) +
  scale_x_continuous(breaks = c(2019, 2024)) +
  scale_color_brewer(palette = "Dark2") +
  scale_linetype_manual(values = c("Range" = "dashed"), name = NULL) +
  labs(title = "Vehicle costs vs. range",
       color = "Vehicle type", x = "Year") +
  theme_minimal(base_size = 14) +
  theme(legend.position = "bottom")

# Separate slope charts
p_cost <- ggplot(df, aes(Year, Costs, color = Vehicle, group = Vehicle)) +
  geom_line(linewidth = 1.3) +
  geom_point(size = 3) +
  labs(title = "Development of vehicle acquisition costs (incl. battery costs)", x = NULL, y = "T€") +
  scale_color_brewer(palette = "Dark2") +
  scale_x_continuous(breaks = c(2019, 2024)) +
  scale_y_continuous(labels = function(x) x/1000) +
  theme_minimal(base_size = 14) +
  theme(legend.position = "none")

# Range slope chart
p_range <- ggplot(df, aes(Year, Range, color = Vehicle, group = Vehicle)) +
  geom_line(linewidth = 1.3, linetype = "dashed") +
  geom_point(size = 3) +
  labs(title = "Development of range (without recharging)", x = "Year", y = "km", color = "Vehicle type") +
  scale_color_brewer(palette = "Dark2") +
  scale_x_continuous(breaks = c(2019, 2024)) +
  theme_minimal(base_size = 14) +
  theme(legend.position = "bottom")

p_cost / p_range + plot_layout(heights = c(2, 1))
