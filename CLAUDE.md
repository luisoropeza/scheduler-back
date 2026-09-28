# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

A Spring Boot 4 REST API for a multi-tenant clinic appointment scheduler: patients book appointments into time slots (`DoctorAvailability`) that doctors (`Personal`) publish, and staff confirm/cancel/reschedule them. Each clinic (`Clinic`) is a tenant — its specialties, staff, patients, schedules, and appointments are isolated from other clinics. Java 26 (toolchain), Gradle, PostgreSQL, JWT auth.

## Commands

Windows shell in this repo — use `gradlew.bat`, not `./gradlew`.

- Build: `gradlew.bat build`
- Run the app: `gradlew.bat bootRun`
- Run all tests: `gradlew.bat test`
- Run a single test class: `gradlew.bat test --tests "com.example.scheduler.SchedulerApplicationTests"`
- Run a single test method: `gradlew.bat test --tests "com.example.scheduler.SchedulerApplicationTests.contextLoads"`

The app needs a running PostgreSQL instance (`DB_URL`/`DB_USERNAME`/`DB_PASSWORD` env vars, defaults point at `localhost:5432/scheduler`). `DataSeeder` (`config/DataSeeder.java`) populates two clinics (each with its own specialties, doctors, receptionist, patients, and schedules) on startup **only if the specialties table is empty** — wipe that table to reseed. Seeded login password for every seeded user is `password123`.

Swagger UI / OpenAPI docs are served at `/swagger-ui/**` and `/v3/api-docs/**` (see `config/OpenApiConfig.java`), and those paths are permitted without auth.

Docker: `Dockerfile` is a two-stage build (Gradle build → JRE runtime image); `.env.example` lists all env vars the container/app expects (copy to `.env` for local `docker run --env-file .env`).

## Architecture

Standard layered structure, package-by-layer (not by feature): `controller` → `service` (interface) + `service.impl` (implementation) → `repository` (Spring Data JPA) → `entity`. `dto` holds request/response records/classes; `mapper` (MapStruct) converts between entities and DTOs — controllers and services never expose JPA entities directly.

