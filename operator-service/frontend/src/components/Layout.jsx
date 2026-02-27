import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { LayoutDashboard, Users, BookOpen, LogOut } from 'lucide-react';

export default function Layout({ children }) {
  const { user, isAdmin, logout } = useAuth();
  const navigate = useNavigate();

  const navItems = [
    { path: '/dashboard', label: 'Dashboard', icon: LayoutDashboard, adminOnly: false },
    { path: '/admin', label: 'Admin', icon: Users, adminOnly: true },
    { path: '/knowledge-base', label: 'Knowledge Base', icon: BookOpen, adminOnly: false }
  ];

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <h1>Operator Console</h1>
        <div className="tagline">AI support workflows</div>
        <nav className="nav">
          {navItems.map((item) => {
            if (item.adminOnly && !isAdmin) return null;
            const Icon = item.icon;
            return (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) => (isActive ? 'active' : '')}
              >
                <Icon size={18} />
                {item.label}
              </NavLink>
            );
          })}
        </nav>

        <div style={{ marginTop: 'auto', paddingTop: 24 }}>
          <div style={{ fontSize: 14, color: 'var(--ink-soft)' }}>Signed in as</div>
          <div style={{ fontSize: 16, fontWeight: 600 }}>{user?.name || user?.username}</div>
          <div style={{ fontSize: 12, fontFamily: 'var(--mono)', color: 'var(--accent)' }}>
            {user?.role}
          </div>
          <button
            className="button secondary"
            style={{ marginTop: 12, width: '100%' }}
            onClick={handleLogout}
          >
            <LogOut size={16} /> Logout
          </button>
        </div>
      </aside>
      <main className="content">{children}</main>
    </div>
  );
}
