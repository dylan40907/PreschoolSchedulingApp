package com.example.preschoolschedulingapp.model;

import java.time.LocalTime;

public class HardRestriction {

    private Teacher teacher;
    private LocalTime startTime;
    private LocalTime endTime;
    private Room room;

    public HardRestriction(Teacher teacher, LocalTime startTime, LocalTime endTime, Room room) {
        this.teacher = teacher;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
    }

    // Getters and Setters
    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
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

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    @Override
    public String toString() {
        return "HardRestriction{" +
                "teacher=" + teacher.getName() +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", room=" + room.getName() +
                '}';
    }
}
