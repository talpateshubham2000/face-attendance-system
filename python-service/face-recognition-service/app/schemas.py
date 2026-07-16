"""
Pydantic models (schemas) for request/response bodies.
These shapes MUST match what the Spring Boot side expects
(see PythonEmbeddingResponse.java and FaceCheckResponse.java).
"""

from typing import List, Optional
from pydantic import BaseModel, Field


class ImageRequest(BaseModel):
    """Common request body - a single base64 encoded image."""
    image: str = Field(..., description="Base64 encoded image, may include 'data:image/jpeg;base64,' prefix")


class EmbeddingResponse(BaseModel):
    """
    Returned from /generate-embedding.
    Matches Java's PythonEmbeddingResponse exactly (field names must match).
    """
    faceDetected: bool
    embedding: Optional[List[float]] = None
    message: str


class CheckResponse(BaseModel):
    """
    Returned from /detect-check.
    Matches Java's FaceCheckResponse exactly (field names must match).
    status: "GOOD" (green border) or "ADJUST" (red border)
    """
    status: str
    message: str


class LivenessCheckRequest(BaseModel):
    """
    Request for /liveness-check - a frame plus which live action is
    currently being asked of the user.
    challenge: one of HOLD_STILL, BLINK, TURN_LEFT, TURN_RIGHT, SMILE
    """
    image: str = Field(..., description="Base64 encoded image")
    challenge: str = Field(..., description="HOLD_STILL | BLINK | TURN_LEFT | TURN_RIGHT | SMILE")


class LivenessCheckResponse(BaseModel):
    """
    Returned from /liveness-check.
    Matches Java's LivenessCheckResponse exactly (field names must match).
    """
    status: str            # "GOOD" or "ADJUST" (face position, same meaning as CheckResponse)
    challengePassed: bool   # true only if the SPECIFIC requested action is currently satisfied
    message: str
