package com.attendance.facesystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Sent repeatedly during face registration while the user is asked to
 * perform a specific action (blink, turn head, smile, etc). This is what
 * stops someone from registering (or later checking in) using a static
 * photo held up to the camera - a photo can't blink or turn.
 *
 * challenge is one of: HOLD_STILL, BLINK, TURN_LEFT, TURN_RIGHT, SMILE
 */
@Data
public class LivenessCheckRequest {

    @NotBlank(message = "image is required")
    private String image;

    @NotBlank(message = "challenge is required")
    private String challenge;
}
