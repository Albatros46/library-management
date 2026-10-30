import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { ErrorBox, Loading, Modal, useLoad } from '../components.jsx';
import MemberForm from './MemberForm.jsx';

const MEMBER_STATUS = { ACTIVE: 'Aktif', SUSPENDED: 'Askıda', PASSIVE: 'Pasif' };

export default function Members() {
  const [q, setQ] = useState('');
  const [search, setSearch] = useState('');
  const [creating, setCreating] = useState(false);
  const { data, error, loading, reload } = useLoad(() => api.members(search), [search]);

  return (
    <>
      <div className="page-head">
        <h2>Üyeler</h2>
        <button className="btn btn-primary" onClick={() => setCreating(true)}>+ Yeni üye</button>
      </div>
      <form className="toolbar" onSubmit={(e) => { e.preventDefault(); setSearch(q); }}>
        <input placeholder="Ad, soyad, e-posta veya telefon ara" value={q} onChange={(e) => setQ(e.target.value)} />
        <button className="btn">Ara</button>
      </form>
      <ErrorBox error={error} />
      <Loading when={loading && !data} />
      {data && (
        <div className="card">
          <table>
            <thead>
              <tr><th>Ad Soyad</th><th>E-posta</th><th>Telefon</th><th>Şehir</th><th>Durum</th></tr>
            </thead>
            <tbody>
              {data.map((m) => (
                <tr key={m.id}>
                  <td><Link to={`/members/${m.id}`}>{m.firstName} {m.lastName}</Link></td>
                  <td>{m.email}</td>
                  <td>{m.phone}</td>
                  <td>{m.address?.city || '—'}</td>
                  <td><span className={`badge badge-member-${m.status.toLowerCase()}`}>{MEMBER_STATUS[m.status]}</span></td>
                </tr>
              ))}
              {!data.length && <tr><td colSpan="5" className="muted">Üye bulunamadı.</td></tr>}
            </tbody>
          </table>
        </div>
      )}
      {creating && (
        <Modal title="Yeni üye" onClose={() => setCreating(false)}>
          <MemberForm onSaved={() => { setCreating(false); reload(); }} />
        </Modal>
      )}
    </>
  );
}

export { MEMBER_STATUS };
