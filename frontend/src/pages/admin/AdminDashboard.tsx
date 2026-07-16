import { useState } from 'react';
import styles from './Admin.module.css';
import { StatsBar } from './StatsBar';
import { AuditSection } from './sections/AuditSection';
import { CatalogueSection } from './sections/CatalogueSection';
import { ReferralsSection } from './sections/ReferralsSection';
import { RulesSection } from './sections/RulesSection';
import { UsersSection } from './sections/UsersSection';

type TabId = 'catalogue' | 'referrals' | 'rules' | 'users' | 'audit';

const TABS: { id: TabId; label: string }[] = [
  { id: 'catalogue', label: 'Catalogue' },
  { id: 'referrals', label: 'Referral payouts' },
  { id: 'rules', label: 'Rules' },
  { id: 'users', label: 'Users' },
  { id: 'audit', label: 'Audit log' },
];

export function AdminDashboard() {
  const [tab, setTab] = useState<TabId>('catalogue');

  return (
    <div>
      <h1 className={styles.h1}>Admin console</h1>

      <StatsBar />

      <div className={styles.tabs}>
        {TABS.map((t) => (
          <button
            key={t.id}
            className={tab === t.id ? styles.tabOn : styles.tab}
            onClick={() => setTab(t.id)}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'catalogue' && <CatalogueSection />}
      {tab === 'referrals' && <ReferralsSection />}
      {tab === 'rules' && <RulesSection />}
      {tab === 'users' && <UsersSection />}
      {tab === 'audit' && <AuditSection />}
    </div>
  );
}
