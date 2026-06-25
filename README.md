# Scheduler Platform

A healthcare appointment scheduling platform built as a microservices system. Medical staff register available time slots; patients book, cancel, and reschedule appointments. Designed to integrate with n8n workflow automation.

## Architecture

```
                  ┌─────────────────────────────────────────────────────────────────────┐
                  │                   gateway-service  :8080  (Spring Cloud Gateway)    │
                  │  /api/auth/**                      → user-service                   │
                  │  /api/personal/**                  → user-service                   │
                  │  /api/patients/**                  → user-service                   │
                  │  /api/specialties/**               → user-service                   │
                  │  /api/schedules/available          → schedule-service               │
                  │  /api/personal/{id}/schedules/**   → schedule-service               │
                  │  /api/appointments/**              → appointment-service            │
                  └──────┬──────────────────────┬────────────────┬──────────────────────┘
                         │                      │                │
                         ▼                      ▼                ▼
                   user-service          schedule-service  appointment-service
                      :8081                  :8082               :8083
                         │                      │                    │
                      user_db             schedule_db         appointment_db
                    (pg :5432)            (pg :5433)           (pg :5434)
                                                                     │
                                                                 RabbitMQ :5672
                                                                     │
                                                           notification-service
                                                                  :8084
                                                                (SMTP email)
```

### Services

| Service | Port | Responsibility |
|---|---|---|
| **user-service** | 8081 | Staff and patient management, doctor-patient M:N relationships, specialty lookup, JWT auth for both staff and patients |
| **schedule-service** | 8082 | Time slot management; public browse with filters + doctor-owned slot CRUD; internal book/release API |
| **appointment-service** | 8083 | Booking, confirmation, cancellation, and rescheduling; publishes RabbitMQ events |
| **notification-service** | 8084 | Consumes appointment events, sends HTML emails to the correct party |
| **gateway-service** | 8080 | Spring Cloud Gateway — path-based routing, single public entry point |

Each service owns its own PostgreSQL database. Cross-service data is denormalized at write time (no cross-service JPA relationships). Appointment events are delivered to notification-service via RabbitMQ, so email failures never roll back a booking.

> **Note:** `appointment-service` also calls `schedule-service` directly via an internal API (`/internal/schedules/**`) that is not routed through the gateway. Each service contains its own copy of `JwtAuthFilter`, `JwtUtil`, `GlobalExceptionHandler`, and shared exception classes — there is no shared `common` library yet.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 26 |
| Framework | Spring Boot 4.0.6 |
| Persistence | Spring Data JPA + PostgreSQL (database-per-service) |
| Messaging | Spring AMQP + RabbitMQ |
| HTTP clients | Spring RestClient (synchronous inter-service calls) |
| Mapping | MapStruct 1.6.3 |
| Boilerplate | Lombok |
| Validation | Spring Validation (Jakarta) |
| Security | Spring Security + JWT (jjwt), BCrypt passwords |
| Email | Spring Mail (SMTP) |
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
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=your-app-password
export MAIL_FROM=your-email@gmail.com

docker compose up --build
```

All services, databases, RabbitMQ, and the gateway start together. The API is available at `http://localhost:8080`.

On first startup each service seeds sample data: 2 staff roles (DOCTOR, RECEPTIONIST), 3 specialties, 3 doctors, 1 receptionist, and 3 patients with several schedule slots.

### Environment Variables

| Variable | Default | Used by |
|---|---|---|
| `JWT_SECRET` | *(required — min 32 chars)* | user, schedule, appointment services |
| `USER_SERVICE_URL` | `http://user-service:8081` | schedule-service, gateway-service |
| `SCHEDULE_SERVICE_URL` | `http://schedule-service:8082` | appointment-service, gateway-service |
| `APPOINTMENT_SERVICE_URL` | `http://appointment-service:8083` | gateway-service |
| `RABBITMQ_HOST` | `rabbitmq` | appointment-service, notification-service |
| `MAIL_HOST` | `smtp.gmail.com` | notification-service |
| `MAIL_PORT` | `587` | notification-service |
| `MAIL_USERNAME` | *(required)* | notification-service |
| `MAIL_PASSWORD` | *(required)* | notification-service |
| `MAIL_FROM` | *(required)* | notification-service |
| `MAIL_FROM_NAME` | `Scheduler` | notification-service |
| `CORS_ALLOWED_ORIGINS` | `*` | user, schedule, appointment services |

