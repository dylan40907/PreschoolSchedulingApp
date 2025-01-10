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
                <!-- List which teachers are on break during this timeSlot -->
                <#if teacherBreaks?has_content>
                    <!-- teacherBreaks?keys returns a list of String IDs (ex: "1","2",...) -->
                    <#list teacherBreaks?keys as teacherId>
                        <!-- Retrieve the list of breaks for teacherId -->
                        <#assign breakList = teacherBreaks[teacherId]![]>
                        <#list breakList as breakEntry>
                        <#-- If this break's timeSlot matches the current timeSlot -->
                            <#if breakEntry.timeSlot == timeSlot>
                                <!-- Retrieve the teacher object from teacherIdMap -->
                                <#assign teacherObj = teacherIdMap[teacherId]!>
                                ${teacherObj.name} (Break)<br>
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
