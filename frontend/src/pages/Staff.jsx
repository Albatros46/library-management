import { useState } from 'react';
import { api } from '../api.js';
import { ErrorBox, Field, Loading, Modal, useAction, useLoad } from '../components.jsx';

export default function Staff() {
  const { data, error, loading, reload } = useLoad(() => api.staff(), []);
  const [creating, setCreating] = useState(false);
  const action = useAction();

  return (
    <>
      <div className="page-head">
        <h2>Personel</h2>
        <button className="btn btn-primary" onClick={() => setCreating(true)}>+ Yeni personel</button>
      </div>
      <ErrorBox error={error || action.error} />
      <Loading when={loading && !data} />
      {data && (
        <div className="card">
          <table>
            <thead><tr><th>Ad Soyad</th><th>E-posta</th><th>Rol</th><th>Durum</th><th></th></tr></thead>
            <tbody>
              {data.map((u) => (
                <tr key={u.id}>
                  <td>{u.fullName}</td>
                  <td>{u.email}</td>
                  <td>{u.role === 'ADMIN' ? 'Yönetici' : 'Personel'}</td>
                  <td>{u.active ? 'Aktif' : 'Pasif'}</td>
                  <td>
                    <button className="btn btn-sm" disabled={action.busy}
                            onClick={() => action.run(async () => { await api.setStaffActive(u.id, !u.active); await reload(); })}>
                      {u.active ? 'Pasifleştir' : 'Aktifleştir'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {creating && (
        <Modal title="Yeni personel" onClose={() => setCreating(false)}>
          <StaffForm onSaved={() => { setCreating(false); reload(); }} />
        </Modal>
      )}
    </>
  );
}

function StaffForm({ onSaved }) {
  const [form, setForm] = useState({ fullName: '', email: '', password: '', role: 'STAFF' });
  const { busy, error, run } = useAction();
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  return (
    <form className="form" onSubmit={(e) => { e.preventDefault(); run(async () => onSaved(await api.createStaff(form))); }}>
      <ErrorBox error={error} />
      <Field label="Ad Soyad"><input value={form.fullName} onChange={set('fullName')} required /></Field>
      <Field label="E-posta"><input type="email" value={form.email} onChange={set('email')} required /></Field>
      <div className="form-row">
        <Field label="Parola (en az 6 karakter)"><input type="password" minLength="6" value={form.password} onChange={set('password')} required /></Field>
        <Field label="Rol">
          <select value={form.role} onChange={set('role')}>
            <option value="STAFF">Personel</option>
            <option value="ADMIN">Yönetici</option>
          </select>
        </Field>
      </div>
      <button className="btn btn-primary" disabled={busy}>Kaydet</button>
    </form>
  );
}
