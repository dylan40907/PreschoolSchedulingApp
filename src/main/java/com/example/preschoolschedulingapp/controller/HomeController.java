package com.example.preschoolschedulingapp.controller;

import com.example.preschoolschedulingapp.model.*;
import com.example.preschoolschedulingapp.repository.RoomRepository;
import com.example.preschoolschedulingapp.repository.ScheduleRepository;
import com.example.preschoolschedulingapp.repository.TeacherRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final ScheduleRepository scheduleRepository;
    private final TeacherRepository teacherRepository;
    private final RoomRepository roomRepository;

    public HomeController(ScheduleRepository scheduleRepository,
                          TeacherRepository teacherRepository,
                          RoomRepository roomRepository) {
        this.scheduleRepository = scheduleRepository;
        this.teacherRepository = teacherRepository;
        this.roomRepository = roomRepository;
    }

    // ----------------------------------------------------------------
    // LOGIN/HOME
    // ----------------------------------------------------------------

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String index() {
        return "home";
    }

    @GetMapping("/home")
    public String home() {
        return "home";
    }

    // ----------------------------------------------------------------
    // TEACHER CRUD
    // ----------------------------------------------------------------

    @RequestMapping("/teacherView")
    public String teacherView(Model model) {
        List<Teacher> teachers = (List<Teacher>) teacherRepository.findAll();
        teachers.forEach(teacher -> {
            if (teacher.getStartTime() == null) {
                teacher.setAvailability("12:00AM-11:59PM");
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
            @RequestParam String name,
            @RequestParam String role,
            @RequestParam String availability,
            @RequestParam List<Long> preferredRooms,
            @RequestParam(required = false) String noBreakPeriods,
            @RequestParam(required = false) String requiredTime,
            @RequestParam(required = false) Long requiredRoom,
            @RequestParam(required = false, defaultValue = "0") int numTenMinBreaks,
            @RequestParam(required = false, defaultValue = "0") int longBreakLength,
            @RequestParam(required = false, defaultValue = "false") boolean hasPriority,

            // NEW: arrays for teacher-level hard restrictions
            @RequestParam(required = false, name = "tHardStart") List<String> tHardStartTimes,
            @RequestParam(required = false, name = "tHardEnd") List<String> tHardEndTimes,
            @RequestParam(required = false, name = "tHardRoom") List<Long> tHardRoomIds
    ) {
        Teacher teacher = new Teacher();
        teacher.setName(name);
        teacher.setRole(role);
        teacher.setPriority(hasPriority);
        teacher.setAvailability(availability);

        List<Room> rooms = (List<Room>) roomRepository.findAllById(preferredRooms);
        teacher.setPreferredRooms(new HashSet<>(rooms));

        if (noBreakPeriods != null && !noBreakPeriods.isEmpty()) {
            teacher.setNoBreakPeriods(parseNoBreakPeriods(noBreakPeriods));
        } else {
            teacher.setNoBreakPeriods(new HashMap<>());
        }

        if (requiredTime != null && !requiredTime.isEmpty()) {
            String[] times = requiredTime.split("-");
            teacher.setRequiredTimeStart(LocalTime.parse(times[0].trim(), DateTimeFormatter.ofPattern("hh:mm a")));
            teacher.setRequiredTimeEnd(LocalTime.parse(times[1].trim(), DateTimeFormatter.ofPattern("hh:mm a")));
        }
        if (requiredRoom != null) {
            Room room = roomRepository.findById(requiredRoom).orElse(null);
            teacher.setRequiredRoom(room);
        }
        teacher.setNumTenMinBreaks(numTenMinBreaks);
        teacher.setLongBreakLength(longBreakLength);

        // --- NEW: parse the teacher-level Hard Restrictions from the arrays ---
        List<TeacherHardRestriction> teacherHRs = new ArrayList<>();
        if (tHardStartTimes != null && tHardEndTimes != null && tHardRoomIds != null) {
            for (int i = 0; i < tHardStartTimes.size(); i++) {
                String startStr = tHardStartTimes.get(i);
                String endStr = tHardEndTimes.get(i);
                Long rmId = tHardRoomIds.get(i);

                if (startStr == null || startStr.isBlank()) continue;
                if (endStr == null || endStr.isBlank()) continue;
                if (rmId == null) continue;

                LocalTime st = LocalTime.parse(startStr.trim(), DateTimeFormatter.ofPattern("hh:mm a"));
                LocalTime en = LocalTime.parse(endStr.trim(), DateTimeFormatter.ofPattern("hh:mm a"));

                teacherHRs.add(new TeacherHardRestriction(st, en, rmId));
            }
        }
        teacher.setHardRestrictions(teacherHRs);

        teacherRepository.save(teacher);
        return "redirect:/teacherView";
    }


    @GetMapping("/editTeacher/{id}")
    public String editTeacher(@PathVariable Long id, Model model) {
        Teacher teacher = teacherRepository.findById(id).orElse(null);
        List<Room> rooms = (List<Room>) roomRepository.findAll();
        model.addAttribute("teacher", teacher);
        model.addAttribute("rooms", rooms);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
        String availability = "";
        if (teacher.getStartTime() != null && teacher.getEndTime() != null) {
            availability = teacher.getStartTime().format(formatter) + " - " + teacher.getEndTime().format(formatter);
        }
        model.addAttribute("availability", availability);

        String requiredTime = "";
        if (teacher.getRequiredTimeStart() != null && teacher.getRequiredTimeEnd() != null) {
            requiredTime = teacher.getRequiredTimeStart().format(formatter)
                    + " - " + teacher.getRequiredTimeEnd().format(formatter);
        }
        model.addAttribute("requiredTime", requiredTime);

        Map<String, String> noBreakPeriodsAsString = new HashMap<>();
        if (teacher.getNoBreakPeriods() != null) {
            noBreakPeriodsAsString = teacher.getNoBreakPeriods().entrySet()
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
            @RequestParam String name,
            @RequestParam String role,
            @RequestParam String availability,
            @RequestParam List<Long> preferredRooms,
            @RequestParam(required = false) String noBreakPeriods,
            @RequestParam(required = false) String requiredTime,
            @RequestParam(required = false) Long requiredRoom,
            @RequestParam(required = false, defaultValue = "0") int numTenMinBreaks,
            @RequestParam(required = false, defaultValue = "0") int longBreakLength,
            @RequestParam(required = false, defaultValue = "false") boolean hasPriority,

            // NEW: parse arrays for teacher-level restrictions
            @RequestParam(required = false, name = "tHardStart") List<String> tHardStartTimes,
            @RequestParam(required = false, name = "tHardEnd") List<String> tHardEndTimes,
            @RequestParam(required = false, name = "tHardRoom") List<Long> tHardRoomIds
    ) {
        Teacher teacher = teacherRepository.findById(id).orElse(null);

        teacher.setName(name);
        teacher.setRole(role);
        teacher.setPriority(hasPriority);

        String[] times = availability.split("-");
        teacher.setStartTime(LocalTime.parse(times[0].trim(), DateTimeFormatter.ofPattern("hh:mm a")));
        teacher.setEndTime(LocalTime.parse(times[1].trim(), DateTimeFormatter.ofPattern("hh:mm a")));

        List<Room> rooms = (List<Room>) roomRepository.findAllById(preferredRooms);
        teacher.setPreferredRooms(new HashSet<>(rooms));

        if (noBreakPeriods != null && !noBreakPeriods.isEmpty()) {
            Map<LocalTime, LocalTime> noBreakMap = parseNoBreakPeriods(noBreakPeriods);
            teacher.setNoBreakPeriods(noBreakMap);
        }
        if (requiredTime != null && !requiredTime.isEmpty()) {
            String[] reqTimes = requiredTime.split("-");
            teacher.setRequiredTimeStart(LocalTime.parse(reqTimes[0].trim(), DateTimeFormatter.ofPattern("hh:mm a")));
            teacher.setRequiredTimeEnd(LocalTime.parse(reqTimes[1].trim(), DateTimeFormatter.ofPattern("hh:mm a")));
        }
        if (requiredRoom != null) {
            Room room = roomRepository.findById(requiredRoom).orElse(null);
            teacher.setRequiredRoom(room);
        }
        teacher.setNumTenMinBreaks(numTenMinBreaks);
        teacher.setLongBreakLength(longBreakLength);

        // NEW: parse the teacher-level Hard Restrictions
        List<TeacherHardRestriction> teacherHRs = new ArrayList<>();
        if (tHardStartTimes != null && tHardEndTimes != null && tHardRoomIds != null) {
            for (int i = 0; i < tHardStartTimes.size(); i++) {
                String startStr = tHardStartTimes.get(i);
                String endStr = tHardEndTimes.get(i);
                Long rmId = tHardRoomIds.get(i);

                if (startStr == null || startStr.isBlank()) continue;
                if (endStr == null || endStr.isBlank()) continue;
                if (rmId == null) continue;

                LocalTime st = LocalTime.parse(startStr.trim(), DateTimeFormatter.ofPattern("hh:mm a"));
                LocalTime en = LocalTime.parse(endStr.trim(), DateTimeFormatter.ofPattern("hh:mm a"));

                teacherHRs.add(new TeacherHardRestriction(st, en, rmId));
            }
        }
        teacher.setHardRestrictions(teacherHRs);

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
                        System.err.println("Invalid no-break period: start after end => " + period);
                        continue;
                    }
                    noBreakMap.put(start, end);
                } catch (Exception e) {
                    System.err.println("Error parsing no-break period: " + period.trim() + " => " + e.getMessage());
                }
            }
        }
        return noBreakMap;
    }

    @PostMapping("/deleteTeacher/{id}")
    public String deleteTeacher(@PathVariable Long id) {
        teacherRepository.deleteById(id);
        return "redirect:/teacherView";
    }

    // ----------------------------------------------------------------
    // ROOM CRUD
    // ----------------------------------------------------------------

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
        room.setName(name);
        roomRepository.save(room);
        return "redirect:/roomView";
    }

    @PostMapping("/deleteRoom/{id}")
    public String deleteRoom(@PathVariable Long id) {
        roomRepository.deleteById(id);
        return "redirect:/roomView";
    }

    // ----------------------------------------------------------------
    // SCHEDULE CRUD
    // ----------------------------------------------------------------

    @GetMapping("/newSchedule")
    public String newSchedule(Model model) {
        List<Room> rooms = (List<Room>) roomRepository.findAll();
        List<Teacher> teachers = (List<Teacher>) teacherRepository.findAll();
        model.addAttribute("rooms", rooms);
        model.addAttribute("teachers", teachers);
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
        Map<String, Entry> entryMap = new HashMap<>();

        for (String key : eventNames.keySet()) {
            String[] parts = key.split("_");
            if (parts.length < 2) {
                System.err.println("Invalid key format => " + key);
                continue;
            }
            String roomId = parts[0].replace("entries[", "");
            String timeSlot = parts[1].replace("].eventName", "").replace("].teachersRequired", "");
            String entryKey = roomId + "_" + timeSlot;

            Entry entry = entryMap.getOrDefault(entryKey, new Entry());
            entry.setRoomId(roomId);
            entry.setTimeSlot(timeSlot);
            entry.setSchedule(schedule);

            if (key.endsWith(".eventName")) {
                entry.setEventName(eventNames.get(key));
            } else if (key.endsWith(".teachersRequired")) {
                try {
                    entry.setTeachersRequired(Integer.parseInt(teachersRequired.get(key)));
                } catch (NumberFormatException e) {
                    entry.setTeachersRequired(0);
                }
            }
            entryMap.put(entryKey, entry);
        }

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
                .orElseThrow(() -> new IllegalArgumentException("No schedule found with id: " + scheduleId));
        List<Room> rooms = (List<Room>) roomRepository.findAll();
        List<Teacher> teachers = (List<Teacher>) teacherRepository.findAll();

        model.addAttribute("schedule", schedule);
        model.addAttribute("rooms", rooms);
        model.addAttribute("teachers", teachers);
        return "preGeneration";
    }

    @GetMapping("/editSchedule/{id}")
    public String editSchedule(@PathVariable Long id, Model model) {
        Schedule schedule = scheduleRepository.findById(id).orElseThrow();
        List<Room> rooms = (List<Room>) roomRepository.findAll();

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
        Schedule schedule = scheduleRepository.findById(id).orElseThrow();
        Map<String, Entry> entryMap = new HashMap<>();

        for (String key : eventNames.keySet()) {
            String[] parts = key.split("_");
            if (parts.length < 2) {
                System.err.println("Invalid key => " + key);
                continue;
            }
            String roomId = parts[0].replace("entries[", "");
            String timeSlot = parts[1].replace("].eventName", "").replace("].teachersRequired", "");
            String entryKey = roomId + "_" + timeSlot;

            Entry entry = entryMap.getOrDefault(entryKey, new Entry());
            entry.setRoomId(roomId);
            entry.setTimeSlot(timeSlot);
            entry.setSchedule(schedule);

            if (key.endsWith(".eventName")) {
                entry.setEventName(eventNames.get(key));
            } else if (key.endsWith(".teachersRequired")) {
                try {
                    entry.setTeachersRequired(Integer.parseInt(teachersRequired.get(key)));
                } catch (NumberFormatException e) {
                    entry.setTeachersRequired(0);
                }
            }
            entryMap.put(entryKey, entry);
        }
        List<Entry> existing = schedule.getEntries();
        existing.clear();
        existing.addAll(entryMap.values());
        scheduleRepository.save(schedule);
        return "redirect:/viewSchedules";
    }

    @PostMapping("/deleteSchedule/{id}")
    public String deleteSchedule(@PathVariable Long id) {
        scheduleRepository.deleteById(id);
        return "redirect:/viewSchedules";
    }

    // -----------------------------------------------------------------------
    //               SCHEDULE GENERATION LOGIC
    // -----------------------------------------------------------------------

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
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));
        List<Room> rooms = (List<Room>) roomRepository.findAll();
        List<Teacher> allTeachers = (List<Teacher>) teacherRepository.findAllById(selectedTeachers);


        // 1) Hard Restriction coverage
        Map<String, Entry> userHardEntries = processUserHardRestrictions(schedule,
                restrictionTeachers, timeSlots, restrictionRooms);

        Map<String, Set<Long>> teacherAssignmentsPerSlot = buildTeacherAssignmentsMap(userHardEntries, schedule);

        // Right after you have loaded allTeachers:

