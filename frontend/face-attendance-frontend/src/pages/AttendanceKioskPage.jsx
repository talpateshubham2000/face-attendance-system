import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import ViewfinderFrame from '../components/ViewfinderFrame';
import { useCamera } from '../hooks/useCamera';
import { checkFace } from '../api/faceApi';
import { markAttendance } from '../api/attendanceApi';

const POLL_INTERVAL_MS = 900;
const HOLD_DURATION_MS = 3000;
const RESULT_DISPLAY_MS = 3500;
const INACTIVITY_TIMEOUT_MS = 2 * 60 * 1000; // 2 minutes with nobody in frame

export default function AttendanceKioskPage() {
  const navigate = useNavigate();
  const { videoRef, isReady, error: cameraError, captureFrame } = useCamera();

  // 'scanning' | 'recognizing' | 'success' | 'error'
  const [phase, setPhase] = useState('scanning');
  const [frameStatus, setFrameStatus] = useState('idle'); // idle | adjust | good
  const [holdProgress, setHoldProgress] = useState(0);
  const [liveMessage, setLiveMessage] = useState('Starting camera…');
  const [result, setResult] = useState(null); // { userName, time }
  const [errorMessage, setErrorMessage] = useState('');

  const goodStartTimeRef = useRef(null);
  const markInFlightRef = useRef(false);
  const resetTimeoutRef = useRef(null);
  const lastPresenceRef = useRef(Date.now());

  // Main polling loop - only active while phase === 'scanning'
  useEffect(() => {
    if (phase !== 'scanning' || !isReady) return;

    const interval = setInterval(async () => {
      const frame = captureFrame();
      if (!frame) return;

      try {
        const check = await checkFace(frame);
        const noOneInFrame = check.message === 'No face detected';

        if (!noOneInFrame) {
          lastPresenceRef.current = Date.now();
        } else if (Date.now() - lastPresenceRef.current > INACTIVITY_TIMEOUT_MS) {
          // Nobody has stood in front of the camera for 2 minutes - go back home
          navigate('/');
          return;
        }

        if (check.status === 'GOOD') {
          if (goodStartTimeRef.current === null) {
            goodStartTimeRef.current = Date.now();
          }
          const elapsed = Date.now() - goodStartTimeRef.current;
          const progress = Math.min(1, elapsed / HOLD_DURATION_MS);

          setFrameStatus('good');
          setHoldProgress(progress);
          setLiveMessage(elapsed >= HOLD_DURATION_MS ? 'Locking in…' : 'Hold still');

          if (elapsed >= HOLD_DURATION_MS && !markInFlightRef.current) {
            markInFlightRef.current = true;
            setPhase('recognizing');

            try {
              const attendance = await markAttendance(frame);
              setResult({
                userName: attendance.userName,
                time: attendance.time
              });
              setPhase('success');
            } catch (err) {
              setErrorMessage(err.message);
              setPhase('error');
            } finally {
              markInFlightRef.current = false;
            }
          }
        } else {
          goodStartTimeRef.current = null;
          setHoldProgress(0);
          setFrameStatus('adjust');
          setLiveMessage(check.message || 'Adjust your position');
        }
      } catch (err) {
        setFrameStatus('adjust');
        setLiveMessage('Cannot reach face service. Check Python service is running.');
      }
    }, POLL_INTERVAL_MS);

    return () => clearInterval(interval);
  }, [phase, isReady, captureFrame, navigate]);

  // After a success/error result, automatically go back to scanning for the next person
  useEffect(() => {
    if (phase === 'success' || phase === 'error') {
      resetTimeoutRef.current = setTimeout(() => {
        goodStartTimeRef.current = null;
        setHoldProgress(0);
        setFrameStatus('idle');
        setResult(null);
        setErrorMessage('');
        setPhase('scanning');
        lastPresenceRef.current = Date.now();
      }, RESULT_DISPLAY_MS);
    }
    return () => clearTimeout(resetTimeoutRef.current);
  }, [phase]);

  return (
    <>
      <Navbar />
      <div className="app-shell">
        <div className="card" style={{ maxWidth: 480 }}>
          <div className="eyebrow">Live check-in</div>
          <h1 className="card-title">Look here to check in</h1>
          <p className="card-subtitle">
            Stand in front of the camera. Once the frame turns green, hold still for 3 seconds.
          </p>

          {cameraError && <div className="banner banner-error">{cameraError}</div>}

          {!cameraError && (
            <div className="mt-24">
              <ViewfinderFrame
                videoRef={videoRef}
                status={phase === 'recognizing' ? 'good' : frameStatus}
                holdProgress={phase === 'recognizing' ? 1 : holdProgress}
                message={
                  phase === 'recognizing'
                    ? 'Marking attendance…'
                    : phase === 'success'
                    ? `You're in, ${result?.userName?.split(' ')[0] || ''} ✓`
                    : phase === 'error'
                    ? errorMessage
                    : liveMessage
                }
              />
            </div>
          )}

          {phase === 'success' && result && (
            <div className="banner banner-info">
              Attendance marked for <strong>{result.userName}</strong> at {result.time}
            </div>
          )}

          {phase === 'error' && <div className="banner banner-error">{errorMessage}</div>}

          <p className="text-center mt-24" style={{ fontSize: 12.5, color: 'var(--text-muted)' }}>
            This screen returns to the homepage automatically after 2 minutes of inactivity.
          </p>
        </div>
      </div>
    </>
  );
}
