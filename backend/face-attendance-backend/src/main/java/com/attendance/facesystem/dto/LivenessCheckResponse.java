package com.attendance.facesystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LivenessCheckResponse {

    // "GOOD" -> face well-positioned, "ADJUST" -> reposition (same meaning as FaceCheckResponse)
    private String status;

    // true only when the SPECIFIC requested challenge (blink/turn/smile/etc) is currently satisfied
    private boolean challengePassed;

    private String message;
}
