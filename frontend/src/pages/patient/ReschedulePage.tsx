import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { SlotPicker } from '../../components/SlotPicker';
import { api } from '../../api/api';
import type { Appointment, SlotAvailability } from '../../types';
import { formatDateTime } from '../../util/format';
import styles from './ReschedulePage.module.css';

export function ReschedulePage() {
  const { id } = useParams();
  const appointmentId = Number(id);
  const navigate = useNavigate();

  const [appt, setAppt] = useState<Appointment | null>(null);
  const [loading, setLoading] = useState(true);
  const [slot, setSlot] = useState<SlotAvailability | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    api.listMyAppointments().then((list) => {
      if (!active) return;
      setAppt(list.find((a) => a.id === appointmentId) ?? null);
      setLoading(false);
    });
    return () => {
      active = false;
    };
  }, [appointmentId]);

  async function confirm() {
    if (!slot) return;
    setError(null);
    setSubmitting(true);
    try {
      await api.reschedule(appointmentId, slot);
      navigate('/patient', { replace: true, state: { rescheduled: true } });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Reschedule failed');
      setSubmitting(false);
    }
  }

  if (loading) {
    return <p className={styles.muted}>Loading…</p>;
  }
  if (!appt) {
    return (
      <div>
        <button className={styles.back} onClick={() => navigate('/patient')}>
          ← Back
        </button>
        <p className={styles.muted}>Appointment not found.</p>
      </div>
    );
  }

  return (
    <div>
      <button className={styles.back} onClick={() => navigate('/patient')}>
        ← Back
      </button>
      <h1 className={styles.h1}>Reschedule appointment</h1>

      <div className={`card ${styles.current}`}>
        <div className={styles.curLabel}>Current time</div>
        <div className={styles.curWhen}>{formatDateTime(appt.slotStartTime)}</div>
        <div className={styles.curStudies}>{appt.studies.map((s) => s.name).join(' · ')}</div>
      </div>

      <h2 className={styles.h2}>Pick a new slot</h2>
      <SlotPicker value={slot} onChange={setSlot} />

      <div className={`card ${styles.summary}`}>
        <div className={styles.sumLine}>{slot ? 'New slot selected' : 'No new slot yet'}</div>
        <div className={styles.sumRight}>
          {error && <span className={styles.error}>{error}</span>}
          <button className="btn-primary" disabled={!slot || submitting} onClick={confirm}>
            {submitting ? 'Saving…' : 'Confirm new time'}
          </button>
        </div>
      </div>
    </div>
  );
}
