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

        // Map to replace entries with hard restrictions
        Map<String, Entry> hardRestrictionEntries = new HashMap<>();
        Map<String, Set<Long>> teacherAssignmentsPerSlot = new HashMap<>();

        // Validate and process hard restrictions
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

                    // Check for existing entry or create a new one
                    int finalI1 = i;
                    Entry existingEntry = hardRestrictionEntries.getOrDefault(key, schedule.getEntries().stream()
                            .filter(entry -> entry.getTimeSlot().equals(timeSlot) && entry.getRoomId().equals(restrictionRooms.get(finalI1).toString()))
                            .findFirst()
                            .orElse(new Entry()));

                    // Update entry
                    String originalEventName = existingEntry.getEventName() != null ? existingEntry.getEventName() + ", " : "";
                    existingEntry.setEventName(originalEventName + "Hard Restriction: " + restrictedTeacher.getName());

                    List<Teacher> assignedTeachers = new ArrayList<>(existingEntry.getAssignedTeachers() != null ? existingEntry.getAssignedTeachers() : new ArrayList<>());
                    if (!assignedTeachers.contains(restrictedTeacher)) assignedTeachers.add(restrictedTeacher);

                    existingEntry.setTeachersRequired(Math.max(existingEntry.getTeachersRequired(), assignedTeachers.size()));
                    existingEntry.setAssignedTeachers(assignedTeachers);

                    hardRestrictionEntries.put(key, existingEntry);
                    teacherAssignmentsPerSlot.computeIfAbsent(timeSlot, k -> new HashSet<>()).add(restrictedTeacher.getId());
                }
            }
        }

        // Process required times (prioritized first)
        List<Teacher> prioritizedTeachers = allTeachers.stream().filter(Teacher::hasPriority).toList();
        processRequiredTimes(prioritizedTeachers, hardRestrictionEntries, teacherAssignmentsPerSlot, schedule);

        // Process remaining required times (non-prioritized)
        List<Teacher> nonPrioritizedTeachers = allTeachers.stream().filter(t -> !t.hasPriority()).toList();
        processRequiredTimes(nonPrioritizedTeachers, hardRestrictionEntries, teacherAssignmentsPerSlot, schedule);

        // Populate room assignments
        Map<String, List<Entry>> roomAssignments = new HashMap<>();
        List<String> timeSlotsList = generateTimeSlots();

        for (String timeSlot : timeSlotsList) {
            List<Teacher> availableTeachers = findAvailableTeachers(allTeachers, timeSlot);

            // Remove already assigned teachers for this time slot
            Set<Long> assignedTeacherIds = teacherAssignmentsPerSlot.getOrDefault(timeSlot, new HashSet<>());
            availableTeachers.removeIf(teacher -> assignedTeacherIds.contains(teacher.getId()));

            for (Room room : rooms) {
                String key = room.getId() + "_" + timeSlot;

                // Combine logic for hardRestrictionEntries and schedule.getEntries()
                Entry entry = hardRestrictionEntries.getOrDefault(key, schedule.getEntries().stream()
                        .filter(e -> e.getTimeSlot().equals(timeSlot) && e.getRoomId().equals(room.getId().toString()))
                        .findFirst()
                        .orElse(null));

                if (entry != null) {
                    // Fill remaining teachers if needed
                    List<Teacher> additionalTeachers = assignTeachers(
                            availableTeachers,
                            entry.getTeachersRequired() - entry.getAssignedTeachers().size(),
                            timeSlot
                    );
                    entry.getAssignedTeachers().addAll(additionalTeachers);
                    availableTeachers.removeAll(additionalTeachers);

                    roomAssignments.computeIfAbsent(key, k -> new ArrayList<>()).add(entry);

                    // Track assigned teachers
                    assignedTeacherIds.addAll(additionalTeachers.stream().map(Teacher::getId).toList());
                }
            }

            teacherAssignmentsPerSlot.put(timeSlot, assignedTeacherIds);
        }

        model.addAttribute("schedule", schedule);
        model.addAttribute("rooms", rooms);
        model.addAttribute("scheduleWithAssignments", roomAssignments);
        model.addAttribute("timeSlots", timeSlotsList);

        return "generateSchedule";
    }

    private void processRequiredTimes(
            List<Teacher> teachers,
            Map<String, Entry> hardRestrictionEntries,
            Map<String, Set<Long>> teacherAssignmentsPerSlot,
            Schedule schedule
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

                    // Check existing or create a new entry
                    Entry existingEntry = hardRestrictionEntries.getOrDefault(key, schedule.getEntries().stream()
                            .filter(entry -> entry.getTimeSlot().equals(timeSlot) && entry.getRoomId().equals(requiredRoom.getId().toString()))
                            .findFirst()
                            .orElse(new Entry()));

                    if (existingEntry.getAssignedTeachers() != null &&
                            existingEntry.getAssignedTeachers().size() >= existingEntry.getTeachersRequired()) {
                        continue; // Skip if the slot is already full
                    }

                    String originalEventName = existingEntry.getEventName() != null ? existingEntry.getEventName() + ", " : "";
                    existingEntry.setEventName(originalEventName + "Required Time: " + teacher.getName());

                    List<Teacher> assignedTeachers = new ArrayList<>(existingEntry.getAssignedTeachers() != null ? existingEntry.getAssignedTeachers() : new ArrayList<>());
                    if (!assignedTeachers.contains(teacher)) assignedTeachers.add(teacher);

                    existingEntry.setTeachersRequired(Math.max(existingEntry.getTeachersRequired(), assignedTeachers.size()));
                    existingEntry.setAssignedTeachers(assignedTeachers);

                    hardRestrictionEntries.put(key, existingEntry);
                    teacherAssignmentsPerSlot.computeIfAbsent(timeSlot, k -> new HashSet<>()).add(teacher.getId());
                }
            }
        }
    }





    private Map<Teacher, List<Entry>> allocateBreaks(
            List<Teacher> allTeachers,
            Map<String, List<Entry>> roomAssignments,
            List<String> timeSlots
    ) {
        Map<Teacher, List<Entry>> teacherBreaksMap = new HashMap<>();

        for (Teacher teacher : allTeachers) {
            List<Entry> teacherBreaks = new ArrayList<>();
            LocalTime current = teacher.getStartTime();
            LocalTime fiveHourMark = teacher.getStartTime().plusHours(5);

            // Allocate ten-minute breaks
            int tenMinBreaksRemaining = teacher.getNumTenMinBreaks();
            while (tenMinBreaksRemaining > 0) {
                if (canScheduleBreak(teacher, current, roomAssignments)) {
                    addBreakEntries(teacher, teacherBreaks, current, 10);
                    tenMinBreaksRemaining--;
                }
                current = current.plusMinutes(30);
                if (current.isAfter(teacher.getEndTime())) break; // Avoid infinite loop
            }

            // Allocate long break (must occur within five hours)
            current = teacher.getStartTime();
            boolean longBreakScheduled = false;
            while (current.isBefore(fiveHourMark) && !longBreakScheduled) {
                if (canScheduleBreak(teacher, current, roomAssignments)) {
                    addBreakEntries(teacher, teacherBreaks, current, teacher.getLongBreakLength());
                    longBreakScheduled = true;
                }
                current = current.plusMinutes(30);
            }

            // Record breaks in the map
            teacherBreaksMap.put(teacher, teacherBreaks);

            // Remove teacher from conflicting entries and reassign
            for (Entry breakEntry : teacherBreaks) {
                String timeSlot = breakEntry.getTimeSlot();
                for (Entry entry : roomAssignments.values().stream().flatMap(List::stream).toList()) {
                    if (entry.getTimeSlot().equals(timeSlot) && entry.getAssignedTeachers().contains(teacher)) {
                        entry.getAssignedTeachers().remove(teacher);

                        // Reassign available teachers
                        List<Teacher> available = findAvailableTeachers(allTeachers, timeSlot);
                        List<Teacher> replacements = assignTeachers(available, entry.getTeachersRequired(), timeSlot);
                        entry.getAssignedTeachers().addAll(replacements);
                    }
                }
            }
        }

        return teacherBreaksMap;
    }

    private boolean canScheduleBreak(Teacher teacher, LocalTime time, Map<String, List<Entry>> roomAssignments) {
        String timeSlot = generateTimeSlotString(time);

        // Check if time falls in any no-break periods
        for (Map.Entry<LocalTime, LocalTime> noBreak : teacher.getNoBreakPeriods().entrySet()) {
            if (!time.isBefore(noBreak.getKey()) && !time.isAfter(noBreak.getValue())) {
                return false;
            }
        }

        // Check if there are hard restrictions or required time at this time slot
        List<Entry> entries = roomAssignments.getOrDefault(timeSlot, new ArrayList<>());
        for (Entry entry : entries) {
            if (entry.getEventName() != null && (entry.getEventName().contains("Hard Restriction") || entry.getEventName().contains("Required Time"))) {
                return false;
            }
        }

        return true;
    }
    private void addBreakEntries(Teacher teacher, List<Entry> teacherBreaks, LocalTime startTime, int breakLength) {
        System.out.println("Adding break for teacher: " + teacher.getName() + " at " + startTime);
        for (int i = 0; i < breakLength / 5; i++) {
            Entry breakEntry = new Entry();
            breakEntry.setTimeSlot(generateTimeSlotString(startTime.plusMinutes(i * 5)));
            breakEntry.setEventName("Break for " + teacher.getName());
            breakEntry.setAssignedTeachers(Collections.singletonList(teacher));
            teacherBreaks.add(breakEntry);
        }
    }

    private String generateTimeSlotString(LocalTime time) {
        LocalTime next = time.plusMinutes(5);
        return time.format(DateTimeFormatter.ofPattern("hh:mm a")) + " - " + next.format(DateTimeFormatter.ofPattern("hh:mm a"));
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

    private List<Teacher> findAvailableTeachers(List<Teacher> teachers, String timeSlot) {
        List<Teacher> availableTeachers = new ArrayList<>();
        for (Teacher teacher : teachers) {
            if (isTeacherAvailable(teacher, timeSlot)) {
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
    private List<String> generateTimeSlots() {
        List<String> timeSlots = new ArrayList<>();
        LocalTime startTime = LocalTime.of(7, 30); // Start at 7:30 AM
        LocalTime endTime = LocalTime.of(17, 55);   // End at 6:00 PM
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");

        while (!startTime.isAfter(endTime)) {
            LocalTime nextTime = startTime.plusMinutes(5); // Add 10 minutes
            timeSlots.add(startTime.format(formatter) + " - " + nextTime.format(formatter));
            startTime = nextTime; // Move to the next time slot
        }

        return timeSlots;
    }

}
