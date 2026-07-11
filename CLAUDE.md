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

# Run a single service (requires external Postgres)
./gradlew :user-service:bootRun

# Run all tests
./gradlew test

# Run tests for one service
./gradlew :appointment-service:test

# Run a single test class
./gradlew :user-service:test --tests "com.example.user.service.impl.AuthServiceImplTest"

# Run a single test method
./gradlew :user-service:test --tests "com.example.user.service.impl.AuthServiceImplTest.loginPatient_validCredentials_returnsToken"

# Start everything via Docker Compose
docker compose up --build
```

Tests use JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`) and do **not** load a Spring context — keep new tests at the unit level.

Swagger UI is available at `http://localhost:{port}/swagger-ui.html` per service when running individually.

## Architecture

Four Spring Boot 4.0.6 (Java 26) services in a Gradle multi-module build, each owning its own PostgreSQL database.

| Service | Port | Responsibility |
|---|---|---|
| **gateway-service** | 8080 | Spring Cloud Gateway — single public entry point, path-based routing |
| **user-service** | 8081 | Staff/patient CRUD, role/specialty lookups, doctor-patient M:N, JWT issuance |
| **schedule-service** | 8082 | Time slot CRUD and public browse; internal book/release API |
| **appointment-service** | 8083 | Booking, confirm, cancel, reschedule |

> **Note:** `notification-service` (email notifications on appointment events via RabbitMQ) has been removed pending a better implementation — don't re-add it without discussing the design first.

### Cross-service communication

- **Synchronous**: `schedule-service` exposes `/internal/schedules/{id}`, `/internal/schedules/{id}/book`, `/internal/schedules/{id}/release` — consumed by `appointment-service` via `ScheduleClient` (Spring `RestClient`). These endpoints are **not routed through the gateway**.
- Cross-service data is **denormalized at write time** — `Appointment` stores a snapshot of schedule/doctor fields at booking time; there are no cross-service JPA relationships.

### Security model

All three backend services (`user`, `schedule`, `appointment`) validate JWTs locally using the shared `JWT_SECRET` — `user-service` is also the sole issuer (it never calls another service to check a token). Public vs. protected paths are controlled per-service via `security.public-paths` in `application.yaml` (comma-separated list fed into `SecurityConfig`). Method-level security (`@EnableMethodSecurity`) is enabled on services that need fine-grained checks, with `@PreAuthorize("hasAnyRole(...)")` on controller methods plus in-service ownership checks (e.g. a patient can only view their own appointments, a doctor can only manage their own schedule/patients) — when adding endpoints with per-resource ownership, follow this same pattern rather than relying on `@PreAuthorize` alone.

Two separate auth flows, unified behind a single `AuthController`/`AuthService` in `user-service` (mirrors the original monolith's design):
- **Staff (Personal)**: JWT includes the staff `role` claim (`DOCTOR` or `RECEPTIONIST`)
- **Patient**: JWT always includes `role: PATIENT`

Registration checks email uniqueness across **both** `Patient` and `Personal` tables, so the two account types can't collide on email.

Tokens expire after 24 hours.

### Key patterns

- **MapStruct** is used for all entity ↔ DTO mapping. Add new mappings in the `mapper/` package of each service.
- **Lombok** `@Builder`, `@Data`, `@RequiredArgsConstructor` throughout.
- Service impls use `@Transactional(readOnly = true)` at class level; write methods override with `@Transactional`.
- `BusinessException` → HTTP 422, `ResourceNotFoundException` → HTTP 404; handled by `GlobalExceptionHandler` in each service.
- Note: there is no shared `common` module — each service has its own copy of `JwtAuthFilter`, `JwtUtil`, `BusinessException`, `GlobalExceptionHandler`, `SecurityUtils`, etc. Changes to these classes must be applied per-service.
- `user-service` has both a DB-backed `Role` entity (`roles` table, seeded with `DOCTOR`/`RECEPTIONIST`, referenced by `Personal.role` as a `@ManyToOne`, looked up via `roleId` on request DTOs) *and* an `ERole` enum (`DOCTOR`, `RECEPTIONIST`, `PATIENT`). Don't conflate them: `ERole` is used for JWT role claims, `@PreAuthorize` role-name comparisons, and `Personal`/`Patient` code that isn't staff-role-specific; `Role` is the actual persisted staff role, compared by `.getName().equals(ERole.X.name())`. `Patient` has no `Role` row — patients aren't staff.
- Each service's `DataSeeder` (`config/DataSeeder.java`) runs independently and only inserts rows when its own table(s) are empty — wipe a table to force reseeding on next startup. `appointment-service`'s seeder hardcodes `scheduleId`/`doctorId` values that assume `user-service` and `schedule-service` seeded in their usual order; reseeding one service out of sync with the others will desync those references.

## Required Environment Variables

Per-service, when running standalone (not via `docker compose`, which sets these to container hostnames itself):

| Variable | Used by |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | user, schedule, appointment services (own Postgres each) |
| `CORS_ALLOWED_ORIGINS` | user, schedule, appointment services |
| `JWT_SECRET` (min 32 chars) | user, schedule, appointment services |
| `PERSONAL_SERVICE_URL` | schedule-service (calls user-service) |
| `SCHEDULE_SERVICE_URL` | appointment-service (calls schedule-service), gateway-service |
| `USER_SERVICE_URL`, `APPOINTMENT_SERVICE_URL` | gateway-service |

Copy `.env.example` as a starting point (covers JWT_SECRET only — DB/service-URL vars are set inline in `docker-compose.yml`).
