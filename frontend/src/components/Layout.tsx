// The shell around every signed-in page: a top bar (brand, who's logged in, log
// out) and a content area where the current route renders via <Outlet />.

import { Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import styles from './Layout.module.css';

export function Layout() {
  const { session, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/login', { replace: true });
  }

  return (
    <div className={styles.shell}>
      <header className={styles.topbar}>
        <span className={styles.brand}>🩻 Radiology Clinic</span>
        <div className={styles.spacer} />
        {session && (
          <>
            <span className={styles.who}>{session.email}</span>
            <span className="badge">{session.role}</span>
            <button onClick={handleLogout}>Log out</button>
          </>
        )}
      </header>
      <main className={styles.main}>
        <Outlet />
      </main>
    </div>
  );
}
