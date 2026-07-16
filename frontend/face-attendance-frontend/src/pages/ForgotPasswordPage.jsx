import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { forgotPassword } from '../api/authApi';

export default function ForgotPasswordPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [token, setToken] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');

    if (!email.trim()) {
      setError('Enter your email');
      return;
    }

    setLoading(true);
    try {
      const resetToken = await forgotPassword(email.trim());
      setToken(resetToken);
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
          <div className="eyebrow">Reset password</div>
          <h1 className="card-title">Forgot your password?</h1>
          <p className="card-subtitle">Enter your email and we'll generate a reset link.</p>

          {!token && (
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

              {error && <div className="banner banner-error">{error}</div>}

              <button className="btn btn-primary" type="submit" disabled={loading}>
                {loading ? 'Generating…' : 'Send reset link'}
              </button>
            </form>
          )}

          {token && (
            <>
              <div className="banner banner-info mt-24">
                <strong>Demo mode:</strong> there's no email service wired up in this
                portfolio project, so here's your reset token directly instead of an
                emailed link.
              </div>
              <button
                className="btn btn-primary"
                onClick={() => navigate('/reset-password', { state: { token } })}
              >
                Continue to reset password
              </button>
            </>
          )}

          <div className="text-center mt-24">
            <Link to="/login" className="link-btn">
              Back to login
            </Link>
          </div>
        </div>
      </div>
    </>
  );
}
