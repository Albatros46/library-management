import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { ErrorBox, Loading, Modal, useLoad } from '../components.jsx';
import AuthorForm from './AuthorForm.jsx';

export default function Authors() {
  const [q, setQ] = useState('');
  const [search, setSearch] = useState('');
  const [creating, setCreating] = useState(false);
  const { data, error, loading, reload } = useLoad(() => api.authors(search), [search]);

  return (
    <>
      <div className="page-head">
        <h2>Yazarlar</h2>
        <button className="btn btn-primary" onClick={() => setCreating(true)}>+ Yeni yazar</button>
      </div>
      <form className="toolbar" onSubmit={(e) => { e.preventDefault(); setSearch(q); }}>
        <input placeholder="Yazar adı ara" value={q} onChange={(e) => setQ(e.target.value)} />
        <button className="btn">Ara</button>
      </form>
      <ErrorBox error={error} />
      <Loading when={loading && !data} />
      {data && (
        <div className="cards">
          {data.map((a) => (
            <Link key={a.id} to={`/authors/${a.id}`} className="card author-card">
              <strong>{a.name}</strong>
              <span className="muted small">
                {[a.nationality, a.birthYear && `${a.birthYear}${a.deathYear ? `–${a.deathYear}` : ''}`].filter(Boolean).join(' · ')}
              </span>
            </Link>
          ))}
          {!data.length && <p className="muted">Yazar bulunamadı.</p>}
        </div>
      )}
      {creating && (
        <Modal title="Yeni yazar" onClose={() => setCreating(false)}>
          <AuthorForm onSaved={() => { setCreating(false); reload(); }} />
        </Modal>
      )}
    </>
  );
}
