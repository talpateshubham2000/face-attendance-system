package com.attendance.facesystem.repository;

import com.attendance.facesystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByResetToken(String resetToken);

    // used by the 24-hour auto-cleanup scheduler (this is a portfolio demo)
    List<User> findByCreatedAtBefore(LocalDateTime cutoff);
}
