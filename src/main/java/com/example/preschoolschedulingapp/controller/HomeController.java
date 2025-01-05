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
            @ModelAttribute Teacher teacher,
            @RequestParam String availability,
            @RequestParam List<Long> preferredRooms // Capture selected room IDs
    ) {
        teacher.setAvailability(availability); // Parse the availability string
        List<Room> rooms = (List<Room>) roomRepository.findAllById(preferredRooms); // Fetch Room entities
        teacher.setPreferredRooms(new HashSet<>(rooms)); // Set preferred rooms
        teacherRepository.save(teacher);
        return "redirect:/teacherView";
    }


    @GetMapping("/editTeacher/{id}")
    public String editTeacher(@PathVariable Long id, Model model) {
        Teacher teacher = teacherRepository.findById(id).orElse(null);
        List<Room> rooms = (List<Room>) roomRepository.findAll(); // Fetch all rooms
        model.addAttribute("teacher", teacher);
        model.addAttribute("rooms", rooms); // Pass rooms to the model
        return "editTeacher";
    }


    @PostMapping("/editTeacher/{id}")
    public String updateTeacher(
            @PathVariable Long id,
            @RequestParam String availability,
            @RequestParam List<Long> preferredRooms // Capture selected room IDs
    ) {
        Teacher teacher = teacherRepository.findById(id).orElse(null);
        String[] times = availability.split("-");
        teacher.setStartTime(LocalTime.parse(times[0].trim()));
        teacher.setEndTime(LocalTime.parse(times[1].trim()));
        List<Room> rooms = (List<Room>) roomRepository.findAllById(preferredRooms); // Fetch Room entities
        teacher.setPreferredRooms(new HashSet<>(rooms)); // Update preferred rooms
        teacherRepository.save(teacher);
        return "redirect:/teacherView";
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
    public String updateRoom(@PathVariable Long id, @RequestParam String name, @RequestParam int capacity) {
        Room room = roomRepository.findById(id).orElse(null);
        room.setName(name); // Room name remains constant
        room.setCapacity(capacity);
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

    @GetMapping("/generateSchedule/{scheduleId}")
    public String generateSchedule(@PathVariable Long scheduleId, Model model) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found with id: " + scheduleId));
        List<Room> rooms = (List<Room>) roomRepository.findAll();
        List<Teacher> allTeachers = (List<Teacher>) teacherRepository.findAll();

        // Map to store room assignments (key: time slot, value: list of entries)
        Map<String, List<Entry>> roomAssignments = new HashMap<>();

        // Map to track breaks for each time slot
        Map<String, Map<String, Integer>> breaksByTimeSlot = new HashMap<>();

        // Get all time slots for the schedule
        List<String> timeSlots = generateTimeSlots();

        // Debugging: Print available time slots
        System.out.println("Time Slots:");
        timeSlots.forEach(System.out::println);

        // Populate room assignments and calculate breaks
        for (String timeSlot : timeSlots) {
            System.out.println("Processing time slot: " + timeSlot);

            // Global pool of available teachers for the time slot
            List<Teacher> availableTeachersForSlot = findAvailableTeachers(allTeachers, timeSlot);

            for (Room room : rooms) {
                // Find entries for the current room and time slot
                for (Entry entry : schedule.getEntries()) {
                    if (entry.getTimeSlot().equals(timeSlot) && entry.getRoomId().equals(room.getId().toString())) {
                        // Add the entry to the corresponding room and time slot key
                        String key = room.getId() + "_" + timeSlot;
                        roomAssignments.computeIfAbsent(key, k -> new ArrayList<>()).add(entry);

                        // Assign teachers to the entry
                        List<Teacher> assignedTeachers = assignTeachers(availableTeachersForSlot, entry.getTeachersRequired(), timeSlot);

                        // Update the entry with assigned teachers
                        entry.setAssignedTeachers(assignedTeachers);

                        // Remove assigned teachers from the global pool
                        availableTeachersForSlot.removeAll(assignedTeachers);

                        // Debugging: Print assigned teachers for each entry
                        System.out.println("Assigned teachers for room: " + room.getName() + ", time slot: " + timeSlot);
                        assignedTeachers.forEach(teacher -> System.out.println(" - " + teacher.getName()));
                    }
                }
            }

            // Calculate breaks for the current time slot
            Map<String, Integer> breaks = calculateBreaksForTimeSlot(allTeachers, roomAssignments, timeSlot);
            breaksByTimeSlot.put(timeSlot, breaks);

            // Debugging: Print breaks for the current time slot
            System.out.println("Breaks for time slot: " + timeSlot);
            breaks.forEach((teacherName, breakDuration) ->
                    System.out.println(teacherName + " -> " + breakDuration + " minutes"));
        }

        // Add attributes to the model
        model.addAttribute("schedule", schedule);
        model.addAttribute("rooms", rooms);
        model.addAttribute("scheduleWithAssignments", roomAssignments);
        model.addAttribute("breaks", breaksByTimeSlot);
        model.addAttribute("timeSlots", timeSlots);

        return "generateSchedule";
    }


    private Map<String, Integer> calculateBreaksForTimeSlot(
            List<Teacher> teachers,
            Map<String, List<Entry>> roomAssignments,
            String timeSlot
    ) {
        Map<String, Integer> breakAssignments = new HashMap<>();

        System.out.println("Calculating breaks for time slot: " + timeSlot);

        for (Teacher teacher : teachers) {
            // Calculate total minutes worked up to the given time slot
            long totalMinutesWorked = calculateMinutesWorked(teacher, roomAssignments, timeSlot);

            // Debugging: Print total minutes worked for the teacher
            System.out.println("Teacher: " + teacher.getName() + ", Total Minutes Worked: " + totalMinutesWorked);

            // Determine the required break duration
            int breakDuration = determineBreakDuration(totalMinutesWorked);

            if (breakDuration > 0) {
                breakAssignments.put(teacher.getName(), breakDuration);

                // Debugging: Print assigned break duration
                System.out.println("Assigned Break: " + teacher.getName() + " -> " + breakDuration + " minutes");
            }
        }

        return breakAssignments;
    }




    // Calculate the total minutes worked by a teacher up to the given timeSlot
    private long calculateMinutesWorked(Teacher teacher, Map<String, List<Entry>> roomAssignments, String timeSlot) {
        long totalMinutes = 0;

        // Parse the current time slot into a LocalTime range
        String[] targetTimes = timeSlot.split(" - ");
        LocalTime targetStart = LocalTime.parse(targetTimes[0], DateTimeFormatter.ofPattern("hh:mm a"));

        for (Map.Entry<String, List<Entry>> entrySet : roomAssignments.entrySet()) {
            // Extract the time slot portion from the key
            String[] keyParts = entrySet.getKey().split("_");
            String currentSlot = keyParts.length > 1 ? keyParts[1] : "";

            // Parse the current slot into a LocalTime range
            String[] currentTimes = currentSlot.split(" - ");
            LocalTime currentEnd = LocalTime.parse(currentTimes[1], DateTimeFormatter.ofPattern("hh:mm a"));

            // Only consider time slots up to and including the given time slot
            if (currentEnd.isAfter(targetStart)) {
                continue;
            }

            for (Entry entry : entrySet.getValue()) {
                // Check if the teacher is assigned to this entry
                if (entry.getAssignedTeachers() != null && entry.getAssignedTeachers().contains(teacher)) {
                    // Parse the time slot string to calculate the duration
                    LocalTime start = LocalTime.parse(currentTimes[0], DateTimeFormatter.ofPattern("hh:mm a"));
                    LocalTime end = LocalTime.parse(currentTimes[1], DateTimeFormatter.ofPattern("hh:mm a"));

                    totalMinutes += ChronoUnit.MINUTES.between(start, end);
                }
            }
        }

        return totalMinutes;
    }



    // Determine break duration based on total minutes worked
    private int determineBreakDuration(long totalMinutesWorked) {
        if (totalMinutesWorked >= 375) {
            return 30; // More than 6.25 hours -> requires another 30-minute break
        } else if (totalMinutesWorked >= 360) {
            return 10; // At 6 hours, add a 10-minute break
        } else if (totalMinutesWorked >= 300) {
            return 30; // After 5 hours, add a 30-minute break
        } else if (totalMinutesWorked >= 210) {
            return 10; // At 3.5 hours, add a 10-minute break
        }
        return 0; // No break needed
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
