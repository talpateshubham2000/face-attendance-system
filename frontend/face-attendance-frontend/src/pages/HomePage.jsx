import { Link } from 'react-router-dom';
import Navbar from '../components/Navbar';
import ParticleNetworkBackground from '../components/ParticleNetworkBackground';
import { useAuth } from '../context/AuthContext';
import { useScrollReveal } from '../hooks/useScrollReveal';
import '../styles/homepage.css';

const STEPS = [
  {
    title: 'Create an account',
    desc: 'Sign up with just an email and password - takes a few seconds.'
  },
  {
    title: 'Register your face',
    desc: 'A guided 6-step camera check (blink, turn, smile) makes sure it\u2019s really you, live.'
  },
  {
    title: 'Walk up to the camera',
    desc: 'Stand in front of any screen running this system - no card, no app to open.'
  },
  {
    title: 'Attendance marked instantly',
    desc: 'Recognized in under a second, with the exact date and time logged automatically.'
  }
];

const USE_CASES = [
  {
    icon: '\u{1F3EB}',
    title: 'Classrooms',
    desc: 'One camera at the door - no more manual roll calls or proxy attendance.'
  },
  {
    icon: '\u{1F3E2}',
    title: 'Offices',
    desc: 'Employees check in by simply walking past a desk-mounted screen.'
  },
  {
    icon: '\u{1F3AB}',
    title: 'Events & Workshops',
    desc: 'Fast, contactless check-in for one-off sessions and seminars.'
  }
];

const TECH_STACK = [
  'React',
  'Spring Boot',
  'Spring Security (JWT)',
  'FastAPI',
  'OpenCV',
  'Mediapipe',
  'MySQL'
];

export default function HomePage() {
  const { isAuthenticated, user } = useAuth();
  const revealRef = useScrollReveal();

  return (
    <div className="home-wrap" ref={revealRef}>
      <Navbar />

      <section className="hero">
        <ParticleNetworkBackground />
        <div className="hero-badge">
          <span className="pulse-dot" />
          Live face-recognition attendance
        </div>
        <h1>
          Attendance that marks <span className="accent-text">itself</span>.
        </h1>
        <p className="hero-subtitle">
          Stand in front of a camera. That's it. No cards, no app taps, no
          roll calls - just walk up and you're checked in, automatically
          timestamped.
        </p>
        <div className="hero-cta-row">
          {!isAuthenticated && (
            <>
              <Link to="/signup" className="hero-cta-primary">
                Get started free
              </Link>
              <Link to="/login" className="hero-cta-secondary">
                I already have an account
              </Link>
            </>
          )}
          {isAuthenticated && (
            <Link to={user?.faceRegistered ? '/kiosk' : '/face-register'} className="hero-cta-primary">
              {user?.faceRegistered ? 'Mark attendance' : 'Finish setting up your face'}
            </Link>
          )}
        </div>
      </section>

      <section className="section">
        <div className="section-label reveal">How it works</div>
        <h2 className="section-title reveal">From sign-up to check-in in under two minutes</h2>
        <div className="steps-grid">
          {STEPS.map((step, i) => (
            <div className="step-card reveal" key={step.title} style={{ transitionDelay: `${i * 80}ms` }}>
              <div className="step-number">{String(i + 1).padStart(2, '0')}</div>
              <h3>{step.title}</h3>
              <p>{step.desc}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="section">
        <div className="section-label reveal">Where it fits</div>
        <h2 className="section-title reveal">Built for any place people check in every day</h2>
        <div className="usecase-grid">
          {USE_CASES.map((uc, i) => (
            <div className="usecase-card reveal" key={uc.title} style={{ transitionDelay: `${i * 80}ms` }}>
              <div className="usecase-icon">{uc.icon}</div>
              <h3>{uc.title}</h3>
              <p>{uc.desc}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="section">
        <div className="section-label reveal">Under the hood</div>
        <h2 className="section-title reveal">A proper 3-tier architecture, not a script</h2>
        <div className="tech-row reveal">
          {TECH_STACK.map((tech) => (
            <span className="tech-pill" key={tech}>
              {tech}
            </span>
          ))}
        </div>
      </section>

      <section className="section">
        <div className="privacy-banner reveal">
          🔒 This is a public portfolio demo. Any account you create - along
          with its face data and attendance records - is automatically and
          permanently deleted 24 hours after signup.
        </div>
      </section>

      <footer className="site-footer">
        Built as a portfolio project · React · Spring Boot · FastAPI
      </footer>
    </div>
  );
}
