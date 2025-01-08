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
            @RequestParam(required = false, defaultValue = "0") int longBreakLength // Default to 0
    ) {
        Teacher teacher = new Teacher();

        // Set name and role
        teacher.setName(name);
        teacher.setRole(role);

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
            @RequestParam(required = false, defaultValue = "0") int longBreakLength // Default to 0
    ) {
        Teacher teacher = teacherRepository.findById(id).orElse(null);

        // Update name and role
        teacher.setName(name);
        teacher.setRole(role);

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
    public String addRoom(Room room) {
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
            @RequestParam List<Long> selectedTeachers, // IDs of included teachers
            @RequestParam(required = false) List<Long> restrictionTeachers, // Teacher IDs for hard restrictions
            @RequestParam(required = false) List<String> timeSlots, // Time slots for hard restrictions
            @RequestParam(required = false) List<Long> restrictionRooms, // Room IDs for hard restrictions
            Model model
    ) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found with id: " + scheduleId));
        List<Room> rooms = (List<Room>) roomRepository.findAll();
        List<Teacher> allTeachers = (List<Teacher>) teacherRepository.findAllById(selectedTeachers);

        // Map to replace entries with hard restrictions
        Map<String, Entry> hardRestrictionEntries = new HashMap<>();

        // Validate and process hard restrictions
        if (restrictionTeachers != null && timeSlots != null && restrictionRooms != null &&
                restrictionTeachers.size() == timeSlots.size() && restrictionTeachers.size() == restrictionRooms.size()) {

            for (int i = 0; i < restrictionTeachers.size(); i++) {
                if (timeSlots.get(i) == null || timeSlots.get(i).isEmpty()) {
                    continue; // Skip invalid or empty time slots
                }

                String[] times = timeSlots.get(i).split("-");
                if (times.length != 2) {
                    continue; // Skip invalid time slot formats
                }

                LocalTime start;
                LocalTime end;
                try {
                    start = LocalTime.parse(times[0].trim(), DateTimeFormatter.ofPattern("hh:mm a"));
                    end = LocalTime.parse(times[1].trim(), DateTimeFormatter.ofPattern("hh:mm a"));
                } catch (Exception e) {
                    continue; // Skip invalid time parsing
                }

                int finalI = i;
                Teacher restrictedTeacher = teacherRepository.findById(restrictionTeachers.get(i))
                        .orElseThrow(() -> new IllegalArgumentException("Teacher not found with id: " + restrictionTeachers.get(finalI)));

                for (LocalTime current = start; current.isBefore(end); current = current.plusMinutes(5)) {
                    LocalTime next = current.plusMinutes(5);
                    String timeSlot = current.format(DateTimeFormatter.ofPattern("hh:mm a")) + " - " + next.format(DateTimeFormatter.ofPattern("hh:mm a"));

                    String key = restrictionRooms.get(i) + "_" + timeSlot;

                    // Check for an existing entry in the local map or the schedule
                    Entry existingEntry = hardRestrictionEntries.get(key);
                    if (existingEntry == null) {
                        int finalI1 = i;
                        existingEntry = schedule.getEntries().stream()
                                .filter(entry -> entry.getTimeSlot().equals(timeSlot) && entry.getRoomId().equals(restrictionRooms.get(finalI1).toString()))
                                .findFirst()
                                .orElse(null);
                    }

                    List<Teacher> combinedTeachers = new ArrayList<>();
                    String combinedEventName;

                    if (existingEntry != null) {
                        // Retain previous teachers and event name
                        combinedTeachers.addAll(existingEntry.getAssignedTeachers());
                        combinedEventName = (existingEntry.getEventName() != null ? existingEntry.getEventName() + ", " : "")
                                + "Hard Restriction: " + restrictedTeacher.getName();
                    } else {
                        combinedEventName = "Hard Restriction: " + restrictedTeacher.getName();
                    }

                    // Add the new teacher to the combined list
                    combinedTeachers.add(restrictedTeacher);

                    // Create the updated restriction entry
                    Entry restrictionEntry = new Entry();
                    restrictionEntry.setTimeSlot(timeSlot);
                    restrictionEntry.setRoomId(restrictionRooms.get(i).toString());
                    restrictionEntry.setTeachersRequired(combinedTeachers.size());
                    restrictionEntry.setAssignedTeachers(combinedTeachers);
                    restrictionEntry.setEventName(combinedEventName);

                    // Store the updated entry in the map
                    hardRestrictionEntries.put(key, restrictionEntry);
                }
            }
        }

        // Process requiredTimeStart and requiredTimeEnd for each teacher
        for (Teacher teacher : allTeachers) {
            if (teacher.getRequiredTimeStart() != null && teacher.getRequiredTimeEnd() != null && teacher.getRequiredRoom() != null) {

                LocalTime start = teacher.getRequiredTimeStart();
                LocalTime end = teacher.getRequiredTimeEnd();
                Room requiredRoom = teacher.getRequiredRoom();

                for (LocalTime current = start; current.isBefore(end); current = current.plusMinutes(5)) {
                    LocalTime next = current.plusMinutes(5);
                    String timeSlot = current.format(DateTimeFormatter.ofPattern("hh:mm a")) + " - " + next.format(DateTimeFormatter.ofPattern("hh:mm a"));

                    String key = requiredRoom.getId() + "_" + timeSlot;

                    // Check for an existing entry in the local map or the schedule
                    Entry existingEntry = hardRestrictionEntries.get(key);
                    if (existingEntry == null) {
                        existingEntry = schedule.getEntries().stream()
                                .filter(entry -> entry.getTimeSlot().equals(timeSlot) && entry.getRoomId().equals(requiredRoom.getId().toString()))
                                .findFirst()
                                .orElse(null);
                    }

                    List<Teacher> combinedTeachers = new ArrayList<>();
                    String combinedEventName;

                    if (existingEntry != null) {
                        // Retain previous teachers and event name
                        combinedTeachers.addAll(existingEntry.getAssignedTeachers());
                        combinedEventName = (existingEntry.getEventName() != null ? existingEntry.getEventName() + ", " : "")
                                + "Required Time: " + teacher.getName();
                    } else {
                        combinedEventName = "Required Time: " + teacher.getName();
                    }

                    // Add the new teacher to the combined list
                    combinedTeachers.add(teacher);

                    // Create the updated required time entry
                    Entry requiredEntry = new Entry();
                    requiredEntry.setTimeSlot(timeSlot);
                    requiredEntry.setRoomId(requiredRoom.getId().toString());
                    requiredEntry.setTeachersRequired(combinedTeachers.size());
                    requiredEntry.setAssignedTeachers(combinedTeachers);
                    requiredEntry.setEventName(combinedEventName);

                    // Store the updated entry in the map
                    hardRestrictionEntries.put(key, requiredEntry);
                }
            }
        }

        // Map to store room assignments (key: time slot, value: list of entries)
        Map<String, List<Entry>> roomAssignments = new HashMap<>();

        // Get all time slots for the schedule
        List<String> timeSlotsList = generateTimeSlots();

        // Populate room assignments and calculate breaks
        for (String timeSlot : timeSlotsList) {
            List<Teacher> availableTeachersForSlot = findAvailableTeachers(allTeachers, timeSlot);

            for (Room room : rooms) {
                String key = room.getId() + "_" + timeSlot;

                // Check for a hard restriction for this room and time slot
                if (hardRestrictionEntries.containsKey(key)) {
                    Entry hardEntry = hardRestrictionEntries.get(key);

                    // Assign the remaining required teacher(s)
                    List<Teacher> remainingTeachers = assignTeachers(
                            availableTeachersForSlot,
                            hardEntry.getTeachersRequired() - hardEntry.getAssignedTeachers().size(),
                            timeSlot
                    );
                    hardEntry.getAssignedTeachers().addAll(remainingTeachers);

                    roomAssignments.computeIfAbsent(key, k -> new ArrayList<>()).add(hardEntry);

                    // Remove assigned teachers from the available pool
                    availableTeachersForSlot.removeAll(hardEntry.getAssignedTeachers());
                } else {
                    // Handle regular schedule entries
                    for (Entry entry : schedule.getEntries()) {
                        if (entry.getTimeSlot().equals(timeSlot) && entry.getRoomId().equals(room.getId().toString())) {
                            // Check preferredRooms for each teacher
                            List<Teacher> validTeachers = availableTeachersForSlot.stream()
                                    .filter(teacher -> teacher.getPreferredRooms().contains(room))
                                    .collect(Collectors.toList());

                            if (!validTeachers.isEmpty()) {
                                roomAssignments.computeIfAbsent(key, k -> new ArrayList<>()).add(entry);

                                List<Teacher> assignedTeachers = assignTeachers(validTeachers, entry.getTeachersRequired(), timeSlot);
                                entry.setAssignedTeachers(assignedTeachers);
                                availableTeachersForSlot.removeAll(assignedTeachers);
                            }
                        }
                    }
                }
            }
        }

        model.addAttribute("schedule", schedule);
        model.addAttribute("rooms", rooms);
        model.addAttribute("scheduleWithAssignments", roomAssignments);
        model.addAttribute("timeSlots", timeSlotsList);

        return "generateSchedule";
    }


    private List<String> generateTimeSlotsInRange(LocalTime start, LocalTime end) {
        List<String> slots = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");

        while (!start.isAfter(end)) {
            LocalTime nextTime = start.plusMinutes(5);
            slots.add(start.format(formatter) + " - " + nextTime.format(formatter));
            start = nextTime;
        }

        return slots;
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
