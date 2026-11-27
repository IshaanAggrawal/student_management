# Student Management API

A REST API for managing students and their courses, built step by step across the Enterprise Java lab (Experiments 1–9). It starts as a single controller returning hard-coded JSON and ends as a MySQL-backed service with validation, centralized error handling, automated tests and HTTP Basic authentication.

**Stack:** Java 17 · Spring Boot 4.1 · Spring Web MVC · Spring Data JPA (Hibernate) · MySQL 8 · Bean Validation · Spring Security · JUnit 5 + MockMvc · Maven

---

## Contents

- [Features](#features)
- [Project structure](#project-structure)
- [Getting started](#getting-started)
- [API reference](#api-reference)
- [Testing](#testing)
- [Experiment log](#experiment-log)
- [Design notes](#design-notes)
- [Key concepts and learning outcomes](#key-concepts-and-learning-outcomes)

---

## Features

- Full CRUD for students, persisted in MySQL through `JpaRepository`
- Search without writing SQL, using derived query methods (`findByName`, `findByNameContaining`, `findByEmail`)
- Pagination and sorting through `Pageable` and `Sort`
- One-to-many Student → Course relationship with cascading deletes
- Request validation with clear 400 responses (`@Valid`, `@RestControllerAdvice`)
- Database-generated IDs (`AUTO_INCREMENT`)
- HTTP Basic authentication. `/hello` is public and everything else needs credentials
- 11 integration tests (MockMvc) and a 43-request Postman collection with assertions

## Project structure

```
Lab1/
├── pom.xml
├── Lab1_Postman_Collection.json          # Postman collection (Exp 3–9)
├── docs/sql/
│   └── exp8_enable_auto_increment.sql    # one-time DB migration for Exp 8
└── src/
    ├── main/
    │   ├── java/com/example/lab1/
    │   │   ├── Lab1Application.java      # entry point
    │   │   ├── Student.java              # @Entity -> students table
    │   │   ├── Course.java               # @Entity -> course table (owns student_id FK)
    │   │   ├── controller/
    │   │   │   └── HelloController.java  # all REST endpoints
    │   │   ├── repository/
    │   │   │   ├── StudentRepository.java
    │   │   │   └── CourseRepository.java
    │   │   ├── exception/
    │   │   │   └── GlobalExceptionHandler.java
    │   │   └── config/
    │   │       └── SecurityConfig.java
    │   └── resources/
    │       └── application.properties
    └── test/java/com/example/lab1/
        └── Lab1ApplicationTests.java
```

## Getting started

### Prerequisites

| Tool  | Version |
|-------|---------|
| JDK   | 17 or newer |
| MySQL | 8.x, running on `localhost:3306` |
| Maven | not required, the project ships with the Maven Wrapper (`mvnw`) |

### 1. Clone

```bash
git clone https://github.com/IshaanAggrawal/student_management.git
cd student_management
```

### 2. Configure the database

The app connects to a database called `java_spring` and creates it on first start (`createDatabaseIfNotExist=true`). Hibernate creates the `students` and `course` tables on its own (`ddl-auto=update`).

Credentials are read from environment variables, with local defaults:

| Variable       | Default    | Used for |
|----------------|------------|----------|
| `DB_USERNAME`  | `root`     | MySQL user |
| `DB_PASSWORD`  | `root123`  | MySQL password |
| `APP_USERNAME` | `admin`    | API Basic auth user |
| `APP_PASSWORD` | `admin123` | API Basic auth password |

If your MySQL password is different, set it before running:

```bash
# Windows (cmd)
set DB_PASSWORD=your_password

# macOS / Linux
export DB_PASSWORD=your_password
```

> **Upgrading a database created before Exp 8?** Hibernate won't change an existing primary key, so run the migration once:
> `mysql -u root -p < docs/sql/exp8_enable_auto_increment.sql`
> If you'd rather start fresh, `DROP TABLE course; DROP TABLE students;` and restart the app.

### 3. Run

```bash
# Windows
mvnw.cmd spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

You can also run `Lab1Application` from IntelliJ. The server starts on **http://localhost:8081**.

```bash
curl http://localhost:8081/hello                          # public -> Hello, Spring Boot!
curl -u admin:admin123 http://localhost:8081/students     # authenticated
```

## API reference

All endpoints except `GET /hello` require HTTP Basic auth.

### Students

| Method | Endpoint | Description | Success | Errors |
|--------|----------|-------------|---------|--------|
| GET    | `/students` | List all students (with courses) | 200 | 401 |
| GET    | `/students/{id}` | Get one student | 200 | 400, 401, 404 |
| POST   | `/students` | Create a student (ID is generated) | 200 | 400, 401 |
| PUT    | `/students/{id}` | Update name and email | 200 | 400, 401, 404 |
| DELETE | `/students/{id}` | Delete a student and their courses | 200 | 401, 404 |

### Search, pagination and sorting

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/students/search/name/{name}` | Exact name match |
| GET | `/students/search/name/contains/{keyword}` | Name contains keyword (`LIKE %kw%`) |
| GET | `/students/search/email/{email}` | Exact email match |
| GET | `/students/page?page=0&size=2&sort=name,desc` | Paginated list (`page`, `size`, `sort` all optional) |
| GET | `/students/sort/name` | All students sorted A–Z |

### Courses

| Method | Endpoint | Description | Success | Errors |
|--------|----------|-------------|---------|--------|
| GET    | `/courses` | List all courses | 200 | 401 |
| POST   | `/students/{studentId}/courses` | Add a course to a student | 200 | 400, 401, 404 |

### Misc

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/hello` | public | Health-style greeting |
| GET | `/student` | required | Sample student object (not stored) |

### Request and response examples

**Create a student**

```bash
curl -i -u admin:admin123 -X POST http://localhost:8081/students \
  -H "Content-Type: application/json" \
  -d '{"name":"Karan Verma","email":"karan@example.com"}'
```

```json
{ "id": 11, "name": "Karan Verma", "email": "karan@example.com", "courses": [] }
```

**Add a course**

```bash
curl -u admin:admin123 -X POST http://localhost:8081/students/1/courses \
  -H "Content-Type: application/json" \
  -d '{"courseName":"Enterprise Java","credits":4}'
```

**Validation failure** (`400 Bad Request`)

```json
{
  "name": "Student name is required",
  "email": "Please provide a valid email address"
}
```

Malformed JSON and non-numeric IDs also return 400, with an `{"error": "..."}` body.

> On Windows `cmd`, single quotes don't work for the body. Escape the double quotes instead:
> `-d "{\"name\":\"Karan Verma\",\"email\":\"karan@example.com\"}"`

### Validation rules

| Entity  | Field        | Rule |
|---------|--------------|------|
| Student | `name`       | not blank |
| Student | `email`      | not blank, valid email format |
| Course  | `courseName` | not blank |
| Course  | `credits`    | greater than 0 |

## Testing

### Automated tests (JUnit 5 + MockMvc)

```bash
mvnw.cmd test        # Windows
./mvnw test          # macOS / Linux
```

The tests boot the full application context against the configured MySQL database, so MySQL must be running. Tests that write data are `@Transactional`, so their changes are rolled back and the database is left as it was.

| Area | What's covered |
|------|----------------|
| Basics | context loads, `/hello` returns the greeting |
| Validation | invalid student → 400 with field messages; invalid course → 400; malformed JSON → 400 |
| CRUD | create → read → update → delete → 404 lifecycle; unknown ID → 404 |
| Data integrity | a rejected PUT doesn't modify the stored record |
| Security | no credentials → 401; wrong password → 401; valid credentials → 200 |

### Postman

Import `Lab1_Postman_Collection.json` into Postman. The collection:

- uses Basic auth from the collection variables `username` / `password`
- stores generated IDs (`studentId`, `courseId`, …) so later requests use real data
- has a test script on every request, so **Run collection** gives a pass/fail report

Folders follow the experiments: CRUD (3–4) → queries (5) → relationships (6) → validation (7) → lifecycle (8) → security (9).

### cURL walkthrough (Exp 8 / 9)

```bash
curl -i http://localhost:8081/hello                                   # 200, public
curl -i http://localhost:8081/students                                # 401
curl -i -u admin:wrong123 http://localhost:8081/students              # 401
curl -i -u admin:admin123 http://localhost:8081/students              # 200
curl -i -u admin:admin123 -X DELETE http://localhost:8081/students/11 # 200 "Student deleted successfully"
curl -i -u admin:admin123 http://localhost:8081/students/11           # 404
```

## Experiment log

| # | Topic | What was implemented |
|---|-------|----------------------|
| 1–2 | Setup | JDK, IntelliJ, Spring Initializr project, `/hello` and `/student` endpoints, server on port 8081 |
| 3 | REST CRUD | `GET/POST/PUT/DELETE /students` on an in-memory `ArrayList`; `@RequestBody`, `@PathVariable`, `ResponseEntity` with 404 handling |
| 4 | JPA + MySQL | Added Spring Data JPA and MySQL Connector/J, turned `Student` into an `@Entity`, created `StudentRepository`, and rewired CRUD to the database |
| 5 | Repository queries | Derived queries (`findByName`, `findByNameContaining`, `findByEmail`), pagination via `Pageable`, sorting via `Sort` |
| 6 | Relationships | `Course` entity, `@OneToMany` / `@ManyToOne` with `student_id` FK, `CourseRepository`, `POST /students/{id}/courses`, recursion handled with `@JsonManagedReference` / `@JsonBackReference` |
| 7 | Validation | `spring-boot-starter-validation`, `@NotBlank` / `@Email` constraints, `@Valid` on POST and PUT, `GlobalExceptionHandler` returning field-level 400 errors |
| 8 | Testing | Switched to DB-generated IDs (`Integer` + `IDENTITY`), migrated the existing table, added MockMvc integration tests, and ran the cURL test sequence |
| 9 | Security | `spring-boot-starter-security`, configurable credentials, and a `SecurityFilterChain` with HTTP Basic: `/hello` public, everything else authenticated |

Experiments 10–11 (Docker / deployment) are outside the scope of this submission.

## Design notes

A few choices go slightly beyond the lab handouts. Each one is here for a reason:

- **POST ignores a client-supplied `id`.** With generated IDs, saving an entity that already carries an ID makes JPA *merge* it, which would let a POST overwrite an existing student. The controller clears the ID so POST always creates.
- **Validation messages are kept on the annotations.** The Exp 8 handout suggests a bare `@NotBlank`, but the Exp 8 test asserts the custom messages. Keeping the messages makes both work.
- **Courses are validated too.** `courseName` must be present and `credits` positive, using the same `@Valid` + handler pattern as students.
- **Stateless security.** The API uses Basic auth on every request and never creates a session, which is also why disabling CSRF is safe here. It would not be safe for a cookie-based browser app.
- **No secrets hard-wired.** DB and API credentials come from environment variables. The defaults in `application.properties` are for local development only.
- **The SQL migration looks up the FK name.** Hibernate generates foreign-key names like `FKk8cjbc26…`, which differ between machines. The script queries `information_schema` instead of hard-coding one.

## Key concepts and learning outcomes

- **REST fundamentals.** Mapping HTTP verbs to CRUD, returning proper status codes (200, 400, 401, 404), and using `ResponseEntity` for responses that may be empty.
- **Dependency injection.** Repositories are injected through the controller constructor. Spring manages their lifecycle and provides the implementations at runtime.
- **ORM with JPA/Hibernate.** Entities map to tables, `ddl-auto` manages the schema, and `JpaRepository` provides CRUD without SQL.
- **Derived queries.** Spring Data parses method names (`findByNameContaining` → `WHERE name LIKE %?%`) into queries.
- **Pagination and sorting.** `Pageable` and `Sort` are resolved straight from query parameters (`?page=&size=&sort=`), which keeps large result sets manageable.
- **Entity relationships.** The `@ManyToOne` side owns the foreign key, `mappedBy` marks the inverse side, and cascade with orphan removal ties a course's lifecycle to its student.
- **JSON serialization pitfalls.** Bidirectional relationships recurse forever unless one side is marked as the back reference.
- **Bean Validation.** Constraints are declared on the model and enforced at the API boundary with `@Valid`. Invalid data never reaches the database.
- **Centralized error handling.** `@RestControllerAdvice` keeps error formatting out of controllers and gives clients consistent responses.
- **Automated testing.** MockMvc exercises the full request pipeline (security, validation, persistence) without starting a server, and transactional tests roll back their changes.
- **Authentication with Spring Security.** The filter chain authenticates the user before the request reaches any controller, and rules are defined per endpoint.

---

**Author:** Ishaan Aggrawal, B.Tech CSE, IIIT Sonepat
Enterprise Java Lab
