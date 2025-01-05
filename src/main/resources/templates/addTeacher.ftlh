<#include "navbar.ftlh">
<form method="post">
    <label for="name">Name:</label>
    <input type="text" id="name" name="name" required>
    <br><br>
    <label for="role">Role:</label>
    <select id="role" name="role">
        <option value="Lead Teacher">Lead Teacher</option>
        <option value="TA">TA</option>
    </select>
    <br><br>
    <label for="availability">Availability:</label>
    <input type="text" id="availability" name="availability">
    <br><br>
    <label>Preferred Rooms:</label>
    <br>
    <#list rooms as room>
        <input type="checkbox" id="room-${room.id}" name="preferredRooms" value="${room.id}">
        <label for="room-${room.id}">${room.name} (Capacity: ${room.capacity})</label><br>
    </#list>
    <br>
    <button type="submit">Add Teacher</button>
</form>
