# Scheduler

A multi-tenant REST API for clinical appointment scheduling built with Spring Boot 4 and Java 26. The platform supports 
multiple clinics isolated through **PostgreSQL schema-based multi-tenancy**, dynamic weekly doctor availability management 
with exception handling, patient booking workflows, role-based access control with JWT, and an AI-powered conversational 
assistant with tool calling powered by Spring AI and Google Gemini.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [System Architecture & Multi-Tenancy](#system-architecture--multi-tenancy)
  - [Schema-per-Tenant Model](#schema-per-tenant-model)
  - [Dynamic Tenant Provisioning](#dynamic-tenant-provisioning)
  - [Context Resolution & Security](#context-resolution--security)
- [Domain Model & Roles](#domain-model--roles)
  - [User Roles](#user-roles)
  - [Entity Model](#entity-model)
  - [Custom Validations](#custom-validations)
  - [Weekly Recurring Availability](#weekly-recurring-availability)
  - [Schedule Exceptions & Blocks](#schedule-exceptions--blocks)
  - [Dynamic Available Slot Calculation](#dynamic-available-slot-calculation)
- [AI Virtual Assistant & Integrations](#ai-virtual-assistant--integrations)
  - [Spring AI Google Gemini Chat](#spring-ai-google-gemini-chat)
  - [Twilio WhatsApp Notifications](#twilio-whatsapp-notifications)
- [Prerequisites](#prerequisites)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
  - [Local Execution](#local-execution)
  - [Docker Execution](#docker-execution)
  - [Initial Data Seeding](#initial-data-seeding)
- [API Reference](#api-reference)
  - [Clinics (`/api/clinics`)](#clinics-apiclinics)
  - [Authentication (`/api/auth`)](#authentication-apiauth)
  - [Staff / Personal (`/api/personal`)](#staff--personal-apipersonal)
  - [Patients (`/api/patients`)](#patients-apipatients)
  - [Doctor Availability (`/api/doctorAvailability`)](#doctor-availability-apidoctoravailability)
  - [Schedule Exceptions (`/api/scheduleException`)](#schedule-exceptions-apischeduleexception)
  - [Appointments (`/api/appointments`)](#appointments-apiappointments)
  - [Specialties (`/api/specialties`)](#specialties-apispecialties)
  - [Roles (`/api/roles`)](#roles-apiroles)
  - [Accounts (`/api/account`)](#accounts-apiaccount)
  - [AI Chat Assistant (`/api/chat`)](#ai-chat-assistant-apichat)
- [Testing](#testing)

---

## Tech Stack

- **Language & Runtime:** Java 26 (Eclipse Temurin toolchain)
- **Framework:** Spring Boot 4.0.7
- **Persistence & ORM:** Spring Data JPA, Hibernate with Schema-based Multi-tenancy (`multiTenancy: SCHEMA`)
- **Database:** PostgreSQL (with automated schema provisioning)
- **Security:** Spring Security with stateless JWT (`jjwt 0.12.6`), Bcrypt password hashing, and method-level security (`@PreAuthorize`)
- **Artificial Intelligence:** Spring AI 2.0.1 (`spring-ai-starter-model-google-genai` with `gemini-3.5-flash-lite`)
- **Messaging:** Twilio Java SDK 13.0.1 (WhatsApp messaging service)
- **Email:** Spring Boot Starter Mail (SMTP)
- **Mapping & Boilerplate:** MapStruct 1.6.3 and Project Lombok
- **API Documentation:** SpringDoc OpenAPI 3.0.2 (Swagger UI)
- **Build Tool:** Gradle

---

## System Architecture & Multi-Tenancy

### Schema-per-Tenant Model

The application isolates tenant data using a **PostgreSQL schema-per-tenant** pattern:

- **`public` schema (Shared):** Contains system-wide data shared across all tenants:
  - `clinics` — registered clinics/tenants.
  - `accounts` — user identities (`ci`, `name`, `email`, `password`, `phone_number`).
  - `roles` — predefined system roles (`ADMINISTRATOR`, `DOCTOR`, `ASSISTANT`, `PATIENT`).
- **`clinic_{id}` schemas (Tenant Isolated):** Dedicated schema automatically provisioned per clinic:
  - `specialties` — medical specialties offered by the clinic.
  - `personal` — clinic staff members linked to shared accounts, roles, and specialties.
  - `patients` — clinic patients linked to shared accounts and roles.
  - `doctor_availabilities` — recurring weekly working schedules for doctors.
  - `schedule_exceptions` — date-specific blocks or time-range exceptions for doctors.
  - `appointments` — patient appointment bookings with optimistic locking (`version`).
  - `doctor_patient` — many-to-many relationship tracking doctor-patient assignments.

### Dynamic Tenant Provisioning

1. At startup, `SchemaProvisioningService` initializes the `public` schema (`sql/public-schema.sql`), ensuring `accounts`, `roles`, and `clinics` tables exist, and seeds standard roles.
2. When a new clinic is registered (`POST /api/clinics`), `SchemaProvisioningService` dynamically generates the schema `clinic_{id}` and executes `sql/tenant-schema.sql`, initializing all tenant-specific tables and setting up default reference data.

### Context Resolution & Security

- **No manual `X-Tenant-ID` header is required.**
- During login (`POST /api/auth/login`), the client sends `{ "clinicId": 1, "email": "...", "password": "..." }`. The system switches `TenantContext` to `clinic_1`, validates the user credentials within that tenant, and issues a JWT token embedding `id`, `role`, `clinicId`, and `name`.
- On all authenticated requests, `JwtAuthFilter` extracts the `clinicId` claim from the validated JWT token and sets `TenantContext.setCurrentTenant("clinic_" + clinicId)`.
- Hibernate's `SchemaBasedMultiTenantConnectionProvider` automatically sets the PostgreSQL connection search path to `clinic_{clinicId}, public`, seamlessly isolating every database query.

---

## Domain Model & Roles

### User Roles

The system defines 4 strict roles (`ERole`):

| Role            | Role ID | Scope & Permissions                                                                                                                                                                                       |
|-----------------|---------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ADMINISTRATOR` | 1       | Clinic administrator. Can register/manage staff members (`personal`), deactivate staff, and create medical specialties.                                                                                   |
| `DOCTOR`        | 2       | Medical doctor. Manages recurring availability (`DoctorAvailability`), sets schedule blocks/exceptions (`ScheduleException`), views own appointments and patient list, and confirms/cancels appointments. |
| `ASSISTANT`     | 3       | Clinic receptionist / assistant. Registers patients, updates patient info, assigns/removes patients to doctors, views doctor schedules, and confirms/cancels appointments.                                |
| `PATIENT`       | 4       | Registered patient. Checks doctor available time slots, books appointments, updates self profile, and interacts with the AI scheduling assistant.                                                         |

### Entity Model

- **`Clinic`** — A tenant entity containing clinic name and contact phone number.
- **`Account`** — Global credential record storing `ci` (national ID), `name`, `email`, `password` (Bcrypt encoded), and optional `phoneNumber`.
- **`Role`** — Global role definition (`ADMINISTRATOR`, `DOCTOR`, `ASSISTANT`, `PATIENT`).
- **`Personal`** — Tenant entity linking an `Account` to a clinic staff member, with assigned `Role`, optional `Specialty`, and assigned patients.
- **`Patient`** — Tenant entity linking an `Account` to a clinic patient record.
- **`DoctorAvailability`** — Recurring weekly availability window for a doctor (`dayOfWeek`, `startTime`, `endTime`, `slotDurationMinutes`, `active`).
- **`ScheduleException`** — Schedule exception or block on a doctor's schedule (`date`, `startTime`, `endTime`, `isFullDayBlock`, `reason`).
- **`Appointment`** — Scheduled appointment linking a doctor and a patient for a specific time interval (`startTime`, `endTime`), with `status` (`PENDING`, `CONFIRMED`, `CANCELLED`) and optimistic locking versioning (`version`).
- **`Specialty`** — Clinic medical specialty (e.g., General Medicine, Dentistry, Pediatrics).

### Custom Validations

The application uses custom Jakarta Bean Validation annotations:

- **`@ValidRole`**: Ensures that staff registrations specify a valid staff role ID (`DOCTOR` or `ASSISTANT`).
- **`@SpecialtyRoleMatch`**: Cross-field validator ensuring that:
  - If the role is `DOCTOR`, a valid `specialtyId` must be provided.
  - If the role is `ASSISTANT`, `specialtyId` must be null.

### Weekly Recurring Availability

Doctors define recurring weekly time slots (`/api/doctorAvailability`):
- `dayOfWeek`: Day of the week (`MONDAY` through `SUNDAY`).
- `startTime`: Shift start time (e.g., `08:00:00`).
- `endTime`: Shift end time (e.g., `12:00:00`).
- `slotDurationMinutes`: Consultation interval in minutes (e.g., `30`).

### Schedule Exceptions & Blocks

Doctors can register exceptions (`/api/scheduleException`) for holidays, medical leaves, or specific unavailable intervals:
- `isFullDayBlock`: When `true`, completely marks the specified date as unavailable.
- `startTime` & `endTime`: Defines a partial time range block for that date.
- `reason`: Description of the unavailability (e.g., "Medical Conference", "Surgery").

### Dynamic Available Slot Calculation

When querying `GET /api/doctorAvailability/{doctorId}/availables?date=YYYY-MM-DD`:
1. Checks if the date has a full-day block exception (`isFullDayBlock = true`). If so, returns an empty list.
2. Loads doctor's active weekly availability windows for that `DayOfWeek`.
3. Slices the availability window into discrete slot start times based on `slotDurationMinutes`.
4. Discards slots that fall within any partial schedule exceptions.
5. Queries `appointments` for that doctor and date where `status != CANCELLED`, and removes slots already occupied.
6. Returns the remaining available slots.

When booking (`POST /api/appointments`), the backend validates that the requested slot fits within the doctor's working hours, is not blocked by exceptions, and is not already taken, rejecting invalid requests with a `400 Bad Request`.

---

## AI Virtual Assistant & Integrations

### Spring AI Google Gemini Chat

The `/api/chat/patient` endpoint provides an interactive AI assistant for patients powered by **Spring AI** and Google's **Gemini 3.5 Flash Lite** model:

- **Conversational Memory:** Preserves multi-turn conversation context keyed by `userId:sessionId`.
- **Function / Tool Calling (`FlowScheduleTool`):** The LLM autonomously accesses backend tools to assist the patient:
  - `getPatientUser`: Fetches the current authenticated patient's profile.
  - `findAllSpecialties`: Retrieves available specialties in the clinic.
  - `findAllDoctors`: Retrieves active doctors for a selected specialty.

### Twilio WhatsApp Notifications

Integrated with the **Twilio SDK** (`TwilioWhatsappService`):
- Supports automated WhatsApp notification delivery (e.g., appointment confirmations, reminders) directly to patients' phone numbers.

---

## Prerequisites

- **JDK 26** (Gradle toolchain will provision it automatically if needed)
- **PostgreSQL 14+** running and accessible
- **Google Gemini API Key** (for Spring AI chat features)

---

## Configuration

Configuration is managed in `src/main/resources/application.yaml` and loaded via environment variables or a `.env` file:

| Variable               | Default / Example                            | Description                                        |
|------------------------|----------------------------------------------|----------------------------------------------------|
| `DB_URL`               | `jdbc:postgresql://localhost:5432/scheduler` | PostgreSQL JDBC connection URL                     |
| `DB_USERNAME`          | `postgres`                                   | PostgreSQL username                                |
| `DB_PASSWORD`          | `mysecretpassword`                           | PostgreSQL password                                |
| `PORT`                 | `8080`                                       | HTTP server port                                   |
| `CORS_ALLOWED_ORIGINS` | `*`                                          | Allowed CORS origins (comma-separated or wildcard) |
| `JWT_SECRET`           | *(minimum 32 characters)*                    | HMAC signing key for JWT tokens                    |
| `GEMINI_API_KEY`       | *(your Gemini API key)*                      | Google GenAI API key for Spring AI chat assistant  |
| `MAIL_HOST`            | `smtp.gmail.com`                             | SMTP server host                                   |
| `MAIL_PORT`            | `587`                                        | SMTP server port                                   |
| `MAIL_USERNAME`        | *(empty)*                                    | SMTP username                                      |
| `MAIL_PASSWORD`        | *(empty)*                                    | SMTP password / app password                       |
| `MAIL_FROM`            | `no-reply@scheduler.local`                   | Sender email address                               |
| `MAIL_FROM_NAME`       | `Scheduler`                                  | Sender display name                                |

Twilio properties are configured under the `twilio` prefix in `application.yaml`:
```yaml
twilio:
  account-sid: ${TWILIO_ACCOUNT_SID:TU_ACCOUNT_SID}
  auth-token: ${TWILIO_AUTH_TOKEN:TU_AUTH_TOKEN}
  whatsapp-number: ${TWILIO_WHATSAPP_NUMBER:whatsapp:+14155238886}
```

---

## Running the Application

### Local Execution

Ensure PostgreSQL is running and a database named `scheduler` exists:

```bash
# Windows (PowerShell / Command Prompt)
.\gradlew.bat bootRun

# Linux / macOS
./gradlew bootRun
```

### Docker Execution

Build and run using the provided multi-stage `Dockerfile`:

```bash
# Build the Docker image
docker build -t scheduler .

# Run with environment variables from .env
docker run --env-file .env -p 8080:8080 scheduler
```

### Initial Data Seeding

On first startup (when `public.clinics` is empty), `DataSeeder` automatically creates two sample clinics with complete test data:

1. **Downtown Clinic** (Phone: `+1-555-2001`)
   - Schema: `clinic_1`
   - Admin: `admin.downtown@clinic.com` (CI: `123123121`)
   - Doctors: Dr. Ana García (`ana.garcia@clinic.com`, General Medicine), Dr. Carlos Méndez (`carlos.mendez@clinic.com`, Dentistry), Dr. Laura Torres (`laura.torres@clinic.com`, Pediatrics)
   - Assistant: Maria Ramos (`maria.ramos@clinic.com`)
   - Patients: John Smith (`john.smith@email.com`), María López (`maria.lopez@email.com`)
2. **Uptown Clinic** (Phone: `+1-555-2002`)
   - Schema: `clinic_2`
   - Admin: `admin.uptown@clinic.com` (CI: `123123128`)
   - Doctors: Dr. Sofía Ramírez (`sofia.ramirez@clinic.com`, Cardiology), Dr. Diego Fernández (`diego.fernandez@clinic.com`, Dermatology), Dr. Valentina Cruz (`valentina.cruz@clinic.com`, Traumatology)
   - Assistant: Pedro Álvarez (`pedro.alvarez@clinic.com`)
   - Patient: James Wilson (`james.wilson@email.com`)

> [!NOTE]
> Every seeded user account has the password: `password123`.

---

## API Reference

Interactive Swagger documentation is available once the server starts at:
```
http://localhost:8080/swagger-ui/index.html
```
OpenAPI JSON spec is available at:
```
http://localhost:8080/v3/api-docs
```

### Clinics (`/api/clinics`)

| Method | Endpoint       | Access | Description                                                                                        |
|--------|----------------|--------|----------------------------------------------------------------------------------------------------|
| `GET`  | `/api/clinics` | Public | List all registered clinics                                                                        |
| `POST` | `/api/clinics` | Public | Register a new clinic and its administrator account; automatically provisions `clinic_{id}` schema |

**Payload for `POST /api/clinics`:**
```json
{
  "name": "St. Jude Clinic",
  "phoneNumber": "+1-555-0199",
  "adminName": "Alice Johnson",
  "adminEmail": "admin.stjude@clinic.com",
  "adminPassword": "password123",
  "adminCi": "987654321"
}
```

---

### Authentication (`/api/auth`)

| Method | Endpoint          | Access | Description                                                       |
|--------|-------------------|--------|-------------------------------------------------------------------|
| `POST` | `/api/auth/login` | Public | Unified user login (staff or patient). Returns a JWT Bearer token |

**Payload for `POST /api/auth/login`:**
```json
{
  "clinicId": 1,
  "email": "ana.garcia@clinic.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

---

### Staff / Personal (`/api/personal`)

| Method   | Endpoint                            | Access                       | Description                                                                             |
|----------|-------------------------------------|------------------------------|-----------------------------------------------------------------------------------------|
| `GET`    | `/api/personal/doctors`             | `PATIENT`, `ASSISTANT`       | List doctors, optional filters: `?specialtyId=&isActive=` (paginated)                   |
| `GET`    | `/api/personal`                     | `ADMINISTRATOR`              | List all staff members, optional filters: `?specialtyId=&isActive=&roleId=` (paginated) |
| `POST`   | `/api/personal`                     | `ADMINISTRATOR`              | Register a new staff member (`DOCTOR` or `ASSISTANT`)                                   |
| `GET`    | `/api/personal/{personalId}`        | `ADMINISTRATOR`, `ASSISTANT` | Get staff details by ID                                                                 |
| `PUT`    | `/api/personal/update/{personalId}` | `ADMINISTRATOR`              | Update staff member info by ID                                                          |
| `PUT`    | `/api/personal/update`              | `DOCTOR`, `ASSISTANT`        | Update authenticated staff member's own profile                                         |
| `DELETE` | `/api/personal/{personalId}`        | `ADMINISTRATOR`              | Deactivate staff member                                                                 |
| `POST`   | `/api/personal/patients/assign`     | `DOCTOR`, `ASSISTANT`        | Assign a patient to a doctor (`{ "patientId": 1, "doctorId": 2 }`)                      |
| `DELETE` | `/api/personal/patients/remove`     | `DOCTOR`, `ASSISTANT`        | Remove a patient from a doctor (`{ "patientId": 1, "doctorId": 2 }`)                    |
| `GET`    | `/api/personal/{doctorId}/patients` | `DOCTOR`, `ASSISTANT`        | List all patients assigned to a doctor                                                  |

**Payload for `POST /api/personal`:**
```json
{
  "name": "Dr. Gregory House",
  "email": "house@clinic.com",
  "ci": "112233445",
  "password": "password123",
  "roleId": 2,
  "specialtyId": 1
}
```

---

### Patients (`/api/patients`)

| Method   | Endpoint                            | Access                 | Description                                    |
|----------|-------------------------------------|------------------------|------------------------------------------------|
| `GET`    | `/api/patients`                     | `DOCTOR`, `ASSISTANT`  | List all patients (paginated)                  |
| `POST`   | `/api/patients`                     | `DOCTOR`, `ASSISTANT`  | Register a new patient account                 |
| `GET`    | `/api/patients/{patientId}`         | `DOCTOR`, `ASSISTANT`  | Get patient details by ID                      |
| `PUT`    | `/api/patients/update/{patientId}`  | `DOCTOR`, `ASSISTANT`  | Update patient details by ID                   |
| `PUT`    | `/api/patients/update`              | `PATIENT`              | Update authenticated patient's own profile     |
| `DELETE` | `/api/patients/{patientId}`         | `DOCTOR`, `ASSISTANT`  | Deactivate a patient account                   |
| `GET`    | `/api/patients/{patientId}/doctors` | `PATIENT`, `ASSISTANT` | List doctors assigned to the specified patient |

**Payload for `POST /api/patients`:**
```json
{
  "name": "Carlos Mendoza",
  "email": "carlos.mendoza@email.com",
  "ci": "554433221",
  "password": "password123",
  "phoneNumber": "+1-555-4001"
}
```

---

### Doctor Availability (`/api/doctorAvailability`)

| Method | Endpoint                                        | Access                           | Description                                                                        |
|--------|-------------------------------------------------|----------------------------------|------------------------------------------------------------------------------------|
| `POST` | `/api/doctorAvailability`                       | `DOCTOR`                         | Configure recurring weekly working availability for authenticated doctor           |
| `GET`  | `/api/doctorAvailability/{doctorId}`            | `DOCTOR`, `ASSISTANT`, `PATIENT` | Get all recurring working schedules for a doctor                                   |
| `GET`  | `/api/doctorAvailability/{doctorId}/availables` | `DOCTOR`, `ASSISTANT`, `PATIENT` | Calculate and return available time slots for a specific date (`?date=YYYY-MM-DD`) |

**Payload for `POST /api/doctorAvailability`:**
```json
{
  "dayOfWeek": "MONDAY",
  "startTime": "08:00:00",
  "endTime": "14:00:00",
  "slotDurationMinutes": 30
}
```

**Response for `GET /api/doctorAvailability/2/availables?date=2026-10-05`:**
```json
{
  "date": "2026-10-05",
  "doctorId": 2,
  "availableSlots": [
    "08:00:00",
    "08:30:00",
    "09:00:00",
    "10:00:00",
    "10:30:00",
    "11:00:00"
  ]
}
```

---

### Schedule Exceptions (`/api/scheduleException`)

| Method | Endpoint                 | Access   | Description                                                            |
|--------|--------------------------|----------|------------------------------------------------------------------------|
| `POST` | `/api/scheduleException` | `DOCTOR` | Add a full-day block or time-window exception for authenticated doctor |

**Payload for `POST /api/scheduleException` (Full-day block):**
```json
{
  "date": "2026-10-12",
  "isFullDayBlock": true,
  "reason": "National Holiday"
}
```

**Payload for `POST /api/scheduleException` (Partial-day block):**
```json
{
  "date": "2026-10-05",
  "startTime": "09:30:00",
  "endTime": "10:00:00",
  "isFullDayBlock": false,
  "reason": "Department Staff Meeting"
}
```

---

### Appointments (`/api/appointments`)

| Method  | Endpoint                                    | Access                | Description                                                                                 |
|---------|---------------------------------------------|-----------------------|---------------------------------------------------------------------------------------------|
| `POST`  | `/api/appointments`                         | Any Authenticated     | Book an appointment for a patient                                                           |
| `GET`   | `/api/appointments/{appointmentId}`         | Any Authenticated     | Get appointment details (scoped to own role)                                                |
| `GET`   | `/api/appointments`                         | Any Authenticated     | List appointments with filters: `?doctorId=&patientId=&status=` (paginated, scoped by role) |
| `PATCH` | `/api/appointments/{appointmentId}/confirm` | `DOCTOR`, `ASSISTANT` | Confirm a pending appointment                                                               |
| `PATCH` | `/api/appointments/{appointmentId}/cancel`  | `DOCTOR`, `ASSISTANT` | Cancel an appointment (frees the slot)                                                      |
| `GET`   | `/api/appointments/board`                   | Any Authenticated     | Group appointments by status in date range (`?from=YYYY-MM-DD&to=YYYY-MM-DD`)               |
| `GET`   | `/api/appointments/calendar`                | Any Authenticated     | Group appointments by day in specified month (`?month=10&year=2026`)                        |

**Payload for `POST /api/appointments`:**
```json
{
  "doctorId": 2,
  "patientId": 1,
  "startTime": "2026-10-05T08:30:00",
  "endTime": "2026-10-05T09:00:00"
}
```

---

### Specialties (`/api/specialties`)

| Method | Endpoint           | Access            | Description                                        |
|--------|--------------------|-------------------|----------------------------------------------------|
| `GET`  | `/api/specialties` | Any Authenticated | List all medical specialties in the current clinic |
| `POST` | `/api/specialties` | `ADMINISTRATOR`   | Create a new specialty in the current clinic       |

**Payload for `POST /api/specialties`:**
```json
{
  "name": "Neurology"
}
```

---

### Roles (`/api/roles`)

| Method | Endpoint     | Access                                 | Description                                  |
|--------|--------------|----------------------------------------|----------------------------------------------|
| `GET`  | `/api/roles` | `ADMINISTRATOR`, `DOCTOR`, `ASSISTANT` | List all available staff roles in the system |

---

### Accounts (`/api/account`)

| Method | Endpoint            | Access            | Description                                             |
|--------|---------------------|-------------------|---------------------------------------------------------|
| `GET`  | `/api/account/{ci}` | Any Authenticated | Retrieve account identity details by national ID (`ci`) |

---

### AI Chat Assistant (`/api/chat`)

| Method | Endpoint            | Access    | Description                                                                    |
|--------|---------------------|-----------|--------------------------------------------------------------------------------|
| `POST` | `/api/chat/patient` | `PATIENT` | Conversational scheduling chat with Spring AI agent and automated tool calling |

**Request Body (`text/plain` or string):**
```text
"Hi! I need to see a general doctor next Monday. What doctors and times are available?"
```

---

## Testing

Execute the test suite using Gradle:

```bash
# Windows
.\gradlew.bat test

# Linux / macOS
./gradlew test
```
