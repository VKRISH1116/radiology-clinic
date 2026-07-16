import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { api } from '../../api/api';
import type { Appointment } from '../../types';
import { formatDateTime, formatINR } from '../../util/format';
import styles from './StaffDashboard.module.css';

const STATUS_CLASS: Record<Appointment['status'], string> = {
  BOOKED: styles.booked,
  IN_PROGRESS: styles.progress,
  COMPLETED: styles.completed,
  CANCELLED: styles.cancelled,
};

/** A file input styled as a button; validates the picked file is a PDF. */
function ReportControl({
  appt,
  busy,
  onUpload,
}: {
  appt: Appointment;
  busy: boolean;
  onUpload: (file: File) => void;
}) {
  return (
    <label className={styles.upload}>
      <input
        type="file"
        accept="application/pdf"
        hidden
        disabled={busy}
        onChange={(e) => {
          const file = e.target.files?.[0];
          if (file) onUpload(file);
          e.target.value = ''; // allow re-picking the same file later
        }}
      />
      {appt.reportFileName ? `Report: ${appt.reportFileName} ✓` : 'Upload report'}
    </label>
  );
}

export function StaffDashboard() {
  const navigate = useNavigate();
  const location = useLocation();
  const walkedIn = (location.state as { walkedIn?: boolean } | null)?.walkedIn;

  const [schedule, setSchedule] = useState<Appointment[]>([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<number | null>(null);

  useEffect(() => {
    let active = true;
    api.listSchedule().then((s) => {
      if (!active) return;
      setSchedule(s);
      setLoading(false);
    });
    return () => {
      active = false;
    };
  }, []);

  async function complete(id: number) {
    setBusyId(id);
    try {
      await api.completeAppointment(id);
      // Patch just the status locally (keeps patient name / report on the row).
      setSchedule((prev) => prev.map((a) => (a.id === id ? { ...a, status: 'COMPLETED' } : a)));
    } finally {
      setBusyId(null);
    }
  }

  async function upload(id: number, file: File) {
    if (file.type !== 'application/pdf') {
      alert('Only PDF files are accepted');
      return;
    }
    setBusyId(id);
    try {
      await api.uploadReport(id, file);
      // Show the name the user just uploaded (the server stores a random name).
      setSchedule((prev) => prev.map((a) => (a.id === id ? { ...a, reportFileName: file.name } : a)));
    } finally {
      setBusyId(null);
    }
  }

  if (loading) {
    return <p className={styles.muted}>Loading…</p>;
  }

  return (
    <div>
      <div className={styles.headRow}>
        <div>
          <h1 className={styles.h1}>Today's schedule</h1>
          <p className={styles.muted}>Front desk &amp; reporting</p>
        </div>
        <button className="btn-primary" onClick={() => navigate('/staff/walk-in')}>
          + Walk-in booking
        </button>
      </div>

      {walkedIn && <div className={styles.banner}>✓ Walk-in appointment booked.</div>}

      <div className={styles.list}>
        {schedule.map((a) => (
          <div key={a.id} className={`card ${styles.row}`}>
            <div className={styles.rowMain}>
              <div className={styles.who}>
                <span className={styles.time}>{formatDateTime(a.slotStartTime)}</span>
                <span className={styles.name}>{a.patientName}</span>
              </div>
              <span className={`${styles.status} ${STATUS_CLASS[a.status]}`}>{a.status}</span>
            </div>
            <div className={styles.studies}>
              {a.studies.map((s) => s.name).join(' · ')} — {formatINR(a.billedAmount)}
            </div>
            <div className={styles.actions}>
              {a.status === 'BOOKED' && (
                <button disabled={busyId === a.id} onClick={() => complete(a.id)}>
                  {busyId === a.id ? 'Working…' : 'Mark complete'}
                </button>
              )}
              <ReportControl
                appt={a}
                busy={busyId === a.id}
                onUpload={(file) => upload(a.id, file)}
              />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
