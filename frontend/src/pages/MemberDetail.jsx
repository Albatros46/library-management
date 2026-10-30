import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api.js';
import { daysUntil, ErrorBox, fmtDate, Loading, Modal, StatusBadge, useLoad } from '../components.jsx';
import MemberForm from './MemberForm.jsx';
import { MEMBER_STATUS } from './Members.jsx';
import { NewLoanForm } from './Loans.jsx';

export default function MemberDetail() {
  const { id } = useParams();
  const [editing, setEditing] = useState(false);
  const [lending, setLending] = useState(false);
  const { data, error, loading, reload } = useLoad(
    () => Promise.all([api.member(id), api.loans({ memberId: id }), api.memberStats(id)])
      .then(([member, loans, stats]) => ({ member, loans, stats })),
    [id]
  );

  if (loading && !data) return <Loading when />;
  if (!data) return <ErrorBox error={error} />;
  const { member: m, loans, stats } = data;
  const current = loans.filter((l) => ['ACTIVE', 'OVERDUE', 'RETURN_PENDING'].includes(l.status));
  const maxCategory = Math.max(1, ...stats.byCategory.map((c) => c.count));

  return (
    <>
      <div className="page-head">
        <h2>{m.firstName} {m.lastName}</h2>
        <div className="actions">
          <button className="btn" onClick={() => setEditing(true)}>Düzenle</button>
          <button className="btn btn-primary" onClick={() => setLending(true)} disabled={m.status !== 'ACTIVE'}>Kitap ver</button>
        </div>
      </div>
      <ErrorBox error={error} />

      <div className="grid-2">
        <section className="card">
          <h3>İletişim bilgileri</h3>
          <dl className="dl">
            <dt>E-posta</dt><dd><a href={`mailto:${m.email}`}>{m.email}</a></dd>
            <dt>Telefon</dt><dd><a href={`tel:${m.phone}`}>{m.phone}</a></dd>
            <dt>Adres</dt>
            <dd>
              {m.address ? (
                <>
                  {m.address.street}<br />
                  {[m.address.district, m.address.city, m.address.postalCode].filter(Boolean).join(', ')}<br />
                  {m.address.country}
                </>
              ) : '—'}
            </dd>
            <dt>Durum</dt><dd>{MEMBER_STATUS[m.status]}</dd>
            <dt>Hatırlatma e-postası</dt><dd>{m.emailNotifications ? 'Açık' : 'Kapalı'}</dd>
            <dt>Üyelik</dt><dd>{fmtDate(m.createdAt)}</dd>
          </dl>
        </section>

        <section className="card">
          <h3>Okuma özeti</h3>
          <div className="stats stats-small">
            <div className="stat"><div className="stat-value">{stats.totalBorrowed}</div><div className="stat-label">ödünç alınan kitap</div></div>
            <div className="stat"><div className="stat-value">{stats.totalPages.toLocaleString('tr-TR')}</div><div className="stat-label">toplam sayfa</div></div>
            <div className="stat"><div className="stat-value">{stats.currentlyBorrowed}</div><div className="stat-label">şu an elinde</div></div>
            <div className={`stat ${stats.overdue ? 'stat-danger' : ''}`}><div className="stat-value">{stats.overdue}</div><div className="stat-label">gecikmiş</div></div>
          </div>
          <h4>Alanlara göre</h4>
          {stats.byCategory.length === 0 && <p className="muted">Henüz ödünç alınmış kitap yok.</p>}
          {stats.byCategory.map((c) => (
            <div key={c.key} className="bar-row">
              <span className="bar-label">{c.name}</span>
              <span className="bar"><span style={{ width: `${(c.count / maxCategory) * 100}%` }} /></span>
              <span className="bar-value">{c.count} kitap · {c.pages} sf.</span>
            </div>
          ))}
          {stats.byAuthor.length > 0 && (
            <>
              <h4>En çok okunan yazarlar</h4>
              <p>
                {stats.byAuthor.slice(0, 5).map((a, i) => (
                  <span key={a.key}>{i > 0 && ', '}<Link to={`/authors/${a.key}`}>{a.name}</Link> ({a.count})</span>
                ))}
              </p>
            </>
          )}
          <p className="muted small">Not: Bu rapor ödünç alınan kitapların kategorilerine dayanır, kitabın okunup okunmadığını göstermez.</p>
        </section>
      </div>

      <section className="card">
        <h3>Üyedeki kitaplar ({current.length})</h3>
        <LoanTable loans={current} showDue />
      </section>

      <section className="card">
        <h3>Ödünç geçmişi</h3>
        <LoanTable loans={loans} />
      </section>

      {editing && (
        <Modal title="Üyeyi düzenle" onClose={() => setEditing(false)}>
          <MemberForm member={m} onSaved={() => { setEditing(false); reload(); }} />
        </Modal>
      )}
      {lending && (
        <Modal title={`${m.firstName} ${m.lastName} – kitap ver`} onClose={() => setLending(false)}>
          <NewLoanForm memberId={m.id} onSaved={() => { setLending(false); reload(); }} />
        </Modal>
      )}
    </>
  );
}

function LoanTable({ loans, showDue }) {
  if (!loans.length) return <p className="muted">Kayıt yok.</p>;
  return (
    <table>
      <thead>
        <tr><th>Kitap</th><th>Yazar</th><th>Alan</th><th>Veriliş</th><th>Son tarih</th><th>İade</th><th>Durum</th></tr>
      </thead>
      <tbody>
        {loans.map((l) => {
          const left = l.dueDate ? daysUntil(l.dueDate) : null;
          return (
            <tr key={l.id}>
              <td><Link to={`/books/${l.bookId}`}>{l.bookTitle}</Link></td>
              <td>{l.authorId ? <Link to={`/authors/${l.authorId}`}>{l.authorName}</Link> : l.authorName}</td>
              <td>{l.category}</td>
              <td>{fmtDate(l.loanDate)}</td>
              <td>
                {fmtDate(l.dueDate)}
                {showDue && left !== null && (
                  <span className={left < 0 ? 'text-danger' : 'muted'}> ({left < 0 ? `${-left} gün gecikti` : `${left} gün kaldı`})</span>
                )}
              </td>
              <td>{fmtDate(l.returnDate)}</td>
              <td><StatusBadge status={l.status} /></td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}
