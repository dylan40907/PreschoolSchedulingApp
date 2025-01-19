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

        // -------------------
        // 1) Process Hard Restrictions
        // -------------------
        if (restrictionTeachers != null && timeSlots != null && restrictionRooms != null &&
                restrictionTeachers.size() == timeSlots.size() &&
                restrictionTeachers.size() == restrictionRooms.size()) {
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
                                .filter(entry -> entry.getTimeSlot().equals(timeSlot)
                                        && entry.getRoomId().equals(restrictionRooms.get(finalI1).toString()))
                                .findFirst()
                                .orElse(new Entry());
                    }

                    // Combine event name and teachers
                    String originalEventName = existingEntry.getEventName() != null
                            ? existingEntry.getEventName() + ", " : "";
                    existingEntry.setEventName(originalEventName + "Hard Restriction: " + restrictedTeacher.getName());

                    List<Teacher> assignedTeachers = new ArrayList<>(existingEntry.getAssignedTeachers() != null
                            ? existingEntry.getAssignedTeachers()
                            : new ArrayList<>());
                    if (!assignedTeachers.contains(restrictedTeacher)) {
                        assignedTeachers.add(restrictedTeacher);
                    }

                    existingEntry.setTeachersRequired(
                            existingEntry.getTeachersRequired() > 0 ? existingEntry.getTeachersRequired() : 2
                    );
                    existingEntry.setAssignedTeachers(assignedTeachers);

                    hardRestrictionEntries.put(key, existingEntry);
                    teacherAssignmentsPerSlot
                            .computeIfAbsent(timeSlot, k -> new HashSet<>())
                            .add(restrictedTeacher.getId());
                }
            }
        }

        // -------------------
        // 2) Process Required Times (priority first, then non-priority)
        // -------------------
        List<Teacher> prioritized = allTeachers.stream().filter(Teacher::hasPriority).toList();
        processRequiredTimes(prioritized, hardRestrictionEntries, teacherAssignmentsPerSlot, schedule, true);

        List<Teacher> nonPrioritized = allTeachers.stream().filter(t -> !t.hasPriority()).toList();
        processRequiredTimes(nonPrioritized, hardRestrictionEntries, teacherAssignmentsPerSlot, schedule, false);

        // ----------------------------------------------------------------------
        // 3) SCHEDULE BREAKS FOR TEACHERS
        // ----------------------------------------------------------------------
        Map<String, List<BreakEntry>> teacherBreaks = new HashMap<>();

        // Generate all 5-minute time slots from 7:30 AM to 5:55 PM
        List<String> timeSlotsList = generateTimeSlots();

        // Place breaks for each teacher
        for (Teacher teacher : allTeachers) {
            List<BreakEntry> breaksForThisTeacher = scheduleBreaksForTeacher(
                    teacher,
                    hardRestrictionEntries,
                    teacherAssignmentsPerSlot,
                    timeSlotsList
            );
            teacherBreaks.put(String.valueOf(teacher.getId()), breaksForThisTeacher);

            // remove the teacher from coverage if slot = break
            removeTeacherFromAssignedBreakSlots(
                    teacher,
                    breaksForThisTeacher,
                    hardRestrictionEntries,
                    schedule,
                    teacherAssignmentsPerSlot
            );
        }

        // -------------------
        // 4) Fill the schedule for the remaining (non-Hard Restriction) times
        // -------------------
        Map<String, List<Entry>> roomAssignments = new HashMap<>();

        for (int i = 0; i < timeSlotsList.size(); i++) {
            String timeSlot = timeSlotsList.get(i);
            // find teachers not on break
            List<Teacher> availableTeachers = findAvailableTeachers(allTeachers, timeSlot, teacherBreaks);

            // remove teachers already assigned
            Set<Long> assignedIds = teacherAssignmentsPerSlot.getOrDefault(timeSlot, new HashSet<>());
            availableTeachers.removeIf(t -> assignedIds.contains(t.getId()));

            // continuity logic
            for (Room room : rooms) {
                String key = room.getId() + "_" + timeSlot;

                if (i > 0) {
                    String prevSlot = timeSlotsList.get(i - 1);
                    String prevKey = room.getId() + "_" + prevSlot;
                    List<Entry> prevEntries = roomAssignments.getOrDefault(prevKey, new ArrayList<>());
                    Set<Teacher> continuityTeachers = new HashSet<>();
                    for (Entry pe : prevEntries) {
                        if (pe.getAssignedTeachers() != null) {
                            continuityTeachers.addAll(pe.getAssignedTeachers());
                        }
                    }
                    // sort available so continuity come first
                    availableTeachers.sort((t1, t2) -> {
                        boolean c1 = continuityTeachers.contains(t1);
                        boolean c2 = continuityTeachers.contains(t2);
                        if (c1 && !c2) return -1;
                        if (c2 && !c1) return 1;
                        return 0;
                    });
                }

                // if there's a Hard Restriction entry for this room+slot, fill up leftover if needed
                if (hardRestrictionEntries.containsKey(key)) {
                    Entry hrEntry = hardRestrictionEntries.get(key);
                    int needed = hrEntry.getTeachersRequired() - hrEntry.getAssignedTeachers().size();
                    if (needed > 0) {
                        List<Teacher> picks = assignTeachers(availableTeachers, needed, timeSlot);
                        hrEntry.getAssignedTeachers().addAll(picks);
                        availableTeachers.removeAll(picks);
                    }
                    roomAssignments
                            .computeIfAbsent(key, x -> new ArrayList<>())
                            .add(hrEntry);
                } else {
                    // normal schedule
                    for (Entry e : schedule.getEntries()) {
                        if (timeSlot.equals(e.getTimeSlot()) && room.getId().toString().equals(e.getRoomId())) {
                            int needed = e.getTeachersRequired();
                            List<Teacher> picks = assignTeachers(availableTeachers, needed, timeSlot);
                            e.setAssignedTeachers(picks);
                            roomAssignments
                                    .computeIfAbsent(key, x -> new ArrayList<>())
                                    .add(e);
                            availableTeachers.removeAll(picks);
                            assignedIds.addAll(picks.stream().map(Teacher::getId).toList());
                        }
                    }
                }
            }
            teacherAssignmentsPerSlot.put(timeSlot, assignedIds);
        }

        // Build teacherIdMap for FTL
        Map<String, Teacher> teacherIdMap = allTeachers.stream()
                .collect(Collectors.toMap(
                        t -> String.valueOf(t.getId()),
                        Function.identity()
                ));

        // Add stuff to model
        model.addAttribute("schedule", schedule);
        model.addAttribute("rooms", rooms);
        model.addAttribute("scheduleWithAssignments", roomAssignments);
        model.addAttribute("timeSlots", timeSlotsList);
        model.addAttribute("teacherBreaks", teacherBreaks);
        model.addAttribute("teacherIdMap", teacherIdMap);

        return "generateSchedule";
    }

    /** Schedules breaks (long + short) for a single teacher. */
    private List<BreakEntry> scheduleBreaksForTeacher(
            Teacher teacher,
            Map<String, Entry> hardRestrictionEntries,
            Map<String, Set<Long>> teacherAssignmentsPerSlot,
            List<String> allTimeSlots
    ) {
        List<BreakEntry> result = new ArrayList<>();

        // e.g. teacher has X short breaks, Y long break length
        int longBreak = (teacher.getLongBreakLength() != null) ? teacher.getLongBreakLength() : 30;
        int numShortBreaks = (teacher.getNumTenMinBreaks() != null) ? teacher.getNumTenMinBreaks() : 0;

        // Convert to increments
        int longIncrements = longBreak / 5; // e.g. 30 -> 6 increments
        int shortIncrements = 2; // each short = 10 min => 2 increments

        // Build candidate break slots
        List<String> candidate = buildCandidateBreakSlotsForTeacher(teacher, allTimeSlots, hardRestrictionEntries);

        // We want the long break done by teacher.getStartTime() + 5 hours
        LocalTime mustFinishLongBy = teacher.getStartTime().plusHours(5);

        // 1) Long break
        List<String> theLong = tryPlaceBreak(candidate, longIncrements, mustFinishLongBy, true);
        if (!theLong.isEmpty()) {
            for (String s : theLong) {
                result.add(new BreakEntry(s));
                candidate.remove(s);
            }
        }

        // 2) short breaks
        for (int i = 0; i < numShortBreaks; i++) {
            List<String> shortOne = tryPlaceBreak(candidate, shortIncrements, null, false);
            if (!shortOne.isEmpty()) {
                for (String s : shortOne) {
                    result.add(new BreakEntry(s));
                    candidate.remove(s);
                }
            }
        }

        return result;
    }

    /** buildCandidateBreakSlotsForTeacher => skip Hard Restriction + noBreakPeriods + 1.5 hr + last hr, etc. */
    private List<String> buildCandidateBreakSlotsForTeacher(
            Teacher teacher,
            List<String> allTimeSlots,
            Map<String, Entry> hardRestrictionEntries
    ) {
        List<String> out = new ArrayList<>();
        LocalTime earliestBreak = teacher.getStartTime().plusMinutes(90);
        LocalTime latestBreak = teacher.getEndTime().minusMinutes(60);

        // gather teacher’s Hard Restriction
        Set<String> teacherHardSlots = findTeacherHardRestrictedTimeSlots(teacher, hardRestrictionEntries);
        // gather teacher’s noBreak
        Set<String> noBreakSlots = expandNoBreakPeriods(teacher.getNoBreakPeriods());

        for (String slot : allTimeSlots) {
            TimeSlot ts = TimeSlot.fromString(slot);
            if (ts.getStart().isBefore(earliestBreak)) continue;
            if (ts.getEnd().isAfter(latestBreak)) continue;
            if (ts.getStart().isBefore(teacher.getStartTime())
                    || ts.getEnd().isAfter(teacher.getEndTime())) {
                continue;
            }
            if (teacherHardSlots.contains(slot)) continue;
            if (noBreakSlots.contains(slot)) continue;
            out.add(slot);
        }
        return out;
    }

    /** tryPlaceBreak => find neededIncrements consecutive 5-min slots, skipping if finish time > mustFinishLongBy */
    private List<String> tryPlaceBreak(
            List<String> candidate,
            int neededIncrements,
            LocalTime mustFinishLongBy,
            boolean isLongBreak
    ) {
        List<String> out = new ArrayList<>();
        TimeSlot[] arr = candidate.stream()
                .map(TimeSlot::fromString)
                .sorted(Comparator.comparing(TimeSlot::getStart))
                .toArray(TimeSlot[]::new);

        for (int i=0;i<arr.length;i++) {
            out.clear();
            out.add(arr[i].toString());
            int j=i+1;
            while (j<arr.length && out.size()<neededIncrements) {
                TimeSlot prev = TimeSlot.fromString(out.get(out.size()-1));
                TimeSlot nxt = arr[j];
                if (!nxt.getStart().equals(prev.getEnd())) {
                    break;
                }
                out.add(nxt.toString());
                j++;
            }
            if (out.size()== neededIncrements) {
                if (isLongBreak && mustFinishLongBy!=null) {
                    // ensure last slot ends by mustFinishLongBy
                    TimeSlot last = TimeSlot.fromString(out.get(out.size()-1));
                    if (last.getEnd().isAfter(mustFinishLongBy)) {
                        continue;
                    }
                }
                return new ArrayList<>(out);
            }
        }
        return new ArrayList<>();
    }

    /** Remove teacher from coverage if they are on break. */
    private void removeTeacherFromAssignedBreakSlots(
            Teacher teacher,
            List<BreakEntry> breakList,
            Map<String, Entry> hardRestrictionEntries,
            Schedule schedule,
            Map<String, Set<Long>> teacherAssignmentsPerSlot
    ) {
        for (BreakEntry b : breakList) {
            String slot = b.getTimeSlot();

            // remove from Hard Restriction
            List<String> hrKeys = hardRestrictionEntries.keySet().stream()
                    .filter(k-> k.endsWith("_"+slot))
                    .toList();
            for (String k : hrKeys) {
                Entry e = hardRestrictionEntries.get(k);
                if (e!=null && e.getAssignedTeachers()!=null && e.getAssignedTeachers().contains(teacher)) {
                    e.getAssignedTeachers().remove(teacher);
                    teacherAssignmentsPerSlot
                            .getOrDefault(slot, new HashSet<>())
                            .remove(teacher.getId());
                }
            }

            // remove from normal schedule
            for (Entry e : schedule.getEntries()) {
                if (slot.equals(e.getTimeSlot())
                        && e.getAssignedTeachers()!=null
                        && e.getAssignedTeachers().contains(teacher)) {
                    e.getAssignedTeachers().remove(teacher);
                    teacherAssignmentsPerSlot
                            .getOrDefault(slot, new HashSet<>())
                            .remove(teacher.getId());
                }
            }
        }
    }

    /** findTeacherHardRestrictedTimeSlots => gather timeslots that are Hard Restriction for teacher */
    private Set<String> findTeacherHardRestrictedTimeSlots(Teacher teacher, Map<String, Entry> hardRestrictionEntries) {
        Set<String> out = new HashSet<>();
        for (Map.Entry<String, Entry> me : hardRestrictionEntries.entrySet()) {
            Entry e = me.getValue();
            if (e.getAssignedTeachers()!=null && e.getAssignedTeachers().contains(teacher)) {
                String[] parts = me.getKey().split("_",2);
                if (parts.length==2) {
                    out.add(parts[1]);
                }
            }
        }
        return out;
    }

    /** expandNoBreakPeriods => convert teacher’s noBreakPeriods => 5-min increments. */
    private Set<String> expandNoBreakPeriods(Map<LocalTime, LocalTime> noBreakMap) {
        Set<String> out = new HashSet<>();
        if (noBreakMap==null || noBreakMap.isEmpty()) {
            return out;
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("hh:mm a");
        for (Map.Entry<LocalTime,LocalTime> e : noBreakMap.entrySet()) {
            LocalTime cur = e.getKey();
            LocalTime end = e.getValue();
            while (!cur.isAfter(end.minusMinutes(5))) {
                LocalTime nxt = cur.plusMinutes(5);
                String slot = cur.format(fmt)+" - "+nxt.format(fmt);
                out.add(slot);
                cur=nxt;
            }
        }
        return out;
    }

    /** findAvailableTeachers => not on break, within start/end time. */
    private List<Teacher> findAvailableTeachers(
            List<Teacher> teachers,
            String timeSlot,
            Map<String, List<BreakEntry>> teacherBreaks
    ) {
        List<Teacher> result = new ArrayList<>();
        for (Teacher t : teachers) {
            if (isTeacherAvailable(t, timeSlot)
                    && !isTeacherOnBreak(t, timeSlot, teacherBreaks)) {
                result.add(t);
            }
        }
        return result;
    }

    private boolean isTeacherAvailable(Teacher teacher, String slotStr) {
        TimeSlot ts = TimeSlot.fromString(slotStr);
        return !teacher.getStartTime().isAfter(ts.getStart())
                && !teacher.getEndTime().isBefore(ts.getEnd());
    }

    private boolean isTeacherOnBreak(Teacher teacher, String slotStr, Map<String,List<BreakEntry>> teacherBreaks) {
        String key = String.valueOf(teacher.getId());
        List<BreakEntry> br = teacherBreaks.getOrDefault(key, new ArrayList<>());
        for (BreakEntry b : br) {
            if (slotStr.equals(b.getTimeSlot())) {
                return true;
            }
        }
        return false;
    }

    /** assignTeachers => up to 'requiredTeachers' from available. */
    private List<Teacher> assignTeachers(
            List<Teacher> available,
            int required,
            String timeSlot
    ) {
        List<Teacher> assigned = new ArrayList<>();
        for (Teacher t : available) {
            if (assigned.size()< required) {
                assigned.add(t);
            } else {
                break;
            }
        }
        return assigned;
    }

    /** generateTimeSlots => 7:30 AM .. 5:55 PM in 5-min increments. */
    private List<String> generateTimeSlots() {
        List<String> slots = new ArrayList<>();
        LocalTime st = LocalTime.of(7,30);
        LocalTime en = LocalTime.of(17,55);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("hh:mm a");

        while (!st.isAfter(en)) {
            LocalTime nxt = st.plusMinutes(5);
            slots.add(st.format(fmt)+" - "+nxt.format(fmt));
            st=nxt;
        }
        return slots;
    }

    /** processRequiredTimes => place teacher in required room.
     * If priority & slot is full => remove non-priority.
     */
    private void processRequiredTimes(
            List<Teacher> teachers,
            Map<String, Entry> hardRestrictionEntries,
            Map<String, Set<Long>> teacherAssignmentsPerSlot,
            Schedule schedule,
            boolean isPriority
    ) {
        for (Teacher teacher : teachers) {
            if (teacher.getRequiredTimeStart()!=null
                    && teacher.getRequiredTimeEnd()!=null
                    && teacher.getRequiredRoom()!=null) {
                LocalTime st = teacher.getRequiredTimeStart();
                LocalTime en = teacher.getRequiredTimeEnd();
                Room rr = teacher.getRequiredRoom();

                for (LocalTime cur=st; cur.isBefore(en); cur=cur.plusMinutes(5)) {
                    LocalTime nxt = cur.plusMinutes(5);
                    String slotStr = cur.format(DateTimeFormatter.ofPattern("hh:mm a"))
                            +" - "+nxt.format(DateTimeFormatter.ofPattern("hh:mm a"));
                    String key = rr.getId()+"_"+slotStr;

                    Entry e = hardRestrictionEntries.get(key);
                    if (e==null) {
                        e = schedule.getEntries().stream()
                                .filter(x-> x.getTimeSlot().equals(slotStr)
                                        && x.getRoomId().equals(rr.getId().toString()))
                                .findFirst()
                                .orElse(new Entry());
                    }

                    // if slot is full & priority => remove non-priority
                    if (isPriority && e.getAssignedTeachers()!=null
                            && e.getAssignedTeachers().size()>= e.getTeachersRequired()) {
                        e.getAssignedTeachers().removeIf(tt -> !tt.hasPriority());
                        // also remove them from teacherAssignmentsPerSlot
                        Set<Long> slotSet = teacherAssignmentsPerSlot.getOrDefault(slotStr, new HashSet<>());
                        Entry finalE = e;
                        slotSet.removeIf(tid-> {
                            Teacher found = finalE.getAssignedTeachers().stream()
                                    .filter(xx-> xx.getId().equals(tid))
                                    .findAny().orElse(null);
                            return (found==null);
                        });
                        teacherAssignmentsPerSlot.put(slotStr, slotSet);
                    }

                    if (e.getAssignedTeachers()!=null
                            && e.getAssignedTeachers().size()>= e.getTeachersRequired()) {
                        continue;
                    }

                    // add teacher
                    String oldName = (e.getEventName()==null)? "" : e.getEventName()+", ";
                    e.setEventName(oldName+"Required Time: "+teacher.getName());

                    List<Teacher> list = new ArrayList<>(
                            e.getAssignedTeachers()!=null
                                    ? e.getAssignedTeachers()
                                    : new ArrayList<>()
                    );
                    if (!list.contains(teacher)) list.add(teacher);

                    e.setTeachersRequired(e.getTeachersRequired()>0 ? e.getTeachersRequired() : 2);
                    e.setAssignedTeachers(list);

                    hardRestrictionEntries.put(key, e);
                    teacherAssignmentsPerSlot
                            .computeIfAbsent(slotStr, x-> new HashSet<>())
                            .add(teacher.getId());
                }
            }
        }
    }

    /** The BreakEntry class for Freemarker usage. */
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
