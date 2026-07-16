package com.attendance.facesystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponse {

    private Long attendanceId;
    private Long userId;
    private String userName;
    private String userEmail;
    private LocalDate date;
    private LocalTime time;
    private String status;
}
