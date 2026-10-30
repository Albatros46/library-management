import { useState } from 'react';
import { api } from '../api.js';
import { ErrorBox, Field, useAction, useLoad } from '../components.jsx';

export default function BookForm({ book, onSaved }) {
  const [form, setForm] = useState(() => book
    ? { ...book, authorId: book.author.id, publisherId: book.publisher.id }
    : { title: '', isbn: '', authorId: '', publisherId: '', pageCount: '', category: '', publishYear: '', description: '', totalCopies: 1 });
  const refs = useLoad(() => Promise.all([api.authors(), api.publishers(), api.categories()])
    .then(([authors, publishers, categories]) => ({ authors, publishers, categories })), []);
  const { busy, error, run } = useAction();
  const [newPublisher, setNewPublisher] = useState('');

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const addPublisher = () => run(async () => {
    const p = await api.createPublisher({ name: newPublisher });
    await refs.reload();
    setForm({ ...form, publisherId: p.id });
    setNewPublisher('');
  });

  const submit = (e) => {
    e.preventDefault();
    const body = {
      ...form,
      pageCount: Number(form.pageCount),
      totalCopies: Number(form.totalCopies),
      publishYear: form.publishYear ? Number(form.publishYear) : null,
    };
    run(async () => onSaved(book ? await api.updateBook(book.id, body) : await api.createBook(body)));
  };

  return (
    <form className="form" onSubmit={submit}>
      <ErrorBox error={error || refs.error} />
      <Field label="Kitap adı"><input value={form.title} onChange={set('title')} required /></Field>
      <div className="form-row">
        <Field label="Yazar">
          <select value={form.authorId} onChange={set('authorId')} required>
            <option value="">Seçin…</option>
            {refs.data?.authors.map((a) => <option key={a.id} value={a.id}>{a.name}</option>)}
          </select>
        </Field>
        <Field label="Yayınevi">
          <select value={form.publisherId} onChange={set('publisherId')} required>
            <option value="">Seçin…</option>
            {refs.data?.publishers.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
          </select>
        </Field>
      </div>
      <div className="inline-add">
        <input placeholder="Listede yoksa yeni yayınevi adı" value={newPublisher} onChange={(e) => setNewPublisher(e.target.value)} />
        <button type="button" className="btn" disabled={!newPublisher.trim() || busy} onClick={addPublisher}>Yayınevi ekle</button>
      </div>
      <div className="form-row">
        <Field label="Kategori / alan">
          <input list="categories" value={form.category} onChange={set('category')} required />
          <datalist id="categories">{refs.data?.categories.map((c) => <option key={c} value={c} />)}</datalist>
        </Field>
        <Field label="Sayfa sayısı"><input type="number" min="1" value={form.pageCount} onChange={set('pageCount')} required /></Field>
      </div>
      <div className="form-row">
        <Field label="ISBN"><input value={form.isbn || ''} onChange={set('isbn')} /></Field>
        <Field label="Basım yılı"><input type="number" value={form.publishYear || ''} onChange={set('publishYear')} /></Field>
        <Field label="Kopya sayısı"><input type="number" min="0" value={form.totalCopies} onChange={set('totalCopies')} required /></Field>
      </div>
      <Field label="Açıklama"><textarea rows="3" value={form.description || ''} onChange={set('description')} /></Field>
      <button className="btn btn-primary" disabled={busy}>{busy ? 'Kaydediliyor…' : 'Kaydet'}</button>
    </form>
  );
}
