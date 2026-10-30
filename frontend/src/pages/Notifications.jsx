import { api } from '../api.js';
import { ErrorBox, fmtDateTime, Loading, useAction, useLoad } from '../components.jsx';

const TYPES = { DUE_SOON: 'İade tarihi yaklaşıyor', OVERDUE: 'Gecikme (otomatik)', MANUAL_REMINDER: 'Gecikme (personel)' };

export default function Notifications() {
  const { data, error, loading, reload } = useLoad(() => api.notifications(), []);
  const action = useAction();

  const runNow = () => action.run(async () => {
    const r = await api.runNotifications();
    alert(`Gönderildi: ${r.dueSoonSent} yaklaşan, ${r.overdueSent} gecikme. Hatalı: ${r.failed}`);
    await reload();
  });

  return (
    <>
      <div className="page-head">
        <h2>Bildirimler</h2>
        <button className="btn btn-primary" onClick={runNow} disabled={action.busy}>Bildirimleri şimdi çalıştır</button>
      </div>
      <ErrorBox error={error || action.error} />
      <Loading when={loading && !data} />
      {data && (
        <div className="card">
          <table>
            <thead><tr><th>Zaman</th><th>Tür</th><th>Alıcı</th><th>Konu</th><th>Sonuç</th></tr></thead>
            <tbody>
              {data.map((n) => (
                <tr key={n.id}>
                  <td>{fmtDateTime(n.sentAt)}</td>
                  <td>{TYPES[n.type]}{n.triggeredBy && <div className="muted small">{n.triggeredBy}</div>}</td>
                  <td>{n.recipient}</td>
                  <td>{n.subject}</td>
                  <td>{n.success ? <span className="badge badge-active">Gönderildi</span> : <span className="badge badge-overdue" title={n.error}>Hata</span>}</td>
                </tr>
              ))}
              {!data.length && <tr><td colSpan="5" className="muted">Henüz bildirim yok.</td></tr>}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
