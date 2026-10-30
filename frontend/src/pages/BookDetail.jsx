import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api.js';
import { ErrorBox, fmtDate, Loading, Modal, StatusBadge, useAction, useLoad } from '../components.jsx';
import BookForm from './BookForm.jsx';
import { Availability } from './Books.jsx';
import { NewLoanForm } from './Loans.jsx';

export default function BookDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [editing, setEditing] = useState(false);
  const [lending, setLending] = useState(false);
  const action = useAction();
  const { data, error, loading, reload } = useLoad(
    () => Promise.all([api.book(id), api.bookHolders(id)]).then(([book, holders]) => ({ book, holders })),
    [id]
  );

  if (loading && !data) return <Loading when />;
  if (!data) return <ErrorBox error={error} />;
  const { book: b, holders } = data;

  const remove = () => {
    if (!confirm(`"${b.title}" silinsin mi?`)) return;
    action.run(async () => { await api.deleteBook(b.id); navigate('/books'); });
  };

  return (
    <>
      <div className="page-head">
        <h2>{b.title}</h2>
        <div className="actions">
          <button className="btn" onClick={() => setEditing(true)}>Düzenle</button>
          <button className="btn btn-danger" onClick={remove}>Sil</button>
          <button className="btn btn-primary" onClick={() => setLending(true)} disabled={b.availableCopies === 0}>Ödünç ver</button>
        </div>
      </div>
      <ErrorBox error={action.error} />
      <div className="grid-2">
        <section className="card">
          <dl className="dl">
            <dt>Yazar</dt><dd><Link to={`/authors/${b.author.id}`}>{b.author.name}</Link></dd>
            <dt>Yayınevi</dt><dd>{b.publisher.name}</dd>
            <dt>Sayfa sayısı</dt><dd>{b.pageCount}</dd>
            <dt>Kategori</dt><dd>{b.category}</dd>
            <dt>Basım yılı</dt><dd>{b.publishYear || '—'}</dd>
            <dt>ISBN</dt><dd>{b.isbn || '—'}</dd>
            <dt>Müsaitlik</dt><dd><Availability book={b} /></dd>
          </dl>
          {b.description && <p>{b.description}</p>}
        </section>
        <section className="card">
          <h3>Kitap şu an kimde?</h3>
          {holders.length === 0 && <p className="muted">Ödünçte kopya yok.</p>}
          {holders.length > 0 && (
            <table>
              <thead><tr><th>Üye</th><th>Son tarih</th><th>Durum</th></tr></thead>
              <tbody>
                {holders.map((l) => (
                  <tr key={l.id}>
                    <td><Link to={`/members/${l.memberId}`}>{l.memberName}</Link></td>
                    <td>{fmtDate(l.dueDate)}</td>
                    <td><StatusBadge status={l.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
      </div>
      {editing && (
        <Modal title="Kitabı düzenle" onClose={() => setEditing(false)}>
          <BookForm book={b} onSaved={() => { setEditing(false); reload(); }} />
        </Modal>
      )}
      {lending && (
        <Modal title={`"${b.title}" ödünç ver`} onClose={() => setLending(false)}>
          <NewLoanForm bookId={b.id} onSaved={() => { setLending(false); reload(); }} />
        </Modal>
      )}
    </>
  );
}
