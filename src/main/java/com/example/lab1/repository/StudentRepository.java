package com.example.lab1.repository;

import com.example.lab1.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Integer> {

    List<Student> findByName(String name);

    List<Student> findByNameContaining(String keyword);

    List<Student> findByEmail(String email);
}
