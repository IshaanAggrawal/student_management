package com.example.lab1.controller;

import java.util.List;

import com.example.lab1.Course;
import com.example.lab1.student;
import com.example.lab1.repository.CourseRepository;
import com.example.lab1.repository.StudentRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    public HelloController(
            StudentRepository studentRepository,
            CourseRepository courseRepository) {
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
    }

    @GetMapping("/courses")
    public List<Course> getCourses() {
        return courseRepository.findAll();
    }

    @PostMapping("/students/{studentId}/courses")
    public ResponseEntity<Course> addCourseToStudent(
            @PathVariable int studentId,
            @RequestBody Course course) {
        return studentRepository.findById(studentId)
                .map(student -> {
                    course.setStudent(student);
                    Course savedCourse = courseRepository.save(course);
                    return ResponseEntity.ok(savedCourse);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello, Spring Boot!";
    }

    @GetMapping("/student")
    public student getStudent() {
        return new student(
                1,
                "Rahul",
                "rahul@example.com"
        );
    }

    // GET all students
    @GetMapping("/students")
    public List<student> getStudents() {
        return studentRepository.findAll();
    }

    // GET students with pagination
    @GetMapping("/students/page")
    public Page<student> getStudentsPage(Pageable pageable) {
        return studentRepository.findAll(pageable);
    }

    // GET students sorted by name
    @GetMapping("/students/sort/name")
    public List<student> getStudentsSortedByName() {
        return studentRepository.findAll(Sort.by("name"));
    }

    @GetMapping("/students/search/name/{name}")
    public List<student> searchByName(@PathVariable String name) {
        return studentRepository.findByName(name);
    }

    @GetMapping("/students/search/name/contains/{keyword}")
    public List<student> searchByNameContaining(@PathVariable String keyword) {
        return studentRepository.findByNameContaining(keyword);
    }

    @GetMapping("/students/search/email/{email}")
    public List<student> searchByEmail(@PathVariable String email) {
        return studentRepository.findByEmail(email);
    }

    // POST - add a new student
    @PostMapping("/students")
    public student addStudent(@RequestBody student student) {
        return studentRepository.save(student);
    }

    // GET student by ID
    @GetMapping("/students/{id}")
    public ResponseEntity<student> getStudentById(@PathVariable int id) {
        return studentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // PUT - update an existing student
    @PutMapping("/students/{id}")
    public ResponseEntity<student> updateStudent(
            @PathVariable int id,
            @RequestBody student updatedStudent) {

        return studentRepository.findById(id)
                .map(student -> {
                    student.setName(updatedStudent.getName());
                    student.setEmail(updatedStudent.getEmail());

                    student savedStudent = studentRepository.save(student);

                    return ResponseEntity.ok(savedStudent);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE - delete a student
    @DeleteMapping("/students/{id}")
    public ResponseEntity<String> deleteStudent(@PathVariable int id) {

        if (!studentRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        studentRepository.deleteById(id);

        return ResponseEntity.ok("Student deleted successfully");
    }
}