## Development Build

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
```

Each service's Swagger UI is available at `http://localhost:{port}/swagger-ui.html` when running individually.

## Authentication

There are two separate auth flows — both handled by user-service. Both issue JWTs signed with the same `JWT_SECRET`, validated in schedule-service and appointment-service.

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

### Access Rules

| Endpoint | Auth required |
|---|---|
| `GET /api/personal`, `GET /api/personal/{id}` | Public |
| `GET /api/specialties` | Public |
| `GET /api/patients`, `GET /api/patients/{id}` | Public |
| `GET /api/personal/{id}/patients` | Public |
| `GET /api/patients/{id}/doctors` | Public |
| `GET /api/schedules` | Public |
| `GET /api/appointments/{id}` | Public |
| `GET /api/appointments/client/{clientId}` | Public |
| `POST /api/auth/**` | Public |
| `POST /api/appointments` | Public (n8n flow) |
| `PUT /api/personal/{id}`, `DELETE /api/personal/{id}` | JWT |
| `PUT /api/patients/{id}`, `DELETE /api/patients/{id}` | JWT |
| `POST /api/personal/{id}/schedules` | JWT |
| `POST /api/personal/{id}/schedules/batch` | JWT |
| `DELETE /api/personal/{id}/schedules/{scheduleId}` | JWT |
| `POST /api/personal/{doctorId}/patients/{patientId}` | JWT |
| `DELETE /api/personal/{doctorId}/patients/{patientId}` | JWT |
| `GET /api/appointments/personal/{personalId}` | JWT |
| `PATCH /api/appointments/{id}/confirm` | JWT |
| `PATCH /api/appointments/{id}/cancel` | JWT |
| `PATCH /api/appointments/{id}/reschedule` | JWT |

---

## API Reference

All public endpoints are reached through the gateway at `http://localhost:8080`.

All list endpoints return a paginated response (`Page<T>`). Pass `?page=0&size=20&sort=fieldName,asc` to control pagination; each endpoint has its own default sort.

---

### Auth  `→ user-service`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/personal/register` | Public | Register a staff account, returns JWT |
| `POST` | `/api/auth/personal/login` | Public | Login as staff, returns JWT |
| `POST` | `/api/auth/patient/register` | Public | Register a patient account, returns JWT |
| `POST` | `/api/auth/patient/login` | Public | Login as patient, returns JWT |

---

### Personal (Staff)  `→ user-service`

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/personal` | Public | List staff. Filter by `?specialtyId=` and/or `?isActive=`. |
| `GET` | `/api/personal/{id}` | Public | Get a staff member by ID. |
| `PUT` | `/api/personal/{id}` | JWT | Update staff information. |
| `DELETE` | `/api/personal/{id}` | JWT | Deactivate a staff member (soft delete). |
| `GET` | `/api/specialties` | Public | List all available specialties. |

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

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/patients` | Public | List all patients (paginated). |
| `GET` | `/api/patients/{id}` | Public | Get a patient by ID. |
| `PUT` | `/api/patients/{id}` | JWT | Update patient information. |
| `DELETE` | `/api/patients/{id}` | JWT | Deactivate a patient (soft delete). |

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

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/personal/{doctorId}/patients/{patientId}` | JWT | Assign a patient to a doctor. |
| `DELETE` | `/api/personal/{doctorId}/patients/{patientId}` | JWT | Remove a patient from a doctor. |
| `GET` | `/api/personal/{doctorId}/patients` | Public | List all patients assigned to a doctor. |
| `GET` | `/api/patients/{patientId}/doctors` | Public | List all doctors assigned to a patient. |

---

### Schedules  `→ schedule-service`

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/schedules` | Public | Browse time slots. All filters are optional. |
| `POST` | `/api/personal/{doctorId}/schedules` | JWT | Add a single time slot for a doctor. |
| `POST` | `/api/personal/{doctorId}/schedules/batch` | JWT | Add multiple time slots at once. |
| `DELETE` | `/api/personal/{doctorId}/schedules/{scheduleId}` | JWT | Remove a slot (fails if already booked). |

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

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/appointments` | Public | Book an appointment on an available slot. |
| `GET` | `/api/appointments/{id}` | Public | Get appointment details by ID. |
| `GET` | `/api/appointments/client/{clientId}` | Public | List all appointments for a patient (paginated). |
| `GET` | `/api/appointments/personal/{personalId}` | JWT | List appointments for a staff member. Filter by `?status=`. |
| `PATCH` | `/api/appointments/{id}/confirm` | JWT | Confirm a `PENDING` appointment. |
| `PATCH` | `/api/appointments/{id}/cancel` | JWT | Cancel an appointment, releases the slot back to `AVAILABLE`. |
| `PATCH` | `/api/appointments/{id}/reschedule` | JWT | Move an appointment to a different available slot. |

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
  "personalId": 1,
  "scheduleStart": "2026-07-01T09:00:00",
  "scheduleEnd":   "2026-07-01T10:00:00",
  "personalName": "Dr. Ana García",
  "personalSpecialty": "General Medicine",
  "personalEmail": "ana.garcia@clinic.com",
  "clientId": 1,
  "clientName": "John Smith",
  "clientEmail": "john.smith@email.com",
  "status": "PENDING",
  "createdAt": "2026-06-18T14:32:00"
}
```

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

