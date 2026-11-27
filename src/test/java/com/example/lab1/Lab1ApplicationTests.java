package com.example.lab1;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests that run against the configured MySQL database.
 * Tests that write data are @Transactional, so every change is rolled back afterwards.
 */
@SpringBootTest
@AutoConfigureMockMvc
class Lab1ApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Value("${spring.security.user.name}")
    private String username;

    @Value("${spring.security.user.password}")
    private String password;

    private String adminAuth;

    @BeforeEach
    void setUpCredentials() {
        adminAuth = basicAuth(username, password);
    }

    private static String basicAuth(String username, String password) {
        String token = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void contextLoads() {
    }

    // ---------- Exp 8: basic endpoint ----------

    @Test
    void helloEndpointReturnsMessage() throws Exception {
        mockMvc.perform(get("/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello, Spring Boot!"));
    }

    // ---------- Exp 7 / 8: validation ----------

    @Test
    void invalidStudentDataReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/students")
                        .header(HttpHeaders.AUTHORIZATION, adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Student name is required"))
                .andExpect(jsonPath("$.email").value("Please provide a valid email address"));
    }

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/students")
                        .header(HttpHeaders.AUTHORIZATION, adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Karan\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @Transactional
    void invalidCourseDataReturnsBadRequest() throws Exception {
        int id = createStudent("Course Validation", "course.validation@example.com");

        mockMvc.perform(post("/students/" + id + "/courses")
                        .header(HttpHeaders.AUTHORIZATION, adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseName\":\"\",\"credits\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.courseName").value("Course name is required"))
                .andExpect(jsonPath("$.credits").value("Credits must be a positive number"));
    }

    // ---------- Exp 8: full CRUD lifecycle ----------

    @Test
    @Transactional
    void studentCrudLifecycle() throws Exception {
        int id = createStudent("Karan Verma", "karan@example.com");

        mockMvc.perform(get("/students/" + id).header(HttpHeaders.AUTHORIZATION, adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Karan Verma"));

        mockMvc.perform(put("/students/" + id)
                        .header(HttpHeaders.AUTHORIZATION, adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Karan Sharma\",\"email\":\"karan.sharma@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Karan Sharma"))
                .andExpect(jsonPath("$.email").value("karan.sharma@example.com"));

        mockMvc.perform(delete("/students/" + id).header(HttpHeaders.AUTHORIZATION, adminAuth))
                .andExpect(status().isOk())
                .andExpect(content().string("Student deleted successfully"));

        mockMvc.perform(get("/students/" + id).header(HttpHeaders.AUTHORIZATION, adminAuth))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void invalidUpdateDoesNotChangeStoredStudent() throws Exception {
        int id = createStudent("Neha Sharma", "neha@example.com");

        mockMvc.perform(put("/students/" + id)
                        .header(HttpHeaders.AUTHORIZATION, adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"invalid-email\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/students/" + id).header(HttpHeaders.AUTHORIZATION, adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("neha@example.com"));
    }

    @Test
    void unknownStudentReturnsNotFound() throws Exception {
        mockMvc.perform(get("/students/999999").header(HttpHeaders.AUTHORIZATION, adminAuth))
                .andExpect(status().isNotFound());
    }

    // ---------- Exp 9: security ----------

    @Test
    void protectedEndpointWithoutCredentialsReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/students"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithWrongPasswordReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/students").header(HttpHeaders.AUTHORIZATION, basicAuth(username, "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithValidCredentialsReturnsOk() throws Exception {
        mockMvc.perform(get("/courses").header(HttpHeaders.AUTHORIZATION, adminAuth))
                .andExpect(status().isOk());
    }

    // ---------- helpers ----------

    private int createStudent(String name, String email) throws Exception {
        String body = mockMvc.perform(post("/students")
                        .header(HttpHeaders.AUTHORIZATION, adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"email\":\"" + email + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.id");
    }
}
