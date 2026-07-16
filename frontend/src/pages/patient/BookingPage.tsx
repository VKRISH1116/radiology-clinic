import { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { mockApi } from '../../mock/api';
import type { Service, SlotAvailability } from '../../types';
import { formatINR } from '../../util/format';
import styles from './BookingPage.module.css';

/** Default the date to tomorrow (yyyy-mm-dd for the <input type="date">). */
function tomorrowISO(): string {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  return d.toISOString().slice(0, 10);
}

export function BookingPage() {
  const navigate = useNavigate();
  const location = useLocation();
  // A study id may be passed in when arriving from a "Book" button on the dashboard.
  const preselect = (location.state as { serviceId?: number } | null)?.serviceId;

  const [services, setServices] = useState<Service[]>([]);
  const [date, setDate] = useState(tomorrowISO());
  const [slots, setSlots] = useState<SlotAvailability[]>([]);
  const [loadingSlots, setLoadingSlots] = useState(true);
  const [selectedStudies, setSelectedStudies] = useState<number[]>(preselect ? [preselect] : []);
  const [selectedSlotId, setSelectedSlotId] = useState<number | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    mockApi.listServices().then(setServices);
  }, []);

  // Re-fetch the slot grid whenever the date changes; clear any slot selection.
  useEffect(() => {
    let active = true;
    setLoadingSlots(true);
    setSelectedSlotId(null);
    mockApi.listSlots(date).then((s) => {
      if (!active) return;
      setSlots(s);
      setLoadingSlots(false);
    });
    return () => {
      active = false;
    };
  }, [date]);

  function toggleStudy(id: number) {
    setSelectedStudies((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id],
    );
  }

  // Derived bill — recomputed only when the selection or catalogue changes.
  const bill = useMemo(
    () =>
      selectedStudies.reduce(
        (sum, id) => sum + (services.find((s) => s.id === id)?.price ?? 0),
        0,
      ),
    [selectedStudies, services],
  );

  const canBook = selectedStudies.length > 0 && selectedSlotId !== null && !submitting;

  async function confirm() {
    const slot = slots.find((s) => s.id === selectedSlotId);
    if (!slot) return;
    setError(null);
    setSubmitting(true);
    try {
      await mockApi.book(slot, selectedStudies);
      navigate('/patient', { replace: true, state: { booked: true } });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Booking failed');
      setSubmitting(false);
    }
  }

  const byCategory = services.reduce<Record<string, Service[]>>((acc, s) => {
    (acc[s.category] ??= []).push(s);
    return acc;
  }, {});

  return (
    <div>
      <button className={styles.back} onClick={() => navigate('/patient')}>
        ← Back
      </button>
      <h1 className={styles.h1}>Book a scan</h1>

      <div className={styles.cols}>
        <section className={styles.col}>
          <h2 className={styles.h2}>1 · Choose studies</h2>
          {Object.entries(byCategory).map(([category, items]) => (
            <div key={category} className={styles.group}>
              <div className={styles.cat}>{category}</div>
              {items.map((s) => (
                <label key={s.id} className={styles.study}>
                  <input
                    type="checkbox"
                    className={styles.check}
                    checked={selectedStudies.includes(s.id)}
                    onChange={() => toggleStudy(s.id)}
                  />
                  <span className={styles.studyName}>{s.name}</span>
                  <span className={styles.studyPrice}>{formatINR(s.price)}</span>
                </label>
              ))}
            </div>
          ))}
        </section>

        <section className={styles.col}>
          <h2 className={styles.h2}>2 · Pick a slot</h2>
          <label htmlFor="date">Date</label>
          <input
            id="date"
            type="date"
            value={date}
            min={new Date().toISOString().slice(0, 10)}
            onChange={(e) => setDate(e.target.value)}
          />

          {loadingSlots ? (
            <p className={styles.muted}>Loading slots…</p>
          ) : (
            <div className={styles.slots}>
              {slots.map((slot) => (
                <button
                  key={slot.id}
                  className={`${styles.slot} ${selectedSlotId === slot.id ? styles.slotOn : ''}`}
                  disabled={slot.available === 0}
                  title={slot.available === 0 ? 'Fully booked' : ''}
                  onClick={() => setSelectedSlotId(slot.id)}
                >
                  {slot.startTime.slice(11, 16)}
                </button>
              ))}
            </div>
          )}
        </section>
      </div>

      <div className={`card ${styles.summary}`}>
        <div>
          <div className={styles.sumLine}>
            {selectedStudies.length} stud{selectedStudies.length === 1 ? 'y' : 'ies'} ·{' '}
            {selectedSlotId ? 'slot selected' : 'no slot yet'}
          </div>
          <div className={styles.total}>{formatINR(bill)}</div>
        </div>
        <div className={styles.sumRight}>
          {error && <span className={styles.error}>{error}</span>}
          <button className="btn-primary" disabled={!canBook} onClick={confirm}>
            {submitting ? 'Booking…' : 'Confirm booking'}
          </button>
        </div>
      </div>
    </div>
  );
}
