<!DOCTYPE html>
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
    </tr>
    </thead>
    <tbody>
    <#list schedules as schedule>
        <tr>
            <td>
                <a href="/editSchedule/${schedule.id}">${schedule.name}</a>
            </td>
        </tr>
    </#list>
    </tbody>
</table>
<a href="/newSchedule">Create a New Schedule</a>
</body>
</html>