// Merge each teacher's hardRestrictions into the userHardEntries map
        Map<String, Entry> teacherLevelEntries = buildTeacherLevelHardRestrictions(allTeachers, schedule);
        teacherLevelEntries.forEach((k,v) -> {
            // If there's already an entry from userHard, unify them
            if (!userHardEntries.containsKey(k)) {
                userHardEntries.put(k, v);
            } else {
                // merge
                Entry existing = userHardEntries.get(k);
                // ensure teachersRequired is at least 2
                if (existing.getTeachersRequired() < 2) {
                    existing.setTeachersRequired(2);
                }
                // combine assigned teachers
                List<Teacher> existList = existing.getAssignedTeachers();
                if (existList == null) {
                    existList = new ArrayList<>();
                    existing.setAssignedTeachers(existList);
                }
                if (v.getAssignedTeachers() != null) {
                    for (Teacher t : v.getAssignedTeachers()) {
                        if (!existList.contains(t)) {
                            existList.add(t);
                        }
                    }
                }
                // Append eventName
                String oldName = existing.getEventName() == null ? "" : (existing.getEventName() + ", ");
                existing.setEventName(oldName + v.getEventName());
            }
        });


        // 2) teacherBreaks (including forced breaks)
        Map<String, List<BreakEntry>> teacherBreaks = new HashMap<>();
        // Also track which teachers have "already used" their long break
        Set<Long> teacherUsedLongBreak = new HashSet<>();

        for (Teacher t : allTeachers) {
            teacherBreaks.put(String.valueOf(t.getId()), new ArrayList<>());
        }

        // forced break if in Break room => remove from coverage + see if it counts as long break
        for (Map.Entry<String, Entry> me : userHardEntries.entrySet()) {
            Entry e = me.getValue();
            Long roomId = Long.parseLong(e.getRoomId());
            Room breakRoom = roomRepository.findById(roomId).orElse(null);
            if (breakRoom != null && "Break".equalsIgnoreCase(breakRoom.getName())) {
                // The timeslot is 5 min, but we might have many consecutive timeslots
                if (e.getAssignedTeachers() != null) {
                    for (Teacher assignedT : e.getAssignedTeachers()) {
                        teacherBreaks.get(String.valueOf(assignedT.getId()))
                                .add(new BreakEntry(e.getTimeSlot()));
                    }
                }
            }
        }

        // Because user might place a teacher in the "Break" room for a large block,
        // let's see if that block >= teacher's longBreak.
        // We'll do so after we parse the userHard coverage fully:
        markUsedLongBreaksIfLargeBlock(allTeachers, teacherBreaks);

        // 3) place coverage for required times in two passes: priority, then non-priority
        List<Teacher> prioritized = allTeachers.stream()
                .filter(Teacher::hasPriority)
                .collect(Collectors.toList());
        processRequiredTimes(prioritized, userHardEntries, teacherAssignmentsPerSlot, schedule, true);

        List<Teacher> nonPrioritized = allTeachers.stream()
                .filter(t -> !t.hasPriority())
                .collect(Collectors.toList());
        processRequiredTimes(nonPrioritized, userHardEntries, teacherAssignmentsPerSlot, schedule, false);

        // 4) build teacherAssignments from userHard
        for (Entry e : userHardEntries.values()) {
            if (e.getAssignedTeachers() != null) {
                for (Teacher t : e.getAssignedTeachers()) {
                    teacherAssignmentsPerSlot
                            .computeIfAbsent(e.getTimeSlot(), x->new HashSet<>())
                            .add(t.getId());
                }
            }
        }

        // 5) all 5-min timeSlots
        List<String> timeSlotsList = generateTimeSlots();

        // 6) schedule auto breaks for each teacher
        for (Teacher teacher : allTeachers) {
            List<BreakEntry> forced = teacherBreaks.get(String.valueOf(teacher.getId()));
            if (forced == null) {
                forced = new ArrayList<>();
                teacherBreaks.put(String.valueOf(teacher.getId()), forced);
            }

            List<BreakEntry> autoBreaks = scheduleBreaksForTeacher(
                    teacher,
                    userHardEntries,
                    schedule,
                    teacherAssignmentsPerSlot,
                    timeSlotsList,
                    forced,
                    teacherUsedLongBreak,
                    allTeachers,
                    teacherBreaks
            );
            forced.addAll(autoBreaks);

            removeTeacherFromAssignedBreakSlots(
                    teacher, autoBreaks, userHardEntries, schedule, teacherAssignmentsPerSlot
            );
        }

        // 7) fill coverage
        Map<String, List<Entry>> roomAssignments = new HashMap<>();

        System.out.println("=== Step 7: Filling normal coverage for each 5-min timeslot ===");

        for (int i = 0; i < timeSlotsList.size(); i++) {
            String ts = timeSlotsList.get(i);
            // First get everyone who is "available" (not assigned, not on break, in hours):
            List<Teacher> baseAvailable = findAvailableTeachers(allTeachers, ts, teacherBreaks);
            Set<Long> assignedIds = teacherAssignmentsPerSlot.getOrDefault(ts, new HashSet<>());
            // Remove those already assigned
            baseAvailable.removeIf(t -> assignedIds.contains(t.getId()));

            for (Room room : rooms) {
                String key = room.getId() + "_" + ts;

                // For continuity logic, we sort by who was in *this same room* in the previous slot
                List<Teacher> preferEligible = new ArrayList<>(baseAvailable);
                if (i > 0) {
                    String prevSlot = timeSlotsList.get(i - 1);
                    String prevKey = room.getId() + "_" + prevSlot;
                    List<Entry> prevEntries = roomAssignments.getOrDefault(prevKey, new ArrayList<>());
                    Set<Teacher> contTeachers = new HashSet<>();
                    for (Entry pe : prevEntries) {
                        if (pe.getAssignedTeachers() != null) {
                            contTeachers.addAll(pe.getAssignedTeachers());
                        }
                    }
                    // Put the continuing teachers first
                    preferEligible.sort((t1, t2) -> {
                        boolean c1 = contTeachers.contains(t1);
                        boolean c2 = contTeachers.contains(t2);
                        if (c1 && !c2) return -1;
                        if (c2 && !c1) return 1;
                        return 0;
                    });
                }

                // **Now** filter out anyone who does not prefer this room:
                preferEligible.removeIf(t -> !canTeachRoom(t, room.getId()));

                // Now handle user-hard or normal coverage
                if (userHardEntries.containsKey(key)) {
                    Entry hr = userHardEntries.get(key);
                    if (hr.getAssignedTeachers() == null) {
                        hr.setAssignedTeachers(new ArrayList<>());
                    }
                    int needed = hr.getTeachersRequired() - hr.getAssignedTeachers().size();
                    if (needed > 0) {
                        List<Teacher> picks = assignTeachers(preferEligible, needed, ts);
                        hr.getAssignedTeachers().addAll(picks);
                        hr.setAssignedTeachers(deduplicateTeachers(hr.getAssignedTeachers()));
                        preferEligible.removeAll(picks);
                        for (Teacher p : picks) {
                            assignedIds.add(p.getId());
                        }
                    }
                    roomAssignments.computeIfAbsent(key, x -> new ArrayList<>()).add(hr);

                } else {
                    // normal coverage
                    for (Entry e : schedule.getEntries()) {
                        if (e.getTimeSlot().equals(ts)
                                && e.getRoomId().equals(room.getId().toString())) {
                            if (e.getAssignedTeachers() == null) {
                                e.setAssignedTeachers(new ArrayList<>());
                            }
                            int needed = e.getTeachersRequired() - e.getAssignedTeachers().size();
                            if (needed > 0) {
                                List<Teacher> picks = assignTeachers(preferEligible, needed, ts);
                                e.getAssignedTeachers().addAll(picks);
                                e.setAssignedTeachers(deduplicateTeachers(e.getAssignedTeachers()));
                                preferEligible.removeAll(picks);
                                for (Teacher p : picks) {
                                    assignedIds.add(p.getId());
                                }
                            }
                            roomAssignments.computeIfAbsent(key, x -> new ArrayList<>()).add(e);
                        }
                    }
                }
            }
            teacherAssignmentsPerSlot.put(ts, assignedIds);
        }

        System.out.println("=== End of Step 7 coverage fill ===");

        // 8) fill Break room
        fillBreakRoomAssignments(schedule, rooms, teacherBreaks, roomAssignments);

        // 9) teacherIdMap
        Map<String, Teacher> teacherIdMap = allTeachers.stream()
                .collect(Collectors.toMap(
                        t -> String.valueOf(t.getId()),
                        Function.identity()
                ));

        // 10) return
        model.addAttribute("schedule", schedule);
        model.addAttribute("rooms", rooms);
        model.addAttribute("scheduleWithAssignments", roomAssignments);
        model.addAttribute("timeSlots", timeSlotsList);
        model.addAttribute("teacherBreaks", teacherBreaks);
        model.addAttribute("teacherIdMap", teacherIdMap);
        debugFinalCoverage(roomAssignments, rooms, timeSlotsList);
        return "generateSchedule";
    }

    /**
     * Builds a map of "roomId_timeSlot" => Entry
     * based on each Teacher's own hardRestrictions.
     */
    private Map<String, Entry> buildTeacherLevelHardRestrictions(List<Teacher> teachers, Schedule schedule) {
        Map<String, Entry> out = new HashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("hh:mm a");

        for (Teacher t : teachers) {
            if (t.getHardRestrictions() == null) continue;

            for (TeacherHardRestriction thr : t.getHardRestrictions()) {
                LocalTime start = thr.getStartTime();
                LocalTime end = thr.getEndTime();
                Long rmId = thr.getRoomId();

                if (start == null || end == null || rmId == null) continue;

                // For every 5-min increment
                for (LocalTime cur = start; cur.isBefore(end); cur = cur.plusMinutes(5)) {
                    LocalTime nxt = cur.plusMinutes(5);
                    String slotStr = cur.format(fmt) + " - " + nxt.format(fmt);
                    String key = rmId + "_" + slotStr;

                    Entry e = out.get(key);
                    if (e == null) {
                        e = new Entry();
                        e.setRoomId(rmId.toString());
                        e.setTimeSlot(slotStr);
                        e.setSchedule(schedule);
                        e.setEventName("Hard Restriction (T): " + t.getName());
                        e.setAssignedTeachers(new ArrayList<>());
                        e.setTeachersRequired(2); // ensure coverage is 2
                        out.put(key, e);
                    } else {
                        // just append
                        String oldEvt = (e.getEventName() == null) ? "" : (e.getEventName() + ", ");
                        e.setEventName(oldEvt + "Hard Restriction (T): " + t.getName());
                    }

                    if (!e.getAssignedTeachers().contains(t)) {
                        e.getAssignedTeachers().add(t);
                    }
                }
            }
        }
        return out;
    }


    /**
     * Dump final coverage details for each Room + timeslot from roomAssignments.
     * This shows how many teachers ended up assigned vs the teachersRequired field,
     * as well as the actual teacher names.
     *
     * Call it at the end of generateSchedule(...) to see the final state.
     */
    private void debugFinalCoverage(Map<String, List<Entry>> roomAssignments,
                                    List<Room> rooms,
                                    List<String> allTimeSlots) {
        System.out.println("\n=== Final Coverage Debug ===");
        for (String ts : allTimeSlots) {
            System.out.println(" Timeslot: " + ts);
            for (Room r : rooms) {
                String key = r.getId() + "_" + ts;
                List<Entry> entries = roomAssignments.get(key);
                if (entries == null || entries.isEmpty()) {
                    // No entry at all for this room & timeslot
                    System.out.println("   Room " + r.getName()
                            + " => [No entry], coverage=0");
                    continue;
                }
                // Usually there's just one Entry in that slot for the room,
                // but if you do have multiple, we show them all:
                for (Entry e : entries) {
                    List<Teacher> assigned = (e.getAssignedTeachers() == null)
                            ? Collections.emptyList()
                            : e.getAssignedTeachers();
                    int coverCount = assigned.size();
                    int needed = e.getTeachersRequired();
                    System.out.println("   Room " + r.getName()
                            + " => assigned=" + coverCount
                            + ", required=" + needed
                            + ", teachers=" + assigned.stream()
                            .map(Teacher::getName)
                            .collect(Collectors.joining(", ")));
                }
            }
        }
        System.out.println("=== End of Final Coverage Debug ===\n");
    }


    // =============== UTILITY METHODS ===============

    private Map<String, Entry> processUserHardRestrictions(
            Schedule schedule,
            List<Long> restrictionTeachers,
            List<String> timeSlots,
            List<Long> restrictionRooms
    ) {
        Map<String, Entry> out = new HashMap<>();
        if (restrictionTeachers == null || timeSlots == null || restrictionRooms == null) {
            return out;
        }
        if (restrictionTeachers.size() != timeSlots.size()
                || restrictionRooms.size() != timeSlots.size()) {
            return out;
        }

        for (int i = 0; i < restrictionTeachers.size(); i++) {
            String slotRange = timeSlots.get(i);
            if (slotRange == null || slotRange.isEmpty()) continue;

            String[] arr = slotRange.split("-");
            if (arr.length < 2) continue;

            LocalTime start = LocalTime.parse(arr[0].trim(), DateTimeFormatter.ofPattern("hh:mm a"));
            LocalTime end = LocalTime.parse(arr[1].trim(), DateTimeFormatter.ofPattern("hh:mm a"));

            Teacher restrictedT = teacherRepository.findById(restrictionTeachers.get(i)).orElse(null);
            if (restrictedT == null) continue;

            for (LocalTime cur = start; cur.isBefore(end); cur = cur.plusMinutes(5)) {
                LocalTime nxt = cur.plusMinutes(5);
                String slotStr = cur.format(DateTimeFormatter.ofPattern("hh:mm a"))
                        + " - " + nxt.format(DateTimeFormatter.ofPattern("hh:mm a"));
                String key = restrictionRooms.get(i) + "_" + slotStr;

                // Attempt to find an existing Entry in the schedule for this room & slot
                int finalI = i;
                Entry existing = schedule.getEntries().stream()
                        .filter(x -> x.getTimeSlot().equals(slotStr)
                                && x.getRoomId().equals(restrictionRooms.get(finalI).toString()))
                        .findFirst()
                        .orElse(null);

                Entry e = out.get(key);
                if (e == null) {
                    // If it's not already in 'out', use either the existing Entry or create new
                    if (existing != null) {
                        e = existing;
                    } else {
                        e = new Entry();
                        e.setRoomId(restrictionRooms.get(i).toString());
                        e.setTimeSlot(slotStr);
                        e.setSchedule(schedule);
                        e.setEventName(""); // will append below
                        e.setAssignedTeachers(new ArrayList<>());
                        // If there's no prior teachersRequired, default to 2
                        e.setTeachersRequired(2);
                    }
                }

                // Append "Hard Restriction" note
                String oldName = (e.getEventName() == null) ? "" : e.getEventName() + ", ";
                e.setEventName(oldName + "Hard Restriction: " + restrictedT.getName());

                // Assign teacher if not already
                if (e.getAssignedTeachers() == null) {
                    e.setAssignedTeachers(new ArrayList<>());
                }
                if (!e.getAssignedTeachers().contains(restrictedT)) {
                    e.getAssignedTeachers().add(restrictedT);
                }

                // If teachersRequired is below 1 (like 0), bump to 2
                if (e.getTeachersRequired() < 1) {
                    e.setTeachersRequired(2);
                }

                out.put(key, e);
            }
        }
        return out;
    }

    



    // ----------------------------------------------------------------
    // Additional helper: if user Hard Restriction forces a teacher
    // to be in the Break room for a contiguous block of time >=
    // the teacher’s longBreakLength, treat that as satisfying
    // the teacher’s long break so we don't schedule an additional one.
    // ----------------------------------------------------------------
    private void markUsedLongBreaksIfLargeBlock(
            List<Teacher> allTeachers,
            Map<String, List<BreakEntry>> teacherBreaks
    ) {
        // For each teacher, we look at their forced Break slots
        // to see if they form a contiguous block >= teacher.longBreakLength
        for (Teacher t : allTeachers) {
            if (t.getLongBreakLength() == null || t.getLongBreakLength() == 0) {
                continue;
            }
            int neededIncrements = t.getLongBreakLength() / 5;
            List<BreakEntry> br = teacherBreaks.get(String.valueOf(t.getId()));
            if (br == null || br.isEmpty()) continue;

            // Sort the break times by start
            List<TimeSlot> forcedSlots = br.stream()
                    .map(b -> TimeSlot.fromString(b.getTimeSlot()))
                    .sorted(Comparator.comparing(TimeSlot::getStart))
                    .collect(Collectors.toList());

            // See if we ever get a run of consecutive timeslots == neededIncrements
            int streak = 1;
            for (int i = 1; i < forcedSlots.size(); i++) {
                TimeSlot prev = forcedSlots.get(i - 1);
                TimeSlot cur = forcedSlots.get(i);
                if (cur.getStart().equals(prev.getEnd())) {
                    streak++;
                } else {
                    streak = 1;
                }
                if (streak >= neededIncrements) {
                    // Found a forced block >= teacher's long break
                    t.setLongBreakLength(0);
                    break;
                }
            }
        }
    }

    private String findRoomNameById(String roomIdStr) {
        if (roomIdStr == null) return null;
        try {
            Long rid = Long.valueOf(roomIdStr);
            Room r = roomRepository.findById(rid).orElse(null);
            if (r != null) {
                return r.getName();
            }
        } catch (Exception ignored) {
        }
        return null;
    }




    /**
     * Places each teacher with required times into the schedule for every 5-minute chunk
     * in [requiredTimeStart, requiredTimeEnd). We do NOT treat these as user-hard restrictions.
     *
     * If the slot is "full" but teacher is priority, we remove non-priority teachers to free up space.
     * If still full after that, we skip. Otherwise, we insert the teacher and set teachersRequired=2
     * if it was 0. This ensures that the fill logic can add an additional teacher if needed.
     *
     * @param teachers the list of teachers (priority or non-priority)
     * @param userHardEntries your map of "hard" user restrictions
     * @param teacherAssignmentsPerSlot a map of timeSlot -> set of teacher IDs assigned so far
     * @param schedule the Schedule object containing normal entries
     * @param isPriority whether these teachers are considered priority
     */
    private void processRequiredTimes(
            List<Teacher> teachers,
            Map<String, Entry> userHardEntries,
            Map<String, Set<Long>> teacherAssignmentsPerSlot,
            Schedule schedule,
            boolean isPriority
    ) {
        for (Teacher teacher : teachers) {
            // Only proceed if teacher has a required time + room
            if (teacher.getRequiredTimeStart() == null
                    || teacher.getRequiredTimeEnd() == null
                    || teacher.getRequiredRoom() == null) {
                continue;
            }

            Room rr = teacher.getRequiredRoom();
            LocalTime st = teacher.getRequiredTimeStart();
            LocalTime en = teacher.getRequiredTimeEnd();

            // For every 5-min increment in the teacher’s required time range
            for (LocalTime cur = st; cur.isBefore(en); cur = cur.plusMinutes(5)) {
                LocalTime nxt = cur.plusMinutes(5);
                String slotStr = cur.format(DateTimeFormatter.ofPattern("hh:mm a"))
                        + " - " + nxt.format(DateTimeFormatter.ofPattern("hh:mm a"));

                // If teacher is already in that slot, skip
                Set<Long> assignedSet = teacherAssignmentsPerSlot.getOrDefault(slotStr, Collections.emptySet());
                if (assignedSet.contains(teacher.getId())) {
                    // e.g. might be userHard or previously assigned
                    continue;
                }

                // Find or create an Entry in the normal schedule for (roomId, slotStr)
                Entry e = schedule.getEntries().stream()
                        .filter(x -> x.getTimeSlot().equals(slotStr)
                                && x.getRoomId().equals(rr.getId().toString()))
                        .findFirst()
                        .orElse(null);

                if (e == null) {
                    e = new Entry();
                    e.setRoomId(rr.getId().toString());
                    e.setTimeSlot(slotStr);
                    e.setSchedule(schedule);
                    e.setEventName(""); // We'll append the required coverage note below
                    e.setAssignedTeachers(new ArrayList<>());
                    // Default to 2 if 0 or negative
                    e.setTeachersRequired(2);
                    // Make sure to add to schedule
                    schedule.getEntries().add(e);
                } else {
                    // If it is below 2, bump it up
                    if (e.getTeachersRequired() < 2) {
                        e.setTeachersRequired(2);
                    }
                    if (e.getAssignedTeachers() == null) {
                        e.setAssignedTeachers(new ArrayList<>());
                    }
                }

                // If "full" and teacher is priority => remove non-priority
                if (isPriority && e.getAssignedTeachers().size() >= e.getTeachersRequired()) {
                    e.getAssignedTeachers().removeIf(tt -> !tt.hasPriority());
                    // Also sync teacherAssignmentsPerSlot
                    Set<Long> slotTeachers = teacherAssignmentsPerSlot
                            .getOrDefault(slotStr, new HashSet<>());
                    Entry finalE = e;
                    slotTeachers.removeIf(tid ->
                            finalE.getAssignedTeachers().stream().noneMatch(x -> x.getId().equals(tid))
                    );
                    teacherAssignmentsPerSlot.put(slotStr, slotTeachers);
                }

                // If STILL full, skip
                if (e.getAssignedTeachers().size() >= e.getTeachersRequired()) {
                    continue;
                }

                // Otherwise, add the teacher
                if (!e.getAssignedTeachers().contains(teacher)) {
                    e.getAssignedTeachers().add(teacher);
                }

                // Append "Required Coverage" note
                String oldName = (e.getEventName() == null) ? "" : e.getEventName();
                if (!oldName.isEmpty()) {
                    oldName += ", ";
                }
                e.setEventName(oldName + "Required Coverage: " + teacher.getName());

                // Mark them in teacherAssignmentsPerSlot
                teacherAssignmentsPerSlot
                        .computeIfAbsent(slotStr, x -> new HashSet<>())
                        .add(teacher.getId());
            }
        }
    }




    /**
         * Build a map of slotStr -> set of teacherIds, from both userHard entries + normal schedule entries.
         * We added some println logs to help you debug.
         */
    private Map<String, Set<Long>> buildTeacherAssignmentsMap(
            Map<String, Entry> hardRestrictionEntries,
            Schedule schedule
    ) {
        Map<String, Set<Long>> map = new HashMap<>();

        // From userHard
        for (Map.Entry<String, Entry> me : hardRestrictionEntries.entrySet()) {
            Entry e = me.getValue();
            if (e.getAssignedTeachers() != null) {
                for (Teacher t : e.getAssignedTeachers()) {
                    map.computeIfAbsent(e.getTimeSlot(), x -> new HashSet<>()).add(t.getId());
                    System.out.println("[buildTeacherAssignmentsMap] userHard slot="
                            + e.getTimeSlot() + " => " + t.getName());
                }
            }
        }

        // From normal schedule
        for (Entry e : schedule.getEntries()) {
            if (e.getAssignedTeachers() != null) {
                for (Teacher t : e.getAssignedTeachers()) {
                    map.computeIfAbsent(e.getTimeSlot(), x -> new HashSet<>()).add(t.getId());
                    System.out.println("[buildTeacherAssignmentsMap] schedule slot="
                            + e.getTimeSlot() + " => " + t.getName());
                }
            }
        }
        return map;
    }



    /**
     * Attempts to schedule the teacher's breaks (short breaks and possibly one long break),
     * starting ~90 minutes after their startTime. We allow breaks even if the teacher
     * has a required coverage in that slot.
     *
     * The key difference: after we find a contiguous block for the break, we
     * (1) remove the teacher from coverage via removeTeacherFromAssignedBreakSlots(...)
     * (2) also mark them in teacherAssignmentsPerSlot so they won't be re‐assigned in fill coverage.
     */
    /**
     * Attempts to schedule short and/or long breaks for a single teacher, starting around
     * 90 minutes after their start time. Each break is placed only if coverage can stay
     * sufficient (see canSafelyTakeBreak logic).
     *
     * @param teacher                the teacher for whom we’re scheduling breaks
     * @param userHardEntries        map of user-hard restrictions
     * @param schedule               the Schedule object
     * @param teacherAssignmentsPerSlot map of timeSlot -> set of teacherIds
     * @param allTimeSlots           a list of all 5-min increments
     * @param existingBreaks         any forced or previously assigned breaks for this teacher
     * @param teacherUsedLongBreak   a set of teacherIds that have already taken their long break
     * @param allTeachers            the complete list of teachers (needed to find substitutes)
     * @param teacherBreaks          map of teacherId -> list of BreakEntry (so we know who’s on break)
     */
    private List<BreakEntry> scheduleBreaksForTeacher(
            Teacher teacher,
            Map<String, Entry> userHardEntries,
            Schedule schedule,
            Map<String, Set<Long>> teacherAssignmentsPerSlot,
            List<String> allTimeSlots,
            List<BreakEntry> existingBreaks,
            Set<Long> teacherUsedLongBreak,
            List<Teacher> allTeachers,
            Map<String, List<BreakEntry>> teacherBreaks
    ) {
        List<BreakEntry> result = new ArrayList<>();

        int shortBreaks = (teacher.getNumTenMinBreaks() == null) ? 0 : teacher.getNumTenMinBreaks();
        int longBreakLength = (teacher.getLongBreakLength() == null) ? 0 : teacher.getLongBreakLength();

        boolean skipLong = teacherUsedLongBreak.contains(teacher.getId());

        // 1) Build a plan of short break(s) and possibly one long break
        List<PlannedBreak> plan = new ArrayList<>();
        int shortIncrements = 2; // 10 min => 2 increments

        // If teacher has at least 1 short break, schedule one first
        if (shortBreaks > 0) {
            plan.add(new PlannedBreak(shortIncrements, "SHORT"));
            shortBreaks--;
        }

        // If the teacher has a long break not yet used
        int longIncrements = longBreakLength / 5;
        if (longIncrements > 0 && !skipLong) {
            plan.add(new PlannedBreak(longIncrements, "LONG"));
        }

        // Add any remaining short breaks
        while (shortBreaks > 0) {
            plan.add(new PlannedBreak(shortIncrements, "SHORT"));
            shortBreaks--;
        }

        // 2) We’ll start searching for the first break after 90 min
        LocalTime earliestStart = teacher.getStartTime().plusMinutes(90);

        // If there's already forced breaks that run later, push earliestStart
        LocalTime forcedBreakLatestEnd = findLatestBreakEnd(existingBreaks, teacher.getStartTime());
        if (forcedBreakLatestEnd.isAfter(earliestStart)) {
            earliestStart = forcedBreakLatestEnd;
        }

        // 3) Try to place each planned break
        for (PlannedBreak pb : plan) {
            // We now pass allTeachers & teacherBreaks into tryPlaceBreakBlock
            List<String> chunk = tryPlaceBreakBlock(
                    teacher,
                    pb.increments,
                    earliestStart,
                    allTimeSlots,
                    userHardEntries,
                    schedule,
                    teacherAssignmentsPerSlot,
                    existingBreaks,
                    allTeachers,
                    teacherBreaks
            );

            if (!chunk.isEmpty()) {
                // Mark these timeslots as BreakEntry for the teacher
                for (String s : chunk) {
                    result.add(new BreakEntry(s));

                    // Also ensure the teacher is considered "assigned" at that slot
                    // so that the fill coverage step won't put them back in coverage
                    teacherAssignmentsPerSlot
                            .computeIfAbsent(s, x -> new HashSet<>())
                            .add(teacher.getId());
                }

                // If it was a LONG break, mark used
                if ("LONG".equals(pb.type)) {
                    teacherUsedLongBreak.add(teacher.getId());
                }

                // Move earliestStart 2 hours after the end of the new break
                TimeSlot lastSlot = TimeSlot.fromString(chunk.get(chunk.size() - 1));
                LocalTime breakEnd = lastSlot.getEnd();
                earliestStart = breakEnd.plusMinutes(120);
            }
        }

        return result;
    }

    private LocalTime findLatestBreakEnd(List<BreakEntry> breaks, LocalTime teacherStart) {
        LocalTime out = teacherStart.plusMinutes(90);
        for (BreakEntry b : breaks) {
            TimeSlot ts = TimeSlot.fromString(b.getTimeSlot());
            if (ts.getEnd().isAfter(out)) {
                out = ts.getEnd();
            }
        }
        return out;
    }

    private List<String> tryPlaceBreakBlock(
            Teacher teacher,
            int neededIncrements,
            LocalTime earliestStart,
            List<String> allTimeSlots,
            Map<String, Entry> userHardEntries,
            Schedule schedule,
            Map<String, Set<Long>> teacherAssignmentsPerSlot,
            List<BreakEntry> existingBreaks,
            List<Teacher> allTeachers,
            Map<String, List<BreakEntry>> teacherBreaks
    ) {
        // same logic except we add 'allTeachers' + 'teacherBreaks' to calls to canSafelyTakeBreak
        Set<String> forcedBreakSlots = existingBreaks.stream()
                .map(BreakEntry::getTimeSlot)
                .collect(Collectors.toSet());
        Set<String> userHardSlots = findTeacherUserHardRestrictedSlots(teacher, userHardEntries);
        Set<String> noBreakSlots = expandNoBreakPeriods(teacher.getNoBreakPeriods());
        

        List<String> candidate = new ArrayList<>();
        for (String slot : allTimeSlots) {
            TimeSlot ts = TimeSlot.fromString(slot);
            if (ts.getEnd().isBefore(earliestStart)) {
                continue;
            }
            if (ts.getStart().isBefore(teacher.getStartTime())
                    || ts.getEnd().isAfter(teacher.getEndTime())) {
                continue;
            }
            if (forcedBreakSlots.contains(slot)) {
                continue;
            }
            if (userHardSlots.contains(slot)) {
                continue;
            }
            if (noBreakSlots.contains(slot)) {
                continue;
            }
            candidate.add(slot);
        }

        List<String> out = new ArrayList<>();
        TimeSlot[] arr = candidate.stream()
                .map(TimeSlot::fromString)
                .sorted(Comparator.comparing(TimeSlot::getStart))
                .toArray(TimeSlot[]::new);

        for (int i = 0; i < arr.length; i++) {
            out.clear();
            out.add(arr[i].toString());
            int j = i + 1;
            while (j < arr.length && out.size() < neededIncrements) {
                TimeSlot prev = TimeSlot.fromString(out.get(out.size() - 1));
                TimeSlot nxt = arr[j];
                if (!nxt.getStart().equals(prev.getEnd())) {
                    break;
                }
                out.add(nxt.toString());
                j++;
            }
            if (out.size() == neededIncrements) {
                boolean feasible = true;
                for (String s : out) {
                    // Now we pass 'allTeachers' & 'teacherBreaks' to canSafelyTakeBreak
                    if (!canSafelyTakeBreak(teacher, s, schedule, userHardEntries,
                            teacherAssignmentsPerSlot, allTeachers, teacherBreaks)) {
                        feasible = false;
                        break;
                    }
                }
                if (feasible) {
                    return new ArrayList<>(out);
                }
            }
        }
        return Collections.emptyList();
    }


    /**
     * Convert a teacher’s noBreakPeriods into 5-min increments.
     */
    private Set<String> expandNoBreakPeriods(Map<LocalTime, LocalTime> noBreakMap) {
        Set<String> out = new HashSet<>();
        if (noBreakMap == null || noBreakMap.isEmpty()) {
            return out;
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("hh:mm a");
        for (Map.Entry<LocalTime, LocalTime> e : noBreakMap.entrySet()) {
            LocalTime cur = e.getKey();
            LocalTime end = e.getValue();
            while (!cur.isAfter(end.minusMinutes(5))) {
                LocalTime nxt = cur.plusMinutes(5);
                out.add(cur.format(fmt) + " - " + nxt.format(fmt));
                cur = nxt;
            }
        }
        return out;
    }

    /**
     * We now block the teacher's break if removing them would
     * leave coverage below teachersRequired and we cannot find
     * a substitute teacher to fill that gap.
     */
    private boolean canSafelyTakeBreak(
            Teacher teacher,
            String slotStr,
            Schedule schedule,
            Map<String, Entry> userHardEntries,
            Map<String, Set<Long>> teacherAssignmentsPerSlot,
            List<Teacher> allTeachers,
            Map<String, List<BreakEntry>> teacherBreaks
    ) {
        // Collect all coverage at this slot
        List<Entry> slotEntries = new ArrayList<>();
        for (Map.Entry<String, Entry> me : userHardEntries.entrySet()) {
            if (me.getKey().endsWith("_" + slotStr)) {
                slotEntries.add(me.getValue());
            }
        }
        for (Entry e : schedule.getEntries()) {
            if (slotStr.equals(e.getTimeSlot())) {
                slotEntries.add(e);
            }
        }
        slotEntries = slotEntries.stream().distinct().collect(Collectors.toList());

        // We'll track revert actions so if we find a coverage gap in a later entry,
        // we revert any partial sub assignments.
        List<Runnable> revertActions = new ArrayList<>();

        for (Entry e : slotEntries) {
            if (e.getAssignedTeachers() == null) continue;
            if (!e.getAssignedTeachers().contains(teacher)) continue;

            // This is the room's ID for the coverage
            Long roomId = Long.valueOf(e.getRoomId());

            int needed = (e.getTeachersRequired() < 1) ? 1 : e.getTeachersRequired();
            int afterRemoval = e.getAssignedTeachers().size() - 1;

            // If removing teacher would drop coverage below needed => gap
            if (afterRemoval < needed) {
                int gap = needed - afterRemoval;

                // Find possible subs who also prefer this room
                List<Teacher> subCandidates = findSubCandidatesForSlot(
                        slotStr, roomId, allTeachers,
                        teacherAssignmentsPerSlot, teacherBreaks
                );

                if (subCandidates.size() < gap) {
                    // Not enough subs => block break
                    System.out.println("   [canSafelyTakeBreak] BLOCK => coverage short. No break for "
                            + teacher.getName() + " at " + slotStr
                            + " (need " + gap + " subs, have only " + subCandidates.size() + ")");
                    // revert any partial changes so far
                    for (Runnable r : revertActions) {
                        r.run(); // revert
                    }
                    return false;
                } else {
                    // We can fill coverage with 'gap' subs:
                    List<Teacher> subPick = subCandidates.subList(0, gap);
                    System.out.println("   [canSafelyTakeBreak] " + teacher.getName()
                            + " => removing from coverage, substituting "
                            + subPick.stream().map(Teacher::getName).collect(Collectors.joining(", "))
                            + " at " + slotStr);

                    // Remove the teacher from coverage in this entry
                    e.getAssignedTeachers().remove(teacher);

                    // Revert action in case we fail on a *later* entry
                    revertActions.add(() -> {
                        // revert removing the teacher
                        e.getAssignedTeachers().add(teacher);
                        // revert adding the subs
                        e.getAssignedTeachers().removeAll(subPick);
                    });

                    // Add the sub(s) now
                    e.getAssignedTeachers().addAll(subPick);

                    // Also update teacherAssignmentsPerSlot
                    Set<Long> assignedSet = teacherAssignmentsPerSlot
                            .getOrDefault(slotStr, new HashSet<>());
                    assignedSet.remove(teacher.getId());
                    for (Teacher sub : subPick) {
                        assignedSet.add(sub.getId());
                    }
                    teacherAssignmentsPerSlot.put(slotStr, assignedSet);

                    // Another revert action for the sub assignments
                    revertActions.add(() -> {
                        for (Teacher sub : subPick) {
                            e.getAssignedTeachers().remove(sub);
                            assignedSet.remove(sub.getId());
                        }
                        e.getAssignedTeachers().add(teacher);
                        assignedSet.add(teacher.getId());
                    });
                }
            }
        }

        // If we get here, we never blocked => coverage is fine
        System.out.println("[canSafelyTakeBreak] => ALLOW break for " + teacher.getName() + " at " + slotStr);
        return true;
    }


    /**
     * Find possible substitute teachers for the given slot,
     * i.e. teachers who are not assigned, not on break, and within work hours.
     * We do not remove them from coverage here; we only see if they're available.
     */
    private List<Teacher> findSubCandidatesForSlot(
            String slotStr,
            Long roomId,
            List<Teacher> allTeachers,
            Map<String, Set<Long>> teacherAssignmentsPerSlot,
            Map<String, List<BreakEntry>> teacherBreaks
    ) {
        Set<Long> alreadyAssigned = teacherAssignmentsPerSlot.getOrDefault(slotStr, Collections.emptySet());

        List<Teacher> subs = new ArrayList<>();
        for (Teacher t : allTeachers) {
            // Only add if t is not assigned in this slot, is within hours,
            // not on break, and "canTeachRoom(t, roomId)" is true
            if (!alreadyAssigned.contains(t.getId())
                    && isTeacherWithinWorkHours(t, slotStr)
                    && !isTeacherOnBreak(t, slotStr, teacherBreaks)
                    && canTeachRoom(t, roomId)) {
                subs.add(t);
            }
        }
        return subs;
    }

    /**
     * Returns true if the teacher’s preferredRooms is empty
     * (meaning "no preference" => can teach any room),
     * OR if it contains the room with the given ID.
     */
    private boolean canTeachRoom(Teacher t, Long roomId) {
        if (t.getPreferredRooms() == null || t.getPreferredRooms().isEmpty()) {
            // No preference => can teach anywhere
            return true;
        }
        // Otherwise, must contain the matching Room ID
        for (Room r : t.getPreferredRooms()) {
            if (r.getId().equals(roomId)) {
                return true;
            }
        }
        return false;
    }


    /**
     * For each newly scheduled break timeslot for this teacher, remove them from coverage
     * in both userHardEntries and normal schedule entries at that timeslot, then also
     * mark them as assigned in teacherAssignmentsPerSlot so that the coverage filler
     * won't re-add them in the same slot.
     */
    private void removeTeacherFromAssignedBreakSlots(
            Teacher teacher,
            List<BreakEntry> breakList,
            Map<String, Entry> userHardEntries,
            Schedule schedule,
            Map<String, Set<Long>> teacherAssignmentsPerSlot
    ) {
        for (BreakEntry b : breakList) {
            String slot = b.getTimeSlot();
            System.out.println("[removeTeacherFromAssignedBreakSlots] Attempting to mark teacher "
                    + teacher.getName() + " as unavailable at slot=" + slot);

            // Instead of physically removing them from e.getAssignedTeachers() again,
            // you could just do:
            teacherAssignmentsPerSlot
                    .computeIfAbsent(slot, x -> new HashSet<>())
                    .add(teacher.getId());
        }
    }


    // ---------- Additional Utility ----------

    private Set<String> findTeacherUserHardRestrictedSlots(Teacher teacher, Map<String, Entry> userHardEntries) {
        Set<String> out = new HashSet<>();
        for (Map.Entry<String, Entry> me : userHardEntries.entrySet()) {
            Entry e = me.getValue();
            if (e.getAssignedTeachers() != null && e.getAssignedTeachers().contains(teacher)) {
                // Only treat it as a hard restriction if the eventName includes "Hard Restriction: "
                if (e.getEventName() != null && e.getEventName().contains("Hard Restriction: ")) {
                    out.add(e.getTimeSlot());
                }
            }
        }
        return out;
    }



    private List<Teacher> findAvailableTeachers(
            List<Teacher> teachers,
            String slotStr,
            Map<String,List<BreakEntry>> teacherBreaks
    ) {
        List<Teacher> result = new ArrayList<>();
        for (Teacher t : teachers) {
            if (isTeacherWithinWorkHours(t, slotStr) && !isTeacherOnBreak(t, slotStr, teacherBreaks)) {
                result.add(t);
            }
        }
        return result;
    }

    private boolean isTeacherWithinWorkHours(Teacher teacher, String slotStr) {
        TimeSlot ts = TimeSlot.fromString(slotStr);
        return !teacher.getStartTime().isAfter(ts.getStart())
                && !teacher.getEndTime().isBefore(ts.getEnd());
    }

    private boolean isTeacherOnBreak(
            Teacher teacher,
            String slotStr,
            Map<String, List<BreakEntry>> teacherBreaks
    ) {
        List<BreakEntry> br = teacherBreaks.getOrDefault(String.valueOf(teacher.getId()), new ArrayList<>());
        for (BreakEntry b : br) {
            if (b.getTimeSlot().equals(slotStr)) {
                return true;
            }
        }
        return false;
    }

    private void fillBreakRoomAssignments(
            Schedule schedule,
            List<Room> allRooms,
            Map<String, List<BreakEntry>> teacherBreaks,
            Map<String, List<Entry>> roomAssignments
    ) {
        // Find the "Break" room by name
        Room breakRoom = allRooms.stream()
                .filter(r -> "Break".equalsIgnoreCase(r.getName()))
                .findFirst()
                .orElse(null);
        if (breakRoom == null) {
            return;
        }
        Long brId = breakRoom.getId();

        for (Map.Entry<String, List<BreakEntry>> me : teacherBreaks.entrySet()) {
            Long tId = Long.valueOf(me.getKey());
            List<BreakEntry> bList = me.getValue();

            for (BreakEntry b : bList) {
                String slot = b.getTimeSlot();
                String key = brId + "_" + slot;

                List<Entry> breakEntries = roomAssignments.computeIfAbsent(key, x -> new ArrayList<>());
                Entry e = breakEntries.stream()
                        .filter(x -> x.getTimeSlot().equals(slot)
                                && x.getRoomId().equals(String.valueOf(brId)))
                        .findFirst()
                        .orElse(null);

                if (e == null) {
                    e = new Entry();
                    e.setRoomId(String.valueOf(brId));
                    e.setTimeSlot(slot);
                    e.setSchedule(schedule);
                    e.setEventName("Break");
                    e.setTeachersRequired(1);
                    e.setAssignedTeachers(new ArrayList<>());
                    breakEntries.add(e);
                }
                boolean already = e.getAssignedTeachers().stream().anyMatch(x -> x.getId().equals(tId));
                if (!already) {
                    Teacher teach = teacherRepository.findById(tId).orElse(null);
                    if (teach != null) {
                        e.getAssignedTeachers().add(teach);
                    }
                }
            }
        }
    }



    /**
     * Generate 5-min increments from 7:30 AM to 5:55 PM.
     */
    private List<String> generateTimeSlots() {
        List<String> slots = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("hh:mm a");
        LocalTime st = LocalTime.of(7, 30);
        LocalTime en = LocalTime.of(17, 55);
        while (!st.isAfter(en)) {
            LocalTime nxt = st.plusMinutes(5);
            slots.add(st.format(fmt) + " - " + nxt.format(fmt));
            st = nxt;
        }
        return slots;
    }

    private List<Teacher> assignTeachers(List<Teacher> available, int required, String timeSlot) {
        System.out.println("[assignTeachers] slot=" + timeSlot
                + ", required=" + required
                + ", available=" + available.stream().map(Teacher::getName).collect(Collectors.joining(", ")));

        List<Teacher> assigned = new ArrayList<>();
        for (Teacher t : available) {
            if (assigned.size() < required) {
                assigned.add(t);
            } else {
                break;
            }
        }
        System.out.println("[assignTeachers] slot=" + timeSlot
                + " => picks=" + assigned.stream().map(Teacher::getName).collect(Collectors.joining(", ")));
        return assigned;
    }


    private List<Teacher> deduplicateTeachers(List<Teacher> assigned) {
        LinkedHashMap<Long, Teacher> map = new LinkedHashMap<>();
        for (Teacher t : assigned) {
            map.put(t.getId(), t);
        }
        return new ArrayList<>(map.values());
    }

    // A small helper struct for a "planned" break with increments + type
    private static class PlannedBreak {
        int increments;
        String type;
        public PlannedBreak(int increments, String type) {
            this.increments = increments;
            this.type = type;
        }
    }

    private static class TimeSlot {
        private final LocalTime start;
        private final LocalTime end;

        public TimeSlot(LocalTime s, LocalTime e) {
            this.start = s;
            this.end = e;
        }

        public LocalTime getStart() {
            return start;
        }

        public LocalTime getEnd() {
            return end;
        }

        public static TimeSlot fromString(String slot) {
            String[] parts = slot.split("-");
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("hh:mm a");
            LocalTime st = LocalTime.parse(parts[0].trim(), fmt);
            LocalTime en = LocalTime.parse(parts[1].trim(), fmt);
            return new TimeSlot(st, en);
        }

        @Override
        public String toString() {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("hh:mm a");
            return start.format(fmt) + " - " + end.format(fmt);
        }
    }

    public static class BreakEntry {
        private String timeSlot;

        public BreakEntry() {}
        public BreakEntry(String timeSlot) { this.timeSlot=timeSlot; }

        public String getTimeSlot() {
            return timeSlot;
        }
        public void setTimeSlot(String timeSlot) {
            this.timeSlot = timeSlot;
        }
    }
}
