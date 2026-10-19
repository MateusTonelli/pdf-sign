import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FormEvent, useState } from 'react';
import * as api from '../api/endpoints';
import { formatDate, getErrorMessage } from '../utils/format';

export function CertificateSettingsPage() {
  const queryClient = useQueryClient();
  const [file, setFile] = useState<File | null>(null);
  const [password, setPassword] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const [formSuccess, setFormSuccess] = useState<string | null>(null);

  const { data, isLoading, error } = useQuery({
    queryKey: ['certificates'],
    queryFn: api.listCertificates,
  });

  const uploadMutation = useMutation({
    mutationFn: async () => {
      if (!file) throw new Error('Selecione o arquivo .pfx.');
      if (!password) throw new Error('Informe a senha do certificado.');
      return api.uploadCertificate(file, password);
    },
    onSuccess: async () => {
      setFile(null);
      setPassword('');
      setFormSuccess('Certificado cadastrado com sucesso.');
      setFormError(null);
      await queryClient.invalidateQueries({ queryKey: ['certificates'] });
    },
    onError: (err) => {
      setFormSuccess(null);
      setFormError(getErrorMessage(err));
    },
  });

  const revokeMutation = useMutation({
    mutationFn: (id: string) => api.revokeCertificate(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['certificates'] });
    },
  });

  async function handleUpload(e: FormEvent) {
    e.preventDefault();
    setFormError(null);
    setFormSuccess(null);
    uploadMutation.mutate();
  }

  const active = (data ?? []).filter((c) => !c.revokedAt && c.active !== false);
  const revoked = (data ?? []).filter((c) => c.revokedAt || c.active === false);

  return (
    <div>
      <div className="page-hero">
        <h1>Certificado digital</h1>
        <p className="muted" style={{ marginBottom: 0 }}>
          Gerencie seu certificado A1 (.pfx) para assinar documentos ICP-Brasil.
        </p>
      </div>

      <div className="card stack">
        <h2 className="card-title">Enviar certificado A1</h2>
        <p className="muted">
          O arquivo permanece criptografado no servidor. Use a senha definida na
          exportação do .pfx.
        </p>
        {formError && <div className="alert alert-error">{formError}</div>}
        {formSuccess && <div className="alert alert-success">{formSuccess}</div>}
        <form className="stack" onSubmit={handleUpload}>
          <div className="form-field">
            <label htmlFor="pfx">Arquivo .pfx</label>
            <input
              id="pfx"
              type="file"
              accept=".pfx,.p12,application/x-pkcs12"
              required
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            />
          </div>
          <div className="form-field">
            <label htmlFor="pfx-password">Senha do certificado</label>
            <input
              id="pfx-password"
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="off"
            />
          </div>
          <button
            type="submit"
            className="btn btn-primary"
            disabled={uploadMutation.isPending}
          >
            {uploadMutation.isPending ? 'Enviando…' : 'Cadastrar certificado'}
          </button>
        </form>
      </div>

      <div className="card">
        <h2 className="card-title">Certificados ativos</h2>
        {isLoading && (
          <div className="loading-center">
            <span className="spinner" aria-hidden />
            Carregando…
          </div>
        )}
        {error && <div className="alert alert-error">{getErrorMessage(error)}</div>}
        {!isLoading && active.length === 0 && (
          <p className="muted">Nenhum certificado ativo.</p>
        )}
        {active.length > 0 && (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Titular</th>
                  <th>Válido de</th>
                  <th>Válido até</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {active.map((cert) => (
                  <tr key={cert.id}>
                    <td>{cert.subject ?? cert.id}</td>
                    <td>{formatDate(cert.validFrom)}</td>
                    <td>{formatDate(cert.validTo)}</td>
                    <td>
                      <button
                        type="button"
                        className="btn btn-danger btn-sm"
                        disabled={revokeMutation.isPending}
                        onClick={() => {
                          if (
                            window.confirm(
                              'Revogar este certificado? Novos documentos ICP não poderão ser enviados até cadastrar outro.',
                            )
                          ) {
                            revokeMutation.mutate(cert.id);
                          }
                        }}
                      >
                        Revogar
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {revokeMutation.error && (
          <div className="alert alert-error" style={{ marginTop: '1rem' }}>
            {getErrorMessage(revokeMutation.error)}
          </div>
        )}
      </div>

      {revoked.length > 0 && (
        <div className="card">
          <h2 className="card-title">Histórico revogado</h2>
          <ul className="muted" style={{ margin: 0, paddingLeft: '1.25rem' }}>
            {revoked.map((cert) => (
              <li key={cert.id}>
                {cert.subject ?? cert.id} — revogado em {formatDate(cert.revokedAt)}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
