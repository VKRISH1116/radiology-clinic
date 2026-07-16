// Small formatting helpers, kept in one place so every screen shows money and
// dates the same way (Indian locale: ₹ and dd Mon yyyy).

export const formatINR = (n: number): string =>
  new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(n);

export const formatDateTime = (iso: string): string =>
  new Date(iso).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' });
