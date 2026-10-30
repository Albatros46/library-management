import { useState } from 'react';
import { api } from '../api.js';
import { ErrorBox, Field, useAction } from '../components.jsx';

export default function AuthorForm({ author, onSaved }) {
  const [form, setForm] = useState(author || { name: '', biography: '', birthYear: '', deathYear: '', nationality: '', website: '' });
  const { busy, error, run } = useAction();
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = (e) => {
    e.preventDefault();
    const body = {
      ...form,
      birthYear: form.birthYear ? Number(form.birthYear) : null,
      deathYear: form.deathYear ? Number(form.deathYear) : null,
    };
    run(async () => onSaved(author ? await api.updateAuthor(author.id, body) : await api.createAuthor(body)));
  };

  return (
    <form className="form" onSubmit={submit}>
      <ErrorBox error={error} />
      <Field label="Ad Soyad"><input value={form.name} onChange={set('name')} required /></Field>
      <div className="form-row">
        <Field label="Doğum yılı"><input type="number" value={form.birthYear || ''} onChange={set('birthYear')} /></Field>
        <Field label="Ölüm yılı"><input type="number" value={form.deathYear || ''} onChange={set('deathYear')} /></Field>
        <Field label="Uyruk"><input value={form.nationality || ''} onChange={set('nationality')} /></Field>
      </div>
      <Field label="Web sitesi"><input type="url" value={form.website || ''} onChange={set('website')} /></Field>
      <Field label="Biyografi"><textarea rows="5" value={form.biography || ''} onChange={set('biography')} /></Field>
      <button className="btn btn-primary" disabled={busy}>{busy ? 'Kaydediliyor…' : 'Kaydet'}</button>
    </form>
  );
}
