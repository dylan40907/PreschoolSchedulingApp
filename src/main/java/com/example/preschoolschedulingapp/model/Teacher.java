package com.example.preschoolschedulingapp.model;

import jakarta.persistence.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Entity
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String role; // Lead teacher, TA, etc.

    private boolean lunchBreakWaiver;

    private LocalTime startTime;

    private LocalTime endTime;

    public Teacher() {
    }

    public Teacher(String name, String role, boolean lunchBreakWaiver, LocalTime startTime, LocalTime endTime) {
        this.name = name;
        this.role = role;
        this.lunchBreakWaiver = lunchBreakWaiver;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isLunchBreakWaiver() {
        return lunchBreakWaiver;
    }

    public void setLunchBreakWaiver(boolean lunchBreakWaiver) {
        this.lunchBreakWaiver = lunchBreakWaiver;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public void setAvailability(String availability) {
        String[] times = availability.split("-");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mma");
        this.startTime = LocalTime.parse(times[0].trim(), formatter);
        this.endTime = LocalTime.parse(times[1].trim(), formatter);
    }
}
