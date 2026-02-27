import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { authApi } from '../api/client';

export default function Login() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [form, setForm] = useState({ username: '', password: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [checkingSystem, setCheckingSystem] = useState(false);

  useEffect(() => {
    setCheckingSystem(false);
  }, []);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');

    if (!form.username || !form.password) {
      setError('Enter username and password');
      return;
    }

    setLoading(true);
    try {
      const response = await authApi.login({
        username: form.username,
        password: form.password
      });

      login({
        token: response.token,
        user: response.user
      });

      if (response.user.firstLogin) {
        navigate('/change-password');
      } else {
        navigate('/dashboard');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid credentials');
    } finally {
      setLoading(false);
    }
  };

  if (checkingSystem) {
    return (
      <div className="login-shell">
        <div className="card login-card">
          <div className="hero" style={{ textAlign: 'center' }}>
            <p>Checking system status...</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="login-shell">
      <div className="card login-card">
        <div className="hero">
          <div className="tag">Operator Console</div>
          <h2 style={{ fontSize: 28, margin: 0 }}>Sign in</h2>
          <p style={{ color: 'var(--ink-soft)', margin: 0 }}>
            Access is provisioned by admins only. No public registration.
          </p>
        </div>

        <form onSubmit={handleSubmit} style={{ marginTop: 24 }}>
          <label className="label">Username</label>
          <input
            className="input"
            value={form.username}
            onChange={(event) => setForm({ ...form, username: event.target.value })}
            placeholder="operator01"
            disabled={loading}
          />

          <div style={{ marginTop: 16 }}>
            <label className="label">Password</label>
            <input
              type="password"
              className="input"
              value={form.password}
              onChange={(event) => setForm({ ...form, password: event.target.value })}
              placeholder="Enter temporary password"
              disabled={loading}
            />
          </div>

          {error && (
            <div style={{ marginTop: 12, color: 'var(--danger)' }}>{error}</div>
          )}

          <button 
            className="button" 
            style={{ marginTop: 20, width: '100%' }}
            disabled={loading}
          >
            {loading ? 'Signing in...' : 'Continue'}
          </button>
        </form>
      </div>
    </div>
  );
}
