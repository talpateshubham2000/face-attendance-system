package com.attendance.facesystem.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Submitted once, right after the user's first login/signup, to fill in
 * their basic info (name, age, department). The user is already
 * authenticated at this point - we know WHO they are from their JWT,
 * so we don't need email/userId in this request body.
 */
@Data
public class CompleteProfileRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Age is required")
    @Min(value = 1, message = "Age must be valid")
    private Integer age;

    private String department; // class / location / department - optional
}
