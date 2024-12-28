package com.example.preschoolschedulingapp.model;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TimeSlot {
    private LocalTime start;
    private LocalTime end;

    // Constructor
    public TimeSlot(LocalTime start, LocalTime end) {
        this.start = start;
        this.end = end;
    }

    // Factory Method to Parse from String
    public static TimeSlot fromString(String timeSlot) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");

        // Split the time slot string
        String[] times = timeSlot.split(" - ");
        if (times.length != 2) {
            throw new IllegalArgumentException("Invalid time slot format: " + timeSlot);
        }

        // Parse start and end times
        LocalTime start = LocalTime.parse(times[0].trim(), formatter);
        LocalTime end = LocalTime.parse(times[1].trim(), formatter);

        return new TimeSlot(start, end);
    }

    // Getters
    public LocalTime getStart() {
        return start;
    }

    public LocalTime getEnd() {
        return end;
    }

    // Convenience Method: Check Overlap
    public boolean overlapsWith(TimeSlot other) {
        return !this.end.isBefore(other.start) && !this.start.isAfter(other.end);
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
        return start.format(formatter) + " - " + end.format(formatter);
    }
}
