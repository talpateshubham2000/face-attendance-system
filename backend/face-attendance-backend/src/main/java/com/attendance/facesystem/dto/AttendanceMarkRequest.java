package com.attendance.facesystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Sent once the frontend has confirmed the "green border for 3 seconds"
 * condition. This frame is sent for face recognition + attendance marking.
 */
@Data
public class AttendanceMarkRequest {

    @NotBlank(message = "image is required")
    private String image; // base64 frame to be recognized
}
