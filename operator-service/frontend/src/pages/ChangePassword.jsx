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
      setError('Заполните оба поля пароля');
      return;
    }

    if (form.newPassword !== form.confirmPassword) {
      setError('Пароли не совпадают');
      return;
    }

    if (form.newPassword.length < 6) {
      setError('Пароль должен быть не менее 6 символов');
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
      setError(err.response?.data?.message || 'Не удалось сменить пароль');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-shell">
      <div className="card login-card">
        <div className="hero">
          <div className="tag">Первый вход</div>
          <h2 style={{ fontSize: 26, margin: 0 }}>Установите новый пароль</h2>
          <p style={{ color: 'var(--ink-soft)', margin: 0 }}>
            Для безопасности измените временный пароль, выданный администратором.
          </p>
        </div>

        <form onSubmit={handleSubmit} style={{ marginTop: 24 }}>
          <label className="label">Новый пароль</label>
          <input
            type="password"
            className="input"
            value={form.newPassword}
            onChange={(event) => setForm({ ...form, newPassword: event.target.value })}
            placeholder="Введите новый пароль"
            minLength={6}
            disabled={loading}
          />

          <div style={{ marginTop: 16 }}>
            <label className="label">Подтвердите новый пароль</label>
            <input
              type="password"
              className="input"
              value={form.confirmPassword}
              onChange={(event) => setForm({ ...form, confirmPassword: event.target.value })}
              placeholder="Подтвердите новый пароль"
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
            {loading ? 'Обновление...' : 'Обновить пароль'}
          </button>
        </form>
      </div>
    </div>
  );
}
