import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { daysUntil, ErrorBox, Field, fmtDate, fmtDateTime, Loading, Modal, StatusBadge, useAction, useLoad } from '../components.jsx';

export default function Loans() {
  const [creating, setCreating] = useState(false);
  const [filter, setFilter] = useState('');
  const pending = useLoad(() => api.pendingLoans(), []);
  const active = useLoad(() => api.activeLoans(), []);
  const action = useAction();

  const reloadAll = () => { pending.reload(); active.reload(); };
  const act = (fn) => action.run(async () => { await fn(); reloadAll(); });

  const reject = (loan) => {
    const reason = prompt('Red gerekçesi (isteğe bağlı):');
    if (reason === null) return;
    act(() => api.rejectLoan(loan.id, reason));
  };

  const extend = (loan) => {
    const days = prompt('Kaç gün uzatılsın?', '7');
    if (!days) return;
    act(() => api.extendLoan(loan.id, Number(days)));
  };

  const f = filter.trim().toLocaleLowerCase('tr-TR');
  const shown = (active.data || []).filter((l) => !f
    || l.memberName.toLocaleLowerCase('tr-TR').includes(f) || l.bookTitle.toLocaleLowerCase('tr-TR').includes(f));

  return (
    <>
      <div className="page-head">
        <h2>Ödünç İşlemleri</h2>
        <button className="btn btn-primary" onClick={() => setCreating(true)}>+ Ödünç ver</button>
      </div>
      <ErrorBox error={action.error || pending.error || active.error} />

      <section className="card">
        <h3>Onay bekleyen işlemler</h3>
        <Loading when={pending.loading && !pending.data} />
        {pending.data?.length === 0 && <p className="muted">Onay bekleyen işlem yok.</p>}
        {pending.data?.length > 0 && (
          <table>
            <thead><tr><th>İşlem</th><th>Üye</th><th>Kitap</th><th>Talep eden</th><th>Tarih</th><th></th></tr></thead>
            <tbody>
              {pending.data.map((l) => (
                <tr key={l.id}>
                  <td><StatusBadge status={l.status} /></td>
                  <td><Link to={`/members/${l.memberId}`}>{l.memberName}</Link></td>
                  <td><Link to={`/books/${l.bookId}`}>{l.bookTitle}</Link></td>
                  <td>{l.status === 'PENDING' ? l.requestedBy?.name : l.returnRequestedBy?.name}</td>
                  <td>{fmtDateTime(l.status === 'PENDING' ? l.requestedAt : l.returnRequestedAt)}</td>
                  <td className="actions">
                    {l.status === 'PENDING' ? (
                      <>
                        <button className="btn btn-primary btn-sm" disabled={action.busy} onClick={() => act(() => api.approveLoan(l.id))}>Vermeyi onayla</button>
                        <button className="btn btn-sm" disabled={action.busy} onClick={() => reject(l)}>Reddet</button>
                      </>
                    ) : (
                      <>
                        <button className="btn btn-primary btn-sm" disabled={action.busy} onClick={() => act(() => api.approveReturn(l.id))}>İadeyi onayla</button>
                        <button className="btn btn-sm" disabled={action.busy} onClick={() => act(() => api.cancelReturn(l.id))}>Vazgeç</button>
                      </>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      <section className="card">
        <div className="page-head">
          <h3>Ödünçteki kitaplar</h3>
          <input className="filter" placeholder="Üye veya kitap filtrele" value={filter} onChange={(e) => setFilter(e.target.value)} />
        </div>
        <Loading when={active.loading && !active.data} />
        {active.data && (
          <table>
            <thead><tr><th>Üye</th><th>Kitap</th><th>Veriliş</th><th>Son tarih</th><th>Durum</th><th></th></tr></thead>
            <tbody>
              {shown.map((l) => {
                const left = daysUntil(l.dueDate);
                return (
                  <tr key={l.id}>
                    <td><Link to={`/members/${l.memberId}`}>{l.memberName}</Link></td>
                    <td><Link to={`/books/${l.bookId}`}>{l.bookTitle}</Link></td>
                    <td>{fmtDate(l.loanDate)}</td>
                    <td>
                      {fmtDate(l.dueDate)}{' '}
                      <span className={left < 0 ? 'text-danger' : 'muted'}>({left < 0 ? `${-left} gün gecikti` : `${left} gün`})</span>
                    </td>
                    <td><StatusBadge status={l.status} /></td>
                    <td className="actions">
                      {l.status !== 'RETURN_PENDING' && (
                        <>
                          <button className="btn btn-sm" disabled={action.busy} onClick={() => act(() => api.requestReturn(l.id))}>İade al</button>
                          <button className="btn btn-primary btn-sm" disabled={action.busy} onClick={() => act(() => api.approveReturn(l.id))}>İade al + onayla</button>
                        </>
                      )}
                      {l.status === 'ACTIVE' && (
                        <button className="btn btn-sm" disabled={action.busy} onClick={() => extend(l)}>Uzat</button>
                      )}
                    </td>
                  </tr>
                );
              })}
              {!shown.length && <tr><td colSpan="6" className="muted">Ödünçte kitap yok.</td></tr>}
            </tbody>
          </table>
        )}
      </section>

      {creating && (
        <Modal title="Ödünç ver" onClose={() => setCreating(false)}>
          <NewLoanForm onSaved={() => { setCreating(false); reloadAll(); }} />
        </Modal>
      )}
    </>
  );
}

/** Opens a lending request. It appears in "Onay bekleyen işlemler" until a staff member approves it. */
export function NewLoanForm({ memberId = '', bookId = '', onSaved }) {
  const [form, setForm] = useState({ memberId, bookId, days: 14, note: '' });
  const refs = useLoad(() => Promise.all([api.members(), api.books({ availableOnly: true })])
    .then(([members, books]) => ({ members: members.filter((m) => m.status === 'ACTIVE'), books })), []);
  const { busy, error, run } = useAction();
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = (approveNow) => (e) => {
    e?.preventDefault();
    run(async () => {
      const loan = await api.requestLoan({ ...form, days: Number(form.days) });
      if (approveNow) await api.approveLoan(loan.id);
      onSaved(loan);
    });
  };

  return (
    <form className="form" onSubmit={submit(false)}>
      <ErrorBox error={error || refs.error} />
      <Field label="Üye">
        <select value={form.memberId} onChange={set('memberId')} required disabled={Boolean(memberId)}>
          <option value="">Seçin…</option>
          {refs.data?.members.map((m) => <option key={m.id} value={m.id}>{m.firstName} {m.lastName} – {m.email}</option>)}
        </select>
      </Field>
      <Field label="Kitap (yalnızca müsait olanlar)">
        <select value={form.bookId} onChange={set('bookId')} required disabled={Boolean(bookId)}>
          <option value="">Seçin…</option>
          {refs.data?.books.map((b) => (
            <option key={b.id} value={b.id}>{b.title} – {b.author.name} ({b.availableCopies} müsait)</option>
          ))}
        </select>
      </Field>
      <div className="form-row">
        <Field label="Ödünç süresi (gün)"><input type="number" min="1" max="90" value={form.days} onChange={set('days')} required /></Field>
        <Field label="Not"><input value={form.note} onChange={set('note')} /></Field>
      </div>
      <div className="actions">
        <button className="btn" disabled={busy}>Onaya gönder</button>
        <button type="button" className="btn btn-primary" disabled={busy || !form.memberId || !form.bookId} onClick={submit(true)}>
          Ver ve onayla
        </button>
      </div>
    </form>
  );
}
