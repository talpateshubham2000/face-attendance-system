import '../styles/viewfinder.css';

const RADIUS = 15;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;

/**
 * The signature visual element of the whole app: a camera-viewfinder style
 * frame (corner brackets, not a plain square) that sits over the live video.
 *
 * status: 'idle' | 'adjust' | 'good'
 * holdProgress: 0 -> 1, fills the small ring while a "good" lock is held
 * message: short instruction text shown under the frame
 */
export default function ViewfinderFrame({ videoRef, status = 'idle', holdProgress = 0, message }) {
  const boxClass = `bracket-box ${status === 'good' ? 'good' : status === 'adjust' ? 'adjust' : ''}`;
  const pillClass = `status-pill state-${status}`;

  const dashOffset = CIRCUMFERENCE * (1 - holdProgress);

  return (
    <div>
      <div className="viewfinder-wrap">
        <video ref={videoRef} className="viewfinder-video" autoPlay playsInline muted />

        <div className="viewfinder-overlay">
          <div className="scan-line" />
          <div className={boxClass}>
            <span className="corner tl" />
            <span className="corner tr" />
            <span className="corner bl" />
            <span className="corner br" />
          </div>

          {status === 'good' && holdProgress > 0 && (
            <div className="hold-ring">
              <svg width="34" height="34">
                <circle className="track" cx="17" cy="17" r={RADIUS} />
                <circle
                  className="progress"
                  cx="17"
                  cy="17"
                  r={RADIUS}
                  strokeDasharray={CIRCUMFERENCE}
                  strokeDashoffset={dashOffset}
                />
              </svg>
            </div>
          )}
        </div>
      </div>

      <div className={pillClass}>
        <span className="dot-indicator" />
        <span>{message}</span>
      </div>
    </div>
  );
}
