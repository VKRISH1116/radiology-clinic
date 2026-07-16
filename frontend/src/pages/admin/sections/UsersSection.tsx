import { useEffect, useState, type FormEvent } from 'react';
import { api } from '../../../api/api';
import type { Role, UserSummary } from '../../../types';
import styles from '../Admin.module.css';

export function UsersSection() {
  const [users, setUsers] = useState<UserSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<Role>('STAFF');
  const [error, setError] = useState<string | null>(null);
  const [ok, setOk] = useState<string | null>(null);

  function reload() {
    api.listUsers().then((u) => {
      setUsers(u);
      setLoading(false);
    });
  }
  useEffect(reload, []);

  async function add(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setOk(null);
    if (password.length < 8) {
      setError('Password must be at least 8 characters');
      return;
    }
    try {
      await api.adminCreateUser(email.trim(), password, role);
      setOk(`Created ${email.trim()} (${role})`);
      setEmail('');
      setPassword('');
      reload();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create user');
    }
  }

  return (
    <div className={styles.section}>
      <h2>Staff &amp; admin accounts</h2>
      <form className={styles.form} onSubmit={add}>
        <div className={styles.field}>
          <label>Email</label>
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </div>
        <div className={styles.field}>
          <label>Password</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </div>
        <div className={styles.field}>
          <label>Role</label>
          <select value={role} onChange={(e) => setRole(e.target.value as Role)}>
            <option value="STAFF">STAFF</option>
            <option value="ADMIN">ADMIN</option>
          </select>
        </div>
        <button className="btn-primary" type="submit">
          Create user
        </button>
        {error && <span className={styles.error}>{error}</span>}
        {ok && <span className={styles.ok}>{ok}</span>}
      </form>

      {loading ? (
        <p className={styles.muted}>Loading…</p>
      ) : (
        <div className={styles.scroll}>
          <table>
            <thead>
              <tr>
                <th>Email</th>
                <th>Role</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.email}>
                  <td>{u.email}</td>
                  <td>
                    <span className={`${styles.badge} ${styles.on}`}>{u.role}</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
