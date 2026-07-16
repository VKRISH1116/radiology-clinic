import styles from '../Console.module.css';

const TILES = [
  { title: "Today's schedule", desc: 'Slots and booked appointments for the day.' },
  { title: 'Walk-in booking', desc: 'Book for a patient without a login account.' },
  { title: 'Mark complete', desc: 'Close out a visit and trigger the referral payout.' },
  { title: 'Upload report', desc: "Attach a scan's PDF report to an appointment." },
];

export function StaffDashboard() {
  return (
    <div>
      <h1 className={styles.h1}>Staff console</h1>
      <p className={styles.sub}>Front-desk and reporting workflows.</p>
      <div className={styles.grid}>
        {TILES.map((t) => (
          <div key={t.title} className={`card ${styles.tile}`}>
            <div className={styles.tileTitle}>{t.title}</div>
            <div className={styles.tileDesc}>{t.desc}</div>
            <span className={styles.soon}>COMING NEXT</span>
          </div>
        ))}
      </div>
    </div>
  );
}
