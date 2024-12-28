<!DOCTYPE html>
<html lang="en">
<head>
    <title>Schedules</title>
</head>
<body>
<#include "navbar.ftlh">
<h1>Schedules</h1>
<table border="1">
    <thead>
    <tr>
        <th>Schedule Name</th>
        <th>Actions</th>
    </tr>
    </thead>
    <tbody>
    <#list schedules as schedule>
        <tr>
            <td>
                <a href="/editSchedule/${schedule.id}">${schedule.name}</a>
            </td>
            <td>
                <!-- Edit and Generate Schedule Buttons -->
                <form action="/generateSchedule/${schedule.id}" method="get" style="display: inline;">
                    <button type="submit">Generate Schedule</button>
                </form>
                <form action="/deleteSchedule/${schedule.id}" method="post" onsubmit="return confirm('Are you sure you want to delete this schedule?');" style="display: inline;">
                    <button type="submit" style="color: red;">Delete</button>
                </form>
            </td>
        </tr>
    </#list>
    </tbody>
</table>
<a href="/newSchedule">Create a New Schedule</a>
</body>
</html>
