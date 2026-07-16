import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
import { mockApi } from '../../mock/api';
import type { Appointment, Service } from '../../types';
import { formatDateTime, formatINR } from '../../util/format';
import styles from './PatientDashboard.module.css';

const STATUS_CLASS: Record<Appointment['status'], string> = {
  BOOKED: styles.booked,
  IN_PROGRESS: styles.progress,
  COMPLETED: styles.completed,
  CANCELLED: styles.cancelled,
};

export function PatientDashboard() {
  const { session } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const flash = location.state as { booked?: boolean; rescheduled?: boolean } | null;
  const [services, setServices] = useState<Service[]>([]);
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [loading, setLoading] = useState(true);
  const [cancellingId, setCancellingId] = useState<number | null>(null);

  // Fetch both lists once when the screen mounts. The `active` flag avoids setting
  // state if the component unmounts before the (async) data arrives.
  useEffect(() => {
    let active = true;
    Promise.all([mockApi.listServices(), mockApi.listMyAppointments()]).then(
      ([svc, appts]) => {
        if (!active) return;
        setServices(svc);
        setAppointments(appts);
        setLoading(false);
      },
    );
    return () => {
      active = false;
    };
  }, []);

  async function handleCancel(id: number) {
    setCancellingId(id);
    try {
      const updated = await mockApi.cancel(id);
      // Replace that appointment with a fresh object so React re-renders the row.
      setAppointments((prev) => prev.map((a) => (a.id === id ? { ...updated } : a)));
    } finally {
      setCancellingId(null);
    }
  }

  if (loading) {
    return <p className={styles.muted}>Loading…</p>;
  }

  // Group the catalogue by category for display.
  const byCategory = services.reduce<Record<string, Service[]>>((acc, s) => {
    (acc[s.category] ??= []).push(s);
    return acc;
  }, {});

  return (
    <div>
      <div className={styles.headRow}>
        <div>
          <h1 className={styles.h1}>Welcome back</h1>
          <p className={styles.muted}>{session?.email}</p>
        </div>
        <button className="btn-primary" onClick={() => navigate('/patient/book')}>
          + Book a scan
        </button>
      </div>

      {flash?.booked && <div className={styles.banner}>✓ Appointment booked.</div>}
      {flash?.rescheduled && <div className={styles.banner}>✓ Appointment rescheduled.</div>}

      <h2 className={styles.h2}>My appointments</h2>
      {appointments.length === 0 ? (
        <p className={styles.muted}>No appointments yet.</p>
      ) : (
        <div className={styles.list}>
          {appointments.map((a) => (
            <div key={a.id} className={`card ${styles.appt}`}>
              <div className={styles.apptTop}>
                <span className={styles.when}>{formatDateTime(a.slotStartTime)}</span>
                <span className={`${styles.status} ${STATUS_CLASS[a.status]}`}>{a.status}</span>
              </div>
              <div className={styles.studies}>
                {a.studies.map((s) => s.name).join(' · ')}
              </div>
              <div className={styles.bill}>{formatINR(a.billedAmount)}</div>
              {a.status === 'BOOKED' && (
                <div className={styles.actions}>
                  <button
                    onClick={() => navigate(`/patient/appointments/${a.id}/reschedule`)}
                  >
                    Reschedule
                  </button>
                  <button
                    className={styles.danger}
                    disabled={cancellingId === a.id}
                    onClick={() => handleCancel(a.id)}
                  >
                    {cancellingId === a.id ? 'Cancelling…' : 'Cancel'}
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      <h2 className={styles.h2}>Available studies</h2>
      {Object.entries(byCategory).map(([category, items]) => (
        <div key={category} className={styles.group}>
          <h3 className={styles.cat}>{category}</h3>
          <div className={styles.grid}>
            {items.map((s) => (
              <div key={s.id} className={`card ${styles.svc}`}>
                <div className={styles.svcName}>{s.name}</div>
                <div className={styles.svcPrice}>{formatINR(s.price)}</div>
                <button onClick={() => navigate('/patient/book', { state: { serviceId: s.id } })}>
                  Book
                </button>
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
