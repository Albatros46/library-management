import { Navigate, NavLink, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth.jsx';
import Login from './pages/Login.jsx';
import Dashboard from './pages/Dashboard.jsx';
import Members from './pages/Members.jsx';
import MemberDetail from './pages/MemberDetail.jsx';
import Books from './pages/Books.jsx';
import BookDetail from './pages/BookDetail.jsx';
import Authors from './pages/Authors.jsx';
import AuthorDetail from './pages/AuthorDetail.jsx';
import Loans from './pages/Loans.jsx';
import Overdue from './pages/Overdue.jsx';
import Notifications from './pages/Notifications.jsx';
import Staff from './pages/Staff.jsx';

export default function App() {
  const { user, loading, logout } = useAuth();

  if (loading) return <div className="center muted">Yükleniyor…</div>;
  if (!user) return <Login />;

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="brand">📚 Kütüphane</div>
        <nav>
          <NavLink to="/" end>Genel Bakış</NavLink>
          <NavLink to="/loans">Ödünç İşlemleri</NavLink>
          <NavLink to="/overdue">Gecikenler</NavLink>
          <NavLink to="/members">Üyeler</NavLink>
          <NavLink to="/books">Kitap Kataloğu</NavLink>
          <NavLink to="/authors">Yazarlar</NavLink>
          <NavLink to="/notifications">Bildirimler</NavLink>
          {user.role === 'ADMIN' && <NavLink to="/staff">Personel</NavLink>}
        </nav>
        <div className="sidebar-user">
          <div>{user.fullName}</div>
          <div className="muted small">{user.role === 'ADMIN' ? 'Yönetici' : 'Personel'}</div>
          <button className="btn btn-ghost" onClick={logout}>Çıkış</button>
        </div>
      </aside>
      <main className="content">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/loans" element={<Loans />} />
          <Route path="/overdue" element={<Overdue />} />
          <Route path="/members" element={<Members />} />
          <Route path="/members/:id" element={<MemberDetail />} />
          <Route path="/books" element={<Books />} />
          <Route path="/books/:id" element={<BookDetail />} />
          <Route path="/authors" element={<Authors />} />
          <Route path="/authors/:id" element={<AuthorDetail />} />
          <Route path="/notifications" element={<Notifications />} />
          {user.role === 'ADMIN' && <Route path="/staff" element={<Staff />} />}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}
