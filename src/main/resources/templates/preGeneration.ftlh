<!DOCTYPE html>
<html lang="en">
<head>
    <title>Pre-Generation</title>
</head>
<body>
<#include "navbar.ftlh">
<h1>Pre-Generation: ${schedule.name}</h1>

<form action="/generateSchedule/${schedule.id}" method="post">
    <!-- Deselect Teachers -->
    <h2>Select Teachers for Inclusion:</h2>
    <#list teachers as teacher>
        <input type="checkbox" id="teacher-${teacher.id}" name="selectedTeachers" value="${teacher.id}" checked>
        <label for="teacher-${teacher.id}">${teacher.name}</label><br>
    </#list>

    <!-- Add Hard Restrictions -->
    <h2>Add Hard Restrictions:</h2>
    <div id="hardRestrictions">
        <div>
            <label for="restrictionTeacher">Teacher:</label>
            <select name="restrictionTeachers">
                <option value="">Select Teacher</option>
                <#list teachers as teacher>
                    <option value="${teacher.id}">${teacher.name}</option>
                </#list>
            </select>

            <label for="timeSlot">Time Slot:</label>
            <input type="text" name="timeSlots" placeholder="09:00 AM - 10:00 AM">

            <label for="restrictionRoom">Room:</label>
            <select name="restrictionRooms">
                <option value="">Select Room</option>
                <#list rooms as room>
                    <option value="${room.id}">${room.name}</option>
                </#list>
            </select>
        </div>
    </div>
    <button type="button" onclick="addRestriction()">Add Another Restriction</button>

    <!-- Submit Button -->
    <button type="submit">Generate Schedule</button>
</form>

<script>
    function addRestriction() {
        const restrictionDiv = document.getElementById('hardRestrictions');
        const newRestriction = restrictionDiv.children[0].cloneNode(true);
        restrictionDiv.appendChild(newRestriction);
    }
</script>
</body>
</html>
