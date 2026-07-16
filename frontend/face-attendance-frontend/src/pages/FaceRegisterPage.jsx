import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import ViewfinderFrame from '../components/ViewfinderFrame';
import { useCamera } from '../hooks/useCamera';
import { useAuth } from '../context/AuthContext';
import { checkLiveness, registerFace } from '../api/faceApi';

const HOLD_DURATION_MS = 3000;
const POLL_INTERVAL_MS = 800;

// The 6 guided steps - each must be held continuously for 3 seconds.
// Mixing hold-still / blink / turns / smile makes it very hard to fake
// with a static photo held up to the camera.
const STEPS = [
  { challenge: 'HOLD_STILL', instruction: 'Look straight ahead' },
  { challenge: 'BLINK', instruction: 'Close your eyes for a moment' },
  { challenge: 'TURN_LEFT', instruction: 'Turn your head to the left' },
  { challenge: 'TURN_RIGHT', instruction: 'Turn your head to the right' },
  { challenge: 'SMILE', instruction: 'Give us a smile' },
  { challenge: 'HOLD_STILL', instruction: 'Look straight ahead, one more time' }
];

export default function FaceRegisterPage() {
  const navigate = useNavigate();
  const { updateUser } = useAuth();
  const { videoRef, isReady, error: cameraError, captureFrame } = useCamera();

  const [stepIndex, setStepIndex] = useState(0);
  const [images, setImages] = useState([]);
  const [frameStatus, setFrameStatus] = useState('idle');
  const [holdProgress, setHoldProgress] = useState(0);
  const [liveMessage, setLiveMessage] = useState('Starting camera…');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const holdStartRef = useRef(null);
  const capturingRef = useRef(false); // guards against double-capture while awaiting

  const currentStep = STEPS[stepIndex];
  const isDone = stepIndex >= STEPS.length;

  useEffect(() => {
    if (!isReady || isDone || submitting) return;

    const interval = setInterval(async () => {
      if (capturingRef.current) return;

      const frame = captureFrame();
      if (!frame) return;

      try {
        const result = await checkLiveness(frame, currentStep.challenge);

        setFrameStatus(result.status === 'GOOD' ? 'good' : 'adjust');

        if (result.status !== 'GOOD') {
          holdStartRef.current = null;
          setHoldProgress(0);
          setLiveMessage(result.message);
          return;
        }

        if (!result.challengePassed) {
          holdStartRef.current = null;
          setHoldProgress(0);
          setLiveMessage(result.message || currentStep.instruction);
          return;
        }

        // Challenge currently satisfied - start/continue the 3-second hold
        if (holdStartRef.current === null) {
          holdStartRef.current = Date.now();
        }
        const elapsed = Date.now() - holdStartRef.current;
        const progress = Math.min(1, elapsed / HOLD_DURATION_MS);
        setHoldProgress(progress);
        setLiveMessage(elapsed >= HOLD_DURATION_MS ? 'Captured!' : 'Hold that…');

        if (elapsed >= HOLD_DURATION_MS) {
          capturingRef.current = true;
          setImages((prev) => {
            const next = [...prev, frame];

            if (next.length >= STEPS.length) {
              submitAllPhotos(next);
            } else {
              holdStartRef.current = null;
              setHoldProgress(0);
              setStepIndex((i) => i + 1);
              capturingRef.current = false;
            }
            return next;
          });
        }
      } catch (err) {
        setFrameStatus('adjust');
        setLiveMessage('Cannot reach face service. Check Python service is running.');
      }
    }, POLL_INTERVAL_MS);

    return () => clearInterval(interval);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isReady, stepIndex, isDone, submitting]);

  async function submitAllPhotos(photos) {
    setSubmitting(true);
    setError('');
    try {
      await registerFace(photos);
      updateUser({ faceRegistered: true });
      navigate('/');
    } catch (err) {
      setError(err.message);
      setSubmitting(false);
      // let them retry the whole sequence on failure
      setImages([]);
      setStepIndex(0);
      holdStartRef.current = null;
      setHoldProgress(0);
      capturingRef.current = false;
    }
  }

  return (
    <>
      <Navbar />
      <div className="app-shell">
        <div className="card" style={{ maxWidth: 480 }}>
          <div className="eyebrow">Step 2 of 2</div>
          <h1 className="card-title">Set up your face</h1>
          <p className="card-subtitle">
            {submitting
              ? 'Almost done - registering your face…'
              : `Follow the instruction below. Step ${Math.min(stepIndex + 1, STEPS.length)} of ${STEPS.length}.`}
          </p>

          {cameraError && <div className="banner banner-error">{cameraError}</div>}

          {!cameraError && (
            <div className="mt-24">
              <ViewfinderFrame
                videoRef={videoRef}
                status={submitting ? 'good' : frameStatus}
                holdProgress={submitting ? 1 : holdProgress}
                message={submitting ? 'Registering your face…' : currentStep ? currentStep.instruction : 'Done'}
              />
            </div>
          )}

          {!submitting && (
            <div className="status-pill state-idle text-center" style={{ marginTop: 10 }}>
              {liveMessage}
            </div>
          )}

          <div className="dot-row">
            {STEPS.map((_, i) => (
              <span key={i} className={`dot ${i < images.length ? 'filled' : ''}`} />
            ))}
          </div>

          {error && <div className="banner banner-error">{error}</div>}
        </div>
      </div>
    </>
  );
}
