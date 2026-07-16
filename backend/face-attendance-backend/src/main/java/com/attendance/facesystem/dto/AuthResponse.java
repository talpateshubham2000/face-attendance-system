package com.attendance.facesystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private Long id;
    private String name;
    private String email;
    private boolean profileCompleted;
    private boolean faceRegistered;
}
