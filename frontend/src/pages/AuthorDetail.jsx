import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api.js';
import { ErrorBox, Loading, Modal, useLoad } from '../components.jsx';
import AuthorForm from './AuthorForm.jsx';
import { Availability } from './Books.jsx';

export default function AuthorDetail() {
  const { id } = useParams();
  const [editing, setEditing] = useState(false);
  const { data, error, loading, reload } = useLoad(() => api.author(id), [id]);

  if (loading && !data) return <Loading when />;
  if (!data) return <ErrorBox error={error} />;
  const { author: a, books } = data;
  const totalPages = books.reduce((s, b) => s + b.pageCount, 0);

  return (
    <>
      <div className="page-head">
        <h2>{a.name}</h2>
        <button className="btn" onClick={() => setEditing(true)}>Düzenle</button>
      </div>
      <section className="card">
        <dl className="dl">
          <dt>Yaşam</dt><dd>{a.birthYear ? `${a.birthYear}${a.deathYear ? ` – ${a.deathYear}` : ''}` : '—'}</dd>
          <dt>Uyruk</dt><dd>{a.nationality || '—'}</dd>
          {a.website && (<><dt>Web</dt><dd><a href={a.website} target="_blank" rel="noreferrer">{a.website}</a></dd></>)}
          <dt>Katalogdaki eser</dt><dd>{books.length} kitap · {totalPages.toLocaleString('tr-TR')} sayfa</dd>
        </dl>
        {a.biography && <p className="bio">{a.biography}</p>}
      </section>

      <section className="card">
        <h3>Yazarın kitapları</h3>
        {books.length === 0 && <p className="muted">Katalogda bu yazara ait kitap yok.</p>}
        {books.length > 0 && (
          <table>
            <thead><tr><th>Kitap</th><th>Yıl</th><th>Yayınevi</th><th>Kategori</th><th>Sayfa</th><th>Müsaitlik</th></tr></thead>
            <tbody>
              {books.map((b) => (
                <tr key={b.id}>
                  <td><Link to={`/books/${b.id}`}>{b.title}</Link></td>
                  <td>{b.publishYear || '—'}</td>
                  <td>{b.publisher.name}</td>
                  <td>{b.category}</td>
                  <td>{b.pageCount}</td>
                  <td><Availability book={b} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
      {editing && (
        <Modal title="Yazarı düzenle" onClose={() => setEditing(false)}>
          <AuthorForm author={a} onSaved={() => { setEditing(false); reload(); }} />
        </Modal>
      )}
    </>
  );
}
