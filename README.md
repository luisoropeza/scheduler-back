# Scheduler Platform

A healthcare appointment scheduling platform built as a microservices system. Medical staff register available time slots; patients book, cancel, and reschedule appointments. Designed to integrate with n8n workflow automation.

## Architecture

```
                  ┌─────────────────────────────────────────────────────────────────────┐
                  │                   gateway-service  :8080  (Spring Cloud Gateway)    │
                  │  /api/auth/**                        → user-service                │
                  │  /api/personal/**                    → user-service                │
                  │  /api/patients/**                    → user-service                │
                  │  /api/specialties/**                 → user-service                │
                  │  /api/roles                          → user-service                │
                  │  /api/integrations/n8n/{specialties,  → user-service                │
                  │    doctors,patients/lookup}                                        │
                  │  /api/schedules                      → schedule-service            │
                  │  /api/personal/{id}/schedules/**     → schedule-service            │
                  │  /api/integrations/n8n/schedules     → schedule-service            │
                  │  /api/appointments/**                → appointment-service         │
                  │  /api/integrations/n8n/appointments  → appointment-service         │
                  └──────┬──────────────────────┬────────────────┬──────────────────────┘
                         │                      │                │
                         ▼                      ▼                ▼
                   user-service          schedule-service  appointment-service
                      :8081                  :8082               :8083
                         │                      │                    │
                      user_db             schedule_db         appointment_db
                    (pg :5432)            (pg :5433)           (pg :5434)
```

### Services

| Service | Port | Responsibility |
|---|---|---|
| **user-service** | 8081 | Staff and patient management, doctor-patient M:N relationships, specialty lookup, JWT auth for both staff and patients |
| **schedule-service** | 8082 | Time slot management; browse with filters + doctor-owned slot CRUD; internal book/release API |
| **appointment-service** | 8083 | Booking, confirmation, cancellation, and rescheduling |
| **gateway-service** | 8080 | Spring Cloud Gateway — path-based routing, single public entry point |

Each service owns its own PostgreSQL database. Cross-service data is denormalized at write time (no cross-service JPA relationships).

