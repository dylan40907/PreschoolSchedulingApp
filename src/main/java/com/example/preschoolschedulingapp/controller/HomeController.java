package com.example.preschoolschedulingapp.controller;

import com.example.preschoolschedulingapp.model.*;
import com.example.preschoolschedulingapp.repository.RoomRepository;
import com.example.preschoolschedulingapp.repository.ScheduleRepository;
import com.example.preschoolschedulingapp.repository.TeacherRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final ScheduleRepository scheduleRepository;
    private final TeacherRepository teacherRepository;
    private final RoomRepository roomRepository;


    public HomeController(ScheduleRepository scheduleRepository, TeacherRepository teacherRepository, RoomRepository roomRepository) {
        this.scheduleRepository = scheduleRepository;
        this.teacherRepository = teacherRepository;
        this.roomRepository = roomRepository;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/home")
    public String home() {
        return "home";
    }

    @RequestMapping("/teacherView")
    public String teacherView(Model model) {
        List<Teacher> teachers = (List<Teacher>) teacherRepository.findAll();
        teachers.forEach(teacher -> {
            if (teacher.getStartTime() == null) {
                teacher.setAvailability("12:00AM-11:59PM"); // Ensure availability is not null
            }
        });

        model.addAttribute("teachers", teachers);
        return "/teacherView";
    }

    @GetMapping("/addTeacher")
    public String addTeacher(Model model) {
        model.addAttribute("teacher", new Teacher());
        model.addAttribute("rooms", roomRepository.findAll());
        return "/addTeacher";
    }

    @PostMapping("/addTeacher")
    public String addTeacher(
            @RequestParam String name, // Capture the name of the teacher
            @RequestParam String role, // Capture the role of the teacher
            @RequestParam String availability,
            @RequestParam List<Long> preferredRooms,
            @RequestParam(required = false) String noBreakPeriods, // Optional parameter
            @RequestParam(required = false) String requiredTime,
            @RequestParam(required = false) Long requiredRoom,
            @RequestParam(required = false, defaultValue = "0") int numTenMinBreaks, // Default to 0
            @RequestParam(required = false, defaultValue = "0") int longBreakLength, // Default to 0
            @RequestParam(required = false, defaultValue = "false") boolean hasPriority
    ) {
        Teacher teacher = new Teacher();

        // Set name and role
        teacher.setName(name);
        teacher.setRole(role);
        teacher.setPriority(hasPriority);

        // Parse availability
        teacher.setAvailability(availability);

        // Parse preferred rooms
        List<Room> rooms = (List<Room>) roomRepository.findAllById(preferredRooms);
        teacher.setPreferredRooms(new HashSet<>(rooms));

        // Parse noBreakPeriods if provided
        if (noBreakPeriods != null && !noBreakPeriods.isEmpty()) {
            teacher.setNoBreakPeriods(parseNoBreakPeriods(noBreakPeriods));
        } else {
            teacher.setNoBreakPeriods(new HashMap<>()); // Default to empty map
        }

        // Parse required time if provided
        if (requiredTime != null && !requiredTime.isEmpty()) {
            String[] times = requiredTime.split("-");
            teacher.setRequiredTimeStart(LocalTime.parse(times[0].trim(), DateTimeFormatter.ofPattern("hh:mm a")));
            teacher.setRequiredTimeEnd(LocalTime.parse(times[1].trim(), DateTimeFormatter.ofPattern("hh:mm a")));
        }

        // Set required room if provided
        if (requiredRoom != null) {
            Room room = roomRepository.findById(requiredRoom).orElse(null);
            teacher.setRequiredRoom(room);
        }

        // Set numTenMinBreaks and longBreakLength
        teacher.setNumTenMinBreaks(numTenMinBreaks);
        teacher.setLongBreakLength(longBreakLength);

        teacherRepository.save(teacher);
        return "redirect:/teacherView";
    }



    @GetMapping("/editTeacher/{id}")
    public String editTeacher(@PathVariable Long id, Model model) {
        Teacher teacher = teacherRepository.findById(id).orElse(null);
        List<Room> rooms = (List<Room>) roomRepository.findAll(); // Fetch all rooms
        model.addAttribute("teacher", teacher);
        model.addAttribute("rooms", rooms); // Pass rooms to the model

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");

        System.out.println("Teacher: " + teacher.getNoBreakPeriods());

        // Format availability
        String availability = "";
        if (teacher.getStartTime() != null && teacher.getEndTime() != null) {
            availability = teacher.getStartTime().format(formatter) + " - " + teacher.getEndTime().format(formatter);
        }
        model.addAttribute("availability", availability);

        // Format required time
        String requiredTime = "";
        if (teacher.getRequiredTimeStart() != null && teacher.getRequiredTimeEnd() != null) {
            requiredTime = teacher.getRequiredTimeStart().format(formatter) + " - " + teacher.getRequiredTimeEnd().format(formatter);
        }
        model.addAttribute("requiredTime", requiredTime);

        // Convert noBreakPeriods to String
        Map<String, String> noBreakPeriodsAsString = new HashMap<>();
        if (teacher.getNoBreakPeriods() != null) {
            noBreakPeriodsAsString = teacher.getNoBreakPeriods()
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey().format(formatter),
                            e -> e.getValue().format(formatter)
                    ));
        }
        model.addAttribute("noBreakPeriodsAsString", noBreakPeriodsAsString);

        return "editTeacher";
    }


    @PostMapping("/editTeacher/{id}")
    public String updateTeacher(
            @PathVariable Long id,
            @RequestParam String name, // Capture the updated name of the teacher
            @RequestParam String role, // Capture the updated role of the teacher
            @RequestParam String availability,
            @RequestParam List<Long> preferredRooms,
            @RequestParam(required = false) String noBreakPeriods,
            @RequestParam(required = false) String requiredTime,
            @RequestParam(required = false) Long requiredRoom,
            @RequestParam(required = false, defaultValue = "0") int numTenMinBreaks, // Default to 0
            @RequestParam(required = false, defaultValue = "0") int longBreakLength, // Default to 0
            @RequestParam(required = false, defaultValue = "false") boolean hasPriority
    ) {
        Teacher teacher = teacherRepository.findById(id).orElse(null);

        // Update name and role
        teacher.setName(name);
        teacher.setRole(role);
        teacher.setPriority(hasPriority);

        // Update availability
        String[] times = availability.split("-");
        teacher.setStartTime(LocalTime.parse(times[0].trim(), DateTimeFormatter.ofPattern("hh:mm a")));
        teacher.setEndTime(LocalTime.parse(times[1].trim(), DateTimeFormatter.ofPattern("hh:mm a")));

        // Update preferred rooms
        List<Room> rooms = (List<Room>) roomRepository.findAllById(preferredRooms);
        teacher.setPreferredRooms(new HashSet<>(rooms));

        // Parse noBreakPeriods
        if (noBreakPeriods != null && !noBreakPeriods.isEmpty()) {
            Map<LocalTime, LocalTime> noBreakMap = parseNoBreakPeriods(noBreakPeriods);
            teacher.setNoBreakPeriods(noBreakMap);
        }

        // Parse required time
        if (requiredTime != null && !requiredTime.isEmpty()) {
            String[] reqTimes = requiredTime.split("-");
            teacher.setRequiredTimeStart(LocalTime.parse(reqTimes[0].trim(), DateTimeFormatter.ofPattern("hh:mm a")));
            teacher.setRequiredTimeEnd(LocalTime.parse(reqTimes[1].trim(), DateTimeFormatter.ofPattern("hh:mm a")));
        }

        // Update required room
        if (requiredRoom != null) {
            Room room = roomRepository.findById(requiredRoom).orElse(null);
            teacher.setRequiredRoom(room);
        }

        // Update numTenMinBreaks and longBreakLength
        teacher.setNumTenMinBreaks(numTenMinBreaks);
        teacher.setLongBreakLength(longBreakLength);

        teacherRepository.save(teacher);
        return "redirect:/teacherView";
    }


    private Map<LocalTime, LocalTime> parseNoBreakPeriods(String noBreakPeriods) {
        Map<LocalTime, LocalTime> noBreakMap = new HashMap<>();
        if (noBreakPeriods != null && !noBreakPeriods.trim().isEmpty()) {

            String[] periods = noBreakPeriods.split(",");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
            for (String period : periods) {
                try {
                    String[] times = period.trim().split("-");
                    if (times.length != 2) {
                        System.err.println("Invalid period format: " + period.trim());
                        continue;
                    }
                    LocalTime start = LocalTime.parse(times[0].trim(), formatter);
                    LocalTime end = LocalTime.parse(times[1].trim(), formatter);

                    if (start.isAfter(end)) {
                        System.err.println("Invalid period: Start time is after end time. Period: " + period.trim());
                        continue;
                    }

                    noBreakMap.put(start, end);
                    System.out.println("Parsed period: " + start + " - " + end);
                } catch (Exception e) {
                    System.err.println("Error parsing period: " + period.trim() + ". Exception: " + e.getMessage());
                }
            }
        } else {
            System.out.println("No noBreakPeriods provided or input is empty.");
        }
        return noBreakMap;
    }

    @PostMapping("/deleteTeacher/{id}")
    public String deleteTeacher(@PathVariable Long id) {
        teacherRepository.deleteById(id);
        return "redirect:/teacherView";
    }

    @RequestMapping("/roomView")
    public String roomView(Model model) {
        List<Room> rooms = (List<Room>) roomRepository.findAll();

        model.addAttribute("rooms", rooms);
        return "/roomView";
    }

    @GetMapping("/addRoom")
    public String addRoom(Model model) {
        model.addAttribute("room", new Room());
        return "/addRoom";
    }

    @PostMapping("/addRoom")
    public String addRoom(@RequestParam String name, Model model) {
        Room room = new Room();
        room.setName(name);
        roomRepository.save(room);
        return "redirect:/roomView";
    }

    @GetMapping("/editRoom/{id}")
    public String editRoom(@PathVariable Long id, Model model) {
        Room room = roomRepository.findById(id).orElse(null);
        model.addAttribute("room", room);
        return "editRoom";
    }

    @PostMapping("/editRoom/{id}")
    public String updateRoom(@PathVariable Long id, @RequestParam String name) {
        Room room = roomRepository.findById(id).orElse(null);
        room.setName(name); // Room name remains constant
        roomRepository.save(room);
        return "redirect:/roomView";
    }

    @PostMapping("/deleteRoom/{id}")
    public String deleteRoom(@PathVariable Long id) {
        roomRepository.deleteById(id);
        return "redirect:/roomView";
    }

    @GetMapping("/newSchedule")
    public String newSchedule(Model model) {
        List<Room> rooms = (List<Room>) roomRepository.findAll();
        List<Teacher> teachers = (List<Teacher>) teacherRepository.findAll(); // Fetch teachers

        model.addAttribute("rooms", rooms);
        model.addAttribute("teachers", teachers); // Add teachers to the model
        model.addAttribute("timeSlots", generateTimeSlots());
        return "newSchedule";
    }


    @PostMapping("/newSchedule")
    public String saveSchedule(
            @RequestParam String scheduleName,
            @RequestParam Map<String, String> eventNames,
            @RequestParam Map<String, String> teachersRequired
    ) {
        Schedule schedule = new Schedule();
        schedule.setName(scheduleName);

        List<Entry> entries = new ArrayList<>();
        // Create a map to group keys by roomId and timeSlot
        Map<String, Entry> entryMap = new HashMap<>();

        for (String key : eventNames.keySet()) {
            String[] parts = key.split("_");
            if (parts.length < 2) {
                System.err.println("Invalid key format: " + key);
                continue;
            }

            String roomId = parts[0].replace("entries[", "");
            String timeSlot = parts[1].replace("].eventName", "").replace("].teachersRequired", "");

            // Create a unique identifier for grouping (roomId + timeSlot)
            String entryKey = roomId + "_" + timeSlot;

            // Fetch or create an Entry object for this grouping
            Entry entry = entryMap.getOrDefault(entryKey, new Entry());
            entry.setRoomId(roomId);
            entry.setTimeSlot(timeSlot);
            entry.setSchedule(schedule);

            // Update the entry based on the current key
            if (key.endsWith(".eventName")) {
                entry.setEventName(eventNames.get(key));
            } else if (key.endsWith(".teachersRequired")) {
                try {
                    entry.setTeachersRequired(Integer.parseInt(teachersRequired.get(key)));
                } catch (NumberFormatException e) {
                    entry.setTeachersRequired(0); // Default value
                }
            }

            // Put the updated entry back into the map
            entryMap.put(entryKey, entry);
        }

// Add all entries from the map to the list
        entries.addAll(entryMap.values());

        schedule.setEntries(entries);
        scheduleRepository.save(schedule);
        return "redirect:/viewSchedules";
    }



    @GetMapping("/viewSchedules")
    public String viewSchedules(Model model) {
        List<Schedule> schedules = (List<Schedule>) scheduleRepository.findAll();
        model.addAttribute("schedules", schedules);
        return "viewSchedules";
    }

    @GetMapping("/preGeneration/{scheduleId}")
    public String preGeneration(@PathVariable Long scheduleId, Model model) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found with id: " + scheduleId));
        List<Room> rooms = (List<Room>) roomRepository.findAll();
        List<Teacher> allTeachers = (List<Teacher>) teacherRepository.findAll();

        model.addAttribute("schedule", schedule);
        model.addAttribute("rooms", rooms);
        model.addAttribute("teachers", allTeachers);

        return "preGeneration";
    }



    @GetMapping("/editSchedule/{id}")
    public String editSchedule(@PathVariable Long id, Model model) {
        Schedule schedule = scheduleRepository.findById(id).orElseThrow();
        List<Room> rooms = (List<Room>) roomRepository.findAll();

        // Debugging: Log schedule details and entries
        System.out.println("Editing Schedule: " + schedule.getName());
        for (Entry entry : schedule.getEntries()) {
            System.out.println("Entry: RoomId=" + entry.getRoomId()
                    + ", TimeSlot=" + entry.getTimeSlot()
                    + ", EventName=" + entry.getEventName()
                    + ", TeachersRequired=" + entry.getTeachersRequired());
        }

        model.addAttribute("rooms", rooms);
        model.addAttribute("schedule", schedule);
        model.addAttribute("entries", schedule.getEntries());
        model.addAttribute("timeSlots", generateTimeSlots());
        return "editSchedule";
    }


    @PostMapping("/editSchedule/{id}")
    public String updateSchedule(
            @PathVariable Long id,
            @RequestParam Map<String, String> eventNames,
            @RequestParam Map<String, String> teachersRequired
    ) {
        // Fetch the existing schedule or throw an exception if not found
        Schedule schedule = scheduleRepository.findById(id).orElseThrow();

        // Create a map to group keys by roomId and timeSlot
        Map<String, Entry> entryMap = new HashMap<>();

        for (String key : eventNames.keySet()) {
            String[] parts = key.split("_");
            if (parts.length < 2) {
                System.err.println("Invalid key format: " + key);
                continue;
            }

            String roomId = parts[0].replace("entries[", "");
            String timeSlot = parts[1].replace("].eventName", "").replace("].teachersRequired", "");

            // Create a unique identifier for grouping (roomId + timeSlot)
            String entryKey = roomId + "_" + timeSlot;

            // Fetch or create an Entry object for this grouping
            Entry entry = entryMap.getOrDefault(entryKey, new Entry());
            entry.setRoomId(roomId);
            entry.setTimeSlot(timeSlot);
            entry.setSchedule(schedule);

            // Update the entry based on the current key
            if (key.endsWith(".eventName")) {
                entry.setEventName(eventNames.get(key));
            } else if (key.endsWith(".teachersRequired")) {
                try {
                    entry.setTeachersRequired(Integer.parseInt(teachersRequired.get(key)));
                } catch (NumberFormatException e) {
                    entry.setTeachersRequired(0); // Default value
                }
            }

            // Put the updated entry back into the map
            entryMap.put(entryKey, entry);
        }

        // Update the existing collection in place
        List<Entry> existingEntries = schedule.getEntries();
        existingEntries.clear();
        existingEntries.addAll(entryMap.values());

        // Save the updated schedule
        scheduleRepository.save(schedule);

        return "redirect:/viewSchedules";
    }



    @PostMapping("/deleteSchedule/{id}")
    public String deleteSchedule(@PathVariable Long id) {
        scheduleRepository.deleteById(id);
        return "redirect:/viewSchedules";
    }

    @PostMapping("/generateSchedule/{scheduleId}")
    public String generateSchedule(
            @PathVariable Long scheduleId,
            @RequestParam List<Long> selectedTeachers,
            @RequestParam(required = false) List<Long> restrictionTeachers,
            @RequestParam(required = false) List<String> timeSlots,
            @RequestParam(required = false) List<Long> restrictionRooms,
            Model model
    ) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found with id: " + scheduleId));
        List<Room> rooms = (List<Room>) roomRepository.findAll();
        List<Teacher> allTeachers = (List<Teacher>) teacherRepository.findAllById(selectedTeachers);

        // Map for hard restriction entries
        Map<String, Entry> hardRestrictionEntries = new HashMap<>();

        // Track teacher assignments per time slot to prevent duplicate assignments
        Map<String, Set<Long>> teacherAssignmentsPerSlot = new HashMap<>();

        // Process hard restrictions
        if (restrictionTeachers != null && timeSlots != null && restrictionRooms != null &&
                restrictionTeachers.size() == timeSlots.size() && restrictionTeachers.size() == restrictionRooms.size()) {
            for (int i = 0; i < restrictionTeachers.size(); i++) {
                if (timeSlots.get(i) == null || timeSlots.get(i).isEmpty()) continue;

                String[] times = timeSlots.get(i).split("-");
                if (times.length != 2) continue;

                LocalTime start = LocalTime.parse(times[0].trim(), DateTimeFormatter.ofPattern("hh:mm a"));
                LocalTime end = LocalTime.parse(times[1].trim(), DateTimeFormatter.ofPattern("hh:mm a"));

                int finalI = i;
                Teacher restrictedTeacher = teacherRepository.findById(restrictionTeachers.get(i))
                        .orElseThrow(() -> new IllegalArgumentException("Teacher not found with id: " + restrictionTeachers.get(finalI)));

                for (LocalTime current = start; current.isBefore(end); current = current.plusMinutes(5)) {
                    LocalTime next = current.plusMinutes(5);
                    String timeSlot = current.format(DateTimeFormatter.ofPattern("hh:mm a")) + " - " + next.format(DateTimeFormatter.ofPattern("hh:mm a"));
                    String key = restrictionRooms.get(i) + "_" + timeSlot;

                    Entry existingEntry = hardRestrictionEntries.get(key);
                    if (existingEntry == null) {
                        int finalI1 = i;
                        existingEntry = schedule.getEntries().stream()
                                .filter(entry -> entry.getTimeSlot().equals(timeSlot) && entry.getRoomId().equals(restrictionRooms.get(finalI1).toString()))
                                .findFirst()
                                .orElse(new Entry());
                    }

                    // Combine event name and teachers
                    String originalEventName = existingEntry.getEventName() != null ? existingEntry.getEventName() + ", " : "";
                    existingEntry.setEventName(originalEventName + "Hard Restriction: " + restrictedTeacher.getName());

                    List<Teacher> assignedTeachers = new ArrayList<>(existingEntry.getAssignedTeachers() != null ? existingEntry.getAssignedTeachers() : new ArrayList<>());
                    if (!assignedTeachers.contains(restrictedTeacher)) assignedTeachers.add(restrictedTeacher);

                    existingEntry.setTeachersRequired(existingEntry.getTeachersRequired() > 0 ? existingEntry.getTeachersRequired() : 2);
                    existingEntry.setAssignedTeachers(assignedTeachers);

                    hardRestrictionEntries.put(key, existingEntry);
                    teacherAssignmentsPerSlot.computeIfAbsent(timeSlot, k -> new HashSet<>()).add(restrictedTeacher.getId());
                }
            }
        }

        // Process required times for teachers with priority
        List<Teacher> prioritizedTeachers = allTeachers.stream().filter(Teacher::hasPriority).toList();
        processRequiredTimes(prioritizedTeachers, hardRestrictionEntries, teacherAssignmentsPerSlot, schedule, true);

        // Process remaining required times
        List<Teacher> nonPrioritizedTeachers = allTeachers.stream().filter(t -> !t.hasPriority()).toList();
        processRequiredTimes(nonPrioritizedTeachers, hardRestrictionEntries, teacherAssignmentsPerSlot, schedule, false);

        // ----------------------------------------------------------------------
        // SCHEDULE BREAKS FOR TEACHERS
        // ----------------------------------------------------------------------
        // teacherBreaks: Key = teacherId as String, Value = list of 5-min break entries
        Map<String, List<BreakEntry>> teacherBreaks = new HashMap<>();

        // Generate the full set of possible 5-minute time slots (07:30 AM to 05:55 PM).
        List<String> timeSlotsList = generateTimeSlots();

        // Schedule breaks for each teacher
        for (Teacher teacher : allTeachers) {
            List<BreakEntry> breaksForThisTeacher = scheduleBreaksForTeacher(teacher, hardRestrictionEntries, teacherAssignmentsPerSlot, timeSlotsList);
            teacherBreaks.put(String.valueOf(teacher.getId()), breaksForThisTeacher);

            // Remove teacher from any assigned time if that time is now used for a break
            removeTeacherFromAssignedBreakSlots(teacher, breaksForThisTeacher, hardRestrictionEntries, schedule, teacherAssignmentsPerSlot);
        }

        // Populate remaining schedule entries (regular entries)
        Map<String, List<Entry>> roomAssignments = new HashMap<>();

        // We iterate over the timeSlot list with an index so we can see the "previous" timeslot for continuity
        for (int i = 0; i < timeSlotsList.size(); i++) {
            String timeSlot = timeSlotsList.get(i);

            // Use the updated findAvailableTeachers to exclude those on break
            List<Teacher> availableTeachers = findAvailableTeachers(allTeachers, timeSlot, teacherBreaks);

            // Remove already assigned teachers for this time slot
            Set<Long> assignedTeacherIds = teacherAssignmentsPerSlot.getOrDefault(timeSlot, new HashSet<>());
            availableTeachers.removeIf(teacher -> assignedTeacherIds.contains(teacher.getId()));

            for (Room room : rooms) {
                String key = room.getId() + "_" + timeSlot;

                // [CHANGE #2] - We attempt to preserve continuity by preferring any teacher
                // who was in the same room during the previous timeslot (if i > 0).
                if (i > 0) {
                    String prevTimeSlot = timeSlotsList.get(i - 1);
                    String prevKey = room.getId() + "_" + prevTimeSlot;
                    List<Entry> prevEntries = roomAssignments.getOrDefault(prevKey, new ArrayList<>());
                    // Gather all teachers assigned to that room in the previous timeslot
                    Set<Teacher> preferredContinuityTeachers = new HashSet<>();
                    for (Entry prevEntry : prevEntries) {
                        if (prevEntry.getAssignedTeachers() != null) {
                            preferredContinuityTeachers.addAll(prevEntry.getAssignedTeachers());
                        }
                    }
                    // Now reorder "availableTeachers" so that teachers in preferredContinuityTeachers come first
                    availableTeachers.sort((t1, t2) -> {
                        boolean t1Preferred = preferredContinuityTeachers.contains(t1);
                        boolean t2Preferred = preferredContinuityTeachers.contains(t2);
                        if (t1Preferred && !t2Preferred) return -1;  // t1 goes first
                        if (t2Preferred && !t1Preferred) return 1;   // t2 goes first
                        return 0; // equal
                    });
                }

                if (hardRestrictionEntries.containsKey(key)) {
                    Entry hardEntry = hardRestrictionEntries.get(key);
                    int remainingTeachers = hardEntry.getTeachersRequired() - hardEntry.getAssignedTeachers().size();
                    List<Teacher> additionalTeachers = assignTeachers(availableTeachers, remainingTeachers, timeSlot);
                    hardEntry.getAssignedTeachers().addAll(additionalTeachers);
                    roomAssignments.computeIfAbsent(key, k -> new ArrayList<>()).add(hardEntry);
                    availableTeachers.removeAll(additionalTeachers);
                } else {
                    for (Entry entry : schedule.getEntries()) {
                        if (entry.getTimeSlot().equals(timeSlot) && entry.getRoomId().equals(room.getId().toString())) {
                            List<Teacher> assignedTeachers = assignTeachers(availableTeachers, entry.getTeachersRequired(), timeSlot);
                            entry.setAssignedTeachers(assignedTeachers);
                            roomAssignments.computeIfAbsent(key, k -> new ArrayList<>()).add(entry);
                            availableTeachers.removeAll(assignedTeachers);
                            assignedTeacherIds.addAll(assignedTeachers.stream().map(Teacher::getId).toList());
                        }
                    }
                }
            }
            teacherAssignmentsPerSlot.put(timeSlot, assignedTeacherIds);
        }

        // Build a quick map of teacherId (as String) -> Teacher for the FTL
        Map<String, Teacher> teacherIdMap = allTeachers.stream()
                .collect(Collectors.toMap(
                        t -> String.valueOf(t.getId()),
                        Function.identity()
                ));

        // Add everything needed to the model
        model.addAttribute("schedule", schedule);
        model.addAttribute("rooms", rooms);
        model.addAttribute("scheduleWithAssignments", roomAssignments);
        model.addAttribute("timeSlots", timeSlotsList);
        model.addAttribute("teacherBreaks", teacherBreaks);
        model.addAttribute("teacherIdMap", teacherIdMap);

        return "generateSchedule";
    }

    /**
     * Helper method: Schedules breaks for a single teacher.
     */
    private List<BreakEntry> scheduleBreaksForTeacher(
            Teacher teacher,
            Map<String, Entry> hardRestrictionEntries,
            Map<String, Set<Long>> teacherAssignmentsPerSlot,
            List<String> allTimeSlots
    ) {
        List<BreakEntry> result = new ArrayList<>();

        int longBreakLength = (teacher.getLongBreakLength() != null) ? teacher.getLongBreakLength() : 30;
        int longBreakIncrements = longBreakLength / 5;

        int numTenMinBreaks = (teacher.getNumTenMinBreaks() != null) ? teacher.getNumTenMinBreaks() : 0;
        int shortBreakIncrements = 2; // 10 min / 5

        // Candidate slots (already filtered for constraints)
        List<String> candidateSlots = buildCandidateBreakSlotsForTeacher(teacher, allTimeSlots, hardRestrictionEntries);

        // Ensure we finish the long break by 5 hours from teacher start
        LocalTime latestLongBreakStart = teacher.getStartTime().plusHours(5);

        // Place the long break
        List<String> chosenLongBreak = tryPlaceBreak(candidateSlots, longBreakIncrements, teacher, latestLongBreakStart, true);
        if (!chosenLongBreak.isEmpty()) {
            for (String slot : chosenLongBreak) {
                result.add(new BreakEntry(slot));
                candidateSlots.remove(slot);
            }
        }

        // Place the short breaks
        for (int i = 0; i < numTenMinBreaks; i++) {
            List<String> chosenShortBreak = tryPlaceBreak(candidateSlots, shortBreakIncrements, teacher, null, false);
            if (!chosenShortBreak.isEmpty()) {
                for (String slot : chosenShortBreak) {
                    result.add(new BreakEntry(slot));
                    candidateSlots.remove(slot);
                }
            }
        }

        return result;
    }

    /**
     * Build a list of candidate time slots (5-min increments) for breaks, applying constraints.
     */
    private List<String> buildCandidateBreakSlotsForTeacher(
            Teacher teacher,
            List<String> allTimeSlots,
            Map<String, Entry> hardRestrictionEntries
    ) {
        List<String> candidateSlots = new ArrayList<>();
        LocalTime teacherStart = teacher.getStartTime();
        LocalTime teacherEnd = teacher.getEndTime();

        LocalTime earliestBreak = teacherStart.plusMinutes(90); // skip first 1.5 hours
        LocalTime latestBreakEnd = teacherEnd.minusMinutes(60); // skip last 1 hour

        // Gather the teacher's Hard Restriction timeslots
        Set<String> hardRestrictedSlots = findTeacherHardRestrictedTimeSlots(teacher, hardRestrictionEntries);

        // Expand teacher's noBreakPeriods
        Set<String> noBreakSlots = expandNoBreakPeriods(teacher.getNoBreakPeriods());

        for (String slotStr : allTimeSlots) {
            TimeSlot slot = TimeSlot.fromString(slotStr);

            // Must be within overall teacher availability
            if (slot.getStart().isBefore(teacherStart) || slot.getEnd().isAfter(teacherEnd)) {
                continue;
            }
            // Not before earliestBreak or after latestBreakEnd
            if (slot.getStart().isBefore(earliestBreak) || slot.getEnd().isAfter(latestBreakEnd)) {
                continue;
            }
            // Skip if Hard Restriction timeslot
            if (hardRestrictedSlots.contains(slotStr)) {
                continue;
            }
            // Skip if in teacher's noBreakPeriods
            if (noBreakSlots.contains(slotStr)) {
                continue;
            }
            // Otherwise it's valid
            candidateSlots.add(slotStr);
        }

        return candidateSlots;
    }

    /**
     * Finds a consecutive run of "neededIncrements" in candidateSlots.
     */
    private List<String> tryPlaceBreak(
            List<String> candidateSlots,
            int neededIncrements,
            Teacher teacher,
            LocalTime latestLongBreakStart,
            boolean isLongBreak
    ) {
        List<String> consecutive = new ArrayList<>();
        TimeSlot[] timeSlotsArray = candidateSlots.stream()
                .map(TimeSlot::fromString)
                .sorted(Comparator.comparing(TimeSlot::getStart))
                .toArray(TimeSlot[]::new);

        for (int i = 0; i < timeSlotsArray.length; i++) {
            consecutive.clear();
            consecutive.add(timeSlotsArray[i].toString());
            int j = i + 1;

            while (j < timeSlotsArray.length && consecutive.size() < neededIncrements) {
                TimeSlot prev = TimeSlot.fromString(consecutive.get(consecutive.size() - 1));
                TimeSlot nextCandidate = timeSlotsArray[j];
                if (nextCandidate.getStart().equals(prev.getEnd())) {
                    consecutive.add(nextCandidate.toString());
                } else {
                    break;
                }
                j++;
            }

            if (consecutive.size() == neededIncrements) {
                if (isLongBreak && latestLongBreakStart != null) {
                    TimeSlot lastSlot = TimeSlot.fromString(consecutive.get(consecutive.size() - 1));
                    // Must finish by or before latestLongBreakStart
                    if (lastSlot.getEnd().isAfter(latestLongBreakStart)) {
                        continue;
                    }
                }
                return new ArrayList<>(consecutive);
            }
        }
        return new ArrayList<>();
    }

    /**
     * Remove teacher from assigned timeslots that overlap with break times.
     */
    private void removeTeacherFromAssignedBreakSlots(
            Teacher teacher,
            List<BreakEntry> breaksForThisTeacher,
            Map<String, Entry> hardRestrictionEntries,
            Schedule schedule,
            Map<String, Set<Long>> teacherAssignmentsPerSlot
    ) {
        for (BreakEntry breakEntry : breaksForThisTeacher) {
            String breakTimeSlot = breakEntry.getTimeSlot();

            // Check hard restriction entries
            List<String> keysToCheck = hardRestrictionEntries.keySet().stream()
                    .filter(k -> k.endsWith("_" + breakTimeSlot))
                    .toList();

            for (String key : keysToCheck) {
                Entry entry = hardRestrictionEntries.get(key);
                if (entry != null && entry.getAssignedTeachers() != null && entry.getAssignedTeachers().contains(teacher)) {
                    entry.getAssignedTeachers().remove(teacher);
                    teacherAssignmentsPerSlot.getOrDefault(breakTimeSlot, new HashSet<>()).remove(teacher.getId());
                }
            }

            // Check normal schedule entries
            for (Entry entry : schedule.getEntries()) {
                if (entry.getTimeSlot().equals(breakTimeSlot) && entry.getAssignedTeachers() != null && entry.getAssignedTeachers().contains(teacher)) {
                    entry.getAssignedTeachers().remove(teacher);
                    teacherAssignmentsPerSlot.getOrDefault(breakTimeSlot, new HashSet<>()).remove(teacher.getId());
                }
            }
        }
    }

    /**
     * Returns the set of timeSlot strings in which the teacher has a hard restriction.
     */
    private Set<String> findTeacherHardRestrictedTimeSlots(Teacher teacher, Map<String, Entry> hardRestrictionEntries) {
        Set<String> result = new HashSet<>();
        for (Map.Entry<String, Entry> e : hardRestrictionEntries.entrySet()) {
            Entry entry = e.getValue();
            if (entry.getAssignedTeachers() != null && entry.getAssignedTeachers().contains(teacher)) {
                // e.getKey() is something like "roomId_timeSlot"
                String[] parts = e.getKey().split("_", 2);
                if (parts.length == 2) {
                    result.add(parts[1]);
                }
            }
        }
        return result;
    }

    /**
     * Expand the teacher's noBreakPeriods (Map<LocalTime,LocalTime>) into 5-min increments.
     */
    private Set<String> expandNoBreakPeriods(Map<LocalTime, LocalTime> noBreakPeriods) {
        Set<String> result = new HashSet<>();
        if (noBreakPeriods == null || noBreakPeriods.isEmpty()) {
            return result;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
        for (Map.Entry<LocalTime, LocalTime> entry : noBreakPeriods.entrySet()) {
            LocalTime current = entry.getKey();
            LocalTime end = entry.getValue();
            while (!current.isAfter(end.minusMinutes(5))) {
                LocalTime slotEnd = current.plusMinutes(5);
                String slotStr = current.format(formatter) + " - " + slotEnd.format(formatter);
                result.add(slotStr);
                current = slotEnd;
            }
        }
        return result;
    }

    /**
     * Updated findAvailableTeachers to exclude teachers who are on break.
     * Takes a Map<String,List<BreakEntry>> for teacherBreaks now.
     */
    private List<Teacher> findAvailableTeachers(List<Teacher> teachers, String timeSlot, Map<String, List<BreakEntry>> teacherBreaks) {
        List<Teacher> availableTeachers = new ArrayList<>();
        for (Teacher teacher : teachers) {
            if (isTeacherAvailable(teacher, timeSlot) && !isTeacherOnBreak(teacher, timeSlot, teacherBreaks)) {
                availableTeachers.add(teacher);
            }
        }
        return availableTeachers;
    }

    private boolean isTeacherAvailable(Teacher teacher, String timeSlotString) {
        TimeSlot timeSlot = TimeSlot.fromString(timeSlotString);
        return !teacher.getStartTime().isAfter(timeSlot.getStart()) &&
                !teacher.getEndTime().isBefore(timeSlot.getEnd());
    }

    private boolean isTeacherOnBreak(Teacher teacher, String timeSlot, Map<String, List<BreakEntry>> teacherBreaks) {
        String teacherKey = String.valueOf(teacher.getId());
        List<BreakEntry> breaks = teacherBreaks.getOrDefault(teacherKey, new ArrayList<>());
        for (BreakEntry b : breaks) {
            if (b.getTimeSlot().equals(timeSlot)) {
                return true;
            }
        }
        return false;
    }

    private List<Teacher> assignTeachers(List<Teacher> availableTeachers, int requiredTeachers, String timeSlot) {
        List<Teacher> assignedTeachers = new ArrayList<>();
        for (Teacher teacher : availableTeachers) {
            if (assignedTeachers.size() < requiredTeachers) {
                assignedTeachers.add(teacher);
            } else {
                break;
            }
        }
        return assignedTeachers;
    }

    private List<String> generateTimeSlots() {
        List<String> timeSlots = new ArrayList<>();
        LocalTime startTime = LocalTime.of(7, 30);
        LocalTime endTime = LocalTime.of(17, 55); // 5:55 PM
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");

        while (!startTime.isAfter(endTime)) {
            LocalTime nextTime = startTime.plusMinutes(5);
            String slotString = startTime.format(formatter) + " - " + nextTime.format(formatter);
            timeSlots.add(slotString);
            startTime = nextTime;
        }

        return timeSlots;
    }

    /**
     * [CHANGE #1] - In processRequiredTimes, if a priority teacher is assigned,
     * remove any conflicting non-priority teacher so that teacher can be freed up.
     */
    private void processRequiredTimes(
            List<Teacher> teachers,
            Map<String, Entry> hardRestrictionEntries,
            Map<String, Set<Long>> teacherAssignmentsPerSlot,
            Schedule schedule,
            boolean isPriority
    ) {
        for (Teacher teacher : teachers) {
            if (teacher.getRequiredTimeStart() != null && teacher.getRequiredTimeEnd() != null && teacher.getRequiredRoom() != null) {
                LocalTime start = teacher.getRequiredTimeStart();
                LocalTime end = teacher.getRequiredTimeEnd();
                Room requiredRoom = teacher.getRequiredRoom();

                for (LocalTime current = start; current.isBefore(end); current = current.plusMinutes(5)) {
                    LocalTime next = current.plusMinutes(5);
                    String timeSlot = current.format(DateTimeFormatter.ofPattern("hh:mm a")) + " - " + next.format(DateTimeFormatter.ofPattern("hh:mm a"));
                    String key = requiredRoom.getId() + "_" + timeSlot;

                    Entry existingEntry = hardRestrictionEntries.get(key);
                    if (existingEntry == null) {
                        existingEntry = schedule.getEntries().stream()
                                .filter(entry -> entry.getTimeSlot().equals(timeSlot) && entry.getRoomId().equals(requiredRoom.getId().toString()))
                                .findFirst()
                                .orElse(new Entry());
                    }

                    // If slot is full, but we have a priority teacher, forcibly remove non-priority teacher(s).
                    if (isPriority && existingEntry.getAssignedTeachers() != null
                            && existingEntry.getAssignedTeachers().size() >= existingEntry.getTeachersRequired()) {
                        // Remove any teacher who does NOT have priority from this entry
                        existingEntry.getAssignedTeachers().removeIf(t -> !t.hasPriority());
                        // Also remove them from teacherAssignmentsPerSlot, so they're free for other rooms
                        Set<Long> slotAssignments = teacherAssignmentsPerSlot.getOrDefault(timeSlot, new HashSet<>());
                        Entry finalExistingEntry = existingEntry;
                        slotAssignments.removeIf(tid -> {
                            Teacher assignedT = finalExistingEntry.getAssignedTeachers().stream()
                                    .filter(x -> x.getId().equals(tid))
                                    .findAny().orElse(null);
                            // If we can't find them in assignedTeachers => they were removed
                            return (assignedT == null);
                        });
                        teacherAssignmentsPerSlot.put(timeSlot, slotAssignments);
                    }

                    if (existingEntry.getAssignedTeachers() != null &&
                            existingEntry.getAssignedTeachers().size() >= existingEntry.getTeachersRequired()) {
                        // Still full after the removal => skip
                        continue;
                    }

                    // If not full, or we are a priority teacher who just freed up space, add the teacher
                    if (isPriority || existingEntry.getEventName() == null || !existingEntry.getEventName().contains("Hard Restriction")) {
                        String originalEventName = existingEntry.getEventName() != null ? existingEntry.getEventName() + ", " : "";
                        existingEntry.setEventName(originalEventName + "Required Time: " + teacher.getName());

                        List<Teacher> assignedTeachers = new ArrayList<>(existingEntry.getAssignedTeachers() != null
                                ? existingEntry.getAssignedTeachers()
                                : new ArrayList<>());

                        if (!assignedTeachers.contains(teacher)) assignedTeachers.add(teacher);

                        existingEntry.setTeachersRequired(existingEntry.getTeachersRequired() > 0 ? existingEntry.getTeachersRequired() : 2);
                        existingEntry.setAssignedTeachers(assignedTeachers);

                        hardRestrictionEntries.put(key, existingEntry);
                        teacherAssignmentsPerSlot.computeIfAbsent(timeSlot, k -> new HashSet<>()).add(teacher.getId());
                    }
                }
            }
        }
    }

    /**
     * Make sure this class is public so the FTL can access it without errors.
     */
    public static class BreakEntry {
        private String timeSlot;

        public BreakEntry() {}

        public BreakEntry(String timeSlot) {
            this.timeSlot = timeSlot;
        }

        public String getTimeSlot() {
            return timeSlot;
        }

        public void setTimeSlot(String timeSlot) {
            this.timeSlot = timeSlot;
        }
    }


}
