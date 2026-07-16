package com.attendance.facesystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // filled in later via the "complete profile" step - null right after signup
    private String name;

    // Email acts as the unique user identifier (as discussed)
    @Column(nullable = false, unique = true)
    private String email;

    // BCrypt-hashed password, never returned in any API response
    @Column(nullable = false)
    private String password;

    private Integer age;

    // e.g. class / department / location where this user belongs
    private String department;

    // true once the user has filled the "basic info" form after signup
    @Column(name = "profile_completed")
    private boolean profileCompleted = false;

    // becomes true once the user has completed face registration (6 photos)
    @Column(name = "face_registered")
    private boolean faceRegistered = false;

    // simplified "forgot password" support - a short-lived token generated on request
    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "reset_token_expiry")
    private LocalDateTime resetTokenExpiry;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
