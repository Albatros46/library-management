import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { daysUntil, ErrorBox, fmtDate, fmtDateTime, Loading, useAction, useLoad } from '../components.jsx';

export default function Overdue() {
  const { data, error, loading, reload } = useLoad(async () => {
    const loans = await api.overdueLoans();
    const logs = loans.length ? await api.notifications({ loanIds: loans.map((l) => l.id) }) : [];
    return { loans, logs };
  }, []);
  const action = useAction();

  const remind = (loan) => action.run(async () => {
    const log = await api.sendReminder(loan.id);
    if (!log.success) throw new Error(`E-posta gönderilemedi: ${log.error}`);
    await reload();
  });
  const takeBack = (loan) => action.run(async () => { await api.approveReturn(loan.id); await reload(); });

  const lastLog = (loanId) => data.logs.find((n) => n.loanId === loanId);

  return (
    <>
      <div className="page-head">
        <h2>Gecikenler</h2>
        <button className="btn" onClick={reload} disabled={loading}>Yenile</button>
      </div>
      <p className="muted">
        İade tarihi geçmiş ödünçler. Sistem bu üyelere her gün 09:00'da otomatik iade hatırlatması gönderir
        (aynı ödünç için en fazla 3 günde bir); buradan elle de gönderebilirsiniz.
      </p>
      <ErrorBox error={error || action.error} />
      <Loading when={loading && !data} />
      {data && (
        <div className="card">
          <table>
            <thead>
              <tr><th>Üye</th><th>İletişim</th><th>Kitap</th><th>Son tarih</th><th>Gecikme</th><th>Hatırlatma</th><th></th></tr>
            </thead>
            <tbody>
              {data.loans.map((l) => {
                const log = lastLog(l.id);
                return (
                  <tr key={l.id} className="row-danger">
                    <td><Link to={`/members/${l.memberId}`}>{l.memberName}</Link></td>
                    <td className="small">{l.memberEmail}</td>
                    <td><Link to={`/books/${l.bookId}`}>{l.bookTitle}</Link></td>
                    <td>{fmtDate(l.dueDate)}</td>
                    <td className="text-danger"><strong>{-daysUntil(l.dueDate)} gün</strong></td>
                    <td className="small">
                      {l.reminderCount > 0 ? `${l.reminderCount} kez, son: ${fmtDateTime(l.lastReminderAt)}` : 'Henüz gönderilmedi'}
                      {log && !log.success && <div className="text-danger">Son deneme başarısız</div>}
                    </td>
                    <td className="actions">
                      <button className="btn btn-sm" disabled={action.busy} onClick={() => remind(l)}>Hatırlatma gönder</button>
                      <button className="btn btn-primary btn-sm" disabled={action.busy} onClick={() => takeBack(l)}>İade al</button>
                    </td>
                  </tr>
                );
              })}
              {!data.loans.length && <tr><td colSpan="7" className="muted">Süresi geçmiş ödünç yok. 🎉</td></tr>}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
