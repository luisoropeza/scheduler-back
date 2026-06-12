# Scheduler Platform

A healthcare appointment scheduling platform built as a microservices system. Providers register available time slots; clients book appointments through those slots. Designed to integrate with n8n workflow automation and WhatsApp-based client flows.

## Architecture

```
                  ┌──────────────────────────────────────────────────────────────────┐
                  │               gateway-service  :8080  (Spring Cloud Gateway)     │
                  │  /api/providers/{id}/schedules/**  → schedule-service            │
                  │  /api/providers/**                 → provider-service            │
                  │  /api/appointments/**              → appointment-service         │
                  └──────────┬──────────────────┬───────────────────┬────────────────┘
                             │                  │                   │
                    ┌────────┘          ┌───────┘            ┌──────┘
                    ▼                   ▼                    ▼
           provider-service     schedule-service     appointment-service
               :8081                 :8082                 :8083
                  │                     │                      │
             provider_db           schedule_db           appointment_db
              (pg :5432)            (pg :5433)             (pg :5434)
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
| **provider-service** | 8081 | Provider CRUD, soft-delete deactivation |
| **schedule-service** | 8082 | Time slot management, public + internal APIs |
| **appointment-service** | 8083 | Booking, confirmation, cancellation; publishes RabbitMQ events |
| **notification-service** | 8084 | Consumes booking events, sends HTML confirmation emails |
| **gateway-service** | 8080 | Spring Cloud Gateway — path-based routing, single public entry point |

Each service owns its own PostgreSQL database. Cross-service data is denormalized at write time (no cross-service JPA relationships). Appointment booking events are delivered to notification-service via RabbitMQ, so email failures never roll back a booking.

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
| Email | Spring Mail (SMTP) |
| API Docs | SpringDoc OpenAPI (Swagger UI per service) |
| Gateway | Spring Cloud Gateway 5.x (`spring-cloud-starter-gateway-server-webflux`) |
| Build | Gradle multi-module (wrapper included) |
| Runtime | Docker + Docker Compose |

## Quick Start (Docker Compose)

```bash
# Clone and start everything
git clone https://github.com/luisoropeza/scheduler-back.git
cd scheduler-back

# Configure mail credentials (required for email notifications)
# Edit docker-compose.yml and set MAIL_HOST, MAIL_USERNAME, MAIL_PASSWORD, MAIL_FROM

docker compose up --build
```

All services, databases, RabbitMQ, and the gateway start together. The API is available at `http://localhost:8080`.

On first startup each service seeds three sample providers, schedules, and appointments automatically (only if the database is empty).

### Environment Variables

The following variables are supported in `docker-compose.yml`. Only mail settings need to be changed for a working local environment.

| Variable | Default | Used by |
|---|---|---|
| `DB_URL` | *(service-specific postgres URL)* | provider, schedule, appointment services |
| `DB_USERNAME` | `postgres` | provider, schedule, appointment services |
| `DB_PASSWORD` | `mysecretpassword` | provider, schedule, appointment services |
| `PROVIDER_SERVICE_URL` | `http://provider-service:8081` | schedule-service, gateway-service |
| `SCHEDULE_SERVICE_URL` | `http://schedule-service:8082` | appointment-service, gateway-service |
| `APPOINTMENT_SERVICE_URL` | `http://appointment-service:8083` | gateway-service |
| `RABBITMQ_HOST` | `rabbitmq` | appointment-service, notification-service |
| `MAIL_HOST` | `smtp.gmail.com` | notification-service |
| `MAIL_PORT` | `587` | notification-service |
| `MAIL_USERNAME` | `your-email@gmail.com` | notification-service |
| `MAIL_PASSWORD` | `your-app-password` | notification-service |
| `MAIL_FROM` | `your-email@gmail.com` | notification-service |
| `MAIL_FROM_NAME` | `Scheduler` | notification-service |
| `CORS_ALLOWED_ORIGINS` | `*` | provider, schedule, appointment |

## Development Build

```bash
# Compile all services
./gradlew compileJava

# Build all JARs
./gradlew build

# Build a single service
./gradlew :schedule-service:bootJar

# Run a single service (requires external Postgres + RabbitMQ)
./gradlew :provider-service:bootRun

# Run tests
./gradlew test

# Run a single test class
./gradlew :provider-service:test --tests "com.example.provider.SomeTest"
```

Each service's Swagger UI is available at `http://localhost:{port}/swagger-ui.html` when running individually.

## API Reference

All public endpoints are reached through the gateway at `http://localhost:8080`. CORS is enabled for all origins by default.

---

### Providers  `→ provider-service`

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

