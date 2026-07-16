import { useEffect, useState } from 'react';
import { api } from '../../api/api';
import type { DashboardStats } from '../../types';
import { formatINR } from '../../util/format';
import styles from './Admin.module.css';

/**
 * The KPI row at the top of the admin console (AC-F7-1): today's appointments,
 * total patients, reports delivered vs pending, and the top-5 referrers. Loaded
 * once when the console mounts.
 */
export function StatsBar() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    api
      .getDashboardStats()
      .then((s) => active && setStats(s))
      .catch((e) => active && setError(e instanceof Error ? e.message : 'Could not load stats'));
    return () => {
      active = false;
    };
  }, []);

  if (error) return <p className={styles.muted}>Dashboard stats unavailable: {error}</p>;
  if (!stats) return <p className={styles.muted}>Loading dashboard…</p>;

  return (
    <div>
      <div className={styles.tiles}>
        <Tile label="Today's appointments" value={stats.todaysAppointments} />
        <Tile label="Total patients" value={stats.totalPatients} />
        <Tile label="Reports delivered" value={stats.reportsDelivered} />
        <Tile label="Reports pending" value={stats.reportsPending} accent={stats.reportsPending > 0} />
      </div>

      <div className={styles.topRef}>
        <span className={styles.topRefLabel}>Top referrers</span>
        {stats.topReferrers.length === 0 ? (
          <span className={styles.muted}>No referral payouts yet</span>
        ) : (
          <ol className={styles.topRefList}>
            {stats.topReferrers.map((r) => (
              <li key={r.referringDoctorId ?? 'none'}>
                <span>{r.doctorName ?? 'Unknown'}</span>
                <span className={styles.topRefAmt}>
                  {formatINR(r.totalAmount)} · {r.referralCount}
                </span>
              </li>
            ))}
          </ol>
        )}
      </div>
    </div>
  );
}

function Tile({ label, value, accent }: { label: string; value: number; accent?: boolean }) {
  return (
    <div className={styles.tile}>
      <div className={`${styles.tileValue} ${accent ? styles.tileAccent : ''}`}>{value}</div>
      <div className={styles.tileLabel}>{label}</div>
    </div>
  );
}
