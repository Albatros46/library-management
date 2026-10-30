import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { ErrorBox, Loading, Modal, useLoad } from '../components.jsx';
import BookForm from './BookForm.jsx';

export default function Books() {
  const [q, setQ] = useState('');
  const [filters, setFilters] = useState({ q: '', category: '', availableOnly: false });
  const [creating, setCreating] = useState(false);
  const { data, error, loading, reload } = useLoad(() => api.books(filters), [filters]);
  const categories = useLoad(() => api.categories(), []);

  return (
    <>
      <div className="page-head">
        <h2>Kitap Kataloğu</h2>
        <button className="btn btn-primary" onClick={() => setCreating(true)}>+ Yeni kitap</button>
      </div>
      <form className="toolbar" onSubmit={(e) => { e.preventDefault(); setFilters({ ...filters, q }); }}>
        <input placeholder="Kitap adı, ISBN veya yazar ara" value={q} onChange={(e) => setQ(e.target.value)} />
        <select value={filters.category} onChange={(e) => setFilters({ ...filters, category: e.target.value })}>
          <option value="">Tüm kategoriler</option>
          {categories.data?.map((c) => <option key={c}>{c}</option>)}
        </select>
        <label className="checkbox">
          <input type="checkbox" checked={filters.availableOnly}
                 onChange={(e) => setFilters({ ...filters, availableOnly: e.target.checked })} />
          Yalnızca müsait
        </label>
        <button className="btn">Ara</button>
      </form>
      <ErrorBox error={error} />
      <Loading when={loading && !data} />
      {data && (
        <div className="card">
          <table>
            <thead>
              <tr><th>Kitap</th><th>Yazar</th><th>Yayınevi</th><th>Kategori</th><th>Sayfa</th><th>Müsaitlik</th></tr>
            </thead>
            <tbody>
              {data.map((b) => (
                <tr key={b.id}>
                  <td><Link to={`/books/${b.id}`}>{b.title}</Link>{b.publishYear && <span className="muted"> ({b.publishYear})</span>}</td>
                  <td><Link to={`/authors/${b.author.id}`}>{b.author.name}</Link></td>
                  <td>{b.publisher.name}</td>
                  <td>{b.category}</td>
                  <td>{b.pageCount}</td>
                  <td><Availability book={b} /></td>
                </tr>
              ))}
              {!data.length && <tr><td colSpan="6" className="muted">Kitap bulunamadı.</td></tr>}
            </tbody>
          </table>
        </div>
      )}
      {creating && (
        <Modal title="Yeni kitap" onClose={() => setCreating(false)}>
          <BookForm onSaved={() => { setCreating(false); reload(); categories.reload(); }} />
        </Modal>
      )}
    </>
  );
}

export function Availability({ book }) {
  const ok = book.availableCopies > 0;
  return (
    <span className={`badge ${ok ? 'badge-active' : 'badge-overdue'}`}>
      {ok ? `${book.availableCopies}/${book.totalCopies} müsait` : 'Tümü ödünçte'}
    </span>
  );
}
