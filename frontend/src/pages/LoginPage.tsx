import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { homePathForRole, useAuth } from '../auth/AuthContext';
import styles from './AuthForm.module.css';

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();

  // Controlled inputs: React state is the single source of truth for the fields.
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const session = await login(email, password);
      navigate(homePathForRole(session.role), { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className={styles.wrap}>
      <form className={`card ${styles.card}`} onSubmit={handleSubmit}>
        <h1 className={styles.title}>Sign in</h1>
        <p className={styles.sub}>Radiology Clinic portal</p>

        {error && <div className={styles.error}>{error}</div>}

        <div className={styles.field}>
          <label htmlFor="email">Email</label>
          <input
            id="email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            autoComplete="username"
          />
        </div>
        <div className={styles.field}>
          <label htmlFor="password">Password</label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            autoComplete="current-password"
          />
        </div>

        <button className="btn-primary" type="submit" disabled={loading}>
          {loading ? 'Signing in…' : 'Sign in'}
        </button>

        <p className={styles.foot}>
          No account? <Link to="/register">Register</Link>
        </p>
        <div className={styles.demo}>
          Demo logins: <code>patient@clinic.local</code> · <code>staff@clinic.local</code> ·{' '}
          <code>admin@clinic.local</code> — password <code>password</code>
        </div>
      </form>
    </div>
  );
}
