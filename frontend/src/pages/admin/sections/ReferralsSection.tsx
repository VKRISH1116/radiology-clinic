import { useEffect, useMemo, useState } from 'react';
import { mockApi } from '../../../mock/api';
import type { Referral } from '../../../types';
import { formatINR } from '../../../util/format';
import styles from '../Admin.module.css';

export function ReferralsSection() {
  const [refs, setRefs] = useState<Referral[]>([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<number | null>(null);

  useEffect(() => {
    let active = true;
    mockApi.listReferrals().then((r) => {
      if (!active) return;
      setRefs([...r]);
      setLoading(false);
    });
    return () => {
      active = false;
    };
  }, []);

  // Per-doctor totals (top referrers), derived from the ledger.
  const summary = useMemo(() => {
    const byDoctor = new Map<string, { count: number; total: number }>();
    for (const r of refs) {
      const entry = byDoctor.get(r.doctorName) ?? { count: 0, total: 0 };
      entry.count += 1;
      entry.total += r.amount;
      byDoctor.set(r.doctorName, entry);
    }
    return [...byDoctor.entries()].sort((a, b) => b[1].total - a[1].total);
  }, [refs]);

  async function pay(id: number) {
    setBusyId(id);
    try {
      const updated = await mockApi.payReferral(id);
      setRefs((prev) => prev.map((r) => (r.id === id ? { ...updated } : r)));
    } finally {
      setBusyId(null);
    }
  }

  if (loading) return <p className={styles.muted}>Loading…</p>;

  return (
    <div className={styles.section}>
      <h2>Top referrers</h2>
      <div className={styles.scroll}>
        <table>
          <thead>
            <tr>
              <th>Doctor</th>
              <th>Referrals</th>
              <th>Total</th>
            </tr>
          </thead>
          <tbody>
            {summary.map(([doctor, s]) => (
              <tr key={doctor}>
                <td>{doctor}</td>
                <td>{s.count}</td>
                <td className={styles.total}>{formatINR(s.total)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <h2 style={{ marginTop: 24 }}>Payout ledger</h2>
      <div className={styles.scroll}>
        <table>
          <thead>
            <tr>
              <th>Doctor</th>
              <th>Appt</th>
              <th>Amount</th>
              <th>Status</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {refs.map((r) => (
              <tr key={r.id}>
                <td>{r.doctorName}</td>
                <td>#{r.appointmentId}</td>
                <td className={styles.total}>{formatINR(r.amount)}</td>
                <td>
                  <span
                    className={`${styles.badge} ${r.status === 'PAID' ? styles.paid : styles.pending}`}
                  >
                    {r.status}
                  </span>
                </td>
                <td>
                  {r.status === 'PENDING' && (
                    <button
                      className={styles.smallBtn}
                      disabled={busyId === r.id}
                      onClick={() => pay(r.id)}
                    >
                      {busyId === r.id ? '…' : 'Mark paid'}
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
