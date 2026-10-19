import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import * as api from '../api/endpoints';
import { StatusBadge } from '../components/StatusBadge';
import { downloadBlob, formatDate, getErrorMessage } from '../utils/format';

export function DocumentListPage() {
  const queryClient = useQueryClient();

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
        {!isLoading && !error && (!data || data.length === 0) && (
          <div className="empty-state">
            <p>Nenhum documento ainda.</p>
            <Link to="/app/documents/new" className="btn btn-primary">
              Enviar primeiro PDF
            </Link>
          </div>
        )}
        {data && data.length > 0 && (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Título</th>
                  <th>Status</th>
                  <th>Tipo</th>
                  <th>Criado em</th>
                  <th aria-label="Ações" />
                </tr>
              </thead>
              <tbody>
                {data.map((doc) => (
                  <tr key={doc.id}>
                    <td>
                      <strong>{doc.title || doc.fileName || 'Sem título'}</strong>
                    </td>
                    <td>
                      <StatusBadge status={doc.status} />
                    </td>
                    <td>
                      {doc.signatureType === 'CERTIFICATE'
                        ? 'Certificado (ICP)'
                        : doc.signatureType === 'SIMPLE'
                          ? 'Simples'
                          : '—'}
                    </td>
                    <td>{formatDate(doc.createdAt)}</td>
                    <td>
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
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
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
