import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { useAuth } from '../context/AuthContext';
import { completeProfile } from '../api/userApi';

export default function CompleteProfilePage() {
  const navigate = useNavigate();
  const { updateUser } = useAuth();

  const [form, setForm] = useState({ name: '', age: '', department: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  function update(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');

    if (!form.name.trim() || !form.age) {
      setError('Please fill in your name and age to continue');
      return;
    }

    setLoading(true);
    try {
      const updated = await completeProfile({
        name: form.name.trim(),
        age: Number(form.age),
        department: form.department.trim()
      });
      updateUser(updated);
      navigate('/face-register');
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
          <div className="eyebrow">Step 1 of 2</div>
          <h1 className="card-title">Tell us about you</h1>
          <p className="card-subtitle">
            A few basic details. Next, we'll set up your face for quick check-ins.
          </p>

          <form onSubmit={handleSubmit}>
            <div className="field">
              <label htmlFor="name">Full name</label>
              <input
                id="name"
                type="text"
                placeholder="Jane Doe"
                value={form.name}
                onChange={(e) => update('name', e.target.value)}
                autoFocus
              />
            </div>

            <div className="field">
              <label htmlFor="age">Age</label>
              <input
                id="age"
                type="number"
                placeholder="21"
                min="1"
                value={form.age}
                onChange={(e) => update('age', e.target.value)}
              />
            </div>

            <div className="field">
              <label htmlFor="department">Class / Department (optional)</label>
              <input
                id="department"
                type="text"
                placeholder="e.g. Computer Engineering - TE"
                value={form.department}
                onChange={(e) => update('department', e.target.value)}
              />
            </div>

            {error && <div className="banner banner-error">{error}</div>}

            <button className="btn btn-primary" type="submit" disabled={loading}>
              {loading ? 'Saving…' : 'Continue to face setup'}
            </button>
          </form>
        </div>
      </div>
    </>
  );
}
