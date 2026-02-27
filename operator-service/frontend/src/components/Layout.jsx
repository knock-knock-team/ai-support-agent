import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { LayoutDashboard, Users, BookOpen, LogOut, BarChart3 } from 'lucide-react';

export default function Layout({ children }) {
  const { user, isAdmin, logout } = useAuth();
  const navigate = useNavigate();

  const navItems = [
    { path: '/dashboard', label: 'Панель аналитики', icon: BarChart3, adminOnly: false },
    { path: '/operator', label: 'Заявки', icon: LayoutDashboard, adminOnly: false },
    { path: '/admin', label: 'Пользователи', icon: Users, adminOnly: true },
    { path: '/knowledge-base', label: 'База знаний', icon: BookOpen, adminOnly: false }
  ];

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <h1>Консоль оператора</h1>
        <div className="tagline">Рабочие процессы AI поддержки</div>
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
          <div style={{ fontSize: 14, color: 'var(--ink-soft)' }}>Вы вошли как</div>
          <div style={{ fontSize: 16, fontWeight: 600 }}>{user?.name || user?.username}</div>
          <div style={{ fontSize: 12, fontFamily: 'var(--mono)', color: 'var(--accent)' }}>
            {user?.role}
          </div>
          <button
            className="button secondary"
            style={{ marginTop: 12, width: '100%' }}
            onClick={handleLogout}
          >
            <LogOut size={16} /> Выйти
          </button>
        </div>
      </aside>
      <main className="content">{children}</main>
    </div>
  );
}
