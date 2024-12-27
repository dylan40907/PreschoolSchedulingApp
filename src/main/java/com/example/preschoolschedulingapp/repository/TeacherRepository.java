package com.example.preschoolschedulingapp.repository;

import com.example.preschoolschedulingapp.model.Teacher;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeacherRepository extends CrudRepository<Teacher, Long> {
}