### Schedules  `→ schedule-service`

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/providers/{providerId}/schedules/available` | List future `AVAILABLE` slots for a provider. |
| `GET` | `/api/providers/{providerId}/schedules` | List all slots (available and booked). |
| `POST` | `/api/providers/{providerId}/schedules` | Add a single time slot. |
| `POST` | `/api/providers/{providerId}/schedules/batch` | Add multiple time slots at once. |
| `DELETE` | `/api/providers/{providerId}/schedules/{scheduleId}` | Remove a slot (fails if already booked). |

**ScheduleRequest**
```json
{
  "startTime": "2026-06-15T09:00:00",
  "endTime":   "2026-06-15T10:00:00"
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
  "endTime":   "2026-06-15T10:00:00",
  "status": "AVAILABLE"
}
```

---

### Appointments  `→ appointment-service`

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/appointments` | Book an appointment on an available slot. Triggers async email notification. |
| `GET` | `/api/appointments/{id}` | Get appointment details by ID. |
| `GET` | `/api/appointments?phone={phone}` | List all appointments for a client phone number. |
| `GET` | `/api/appointments/provider/{providerId}?status=` | List appointments for a provider. Default status: `CONFIRMED`. |
| `PATCH` | `/api/appointments/{id}/confirm` | Confirm a `PENDING` appointment. |
| `PATCH` | `/api/appointments/{id}/cancel` | Cancel an appointment and release the slot back to `AVAILABLE`. |

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
  "providerId": 1,
  "scheduleStart": "2026-06-15T09:00:00",
  "scheduleEnd":   "2026-06-15T10:00:00",
  "providerName": "Dr. Ana García",
  "providerSpecialty": "General Medicine",
  "clientName": "John Doe",
  "clientPhone": "+1-555-1001",
  "clientEmail": "john.doe@example.com",
  "status": "PENDING",
  "notes": "Annual check-up",
  "createdAt": "2026-06-12T14:32:00"
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
- Cancelling an appointment returns its slot to `AVAILABLE`, allowing rebooking.

---

## Email Notifications

When an appointment is booked, appointment-service publishes an `AppointmentBookedEvent` to RabbitMQ. notification-service consumes it and sends an HTML confirmation email to the client (if an email address was provided). The email includes the provider name and specialty, appointment date and time, and any notes.

Email delivery is fully decoupled from the booking transaction — a mail failure has no effect on the appointment record.

---

## Error Responses

All services return the same error structure:

```json
{
  "timestamp": "2026-06-12T14:00:00",
  "status": 404,
  "message": "Schedule not found with id: 99"
}
```

Validation errors (HTTP 400) additionally include an `errors` array with per-field messages.

| HTTP Status | Cause |
|---|---|
| `400` | Invalid request body / failed bean validation |
| `404` | Resource not found |
| `422` | Business rule violation (e.g., booking an already-booked slot) |

---

## Project Structure

```
scheduler-platform/
├── build.gradle              # Parent build — applies plugins to all subprojects
├── settings.gradle           # Module declarations
├── docker-compose.yml        # Full stack: 3 DBs + RabbitMQ + 5 services
├── gateway-service/
│   ├── build.gradle          # spring-cloud-starter-gateway-server-webflux
│   ├── Dockerfile
│   └── src/main/java/com/example/gateway/
│       └── GatewayApplication.java
├── provider-service/
│   ├── build.gradle
│   ├── Dockerfile
│   └── src/main/java/com/example/provider/
│       ├── controller/       # ProviderController
│       ├── service/          # ProviderService + impl
│       ├── repository/       # ProviderRepository
│       ├── entity/           # Provider
│       ├── dto/              # ProviderRequest, ProviderResponse
│       ├── mapper/           # ProviderMapper (MapStruct)
│       ├── exception/        # GlobalExceptionHandler, custom exceptions
│       └── config/           # WebConfig, DataSeeder
├── schedule-service/
│   ├── build.gradle
│   ├── Dockerfile
│   └── src/main/java/com/example/schedule/
│       ├── controller/       # ScheduleController (public), ScheduleInternalController (internal)
│       ├── service/
│       ├── repository/
│       ├── entity/
│       ├── dto/
│       ├── mapper/
│       ├── client/           # ProviderClient (RestClient → provider-service)
│       ├── exception/
│       └── config/
├── appointment-service/
│   ├── build.gradle
│   ├── Dockerfile
│   └── src/main/java/com/example/appointment/
│       ├── controller/       # AppointmentController
│       ├── service/
│       ├── repository/
│       ├── entity/
│       ├── dto/
│       ├── mapper/
│       ├── client/           # ScheduleClient (RestClient → schedule-service internal API)
│       ├── event/            # AppointmentBookedEvent (RabbitMQ payload)
│       ├── exception/
│       └── config/           # RabbitConfig, WebConfig, DataSeeder
└── notification-service/
    ├── build.gradle
    ├── Dockerfile
    └── src/main/java/com/example/notification/
        ├── listener/         # AppointmentEventListener (@RabbitListener)
        ├── service/          # NotificationService + impl (email builder)
        ├── event/            # AppointmentBookedEvent (copy)
        └── config/           # RabbitConfig, MailProperties
```
