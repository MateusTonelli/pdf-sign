import { useMutation, useQuery } from '@tanstack/react-query';
import { FormEvent, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import * as api from '../api/endpoints';
import { SignatureCanvas } from '../components/SignatureCanvas';
import { downloadBlob, getErrorMessage } from '../utils/format';

export function SignPublicPage() {
  const { token } = useParams<{ token: string }>();
  const [typedName, setTypedName] = useState('');
  const [signatureImage, setSignatureImage] = useState<string | undefined>();
  const [pfxPassword, setPfxPassword] = useState('');
  const [done, setDone] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const sessionQuery = useQuery({
    queryKey: ['sign-session', token],
    queryFn: () => api.getSignSession(token!),
    enabled: !!token,
  });

  const simpleMutation = useMutation({
    mutationFn: () =>
      api.signSimple(token!, {
        typedName: typedName.trim(),
        signatureImageBase64: signatureImage,
      }),
    onSuccess: () => {
      setDone(true);
      setError(null);
    },
    onError: (err) => setError(getErrorMessage(err)),
  });

  const certMutation = useMutation({
    mutationFn: () => api.signCertificate(token!, pfxPassword),
    onSuccess: () => {
      setDone(true);
      setError(null);
      setPfxPassword('');
    },
    onError: (err) => setError(getErrorMessage(err)),
  });

  const downloadMutation = useMutation({
    mutationFn: async () => {
      const blob = await api.downloadSignedDocument(token!);
      const title = sessionQuery.data?.documentTitle ?? 'documento-assinado';
      downloadBlob(blob, `${title}.pdf`);
    },
    onError: (err) => setError(getErrorMessage(err)),
  });

  async function handleSimpleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!typedName.trim()) {
      setError('Informe seu nome completo.');
      return;
    }
    simpleMutation.mutate();
  }

  async function handleCertSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!pfxPassword) {
      setError('Informe a senha do certificado .pfx.');
      return;
    }
    certMutation.mutate();
  }

  if (!token) {
    return (
      <div className="auth-layout">
        <div className="alert alert-error">Link de assinatura inválido.</div>
      </div>
    );
  }

  const session = sessionQuery.data;
  const isCert = session?.signatureType === 'CERTIFICATE';

  return (
    <div className="auth-layout" style={{ alignItems: 'flex-start', paddingTop: '2rem' }}>
      <div style={{ width: 'min(100%, 560px)' }} className="stack">
        <div className="page-hero">
          <Link to="/" className="brand">
            <span className="brand-mark" aria-hidden>
              AP
            </span>
            AssinaPDF
          </Link>
          <h1 style={{ marginTop: '1rem' }}>Assinar documento</h1>
          <p className="muted" style={{ marginBottom: 0 }}>
            Você recebeu um convite para assinar um PDF.
          </p>
        </div>

        {sessionQuery.isLoading && (
          <div className="card loading-center">
            <span className="spinner" aria-hidden />
            Carregando convite…
          </div>
        )}

        {sessionQuery.error && (
          <div className="alert alert-error">{getErrorMessage(sessionQuery.error)}</div>
        )}

        {session && (
          <div className="card stack">
            <div>
              <h2 className="card-title" style={{ marginBottom: '0.25rem' }}>
                {session.documentTitle ?? session.fileName ?? 'Documento'}
              </h2>
              <p className="muted" style={{ margin: 0 }}>
                Signatário: {session.signerName ?? '—'} ({session.signerEmail ?? '—'})
              </p>
            </div>

            {session.alreadySigned && !done && (
              <div className="alert alert-info">
                Este convite já foi utilizado. Você pode baixar a cópia assinada abaixo.
              </div>
            )}

            {done && (
              <div className="alert alert-success">
                Assinatura registrada com sucesso. O remetente será notificado.
              </div>
            )}

            {error && (
              <div className="alert alert-error" role="alert">
                {error}
              </div>
            )}

            {!done && !session.alreadySigned && !isCert && (
              <form className="stack" onSubmit={handleSimpleSubmit}>
                <div className="form-field">
                  <label htmlFor="typedName">Nome completo (como consta no documento)</label>
                  <input
                    id="typedName"
                    required
                    value={typedName}
                    onChange={(e) => setTypedName(e.target.value)}
                    placeholder={session.signerName ?? ''}
                  />
                </div>
                <div>
                  <p className="muted" style={{ marginBottom: '0.5rem' }}>
                    Rubrica manual (opcional)
                  </p>
                  <SignatureCanvas onChange={setSignatureImage} />
                </div>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={simpleMutation.isPending}
                >
                  {simpleMutation.isPending ? 'Assinando…' : 'Confirmar assinatura'}
                </button>
              </form>
            )}

            {!done && !session.alreadySigned && isCert && !session.canSignWithCertificate && (
              <div className="empty-state stack" style={{ padding: '1rem' }}>
                <div className="alert alert-warning" style={{ textAlign: 'left' }}>
                  <strong>Certificado digital necessário</strong>
                  <p style={{ margin: '0.5rem 0 0' }}>
                    Este documento exige assinatura ICP-Brasil. Para continuar:
                  </p>
                  <ol style={{ margin: '0.5rem 0 0', paddingLeft: '1.25rem' }}>
                    <li>
                      Crie uma conta com o mesmo e-mail do convite (
                      {session.signerEmail}).
                    </li>
                    <li>Cadastre seu certificado A1 (.pfx) em Configurações.</li>
                    <li>Retorne a este link e informe a senha do certificado.</li>
                  </ol>
                </div>
                <Link to="/register" className="btn btn-primary">
                  Criar conta
                </Link>
                <Link to="/login" className="btn btn-secondary">
                  Já tenho conta — entrar
                </Link>
              </div>
            )}

            {!done && !session.alreadySigned && isCert && session.canSignWithCertificate && (
              <form className="stack" onSubmit={handleCertSubmit}>
                <div className="alert alert-info">
                  Será usado o certificado A1 cadastrado na sua conta para este e-mail.
                </div>
                <div className="form-field">
                  <label htmlFor="pfxPassword">Senha do certificado (.pfx)</label>
                  <input
                    id="pfxPassword"
                    type="password"
                    required
                    autoComplete="off"
                    value={pfxPassword}
                    onChange={(e) => setPfxPassword(e.target.value)}
                  />
                </div>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={certMutation.isPending}
                >
                  {certMutation.isPending ? 'Assinando…' : 'Assinar com certificado'}
                </button>
              </form>
            )}

            {(done || session.alreadySigned) && (
              <button
                type="button"
                className="btn btn-secondary"
                disabled={downloadMutation.isPending}
                onClick={() => downloadMutation.mutate()}
              >
                {downloadMutation.isPending ? 'Preparando…' : 'Baixar PDF'}
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
