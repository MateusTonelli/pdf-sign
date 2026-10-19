import { FormEvent, useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { getErrorMessage } from '../utils/format';

export function RegisterPage() {
  const { register, isAuthenticated, isLoading } = useAuth();
  const navigate = useNavigate();

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [companyName, setCompanyName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (!isLoading && isAuthenticated) {
    return <Navigate to="/app" replace />;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await register({
        name: name.trim(),
        email: email.trim(),
        password,
        companyName: companyName.trim(),
      });
      navigate('/app', { replace: true });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-layout">
      <div className="card auth-card">
        <h1>Criar conta</h1>
        <p className="muted">Cadastre sua empresa para enviar documentos.</p>
        {error && (
          <div className="alert alert-error" role="alert">
            {error}
          </div>
        )}
        <form className="stack" onSubmit={handleSubmit}>
          <div className="form-field">
            <label htmlFor="name">Nome completo</label>
            <input
              id="name"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
          </div>
          <div className="form-field">
            <label htmlFor="email">E-mail</label>
            <input
              id="email"
              type="email"
              autoComplete="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>
          <div className="form-field">
            <label htmlFor="companyName">Nome da empresa</label>
            <input
              id="companyName"
              required
              value={companyName}
              onChange={(e) => setCompanyName(e.target.value)}
            />
          </div>
          <div className="form-field">
            <label htmlFor="password">Senha</label>
            <input
              id="password"
              type="password"
              autoComplete="new-password"
              required
              minLength={8}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Criando…' : 'Registrar'}
          </button>
        </form>
        <p className="muted" style={{ marginTop: '1rem', marginBottom: 0 }}>
          Já tem conta? <Link to="/login">Entrar</Link>
        </p>
      </div>
    </div>
  );
}
