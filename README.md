# Scheduler API

A REST API for healthcare appointment scheduling built with Spring Boot. Providers (doctors, specialists, etc.) register their available time slots, and clients book appointments through those slots. Designed to integrate with n8n workflow automation and WhatsApp-based client flows.

## Features

- Provider registration and management with optional specialty filtering
- Time slot management (individual or batch creation)
- Appointment booking with automatic slot locking
- Appointment lifecycle: `PENDING` → `CONFIRMED` or `CANCELLED`
- HTML email notifications sent to clients on booking
- Soft-delete deactivation for providers
- Auto-seeded sample data on first startup
- OpenAPI/Swagger UI at `/swagger-ui.html`

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 26 |
| Framework | Spring Boot 4.0.6 |
| Persistence | Spring Data JPA + Hibernate + PostgreSQL |
| Mapping | MapStruct 1.6.3 |
| Boilerplate | Lombok |
| Validation | Spring Validation (Jakarta) |
| Email | Spring Mail (SMTP) |
| API Docs | SpringDoc OpenAPI 3.0.2 |
| Build | Gradle (Wrapper included) |

## Prerequisites

- Java 26+
- PostgreSQL running locally (or reachable via `DB_URL`)
- An SMTP server (Gmail with an App Password works out of the box)

## Getting Started

### 1. Clone and configure

Copy the environment variables below and set them before running (or let the defaults apply for local development):

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/scheduler` | JDBC connection string |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | `mysecretpassword` | Database password |
| `MAIL_HOST` | `smtp.gmail.com` | SMTP host |
| `MAIL_PORT` | `587` | SMTP port |
| `MAIL_USERNAME` | `your-email@gmail.com` | SMTP username |
| `MAIL_PASSWORD` | `your-app-password` | SMTP password / App Password |
| `MAIL_FROM` | `your-email@gmail.com` | Sender address |
| `MAIL_FROM_NAME` | `Scheduler` | Sender display name |
| `CORS_ALLOWED_ORIGINS` | `*` | Allowed CORS origins |

### 2. Create the database

```sql
CREATE DATABASE scheduler;
```

Hibernate will create and update the schema automatically on startup (`ddl-auto: update`).

### 3. Run

```bash
./gradlew bootRun
```

The API will be available at `http://localhost:8080`.  
Swagger UI: `http://localhost:8080/swagger-ui.html`

On first startup, three sample providers with schedules and appointments are seeded automatically (only if the database is empty).

## Build Commands

```bash
# Full build (compiles, tests, packages)
./gradlew build

# Run the application
./gradlew bootRun

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.example.scheduler.SomeTest"

# Run a single test method
./gradlew test --tests "com.example.scheduler.SomeTest.methodName"

# Compile without running tests
./gradlew compileJava
```

## API Reference

All endpoints are prefixed with `/api`. CORS is enabled for all origins by default.

---

### Providers `/api/providers`

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/providers` | List all active providers. Filter by `?specialty=` (case-insensitive). |
| `GET` | `/api/providers/{id}` | Get a single provider by ID. |
| `POST` | `/api/providers` | Register a new provider. |
| `PUT` | `/api/providers/{id}` | Update provider information. |
| `DELETE` | `/api/providers/{id}` | Deactivate a provider (soft delete). |

**ProviderRequest**
```json
{
  "name": "Dr. Ana García",
  "specialty": "General Medicine",
  "phone": "+1-555-0101",
  "email": "ana.garcia@clinic.com"
}
```

**ProviderResponse**
```json
{
  "id": 1,
  "name": "Dr. Ana García",
  "specialty": "General Medicine",
  "phone": "+1-555-0101",
  "email": "ana.garcia@clinic.com",
  "active": true
}
```

---

### Schedules `/api/providers/{providerId}/schedules`

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/providers/{providerId}/schedules/available` | List future `AVAILABLE` slots for a provider (used by n8n to present options to clients). |
| `GET` | `/api/providers/{providerId}/schedules` | List all slots (available and booked). |
| `POST` | `/api/providers/{providerId}/schedules` | Add a single time slot. |
| `POST` | `/api/providers/{providerId}/schedules/batch` | Add multiple time slots at once. |
| `DELETE` | `/api/providers/{providerId}/schedules/{scheduleId}` | Remove a slot (fails if already booked). |

