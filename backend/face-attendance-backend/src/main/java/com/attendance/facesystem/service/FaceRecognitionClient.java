package com.attendance.facesystem.service;

import com.attendance.facesystem.dto.FaceCheckResponse;
import com.attendance.facesystem.dto.LivenessCheckResponse;
import com.attendance.facesystem.dto.PythonEmbeddingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * This is the ONLY class that talks to the Python FastAPI (OpenCV) service.
 * Python's job is strictly: "here is an image, detect the face and give me
 * back the embedding" (or "is a face properly positioned"). All business
 * logic (whose face it is, marking attendance, etc.) stays here in Java.
 *
 * NOTE: The Python service isn't built yet - these calls will start working
 * once we build the FastAPI project next. Endpoints assumed:
 *   POST /generate-embedding   { image }         -> PythonEmbeddingResponse
 *   POST /detect-check         { image }         -> { status, message }
 */
@Service
@RequiredArgsConstructor
public class FaceRecognitionClient {

    private final WebClient pythonWebClient;

    /**
     * Sends a single image to Python and gets back the face embedding.
     * Used both during face registration (5-6 times) and during recognition (once).
     */
    public PythonEmbeddingResponse generateEmbedding(String base64Image) {
        return pythonWebClient.post()
                .uri("/generate-embedding")
                .bodyValue(Map.of("image", base64Image))
                .retrieve()
                .bodyToMono(PythonEmbeddingResponse.class)
                .block();
    }

    /**
     * Sends a live camera frame to Python just to check if the face is
     * properly positioned (used to drive the green/red border on the UI).
     */
    public FaceCheckResponse checkFacePosition(String base64Image) {
        return pythonWebClient.post()
                .uri("/detect-check")
                .bodyValue(Map.of("image", base64Image))
                .retrieve()
                .bodyToMono(FaceCheckResponse.class)
                .block();
    }

    /**
     * Sends a frame + a specific challenge ("BLINK", "TURN_LEFT", etc) to
     * Python, which checks whether the user is actually performing that
     * action live - this is what prevents someone from registering (or
     * later checking in) using a static photo held up to the camera.
     */
    public LivenessCheckResponse checkLiveness(String base64Image, String challenge) {
        return pythonWebClient.post()
                .uri("/liveness-check")
                .bodyValue(Map.of("image", base64Image, "challenge", challenge))
                .retrieve()
                .bodyToMono(LivenessCheckResponse.class)
                .block();
    }
}
