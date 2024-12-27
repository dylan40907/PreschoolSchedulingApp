<#include "navbar.ftlh">
<h1>Teacher List</h1>

<table>
    <thead>
    <tr>
        <th>Name</th>
        <th>Role</th>
        <th>Availability</th>
        <th>Actions</th> <!-- New column for edit/delete actions -->
    </tr>
    </thead>
    <tbody>
    <#list teachers as teacher>
        <tr>
            <td>${teacher.name}</td>
            <td>${teacher.role}</td>
            <td>
                ${teacher.startTime} - ${teacher.endTime}<br/>
            </td>
            <td>
                <a href="/editTeacher/${teacher.id}">Edit</a> <!-- Link to edit page -->
            </td>
        </tr>
    </#list>
    </tbody>
</table>

<a href="/addTeacher">Add New Teacher</a>
