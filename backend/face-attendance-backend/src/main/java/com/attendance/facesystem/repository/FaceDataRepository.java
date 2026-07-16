package com.attendance.facesystem.repository;

import com.attendance.facesystem.entity.FaceData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaceDataRepository extends JpaRepository<FaceData, Long> {

    List<FaceData> findByUserId(Long userId);

    // used by the 24-hour auto-cleanup scheduler
    void deleteByUserId(Long userId);

    // used during recognition to compare captured face against ALL registered embeddings
    List<FaceData> findAll();
}
