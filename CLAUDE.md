# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

A Spring Boot 4 REST API for a clinic appointment scheduler: patients book appointments into time slots (`Schedule`) that doctors (`Personal`) publish, and staff confirm/cancel/reschedule them. Java 26 (toolchain), Gradle, PostgreSQL, JWT auth.

## Commands

Windows shell in this repo — use `gradlew.bat`, not `./gradlew`.

- Build: `gradlew.bat build`
- Run the app: `gradlew.bat bootRun`
- Run all tests: `gradlew.bat test`
- Run a single test class: `gradlew.bat test --tests "com.example.scheduler.SchedulerApplicationTests"`
- Run a single test method: `gradlew.bat test --tests "com.example.scheduler.SchedulerApplicationTests.contextLoads"`

The app needs a running PostgreSQL instance (`DB_URL`/`DB_USERNAME`/`DB_PASSWORD` env vars, defaults point at `localhost:5432/scheduler`). `DataSeeder` (`config/DataSeeder.java`) populates specialties, roles, doctors, patients, schedules, and appointments on startup **only if the specialties table is empty** — wipe that table to reseed. Seeded login password for every seeded user is `password123`.

Swagger UI / OpenAPI docs are served at `/swagger-ui/**` and `/v3/api-docs/**` (see `config/OpenApiConfig.java`), and those paths are permitted without auth.

Docker: `Dockerfile` is a two-stage build (Gradle build → JRE runtime image); `.env.example` lists all env vars the container/app expects (copy to `.env` for local `docker run --env-file .env`).

## Architecture

Standard layered structure, package-by-layer (not by feature): `controller` → `service` (interface) + `service.impl` (implementation) → `repository` (Spring Data JPA) → `entity`. `dto` holds request/response records/classes; `mapper` (MapStruct) converts between entities and DTOs — controllers and services never expose JPA entities directly.

**Auth & security**: Stateless JWT, no sessions. `JwtUtil` signs/parses tokens (`sub` = numeric user id, `role` claim). `JwtAuthFilter` runs before `UsernamePasswordAuthenticationFilter`, reads `Authorization: Bearer <token>`, and populates `SecurityContextHolder` with a single `ROLE_<role>` authority — there is no DB lookup per request. `SecurityConfig` permits `security.public-paths` (currently `/api/auth/**`) plus Swagger paths; everything else requires authentication. Method-level authorization uses `@PreAuthorize("hasRole(...)")`/`hasAnyRole(...)` on controller methods. Roles are `DOCTOR`, `RECEPTIONIST` (note: misspelled, not "RECEPTIONIST"), `PATIENT` (`enums/ERole.java`).

Patients and staff (`Personal`) are separate entity types/tables with separate auth endpoints (`/api/auth/patient/*` vs `/api/auth/personal/*`), each issuing the same kind of JWT. Controllers that need the caller's identity/role pull them out of the injected `Authentication` (id = `auth.getName()`, role = first authority stripped of `ROLE_` prefix via `SecurityUtils.extractRole`), then pass them into the service layer, which does its own authorization checks (e.g. a patient can only see/book their own appointments, a doctor can only see their own schedule or assign their own patients) alongside `@PreAuthorize`. When adding endpoints with per-resource ownership checks, follow this same pattern rather than relying on `@PreAuthorize` alone — and double-check the equality condition's sense (it guards against the *mismatched* id, so the throw belongs behind `!id.equals(userId)`, not `id.equals(userId)`).

A third auth path exists alongside the two JWT flows: `ApiKeyAuthFilter` (`security/ApiKeyAuthFilter.java`) matches any `/api/integrations/**` request, checks the `X-API-Key` header against `n8n.api-key` (env `N8N_API_KEY`), and on match grants a synthetic `ROLE_INTEGRATION` authentication — no JWT involved. Both `JwtAuthFilter` and `ApiKeyAuthFilter` are registered before `UsernamePasswordAuthenticationFilter` in `SecurityConfig`; requests under `/api/integrations/**` fall through the JWT filter (no bearer token) and get authenticated by the API key filter instead. `IntegrationController` (`@PreAuthorize("hasRole('INTEGRATION')")`) is a read-mostly facade for automated callers (currently an n8n WhatsApp workflow) that only know a patient's phone number: it looks patients up by phone (`PatientService.findByPhoneNumber`) rather than by JWT identity, then delegates to the normal service layer.

**Domain flow**: `Schedule` = a doctor's bookable time slot (`AVAILABLE`/`BOOKED`), created individually or in batch by a doctor. `Appointment` links a `Patient` to a booked `Schedule` and carries its own lifecycle (`PENDING`/`CONFIRMED`/`CANCELLED`) independent of the schedule's status. Booking, confirming, cancelling, and rescheduling all flip both the `Appointment.status` and the linked `Schedule.status` together (cancel/reschedule free up the old slot) — keep these in sync when touching `AppointmentServiceImpl`. `Schedule` carries a `@Version` column for optimistic locking, so two concurrent bookings of the same slot race and the loser throws `ObjectOptimisticLockingFailureException`, which `GlobalExceptionHandler` maps to 409 rather than 500.

**Error handling**: `GlobalExceptionHandler` (`@RestControllerAdvice`) is the single place mapping exceptions to responses — `ResourceNotFoundException` → 404, `UnauthorizedException` → 401, `ForbiddenException` → 403, `BusinessException` → 422, `ObjectOptimisticLockingFailureException` → 409, `MethodArgumentNotValidException` → 400 with field errors, anything else → 500. Services pick the exception by what actually went wrong rather than returning nulls/booleans or handling errors ad hoc in controllers: not-found → `ResourceNotFoundException`, bad credentials/inactive account → `UnauthorizedException`, caller doesn't own the resource they're acting on → `ForbiddenException`, everything else that's a valid-but-disallowed state transition → `BusinessException`.

## Notes

- There is currently only one test (`SchedulerApplicationTests.contextLoads`) — no service/controller test suite exists yet.
- CORS allowed origins and the JWT signing secret are both externalized via env vars (`CORS_ALLOWED_ORIGINS`, `JWT_SECRET`) with insecure defaults meant only for local dev — don't rely on the defaults for anything beyond local testing.
