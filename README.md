# RideShare Backend

A Spring Boot + MongoDB REST API for a simple ride-booking workflow: passengers request rides, drivers accept them, and either party marks the ride completed. Authentication is stateless JWT with two roles (`ROLE_USER` for passengers, `ROLE_DRIVER` for drivers). Interactive API docs are served with Swagger UI.

## Tech stack

- **Java 17**, **Spring Boot 3.3** (Web, Security, Validation, Data MongoDB)
- **MongoDB** for persistence
- **JWT** via jjwt 0.12 (HS256, secret from the environment)
- **springdoc-openapi** for OpenAPI docs and Swagger UI
- **Lombok** for boilerplate
- **JUnit 5, MockMvc, Testcontainers** for integration tests against a real MongoDB

## Project structure

```
src/main/java/org/example/rideshare
├── RideshareApplication.java
├── config/       SecurityConfig, JwtAuthFilter, OpenApiConfig
├── controller/   AuthController, UserRideController, DriverRideController, HomeController
├── dto/          RegisterRequest, LoginRequest, CreateRideRequest, JwtResponse
├── exception/    GlobalExceptionHandler, BadRequestException, ForbiddenException, NotFoundException
├── model/        User, Ride
├── repository/   UserRepository, RideRepository
├── service/      AuthService, RideService, CustomUserDetailsService
└── util/         JwtUtil
```

Requests flow controller → service → repository. `JwtAuthFilter` validates the `Authorization: Bearer <token>` header on every request and `SecurityConfig` applies the role rules.

## Domain

**User**: `id`, `username`, `password` (BCrypt hash), `role` (`ROLE_USER` or `ROLE_DRIVER`).

**Ride**: `id`, `userId`, `driverId`, `pickupLocation`, `dropLocation`, `status` (`REQUESTED → ACCEPTED → COMPLETED`), `createdAt`.

## Endpoints

| Method | Path | Who | Purpose |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | Public | Register a passenger or driver |
| POST | `/api/auth/login` | Public | Log in and receive a JWT |
| POST | `/api/v1/rides` | `ROLE_USER` | Request a ride |
| GET | `/api/v1/user/rides` | `ROLE_USER` | List my rides |
| GET | `/api/v1/driver/rides/requests` | `ROLE_DRIVER` | List rides waiting for a driver |
| POST | `/api/v1/driver/rides/{rideId}/accept` | `ROLE_DRIVER` | Accept a requested ride |
| POST | `/api/v1/rides/{rideId}/complete` | That ride's passenger or driver | Mark an accepted ride completed |
| GET | `/swagger-ui.html` | Public | Interactive API docs (`/` redirects here) |

### Rules enforced

- Registration accepts only `ROLE_USER` or `ROLE_DRIVER`; passwords need at least 8 characters; usernames are unique.
- Only passengers can request rides and only drivers can accept them.
- A ride can be accepted only while `REQUESTED`, and completed only while `ACCEPTED`.
- Only the ride's own passenger or assigned driver can complete it.

### Errors

Every error has the same JSON shape:

```json
{ "error": "FORBIDDEN", "message": "Only drivers can accept rides", "timestamp": "2026-10-29T08:19:07Z" }
```

| Status | `error` | When |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` / `BAD_REQUEST` | Invalid body, wrong ride state, duplicate username |
| 401 | `UNAUTHORIZED` | Missing, invalid or expired token; wrong username or password |
| 403 | `FORBIDDEN` | Role or ownership does not allow the action |
| 404 | `NOT_FOUND` | Ride or user does not exist |
| 500 | `INTERNAL_ERROR` | Unexpected error (details are logged, not returned) |

## Configuration

All settings come from environment variables; nothing secret is committed.

| Variable | Required | Default | Purpose |
| --- | --- | --- | --- |
| `JWT_SECRET` | Yes | — | HS256 signing key, at least 32 characters. The app refuses to start without it. |
| `MONGODB_URI` | No | `mongodb://localhost:27017/rideshare` | MongoDB connection string (e.g. MongoDB Atlas) |
| `JWT_EXPIRATION_MS` | No | `3600000` (1 hour) | Token lifetime |
| `PORT` | No | `8081` | HTTP port |

Generate a secret with `openssl rand -hex 32`.

## Running locally

Requires Java 17+ and MongoDB (Docker is easiest).

```bash
docker run -d --name rideshare-mongo -p 27017:27017 mongo:7
export JWT_SECRET=$(openssl rand -hex 32)
./mvnw spring-boot:run
```

Or build and run the jar:

```bash
./mvnw clean package
java -jar target/rideshare-0.0.1-SNAPSHOT.jar
```

Then open http://localhost:8081 for Swagger UI.

## Trying it in Swagger UI

1. `POST /api/auth/register` with `{"username":"john","password":"password1","role":"ROLE_USER"}`, and again for a driver with `"role":"ROLE_DRIVER"`.
2. `POST /api/auth/login` and copy the `token`.
3. Click **Authorize**, paste the token, and call the passenger endpoints. Log in as the driver and authorize with that token to accept the ride.

## Sample requests (curl)

```bash
# Register a passenger and a driver
curl -X POST http://localhost:8081/api/auth/register -H "Content-Type: application/json" \
  -d '{"username":"john","password":"password1","role":"ROLE_USER"}'
curl -X POST http://localhost:8081/api/auth/register -H "Content-Type: application/json" \
  -d '{"username":"driver1","password":"password1","role":"ROLE_DRIVER"}'

# Log in (returns {"token":"..."})
curl -X POST http://localhost:8081/api/auth/login -H "Content-Type: application/json" \
  -d '{"username":"john","password":"password1"}'

# Request a ride as the passenger
curl -X POST http://localhost:8081/api/v1/rides -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" -d '{"pickupLocation":"MG Road","dropLocation":"Airport"}'

# As the driver: list requests, then accept one
curl http://localhost:8081/api/v1/driver/rides/requests -H "Authorization: Bearer <DRIVER_TOKEN>"
curl -X POST http://localhost:8081/api/v1/driver/rides/<RIDE_ID>/accept -H "Authorization: Bearer <DRIVER_TOKEN>"

# Complete it (passenger or driver of that ride)
curl -X POST http://localhost:8081/api/v1/rides/<RIDE_ID>/complete -H "Authorization: Bearer <USER_TOKEN>"
```

## Deployment (Render + MongoDB Atlas)

`render.yaml` defines a free Docker web service (`Dockerfile`: Maven build stage, JRE 17 runtime as a non-root user).

1. Create a free MongoDB Atlas cluster, a database user, and allow network access from `0.0.0.0/0` (Render's free tier has no fixed outbound IP).
2. In Render: **New → Blueprint** → this repository. Render generates `JWT_SECRET`.
3. Set `MONGODB_URI` to the Atlas connection string with the database name, e.g. `mongodb+srv://<user>:<password>@<cluster>.mongodb.net/rideshare`.

Free instances sleep after 15 minutes idle, so the first request can take about a minute.

## Tests

```bash
./mvnw test
```

17 integration tests start a real MongoDB with Testcontainers (Docker must be running) and drive the API through MockMvc: registration and login, invalid roles and passwords, 401 for missing or tampered tokens, the full request → accept → complete flow, role and ownership checks (403), invalid state transitions, 404s, and the public API docs.
