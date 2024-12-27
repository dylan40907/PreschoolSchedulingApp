<!DOCTYPE html>
<html lang="en">
<head>
    <title>Login</title>
</head>
<body>
<#include "navbar.ftlh">
<h1>Login</h1>
<form method="post">
    <div>
        <label for="username">Username:</label>
        <input type="text" id="username" name="username" placeholder="Enter username" required>
    </div>
    <div>
        <label for="password">Password:</label>
        <input type="password" id="password" name="password" placeholder="Enter password" required>
    </div>
    <div>
        <button type="submit">Login</button>
    </div>
    <div>
        <#if param??>
            <#if param.error??>
                <p style="color: red;">Invalid username or password.</p>
            <#elseif param.logout??>
                <p style="color: green;">You have been logged out successfully.</p>
            </#if>
        <#else>
            <p style="color: gray;">Please log in to continue.</p>
        </#if>
    </div>
</form>
</body>
</html>
