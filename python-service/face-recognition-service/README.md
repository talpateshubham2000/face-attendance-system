# Face Recognition Service (Python + FastAPI + OpenCV + Mediapipe + ONNX)

This is the **face-related microservice** for the Face Attendance System.
Its job: detect faces, check liveness, and generate real identity embeddings.
It has **no database** and **no business logic** - that all lives in the
Spring Boot backend.

## Architecture Recap

```
ReactJS  --->  Spring Boot (Java)  --->  Python FastAPI (this project)
                                              |
                              Mediapipe (detection/liveness) + MobileFaceNet ONNX (identity)
```

Spring Boot's `FaceRecognitionClient` calls this service's endpoints and uses
the results for everything else (saving embeddings, matching, attendance).

## Why two different models?

- **Mediapipe Face Mesh** - fast, lightweight, no C++ build tools needed.
  Used for: face detection, position checking (green/red border), and
  liveness checks (blink/turn/smile). Great at *finding and describing* a
  face, but its landmark geometry is NOT discriminative enough to reliably
  tell two *different* people apart (everyone's face proportions are
  roughly similar) - an earlier version of this project tried using
  landmark geometry as the "embedding" and any photo could match any
  registered account.
- **MobileFaceNet (ONNX)** - a real, pretrained deep-learning face
  recognition model (from InsightFace, ~13MB, no dlib/C++ build needed since
  it just runs via `onnxruntime`). This is what actually distinguishes one
  person's face from another. Only used for the final 512-d identity
  embedding, both during registration and recognition.

This combination gives good speed (Mediapipe) AND accurate identity
matching (MobileFaceNet), without ever needing to compile `dlib`.

## Project Structure

```
app/
  main.py               -> FastAPI app entrypoint, CORS setup
  schemas.py             -> Pydantic request/response models
  models/
    w600k_mbf.onnx        -> MobileFaceNet ONNX model (bundled, ~13MB)
  routers/
    face_router.py       -> HTTP endpoints (equivalent of a "Controller")
  services/
    face_service.py       -> Detection, liveness, alignment + embedding logic
  utils/
    image_utils.py         -> base64 -> OpenCV image conversion
requirements.txt
```

## Setup (PyCharm / any IDE)

1. Open this folder as a project.
2. Create a virtual environment:
   ```
   python -m venv venv
   venv\Scripts\activate        (Windows)
   source venv/bin/activate     (Mac/Linux)
   ```
3. Install dependencies:
   ```
   pip install -r requirements.txt
   ```
   No C++ build tools needed - every dependency here (`mediapipe`,
   `onnxruntime`, `opencv-python`) installs from prebuilt wheels.

4. Run the service:
   ```
   uvicorn app.main:app --reload --port 8000
   ```
5. Service runs on **http://localhost:8000** - open **http://localhost:8000/docs**
   for the Swagger UI to test endpoints directly.

## API Endpoints

### POST `/generate-embedding`
Used for both face registration (6 times) and attendance recognition (once).
Internally: detects the face, extracts 5 alignment landmarks (eyes, nose,
mouth corners), aligns to a standard 112x112 crop, runs it through
MobileFaceNet, and returns a normalized 512-dimension embedding.

Request:
```json
{ "image": "data:image/jpeg;base64,....." }
```

Response:
```json
{
  "faceDetected": true,
  "embedding": [0.041, -0.083, ...],   // 512 numbers, L2-normalized
  "message": "Face detected successfully"
}
```

### POST `/detect-check`
Used continuously by the live camera screen to drive the green/red border.
Uses Mediapipe Face Detection only (fast, doesn't need the recognition model).

Request: `{ "image": "data:image/jpeg;base64,....." }`
Response: `{ "status": "GOOD", "message": "Face positioned correctly" }`

### POST `/liveness-check`
Used during the 6-step guided face registration flow. Confirms the user is
actually performing the requested live action - this is what stops someone
from registering (or later checking in) with a static photo held up to the
camera.

Request:
```json
{ "image": "data:image/jpeg;base64,.....", "challenge": "BLINK" }
```
`challenge` is one of: `HOLD_STILL`, `BLINK`, `TURN_LEFT`, `TURN_RIGHT`, `SMILE`

Response:
```json
{ "status": "GOOD", "challengePassed": true, "message": "Eyes closed - hold it" }
```

**How each challenge is detected (Mediapipe Face Mesh landmarks):**
- `HOLD_STILL` - face roughly frontal (low left/right asymmetry) + eyes open
- `BLINK` - Eye Aspect Ratio (EAR) drops below threshold (eyes closed - the
  instruction is phrased as "close your eyes for a moment" since holding a
  literal blink for 3 seconds isn't physically natural)
- `TURN_LEFT` / `TURN_RIGHT` - asymmetry between nose-to-left-cheek vs nose-to-right-cheek distance
- `SMILE` - mouth stretches wide AND/OR opens slightly (teeth-showing smiles do both)

> Note: the browser mirrors the camera preview for the user, but the raw
> frame sent to this service is NOT mirrored - so "turn left" on screen may
> correspond to the opposite geometric direction here. That's fine, the two
> turn challenges just need to be two distinct, verifiably-live poses.

All thresholds are defined as constants at the top of
`app/services/face_service.py` - **there are debug `print()` statements in
the terminal for BLINK/TURN/SMILE checks showing the live measured ratio
next to the required threshold** - if a challenge won't trigger for you,
check the terminal output while testing and adjust the threshold constant
to match (camera quality/lighting/distance all affect these numbers).

### GET `/health`
Simple health check - returns `{ "status": "UP" }`.

## How Matching Actually Works (recap)

This service converts a face image into a 512-number identity "fingerprint"
using MobileFaceNet. The Spring Boot `FaceService` stores these numbers in
the database and compares them using cosine similarity when deciding who is
standing in front of the camera. Real testing showed same-person comparisons
scoring ~0.5-0.99+ and different-person comparisons scoring near 0 or
negative - `face.match.threshold=0.40` in the Spring Boot
`application.properties` is a safe starting point.

## Important: Thread Safety

Mediapipe's Face Detection/Face Mesh objects are **not thread-safe**, but
FastAPI serves requests on multiple worker threads. All calls to them are
wrapped in a `threading.Lock()` in `face_service.py` - without this, rapid
polling from the frontend can crash the service with
`"Packet timestamp mismatch"` errors. (ONNX Runtime sessions, unlike
Mediapipe, ARE thread-safe, so no lock is needed around the recognition
model itself.)

## Next Step

Once this is running on port 8000 and Spring Boot is running on port 8080,
the two should already talk to each other automatically (Spring Boot is
pre-configured with `python.service.base-url=http://localhost:8000`).
