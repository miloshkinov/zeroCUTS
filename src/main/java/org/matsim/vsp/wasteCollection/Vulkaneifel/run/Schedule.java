package org.matsim.vsp.wasteCollection.Vulkaneifel.run;

public class Schedule {
    private String name;
    private String day;
    private String week;

    // Constructor
    public Schedule(String name, String day, String week) {
        this.name = name;
        this.day = day;
        this.week = week;
    }

    // Getter for the name attribute
    public String getName() {
        return name;
    }

    // Setter for the name attribute
    public void setName(String name) {
        this.name = name;
    }

    // Getter for the day attribute
    public String getDay() {
        return day;
    }

    // Setter for the day attribute
    public void setDay(String day) {
        this.day = day;
    }

    // Getter for the week attribute
    public String getWeek() {
        return week;
    }

    // Setter for the week attribute
    public void setWeek(String week) {
        this.week = week;
    }
}

