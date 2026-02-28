import { useState, useEffect } from 'react';
import { adminApi } from '../api/client.js';
import { useAuth } from '../context/AuthContext';

export default function AdminDashboard() {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState([]);
  const [showModal, setShowModal] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadUsers();
  }, []);

  const loadUsers = async () => {
    try {
      setLoading(true);
      const loadedUsers = await adminApi.getUsers();
      setUsers(loadedUsers);
    } catch (error) {
      console.error('Failed to load users:', error);
      alert('Не удалось загрузить пользователей');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteUser = async (userId, username) => {
    if (!confirm(`Вы уверены, что хотите удалить пользователя "${username}"?`)) {
      return;
    }

    try {
      await adminApi.deleteUser(userId);
      setUsers(users.filter(u => u.id !== userId));
      alert('Пользователь удалён успешно');
    } catch (error) {
      console.error('Failed to delete user:', error);
      alert(error.response?.data?.message || 'Не удалось удалить пользователя');
    }
  };

  const handleToggleActive = async (userId) => {
    try {
      const updatedUser = await adminApi.toggleUserActive(userId);
      setUsers(users.map(u => u.id === userId ? updatedUser : u));
      alert(`Пользователь ${updatedUser.active ? 'активирован' : 'деактивирован'} успешно`);
    } catch (error) {
      console.error('Failed to toggle user status:', error);
      alert(error.response?.data?.message || 'Не удалось изменить статус пользователя');
    }
  };

  const handleCreateUser = (payload) => {
    // After creating, reload users from API
    loadUsers();
    setShowModal(false);
  };

  return (
    <div className="grid" style={{ gap: 20 }}>

      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h3 style={{ margin: 0 }}>Пользователи</h3>
          <button className="button" onClick={() => setShowModal(true)}>
            Создать пользователя
          </button>
        </div>
        <table className="table" style={{ marginTop: 12 }}>
          <thead>
            <tr>
              <th>Имя пользователя</th>
              <th>Полное имя</th>
              <th>Роль</th>
              <th>Статус</th>
              <th>Действия</th>
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id}>
                <td>
                  {user.username}
                  {user.superAdmin && (
                    <span style={{ 
                      marginLeft: 8, 
                      fontSize: 11, 
                      padding: '2px 6px', 
                      background: 'var(--accent)', 
                      borderRadius: 4,
                      color: 'var(--bg)'
                    }}>
                      СУПЕР АДМИН
                    </span>
                  )}
                </td>
                <td>{user.fullName}</td>
                <td>{user.role}</td>
                <td>
                  <span style={{ 
                    color: user.active ? 'var(--accent)' : 'var(--danger)',
                    fontSize: 13
                  }}>
                    {user.active ? 'Активен' : 'Неактивен'}
                  </span>
                </td>
                <td>
                  <div style={{ display: 'flex', gap: 8 }}>
                    <button 
                      className="button secondary"
                      onClick={() => handleToggleActive(user.id)}
                      style={{ fontSize: 12, padding: '4px 12px' }}
                    >
                      {user.active ? 'Деактивировать' : 'Активировать'}
                    </button>
                    {!user.superAdmin && (
                      currentUser?.superAdmin || user.role !== 'ROLE_ADMIN'
                    ) && (
                      <button 
                        className="button"
                        onClick={() => handleDeleteUser(user.id, user.username)}
                        style={{ 
                          fontSize: 12, 
                          padding: '4px 12px',
                          background: 'var(--danger)'
                        }}
                      >
                        Удалить
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {showModal && <CreateUserModal onClose={() => setShowModal(false)} onCreate={handleCreateUser} />}
    </div>
  );
}

function CreateUserModal({ onClose, onCreate }) {
  const [form, setForm] = useState({
    username: '',
    password: '',
    fullName: '',
    role: 'ROLE_OPERATOR'
  });
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!form.username || !form.password || !form.fullName) {
      alert('Пожалуйста, заполните все поля');
      return;
    }
    
    if (form.password.length < 6) {
      alert('Пароль должен быть не менее 6 символов');
      return;
    }

    setLoading(true);
    try {
      await adminApi.createUser({
        username: form.username,
        password: form.password,
        fullName: form.fullName,
        role: form.role
      });
      alert('Пользователь создан успешно');
      onCreate(form);
    } catch (error) {
      console.error('Failed to create user:', error);
      alert(error.response?.data?.message || 'Не удалось создать пользователя');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(10, 14, 18, 0.75)',
        display: 'grid',
        placeItems: 'center',
        zIndex: 1000
      }}
      onClick={onClose}
    >
      <div 
        className="card" 
        style={{ width: 400 }}
        onClick={(e) => e.stopPropagation()}
      >
        <h3 style={{ marginTop: 0 }}>Создать пользователя</h3>
        <form onSubmit={handleSubmit} style={{ display: 'grid', gap: 12 }}>
          <div>
            <label className="label">Имя пользователя</label>
            <input
              className="input"
              value={form.username}
              onChange={(event) => setForm({ ...form, username: event.target.value })}
              placeholder="operator1"
              disabled={loading}
              required
            />
          </div>
          <div>
            <label className="label">Временный пароль</label>
            <input
              type="password"
              className="input"
              value={form.password}
              onChange={(event) => setForm({ ...form, password: event.target.value })}
              placeholder="Минимум 6 символов"
              minLength={6}
              disabled={loading}
              required
            />
          </div>
          <div>
            <label className="label">Полное имя</label>
            <input
              className="input"
              value={form.fullName}
              onChange={(event) => setForm({ ...form, fullName: event.target.value })}
              placeholder="Иван Иванов"
              disabled={loading}
              required
            />
          </div>
          <div>
            <label className="label">Роль</label>
            <select
              className="select"
              value={form.role}
              onChange={(event) => setForm({ ...form, role: event.target.value })}
              disabled={loading}
            >
              <option value="ROLE_OPERATOR">Оператор</option>
              <option value="ROLE_ADMIN">Администратор</option>
            </select>
          </div>
          <div style={{ display: 'flex', gap: 12, marginTop: 8 }}>
            <button 
              className="button" 
              type="submit"
              disabled={loading}
            >
              {loading ? 'Создание...' : 'Создать пользователя'}
            </button>
            <button 
              className="button secondary" 
              type="button" 
              onClick={onClose}
              disabled={loading}
            >
              Отмена
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
