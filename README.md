# Scheduler

A REST API for a clinic appointment scheduler built with Spring Boot. Doctors publish available time slots, patients book them, and staff can confirm, cancel, or reschedule appointments.

## Tech stack

- Java 26, Spring Boot 4
- Spring Data JPA + PostgreSQL
- Spring Security with stateless JWT authentication
- MapStruct (entity ↔ DTO mapping) + Lombok
- springdoc-openapi (Swagger UI)
- Gradle

## Prerequisites

- JDK 26 (a Gradle toolchain will provision it automatically if not present)
- A running PostgreSQL instance

## Configuration

All configuration lives in `src/main/resources/application.yaml` and is overridable via environment variables:

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/scheduler` | Database URL |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | `postgres` | Database password |
| `CORS_ALLOWED_ORIGINS` | `*` | Allowed CORS origins |
| `JWT_SECRET` | `change-me-in-production-minimum-32-chars!` | JWT signing secret (HMAC, must be ≥32 chars) |
| `N8N_API_KEY` | `change-me-n8n-key` | Static API key for the `/api/integrations/n8n/**` facade (sent as `X-API-Key`) |

The defaults are for local development only — set real values for anything beyond that.

## Running

```bash
# Windows
gradlew.bat bootRun

# macOS/Linux
./gradlew bootRun
```

Or with Docker:

```bash
docker build -t scheduler .
docker run --env-file .env -p 8080:8080 scheduler
```

The app starts on `http://localhost:8080`. `spring.jpa.hibernate.ddl-auto` is set to `update`, so the schema is created/updated automatically against the configured database — no separate migration step is needed.

On first startup (only when the `specialty` table is empty), `DataSeeder` seeds sample data: 3 specialties, 2 roles, 3 doctors, 1 receptionist, 3 patients, several schedule slots, and 2 appointments. Every seeded user's password is `password123`.

## API

Interactive API docs (Swagger UI) are available at `http://localhost:8080/swagger-ui/index.html` once the app is running, backed by the OpenAPI spec at `/v3/api-docs`.

Authentication is JWT-based: register or log in via `/api/auth/**` to get a token, then send it as `Authorization: Bearer <token>` on subsequent requests. Endpoints are grouped below by resource; roles are `DOCTOR`, `RECEPTIONIST`, `PATIENT`.

### Auth (`/api/auth`) — public

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/patient/register` | Register a new patient, returns a JWT |
| POST | `/api/auth/patient/login` | Log in as a patient |
| POST | `/api/auth/personal/register` | Register a new staff member, returns a JWT |
| POST | `/api/auth/personal/login` | Log in as staff |

### Schedules (`/api/schedules`, `/api/personal/{doctorId}/schedules`)

| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/api/schedules` | any authenticated user | Browse slots; filter by `doctorId`, `specialtyId`, `status`, `after` |
| GET | `/api/schedules/{id}` | any authenticated user | Get a slot by ID |
| POST | `/api/personal/{doctorId}/schedules` | DOCTOR | Create one available slot |
| POST | `/api/personal/{doctorId}/schedules/batch` | DOCTOR | Create multiple slots at once |
| DELETE | `/api/personal/{doctorId}/schedules/{scheduleId}` | DOCTOR | Remove an available slot |

### Appointments (`/api/appointments`)

| Method | Path | Access | Description |
|---|---|---|---|
| POST | `/api/appointments` | any authenticated user | Book a slot for a patient |
| GET | `/api/appointments/{id}` | any authenticated user | Get appointment details |
| GET | `/api/appointments/client/{clientId}` | any authenticated user | List a patient's appointments |
| GET | `/api/appointments/personal/{doctorId}` | DOCTOR, RECEPCIONIST | List a doctor's appointments; filter by `status` |
| PATCH | `/api/appointments/{id}/confirm` | DOCTOR, RECEPCIONIST | Confirm a pending appointment |
| PATCH | `/api/appointments/{id}/cancel` | DOCTOR, RECEPCIONIST | Cancel an appointment (frees the slot) |
| PATCH | `/api/appointments/{id}/reschedule` | DOCTOR, RECEPCIONIST | Move an appointment to a new slot |

### Patients (`/api/patients`)

| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/api/patients` | DOCTOR, RECEPCIONIST | List all patients |
| GET | `/api/patients/{id}` | DOCTOR, RECEPCIONIST | Get a patient by ID |
| PUT | `/api/patients/{id}` | any authenticated user | Update patient info |
| DELETE | `/api/patients/{id}` | any authenticated user | Deactivate a patient account |
| GET | `/api/patients/{patientId}/doctors` | DOCTOR, RECEPCIONIST | List doctors assigned to a patient |

### Personal / staff (`/api/personal`)

| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/api/personal` | any authenticated user | List staff; filter by `specialtyId`, `isActive` |
| GET | `/api/personal/{id}` | DOCTOR, RECEPCIONIST | Get a staff member by ID |
| PUT | `/api/personal/{id}` | DOCTOR, RECEPCIONIST | Update a staff member |
| DELETE | `/api/personal/{id}` | DOCTOR, RECEPCIONIST | Deactivate a staff member |
| POST | `/api/personal/{doctorId}/patients/{patientId}` | DOCTOR, RECEPCIONIST | Assign a patient to a doctor |
| DELETE | `/api/personal/{doctorId}/patients/{patientId}` | DOCTOR, RECEPCIONIST | Unassign a patient from a doctor |
| GET | `/api/personal/{doctorId}/patients` | DOCTOR, RECEPCIONIST | List a doctor's patients |

### Reference data

| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/api/roles` | any authenticated user | List available staff roles |
| GET | `/api/specialties` | DOCTOR, RECEPCIONIST | List available specialties |

### Integrations (`/api/integrations/n8n`)

Read-only browsing + booking facade for automated callers (currently the n8n WhatsApp workflow). Authenticated via a static API key (`X-API-Key` header, see `N8N_API_KEY`) instead of a per-patient JWT, since the caller only knows the patient's phone number.

| Method | Path | Description |
|---|---|---|
| GET | `/api/integrations/n8n/specialties` | List all specialties |
| GET | `/api/integrations/n8n/doctors` | List active doctors; filter by `specialtyId` |
| GET | `/api/integrations/n8n/schedules` | List available slots; filter by `doctorId` |
| GET | `/api/integrations/n8n/patients/lookup` | Find a registered patient by `phoneNumber` |
| POST | `/api/integrations/n8n/appointments` | Book a slot for the patient identified by phone number |

## Domain model

- **Schedule** — a doctor's bookable time slot; status is `AVAILABLE` or `BOOKED`.
- **Appointment** — links a patient to a booked schedule; status is `PENDING`, `CONFIRMED`, or `CANCELLED`, tracked independently of the schedule's own status. Booking, confirming, cancelling, and rescheduling keep both statuses in sync (cancelling or rescheduling frees the old slot back to `AVAILABLE`).
- **Personal** — staff member (doctor or receptionist) with a role and, for doctors, a specialty and an assigned list of patients.
- **Patient** — a clinic patient, optionally assigned to one or more doctors.

## Testing

```bash
# Windows
gradlew.bat test

# macOS/Linux
./gradlew test
```
