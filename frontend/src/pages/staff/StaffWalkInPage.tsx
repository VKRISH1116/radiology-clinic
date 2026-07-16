import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { SlotPicker } from '../../components/SlotPicker';
import { mockApi } from '../../mock/api';
import type { Service, SlotAvailability } from '../../types';
import { formatINR } from '../../util/format';
// Reuse the patient booking page's layout styles — same two-column shape.
import styles from '../patient/BookingPage.module.css';

export function StaffWalkInPage() {
  const navigate = useNavigate();
  const [services, setServices] = useState<Service[]>([]);
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [selectedStudies, setSelectedStudies] = useState<number[]>([]);
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

  const bill = useMemo(
    () =>
      selectedStudies.reduce(
        (sum, id) => sum + (services.find((s) => s.id === id)?.price ?? 0),
        0,
      ),
    [selectedStudies, services],
  );

  const canBook =
    name.trim() !== '' && selectedStudies.length > 0 && slot !== null && !submitting;

  async function confirm() {
    if (!slot) return;
    setError(null);
    setSubmitting(true);
    try {
      await mockApi.walkInBook(name.trim(), slot, selectedStudies);
      navigate('/staff', { replace: true, state: { walkedIn: true } });
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
      <button className={styles.back} onClick={() => navigate('/staff')}>
        ← Back
      </button>
      <h1 className={styles.h1}>Walk-in booking</h1>

      <div className={styles.cols}>
        <section className={styles.col}>
          <h2 className={styles.h2}>1 · Patient</h2>
          <div className={styles.group}>
            <label htmlFor="name">Full name</label>
            <input id="name" value={name} onChange={(e) => setName(e.target.value)} required />
          </div>
          <div className={styles.group}>
            <label htmlFor="phone">Phone (optional)</label>
            <input id="phone" value={phone} onChange={(e) => setPhone(e.target.value)} />
          </div>

          <h2 className={styles.h2}>2 · Studies</h2>
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
          <h2 className={styles.h2}>3 · Slot</h2>
          <SlotPicker value={slot} onChange={setSlot} />
        </section>
      </div>

      <div className={`card ${styles.summary}`}>
        <div>
          <div className={styles.sumLine}>
            {name.trim() || 'No name yet'} · {selectedStudies.length} stud
            {selectedStudies.length === 1 ? 'y' : 'ies'} · {slot ? 'slot selected' : 'no slot'}
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