**ScheduleRequest**
```json
{
  "startTime": "2026-06-15T09:00:00",
  "endTime": "2026-06-15T10:00:00"
}
```

**Batch request:** array of `ScheduleRequest` objects.

**ScheduleResponse**
```json
{
  "id": 5,
  "providerId": 1,
  "providerName": "Dr. Ana García",
  "providerSpecialty": "General Medicine",
  "startTime": "2026-06-15T09:00:00",
  "endTime": "2026-06-15T10:00:00",
  "status": "AVAILABLE"
}
```

---

### Appointments `/api/appointments`

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/appointments` | Book an appointment on an available slot (called by n8n after slot selection). Triggers email notification. |
| `GET` | `/api/appointments/{id}` | Get appointment details by ID. |
| `GET` | `/api/appointments?phone={phone}` | List all appointments for a client phone number (used by WhatsApp flow). |
| `GET` | `/api/appointments/provider/{providerId}?status=` | List appointments for a provider. Default status: `CONFIRMED`. |
| `PATCH` | `/api/appointments/{id}/confirm` | Confirm a `PENDING` appointment. |
| `PATCH` | `/api/appointments/{id}/cancel` | Cancel an appointment and release the time slot back to `AVAILABLE`. |

**AppointmentRequest**
```json
{
  "scheduleId": 5,
  "clientName": "John Doe",
  "clientPhone": "+1-555-1001",
  "clientEmail": "john.doe@example.com",
  "notes": "Annual check-up"
}
```

**AppointmentResponse**
```json
{
  "id": 1,
  "scheduleId": 5,
  "scheduleStart": "2026-06-15T09:00:00",
  "scheduleEnd": "2026-06-15T10:00:00",
  "providerName": "Dr. Ana García",
  "providerSpecialty": "General Medicine",
  "clientName": "John Doe",
  "clientPhone": "+1-555-1001",
  "clientEmail": "john.doe@example.com",
  "status": "PENDING",
  "notes": "Annual check-up",
  "createdAt": "2026-06-09T14:32:00"
}
```

---

## Appointment Lifecycle

```
PENDING ──► CONFIRMED
   │
   └────────► CANCELLED  (releases slot back to AVAILABLE)
```

- New appointments start as `PENDING`.
- Only `PENDING` appointments can be confirmed.
- Cancelling an appointment returns its time slot to `AVAILABLE`, allowing rebooking.

---

## Error Responses

All errors return a consistent JSON structure:

```json
{
  "timestamp": "2026-06-09T14:00:00",
  "status": 404,
  "message": "Provider not found with id: 99"
}
```

Validation errors (HTTP 400) include a `fieldErrors` map with per-field messages.

| HTTP Status | Cause |
|---|---|
| `400` | Invalid request body / failed bean validation |
| `404` | Resource not found (`ResourceNotFoundException`) |
| `422` | Business rule violation (e.g., booking a booked slot, confirming a non-pending appointment) |

---

## Email Notifications

When an appointment is booked, an HTML confirmation email is automatically sent to the client's email address (if provided). The email includes:

- Patient name
- Provider name and specialty
- Appointment date and time
- Notes (if any)
- A green **CONFIRMED** status badge

Email failures are logged but do not fail the booking request.

---

## Data Model

```
Provider ──< Schedule >── Appointment
```

- A **Provider** has many **Schedules** (time slots).
- Each **Schedule** has at most one **Appointment** (one-to-one).
- Deleting a provider cascades to its schedules.

### Enums

**`ScheduleStatus`**: `AVAILABLE`, `BOOKED`

**`AppointmentStatus`**: `PENDING`, `CONFIRMED`, `CANCELLED`

---

## Project Structure

```
src/main/java/com/example/scheduler/
├── controller/      # REST controllers (@RestController)
├── service/         # Business logic interfaces + implementations (@Service)
├── repository/      # Spring Data JPA repositories (@Repository)
├── entity/          # JPA entities (@Entity)
├── dto/             # Request/response DTOs
├── mapper/          # MapStruct mapper interfaces
├── exception/       # Custom exceptions + GlobalExceptionHandler
└── config/          # CORS, mail, and other configuration
```
