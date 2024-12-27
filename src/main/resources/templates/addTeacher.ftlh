<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Add Teacher</title>
</head>
<body>
<#include "navbar.ftlh">
<h1>Add Teacher</h1>
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
    <button type="submit">Add Teacher</button>
</form>
</body>
</html>
