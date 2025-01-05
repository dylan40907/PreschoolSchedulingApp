<#include "navbar.ftlh">
<form action="/editTeacher/${teacher.id}" method="post">
    <label for="name">Name:</label>
    <input type="text" id="name" name="name" value="${teacher.name}">
    <br><br>
    <label for="role">Role:</label>
    <input type="text" id="role" name="role" value="${teacher.role}">
    <br><br>
    <label for="availability">Availability (e.g., 9:00-17:00):</label>
    <input type="text" id="availability" name="availability" value="${teacher.startTime} - ${teacher.endTime}">
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
        <label for="room-${room.id}">${room.name} (Capacity: ${room.capacity})</label><br>
    </#list>
    <br>
    <button type="submit">Save</button>
</form>

<form action="/deleteTeacher/${teacher.id}" method="post" onsubmit="return confirm('Are you sure you want to delete this teacher?');">
    <button type="submit" style="color: red;">Delete</button>
</form>

<a href="/teacherView">Cancel</a>