**Multi-tenancy**: `Clinic` (`entity/Clinic.java`) is the tenant root — created via `POST /api/clinics` (public, no auth). `Specialty`, `Personal`, `Patient`, `DoctorAvailability`, and `Appointment` each carry a `@TenantId`-annotated `clinicId` column (Hibernate's discriminator-based multi-tenancy: one schema, every tenant-scoped query is filtered by `clinic_id` automatically). The resolver chain: `TenantFilter` (`middleware/TenantFilter.java`) reads the required `X-Tenant-ID` header on every request, rejects the request with 400 if it's missing, cross-checks it against the `clinicId` claim in the caller's JWT (403 on mismatch) if a bearer token is present, then stores it in `TenantContext` (a `ThreadLocal`) for the duration of the request. `HeaderTenantIdentifierResolver` (`config/tenant/HeaderTenantIdentifierResolver.java`) is what Hibernate actually calls per-query to read `TenantContext` (falling back to `"public"` if unset — a state that should only occur for non-tenant-scoped entities like `Clinic`/`Role`). When seeding or otherwise writing data outside a request (e.g. `DataSeeder`), you must manually bracket the work in `TenantContext.setCurrentTenant(...)` / `TenantContext.clear()` (in a `try`/`finally`) — see `DataSeeder.run` for the pattern. `Role` and `Clinic` are the only entities *without* a `clinicId` — they're global/shared across tenants.

**Auth & security**: Stateless JWT, no sessions. `JwtUtil` signs/parses tokens (`sub` = numeric user id, `role` claim, `clinicId` claim). `JwtAuthFilter` runs before `UsernamePasswordAuthenticationFilter`, reads `Authorization: Bearer <token>`, and populates `SecurityContextHolder` with a single `ROLE_<role>` authority — there is no DB lookup per request. `TenantFilter` runs immediately after it (`addFilterAfter(tenantFilter, jwtAuthFilter.getClass())`) so it can validate the token's `clinicId` against the request header. `SecurityConfig` permits `security.public-paths` (currently `/api/auth/**` and `/api/clinics/**`) plus Swagger paths; everything else requires authentication. Method-level authorization uses `@PreAuthorize("hasRole(...)")`/`hasAnyRole(...)` on controller methods. Roles are `DOCTOR`, `RECEPTIONIST` (note: misspelled, not "RECEPTIONIST"), `PATIENT` (`enums/ERole.java`).

Patients and staff (`Personal`) are separate entity types/tables with separate auth endpoints (`/api/auth/patient/*` vs `/api/auth/personal/*`), each issuing the same kind of JWT (now including the registrant's `clinicId`, resolved from `TenantContext` at save time via `@TenantId`). Controllers that need the caller's identity/role pull them out of the injected `Authentication` (id = `auth.getName()`, role = first authority stripped of `ROLE_` prefix via `SecurityUtils.extractRole`), then pass them into the service layer, which does its own authorization checks (e.g. a patient can only see/book their own appointments, a doctor can only see their own schedule or assign their own patients) alongside `@PreAuthorize`. When adding endpoints with per-resource ownership checks, follow this same pattern rather than relying on `@PreAuthorize` alone — and double-check the equality condition's sense (it guards against the *mismatched* id, so the throw belongs behind `!id.equals(userId)`, not `id.equals(userId)`).

A third auth path exists alongside the two JWT flows: `ApiKeyAuthFilter` (`middleware/ApiKeyAuthFilter.java`) matches any `/api/integrations/**` request, checks the `X-API-Key` header against `n8n.api-key` (env `N8N_API_KEY`), and on match grants a synthetic `ROLE_INTEGRATION` authentication — no JWT involved. Requests to `/api/integrations/**` still need the `X-Tenant-ID` header (`TenantFilter` applies regardless of which auth filter authenticated the request), they just skip the JWT-vs-header `clinicId` cross-check since there's no bearer token. `IntegrationController` (`@PreAuthorize("hasRole('INTEGRATION')")`) is a read-mostly facade for automated callers (currently an n8n WhatsApp workflow) that only know a patient's phone number: it looks patients up by phone (`PatientService.findByPhoneNumber`) rather than by JWT identity, then delegates to the normal service layer.

**Domain flow**: `DoctorAvailability` = a doctor's bookable time slot (`AVAILABLE`/`BOOKED`), created individually or in batch by a doctor. `Appointment` links a `Patient` to a booked `DoctorAvailability` and carries its own lifecycle (`PENDING`/`CONFIRMED`/`CANCELLED`) independent of the schedule's status. Booking, confirming, cancelling, and rescheduling all flip both the `Appointment.status` and the linked `Schedule.status` together (cancel/reschedule free up the old slot) — keep these in sync when touching `AppointmentServiceImpl`. `DoctorAvailability` carries a `@Version` column for optimistic locking, so two concurrent bookings of the same slot race and the loser throws `ObjectOptimisticLockingFailureException`, which `GlobalExceptionHandler` maps to 409 rather than 500.

**Error handling**: `GlobalExceptionHandler` (`@RestControllerAdvice`) is the single place mapping exceptions to responses — `ResourceNotFoundException` → 404, `UnauthorizedException` → 401, `ForbiddenException` → 403, `BusinessException` → 422, `ObjectOptimisticLockingFailureException` → 409, `MethodArgumentNotValidException` → 400 with field errors, anything else → 500. Services pick the exception by what actually went wrong rather than returning nulls/booleans or handling errors ad hoc in controllers: not-found → `ResourceNotFoundException`, bad credentials/inactive account → `UnauthorizedException`, caller doesn't own the resource they're acting on → `ForbiddenException`, everything else that's a valid-but-disallowed state transition → `BusinessException`.

## Notes

- There is currently only one test (`SchedulerApplicationTests.contextLoads`) — no service/controller test suite exists yet.
- CORS allowed origins and the JWT signing secret are both externalized via env vars (`CORS_ALLOWED_ORIGINS`, `JWT_SECRET`) with insecure defaults meant only for local dev — don't rely on the defaults for anything beyond local testing.
- Every request to a non-public path must carry `X-Tenant-ID`, including Swagger "try it out" calls and integration calls authenticated via `X-API-Key` — a missing header fails closed with 400 before auth is even checked.
