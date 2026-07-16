"""
Entry point for the Python Face Recognition microservice.
Run with:  uvicorn app.main:app --reload --port 8000
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.routers import face_router

app = FastAPI(
    title="Face Recognition Service",
    description="Python microservice (OpenCV + face_recognition) used by the "
                "Spring Boot backend for face detection, embedding generation, "
                "and live position checking. This service holds NO business logic "
                "and NO database - Spring Boot handles all of that.",
    version="1.0.0"
)

# Allow calls from Spring Boot (running on a different port)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(face_router.router)


@app.get("/")
def root():
    return {"service": "face-recognition-service", "status": "running"}


@app.get("/health")
def health_check():
    return {"status": "UP"}
