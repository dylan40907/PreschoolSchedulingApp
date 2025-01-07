package com.example.preschoolschedulingapp.model;

import jakarta.persistence.*;
import java.util.Set;

@Entity
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToMany(mappedBy = "preferredRooms") // Link back to Teacher
    private Set<Teacher> teachersWithPreference; // Teachers who prefer this room

    public Room() {
    }

    public Room(String name) {
        this.name = name;
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

    public Set<Teacher> getTeachersWithPreference() {
        return teachersWithPreference;
    }

    public void setTeachersWithPreference(Set<Teacher> teachersWithPreference) {
        this.teachersWithPreference = teachersWithPreference;
    }
}