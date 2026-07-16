import { useEffect, useState, type FormEvent } from 'react';
import { api } from '../../../api/api';
import type { AdminService } from '../../../types';
import styles from '../Admin.module.css';

/** One catalogue row with an editable price (a small stateful child component). */
function CatalogRow({
  svc,
  onChanged,
}: {
  svc: AdminService;
  onChanged: (s: AdminService) => void;
}) {
  const [price, setPrice] = useState(String(svc.price));
  const [busy, setBusy] = useState(false);
  const dirty = price !== '' && Number(price) !== svc.price;

  async function savePrice() {
    setBusy(true);
    try {
      onChanged(await api.updateService({ ...svc, price: Number(price) }));
    } finally {
      setBusy(false);
    }
  }
  async function toggle() {
    setBusy(true);
    try {
      onChanged(await api.updateService({ ...svc, active: !svc.active }));
    } finally {
      setBusy(false);
    }
  }

  return (
    <tr>
      <td>{svc.name}</td>
      <td>{svc.category}</td>
      <td>
        <input
          className={styles.priceInput}
          type="number"
          min="0"
          value={price}
          onChange={(e) => setPrice(e.target.value)}
        />
        {dirty && (
          <button className={styles.smallBtn} disabled={busy} onClick={savePrice}>
            Save
          </button>
        )}
      </td>
      <td>
        <span className={`${styles.badge} ${svc.active ? styles.on : styles.off}`}>
          {svc.active ? 'ACTIVE' : 'INACTIVE'}
        </span>
      </td>
      <td>
        <button className={styles.smallBtn} disabled={busy} onClick={toggle}>
          {svc.active ? 'Deactivate' : 'Activate'}
        </button>
      </td>
    </tr>
  );
}

export function CatalogueSection() {
  const [items, setItems] = useState<AdminService[]>([]);
  const [loading, setLoading] = useState(true);
  const [category, setCategory] = useState('General');
  const [name, setName] = useState('');
  const [price, setPrice] = useState('');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    api.listCatalog().then((list) => {
      if (!active) return;
      setItems([...list]);
      setLoading(false);
    });
    return () => {
      active = false;
    };
  }, []);

  function replace(s: AdminService) {
    setItems((prev) => prev.map((x) => (x.id === s.id ? { ...s } : x)));
  }

  async function add(e: FormEvent) {
    e.preventDefault();
    setError(null);
    const p = Number(price);
    if (!name.trim() || price === '' || p < 0) {
      setError('Enter a name and a valid price');
      return;
    }
    const created = await api.createService(category, name.trim(), p);
    setItems((prev) => [...prev, created]);
    setName('');
    setPrice('');
  }

  if (loading) return <p className={styles.muted}>Loading…</p>;

  return (
    <div className={styles.section}>
      <h2>Service catalogue</h2>
      <form className={styles.form} onSubmit={add}>
        <div className={styles.field}>
          <label>Category</label>
          <select value={category} onChange={(e) => setCategory(e.target.value)}>
            <option>General</option>
            <option>Obstetrics</option>
          </select>
        </div>
        <div className={styles.field}>
          <label>Name</label>
          <input value={name} onChange={(e) => setName(e.target.value)} />
        </div>
        <div className={styles.field}>
          <label>Price (₹)</label>
          <input type="number" min="0" value={price} onChange={(e) => setPrice(e.target.value)} />
        </div>
        <button className="btn-primary" type="submit">
          Add study
        </button>
        {error && <span className={styles.error}>{error}</span>}
      </form>

      <div className={styles.scroll}>
        <table>
          <thead>
            <tr>
              <th>Study</th>
              <th>Category</th>
              <th>Price</th>
              <th>Status</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {items.map((s) => (
              <CatalogRow key={s.id} svc={s} onChanged={replace} />
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
