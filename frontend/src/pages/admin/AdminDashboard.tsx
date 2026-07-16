import styles from '../Console.module.css';

const TILES = [
  { title: 'Service catalogue', desc: 'Add, edit and deactivate ultrasound studies.' },
  { title: 'Referral payouts', desc: 'Ledger, per-doctor totals, mark as paid.' },
  { title: 'Referral rules', desc: 'Configure the commission rule engine.' },
  { title: 'User management', desc: 'Create staff and admin accounts.' },
  { title: 'Audit log', desc: 'Recent sensitive actions across the system.' },
];

export function AdminDashboard() {
  return (
    <div>
      <h1 className={styles.h1}>Admin console</h1>
      <p className={styles.sub}>Catalogue, payouts and system administration.</p>
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
