# RideShare Backend

A lightweight Spring Boot + MongoDB backend designed for a simple ride-booking workflow. It uses JWT for authentication, role-based authorization, request validation, and central exception handling. The project follows a clear controller → service → repository structure.

## Technology Overview

•⁠  ⁠*Spring Boot 3.2.x* (Web, Security, Validation, MongoDB)
•⁠  ⁠*JSON Web Tokens* (jjwt 0.12.x)
•⁠  ⁠*MongoDB* for persistence
•⁠  ⁠*Lombok* to simplify boilerplate
•⁠  ⁠*Java 21*

## Project Structure

•⁠  ⁠*Application entry point:* ⁠ RideshareApplication.java ⁠
•⁠  ⁠*Security layer:* Stateless JWT authentication with role verification handled inside ⁠ SecurityConfig.java ⁠ and token processing in ⁠ JwtFilter.java ⁠.
•⁠  ⁠*Authentication:* User registration & login managed through ⁠ AuthController ⁠, backed by ⁠ AuthServiceImpl ⁠ and JWT helper methods in ⁠ JwtUtil ⁠.
•⁠  ⁠*Ride domain:* User ride operations inside ⁠ RideController ⁠, and driver-specific actions inside ⁠ DriverController ⁠, implemented via ⁠ RideServiceImpl ⁠.
•⁠  ⁠*Data access:* ⁠ UserRepository ⁠ and ⁠ RideRepository ⁠ provide database operations.
•⁠  ⁠*Entities:* ⁠ User ⁠ and ⁠ Ride ⁠ models define fields and lifecycle.
•⁠  ⁠*DTOs:* Request objects include embedded Jakarta validation.
•⁠  ⁠*Error handling:* Centralized through ⁠ GlobalExceptionHandler ⁠, with custom ⁠ BadRequestException ⁠ and ⁠ NotFoundException ⁠.

## Domain Entities

### User

Fields: ⁠ id ⁠, ⁠ username ⁠, ⁠ password ⁠ (BCrypt), and a ⁠ role ⁠ that can be ⁠ ROLE_USER ⁠ or ⁠ ROLE_DRIVER ⁠.

### Ride

Fields: ⁠ id ⁠, ⁠ userId ⁠, ⁠ driverId ⁠, ⁠ pickupLocation ⁠, ⁠ dropLocation ⁠, ⁠ status ⁠ (⁠ REQUESTED → ACCEPTED → COMPLETED ⁠), and ⁠ createdAt ⁠.

## Authentication & Authorization Flow

•⁠  ⁠Public routes: ⁠ /api/auth/** ⁠
•⁠  ⁠JWT required for all other endpoints via ⁠ Authorization: Bearer <token> ⁠
•⁠  ⁠Role-specific access:

    * ⁠ ROLE_DRIVER ⁠ → ⁠ /api/v1/driver/** ⁠
    * ⁠ ROLE_USER ⁠ → ⁠ /api/v1/user/** ⁠
•⁠  ⁠JWT includes ⁠ username ⁠ + ⁠ role ⁠, validated via ⁠ JwtUtil ⁠.

## Validation & Error Response System

•⁠  ⁠DTO classes use Jakarta Bean Validation.
•⁠  ⁠Validation errors return a ⁠ 400 ⁠ response with a descriptive message.
•⁠  ⁠Domain-specific issues return ⁠ BAD_REQUEST ⁠ or ⁠ NOT_FOUND ⁠ via the global handler.

## Available Endpoints

| Method | Path                                   | Role           | Purpose                    |
| ------ | -------------------------------------- | -------------- | -------------------------- |
| POST   | ⁠ /api/auth/register ⁠                   | Public         | Register a user or driver  |
| POST   | ⁠ /api/auth/login ⁠                      | Public         | Login & receive JWT token  |
| POST   | ⁠ /api/v1/rides ⁠                        | ROLE_USER      | Create a new ride          |
| GET    | ⁠ /api/v1/user/rides ⁠                   | ROLE_USER      | List user’s rides          |
| GET    | ⁠ /api/v1/driver/rides/requests ⁠        | ROLE_DRIVER    | View pending ride requests |
| POST   | ⁠ /api/v1/driver/rides/{rideId}/accept ⁠ | ROLE_DRIVER    | Accept a pending ride      |
| POST   | ⁠ /api/v1/rides/{rideId}/complete ⁠      | USER or DRIVER | Mark ride as completed     |

## Sample Usage (Replace ⁠ <TOKEN> ⁠)

⁠ bash
# User registration
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"1234","role":"ROLE_USER"}'

# Driver registration
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"driver1","password":"abcd","role":"ROLE_DRIVER"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"1234"}'

# Create ride (user)
curl -X POST http://localhost:8080/api/v1/rides \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"pickupLocation":"A","dropLocation":"B"}'

# Driver view pending rides
curl -X GET http://localhost:8080/api/v1/driver/rides/requests \
  -H "Authorization: Bearer <TOKEN>"

# Accept a ride
curl -X POST http://localhost:8080/api/v1/driver/rides/{rideId}/accept \
  -H "Authorization: Bearer <TOKEN>"

# Complete a ride
curl -X POST http://localhost:8080/api/v1/rides/{rideId}/complete \
  -H "Authorization: Bearer <TOKEN>"
 ⁠

## Configuration

Add the following to your ⁠ application.properties ⁠:

⁠ properties
spring.data.mongodb.uri=mongodb://localhost:27017/rideshare
jwt.secret=replace-with-strong-256bit-secret
jwt.expirationMs=86400000
# server.port=8080
 ⁠

## Running the Project

1.⁠ ⁠Make sure MongoDB is running or adjust the DB URI.
2.⁠ ⁠Start the backend using:

   ⁠ bash
   ./mvnw spring-boot:run
    ⁠

   or package it with:

   ⁠ bash
   ./mvnw clean package
   java -jar target/rideshare-0.0.1-SNAPSHOT.jar
    ⁠

## Additional Notes

•⁠  ⁠All passwords are hashed using BCrypt.
•⁠  ⁠JWT must be included in every protected request header.
•⁠  ⁠Service-level role checks ensure proper access control.
•⁠  ⁠Error responses contain ⁠ error ⁠, ⁠ message ⁠, ⁠ timestamp ⁠.

---
