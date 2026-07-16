# Face Attendance System - React Frontend

Public homepage, authentication, guided face registration (with liveness
checks), and the live attendance kiosk screen.

## Architecture Recap

```
This React app  --->  Spring Boot (http://localhost:8080)  --->  Python FastAPI (http://localhost:8000)
```

## Project Structure

```
src/
  api/                    -> axios wrappers (auth, users, face, attendance)
  context/
    AuthContext.jsx        -> global auth state (user, token) via localStorage
  components/
    Navbar.jsx              -> top nav, conditional Login/Signup vs Mark Attendance/Account
    ProtectedRoute.jsx       -> redirects based on login/profile/face-registration status
    ParticleNetworkBackground.jsx -> animated canvas background (face-mesh themed)
    ViewfinderFrame.jsx      -> the scanning frame (corner brackets, green/red, hold-ring)
  hooks/
    useCamera.js             -> webcam access + frame capture as base64
    useScrollReveal.js       -> IntersectionObserver-based fade-in-on-scroll
  pages/
    HomePage.jsx              -> public marketing page
    LoginPage.jsx / SignupPage.jsx
    ForgotPasswordPage.jsx / ResetPasswordPage.jsx
    CompleteProfilePage.jsx   -> name/age/department form (after signup/login)
    FaceRegisterPage.jsx      -> 6-step GUIDED LIVENESS face capture (core anti-spoofing feature)
    AttendanceKioskPage.jsx   -> the live camera screen, auto-returns home after 2 min idle
  styles/
    index.css, viewfinder.css, navbar.css, homepage.css
  App.jsx / main.jsx
```

## Setup (VS Code)

```
npm install
npm run dev
```
Open **http://localhost:5173**. Backend URL is hardcoded in `src/api/axiosClient.js`.

## Full Flow

1. **Homepage** (`/`) - public info page. Nav bar shows Login/Sign up (top right).
2. **Sign up** (`/signup`) - email + password only.
3. **Complete profile** (`/complete-profile`) - name, age, department.
4. **Face setup** (`/face-register`) - the guided **liveness** flow:
   - 6 steps, each with a different instruction: look straight, blink,
     turn left, turn right, smile, look straight again
   - Each step polls `/api/face/liveness-check` continuously; once the
     requested action is held for 3 seconds straight, that photo is
     captured automatically and it moves to the next step - **no buttons,
     fully automatic**
   - This prevents someone from registering with a static photo held up
     to the camera (a photo can't blink or turn its head)
   - After all 6 steps, photos are submitted together to `/api/face/register`
5. **Homepage again** - now shows a "Mark Attendance" button next to the
   account menu in the nav bar.
6. **Live kiosk** (`/kiosk`) - stand in front of the camera, hold still for
   3 seconds once the frame turns green, get recognized + marked
   automatically. **Auto-returns to the homepage after 2 minutes** with
   nobody in front of the camera.

## Design Notes

- The homepage background is an animated particle network (canvas) that
  deliberately echoes the actual face-mesh landmark grid used by the
  recognition system - not just generic "tech" decoration.
- `AuthContext` + `ProtectedRoute` gate pages based on 3 states: logged in,
  profile completed, face registered - always routing to the next
  incomplete step rather than a dead end.
- JWT is stored in `localStorage` and attached automatically to every
  request by `axiosClient`'s interceptor.
- `/api/face/check` and `/api/attendance/mark` don't require login - the
  kiosk screen recognizes WHOEVER stands in front of it, it isn't tied to
  a browser session.

## Suggestions for Going Further (optional, if you want to keep polishing)

- Add a small demo video / GIF on the homepage showing the actual scan flow.
- Add a live "recent check-ins" ticker on the homepage (calls `/api/attendance/all`)
  for a nice "look, it's really working" portfolio touch.
- Add basic rate-limiting or a CAPTCHA on signup if this stays public for long,
  since it's an open portfolio demo.
- Consider a light/dark theme toggle - right now it's dark-only by design.

## Testing Tip

Run all three services together:
1. Python FastAPI on port 8000
2. Spring Boot on port 8080
3. This React app on port 5173 (`npm run dev`)
