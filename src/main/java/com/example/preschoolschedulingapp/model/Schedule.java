package com.example.preschoolschedulingapp.model;

import jakarta.persistence.*;

import java.util.List;
import java.util.Map;

@Entity
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;


    @ElementCollection
    private Map<String, String> entries; // Key: "roomId_timeSlot", Value: "Event Name"

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Room> rooms;

    public Schedule() {}

    public Schedule(String name, Map<String, String> entries) {
        this.name = name;
        this.entries = entries;
    }

    // Getters and setters
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

    public Map<String, String> getEntries() {
        return entries;
    }

    public void setEntries(Map<String, String> entries) {
        this.entries = entries;
    }
}
