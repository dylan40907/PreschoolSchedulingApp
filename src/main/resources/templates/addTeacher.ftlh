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

    <label>Preferred Rooms:</label><br>
    <#list rooms as room>
        <input type="checkbox" id="room-${room.id}" name="preferredRooms" value="${room.id}">
        <label for="room-${room.id}">${room.name}</label><br>
    </#list>
    <br>

    <label for="noBreakPeriods">No Break Periods (e.g., 09:30 AM-11:00 AM):</label>
    <input type="text" id="noBreakPeriods" name="noBreakPeriods">
    <br><br>

    <label for="requiredTime">Required Time (e.g., 08:00 AM-09:00 AM):</label>
    <input type="text" id="requiredTime" name="requiredTime">
    <br><br>

    <label for="requiredRoom">Required Room:</label>
    <select id="requiredRoom" name="requiredRoom">
        <option value="" selected>None</option>
        <#list rooms as room>
            <option value="${room.id}">${room.name}</option>
        </#list>
    </select>
    <button type="button" onclick="document.getElementById('requiredRoom').value=''">Clear</button>
    <br><br>

    <label for="numTenMinBreaks">Number of Ten-Minute Breaks:</label>
    <input type="number" id="numTenMinBreaks" name="numTenMinBreaks" min="0">
    <br><br>

    <label for="longBreakLength">Long Break Length (minutes):</label>
    <input type="number" id="longBreakLength" name="longBreakLength" min="0">
    <br><br>

    <label for="hasPriority">Has Priority:</label>
    <input type="checkbox" id="hasPriority" name="hasPriority">
    <br><br>


    <!-- TEACHER-LEVEL HARD RESTRICTIONS SECTION -->
    <h2>Teacher Hard Restrictions</h2>
    <div id="thrContainer">
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
    </div>
    <button type="button" onclick="addThr()">Add Another Hard Restriction</button>
    <br><br>
    <!-- END TEACHER HR SECTION -->

    <button type="submit">Add Teacher</button>
</form>

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
        if (document.querySelectorAll('.thrItem').length > 1) {
            div.remove();
        } else {
            // just clear fields if only one
            div.querySelectorAll('input').forEach(i => i.value='');
            div.querySelector('select').selectedIndex = 0;
        }
    }
</script>
