<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Edit Schedule</title>
</head>
<body>
<#include "navbar.ftlh">
<h1>Edit Schedule: ${schedule.name}</h1>

<form action="/editSchedule/${schedule.id}" method="post">
    <table border="1">
        <thead>
        <tr>
            <th>Time Slot</th>
            <#list rooms as room>
                <th>${room.name} (Capacity: ${room.capacity})</th>
            </#list>
        </tr>
        </thead>
        <tbody>
        <#list timeSlots as timeSlot>
            <tr>
                <td>${timeSlot}</td>
                <#list rooms as room>
                    <td>
                        <#assign entryFound = false />
                        <#list entries as entry>
                            <#if entry.roomId == room.id?string && entry.timeSlot == timeSlot>
                                <!-- Event Name -->
                                <label>
                                    Event:
                                    <input type="text"
                                           name="entries[${room.id}_${timeSlot}].eventName"
                                           value="${entry.eventName!}"
                                           placeholder="Enter class/event"
                                           >
                                </label>
                                <br>
                                <!-- Teachers Required -->
                                <label>
                                    Teachers Required:
                                    <input type="number"
                                           name="entries[${room.id}_${timeSlot}].teachersRequired"
                                           value="${entry.teachersRequired!0}"
                                           min="0"
                                           placeholder="Number of teachers"
                                           >
                                </label>
                                <br>
                                <#assign entryFound = true />
                            </#if>
                        </#list>
                        <!-- Empty Inputs for Missing Entries -->
                        <#if !entryFound>
                            <label>
                                Event:
                                <input type="text"
                                       name="entries[${room.id}_${timeSlot}].eventName"
                                       placeholder="Enter class/event"
                                       >
                            </label>
                            <br>
                            <label>
                                Teachers Required:
                                <input type="number"
                                       name="entries[${room.id}_${timeSlot}].teachersRequired"
                                       min="0"
                                       placeholder="Number of teachers"
                                       >
                            </label>
                            <br>
                        </#if>
                    </td>
                </#list>
            </tr>
        </#list>
        </tbody>
    </table>

    <button type="submit">Update Schedule</button>
</form>

<form action="/deleteSchedule/${schedule.id}" method="post" onsubmit="return confirm('Are you sure you want to delete this schedule?');">
    <button type="submit" style="color: red;">Delete</button>
</form>

<a href="/viewSchedules">Cancel</a>
</body>
</html>
