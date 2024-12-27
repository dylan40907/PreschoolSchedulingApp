package com.example.preschoolschedulingapp.controller;

import com.example.preschoolschedulingapp.model.Room;
import com.example.preschoolschedulingapp.model.Schedule;
import com.example.preschoolschedulingapp.model.Teacher;
import com.example.preschoolschedulingapp.repository.RoomRepository;
import com.example.preschoolschedulingapp.repository.ScheduleRepository;
import com.example.preschoolschedulingapp.repository.TeacherRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        return "/addTeacher";
    }

    @PostMapping("/addTeacher")
    public String addTeacher(@ModelAttribute Teacher teacher, @RequestParam String availability) {
        teacher.setAvailability(availability); // Parse the availability string
        teacherRepository.save(teacher);
        return "redirect:/teacherView";
    }

    @GetMapping("/editTeacher/{id}")
    public String editTeacher(@PathVariable Long id, Model model) {
        Teacher teacher = teacherRepository.findById(id).orElse(null);
        model.addAttribute("teacher", teacher);
        return "editTeacher";
    }

    @PostMapping("/editTeacher/{id}")
    public String updateTeacher(@PathVariable Long id, @RequestParam String availability) {
        Teacher teacher = teacherRepository.findById(id).orElse(null);
        String[] times = availability.split("-");
        teacher.setStartTime(LocalTime.parse(times[0].trim()));
        teacher.setEndTime(LocalTime.parse(times[1].trim()));
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
        model.addAttribute("rooms", rooms);
        model.addAttribute("timeSlots", generateTimeSlots());
        return "newSchedule";
    }

    @PostMapping("/newSchedule")
    public String saveSchedule(@RequestParam String scheduleName, @RequestParam Map<String, String> entries) {
        Schedule schedule = new Schedule(scheduleName, entries);
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
        model.addAttribute("rooms", rooms);
        model.addAttribute("schedule", schedule);
        model.addAttribute("entries", schedule.getEntries());
        model.addAttribute("timeSlots", generateTimeSlots());
        return "editSchedule";
    }

    @PostMapping("/editSchedule/{id}")
    public String updateSchedule(@PathVariable Long id, @RequestParam Map<String, String> entries) {
        Schedule schedule = scheduleRepository.findById(id).orElseThrow();
        schedule.setEntries(entries);
        scheduleRepository.save(schedule);
        return "redirect:/viewSchedules";
    }

    @PostMapping("/deleteSchedule/{id}")
    public String deleteSchedule(@PathVariable Long id) {
        scheduleRepository.deleteById(id);
        return "redirect:/viewSchedules";
    }
    @GetMapping("/debugSchedule/{id}")
    @ResponseBody
    public String debugSchedule(@PathVariable Long id) {
        Schedule schedule = scheduleRepository.findById(id).orElse(null);
        if (schedule == null) {
            return "Schedule with ID " + id + " not found.";
        }

        StringBuilder debugInfo = new StringBuilder();
        debugInfo.append("Schedule Name: ").append(schedule.getName()).append("<br>");
        debugInfo.append("Entries:<br>");
        for (Map.Entry<String, String> entry : schedule.getEntries().entrySet()) {
            debugInfo.append(entry.getKey()).append(" -> ").append(entry.getValue()).append("<br>");
        }
        return debugInfo.toString();
    }


    private List<String> generateTimeSlots() {
        List<String> timeSlots = new ArrayList<>();
        LocalTime startTime = LocalTime.of(7, 30); // Start at 7:30 AM
        LocalTime endTime = LocalTime.of(17, 50);   // End at 6:00 PM
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");

        while (!startTime.isAfter(endTime)) {
            LocalTime nextTime = startTime.plusMinutes(10); // Add 10 minutes
            timeSlots.add(startTime.format(formatter) + " - " + nextTime.format(formatter));
            startTime = nextTime; // Move to the next time slot
        }

        return timeSlots;
    }

}
