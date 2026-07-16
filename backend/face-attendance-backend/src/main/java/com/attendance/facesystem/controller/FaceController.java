package com.attendance.facesystem.controller;

import com.attendance.facesystem.dto.*;
import com.attendance.facesystem.service.FaceService;
import com.attendance.facesystem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/face")
@RequiredArgsConstructor
public class FaceController {

    private final FaceService faceService;
    private final UserService userService;

    // Called after all 6 liveness-verified photos are captured (see /liveness-check).
    // Requires login - the current user (from JWT) is who the face gets registered to.
    @PostMapping("/register")
    public ApiResponse<String> registerFace(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody FaceRegisterRequest request) {
        Long userId = userService.getUserIdByEmail(principal.getUsername());
        int savedCount = faceService.registerFace(userId, request.getImages());
        return ApiResponse.success("Face registered successfully using " + savedCount + " photo(s).", null);
    }

    // Called continuously (e.g. every second) to show green/red border - no login required
    // (also used by the public live-attendance kiosk screen)
    @PostMapping("/check")
    public ApiResponse<FaceCheckResponse> checkFace(@Valid @RequestBody FaceCheckRequest request) {
        FaceCheckResponse response = faceService.checkFacePosition(request.getImage());
        return ApiResponse.success("Checked", response);
    }

    // Called during face registration - checks whether the user is actually
    // performing the requested live action (blink/turn/smile/hold still).
    @PostMapping("/liveness-check")
    public ApiResponse<LivenessCheckResponse> livenessCheck(@Valid @RequestBody LivenessCheckRequest request) {
        LivenessCheckResponse response = faceService.checkLiveness(request.getImage(), request.getChallenge());
        return ApiResponse.success("Checked", response);
    }
}
