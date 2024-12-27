<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Edit Room</title>
</head>
<body>
<#include "navbar.ftlh">
<h1>Edit Room</h1>

<form action="/editRoom/${room.id}" method="post">
    <label for="name">Room Name:</label>
    <input type="text" id="name" name="name" value="${room.name}" readonly>
    <br>

    <label for="capacity">Capacity:</label>
    <input type="number" id="capacity" name="capacity" value="${room.capacity}">
    <br>

    <button type="submit">Save</button>
</form>

<form action="/deleteRoom/${room.id}" method="post" onsubmit="return confirm('Are you sure you want to delete this room?');">
    <button type="submit" style="color: red;">Delete</button>
</form>

<a href="/rooms">Cancel</a>
</body>
</html>
