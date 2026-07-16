package com.attendance.facesystem.repository;

import com.attendance.facesystem.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByUserId(Long userId);

    List<Attendance> findByDate(LocalDate date);

    Optional<Attendance> findByUserIdAndDate(Long userId, LocalDate date);

    boolean existsByUserIdAndDate(Long userId, LocalDate date);

    // used by the 24-hour auto-cleanup scheduler
    void deleteByUserId(Long userId);
}
