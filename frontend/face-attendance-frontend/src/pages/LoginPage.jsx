import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { useAuth } from '../context/AuthContext';
import { login as loginApi } from '../api/authApi';

export default function LoginPage() {
  const navigate = useNavigate();
  const { saveSession } = useAuth();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');

    if (!email.trim() || !password) {
      setError('Enter your email and password');
      return;
    }

    setLoading(true);
    try {
      const authResponse = await loginApi(email.trim(), password);
      saveSession(authResponse);

      if (!authResponse.profileCompleted) {
        navigate('/complete-profile');
      } else if (!authResponse.faceRegistered) {
        navigate('/face-register');
      } else {
        navigate('/');
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <>
      <Navbar />
      <div className="app-shell">
        <div className="card">
          <div className="eyebrow">Sign in</div>
          <h1 className="card-title">Welcome back</h1>
          <p className="card-subtitle">Log in to check your attendance or register your face.</p>

          <form onSubmit={handleSubmit}>
            <div className="field">
              <label htmlFor="email">Email</label>
              <input
                id="email"
                type="email"
                placeholder="you@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoFocus
              />
            </div>

            <div className="field">
              <label htmlFor="password">Password</label>
              <input
                id="password"
                type="password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>

            {error && <div className="banner banner-error">{error}</div>}

            <button className="btn btn-primary" type="submit" disabled={loading}>
              {loading ? 'Logging in…' : 'Log in'}
            </button>
          </form>

          <div className="text-center mt-24">
            <Link to="/forgot-password" className="link-btn">
              Forgot password?
            </Link>
          </div>
          <div className="text-center mt-24">
            <span style={{ color: 'var(--text-muted)', fontSize: 13.5 }}>New here? </span>
            <Link to="/signup" className="link-btn">
              Create an account
            </Link>
          </div>
        </div>
      </div>
    </>
  );
}
