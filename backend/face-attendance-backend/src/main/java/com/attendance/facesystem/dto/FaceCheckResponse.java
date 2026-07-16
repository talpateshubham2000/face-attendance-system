package com.attendance.facesystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FaceCheckResponse {

    // "GOOD" -> show green border, "ADJUST" -> show red border
    private String status;
    private String message; // e.g. "Face too far", "Face not detected", "Looks good"
}
