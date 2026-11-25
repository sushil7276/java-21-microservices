// Indian rupees with lakh/crore digit grouping, e.g. ₹1,23,456. Whole amounts show no decimals, others show paise.
const inr = (digits) => new Intl.NumberFormat('en-IN', {
  style: 'currency', currency: 'INR', minimumFractionDigits: digits, maximumFractionDigits: digits,
});
const whole = inr(0);
const withPaise = inr(2);
export const formatMoney = (n) => {
  const v = Number(n ?? 0);
  return (Number.isInteger(v) ? whole : withPaise).format(v);
};

export const formatDate = (iso, withTime = false) => {
  if (!iso) return '–';
  return new Date(iso).toLocaleString('en-IN', {
    year: 'numeric', month: 'short', day: 'numeric',
    ...(withTime ? { hour: '2-digit', minute: '2-digit' } : {}),
  });
};

/** Backend error shape: { status, error, message, details } – turn any RTK Query error into a readable string. */
export const errorMessage = (err, fallback = 'Something went wrong. Please try again.') => {
  if (!err) return fallback;
  if (err.status === 'FETCH_ERROR') return 'Cannot reach the server. Is the API gateway running?';
  if (err.status === 429) return 'Too many requests. Please wait a few seconds and try again.';
  if (err.status === 503) return 'This service is starting up or temporarily unavailable. Try again in a moment.';
  const d = err.data;
  if (d?.details && typeof d.details === 'object') {
    const first = Object.values(d.details)[0];
    if (first) return String(first);
  }
  return d?.message || fallback;
};

export const CATEGORY_META = {
  mobiles: { gradient: 'from-indigo-500 via-violet-500 to-fuchsia-500', icon: 'Smartphone' },
  laptops: { gradient: 'from-sky-500 via-blue-500 to-indigo-600', icon: 'Laptop' },
  audio: { gradient: 'from-rose-500 via-pink-500 to-orange-400', icon: 'Headphones' },
  accessories: { gradient: 'from-emerald-500 via-teal-500 to-cyan-500', icon: 'Watch' },
  tablets: { gradient: 'from-amber-400 via-orange-500 to-red-500', icon: 'Tablet' },
};
export const categoryMeta = (c) =>
  CATEGORY_META[(c || '').toLowerCase()] ?? { gradient: 'from-slate-500 via-slate-600 to-slate-800', icon: 'Package' };
