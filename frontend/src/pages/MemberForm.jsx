import { useState } from 'react';
import { api } from '../api.js';
import { ErrorBox, Field, useAction } from '../components.jsx';

const EMPTY = {
  firstName: '', lastName: '', email: '', phone: '', status: 'ACTIVE', emailNotifications: true,
  address: { street: '', district: '', city: '', postalCode: '', country: 'Türkiye' },
};

export default function MemberForm({ member, onSaved }) {
  const [form, setForm] = useState(() => (member ? { ...EMPTY, ...member, address: { ...EMPTY.address, ...member.address } } : EMPTY));
  const { busy, error, run } = useAction();

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value });
  const setAddr = (k) => (e) => setForm({ ...form, address: { ...form.address, [k]: e.target.value } });

  const submit = (e) => {
    e.preventDefault();
    run(async () => {
      const saved = member ? await api.updateMember(member.id, form) : await api.createMember(form);
      onSaved(saved);
    });
  };

  return (
    <form onSubmit={submit} className="form">
      <ErrorBox error={error} />
      <div className="form-row">
        <Field label="Ad"><input value={form.firstName} onChange={set('firstName')} required /></Field>
        <Field label="Soyad"><input value={form.lastName} onChange={set('lastName')} required /></Field>
      </div>
      <div className="form-row">
        <Field label="E-posta"><input type="email" value={form.email} onChange={set('email')} required /></Field>
        <Field label="Telefon"><input value={form.phone} onChange={set('phone')} required placeholder="+90 5xx xxx xx xx" /></Field>
      </div>
      <Field label="Adres"><input value={form.address.street} onChange={setAddr('street')} placeholder="Cadde, sokak, no" /></Field>
      <div className="form-row">
        <Field label="İlçe"><input value={form.address.district} onChange={setAddr('district')} /></Field>
        <Field label="Şehir"><input value={form.address.city} onChange={setAddr('city')} /></Field>
        <Field label="Posta kodu"><input value={form.address.postalCode} onChange={setAddr('postalCode')} /></Field>
      </div>
      <div className="form-row">
        <Field label="Durum">
          <select value={form.status} onChange={set('status')}>
            <option value="ACTIVE">Aktif</option>
            <option value="SUSPENDED">Askıda</option>
            <option value="PASSIVE">Pasif</option>
          </select>
        </Field>
        <label className="checkbox">
          <input type="checkbox" checked={form.emailNotifications} onChange={set('emailNotifications')} />
          İade tarihi yaklaşınca e-posta gönderilsin
        </label>
      </div>
      <button className="btn btn-primary" disabled={busy}>{busy ? 'Kaydediliyor…' : 'Kaydet'}</button>
    </form>
  );
}
