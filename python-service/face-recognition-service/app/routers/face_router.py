"""
Router = equivalent of a "Controller" in Spring Boot.
Defines the actual HTTP endpoints that Spring Boot's FaceRecognitionClient calls.
"""

from fastapi import APIRouter

from app.schemas import ImageRequest, EmbeddingResponse, CheckResponse, LivenessCheckRequest, LivenessCheckResponse
from app.services.face_service import face_service

router = APIRouter(tags=["Face"])


@router.post("/generate-embedding", response_model=EmbeddingResponse)
def generate_embedding(request: ImageRequest):
    """
    Called by Spring Boot:
      - during face registration (once per captured photo, 6 times)
      - during attendance marking (once, on the confirmed frame)
    """
    return face_service.generate_embedding(request.image)


@router.post("/detect-check", response_model=CheckResponse)
def detect_check(request: ImageRequest):
    """
    Called repeatedly by Spring Boot (forwarded from React) while the user
    stands in front of the live camera, to drive the green/red border UI.
    """
    return face_service.check_face_position(request.image)


@router.post("/liveness-check", response_model=LivenessCheckResponse)
def liveness_check(request: LivenessCheckRequest):
    """
    Called during the 6-step guided face registration flow. Confirms the
    user is actually performing the requested live action (blink, turn,
    smile, hold still) - this is what stops someone from registering (or
    later checking in) using a static photo held up to the camera.
    """
    return face_service.check_liveness(request.image, request.challenge)
