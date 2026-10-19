import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import * as api from '../api/endpoints';
import type { DocumentSigner, DocumentSummary } from '../api/types';
import { StatusBadge } from '../components/StatusBadge';
import { downloadBlob, formatDate, getErrorMessage } from '../utils/format';

function buildSignUrl(signer: DocumentSigner): string | null {
  if (signer.signUrl) return signer.signUrl;
  if (!signer.accessToken) return null;
  return `${window.location.origin}/sign/${signer.accessToken}`;
}

export function DocumentListPage() {
  const queryClient = useQueryClient();
  const [copiedKey, setCopiedKey] = useState<string | null>(null);
  const [copyError, setCopyError] = useState<string | null>(null);

  const { data, isLoading, error } = useQuery({
    queryKey: ['documents'],
    queryFn: api.listDocuments,
  });

  const downloadMutation = useMutation({
    mutationFn: async (doc: { id: string; fileName?: string; title?: string }) => {
      const blob = await api.downloadDocument(doc.id);
      const name =
        doc.fileName || (doc.title ? `${doc.title}.pdf` : `documento-${doc.id}.pdf`);
      downloadBlob(blob, name);
    },
  });

  async function refresh() {
    await queryClient.invalidateQueries({ queryKey: ['documents'] });
  }

  async function copyLink(doc: DocumentSummary, signer: DocumentSigner) {
    const url = buildSignUrl(signer);
    if (!url) {
      setCopyError('Link ainda não disponível. Envie o documento para gerar o convite.');
      return;
    }
    try {
      await navigator.clipboard.writeText(url);
      const key = `${doc.id}:${signer.email}`;
      setCopiedKey(key);
      setCopyError(null);
      window.setTimeout(() => {
        setCopiedKey((current) => (current === key ? null : current));
      }, 2000);
    } catch {
      setCopyError('Não foi possível copiar. Copie manualmente: ' + url);
    }
  }

  return (
    <div>
      <div className="page-hero row-between">
        <div>
          <h1>Documentos</h1>
          <p className="muted" style={{ marginBottom: 0 }}>
            Acompanhe rascunhos e solicitações de assinatura.
          </p>
        </div>
        <div className="row">
          <button type="button" className="btn btn-secondary btn-sm" onClick={refresh}>
            Atualizar
          </button>
          <Link to="/app/documents/new" className="btn btn-primary">
            Novo documento
          </Link>
        </div>
      </div>

      <div className="card">
        {isLoading && (
          <div className="loading-center">
            <span className="spinner" aria-hidden />
            Carregando documentos…
          </div>
        )}
        {error && (
          <div className="alert alert-error">{getErrorMessage(error)}</div>
        )}
        {copyError && (
          <div className="alert alert-warning" style={{ marginBottom: '1rem' }}>
            {copyError}
          </div>
        )}
        {!isLoading && !error && (!data || data.length === 0) && (
          <div className="empty-state">
            <p>Nenhum documento ainda.</p>
            <Link to="/app/documents/new" className="btn btn-primary">
              Enviar primeiro PDF
            </Link>
          </div>
        )}
        {data && data.length > 0 && (
          <div className="stack">
            {data.map((doc) => {
              const signers = doc.signers ?? [];
              const hasLinks = signers.some((s) => buildSignUrl(s));
              return (
                <article key={doc.id} className="doc-list-item">
                  <div className="doc-list-header row-between">
                    <div>
                      <strong>{doc.title || doc.fileName || 'Sem título'}</strong>
                      <div className="row" style={{ marginTop: '0.35rem', gap: '0.5rem' }}>
                        <StatusBadge status={doc.status} />
                        <span className="muted" style={{ fontSize: '0.8125rem' }}>
                          {doc.signatureType === 'CERTIFICATE'
                            ? 'Certificado (ICP)'
                            : doc.signatureType === 'SIMPLE'
                              ? 'Simples'
                              : '—'}
                          {' · '}
                          {formatDate(doc.createdAt)}
                        </span>
                      </div>
                    </div>
                    <div className="row" style={{ justifyContent: 'flex-end' }}>
                      {doc.status === 'DRAFT' && (
                        <Link
                          to={`/app/documents/new?documentId=${doc.id}`}
                          className="btn btn-secondary btn-sm"
                        >
                          Continuar
                        </Link>
                      )}
                      <button
                        type="button"
                        className="btn btn-ghost btn-sm"
                        disabled={downloadMutation.isPending}
                        onClick={() => downloadMutation.mutate(doc)}
                      >
                        Baixar
                      </button>
                    </div>
                  </div>

                  {signers.length > 0 && (
                    <div className="doc-signers">
                      <p className="muted" style={{ margin: '0 0 0.5rem', fontSize: '0.8125rem' }}>
                        Signatários
                        {!hasLinks && doc.status === 'DRAFT'
                          ? ' — envie o documento para gerar os links'
                          : ''}
                      </p>
                      <ul className="doc-signer-list">
                        {signers.map((signer) => {
                          const key = `${doc.id}:${signer.email}`;
                          const url = buildSignUrl(signer);
                          const canCopy =
                            !!url &&
                            doc.status !== 'DRAFT' &&
                            doc.status !== 'CANCELLED';
                          return (
                            <li key={signer.email} className="doc-signer-row">
                              <div>
                                <strong>{signer.name}</strong>
                                <div className="muted" style={{ fontSize: '0.8125rem' }}>
                                  {signer.email}
                                  {signer.status ? ` · ${signer.status}` : ''}
                                </div>
                              </div>
                              <button
                                type="button"
                                className="btn btn-secondary btn-sm"
                                disabled={!canCopy}
                                onClick={() => copyLink(doc, signer)}
                                title={
                                  canCopy
                                    ? 'Copiar link de assinatura'
                                    : 'Disponível após enviar o documento'
                                }
                              >
                                {copiedKey === key ? 'Copiado!' : 'Copiar link'}
                              </button>
                            </li>
                          );
                        })}
                      </ul>
                    </div>
                  )}
                </article>
              );
            })}
          </div>
        )}
        {downloadMutation.error && (
          <div className="alert alert-error" style={{ marginTop: '1rem' }}>
            {getErrorMessage(downloadMutation.error)}
          </div>
        )}
      </div>
    </div>
  );
}
