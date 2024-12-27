package com.example.preschoolschedulingapp;

import com.example.preschoolschedulingapp.model.Teacher;
import com.example.preschoolschedulingapp.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PreschoolSchedulingAppApplication implements CommandLineRunner {

    @Autowired
    private TeacherRepository teacherRepository;

    public static void main(String[] args) {
        SpringApplication.run(PreschoolSchedulingAppApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

    }
}
