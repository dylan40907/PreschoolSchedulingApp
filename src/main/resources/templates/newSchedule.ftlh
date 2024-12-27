<!DOCTYPE html>
<html lang="en">
<head>
    <title>Create New Schedule</title>
</head>
<body>
<#include "navbar.ftlh">
<h1>Create a New Schedule</h1>
<form method="post">
    <div>
        <label for="scheduleName">Schedule Name:</label>
        <input type="text" id="scheduleName" name="scheduleName" placeholder="Enter schedule name" required>
    </div>
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
                        <input type="text" name="entries[${room.id}_${timeSlot}]" placeholder="Enter class/event">
                    </td>
                </#list>
            </tr>
        </#list>
        </tbody>
    </table>
    <button type="submit">Save Schedule</button>
</form>
</body>
</html>
