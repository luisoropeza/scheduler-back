# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Compile all services
./gradlew compileJava

# Build all JARs
./gradlew build

# Build a single service
./gradlew :schedule-service:bootJar

# Run a single service (requires external Postgres + RabbitMQ)
./gradlew :user-service:bootRun

# Run all tests
./gradlew test

# Run tests for one service
./gradlew :appointment-service:test

# Start everything via Docker Compose
docker compose up --build
```

Tests use JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`) and do **not** load a Spring context — keep new tests at the unit level.

Swagger UI is available at `http://localhost:{port}/swagger-ui.html` per service when running individually.

## Architecture

Five Spring Boot 4.0.6 (Java 26) services in a Gradle multi-module build, each owning its own PostgreSQL database.

| Service | Port | Responsibility |
|---|---|---|
| **gateway-service** | 8080 | Spring Cloud Gateway — single public entry point, path-based routing |
| **user-service** | 8081 | Staff/patient CRUD, role/specialty lookups, doctor-patient M:N, JWT issuance |
| **schedule-service** | 8082 | Time slot CRUD and public browse; internal book/release API |
| **appointment-service** | 8083 | Booking, confirm, cancel, reschedule; publishes RabbitMQ events |
| **notification-service** | 8084 | Consumes appointment events, sends HTML emails via SMTP |

### Cross-service communication

- **Synchronous**: `schedule-service` exposes `/internal/schedules/{id}`, `/internal/schedules/{id}/book`, `/internal/schedules/{id}/release` — consumed by `appointment-service` via `ScheduleClient` (Spring `RestClient`). These endpoints are **not routed through the gateway**.
- **Async**: `appointment-service` publishes `AppointmentBookedEvent` to RabbitMQ; `notification-service` consumes it. Mail failures never roll back a booking.
- Cross-service data is **denormalized at write time** — `Appointment` stores a snapshot of schedule/doctor fields at booking time; there are no cross-service JPA relationships.

### Security model

Both `schedule-service` and `appointment-service` validate JWTs locally using the shared `JWT_SECRET`. Public vs. protected paths are controlled per-service via `security.public-paths` in `application.yaml` (comma-separated list fed into `SecurityConfig`). Method-level security (`@EnableMethodSecurity`) is enabled on services that need fine-grained checks.

Two separate auth flows in `user-service`:
- **Staff (Personal)**: JWT includes the staff `role` claim (e.g. `DOCTOR`, `NURSE`)
- **Patient**: JWT always includes `role: PATIENT`

Tokens expire after 24 hours.

### Key patterns

- **MapStruct** is used for all entity ↔ DTO mapping. Add new mappings in the `mapper/` package of each service.
- **Lombok** `@Builder`, `@Data`, `@RequiredArgsConstructor` throughout.
- Service impls use `@Transactional(readOnly = true)` at class level; write methods override with `@Transactional`.
- `BusinessException` → HTTP 422, `ResourceNotFoundException` → HTTP 404; handled by `GlobalExceptionHandler` in each service.
- Note: each service currently has its own copy of `JwtAuthFilter`, `JwtUtil`, `BusinessException`, `GlobalExceptionHandler`, etc. The README describes a planned `common` library, but it does not exist yet — changes to these classes must be applied per-service.

## Required Environment Variables

| Variable | Used by |
|---|---|
| `JWT_SECRET` (min 32 chars) | user, schedule, appointment services |
| `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | notification-service |
| `USER_SERVICE_URL`, `SCHEDULE_SERVICE_URL`, `APPOINTMENT_SERVICE_URL` | gateway + client services |
| `RABBITMQ_HOST` | appointment-service, notification-service |

Copy `.env.example` as a starting point.
