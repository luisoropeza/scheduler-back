# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build all services
./gradlew build

# Compile without tests
./gradlew compileJava

# Build a single service JAR
./gradlew :provider-service:bootJar

# Run a single service (requires Postgres / RabbitMQ already running)
./gradlew :provider-service:bootRun

# Run all tests
./gradlew test

# Run tests for one service
./gradlew :appointment-service:test

# Run a single test class
./gradlew :provider-service:test --tests "com.example.provider.SomeTest"

# Run a single test method
./gradlew :provider-service:test --tests "com.example.provider.SomeTest.methodName"

# Start the full stack
docker compose up --build
```

## Architecture

Five Spring Boot services in a Gradle multi-module build. Each service is a fully independent deployable with its own database.

```
gateway-service :8080  (WebFlux reverse proxy)
  /api/providers/**     → provider-service   :8081  (PostgreSQL :5432)
  /api/schedules/**     → schedule-service   :8082  (PostgreSQL :5433)
  /api/appointments/**  → appointment-service :8083  (PostgreSQL :5434)
                                                          │
                                                     RabbitMQ :5672
                                                          │
                                              notification-service :8084  (SMTP)
```

**Tech stack:** Java 26, Spring Boot 4.0.6, Spring Data JPA, MapStruct 1.6.3, Lombok, SpringDoc OpenAPI (Swagger UI at `/swagger-ui.html` per service).

## Key Design Patterns

**Cross-service data denormalization.** There are no cross-service JPA relationships. When a schedule is created, it copies `providerName` and `providerSpecialty` from a synchronous call to provider-service. When an appointment is booked, it copies the full schedule and provider snapshot. This means reads never fan out across services.

**Internal vs. public APIs in schedule-service.** `ScheduleController` exposes the public `/api/schedules/**` endpoints. `ScheduleInternalController` exposes `/internal/schedules/{id}/book` and `/internal/schedules/{id}/release` — called only by appointment-service to change slot status. The gateway does not route `/internal/**`.

**Synchronous inter-service calls use `RestClient`** (blocking). `ProviderClient` in schedule-service calls provider-service; `ScheduleClient` in appointment-service calls the internal schedule-service API.

**RabbitMQ for booking events.** After a successful booking, appointment-service publishes `AppointmentBookedEvent` to a topic exchange (`appointment.exchange`, routing key `appointment.booked`). notification-service listens on `appointment.booking.queue` and sends an HTML email. Because these are decoupled, email failure never rolls back a booking. The `AppointmentBookedEvent` record is duplicated in both services (no shared module).

**Appointment lifecycle:** `PENDING → CONFIRMED` or `PENDING → CANCELLED`. Cancellation calls the internal schedule-service API to release the slot back to `AVAILABLE`.

**gateway-service** is a Spring WebFlux `WebFilter` (`RoutingFilter`) that proxies requests using `WebClient`. It reads target URLs from `GatewayProperties` (`@ConfigurationProperties(prefix = "gateway")`), which are set via `PROVIDER_SERVICE_URL`, `SCHEDULE_SERVICE_URL`, and `APPOINTMENT_SERVICE_URL` environment variables. It strips hop-by-hop headers and streams bodies without buffering.

## Service Package Roots

| Service | Package |
|---|---|
| gateway-service | `com.example.gateway` |
| provider-service | `com.example.provider` |
| schedule-service | `com.example.schedule` |
| appointment-service | `com.example.appointment` |
| notification-service | `com.example.notification` |

Each service follows the same layered layout: `controller/`, `service/`, `repository/`, `entity/`, `dto/`, `mapper/`, `exception/`, `config/`. schedule-service and appointment-service also have a `client/` package for outbound HTTP calls.

## Error Handling

All services use a `GlobalExceptionHandler` (`@RestControllerAdvice`) that maps `ResourceNotFoundException` → 404, `BusinessException` → 422, and validation errors → 400. The response body always has `timestamp`, `status`, and `message` fields.

## Data Seeding

Each service with a database has a `DataSeeder` (`ApplicationRunner`) that inserts sample records on startup if the table is empty.
