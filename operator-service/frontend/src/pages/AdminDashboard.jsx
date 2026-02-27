import { useMemo, useState, useEffect } from 'react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';
import { mockStats } from '../data/mock.js';
import { adminApi } from '../api/client.js';
import { useAuth } from '../context/AuthContext';

const palette = ['#28c4a1', '#f59e0b', '#60a5fa', '#ef4444'];

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
      alert('Failed to load users');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteUser = async (userId, username) => {
    if (!confirm(`Are you sure you want to delete user "${username}"?`)) {
      return;
    }

    try {
      await adminApi.deleteUser(userId);
      setUsers(users.filter(u => u.id !== userId));
      alert('User deleted successfully');
    } catch (error) {
      console.error('Failed to delete user:', error);
      alert(error.response?.data?.message || 'Failed to delete user');
    }
  };

  const handleToggleActive = async (userId) => {
    try {
      const updatedUser = await adminApi.toggleUserActive(userId);
      setUsers(users.map(u => u.id === userId ? updatedUser : u));
      alert(`User ${updatedUser.active ? 'activated' : 'deactivated'} successfully`);
    } catch (error) {
      console.error('Failed to toggle user status:', error);
      alert(error.response?.data?.message || 'Failed to toggle user status');
    }
  };

  const totals = useMemo(
    () => [
      { label: 'Total', value: mockStats.total },
      { label: 'Pending', value: mockStats.pending },
      { label: 'Approved', value: mockStats.approved },
      { label: 'Edited', value: mockStats.edited }
    ],
    []
  );

  const handleCreateUser = (payload) => {
    // After creating, reload users from API
    loadUsers();
    setShowModal(false);
  };

  return (
    <div className="grid" style={{ gap: 20 }}>
      <div className="grid cols-4">
        {totals.map((item) => (
          <div key={item.label} className="card">
            <div className="label">{item.label}</div>
            <div style={{ fontSize: 28, fontWeight: 700 }}>{item.value}</div>
          </div>
        ))}
      </div>

      <div className="grid cols-2">
        <div className="card">
          <h3 style={{ marginTop: 0 }}>Requests by status</h3>
          <div style={{ height: 260 }}>
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={mockStats.byStatus} dataKey="value" nameKey="name" innerRadius={60} outerRadius={100}>
                  {mockStats.byStatus.map((entry, index) => (
                    <Cell key={entry.name} fill={palette[index % palette.length]} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="card">
          <h3 style={{ marginTop: 0 }}>Requests by category</h3>
          <div style={{ height: 260 }}>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={mockStats.byCategory}>
                <XAxis dataKey="name" stroke="#b6c2d0" />
                <YAxis stroke="#b6c2d0" />
                <Tooltip />
                <Bar dataKey="value" fill="#28c4a1" radius={[8, 8, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h3 style={{ margin: 0 }}>Users</h3>
          <button className="button" onClick={() => setShowModal(true)}>
            Create user
          </button>
        </div>
        <table className="table" style={{ marginTop: 12 }}>
          <thead>
            <tr>
              <th>Username</th>
              <th>Full Name</th>
              <th>Role</th>
              <th>Status</th>
              <th>Actions</th>
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
                      SUPER ADMIN
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
                    {user.active ? 'Active' : 'Inactive'}
                  </span>
                </td>
                <td>
                  <div style={{ display: 'flex', gap: 8 }}>
                    <button 
                      className="button secondary"
                      onClick={() => handleToggleActive(user.id)}
                      style={{ fontSize: 12, padding: '4px 12px' }}
                    >
                      {user.active ? 'Deactivate' : 'Activate'}
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
                        Delete
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
      alert('Please fill in all fields');
      return;
    }
    
    if (form.password.length < 6) {
      alert('Password must be at least 6 characters');
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
      alert('User created successfully');
      onCreate(form);
    } catch (error) {
      console.error('Failed to create user:', error);
      alert(error.response?.data?.message || 'Failed to create user');
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
        <h3 style={{ marginTop: 0 }}>Create User</h3>
        <form onSubmit={handleSubmit} style={{ display: 'grid', gap: 12 }}>
          <div>
            <label className="label">Username</label>
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
            <label className="label">Temporary Password</label>
            <input
              type="password"
              className="input"
              value={form.password}
              onChange={(event) => setForm({ ...form, password: event.target.value })}
              placeholder="Min 6 characters"
              minLength={6}
              disabled={loading}
              required
            />
          </div>
          <div>
            <label className="label">Full Name</label>
            <input
              className="input"
              value={form.fullName}
              onChange={(event) => setForm({ ...form, fullName: event.target.value })}
              placeholder="John Doe"
              disabled={loading}
              required
            />
          </div>
          <div>
            <label className="label">Role</label>
            <select
              className="select"
              value={form.role}
              onChange={(event) => setForm({ ...form, role: event.target.value })}
              disabled={loading}
            >
              <option value="ROLE_OPERATOR">Operator</option>
              <option value="ROLE_ADMIN">Admin</option>
            </select>
          </div>
          <div style={{ display: 'flex', gap: 12, marginTop: 8 }}>
            <button 
              className="button" 
              type="submit"
              disabled={loading}
            >
              {loading ? 'Creating...' : 'Create User'}
            </button>
            <button 
              className="button secondary" 
              type="button" 
              onClick={onClose}
              disabled={loading}
            >
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
