import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { daysUntil, ErrorBox, fmtDate, Loading, StatusBadge, useLoad } from '../components.jsx';

export default function Dashboard() {
  const { data, error, loading } = useLoad(() =>
    Promise.all([api.pendingLoans(), api.activeLoans(), api.overdueLoans(), api.members(), api.books()])
      .then(([pending, active, overdue, members, books]) => ({ pending, active, overdue, members, books }))
  );

  return (
    <>
      <h2>Genel Bakış</h2>
      <ErrorBox error={error} />
      <Loading when={loading} />
      {data && (
        <>
          <div className="stats">
            <Stat label="Onay bekleyen işlem" value={data.pending.length} to="/loans" tone="warn" />
            <Stat label="Ödünçteki kitap" value={data.active.length} to="/loans" />
            <Stat label="Gecikmiş ödünç" value={data.overdue.length} to="/overdue" tone="danger" />
            <Stat label="Üye" value={data.members.length} to="/members" />
            <Stat label="Kitap başlığı" value={data.books.length} to="/books" />
          </div>

          <div className="grid-2">
            <section className="card">
              <h3>Onay bekleyenler</h3>
              <LoanMiniTable loans={data.pending.slice(0, 6)} empty="Onay bekleyen işlem yok." />
              <Link to="/loans">Tümü →</Link>
            </section>
            <section className="card">
              <h3>Son tarihi geçenler</h3>
              <LoanMiniTable loans={data.overdue.slice(0, 6)} empty="Geciken ödünç yok. 🎉" />
              <Link to="/overdue">Tümü →</Link>
            </section>
          </div>
        </>
      )}
    </>
  );
}

function Stat({ label, value, to, tone }) {
  return (
    <Link to={to} className={`stat ${tone ? `stat-${tone}` : ''}`}>
      <div className="stat-value">{value}</div>
      <div className="stat-label">{label}</div>
    </Link>
  );
}

function LoanMiniTable({ loans, empty }) {
  if (!loans.length) return <p className="muted">{empty}</p>;
  return (
    <table>
      <tbody>
        {loans.map((l) => (
          <tr key={l.id}>
            <td><Link to={`/members/${l.memberId}`}>{l.memberName}</Link></td>
            <td><Link to={`/books/${l.bookId}`}>{l.bookTitle}</Link></td>
            <td>
              {fmtDate(l.dueDate)}
              {l.status === 'OVERDUE' && <span className="text-danger"> ({-daysUntil(l.dueDate)} gün)</span>}
            </td>
            <td><StatusBadge status={l.status} /></td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
