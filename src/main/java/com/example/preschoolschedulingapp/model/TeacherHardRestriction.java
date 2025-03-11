package com.example.preschoolschedulingapp.model;

import jakarta.persistence.Embeddable;
import java.time.LocalTime;

@Embeddable
public class TeacherHardRestriction {

    private LocalTime startTime;
    private LocalTime endTime;
    private Long roomId;

    public TeacherHardRestriction() {}

    public TeacherHardRestriction(LocalTime startTime, LocalTime endTime, Long roomId) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.roomId = roomId;
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
    public Long getRoomId() {
        return roomId;
    }
    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }
}
