import { Link } from 'react-router-dom';
import { Loader2, X } from 'lucide-react';
import { useEffect } from 'react';

export const Spinner = ({ size = 20, className = '' }) => <Loader2 size={size} className={`animate-spin ${className}`} />;

export function PageLoader({ label = 'Loading…' }) {
  return (
    <div className="grid min-h-[40vh] place-items-center text-slate-500">
      <div className="flex flex-col items-center gap-3"><Spinner size={32} className="text-brand-500" /><span className="text-sm">{label}</span></div>
    </div>
  );
}

export function EmptyState({ icon: Icon, title, text, action }) {
  return (
    <div className="card mx-auto flex max-w-lg flex-col items-center px-6 py-14 text-center">
      {Icon && <span className="mb-4 grid h-16 w-16 place-items-center rounded-2xl bg-brand-50 text-brand-600 dark:bg-brand-900/40 dark:text-brand-300"><Icon size={30} /></span>}
      <h3 className="text-lg font-bold text-slate-900 dark:text-white">{title}</h3>
      {text && <p className="mt-1.5 max-w-sm text-sm text-slate-500 dark:text-slate-400">{text}</p>}
      {action && (action.to
        ? <Link to={action.to} className="btn-primary mt-6">{action.label}</Link>
        : <button onClick={action.onClick} className="btn-primary mt-6">{action.label}</button>)}
    </div>
  );
}

/** Shown instead of fake/placeholder content when the backend returns nothing. */
export function NoData({ title = 'No data', text, icon: Icon, action, compact = false }) {
  return (
    <div className={`flex flex-col items-center justify-center text-center ${compact ? 'py-8' : 'py-14'}`}>
      {Icon && <span className="mb-3 grid h-12 w-12 place-items-center rounded-2xl bg-slate-100 text-slate-400 dark:bg-slate-800"><Icon size={24} /></span>}
      <p className="font-bold text-slate-700 dark:text-slate-200">{title}</p>
      {text && <p className="mt-1 max-w-sm text-sm text-slate-500 dark:text-slate-400">{text}</p>}
      {action && <button onClick={action.onClick} className="btn-primary btn-sm mt-4">{action.label}</button>}
    </div>
  );
}

export function ErrorState({ error, onRetry, message }) {
  return (
    <div className="card mx-auto max-w-lg px-6 py-10 text-center">
      <h3 className="text-lg font-bold text-slate-900 dark:text-white">We couldn't load this</h3>
      <p className="mt-1.5 text-sm text-slate-500 dark:text-slate-400">{message}</p>
      {onRetry && <button onClick={onRetry} className="btn-outline mt-5">Try again</button>}
    </div>
  );
}

export function PageHeader({ title, subtitle, children }) {
  return (
    <div className="mb-8 flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 className="h-title">{title}</h1>
        {subtitle && <p className="mt-2 text-slate-500 dark:text-slate-400">{subtitle}</p>}
      </div>
      {children}
    </div>
  );
}

const STATUS = {
  PENDING: 'bg-amber-100 text-amber-800 dark:bg-amber-500/15 dark:text-amber-300',
  PAID: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-500/15 dark:text-emerald-300',
  PAYMENT_FAILED: 'bg-rose-100 text-rose-800 dark:bg-rose-500/15 dark:text-rose-300',
  CANCELLED: 'bg-slate-200 text-slate-700 dark:bg-slate-700 dark:text-slate-300',
  SHIPPED: 'bg-sky-100 text-sky-800 dark:bg-sky-500/15 dark:text-sky-300',
  IN_TRANSIT: 'bg-sky-100 text-sky-800 dark:bg-sky-500/15 dark:text-sky-300',
  CREATED: 'bg-indigo-100 text-indigo-800 dark:bg-indigo-500/15 dark:text-indigo-300',
  DELIVERED: 'bg-teal-100 text-teal-800 dark:bg-teal-500/15 dark:text-teal-300',
  SUCCESS: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-500/15 dark:text-emerald-300',
  FAILED: 'bg-rose-100 text-rose-800 dark:bg-rose-500/15 dark:text-rose-300',
  ADMIN: 'bg-fuchsia-100 text-fuchsia-800 dark:bg-fuchsia-500/15 dark:text-fuchsia-300',
  USER: 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300',
};
export const StatusBadge = ({ status }) => (
  <span className={`badge ${STATUS[status] ?? STATUS.USER}`}>{String(status ?? '').replace(/_/g, ' ')}</span>
);

export function Modal({ open, onClose, title, children, wide }) {
  useEffect(() => {
    if (!open) return undefined;
    const onKey = (e) => e.key === 'Escape' && onClose();
    document.addEventListener('keydown', onKey);
    document.body.style.overflow = 'hidden';
    return () => { document.removeEventListener('keydown', onKey); document.body.style.overflow = ''; };
  }, [open, onClose]);
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 grid place-items-center p-4" role="dialog" aria-modal="true" aria-label={title}>
      <div className="absolute inset-0 bg-slate-900/60 backdrop-blur-sm" onClick={onClose} />
      <div className={`card relative max-h-[90vh] w-full animate-fade-up overflow-y-auto p-6 ${wide ? 'max-w-2xl' : 'max-w-lg'}`}>
        <div className="mb-5 flex items-center justify-between">
          <h2 className="text-lg font-bold text-slate-900 dark:text-white">{title}</h2>
          <button onClick={onClose} aria-label="Close" className="btn-ghost -mr-2 p-2"><X size={18} /></button>
        </div>
        {children}
      </div>
    </div>
  );
}

export function ConfirmModal({ open, onClose, onConfirm, title, text, confirmLabel = 'Delete', busy }) {
  return (
    <Modal open={open} onClose={onClose} title={title}>
      <p className="text-sm text-slate-600 dark:text-slate-400">{text}</p>
      <div className="mt-6 flex justify-end gap-3">
        <button onClick={onClose} className="btn-outline">Cancel</button>
        <button onClick={onConfirm} disabled={busy} className="btn-danger">{busy && <Spinner size={16} />}{confirmLabel}</button>
      </div>
    </Modal>
  );
}

export function QuantityStepper({ value, onChange, min = 1, max = 99, disabled }) {
  return (
    <div className="inline-flex items-center rounded-xl border border-slate-300 dark:border-slate-700">
      <button type="button" aria-label="Decrease quantity" disabled={disabled || value <= min} onClick={() => onChange(value - 1)}
        className="grid h-10 w-10 place-items-center text-lg text-slate-600 transition hover:bg-slate-100 disabled:opacity-40 dark:text-slate-300 dark:hover:bg-slate-800 rounded-l-xl">−</button>
      <span className="w-10 text-center text-sm font-semibold tabular-nums" aria-live="polite">{value}</span>
      <button type="button" aria-label="Increase quantity" disabled={disabled || value >= max} onClick={() => onChange(value + 1)}
        className="grid h-10 w-10 place-items-center text-lg text-slate-600 transition hover:bg-slate-100 disabled:opacity-40 dark:text-slate-300 dark:hover:bg-slate-800 rounded-r-xl">+</button>
    </div>
  );
}

export const ProductCardSkeleton = () => (
  <div className="card overflow-hidden"><div className="skeleton aspect-[4/3] rounded-none" /><div className="space-y-3 p-4"><div className="skeleton h-3 w-1/3" /><div className="skeleton h-4 w-3/4" /><div className="skeleton h-5 w-1/4" /></div></div>
);
