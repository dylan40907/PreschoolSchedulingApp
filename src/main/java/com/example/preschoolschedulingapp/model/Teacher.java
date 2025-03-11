package com.example.preschoolschedulingapp.model;

import jakarta.persistence.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
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

    private LocalTime requiredTimeStart;
    private LocalTime requiredTimeEnd;

    private boolean hasPriority;

    @ManyToOne
    @JoinColumn(name = "required_room_id")
    private Room requiredRoom;

    @ManyToMany
    @JoinTable(
            name = "teacher_preferred_rooms",
            joinColumns = @JoinColumn(name = "teacher_id"),
            inverseJoinColumns = @JoinColumn(name = "room_id")
    )
    private Set<Room> preferredRooms;

    @ElementCollection
    @CollectionTable(name = "teacher_no_break_periods", joinColumns = @JoinColumn(name = "teacher_id"))
    @MapKeyColumn(name = "start_time")
    @Column(name = "end_time")
    private Map<LocalTime, LocalTime> noBreakPeriods;

    private Integer numTenMinBreaks;
    private Integer longBreakLength;

    /**
     *  NEW: a list of teacher-level hard restrictions
     *  stored in the DB table teacher_hard_restrictions
     */
    @ElementCollection
    @CollectionTable(name = "teacher_hard_restrictions",
            joinColumns = @JoinColumn(name="teacher_id"))
    private List<TeacherHardRestriction> hardRestrictions;

    public Teacher() {
    }

    public Teacher(String name, String role, LocalTime startTime, LocalTime endTime,
                   LocalTime requiredTimeStart, LocalTime requiredTimeEnd,
                   Room requiredRoom, Set<Room> preferredRooms,
                   Map<LocalTime, LocalTime> noBreakPeriods,
                   Integer numTenMinBreaks, Integer longBreakLength,
                   boolean hasPriority) {
        this.name = name;
        this.role = role;
        this.startTime = startTime;
        this.endTime = endTime;
        this.requiredTimeStart = requiredTimeStart;
        this.requiredTimeEnd = requiredTimeEnd;
        this.requiredRoom = requiredRoom;
        this.preferredRooms = preferredRooms;
        this.noBreakPeriods = noBreakPeriods;
        this.numTenMinBreaks = numTenMinBreaks;
        this.longBreakLength = longBreakLength;
        this.hasPriority = hasPriority;
    }

    // --------------- Getters + Setters ---------------
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public LocalTime getRequiredTimeStart() { return requiredTimeStart; }
    public void setRequiredTimeStart(LocalTime requiredTimeStart) { this.requiredTimeStart = requiredTimeStart; }

    public LocalTime getRequiredTimeEnd() { return requiredTimeEnd; }
    public void setRequiredTimeEnd(LocalTime requiredTimeEnd) { this.requiredTimeEnd = requiredTimeEnd; }

    public boolean hasPriority() { return hasPriority; }
    public void setPriority(boolean hasPriority) { this.hasPriority = hasPriority; }

    public Room getRequiredRoom() { return requiredRoom; }
    public void setRequiredRoom(Room requiredRoom) { this.requiredRoom = requiredRoom; }

    public Set<Room> getPreferredRooms() { return preferredRooms; }
    public void setPreferredRooms(Set<Room> preferredRooms) { this.preferredRooms = preferredRooms; }

    public Map<LocalTime, LocalTime> getNoBreakPeriods() { return noBreakPeriods; }
    public void setNoBreakPeriods(Map<LocalTime, LocalTime> noBreakPeriods) { this.noBreakPeriods = noBreakPeriods; }

    public Integer getNumTenMinBreaks() { return numTenMinBreaks; }
    public void setNumTenMinBreaks(Integer numTenMinBreaks) { this.numTenMinBreaks = numTenMinBreaks; }

    public Integer getLongBreakLength() { return longBreakLength; }
    public void setLongBreakLength(Integer longBreakLength) { this.longBreakLength = longBreakLength; }

    public void setAvailability(String availability) {
        String[] times = availability.split("-");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
        this.startTime = LocalTime.parse(times[0].trim(), formatter);
        this.endTime = LocalTime.parse(times[1].trim(), formatter);
    }

    public void addNoBreakPeriod(String period) {
        String[] times = period.split("-");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
        LocalTime start = LocalTime.parse(times[0].trim(), formatter);
        LocalTime end = LocalTime.parse(times[1].trim(), formatter);
        noBreakPeriods.put(start, end);
    }

    // NEW: teacher-level Hard Restrictions
    public List<TeacherHardRestriction> getHardRestrictions() {
        return hardRestrictions;
    }
    public void setHardRestrictions(List<TeacherHardRestriction> hardRestrictions) {
        this.hardRestrictions = hardRestrictions;
    }
}
