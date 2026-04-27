
## Commands

```bash
# Build
./mvnw clean package

# Run (port 8082)
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=FreelancerProfileServiceTest

# Code coverage report (generated under target/site/jacoco/)
./mvnw test jacoco:report

# Sonar analysis (requires local Sonar at localhost:9000)
./mvnw sonar:sonar
```

Swagger UI: `http://localhost:8082/swagger-ui.html`

## Architecture

Standard Spring Boot 3.2.5 layered microservice (Java 17):

```
controllers/    → REST handlers, no business logic
services/       → Interfaces (I*Service) + implementations (*ServiceImpl)
repositories/   → Spring Data JPA repositories with custom @Query
entities/       → JPA domain models
dto/
  request/      → Incoming payloads
  response/     → Outgoing payloads (never expose entities directly)
enums/          → SkillCategory, SkillLevel, AvailabilityStatus, ReportStatus, etc.
security/       → JwtAuthFilter + JwtUtil (token comes from user-service)
scheduler/      → Dynamic scheduled jobs driven by DB config
clients/        → HTTP clients to external services (UserClient, MlServiceClient)
config/         → SecurityConfig, WebSocketConfig, MlServiceConfig
```

## Service Dependencies

This service runs on **port 8082** and depends on:

| Service | URL | Usage |
|---|---|---|
| user-service (Module 01) | `http://localhost:8081` | Fetch user name/email/phone/KYC for notifications |
| ML service (Flask) | `http://localhost:5000` | Sentiment analysis, trust score prediction |
| MySQL | default | Primary datastore |
| Gmail SMTP | external | Profile completion + certification expiry emails |
| Twilio | external | Optional SMS notifications |

`UserClient` and `MlServiceClient` use `RestTemplate` with graceful degradation — if an external service is down, methods return fallback values (e.g., `UNKNOWN` for sentiment).

## Security Model

JWT tokens are **issued by user-service** and validated here via `JwtAuthFilter` → `JwtUtil`. The secret must match: configure `jwt.secret` in `application.properties`.

Authorization tiers:
- **Public** (no token): `GET /api/profiles/**`, `GET /api/reviews/**`, `GET /api/views/**`, Swagger, WebSocket, ML endpoints
- **Authenticated**: profile mutations, skills, portfolio, certifications, experience, education, notifications, CV export
- **Admin** (`hasRole('ADMIN')`): review moderation, report management, admin Excel/PDF exports, scheduler config

## Core Domain

**FreelancerProfile** is the aggregate root. It holds `userId` (FK to user-service — never stored as an entity here) and has `@OneToMany` relationships to: `Skill`, `PortfolioItem`, `WorkExperience`, `Education`, `Certification`, `ProfileReview`, `ProfileView`, `ProfileReport`.

Key derived values (not stored, always computed):
- **completenessScore** (0–100%) — weighted across all profile sections; drives `regionalRank`
- **authenticityScore** per skill — `(examScore + endorsementCount) / 2`
- **trustScore** — sent to ML service with ~10 features, returns `HIGH / MEDIUM / LOW`

## Dynamic Scheduler

`ProfileScheduler` runs a meta-tick every 60 seconds and reads job configs from the `scheduler_config` table. No restart needed to change job timing. Four jobs:

| Job Name | Default Action |
|---|---|
| `recalculateAllSkillScores` | Recomputes authenticity for all skills |
| `updateRegionalRankings` | Re-ranks freelancers by region + completeness |
| `sendProfileCompletionReminders` | Emails users below completion threshold |
| `checkCertificationExpiry` | Marks expired certs, notifies owners |

Manually trigger any job via `POST /api/scheduler/config/{jobName}/run`.

## Real-time Notifications

WebSocket uses raw STOMP (no SockJS) at `ws://localhost:8082/ws`. Clients subscribe to `/topic/user/{userId}/notifications`. The `Notification` entity is persisted in MySQL; unread count drives the frontend badge. Always push via `SimpMessagingTemplate` **after** persisting so the DB is the source of truth.

## Key Constraints

- `(profile_id, normalized_name)` is unique on `Skill` — normalize skill names to lowercase before insert.
- `(client_id, profile_id)` is unique on `ProfileReview` — one review per client per profile.
- `(profile_id, institution, degree)` is unique on `Education`.
- Profile views: self-views are silently dropped; duplicate views in the same session are filtered.
- `avatarUrl` on `FreelancerProfile` is a Cloudinary CDN URL — never a local file path.

## DTO Pattern

Controllers always return `*Response` DTOs, never JPA entities. `@Builder` (Lombok) is used on all DTOs. Request DTOs use Bean Validation (`@NotBlank`, `@Min`, `@Max`, etc.) — validation errors surface as 400 with field-level messages.
