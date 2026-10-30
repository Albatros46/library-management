import { useState } from 'react';
import { useAuth } from '../auth.jsx';
import { ErrorBox, Field, useAction } from '../components.jsx';

export default function Login() {
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const { busy, error, run } = useAction();

  const submit = (e) => {
    e.preventDefault();
    run(() => login(email, password));
  };

  return (
    <div className="login-page">
      <form className="card login-card" onSubmit={submit}>
        <h1>📚 Kütüphane Yönetimi</h1>
        <p className="muted">Personel girişi</p>
        <ErrorBox error={error} />
        <Field label="E-posta">
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus />
        </Field>
        <Field label="Parola">
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </Field>
        <button className="btn btn-primary" disabled={busy}>{busy ? 'Giriş yapılıyor…' : 'Giriş yap'}</button>
      </form>
    </div>
  );
}
