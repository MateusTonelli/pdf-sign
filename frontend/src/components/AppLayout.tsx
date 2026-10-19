import { Link, NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

export function AppLayout() {
  const { user, logout } = useAuth();

  return (
    <div className="app-shell">
      <header className="app-header">
        <Link to="/app" className="brand">
          <span className="brand-mark" aria-hidden>
            AP
          </span>
          AssinaPDF
        </Link>
        <nav className="nav-links" aria-label="Principal">
          <NavLink to="/app" end>
            Documentos
          </NavLink>
          <NavLink to="/app/documents/new">Novo documento</NavLink>
          <NavLink to="/app/settings/certificate">Certificado</NavLink>
          {user && (
            <span className="muted" style={{ fontSize: '0.875rem' }}>
              {user.name}
            </span>
          )}
          <button type="button" className="btn btn-ghost btn-sm" onClick={logout}>
            Sair
          </button>
        </nav>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
      <footer className="app-footer">AssinaPDF — MVP de assinatura de PDF</footer>
    </div>
  );
}
