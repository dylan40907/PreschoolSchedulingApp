<#include "navbar.ftlh">
<h1>Edit Teacher</h1>

<form action="/editTeacher/${teacher.id}" method="post">
    <label for="name">Name:</label>
    <input type="text" id="name" name="name" value="${teacher.name}">
    <br><br>

    <label for="role">Role:</label>
    <input type="text" id="role" name="role" value="${teacher.role}">
    <br><br>

    <label for="availability">Availability (e.g., 09:00 AM-05:00 PM):</label>
    <input type="text" id="availability" name="availability" value="${availability}">
    <br><br>

    <label>Preferred Rooms:</label>
    <br>
    <#list rooms as room>
        <input
                type="checkbox"
                id="room-${room.id}"
                name="preferredRooms"
                value="${room.id}"
                <#if teacher.preferredRooms?seq_contains(room)>checked</#if>
        >
        <label for="room-${room.id}">${room.name}</label><br>
    </#list>
    <br>

    <label>No Break Periods (e.g., 09:30 AM-11:00 AM, 02:00 PM-03:00 PM):</label>
    <input
            type="text"
            id="noBreakPeriods"
            name="noBreakPeriods"
            value="<#list noBreakPeriodsAsString?keys as startTime>${startTime}-${noBreakPeriodsAsString[startTime]}<#if startTime_has_next>, </#if></#list>"
    >
    <br><br>

    <label for="requiredTime">Required Time (e.g., 08:00 AM-09:00 AM):</label>
    <input type="text" id="requiredTime" name="requiredTime" value="${requiredTime}">
    <br><br>

    <label for="requiredRoom">Required Room:</label>
    <select id="requiredRoom" name="requiredRoom">
        <option value="">None</option>
        <#list rooms as room>
            <option value="${room.id}" <#if teacher.requiredRoom?exists && teacher.requiredRoom.id == room.id>selected</#if>>${room.name}</option>
        </#list>
    </select>
    <button type="button" onclick="document.getElementById('requiredRoom').value=''">Clear</button>
    <br><br>

    <label for="numTenMinBreaks">Number of Ten-Minute Breaks:</label>
    <input type="number" id="numTenMinBreaks" name="numTenMinBreaks" value="${teacher.numTenMinBreaks!0}" min="0">
    <br><br>

    <label for="longBreakLength">Long Break Length (minutes):</label>
    <input type="number" id="longBreakLength" name="longBreakLength" value="${teacher.longBreakLength!0}" min="0">
    <br><br>

    <button type="submit">Save</button>
</form>

<form action="/deleteTeacher/${teacher.id}" method="post" onsubmit="return confirm('Are you sure you want to delete this teacher?');">
    <button type="submit" style="color: red;">Delete</button>
</form>

<a href="/teacherView">Cancel</a>
