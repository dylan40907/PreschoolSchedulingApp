<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Rooms</title>
</head>
<body>
<#include "navbar.ftlh">
<h1>Room List</h1>
<table>
    <tr>
        <th>Room Name</th>
        <th>Actions</th> <!-- New column for actions -->
    </tr>
    <#list rooms as room>
        <tr>
            <td>${room.name}</td>
            <td>
                <a href="/editRoom/${room.id}">Edit</a> <!-- Link to edit room -->
            </td>
        </tr>
    </#list>
</table>
<a href="/addRoom">Add New Room</a>
</body>
</html>
