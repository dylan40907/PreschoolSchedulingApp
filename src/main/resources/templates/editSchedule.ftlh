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
                        <input type="text"
                               name="entries[${room.id}_${timeSlot}]"
                               value="${entries['entries[' + room.id + '_' + timeSlot + ']']!''}"
                               placeholder="Enter class/event">
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
