import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { authApi } from '../api/client';

export default function ChangePassword() {
  const navigate = useNavigate();
  const { user, login } = useAuth();
  const [form, setForm] = useState({ newPassword: '', confirmPassword: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');

    if (!form.newPassword || !form.confirmPassword) {
      setError('Fill in both password fields');
      return;
    }

    if (form.newPassword !== form.confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    if (form.newPassword.length < 6) {
      setError('Password must be at least 6 characters');
      return;
    }

    setLoading(true);
    try {
      const response = await authApi.changePassword({
        newPassword: form.newPassword
      });

      // Update user in context
      login({
        token: localStorage.getItem('token'),
        user: response.data
      });

      navigate('/dashboard');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to change password');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-shell">
      <div className="card login-card">
        <div className="hero">
          <div className="tag">First login</div>
          <h2 style={{ fontSize: 26, margin: 0 }}>Set a new password</h2>
          <p style={{ color: 'var(--ink-soft)', margin: 0 }}>
            For security, change the temporary password issued by the admin.
          </p>
        </div>

        <form onSubmit={handleSubmit} style={{ marginTop: 24 }}>
          <label className="label">New password</label>
          <input
            type="password"
            className="input"
            value={form.newPassword}
            onChange={(event) => setForm({ ...form, newPassword: event.target.value })}
            placeholder="Enter new password"
            minLength={6}
            disabled={loading}
          />

          <div style={{ marginTop: 16 }}>
            <label className="label">Confirm new password</label>
            <input
              type="password"
              className="input"
              value={form.confirmPassword}
              onChange={(event) => setForm({ ...form, confirmPassword: event.target.value })}
              placeholder="Confirm new password"
              minLength={6}
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
            {loading ? 'Updating...' : 'Update password'}
          </button>
        </form>
      </div>
    </div>
  );
}
