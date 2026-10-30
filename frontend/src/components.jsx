import { useCallback, useEffect, useState } from 'react';

export const STATUS_LABELS = {
  PENDING: 'Verme onayı bekliyor',
  ACTIVE: 'Ödünçte',
  OVERDUE: 'Gecikmiş',
  RETURN_PENDING: 'İade onayı bekliyor',
  RETURNED: 'İade edildi',
  REJECTED: 'Reddedildi',
};

export function StatusBadge({ status }) {
  return <span className={`badge badge-${status?.toLowerCase()}`}>{STATUS_LABELS[status] || status}</span>;
}

export const fmtDate = (d) => (d ? new Date(d).toLocaleDateString('tr-TR') : '—');
export const fmtDateTime = (d) => (d ? new Date(d).toLocaleString('tr-TR') : '—');

/** Days between today and a yyyy-MM-dd date; negative means the date has passed. */
export function daysUntil(date) {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return Math.round((new Date(`${date}T00:00:00`) - today) / 86400000);
}

/** Loads data with a promise factory and exposes reload, error and loading state. */
export function useLoad(factory, deps = []) {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const load = useCallback(() => {
    setLoading(true);
    return factory()
      .then((d) => { setData(d); setError(null); })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, deps);
  useEffect(() => { load(); }, [load]);
  return { data, error, loading, reload: load, setData };
}

export function ErrorBox({ error }) {
  return error ? <div className="alert alert-error">{error}</div> : null;
}

export function Loading({ when }) {
  return when ? <div className="muted">Yükleniyor…</div> : null;
}

export function Modal({ title, onClose, children }) {
  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal" role="dialog" aria-label={title}>
        <div className="modal-head">
          <h3>{title}</h3>
          <button className="btn-icon" onClick={onClose} aria-label="Kapat">×</button>
        </div>
        {children}
      </div>
    </div>
  );
}

/** Runs an async action, keeps a busy flag and reports errors. */
export function useAction() {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const run = async (fn) => {
    setBusy(true);
    setError(null);
    try {
      return await fn();
    } catch (e) {
      setError(e.message);
      return undefined;
    } finally {
      setBusy(false);
    }
  };
  return { busy, error, run, setError };
}

export function Field({ label, children }) {
  return (
    <label className="field">
      <span>{label}</span>
      {children}
    </label>
  );
}
