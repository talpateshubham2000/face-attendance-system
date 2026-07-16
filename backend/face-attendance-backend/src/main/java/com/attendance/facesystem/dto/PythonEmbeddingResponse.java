package com.attendance.facesystem.dto;

import lombok.Data;

import java.util.List;

/**
 * Shape of the response we EXPECT from the Python FastAPI service
 * when it generates a face embedding from an image.
 *
 * Example Python response:
 * {
 *   "faceDetected": true,
 *   "embedding": [0.123, -0.456, ...],
 *   "message": "Face detected successfully"
 * }
 */
@Data
public class PythonEmbeddingResponse {
    private boolean faceDetected;
    private List<Double> embedding;
    private String message;
}
