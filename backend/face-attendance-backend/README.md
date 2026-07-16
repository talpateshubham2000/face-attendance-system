# Face Attendance System - Spring Boot Backend

This is the **main backend** for the Face Recognition Attendance System.
It handles: users, authentication (simple), attendance records, and orchestrates
calls to the Python (FastAPI + OpenCV) microservice for anything face-related.

## Architecture

```
ReactJS  <--->  Spring Boot (this project)  <--->  Python FastAPI (OpenCV)
                        |
                        v
                    MySQL Database
```

Spring Boot does NOT do any image processing itself. It forwards images to the
Python service via `FaceRecognitionClient` (using WebClient) and stores/uses
the results (embeddings) for all business logic - matching, attendance, etc.

## Project Structure (layered architecture)

```
config/       -> CorsConfig, WebClientConfig
entity/       -> User, FaceData, Attendance   (JPA entities / DB tables)
repository/   -> UserRepository, FaceDataRepository, AttendanceRepository
dto/          -> Request/Response objects (never expose entities directly)
service/      -> UserService, FaceService, AttendanceService, FaceRecognitionClient
controller/   -> AuthController, UserController, FaceController, AttendanceController
exception/    -> Custom exceptions + GlobalExceptionHandler
```

## How to Run

1. Install Java 17+ and Maven (or just open in Spring Tool Suite - it will handle Maven).
2. Create a MySQL database (or the app can auto-create it, see `application.properties`):
   - Default expects DB `face_attendance_db`, user `root`, password `root` - change
     these in `src/main/resources/application.properties` to match your local MySQL.
   - Alternatively, uncomment the H2 in-memory DB section in `application.properties`
     to test without installing MySQL at all.
3. Run:
   ```
   mvn spring-boot:run
   ```
   Or run `FaceAttendanceSystemApplication.java` directly from your IDE (STS).
4. Server starts on **http://localhost:8080**

> NOTE: The Python FastAPI face service is a separate project (built next).
> Until it's running on `http://localhost:8000`, the `/api/face/**` and
> `/api/attendance/mark` endpoints will fail when they try to reach it -
> that's expected. Everything else (login, user registration) works standalone.

## API Endpoints

### Auth (all public)
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/signup` | body: `{ email, password }` -> creates account, returns JWT |
| POST | `/api/auth/login` | body: `{ email, password }` -> returns JWT |
| POST | `/api/auth/forgot-password` | body: `{ email }` -> returns a reset token (demo mode - would be emailed in production) |
| POST | `/api/auth/reset-password` | body: `{ token, newPassword }` |

### Users (require `Authorization: Bearer <token>`)
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/users/me` | current logged-in user's profile |
| PUT | `/api/users/me` | body: `{ name, age, department }` - completes profile after signup |

### Face
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/face/register` | required | body: `{ images: [...] }` - registers face to the logged-in user |
| POST | `/api/face/check` | public | body: `{ image }` - live green/red border check |
| POST | `/api/face/liveness-check` | public | body: `{ image, challenge }` - verifies a specific live action (BLINK/TURN_LEFT/TURN_RIGHT/SMILE/HOLD_STILL) |

### Attendance (all public - kiosk recognizes whoever stands in front, no login tied to it)
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/attendance/mark` | body: `{ image }` - recognizes face + marks attendance |
| GET | `/api/attendance/user/{userId}` | attendance history for a user |
| GET | `/api/attendance/date?date=YYYY-MM-DD` | attendance for a specific date |
| GET | `/api/attendance/all` | all attendance records |

## Flow Recap (matches project requirement)

1. Signup (`email` + `password`) or Login -> receive a JWT, store it in the frontend
2. Complete profile -> `PUT /api/users/me` (name, age, department)
3. Register face -> capture 6 photos, each verified live via `/api/face/liveness-check`
   (hold still, blink, turn left, turn right, smile, hold still again) -> then `/api/face/register`
4. Live kiosk screen -> continuously calls `/api/face/check` (green/red border), and once
   "green for 3 seconds" -> calls `/api/attendance/mark` once -> recognized + attendance saved

## Important Design Notes

- **JWT-based auth** (stateless, no server-side sessions) - simple by design, as requested.
- Passwords are hashed with BCrypt, never stored or returned in plaintext.
- **Forgot password is simplified for this demo**: since there's no email service wired up,
  `/api/auth/forgot-password` returns the reset token directly in the response instead of
  emailing it. In a real deployment, integrate an email provider and never return the token.
- **24-hour auto-cleanup**: since this project is meant to be shared as a public portfolio
  demo, `DataCleanupService` runs every hour and permanently deletes any account (plus its
  face data and attendance records) that is more than 24 hours old. Remove/adjust this if
  you ever deploy this for a real class/office.
- Face matching (cosine similarity) still happens **in Java** (`FaceService`) - Python's
  job is only detection, liveness-checking, and embedding generation.
- `face.match.threshold` default was raised to `0.90` because the Python service now uses
  Mediapipe landmark-based embeddings (not a deep-learning model), which naturally produce
  higher similarity scores between different people too - test and tune this with real data.

## Next Steps

1. Update the Python FastAPI service with the new `/liveness-check` endpoint
   (blink/turn/smile detection using Mediapipe Face Mesh).
2. Rebuild the ReactJS frontend: public homepage, login/signup pages with forgot-password,
   the guided 6-step liveness face registration, and a kiosk screen that auto-returns home
   after 2 minutes of inactivity.
