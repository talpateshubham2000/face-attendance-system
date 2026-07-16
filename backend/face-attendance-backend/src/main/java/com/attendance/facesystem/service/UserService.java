package com.attendance.facesystem.service;

import com.attendance.facesystem.dto.CompleteProfileRequest;
import com.attendance.facesystem.dto.UserResponse;
import com.attendance.facesystem.entity.User;
import com.attendance.facesystem.exception.ResourceNotFoundException;
import com.attendance.facesystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return toResponse(user);
    }

    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toResponse(user);
    }

    public User getUserEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public Long getUserIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();
    }

    /**
     * Step right after signup/login: fills in name, age, department for the
     * currently authenticated user (identified by email from their JWT).
     */
    public UserResponse completeProfile(String email, CompleteProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setName(request.getName());
        user.setAge(request.getAge());
        user.setDepartment(request.getDepartment());
        user.setProfileCompleted(true);

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    public void markFaceRegistered(Long userId) {
        User user = getUserEntityById(userId);
        user.setFaceRegistered(true);
        userRepository.save(user);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAge(),
                user.getDepartment(),
                user.isProfileCompleted(),
                user.isFaceRegistered()
        );
    }
}
