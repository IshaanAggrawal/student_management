package com.example.lab1.controller;

import java.util.List;

import com.example.lab1.Course;
import com.example.lab1.Student;
import com.example.lab1.repository.CourseRepository;
import com.example.lab1.repository.StudentRepository;

import jakarta.validation.Valid;
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

    public HelloController(StudentRepository studentRepository, CourseRepository courseRepository) {
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
    }

    // ---------- General ----------

    @GetMapping("/hello")
    public String hello() {
        return "Hello, Spring Boot!";
    }

    @GetMapping("/student")
    public Student getStudent() {
        return new Student("Rahul", "rahul@example.com");
    }

    // ---------- Student CRUD (Exp 3, Exp 4) ----------

    @GetMapping("/students")
    public List<Student> getStudents() {
        return studentRepository.findAll();
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<Student> getStudentById(@PathVariable int id) {
        return studentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/students")
    public Student addStudent(@Valid @RequestBody Student student) {
        // The database generates the ID. Ignoring any client-supplied id stops a POST
        // from silently overwriting an existing record.
        student.setId(null);
        return studentRepository.save(student);
    }

    @PutMapping("/students/{id}")
    public ResponseEntity<Student> updateStudent(@PathVariable int id,
                                                 @Valid @RequestBody Student updatedStudent) {
        return studentRepository.findById(id)
                .map(student -> {
                    student.setName(updatedStudent.getName());
                    student.setEmail(updatedStudent.getEmail());
                    return ResponseEntity.ok(studentRepository.save(student));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/students/{id}")
    public ResponseEntity<String> deleteStudent(@PathVariable int id) {
        if (!studentRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        studentRepository.deleteById(id);
        return ResponseEntity.ok("Student deleted successfully");
    }

    // ---------- Derived queries, pagination, sorting (Exp 5) ----------

    @GetMapping("/students/search/name/{name}")
    public List<Student> searchByName(@PathVariable String name) {
        return studentRepository.findByName(name);
    }

    @GetMapping("/students/search/name/contains/{keyword}")
    public List<Student> searchByNameContaining(@PathVariable String keyword) {
        return studentRepository.findByNameContaining(keyword);
    }

    @GetMapping("/students/search/email/{email}")
    public List<Student> searchByEmail(@PathVariable String email) {
        return studentRepository.findByEmail(email);
    }

    @GetMapping("/students/page")
    public Page<Student> getStudentsPage(Pageable pageable) {
        return studentRepository.findAll(pageable);
    }

    @GetMapping("/students/sort/name")
    public List<Student> getStudentsSortedByName() {
        return studentRepository.findAll(Sort.by("name"));
    }

    // ---------- Courses / One-to-Many (Exp 6) ----------

    @GetMapping("/courses")
    public List<Course> getCourses() {
        return courseRepository.findAll();
    }

    @PostMapping("/students/{studentId}/courses")
    public ResponseEntity<Course> addCourseToStudent(@PathVariable int studentId,
                                                     @Valid @RequestBody Course course) {
        return studentRepository.findById(studentId)
                .map(student -> {
                    course.setId(null);
                    course.setStudent(student);
                    return ResponseEntity.ok(courseRepository.save(course));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
