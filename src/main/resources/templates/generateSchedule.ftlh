<!DOCTYPE html>
<html lang="en">
<head>
    <title>Generated Schedule</title>
</head>
<body>
<#include "navbar.ftlh" />
<h1>Generated Schedule for ${schedule.name}</h1>

<table border="1">
    <thead>
    <tr>
        <th>Time Slot</th>
        <#-- We list *all* rooms, including "Break" -->
        <#list rooms as room>
            <th>${room.name}</th>
        </#list>
    </tr>
    </thead>
    <tbody>
    <#list timeSlots as ts>
        <tr>
            <td>${ts}</td>
            <#-- For each room, display the assigned entries -->
            <#list rooms as room>
                <td>
                    <#list scheduleWithAssignments[room.id + "_" + ts]![] as entry>
                        <strong>${entry.eventName}</strong><br/>
                        <#list entry.assignedTeachers![] as t>
                            ${t.name}<br/>
                        </#list>
                        <br/>
                    </#list>
                </td>
            </#list>
        </tr>
    </#list>
    </tbody>
</table>
</body>
</html>
