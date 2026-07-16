import { useEffect, useState, type FormEvent } from 'react';
import { api } from '../../../api/api';
import type { ReferralRule, ReferringDoctor, Service } from '../../../types';
import styles from '../Admin.module.css';

export function RulesSection() {
  const [rules, setRules] = useState<ReferralRule[]>([]);
  const [doctors, setDoctors] = useState<ReferringDoctor[]>([]);
  const [services, setServices] = useState<Service[]>([]);
  const [loading, setLoading] = useState(true);
  const [doctorId, setDoctorId] = useState('');
  const [serviceId, setServiceId] = useState('');
  const [minAmount, setMinAmount] = useState('');
  const [percentage, setPercentage] = useState('');
  const [error, setError] = useState<string | null>(null);

  // Load rules plus the doctors/services needed to show names for their ids.
  useEffect(() => {
    let active = true;
    Promise.all([api.listRules(), api.listReferringDoctors(), api.listServices()]).then(
      ([r, d, s]) => {
        if (!active) return;
        setRules([...r]);
        setDoctors(d);
        setServices(s);
        setLoading(false);
      },
    );
    return () => {
      active = false;
    };
  }, []);

  const doctorName = (id: number | null) => doctors.find((d) => d.id === id)?.name ?? `#${id}`;
  const serviceName = (id: number | null) => services.find((s) => s.id === id)?.name ?? `#${id}`;

  function scopeLabel(r: ReferralRule): string {
    const parts: string[] = [];
    if (r.referringDoctorId != null) parts.push(doctorName(r.referringDoctorId));
    if (r.serviceId != null) parts.push(serviceName(r.serviceId));
    if (r.minAmount != null) parts.push(`≥ ₹${r.minAmount}`);
    return parts.length ? parts.join(' · ') : 'Any (default)';
  }

  async function add(e: FormEvent) {
    e.preventDefault();
    setError(null);
    const pct = Number(percentage);
    if (percentage === '' || pct < 0 || pct > 100) {
      setError('Percentage must be 0–100');
      return;
    }
    const created = await api.addRule({
      referringDoctorId: doctorId ? Number(doctorId) : null,
      serviceId: serviceId ? Number(serviceId) : null,
      minAmount: minAmount ? Number(minAmount) : null,
      maxAmount: null,
      percentage: pct,
    });
    setRules((prev) => [...prev, created]);
    setDoctorId('');
    setServiceId('');
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
          <label>Doctor</label>
          <select value={doctorId} onChange={(e) => setDoctorId(e.target.value)}>
            <option value="">Any</option>
            {doctors.map((d) => (
              <option key={d.id} value={d.id}>
                {d.name}
              </option>
            ))}
          </select>
        </div>
        <div className={styles.field}>
          <label>Study</label>
          <select value={serviceId} onChange={(e) => setServiceId(e.target.value)}>
            <option value="">Any</option>
            {services.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>
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
