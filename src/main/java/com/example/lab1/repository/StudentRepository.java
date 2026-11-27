package com.example.lab1.repository;

import com.example.lab1.student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudentRepository extends JpaRepository<student, Integer> {
    List<student> findByName(String name);
    List<student> findByNameContaining(String keyword);
    List<student> findByEmail(String email);
}
