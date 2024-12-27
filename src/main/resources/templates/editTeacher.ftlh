<#include "navbar.ftlh">
<h1>Edit Teacher</h1>

<form action="/editTeacher/${teacher.id}" method="post">
    <label for="name">Name:</label>
    <input type="text" id="name" name="name" value="${teacher.name}" readonly>
    <br>

    <label for="role">Role:</label>
    <input type="text" id="role" name="role" value="${teacher.role}" readonly>
    <br>

    <label for="availability">Availability (e.g., 9:00-17:00):</label>
    <input type="text" id="availability" name="availability" value="${teacher.startTime} - ${teacher.endTime}">
    <br>

    <button type="submit">Save</button>
</form>

<form action="/deleteTeacher/${teacher.id}" method="post" onsubmit="return confirm('Are you sure you want to delete this teacher?');">
    <button type="submit" style="color: red;">Delete</button>
</form>

<a href="/teacherView">Cancel</a>
