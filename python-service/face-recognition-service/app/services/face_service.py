import os
import threading

import cv2
import mediapipe as mp
import numpy as np
import onnxruntime as ort
from app.schemas import EmbeddingResponse, CheckResponse, LivenessCheckResponse
from app.utils.image_utils import decode_base64_image

# Initialize Mediapipe Face Detection (for position check - bounding box only)
mp_face_detection = mp.solutions.face_detection
face_detection = mp_face_detection.FaceDetection(model_selection=1, min_detection_confidence=0.5)

# Initialize Mediapipe Face Mesh (for embedding + liveness - 468 landmark points)
mp_face_mesh = mp.solutions.face_mesh
face_mesh = mp_face_mesh.FaceMesh(
    static_image_mode=True,
    max_num_faces=1,
    refine_landmarks=False,
    min_detection_confidence=0.5
)

# Mediapipe's solution objects are NOT thread-safe. FastAPI runs each request
# in a worker thread, and the frontend polls rapidly (every ~800ms) - without
# this lock, two overlapping requests calling .process() on the same shared
# face_detection/face_mesh instance at once corrupts Mediapipe's internal
# graph state and crashes with "Packet timestamp mismatch" errors.
_mp_lock = threading.Lock()

# ---- Real face-recognition model (MobileFaceNet, ONNX, 512-d embeddings) ----
# This replaces the earlier landmark-geometry "embedding", which could not
# reliably tell different people apart (everyone's face geometry is roughly
# similar in proportion). This is a proper deep-learning identity embedding,
# trained specifically to separate different people's faces.
_MODEL_PATH = os.path.join(os.path.dirname(__file__), "..", "models", "w600k_mbf.onnx")
_onnx_session = ort.InferenceSession(_MODEL_PATH, providers=["CPUExecutionProvider"])
_ONNX_INPUT_NAME = _onnx_session.get_inputs()[0].name

# Standard 5-point ArcFace alignment template (where these 5 landmarks should
# land on a 112x112 crop) - aligning to this before embedding is what makes
# the model accurate; skipping alignment badly hurts recognition quality.
_ARCFACE_TEMPLATE = np.array([
    [38.2946, 51.6963],   # left eye
    [73.5318, 51.5014],   # right eye
    [56.0252, 71.7366],   # nose tip
    [41.5493, 92.3655],   # left mouth corner
    [70.7299, 92.2041]    # right mouth corner
], dtype=np.float32)

# ---- Position-check thresholds ----
FRAME_CENTER_TOLERANCE = 0.18
MIN_FACE_WIDTH_RATIO = 0.18
MAX_FACE_WIDTH_RATIO = 0.75

# ---- Landmark indices (standard Mediapipe Face Mesh topology) ----
NOSE_TIP = 1
LEFT_EYE_OUTER = 33
RIGHT_EYE_OUTER = 263
LEFT_CHEEK = 234   # left-most point of face silhouette
RIGHT_CHEEK = 454  # right-most point of face silhouette
MOUTH_LEFT = 61
MOUTH_RIGHT = 291

# 6-point EAR (Eye Aspect Ratio) landmark sets
RIGHT_EYE_EAR_IDX = [33, 160, 158, 133, 153, 144]
LEFT_EYE_EAR_IDX = [362, 385, 387, 263, 373, 380]

# ---- Liveness thresholds (tune these with real testing) ----
EAR_BLINK_THRESHOLD = 0.28        # below this -> eyes considered closed (mediapipe's eyelid tracking is less precise than dlib, so this needs to be more lenient)
YAW_TURN_RATIO_THRESHOLD = 1.35   # how lopsided nose-to-cheek distances must be to count as "turned"
YAW_FRONTAL_RATIO_MAX = 1.25      # how close to 1.0 the ratio must be to count as "facing forward"
SMILE_WIDTH_RATIO_THRESHOLD = 0.55  # mouth-width / inter-ocular-distance
SMILE_MOUTH_OPEN_THRESHOLD = 0.10   # mouth vertical gap / inter-ocular-distance (teeth-showing smiles)


