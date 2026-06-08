# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew build

# Run
./gradlew bootRun

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.example.scheduler.SomeTest"

# Run a single test method
./gradlew test --tests "com.example.scheduler.SomeTest.methodName"

# Compile without running tests
./gradlew compileJava
```

## Tech Stack

- **Java 26**, Spring Boot 4.0.6
- **Spring MVC** (REST API), **Spring Data JPA** with PostgreSQL
- **Spring Mail** for email sending
- **Lombok** for boilerplate reduction (`@Data`, `@Builder`, etc.)
- **MapStruct 1.6.3** for DTO ↔ entity mapping (annotation-processor-based; mappers go in a `mapper` package)
- **SpringDoc OpenAPI** — Swagger UI available at `/swagger-ui.html` when running
- **Spring Validation** for request validation

## Architecture

Standard Spring Boot layered architecture under `com.example.scheduler`:

- `controller/` — REST controllers (`@RestController`)
- `service/` — business logic (`@Service`)
- `repository/` — Spring Data JPA repositories (`@Repository`)
- `entity/` — JPA entities (`@Entity`)
- `dto/` — request/response DTOs
- `mapper/` — MapStruct mapper interfaces

## Configuration

`application.yaml` currently only sets the app name. Before running, add:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/scheduler
    username: <user>
    password: <pass>
  jpa:
    hibernate:
      ddl-auto: update
  mail:
    host: <smtp-host>
    port: 587
    username: <user>
    password: <pass>
```

A running PostgreSQL instance is required for the context-load test (`SchedulerApplicationTests`) to pass.
