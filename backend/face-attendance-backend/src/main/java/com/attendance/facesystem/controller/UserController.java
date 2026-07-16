package com.attendance.facesystem.controller;

import com.attendance.facesystem.dto.ApiResponse;
import com.attendance.facesystem.dto.CompleteProfileRequest;
import com.attendance.facesystem.dto.UserResponse;
import com.attendance.facesystem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Fetches the profile of whoever is currently logged in (from JWT)
    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(@AuthenticationPrincipal UserDetails principal) {
        UserResponse user = userService.getUserByEmail(principal.getUsername());
        return ApiResponse.success("Fetched", user);
    }

    // Step right after signup/login: fill basic info (name, age, department)
    @PutMapping("/me")
    public ApiResponse<UserResponse> completeProfile(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CompleteProfileRequest request) {
        UserResponse user = userService.completeProfile(principal.getUsername(), request);
        return ApiResponse.success("Profile updated successfully", user);
    }
}