class FaceService:

    # ================= EMBEDDING (for registration + recognition) =================

    def generate_embedding(self, base64_image: str) -> EmbeddingResponse:
        img = decode_base64_image(base64_image)
        if img is None:
            return EmbeddingResponse(faceDetected=False, embedding=None, message="Invalid image data")

        rgb_img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)

        with _mp_lock:
            results = face_detection.process(rgb_img)
        if not results.detections:
            return EmbeddingResponse(faceDetected=False, embedding=None, message="No face detected")
        if len(results.detections) > 1:
            return EmbeddingResponse(faceDetected=False, embedding=None, message="Multiple faces detected")

        with _mp_lock:
            mesh_results = face_mesh.process(rgb_img)
        if not mesh_results.multi_face_landmarks:
            return EmbeddingResponse(faceDetected=False, embedding=None, message="Could not extract face landmarks")

        landmarks = mesh_results.multi_face_landmarks[0].landmark
        embedding = self._align_and_embed(img, landmarks)

        if embedding is None:
            return EmbeddingResponse(faceDetected=False, embedding=None, message="Could not align face for recognition")

        return EmbeddingResponse(faceDetected=True, embedding=embedding, message="Face detected successfully")

    # ================= POSITION CHECK (green/red border) =================

    def check_face_position(self, base64_image: str) -> CheckResponse:
        img = decode_base64_image(base64_image)
        if img is None:
            return CheckResponse(status="ADJUST", message="Invalid image")

        h, w = img.shape[:2]
        with _mp_lock:
            results = face_detection.process(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))

        if not results.detections:
            return CheckResponse(status="ADJUST", message="No face detected")
        if len(results.detections) > 1:
            return CheckResponse(status="ADJUST", message="Multiple faces detected")

        bboxC = results.detections[0].location_data.relative_bounding_box
        left, top, face_width, face_height = bboxC.xmin * w, bboxC.ymin * h, bboxC.width * w, bboxC.height * h

        face_width_ratio = face_width / w
        if face_width_ratio < MIN_FACE_WIDTH_RATIO:
            return CheckResponse(status="ADJUST", message="Move closer")
        if face_width_ratio > MAX_FACE_WIDTH_RATIO:
            return CheckResponse(status="ADJUST", message="Move farther")

        face_center_x = left + (face_width / 2)
        face_center_y = top + (face_height / 2)
        x_offset_ratio = abs(face_center_x - (w / 2)) / w
        y_offset_ratio = abs(face_center_y - (h / 2)) / h

        if x_offset_ratio > FRAME_CENTER_TOLERANCE or y_offset_ratio > FRAME_CENTER_TOLERANCE:
            return CheckResponse(status="ADJUST", message="Center your face")

        return CheckResponse(status="GOOD", message="Face positioned correctly")

    # ================= LIVENESS CHECK (anti-spoofing) =================

    def check_liveness(self, base64_image: str, challenge: str) -> LivenessCheckResponse:
        """
        Confirms the user is actually performing the requested live action,
        rather than holding up a static photo to the camera. Used only
        during the 6-step face registration flow.
        """
        img = decode_base64_image(base64_image)
        if img is None:
            return LivenessCheckResponse(status="ADJUST", challengePassed=False, message="Invalid image")

        h, w = img.shape[:2]
        rgb_img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)

        # Step 1: basic position check first (reuse face_detection)
        with _mp_lock:
            det_results = face_detection.process(rgb_img)
        if not det_results.detections:
            return LivenessCheckResponse(status="ADJUST", challengePassed=False, message="No face detected")
        if len(det_results.detections) > 1:
            return LivenessCheckResponse(status="ADJUST", challengePassed=False, message="Multiple faces detected")

        bboxC = det_results.detections[0].location_data.relative_bounding_box
        face_width_ratio = bboxC.width

        if face_width_ratio < MIN_FACE_WIDTH_RATIO:
            return LivenessCheckResponse(status="ADJUST", challengePassed=False, message="Move closer")
        if face_width_ratio > MAX_FACE_WIDTH_RATIO:
            return LivenessCheckResponse(status="ADJUST", challengePassed=False, message="Move farther")

        # Step 2: get landmarks for the actual liveness check
        with _mp_lock:
            mesh_results = face_mesh.process(rgb_img)

        if not mesh_results.multi_face_landmarks:
            return LivenessCheckResponse(status="ADJUST", challengePassed=False, message="Could not read face detail")

        landmarks = mesh_results.multi_face_landmarks[0].landmark

        challenge = challenge.upper().strip()

        if challenge == "HOLD_STILL":
            passed, message = self._check_hold_still(landmarks)
        elif challenge == "BLINK":
            passed, message = self._check_blink(landmarks)
        elif challenge == "TURN_LEFT":
            passed, message = self._check_turn(landmarks, direction="LEFT")
        elif challenge == "TURN_RIGHT":
            passed, message = self._check_turn(landmarks, direction="RIGHT")
        elif challenge == "SMILE":
            passed, message = self._check_smile(landmarks)
        else:
            return LivenessCheckResponse(status="GOOD", challengePassed=False, message=f"Unknown challenge: {challenge}")

        return LivenessCheckResponse(status="GOOD", challengePassed=passed, message=message)

    # ================= liveness helper checks =================

    def _get_point(self, landmarks, idx):
        return np.array([landmarks[idx].x, landmarks[idx].y])

    def _eye_aspect_ratio(self, landmarks, idx_set):
        p1, p2, p3, p4, p5, p6 = [self._get_point(landmarks, i) for i in idx_set]
        vertical_1 = np.linalg.norm(p2 - p6)
        vertical_2 = np.linalg.norm(p3 - p5)
        horizontal = np.linalg.norm(p1 - p4)
        if horizontal == 0:
            return 1.0
        return (vertical_1 + vertical_2) / (2.0 * horizontal)

    def _yaw_ratio(self, landmarks):
        """Returns (left_dist, right_dist) from nose tip to each cheek edge."""
        nose = self._get_point(landmarks, NOSE_TIP)
        left_cheek = self._get_point(landmarks, LEFT_CHEEK)
        right_cheek = self._get_point(landmarks, RIGHT_CHEEK)
        left_dist = np.linalg.norm(nose - left_cheek)
        right_dist = np.linalg.norm(nose - right_cheek)
        return left_dist, right_dist

    def _check_hold_still(self, landmarks):
        left_dist, right_dist = self._yaw_ratio(landmarks)
        ratio = max(left_dist, right_dist) / max(min(left_dist, right_dist), 1e-6)

        ear = (
            self._eye_aspect_ratio(landmarks, RIGHT_EYE_EAR_IDX)
            + self._eye_aspect_ratio(landmarks, LEFT_EYE_EAR_IDX)
        ) / 2.0

        print(f"[HOLD_STILL] yaw_ratio={ratio:.3f} (max allowed {YAW_FRONTAL_RATIO_MAX}) | ear={ear:.3f} (min allowed {EAR_BLINK_THRESHOLD})")

        if ratio > YAW_FRONTAL_RATIO_MAX:
            return False, "Please face the camera directly"
        if ear < EAR_BLINK_THRESHOLD:
            return False, "Keep your eyes open"
        return True, "Hold still - looking good"

    def _check_blink(self, landmarks):
        ear = (
            self._eye_aspect_ratio(landmarks, RIGHT_EYE_EAR_IDX)
            + self._eye_aspect_ratio(landmarks, LEFT_EYE_EAR_IDX)
        ) / 2.0

        print(f"[BLINK] ear={ear:.3f} (need below {EAR_BLINK_THRESHOLD} to count as closed)")

        if ear < EAR_BLINK_THRESHOLD:
            return True, "Eyes closed - hold it"
        return False, "Please close your eyes"

    def _check_turn(self, landmarks, direction: str):
        left_dist, right_dist = self._yaw_ratio(landmarks)

        # NOTE: this is measured on the RAW (unmirrored) camera frame.
        # Since the frontend mirrors the video preview for the user (like a
        # normal mirror/webcam), "turn left/right" as instructed on-screen
        # may correspond to the opposite ratio direction here - this is
        # fine since the two turn challenges just need to be two distinct,
        # verifiably-live poses; exact left/right naming isn't safety-critical.
        if direction == "LEFT":
            ratio = right_dist / max(left_dist, 1e-6)
        else:
            ratio = left_dist / max(right_dist, 1e-6)

        print(f"[TURN_{direction}] ratio={ratio:.3f} (need above {YAW_TURN_RATIO_THRESHOLD})")

        if ratio > YAW_TURN_RATIO_THRESHOLD:
            return True, "Turn detected"
        return False, "Please turn your head further"

    def _check_smile(self, landmarks):
        mouth_left = self._get_point(landmarks, MOUTH_LEFT)
        mouth_right = self._get_point(landmarks, MOUTH_RIGHT)
        left_eye = self._get_point(landmarks, LEFT_EYE_OUTER)
        right_eye = self._get_point(landmarks, RIGHT_EYE_OUTER)
        mouth_top = self._get_point(landmarks, 13)
        mouth_bottom = self._get_point(landmarks, 14)

        mouth_width = np.linalg.norm(mouth_left - mouth_right)
        mouth_gap = np.linalg.norm(mouth_top - mouth_bottom)
        inter_ocular_distance = np.linalg.norm(left_eye - right_eye)

        if inter_ocular_distance == 0:
            return False, "Please smile"

        width_ratio = mouth_width / inter_ocular_distance
        gap_ratio = mouth_gap / inter_ocular_distance
        print(f"[SMILE] width_ratio={width_ratio:.3f} (need > {SMILE_WIDTH_RATIO_THRESHOLD}) "
              f"gap_ratio={gap_ratio:.3f} (need > {SMILE_MOUTH_OPEN_THRESHOLD})")

        # Passes if EITHER the mouth stretches wide OR opens a bit (teeth-showing
        # smiles do both) - much easier to trigger than requiring width alone.
        if width_ratio > SMILE_WIDTH_RATIO_THRESHOLD or gap_ratio > SMILE_MOUTH_OPEN_THRESHOLD:
            return True, "Smile detected"
        return False, "Please smile a bit more"

    # ================= embedding helper (real recognition) =================

    def _get_5_alignment_points(self, landmarks, w, h):
        """Extracts the 5 landmark points (both eyes, nose tip, both mouth
        corners) used to align the face before feeding it to the recognition
        model - proper alignment is what makes the embedding accurate."""

        def pt(idx):
            return np.array([landmarks[idx].x * w, landmarks[idx].y * h])

        left_eye = (pt(33) + pt(133)) / 2.0
        right_eye = (pt(362) + pt(263)) / 2.0
        nose = pt(NOSE_TIP)
        mouth_left = pt(MOUTH_LEFT)
        mouth_right = pt(MOUTH_RIGHT)

        return np.array([left_eye, right_eye, nose, mouth_left, mouth_right], dtype=np.float32)

    def _align_and_embed(self, img, landmarks):
        """
        Aligns the face to a standard 112x112 crop (using the 5-point
        ArcFace template) and runs it through MobileFaceNet (ONNX) to get a
        proper 512-dimension identity embedding. This is what makes
        recognition actually discriminate between different people, unlike
        the earlier raw-landmark-geometry approach.
        """
        h, w = img.shape[:2]
        src_pts = self._get_5_alignment_points(landmarks, w, h)

        transform_matrix, _ = cv2.estimateAffinePartial2D(
            src_pts, _ARCFACE_TEMPLATE, method=cv2.LMEDS
        )
        if transform_matrix is None:
            return None

        aligned = cv2.warpAffine(img, transform_matrix, (112, 112), borderValue=0.0)

        aligned_rgb = cv2.cvtColor(aligned, cv2.COLOR_BGR2RGB).astype(np.float32)
        normalized = (aligned_rgb / 255.0 - 0.5) / 0.5  # scale to [-1, 1], as MobileFaceNet expects
        chw = np.transpose(normalized, (2, 0, 1))
        batch = np.expand_dims(chw, axis=0)

        # Note: onnxruntime sessions ARE thread-safe for concurrent Run() calls
        # (unlike Mediapipe), so no lock is needed around this call.
        raw_embedding = _onnx_session.run(None, {_ONNX_INPUT_NAME: batch})[0][0]

        # L2-normalize so cosine similarity comparisons on the Java side behave consistently
        norm = np.linalg.norm(raw_embedding)
        if norm == 0:
            return None
        normalized_embedding = raw_embedding / norm

        return normalized_embedding.tolist()


face_service = FaceService()
