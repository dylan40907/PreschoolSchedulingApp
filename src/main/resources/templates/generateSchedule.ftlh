<!DOCTYPE html>
<html lang="en">
<head>
    <title>Generated Schedule</title>
</head>
<body>
<#include "navbar.ftlh">
<h1>Generated Schedule for ${schedule.name}</h1>
<table border="1">
    <thead>
    <tr>
        <th>Time Slot</th>
        <#list rooms as room>
            <th>${room.name}</th>
        </#list>
        <th>Teachers on Break</th>
    </tr>
    </thead>
    <tbody>
    <#list timeSlots as timeSlot>
        <tr>
            <td>${timeSlot}</td>
            <#list rooms as room>
                <td>
                    <!-- Iterate over the entries for this room and time slot -->
                    <#list scheduleWithAssignments[room.id + "_" + timeSlot]![] as entry>
                        <!-- Display the class/event name -->
                        <strong>${entry.eventName}</strong><br>
                        <!-- For each entry, iterate over the assigned teachers -->
                        <#list entry.assignedTeachers![] as teacher>
                            ${teacher.name}<br>
                        </#list>
                        <br> <!-- Add some spacing between events -->
                    </#list>
                </td>
            </#list>
            <td>
                <!-- Check if teacherBreaks exists and has content -->
                <#if teacherBreaks?has_content>
                    <!-- Safely iterate over the keys in teacherBreaks -->
                    <#list teacherBreaks?keys as teacher>
                        <!-- Safely access teacherBreaks[teacher] -->
                        <#list teacherBreaks[teacher]![] as breakEntry>
                            <!-- Check if the current breakEntry's timeSlot matches the timeSlot -->
                            <#if breakEntry.timeSlot == timeSlot>
                                ${teacher.name} (${breakEntry.timeSlot})<br>
                            </#if>
                        </#list>
                    </#list>
                <#else>
                    No breaks
                </#if>
            </td>
        </tr>
    </#list>
    </tbody>
</table>
</body>
</html>