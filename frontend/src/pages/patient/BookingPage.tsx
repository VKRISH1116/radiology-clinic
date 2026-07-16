import { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { SlotPicker } from '../../components/SlotPicker';
import { mockApi } from '../../mock/api';
import type { Service, SlotAvailability } from '../../types';
import { formatINR } from '../../util/format';
import styles from './BookingPage.module.css';

export function BookingPage() {
  const navigate = useNavigate();
  const location = useLocation();
  // A study id may be passed in when arriving from a "Book" button on the dashboard.
  const preselect = (location.state as { serviceId?: number } | null)?.serviceId;

  const [services, setServices] = useState<Service[]>([]);
  const [selectedStudies, setSelectedStudies] = useState<number[]>(preselect ? [preselect] : []);
  const [slot, setSlot] = useState<SlotAvailability | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    mockApi.listServices().then(setServices);
  }, []);

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

  const canBook = selectedStudies.length > 0 && slot !== null && !submitting;

  async function confirm() {
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
          <SlotPicker value={slot} onChange={setSlot} />
        </section>
      </div>

      <div className={`card ${styles.summary}`}>
        <div>
          <div className={styles.sumLine}>
            {selectedStudies.length} stud{selectedStudies.length === 1 ? 'y' : 'ies'} ·{' '}
            {slot ? 'slot selected' : 'no slot yet'}
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
