package com.example.preschoolschedulingapp.model;

import jakarta.persistence.*;
import java.util.Set;

@Entity
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private int capacity;

    @ManyToMany(mappedBy = "preferredRooms") // Link back to Teacher
    private Set<Teacher> teachersWithPreference; // Teachers who prefer this room

    public Room() {
    }

    public Room(String name, int capacity) {
        this.name = name;
        this.capacity = capacity;
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

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public Set<Teacher> getTeachersWithPreference() {
        return teachersWithPreference;
    }

    public void setTeachersWithPreference(Set<Teacher> teachersWithPreference) {
        this.teachersWithPreference = teachersWithPreference;
    }
}