## Email Notifications

appointment-service publishes an `AppointmentBookedEvent` to RabbitMQ for each action; notification-service consumes it and sends an HTML email. The routing logic in notification-service determines the recipient:

| Event | Actor | Email sent to |
|---|---|---|
| `BOOKED` | — | Patient |
| `CANCELLED` | Doctor | Patient |
| `CANCELLED` | Patient | Doctor |
| `RESCHEDULED` | Doctor | Patient |
| `RESCHEDULED` | Patient | Doctor |

Email delivery is fully decoupled from the booking transaction — a mail failure has no effect on the appointment record.

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
| `404` | Resource not found |
| `422` | Business rule violation (e.g., booking an already-booked slot) |
| `500` | Unhandled server error |

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
│       │   ├── controller/   # PersonalController, PatientController, DoctorPatientController
│       │   │                 # PersonalAuthController, PatientAuthController, SpecialtyController
│       │   ├── service/      # PersonalService, PatientService + impls
│       │   │                 # PersonalAuthService, PatientAuthService, SpecialtyService
│       │   ├── repository/   # PersonalRepository, PatientRepository, SpecialtyRepository
│       │   ├── entity/       # Personal, Patient, Specialty
│       │   ├── enums/        # ERole (DOCTOR, RECEPTIONIST, PATIENT)
│       │   ├── dto/          # Request/Response DTOs for all resources
│       │   ├── mapper/       # PersonalMapper, PatientMapper, SpecialtyMapper (MapStruct)
│       │   └── config/       # SecurityConfig, DataSeeder
│       └── test/java/com/example/user/service/
│           ├── PersonalAuthServiceTest
│           ├── PatientAuthServiceTest
│           ├── impl/PersonalServiceImplTest
│           └── impl/PatientServiceImplTest
├── schedule-service/
│   └── src/
│       ├── main/java/com/example/schedule/
│       │   ├── controller/   # ScheduleController (public browse + doctor CRUD)
│       │   │                 # ScheduleInternalController (/internal/schedules — book/release, not gateway-routed)
│       │   ├── service/
│       │   ├── repository/   # ScheduleRepository (single JPQL filter query with pagination)
│       │   ├── entity/       # Schedule
│       │   ├── dto/
│       │   ├── mapper/
│       │   ├── client/       # PersonalClient (RestClient → user-service)
│       │   └── config/       # SecurityConfig, DataSeeder
│       └── test/java/com/example/schedule/service/impl/
│           └── ScheduleServiceImplTest
├── appointment-service/
│   └── src/
│       ├── main/java/com/example/appointment/
│       │   ├── controller/   # AppointmentController
│       │   ├── service/
│       │   │   └── impl/     # AppointmentServiceImpl
│       │   ├── repository/   # AppointmentRepository (single JPQL filter query with pagination)
│       │   ├── entity/       # Appointment (stores a snapshot of schedule/doctor fields at booking time)
│       │   ├── dto/
│       │   ├── mapper/
│       │   ├── client/       # ScheduleClient (RestClient → schedule-service internal API)
│       │   └── config/       # SecurityConfig, RabbitConfig, DataSeeder
│       └── test/java/com/example/appointment/service/impl/
│           └── AppointmentServiceImplTest
└── notification-service/
    └── src/main/java/com/example/notification/
        ├── listener/     # AppointmentEventListener (@RabbitListener)
        ├── service/      # NotificationService + impl (builds and sends HTML email)
        └── config/       # RabbitConfig
```
