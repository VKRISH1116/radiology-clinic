import { useEffect, useState } from 'react';
import { mockApi } from '../../../mock/api';
import type { AuditEntry } from '../../../types';
import { formatDateTime } from '../../../util/format';
import styles from '../Admin.module.css';

export function AuditSection() {
  const [rows, setRows] = useState<AuditEntry[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    mockApi.listAudit().then((r) => {
      if (!active) return;
      setRows(r);
      setLoading(false);
    });
    return () => {
      active = false;
    };
  }, []);

  if (loading) return <p className={styles.muted}>Loading…</p>;

  return (
    <div className={styles.section}>
      <h2>Audit log</h2>
      <div className={styles.scroll}>
        <table>
          <thead>
            <tr>
              <th>When</th>
              <th>Action</th>
              <th>Entity</th>
              <th>Actor</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.id}>
                <td>{formatDateTime(r.createdAt)}</td>
                <td>{r.action}</td>
                <td>
                  {r.entity}
                  {r.entityId != null ? ` #${r.entityId}` : ''}
                </td>
                <td className={styles.muted}>{r.actor}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
