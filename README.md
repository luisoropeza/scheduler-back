# Scheduler

A multi-tenant REST API for a clinic appointment scheduler built with Spring Boot. Each clinic is an isolated tenant: doctors publish available time slots, patients book them, and staff can confirm, cancel, or reschedule appointments.

## Tech stack

- Java 26, Spring Boot 4
- Spring Data JPA + PostgreSQL, with Hibernate discriminator-based multi-tenancy (`@TenantId`)
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
| `DB_PASSWORD` | `mysecretpassword` | Database password |
| `CORS_ALLOWED_ORIGINS` | `*` | Allowed CORS origins |
| `JWT_SECRET` | `change-me-in-production-minimum-32-chars!` | JWT signing secret (HMAC, must be ≥32 chars) |
| `N8N_API_KEY` | `change-me-n8n-key` | Static API key for the `/api/integrations/n8n/**` facade (sent as `X-API-Key`) |
| `MAIL_HOST` | `smtp.gmail.com` | SMTP host |
| `MAIL_PORT` | `587` | SMTP port |
| `MAIL_USERNAME` | *(empty)* | SMTP username |
| `MAIL_PASSWORD` | *(empty)* | SMTP password |
| `MAIL_FROM` | `no-reply@scheduler.local` | From address for outgoing mail |
| `MAIL_FROM_NAME` | `Scheduler` | From display name for outgoing mail |

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

On first startup (only when the `clinic` table is empty), `DataSeeder` seeds two clinics, each with its own admin account, 3 specialties, 3 doctors, 1 receptionist, patients, and schedule slots. Every seeded user's password is `password123`.

## Multi-tenancy

Every clinic (`Clinic`) is a tenant. Tenant-scoped entities (`Specialty`, `Personal`, `Patient`, `DoctorAvailability`, `Appointment`) carry a `clinicId` column and are automatically filtered by it on every query via Hibernate's discriminator-based multi-tenancy. `Role` and `Clinic` itself are shared across all tenants.

**Every request to a non-public endpoint must include an `X-Tenant-ID` header** set to the numeric clinic id — requests without it are rejected with `400 Bad Request` before authentication is even checked. If the request also carries a JWT, the header's clinic id must match the `clinicId` claim embedded in the token at login/registration time, or the request is rejected with `403 Forbidden`.

Create a clinic first (no auth or tenant header required) — this also creates its admin account. Log in as that admin (sending `X-Tenant-ID: <clinicId>`) and use their JWT to register staff and patients against the clinic; each new account's `clinicId` is stamped from the `X-Tenant-ID` header on the registration call.

## API

Interactive API docs (Swagger UI) are available at `http://localhost:8080/swagger-ui/index.html` once the app is running, backed by the OpenAPI spec at `/v3/api-docs`. Remember to set `X-Tenant-ID` when trying endpoints out.

Authentication is JWT-based: log in via `/api/auth/**` to get a token, then send it as `Authorization: Bearer <token>` on subsequent requests. There's no self-service sign-up — accounts are created by a clinic's admin (creating a clinic) or by existing staff (registering patients/staff). Endpoints are grouped below by resource; roles are `ADMINISTRATOR`, `DOCTOR`, `SUPERVISOR`, `RECEPTIONIST`, `PATIENT`.

### Clinics (`/api/clinics`)

| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/api/clinics` | public | List all clinics |
| POST | `/api/clinics` | public | Create a new clinic (tenant) and its admin account (`adminName`/`adminEmail`/`adminPassword`) |
| GET | `/api/clinics/mine` | any authenticated user | List the clinics the caller belongs to |

### Auth (`/api/auth`)

| Method | Path | Access | Description |
|---|---|---|---|
| POST | `/api/auth/patient/register` | ADMINISTRATOR, DOCTOR, RECEPTIONIST | Register a new patient |
| POST | `/api/auth/patient/login` | public | Log in as a patient, returns a JWT |
| POST | `/api/auth/personal/register` | ADMINISTRATOR | Register a new staff member |
| POST | `/api/auth/personal/login` | public | Log in as staff, returns a JWT |

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
| GET | `/api/appointments` | any authenticated user | Paged list, filter by `?doctorId=&clientId=&status=` (all optional; DOCTOR/PATIENT callers are scoped to themselves, RECEPTIONIST can filter freely or omit both for a clinic-wide list) |
| GET | `/api/appointments/board` | any authenticated user | Appointments in `?from=&to=` grouped by status (also filterable by `doctorId`/`clientId`, same scoping rules) |
| GET | `/api/appointments/calendar` | any authenticated user | Appointments in `?month=&year=` grouped by day (also filterable by `doctorId`/`clientId`, same scoping rules) |
| PATCH | `/api/appointments/{id}/confirm` | DOCTOR, RECEPTIONIST | Confirm a pending appointment |
| PATCH | `/api/appointments/{id}/cancel` | DOCTOR, RECEPTIONIST | Cancel an appointment (frees the slot) |
| PATCH | `/api/appointments/{id}/reschedule` | DOCTOR, RECEPTIONIST | Move an appointment to a new slot |

### Patients (`/api/patients`)

| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/api/patients` | DOCTOR, RECEPTIONIST | List all patients |
| GET | `/api/patients/{id}` | DOCTOR, RECEPTIONIST | Get a patient by ID |
| PUT | `/api/patients/{id}` | any authenticated user | Update patient info |
| DELETE | `/api/patients/{id}` | any authenticated user | Deactivate a patient account |
| GET | `/api/patients/{patientId}/doctors` | DOCTOR, RECEPTIONIST | List doctors assigned to a patient |

### Personal / staff (`/api/personal`)

| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/api/personal` | any authenticated user | List staff; filter by `specialtyId`, `isActive` |
| GET | `/api/personal/{id}` | DOCTOR, RECEPTIONIST | Get a staff member by ID |
| PUT | `/api/personal/{id}` | DOCTOR, RECEPTIONIST | Update a staff member |
| DELETE | `/api/personal/{id}` | DOCTOR, RECEPTIONIST | Deactivate a staff member |
| POST | `/api/personal/{doctorId}/patients/{patientId}` | DOCTOR, RECEPTIONIST | Assign a patient to a doctor |
| DELETE | `/api/personal/{doctorId}/patients/{patientId}` | DOCTOR, RECEPTIONIST | Unassign a patient from a doctor |
| GET | `/api/personal/{doctorId}/patients` | DOCTOR, RECEPTIONIST | List a doctor's patients |

### Reference data

| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/api/roles` | DOCTOR, RECEPTIONIST | List available staff roles |
| GET | `/api/specialties` | any authenticated user | List available specialties |
| POST | `/api/specialties` | ADMINISTRATOR | Create a specialty |

### Integrations (`/api/integrations/n8n`)

Read-only browsing + booking facade for automated callers (currently the n8n WhatsApp workflow). Authenticated via a static API key (`X-API-Key` header, see `N8N_API_KEY`) instead of a per-patient JWT, since the caller only knows the patient's phone number. `X-Tenant-ID` is still required, same as any other endpoint.

| Method | Path | Description |
|---|---|---|
| GET | `/api/integrations/n8n/clinics` | List the clinics a patient (by `phoneNumber`) belongs to |
| GET | `/api/integrations/n8n/specialties` | List all specialties |
| GET | `/api/integrations/n8n/doctors` | List active doctors; filter by `specialtyId` |
| GET | `/api/integrations/n8n/schedules` | List available slots; filter by `doctorId` |
| GET | `/api/integrations/n8n/patients/lookup` | Find a registered patient by `phoneNumber` |
| POST | `/api/integrations/n8n/appointments` | Book a slot for the patient identified by phone number |

## Domain model

- **Clinic** — a tenant; owns its own specialties, staff, patients, schedules, and appointments.
- **Account** — shared login record (name, email, password, role) referenced by both `Personal` and `Patient`; `Patient` accounts also carry a `phoneNumber`.
- **Schedule** — a doctor's bookable time slot; status is `AVAILABLE` or `BOOKED`.
- **Appointment** — links a patient to a booked schedule; status is `PENDING`, `CONFIRMED`, or `CANCELLED`, tracked independently of the schedule's own status. Booking, confirming, cancelling, and rescheduling keep both statuses in sync (cancelling or rescheduling frees the old slot back to `AVAILABLE`).
- **Personal** — staff member (`ADMINISTRATOR`, `DOCTOR`, `SUPERVISOR`, or `RECEPTIONIST`) with, for doctors, a specialty and an assigned list of patients.
- **Patient** — a clinic patient, optionally assigned to one or more doctors.

## Testing

```bash
# Windows
gradlew.bat test

# macOS/Linux
./gradlew test
```
