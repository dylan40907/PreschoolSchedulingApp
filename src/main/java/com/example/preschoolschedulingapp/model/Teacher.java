package com.example.preschoolschedulingapp.model;

import jakarta.persistence.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@Entity
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String role; // Lead teacher, TA, etc.

    private LocalTime startTime;

    private LocalTime endTime;

    @ManyToMany // Define a many-to-many relationship with Room
    @JoinTable(
            name = "teacher_preferred_rooms", // Join table name
            joinColumns = @JoinColumn(name = "teacher_id"), // Foreign key for Teacher
            inverseJoinColumns = @JoinColumn(name = "room_id") // Foreign key for Room
    )
    private Set<Room> preferredRooms; // Set of preferred Room objects

    public Teacher() {
    }

    public Teacher(String name, String role, LocalTime startTime, LocalTime endTime, Set<Room> preferredRooms) {
        this.name = name;
        this.role = role;
        this.startTime = startTime;
        this.endTime = endTime;
        this.preferredRooms = preferredRooms;
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

    public Set<Room> getPreferredRooms() {
        return preferredRooms;
    }

    public void setPreferredRooms(Set<Room> preferredRooms) {
        this.preferredRooms = preferredRooms;
    }

    public void setAvailability(String availability) {
        String[] times = availability.split("-");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mma");
        this.startTime = LocalTime.parse(times[0].trim(), formatter);
        this.endTime = LocalTime.parse(times[1].trim(), formatter);
    }
}