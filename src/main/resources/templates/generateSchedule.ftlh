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
            <th>${room.name} (Capacity: ${room.capacity})</th>
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
                    <#list scheduleWithAssignments[room.id + "_" + timeSlot]![] as teacher>
                        ${teacher.name}<br>
                    </#list>
                </td>
            </#list>
            <td>
                <#list breaks![] as teacherOnBreak>
                    ${teacherOnBreak.name}<br>
                </#list>
            </td>
        </tr>
    </#list>
    </tbody>
</table>
</body>
</html>
