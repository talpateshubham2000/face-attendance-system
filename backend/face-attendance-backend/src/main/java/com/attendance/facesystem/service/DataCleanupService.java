package com.attendance.facesystem.service;

import com.attendance.facesystem.entity.User;
import com.attendance.facesystem.repository.AttendanceRepository;
import com.attendance.facesystem.repository.FaceDataRepository;
import com.attendance.facesystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * This project is deployed as a public portfolio DEMO - anyone visiting can
 * sign up and try the whole flow. To avoid indefinitely accumulating
 * strangers' face data and personal info, every account (plus its face
 * data and attendance records) is automatically wiped exactly 24 hours
 * after it was created.
 *
 * NOTE: if you deploy this for REAL classroom/office use (not as a demo),
 * remove or heavily restrict this class - you don't want real attendance
 * history disappearing after a day!
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataCleanupService {

    private final UserRepository userRepository;
    private final FaceDataRepository faceDataRepository;
    private final AttendanceRepository attendanceRepository;

    // Runs every hour; deletes any account older than 24 hours along with its data
    @Scheduled(fixedRate = 60 * 60 * 1000)
    @Transactional
    public void deleteExpiredAccounts() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        List<User> expiredUsers = userRepository.findByCreatedAtBefore(cutoff);

        if (expiredUsers.isEmpty()) {
            return;
        }

        for (User user : expiredUsers) {
            faceDataRepository.deleteByUserId(user.getId());
            attendanceRepository.deleteByUserId(user.getId());
        }
        userRepository.deleteAll(expiredUsers);

        log.info("Demo data cleanup: removed {} account(s) older than 24 hours", expiredUsers.size());
    }
}
