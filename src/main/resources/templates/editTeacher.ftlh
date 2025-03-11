<#include "navbar.ftlh">
<h1>Edit Teacher</h1>

<form action="/editTeacher/${teacher.id}" method="post">
    <label for="name">Name:</label>
    <input type="text" id="name" name="name" value="${teacher.name}">
    <br><br>

    <label for="role">Role:</label>
    <input type="text" id="role" name="role" value="${teacher.role}">
    <br><br>

    <label for="availability">Availability:</label>
    <input type="text" id="availability" name="availability" value="${availability}">
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
        <label for="room-${room.id}">${room.name}</label><br>
    </#list>
    <br>

    <label for="noBreakPeriods">No Break Periods:</label>
    <input type="text" id="noBreakPeriods" name="noBreakPeriods"
           value="<#list noBreakPeriodsAsString?keys as startTime>${startTime}-${noBreakPeriodsAsString[startTime]}<#if startTime_has_next>, </#if></#list>">
    <br><br>

    <label for="requiredTime">Required Time:</label>
    <input type="text" id="requiredTime" name="requiredTime" value="${requiredTime}">
    <br><br>

    <label for="requiredRoom">Required Room:</label>
    <select id="requiredRoom" name="requiredRoom">
        <option value="">None</option>
        <#list rooms as room>
            <option value="${room.id}" <#if teacher.requiredRoom?exists && teacher.requiredRoom.id == room.id>selected</#if>>${room.name}</option>
        </#list>
    </select>
    <button type="button" onclick="document.getElementById('requiredRoom').value=''">Clear</button>
    <br><br>

    <label for="numTenMinBreaks">Number of Ten-Minute Breaks:</label>
    <input type="number" id="numTenMinBreaks" name="numTenMinBreaks" value="${teacher.numTenMinBreaks!0}" min="0">
    <br><br>

    <label for="longBreakLength">Long Break Length:</label>
    <input type="number" id="longBreakLength" name="longBreakLength" value="${teacher.longBreakLength!0}" min="0">
    <br><br>

    <label for="hasPriority">Has Priority:</label>
    <input type="checkbox" id="hasPriority" name="hasPriority" value="true" <#if teacher.hasPriority()>checked</#if>>
    <br><br>

    <!-- TEACHER-LEVEL HARD RESTRICTIONS -->
    <h2>Teacher Hard Restrictions</h2>
    <div id="thrContainer">
        <#if teacher.hardRestrictions?has_content>
            <#list teacher.hardRestrictions as thr>
                <div class="thrItem">
                    <label>Start:</label>
                    <input type="text" name="tHardStart" value="${thr.startTime?string('hh:mm a')}">
                    <label>End:</label>
                    <input type="text" name="tHardEnd" value="${thr.endTime?string('hh:mm a')}">
                    <label>Room:</label>
                    <select name="tHardRoom">
                        <option value="">Select Room</option>
                        <#list rooms as r>
                            <option value="${r.id}" <#if r.id == thr.roomId>selected</#if>>${r.name}</option>
                        </#list>
                    </select>
                    <button type="button" onclick="removeThr(this)">X</button>
                </div>
            </#list>
        <#else>
            <!-- If none, display a single blank row -->
            <div class="thrItem">
                <label>Start:</label>
                <input type="text" name="tHardStart" placeholder="09:00 AM">
                <label>End:</label>
                <input type="text" name="tHardEnd" placeholder="10:00 AM">
                <label>Room:</label>
                <select name="tHardRoom">
                    <option value="">Select Room</option>
                    <#list rooms as r>
                        <option value="${r.id}">${r.name}</option>
                    </#list>
                </select>
                <button type="button" onclick="removeThr(this)">X</button>
            </div>
        </#if>
    </div>
    <button type="button" onclick="addThr()">Add Another Hard Restriction</button>
    <br><br>
    <!-- END TEACHER HR -->

    <button type="submit">Save</button>
</form>

<form action="/deleteTeacher/${teacher.id}" method="post"
      onsubmit="return confirm('Are you sure you want to delete this teacher?');">
    <button type="submit" style="color: red;">Delete</button>
</form>

<a href="/teacherView">Cancel</a>

<script>
    function addThr() {
        const container = document.getElementById('thrContainer');
        const original = container.querySelector('.thrItem');
        const clone = original.cloneNode(true);
        // clear the cloned inputs
        clone.querySelectorAll('input').forEach(i => i.value = '');
        clone.querySelector('select').selectedIndex = 0;
        container.appendChild(clone);
    }

    function removeThr(btn) {
        const div = btn.closest('.thrItem');
        const allItems = document.querySelectorAll('.thrItem');
        if (allItems.length > 1) {
            div.remove();
        } else {
            // if only 1, just clear
            div.querySelectorAll('input').forEach(i => i.value='');
            div.querySelector('select').selectedIndex = 0;
        }
    }
</script>