> **Note:** `appointment-service` also calls `schedule-service` and `user-service` directly via internal APIs (`/internal/schedules/**`, `/internal/patients/**`) that are not routed through the gateway. Each service contains its own copy of `JwtAuthFilter`, `JwtUtil`, `GlobalExceptionHandler`, and shared exception classes — there is no shared `common` library.
>
> A `notification-service` (email notifications on appointment events via RabbitMQ) previously existed and has been removed pending a better implementation.
>
> See [Integration API (n8n)](#integration-api-n8n) below for the automated-booking facade, and [Concurrency](#concurrency) for how double-bookings are prevented.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 26 |
| Framework | Spring Boot 4.0.6 |
| Persistence | Spring Data JPA + PostgreSQL (database-per-service) |
| HTTP clients | Spring RestClient (synchronous inter-service calls) |
| Mapping | MapStruct 1.6.3 |
| Boilerplate | Lombok |
| Validation | Spring Validation (Jakarta) |
| Security | Spring Security + JWT (jjwt), BCrypt passwords |
| API Docs | SpringDoc OpenAPI (Swagger UI per service, Bearer auth wired) |
| Gateway | Spring Cloud Gateway 5.x (`spring-cloud-starter-gateway-server-webflux`) |
| Build | Gradle multi-module (wrapper included) |
| Runtime | Docker + Docker Compose |

## Quick Start (Docker Compose)

```bash
git clone https://github.com/luisoropeza/scheduler-back.git
cd scheduler-back

# Set required secrets
export JWT_SECRET=your-secret-key-min-32-chars

docker compose up --build
```

All services, databases, and the gateway start together. The API is available at `http://localhost:8080`.

On first startup each service seeds sample data: 3 specialties, 3 doctors + 1 receptionist, 3 patients with several schedule slots (some already booked), and two sample appointments.

### Environment Variables

| Variable | Default | Used by |
|---|---|---|
| `JWT_SECRET` | *(required — min 32 chars)* | user, schedule, appointment services |
| `N8N_API_KEY` | `change-me-n8n-key` | user, schedule, appointment services (validates `/api/integrations/n8n/**` callers) |
| `USER_SERVICE_URL` | `http://user-service:8081` | appointment-service, gateway-service |
| `SCHEDULE_SERVICE_URL` | `http://schedule-service:8082` | appointment-service, gateway-service |
| `APPOINTMENT_SERVICE_URL` | `http://appointment-service:8083` | gateway-service |
| `PERSONAL_SERVICE_URL` | `http://user-service:8081` | schedule-service |
| `CORS_ALLOWED_ORIGINS` | `*` | user, schedule, appointment services |

## Development Build

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
```

Each service's Swagger UI is available at `http://localhost:{port}/swagger-ui.html` when running individually.

## Authentication

There are two separate auth flows — both handled by user-service. Both issue JWTs signed with the same `JWT_SECRET`, validated locally in schedule-service and appointment-service.

### Staff (Personal) Auth

**Register** a new staff account:
```http
POST /api/auth/personal/register
Content-Type: application/json

{
  "name": "Dr. Ana García",
  "email": "ana.garcia@clinic.com",
  "password": "secret123",
  "roleId": 1,
  "specialtyId": 1
}
```

**Login:**
```http
POST /api/auth/personal/login
Content-Type: application/json

{
  "email": "ana.garcia@clinic.com",
  "password": "secret123"
}
```

Returns: `{ "token": "<jwt>", "userId": 1 }`

The JWT `role` claim contains the staff member's role name (e.g. `DOCTOR`, `RECEPTIONIST`).

### Patient Auth

**Register:**
```http
POST /api/auth/patient/register
Content-Type: application/json

{
  "name": "John Smith",
  "email": "john.smith@email.com",
  "password": "secret123",
  "phoneNumber": "+1-555-1001"
}
```

**Login:**
```http
POST /api/auth/patient/login
Content-Type: application/json

{
  "email": "john.smith@email.com",
  "password": "secret123"
}
```

Returns: `{ "token": "<jwt>", "userId": 1 }`

The JWT `role` claim is always `PATIENT`.

### Using the Token

```http
Authorization: Bearer <jwt>
```

Tokens expire after **24 hours**.

### Service Integration Auth (n8n)

A third, separate auth path exists for automated callers (currently an n8n WhatsApp workflow) that only know a patient's phone number, not a JWT. Requests to `/api/integrations/n8n/**` are authenticated by an API key instead:

```http
X-API-Key: <N8N_API_KEY>
```

A matching key grants a synthetic `ROLE_INTEGRATION` identity, scoped only to the `/api/integrations/n8n/**` endpoints — see [Integration API (n8n)](#integration-api-n8n) for the endpoint list.

### Public paths per service

Each service permits a fixed, comma-separated list of path patterns (`security.public-paths` in its `application.yaml`); every other request must carry a valid Bearer JWT.

| Service | `security.public-paths` |
|---|---|
| user-service | `/api/auth/**`, `/internal/**` (internal only — not gateway-routed) |
| schedule-service | `/internal/**` (internal only — not gateway-routed) |
| appointment-service | *(none configured — every `/api/appointments/**` route requires a JWT)* |

`/api/appointments/{id}/confirm`, `/cancel`, and `/reschedule` additionally read the caller's id straight from the JWT subject (`Authentication.getName()`), so they need a token issued by user-service regardless of the public-paths setting.

`/api/integrations/n8n/**` on all three services sits outside `security.public-paths` — it's authenticated by a separate `ApiKeyAuthFilter` (see [Integration API (n8n)](#integration-api-n8n)) rather than being open or requiring a JWT.

---

## API Reference

All endpoints are reached through the gateway at `http://localhost:8080`. All list endpoints return a paginated response (`Page<T>`) — pass `?page=0&size=20&sort=fieldName,asc` to control pagination; each endpoint has its own default sort.

---

### Auth  `→ user-service`

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/auth/personal/register` | Register a staff account, returns JWT |
| `POST` | `/api/auth/personal/login` | Login as staff, returns JWT |
| `POST` | `/api/auth/patient/register` | Register a patient account, returns JWT |
| `POST` | `/api/auth/patient/login` | Login as patient, returns JWT |

---

### Personal (Staff)  `→ user-service`

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/personal` | List staff. Filter by `?specialtyId=` and/or `?isActive=`. |
| `GET` | `/api/personal/{id}` | Get a staff member by ID. |
| `PUT` | `/api/personal/{id}` | Update staff information. |
| `DELETE` | `/api/personal/{id}` | Deactivate a staff member (soft delete). |
| `GET` | `/api/specialties` | List all available specialties. |
| `GET` | `/api/roles` | List all available staff roles. |

**PersonalResponse**
```json
{
  "id": 1,
  "name": "Dr. Ana García",
  "email": "ana.garcia@clinic.com",
  "active": true,
  "roleName": "DOCTOR",
  "specialtyName": "General Medicine"
}
```

---

### Patients  `→ user-service`

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/patients` | List all patients (paginated). |
| `GET` | `/api/patients/{id}` | Get a patient by ID. |
| `PUT` | `/api/patients/{id}` | Update patient information. |
| `DELETE` | `/api/patients/{id}` | Deactivate a patient (soft delete). |

**PatientResponse**
```json
{
  "id": 1,
  "name": "John Smith",
  "email": "john.smith@email.com",
  "phoneNumber": "+1-555-1001",
  "active": true
}
```

---

### Doctor-Patient Relationships  `→ user-service`

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/personal/{doctorId}/patients/{patientId}` | Assign a patient to a doctor. |
| `DELETE` | `/api/personal/{doctorId}/patients/{patientId}` | Remove a patient from a doctor. |
| `GET` | `/api/personal/{doctorId}/patients` | List all patients assigned to a doctor. |
| `GET` | `/api/patients/{patientId}/doctors` | List all doctors assigned to a patient. |

---

### Schedules  `→ schedule-service`

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/schedules` | Browse time slots. All filters are optional. |
| `POST` | `/api/personal/{doctorId}/schedules` | Add a single time slot for a doctor. |
| `POST` | `/api/personal/{doctorId}/schedules/batch` | Add multiple time slots at once. |
| `DELETE` | `/api/personal/{doctorId}/schedules/{scheduleId}` | Remove a slot (fails if already booked). |

**GET /api/schedules — query parameters**

| Param | Type | Default | Description |
|---|---|---|---|
| `doctorId` | Long | — | Filter by doctor ID. Validates the doctor is active. |
| `specialty` | String | — | Filter by specialty (case-insensitive). |
| `status` | `AVAILABLE` \| `BOOKED` | — | Slot status to return. |
| `after` | ISO datetime | — | Only return slots starting after this time. |

**ScheduleRequest**
```json
{
  "startTime": "2026-07-01T09:00:00",
  "endTime":   "2026-07-01T10:00:00"
}
```

**ScheduleResponse**
```json
{
  "id": 5,
  "doctorId": 1,
  "doctorName": "Dr. Ana García",
  "doctorSpecialty": "General Medicine",
  "doctorEmail": "ana.garcia@clinic.com",
  "startTime": "2026-07-01T09:00:00",
  "endTime":   "2026-07-01T10:00:00",
  "status": "AVAILABLE"
}
```

---

### Appointments  `→ appointment-service`

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/appointments` | Book an appointment on an available slot. |
| `GET` | `/api/appointments/{id}` | Get appointment details by ID. |
| `GET` | `/api/appointments/client/{clientId}` | List all appointments for a patient (paginated). |
| `GET` | `/api/appointments/personal/{doctorId}` | List appointments for a staff member. Filter by `?status=`. |
| `PATCH` | `/api/appointments/{id}/confirm` | Confirm a `PENDING` appointment. |
| `PATCH` | `/api/appointments/{id}/cancel` | Cancel an appointment, releases the slot back to `AVAILABLE`. |
| `PATCH` | `/api/appointments/{id}/reschedule` | Move an appointment to a different available slot. |

**AppointmentRequest**
```json
{
  "scheduleId": 5,
  "clientId": 1,
  "clientName": "John Smith",
  "clientEmail": "john.smith@email.com"
}
```

**RescheduleRequest**
```json
{
  "scheduleId": 8
}
```

**AppointmentResponse**
```json
{
  "id": 1,
  "scheduleId": 5,
  "scheduleStart": "2026-07-01T09:00:00",
  "scheduleEnd":   "2026-07-01T10:00:00",
  "doctorId": 1,
  "doctorName": "Dr. Ana García",
  "doctorSpecialty": "General Medicine",
  "doctorEmail": "ana.garcia@clinic.com",
  "clientId": 1,
  "clientName": "John Smith",
  "clientEmail": "john.smith@email.com",
  "status": "PENDING",
  "createdAt": "2026-06-18T14:32:00"
}
```

---

### Integration API (n8n)

`→ user-service, schedule-service, appointment-service`

Read-mostly booking facade for automated callers that only know a patient's phone number. Authenticated with `X-API-Key: <N8N_API_KEY>` instead of a JWT (see [Service Integration Auth](#service-integration-auth-n8n)) — **not** a Bearer token.

| Method | Path | Routed to | Description |
|---|---|---|---|
| `GET` | `/api/integrations/n8n/specialties` | user-service | List all available specialties. |
| `GET` | `/api/integrations/n8n/doctors?specialtyId=` | user-service | List active doctors for a specialty (paginated). |
| `GET` | `/api/integrations/n8n/patients/lookup?phoneNumber=` | user-service | Find a registered patient by phone number. |
| `GET` | `/api/integrations/n8n/schedules?doctorId=` | schedule-service | List a doctor's available slots (paginated). |
| `POST` | `/api/integrations/n8n/appointments` | appointment-service | Book a slot for the patient matching `phoneNumber`. |

**IntegrationBookingRequest**
```json
{
  "phoneNumber": "+1-555-1001",
  "scheduleId": 5
}
```

`appointment-service` resolves `phoneNumber` → patient by calling user-service's internal `/internal/patients/lookup` endpoint (not gateway-routed), then books through the normal appointment flow — the response shape is the same `AppointmentResponse` as `POST /api/appointments`.

---

## Appointment Lifecycle

```
PENDING ──► CONFIRMED
   │
   ├────────► CANCELLED      (releases slot back to AVAILABLE)
   │
   └────────► PENDING again  (reschedule: releases old slot, books new slot)
```

- New appointments start as `PENDING`.
- Only `PENDING` appointments can be confirmed.
- Cancelling or rescheduling releases the original slot back to `AVAILABLE`.

---

## Error Responses

All services return the same error structure:

```json
{
  "timestamp": "2026-06-18T14:00:00",
  "status": 404,
  "message": "Schedule not found with id: 99"
}
```

| HTTP Status | Cause |
|---|---|
| `400` | Invalid request body / failed bean validation |
| `401` | Bad credentials or inactive account on login |
| `403` | Caller doesn't own the resource they're acting on (e.g. a patient booking for someone else) |
| `404` | Resource not found |
| `409` | *(schedule-service's own API only — see [Concurrency](#concurrency))* Two requests booked the same slot at once |
| `422` | Business rule violation (e.g., booking an already-booked slot) |
| `500` | Unhandled server error |

---

## Concurrency

`schedule-service`'s `Schedule` entity carries a JPA `@Version` column. If two requests try to book the same slot at the same time, the loser's transaction fails with `ObjectOptimisticLockingFailureException`, which `GlobalExceptionHandler` maps to `409 Conflict`:

```json
{
  "timestamp": "2026-07-14T14:00:00",
  "status": 409,
  "message": "This schedule slot was just booked by someone else, please choose another"
}
```

This 409 is only visible to callers of `schedule-service` directly. `POST /api/appointments` (the public booking flow) goes through `appointment-service`'s `ScheduleClient`, which catches any 4xx from the internal `/internal/schedules/{id}/book` call and re-throws it as its own `422 BusinessException` — so a losing race on the public API currently surfaces as `422`, not `409`.

---

## Project Structure

```
scheduler-platform/
├── build.gradle
├── settings.gradle
├── docker-compose.yml
├── gateway-service/
│   └── src/main/java/com/example/gateway/
│       └── GatewayApplication.java
├── user-service/
│   └── src/
│       ├── main/java/com/example/user/
│       │   ├── controller/   # AuthController (patient + staff register/login), PersonalController
│       │   │                 # PatientController, DoctorPatientController, SpecialtyController, RoleController
│       │   │                 # IntegrationController (/api/integrations/n8n — API-key auth)
│       │   │                 # PatientInternalController (/internal/patients/lookup, not gateway-routed)
│       │   ├── service/      # AuthService, PersonalService, PatientService, RoleService + impls
│       │   ├── repository/   # PersonalRepository, PatientRepository, SpecialtyRepository, RoleRepository
│       │   ├── entity/       # Personal, Patient, Specialty, Role (DB-backed, seeded with DOCTOR/RECEPTIONIST)
│       │   ├── enums/        # ERole (DOCTOR, RECEPTIONIST, PATIENT) — still used for the JWT role claim and comparisons
│       │   ├── dto/          # Request/Response DTOs for all resources
│       │   ├── mapper/       # PersonalMapper, PatientMapper, SpecialtyMapper, RoleMapper (MapStruct)
│       │   ├── security/     # JwtAuthFilter, JwtUtil, SecurityUtils, ApiKeyAuthFilter (n8n)
│       │   ├── exception/    # BusinessException(422), UnauthorizedException(401), ForbiddenException(403),
│       │   │                 # ResourceNotFoundException(404), GlobalExceptionHandler
│       │   └── config/       # SecurityConfig, DataSeeder
│       └── test/java/com/example/user/service/impl/
│           ├── AuthServiceImplTest
│           ├── PersonalServiceImplTest
│           └── PatientServiceImplTest
├── schedule-service/
│   └── src/
│       ├── main/java/com/example/schedule/
│       │   ├── controller/   # ScheduleController (browse + doctor CRUD)
│       │   │                 # ScheduleInternalController (/internal/schedules — book/release, not gateway-routed)
│       │   │                 # IntegrationController (/api/integrations/n8n — API-key auth)
│       │   ├── service/
│       │   ├── repository/   # ScheduleRepository (single JPQL filter query with pagination)
│       │   ├── entity/       # Schedule (carries a @Version column for optimistic locking on booking)
│       │   ├── dto/
│       │   ├── mapper/
│       │   ├── client/       # PersonalClient (RestClient → user-service)
│       │   ├── security/     # JwtAuthFilter, JwtUtil, SecurityUtils, ApiKeyAuthFilter (n8n)
│       │   ├── exception/    # BusinessException(422), UnauthorizedException(401), ForbiddenException(403),
│       │   │                 # ResourceNotFoundException(404), GlobalExceptionHandler (also maps
│       │   │                 # ObjectOptimisticLockingFailureException → 409)
│       │   └── config/       # SecurityConfig, DataSeeder
│       └── test/java/com/example/schedule/service/impl/
│           └── ScheduleServiceImplTest
└── appointment-service/
    └── src/
        ├── main/java/com/example/appointment/
        │   ├── controller/   # AppointmentController
        │   │                 # IntegrationController (/api/integrations/n8n — API-key auth)
        │   ├── service/
        │   │   └── impl/     # AppointmentServiceImpl
        │   ├── repository/   # AppointmentRepository (single JPQL filter query with pagination)
        │   ├── entity/       # Appointment (stores a snapshot of schedule/doctor fields at booking time)
        │   ├── dto/
        │   ├── mapper/
        │   ├── client/       # ScheduleClient (RestClient → schedule-service internal API)
        │   │                 # PatientClient (RestClient → user-service internal API)
        │   ├── security/     # JwtAuthFilter, JwtUtil, SecurityUtils, ApiKeyAuthFilter (n8n)
        │   ├── exception/    # BusinessException(422), UnauthorizedException(401), ForbiddenException(403),
        │   │                 # ResourceNotFoundException(404), GlobalExceptionHandler
        │   └── config/       # SecurityConfig, DataSeeder
        └── test/java/com/example/appointment/service/impl/
            └── AppointmentServiceImplTest
```
