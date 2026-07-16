import { useEffect, useState, type FormEvent } from 'react';
import { mockApi } from '../../../mock/api';
import type { ReferralRule } from '../../../types';
import styles from '../Admin.module.css';

function scopeLabel(r: ReferralRule): string {
  const parts: string[] = [];
  if (r.doctorName) parts.push(r.doctorName);
  if (r.serviceName) parts.push(r.serviceName);
  if (r.minAmount != null) parts.push(`≥ ₹${r.minAmount}`);
  return parts.length ? parts.join(' · ') : 'Any (default)';
}

export function RulesSection() {
  const [rules, setRules] = useState<ReferralRule[]>([]);
  const [loading, setLoading] = useState(true);
  const [doctor, setDoctor] = useState('');
  const [service, setService] = useState('');
  const [minAmount, setMinAmount] = useState('');
  const [percentage, setPercentage] = useState('');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    mockApi.listRules().then((r) => {
      if (!active) return;
      setRules([...r]);
      setLoading(false);
    });
    return () => {
      active = false;
    };
  }, []);

  async function add(e: FormEvent) {
    e.preventDefault();
    setError(null);
    const pct = Number(percentage);
    if (percentage === '' || pct < 0 || pct > 100) {
      setError('Percentage must be 0–100');
      return;
    }
    const created = await mockApi.addRule({
      doctorName: doctor.trim() || null,
      serviceName: service.trim() || null,
      minAmount: minAmount ? Number(minAmount) : null,
      percentage: pct,
    });
    setRules((prev) => [...prev, created]);
    setDoctor('');
    setService('');
    setMinAmount('');
    setPercentage('');
  }

  if (loading) return <p className={styles.muted}>Loading…</p>;

  return (
    <div className={styles.section}>
      <h2>Referral rules</h2>
      <p className={styles.muted}>
        Most specific rule wins: doctor +4 · service +2 · amount range +1.
      </p>
      <form className={styles.form} onSubmit={add}>
        <div className={styles.field}>
          <label>Doctor (opt)</label>
          <input value={doctor} onChange={(e) => setDoctor(e.target.value)} />
        </div>
        <div className={styles.field}>
          <label>Study (opt)</label>
          <input value={service} onChange={(e) => setService(e.target.value)} />
        </div>
        <div className={styles.field}>
          <label>Min ₹ (opt)</label>
          <input type="number" min="0" value={minAmount} onChange={(e) => setMinAmount(e.target.value)} />
        </div>
        <div className={styles.field}>
          <label>Percent</label>
          <input
            type="number"
            min="0"
            max="100"
            value={percentage}
            onChange={(e) => setPercentage(e.target.value)}
          />
        </div>
        <button className="btn-primary" type="submit">
          Add rule
        </button>
        {error && <span className={styles.error}>{error}</span>}
      </form>

      <div className={styles.scroll}>
        <table>
          <thead>
            <tr>
              <th>Scope</th>
              <th>Percentage</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {rules.map((r) => (
              <tr key={r.id}>
                <td>{scopeLabel(r)}</td>
                <td>{r.percentage}%</td>
                <td>
                  <span className={`${styles.badge} ${r.active ? styles.on : styles.off}`}>
                    {r.active ? 'ACTIVE' : 'INACTIVE'}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
