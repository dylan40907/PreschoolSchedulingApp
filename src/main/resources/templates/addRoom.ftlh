<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Add Room</title>
</head>
<body>
<#include "navbar.ftlh">
<h1>Add Room</h1>
<form method="post">
    <label for="name">Room Name:</label>
    <input type="text" id="name" name="name" required>
    <br><br>
    <button type="submit">Add Room</button>
</form>
</body>
</html>
