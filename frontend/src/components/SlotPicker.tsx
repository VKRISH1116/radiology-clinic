// A reusable date + slot picker, shared by the booking and reschedule flows.
// It owns the date and the fetched grid; the chosen slot is lifted to the parent
// via `value`/`onChange` (a "controlled" component), so the parent decides what
// to do with the selection.

import { useEffect, useState } from 'react';
import { api } from '../api/api';
import type { SlotAvailability } from '../types';
import styles from './SlotPicker.module.css';

function tomorrowISO(): string {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  return d.toISOString().slice(0, 10);
}

interface Props {
  value: SlotAvailability | null;
  onChange: (slot: SlotAvailability | null) => void;
}

export function SlotPicker({ value, onChange }: Props) {
  const [date, setDate] = useState(tomorrowISO());
  const [slots, setSlots] = useState<SlotAvailability[]>([]);
  const [loading, setLoading] = useState(true);

  // Reload the grid whenever the date changes, and clear any stale selection.
  // (onChange is intentionally omitted from deps — including it would re-run on
  //  every parent render; we only want to react to the date.)
  useEffect(() => {
    let active = true;
    setLoading(true);
    onChange(null);
    api.listSlots(date).then((s) => {
      if (!active) return;
      setSlots(s);
      setLoading(false);
    });
    return () => {
      active = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [date]);

  return (
    <div>
      <label htmlFor="date">Date</label>
      <input
        id="date"
        type="date"
        value={date}
        min={new Date().toISOString().slice(0, 10)}
        onChange={(e) => setDate(e.target.value)}
      />

      {loading ? (
        <p className={styles.muted}>Loading slots…</p>
      ) : (
        <div className={styles.slots}>
          {slots.map((slot) => (
            <button
              key={slot.id}
              className={`${styles.slot} ${value?.id === slot.id ? styles.slotOn : ''}`}
              disabled={slot.available === 0}
              title={slot.available === 0 ? 'Fully booked' : ''}
              onClick={() => onChange(slot)}
            >
              {slot.startTime.slice(11, 16)}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
