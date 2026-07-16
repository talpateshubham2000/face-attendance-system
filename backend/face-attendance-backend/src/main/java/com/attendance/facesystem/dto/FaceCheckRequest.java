package com.attendance.facesystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Sent repeatedly (e.g. every ~1 sec) from React while user stands in front
 * of the camera, just to check if the face is properly positioned
 * (used to show green/red square border on frontend).
 */
@Data
public class FaceCheckRequest {

    @NotBlank(message = "image is required")
    private String image; // single base64 frame from webcam
}
