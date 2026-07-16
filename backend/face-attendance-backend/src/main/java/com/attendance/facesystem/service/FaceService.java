package com.attendance.facesystem.service;

import com.attendance.facesystem.dto.FaceCheckResponse;
import com.attendance.facesystem.dto.LivenessCheckResponse;
import com.attendance.facesystem.dto.PythonEmbeddingResponse;
import com.attendance.facesystem.entity.FaceData;
import com.attendance.facesystem.entity.User;
import com.attendance.facesystem.exception.BadRequestException;
import com.attendance.facesystem.repository.FaceDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Handles:
 *  1. Registering a user's face (5-6 photos -> embeddings -> saved in DB)
 *  2. Live face-position check (green/red border)
 *  3. Face recognition (matching a live frame against ALL stored embeddings)
 *
 * Python only generates embeddings / detects faces. ALL matching /
 * business decisions happen HERE in Java, as intended.
 */
@Service
@RequiredArgsConstructor
public class FaceService {

    private final FaceDataRepository faceDataRepository;
    private final FaceRecognitionClient faceRecognitionClient;
    private final UserService userService;

    // Minimum cosine similarity to consider two faces "the same person".
    // Tune this value once you test with real data (typical range 0.6 - 0.85).
    @Value("${face.match.threshold:0.75}")
    private double matchThreshold;

    /**
     * Registers a user's face using multiple photos (5-6). Each photo is sent
     * to Python individually, and each resulting embedding is stored as a
     * separate FaceData row -> gives better accuracy during matching later.
     */
    public int registerFace(Long userId, List<String> images) {
        User user = userService.getUserEntityById(userId);

        if (images == null || images.size() < 3) {
            throw new BadRequestException("Please provide at least 3-5 face photos for accurate registration.");
        }

        int savedCount = 0;
        for (String image : images) {
            PythonEmbeddingResponse response = faceRecognitionClient.generateEmbedding(image);

            if (response == null || !response.isFaceDetected()) {
                continue; // skip photos where no face was clearly detected
            }

            FaceData faceData = new FaceData();
            faceData.setUser(user);
            faceData.setEmbedding(embeddingToString(response.getEmbedding()));
            faceDataRepository.save(faceData);
            savedCount++;
        }

        if (savedCount == 0) {
            throw new BadRequestException("No clear face was detected in any of the photos. Please try again.");
        }

        userService.markFaceRegistered(userId);
        return savedCount;
    }

    /**
     * Used continuously while the user stands in front of the camera, to
     * decide whether to show a green or red square border on the frontend.
     */
    public FaceCheckResponse checkFacePosition(String image) {
        FaceCheckResponse response = faceRecognitionClient.checkFacePosition(image);
        if (response == null) {
            return new FaceCheckResponse("ADJUST", "Could not reach face detection service.");
        }
        return response;
    }

    /**
     * Used only during face REGISTRATION (each of the 6 photos), to confirm
     * the user is actually performing the requested live action
     * (blink / turn / smile / hold still) rather than showing a static photo.
     */
    public LivenessCheckResponse checkLiveness(String image, String challenge) {
        LivenessCheckResponse response = faceRecognitionClient.checkLiveness(image, challenge);
        if (response == null) {
            return new LivenessCheckResponse("ADJUST", false, "Could not reach face detection service.");
        }
        return response;
    }

    /**
     * Core recognition: takes a live frame, generates its embedding via
     * Python, then compares it against every stored embedding in the DB
     * using cosine similarity. Returns the matched User if similarity
     * crosses the threshold, otherwise empty.
     */
    public Optional<User> recognizeFace(String image) {
        PythonEmbeddingResponse response = faceRecognitionClient.generateEmbedding(image);

        if (response == null || !response.isFaceDetected() || response.getEmbedding() == null) {
            return Optional.empty();
        }

        double[] liveEmbedding = response.getEmbedding().stream()
                .mapToDouble(Double::doubleValue)
                .toArray();

        List<FaceData> allFaces = faceDataRepository.findAll();

        User bestMatchUser = null;
        double bestScore = -1;

        for (FaceData faceData : allFaces) {
            double[] storedEmbedding = stringToEmbedding(faceData.getEmbedding());
            double similarity = cosineSimilarity(liveEmbedding, storedEmbedding);

            if (similarity > bestScore) {
                bestScore = similarity;
                bestMatchUser = faceData.getUser();
            }
        }

        if (bestMatchUser != null && bestScore >= matchThreshold) {
            return Optional.of(bestMatchUser);
        }
        return Optional.empty();
    }

    // ---------- helper methods ----------

    private String embeddingToString(List<Double> embedding) {
        return embedding.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private double[] stringToEmbedding(String embeddingStr) {
        String[] parts = embeddingStr.split(",");
        double[] result = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Double.parseDouble(parts[i]);
        }
        return result;
    }

    private double cosineSimilarity(double[] a, double[] b) {
        if (a.length != b.length) return -1;

        double dotProduct = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += Math.pow(a[i], 2);
            normB += Math.pow(b[i], 2);
        }
        if (normA == 0 || normB == 0) return -1;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
