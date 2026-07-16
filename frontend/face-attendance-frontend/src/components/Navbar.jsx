import { Link, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import '../styles/navbar.css';

export default function Navbar() {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);

  function handleLogout() {
    logout();
    setMenuOpen(false);
    navigate('/');
  }

  return (
    <header className="navbar">
      <Link to="/" className="navbar-brand">
        <span className="navbar-brand-dot" />
        Face Attendance
      </Link>

      <nav className="navbar-actions">
        {!isAuthenticated && (
          <>
            <Link to="/login" className="nav-btn nav-btn-ghost">
              Log in
            </Link>
            <Link to="/signup" className="nav-btn nav-btn-solid">
              Sign up
            </Link>
          </>
        )}

        {isAuthenticated && (
          <>
            <Link to="/kiosk" className="nav-btn nav-btn-solid">
              Mark Attendance
            </Link>
            <div className="navbar-user" onClick={() => setMenuOpen((v) => !v)}>
              <span className="navbar-avatar">{(user?.name || user?.email || '?')[0].toUpperCase()}</span>
              {menuOpen && (
                <div className="navbar-dropdown">
                  <div className="navbar-dropdown-name">{user?.name || user?.email}</div>
                  <button className="navbar-dropdown-item" onClick={handleLogout}>
                    Log out
                  </button>
                </div>
              )}
            </div>
          </>
        )}
      </nav>
    </header>
  );
}
