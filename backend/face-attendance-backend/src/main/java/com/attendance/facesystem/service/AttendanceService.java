package com.attendance.facesystem.service;

import com.attendance.facesystem.dto.AttendanceResponse;
import com.attendance.facesystem.entity.Attendance;
import com.attendance.facesystem.entity.User;
import com.attendance.facesystem.exception.BadRequestException;
import com.attendance.facesystem.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final FaceService faceService;

    /**
     * This is the main "attendance marking" flow:
     *  1. Take the live camera frame
     *  2. Ask FaceService to recognize which user it belongs to
     *  3. If recognized and not already marked today -> save attendance
     *
     * Returns the AttendanceResponse if successful.
     * Throws BadRequestException if face not recognized or already marked.
     */
    public AttendanceResponse markAttendanceFromFace(String image) {
        Optional<User> recognizedUser = faceService.recognizeFace(image);

        if (recognizedUser.isEmpty()) {
            throw new BadRequestException("Face not recognized. Please make sure you are registered.");
        }

        User user = recognizedUser.get();
        LocalDate today = LocalDate.now();

        if (attendanceRepository.existsByUserIdAndDate(user.getId(), today)) {
            Attendance existing = attendanceRepository.findByUserIdAndDate(user.getId(), today).get();
            throw new BadRequestException(
                    "Attendance already marked for " + user.getName() + " today at " + existing.getTime());
        }

        Attendance attendance = new Attendance();
        attendance.setUser(user);
        attendance.setDate(today);
        attendance.setTime(LocalTime.now());
        attendance.setStatus("PRESENT");

        Attendance saved = attendanceRepository.save(attendance);
        return toResponse(saved);
    }

    public List<AttendanceResponse> getAttendanceByUser(Long userId) {
        return attendanceRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AttendanceResponse> getAttendanceByDate(LocalDate date) {
        return attendanceRepository.findByDate(date).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AttendanceResponse> getAllAttendance() {
        return attendanceRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private AttendanceResponse toResponse(Attendance attendance) {
        return new AttendanceResponse(
                attendance.getId(),
                attendance.getUser().getId(),
                attendance.getUser().getName(),
                attendance.getUser().getEmail(),
                attendance.getDate(),
                attendance.getTime(),
                attendance.getStatus()
        );
    }
}
