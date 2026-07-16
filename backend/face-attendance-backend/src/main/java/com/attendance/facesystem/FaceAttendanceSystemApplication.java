package com.attendance.facesystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FaceAttendanceSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(FaceAttendanceSystemApplication.class, args);
    }

}
