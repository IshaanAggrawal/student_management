# Student Management API

A Spring Boot REST service for managing students and the courses they take. I built it incrementally over the Enterprise Java lab: the first version served hard-coded JSON from one controller, and the final version stores data in MySQL, validates input, handles errors in one place, has automated tests, and sits behind HTTP Basic authentication.

| | |
|---|---|
| **Name** | Ishaan Aggrawal |
| **Roll No.** | 12411004 |
| **Class** | CSE-A, Batch 2024–2028 |
| **Institute** | IIIT Sonepat |
| **Course** | Enterprise Java Lab (Experiments 1–9) |

**Built with:** Java 17, Spring Boot 4.1, Spring Web MVC, Spring Data JPA (Hibernate), MySQL 8, Bean Validation, Spring Security, JUnit 5 with MockMvc, Maven

---

## Experiments

| Exp | Title | Status |
|-----|-------|--------|
| 1–2 | Basic concepts, project setup and installation of required software | ✅ Completed |
| 3 | CRUD REST API with in-memory storage | ✅ Completed |
| 4 | MySQL connectivity with a JPA entity and Spring Data repository | ✅ Completed |
| 5 | Spring Data JPA: derived queries, pagination and sorting | ✅ Completed |
| 6 | One-to-many relationship (Student → Course) | ✅ Completed |
| 7 | Input validation and centralized exception handling (`@Valid`, `@ExceptionHandler`) | ✅ Completed |
| 8 | REST API testing with cURL and automated tests (JUnit + MockMvc) | ✅ Completed |
| 9 | HTTP Basic authentication with Spring Security | ✅ Completed |
| 10–11 | Deployment with Docker | Not covered in this course |

