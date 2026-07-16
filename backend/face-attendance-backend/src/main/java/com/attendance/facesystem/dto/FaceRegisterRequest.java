package com.attendance.facesystem.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * Sent from React after capturing photos via webcam (each verified live with
 * a liveness challenge - see LivenessCheckRequest). The user is already
 * authenticated (JWT), so we don't need a userId in the body - the
 * controller resolves the current user from the token.
 */
@Data
public class FaceRegisterRequest {

    @NotEmpty(message = "At least a few photos are required")
    private List<String> images; // base64 encoded strings, e.g. "data:image/jpeg;base64,..."
}
