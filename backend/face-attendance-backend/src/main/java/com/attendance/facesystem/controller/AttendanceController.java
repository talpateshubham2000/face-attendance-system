package com.attendance.facesystem.controller;

import com.attendance.facesystem.dto.ApiResponse;
import com.attendance.facesystem.dto.AttendanceMarkRequest;
import com.attendance.facesystem.dto.AttendanceResponse;
import com.attendance.facesystem.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    /**
     * The MAIN endpoint for the live camera flow:
     * Once frontend detects "green border for 3 seconds", it sends the frame here.
     * This single call does recognition + attendance marking internally.
     */
    @PostMapping("/mark")
    public ApiResponse<AttendanceResponse> markAttendance(@Valid @RequestBody AttendanceMarkRequest request) {
        AttendanceResponse response = attendanceService.markAttendanceFromFace(request.getImage());
        return ApiResponse.success("Attendance marked successfully!", response);
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<List<AttendanceResponse>> getByUser(@PathVariable Long userId) {
        return ApiResponse.success("Fetched", attendanceService.getAttendanceByUser(userId));
    }

    @GetMapping("/date")
    public ApiResponse<List<AttendanceResponse>> getByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success("Fetched", attendanceService.getAttendanceByDate(date));
    }

    @GetMapping("/all")
    public ApiResponse<List<AttendanceResponse>> getAll() {
        return ApiResponse.success("Fetched", attendanceService.getAllAttendance());
    }
}