A detailed breakdown of what each experiment added is in [Experiment-wise progress](#experiment-wise-progress).

---

## Table of contents

- [Experiments](#experiments)
- [What it does](#what-it-does)
- [Repository layout](#repository-layout)
- [Running locally](#running-locally)
- [Endpoints](#endpoints)
- [Testing](#testing)
- [Experiment-wise progress](#experiment-wise-progress)
- [Implementation decisions](#implementation-decisions)
- [What I learned](#what-i-learned)

---

## What it does

- Create, read, update and delete students, with every change saved to MySQL via `JpaRepository`
- Look students up by exact name, partial name or email using derived query methods, so no SQL is written by hand
- Return results one page at a time and in sorted order using `Pageable` and `Sort`
- Link each student to many courses; deleting a student also removes their courses
- Reject bad input with a 400 response that names each problem field
- Let MySQL assign student IDs (`AUTO_INCREMENT`)
- Require a username and password for every endpoint except `/hello`
- Come with 11 MockMvc integration tests and a Postman collection of 43 requests, each with assertions

## Repository layout

```
Lab1/
├── pom.xml
├── Lab1_Postman_Collection.json          # Postman requests for Exp 3–9
├── docs/sql/
│   └── exp8_enable_auto_increment.sql    # run once when upgrading to Exp 8
└── src/
    ├── main/
    │   ├── java/com/example/lab1/
    │   │   ├── Lab1Application.java      # Spring Boot entry point
    │   │   ├── Student.java              # entity mapped to the students table
    │   │   ├── Course.java               # entity mapped to course; holds the student_id foreign key
    │   │   ├── controller/
    │   │   │   └── HelloController.java  # every REST endpoint
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

## Running locally

### Requirements

| Tool  | Version |
|-------|---------|
| JDK   | 17+ |
| MySQL | 8.x, listening on `localhost:3306` |
| Maven | not needed; the bundled wrapper (`mvnw`) downloads it |

### Step 1: Get the code

```bash
git clone https://github.com/IshaanAggrawal/student_management.git
cd student_management
```

### Step 2: Set up MySQL

You don't have to create anything manually. On first start the app creates a database named `java_spring` (`createDatabaseIfNotExist=true`), and Hibernate builds the `students` and `course` tables (`ddl-auto=update`).

Credentials come from environment variables. If a variable isn't set, the local default below is used:

| Variable       | Default    | Purpose |
|----------------|------------|---------|
| `DB_USERNAME`  | `root`     | MySQL username |
| `DB_PASSWORD`  | `root123`  | MySQL password |
| `APP_USERNAME` | `admin`    | API login username |
| `APP_PASSWORD` | `admin123` | API login password |

If your MySQL password is something else, export it first:

```bash
# Windows (cmd)
set DB_PASSWORD=your_password

# macOS / Linux
export DB_PASSWORD=your_password
```

> **Database left over from Exp 4–7?** Hibernate won't modify an existing primary key, so the `id` column won't become auto-increment on its own. Run the migration once:
> `mysql -u root -p < docs/sql/exp8_enable_auto_increment.sql`
> You can also drop both tables (`DROP TABLE course; DROP TABLE students;`) and let the app recreate them.

### Step 3: Start the app

```bash
# Windows
mvnw.cmd spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

Running `Lab1Application` from IntelliJ works too. The API listens on **http://localhost:8081**.

Quick check:

```bash
curl http://localhost:8081/hello                          # no login needed -> Hello, Spring Boot!
curl -u admin:admin123 http://localhost:8081/students     # needs login
```

## Endpoints

`GET /hello` is open to everyone. All other endpoints need HTTP Basic credentials.

### Students

| Method | Path | Purpose | On success | Possible errors |
|--------|------|---------|------------|-----------------|
| GET    | `/students` | List all students with their courses | 200 | 401 |
| GET    | `/students/{id}` | Fetch a single student | 200 | 400, 401, 404 |
| POST   | `/students` | Add a student; the ID is assigned by the database | 200 | 400, 401 |
| PUT    | `/students/{id}` | Change a student's name and email | 200 | 400, 401, 404 |
| DELETE | `/students/{id}` | Remove a student along with their courses | 200 | 401, 404 |

### Searching, paging and sorting

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/students/search/name/{name}` | Students whose name matches exactly |
| GET | `/students/search/name/contains/{keyword}` | Students whose name includes the keyword (`LIKE %keyword%`) |
| GET | `/students/search/email/{email}` | Students with that exact email |
| GET | `/students/page?page=0&size=2&sort=name,desc` | One page of results; `page`, `size` and `sort` are all optional |
| GET | `/students/sort/name` | Every student, alphabetically by name |

### Courses

| Method | Path | Purpose | On success | Possible errors |
|--------|------|---------|------------|-----------------|
| GET    | `/courses` | List every course | 200 | 401 |
| POST   | `/students/{studentId}/courses` | Enrol a student in a new course | 200 | 400, 401, 404 |

### Other

| Method | Path | Login | Purpose |
|--------|------|-------|---------|
| GET | `/hello` | not required | Simple greeting, useful as a liveness check |
| GET | `/student` | required | Returns a sample student; nothing is saved |

### Examples

**Adding a student**

```bash
curl -i -u admin:admin123 -X POST http://localhost:8081/students \
  -H "Content-Type: application/json" \
  -d '{"name":"Karan Verma","email":"karan@example.com"}'
```

```json
{ "id": 11, "name": "Karan Verma", "email": "karan@example.com", "courses": [] }
```

**Enrolling a student in a course**

```bash
curl -u admin:admin123 -X POST http://localhost:8081/students/1/courses \
  -H "Content-Type: application/json" \
  -d '{"courseName":"Enterprise Java","credits":4}'
```

**What a validation error looks like** (`400 Bad Request`)

```json
{
  "name": "Student name is required",
  "email": "Please provide a valid email address"
}
```

Broken JSON and non-numeric IDs (for example `/students/abc`) also get a 400, with the body `{"error": "..."}`.

> Windows `cmd` doesn't accept single quotes around the body, so escape the inner double quotes:
> `-d "{\"name\":\"Karan Verma\",\"email\":\"karan@example.com\"}"`

### Input rules

| Entity  | Field        | Requirement |
|---------|--------------|-------------|
| Student | `name`       | must not be empty or blank |
| Student | `email`      | must not be blank and must be a valid email address |
| Course  | `courseName` | must not be empty or blank |
| Course  | `credits`    | must be greater than zero |

## Testing

### JUnit + MockMvc

```bash
mvnw.cmd test        # Windows
./mvnw test          # macOS / Linux
```

These are integration tests: they start the whole application and talk to the MySQL database from `application.properties`, so MySQL has to be running. Any test that writes data is marked `@Transactional` and rolls back when it finishes, so your data stays as it was.

| Group | Checks |
|-------|--------|
| Startup | the Spring context starts; `/hello` returns the expected text |
| Validation | bad student data returns 400 with per-field messages; bad course data returns 400; broken JSON returns 400 |
| CRUD | a student can be created, read, updated and deleted, after which it returns 404; an ID that doesn't exist returns 404 |
| Data integrity | a rejected update leaves the stored record unchanged |
| Security | a request with no login gets 401, a wrong password gets 401, and correct credentials get 200 |

### Postman

Import `Lab1_Postman_Collection.json` into Postman. How the collection works:

- Requests log in with the `username` and `password` collection variables.
- When a request creates something, its ID is saved (`studentId`, `courseId`, and so on) so later requests use real records.
- Every request has a test script, so **Run collection** gives you a full pass/fail report.

There is one folder per experiment, in this order: CRUD (Exp 3–4), queries (5), relationships (6), validation (7), the end-to-end lifecycle (8) and security (9).

### Manual checks with cURL (Exp 8 and 9)

```bash
curl -i http://localhost:8081/hello                                   # 200, no login
curl -i http://localhost:8081/students                                # 401
curl -i -u admin:wrong123 http://localhost:8081/students              # 401
curl -i -u admin:admin123 http://localhost:8081/students              # 200
curl -i -u admin:admin123 -X DELETE http://localhost:8081/students/11 # 200 "Student deleted successfully"
curl -i -u admin:admin123 http://localhost:8081/students/11           # 404
```

## Experiment-wise progress

| Exp | Focus | Work done |
|-----|-------|-----------|
| 1–2 | Setup and installation | Covered the basic concepts and installed the required software (JDK 17 and IntelliJ IDEA). Created the Spring Boot project with Spring Initializr and checked that it runs on port 8081 |
| 3 | CRUD over REST | Built `GET`, `POST`, `PUT` and `DELETE` on `/students`, backed by an in-memory `ArrayList`; used `@RequestBody`, `@PathVariable`, and `ResponseEntity` to return 404 for missing students |
| 4 | Persistence | Installed MySQL 8 and created the database, added Spring Data JPA and the MySQL driver, annotated `Student` as an `@Entity`, wrote `StudentRepository`, and switched the CRUD endpoints from the list to the database |
| 5 | Querying | Added derived queries (`findByName`, `findByNameContaining`, `findByEmail`), paging with `Pageable` and sorting with `Sort` |
| 6 | Relationships | Created the `Course` entity, linked it to `Student` with `@OneToMany` / `@ManyToOne` through a `student_id` foreign key, added `CourseRepository` and `POST /students/{id}/courses`, and stopped infinite JSON nesting with `@JsonManagedReference` / `@JsonBackReference` |
| 7 | Validation | Added `spring-boot-starter-validation`, put `@NotBlank` and `@Email` on the fields, enabled checking with `@Valid` on POST and PUT, and wrote `GlobalExceptionHandler` so failures return field-by-field 400 responses |
| 8 | Testing | Moved to database-generated IDs (`Integer` with `IDENTITY`), migrated the existing table, wrote MockMvc integration tests, and ran the cURL test sequence |
| 9 | Security | Added `spring-boot-starter-security`, made the login configurable, and defined a `SecurityFilterChain` with HTTP Basic that leaves `/hello` public and protects everything else |

Experiments 10 and 11 (Docker and deployment) were not part of this submission.

## Implementation decisions

These are the places where I went slightly past the lab handouts, and why:

- **POST always creates a new student.** Once IDs are generated, saving an entity that already has an ID makes JPA treat it as an update (a merge). If a client sent an existing ID, a POST could overwrite that student. To prevent that, the controller clears any incoming ID.
- **Custom validation messages stay.** The Exp 8 handout switches to a plain `@NotBlank`, but the Exp 8 test checks for the custom messages. I kept the messages so both parts of the handout agree.
- **Courses get validated as well.** A course needs a name and a positive credit count, enforced with the same `@Valid` plus handler approach as students.
- **No server-side sessions.** The client sends Basic credentials on every request and the server never opens a session. That is why turning off CSRF protection is acceptable here; it would not be in a browser app that logs in with cookies.
- **Credentials live outside the code.** Database and API passwords come from environment variables. The fallback values in `application.properties` are only meant for local development.
- **The migration finds the foreign key by itself.** Hibernate gives foreign keys generated names (like `FKk8cjbc26…`) that differ between machines. Instead of hard-coding one, the SQL script looks the name up in `information_schema`.

## What I learned

- **REST design:** mapping HTTP methods onto create, read, update and delete; choosing the right status code (200, 400, 401, 404); and using `ResponseEntity` when a response may have no body.
- **Dependency injection:** Spring creates the repositories and passes them into the controller's constructor, so the controller never instantiates them itself.
- **ORM with JPA and Hibernate:** classes become tables, `ddl-auto` keeps the schema in sync, and `JpaRepository` provides standard database operations with no SQL.
- **Derived queries:** Spring Data turns a method name such as `findByNameContaining` into the matching query (`WHERE name LIKE %?%`).
- **Paging and sorting:** Spring fills in `Pageable` and `Sort` straight from URL parameters, which keeps large result sets fast and manageable.
- **Entity relationships:** the `@ManyToOne` side stores the foreign key, `mappedBy` marks the other side, and cascading with orphan removal ties a course's life to its student.
- **JSON serialization:** a relationship that points both ways keeps nesting forever unless one side is marked as the back reference.
- **Bean Validation:** rules are declared once on the model and checked by `@Valid` as requests arrive, so invalid data never reaches the database.
- **Centralized error handling:** `@RestControllerAdvice` keeps error formatting out of the controllers and gives clients a consistent response shape.
- **Automated testing:** MockMvc sends requests through the full stack (security, validation, database) without starting a real server, and transactional tests clean up after themselves.
- **Spring Security:** requests pass through a chain of filters that check who the user is before any controller runs, and access rules can be set per endpoint.
