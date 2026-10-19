import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FormEvent, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import * as api from '../api/endpoints';
import type { SignatureField, SignatureType } from '../api/types';
import { getErrorMessage } from '../utils/format';

const STEPS = [
  'Upload',
  'Tipo de assinatura',
  'Signatários',
  'Campos',
  'Revisão',
] as const;

type SignerDraft = { name: string; email: string };

const DEFAULT_FIELD = {
  page: 1,
  x: 72,
  y: 72,
  width: 180,
  height: 48,
};

export function DocumentWizardPage() {
  const [searchParams] = useSearchParams();
  const resumeId = searchParams.get('documentId');
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [step, setStep] = useState(0);
  const [documentId, setDocumentId] = useState<string | null>(resumeId);
  const [title, setTitle] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [signatureType, setSignatureType] = useState<SignatureType>('SIMPLE');
  const [signers, setSigners] = useState<SignerDraft[]>([
    { name: '', email: '' },
  ]);
  const [fields, setFields] = useState<SignatureField[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const certsQuery = useQuery({
    queryKey: ['certificates'],
    queryFn: api.listCertificates,
    enabled: signatureType === 'CERTIFICATE',
  });

  const docQuery = useQuery({
    queryKey: ['document', documentId],
    queryFn: () => api.getDocument(documentId!),
    enabled: !!documentId,
  });

  useEffect(() => {
    const doc = docQuery.data;
    if (!doc) return;
    if (doc.title) setTitle(doc.title);
    if (doc.signatureType) setSignatureType(doc.signatureType);
    if (doc.signers?.length) {
      setSigners(doc.signers.map((s) => ({ name: s.name, email: s.email })));
    }
    if (doc.fields?.length) {
      setFields(doc.fields);
    }
    if (resumeId) {
      if (!doc.signatureType) setStep(1);
      else if (!doc.signers?.length) setStep(2);
      else if (!doc.fields?.length) setStep(3);
      else setStep(4);
    }
  }, [docQuery.data, resumeId]);

  const activeCerts = useMemo(() => {
    const list = certsQuery.data ?? [];
    return list.filter((c) => !c.revokedAt && c.active !== false);
  }, [certsQuery.data]);

  const certificateBlocked =
    signatureType === 'CERTIFICATE' &&
    !certsQuery.isLoading &&
    activeCerts.length === 0;

  const uploadMutation = useMutation({
    mutationFn: async () => {
      if (!file) throw new Error('Selecione um arquivo PDF.');
      return api.uploadDocument(file, title.trim() || undefined);
    },
    onSuccess: (doc) => {
      setDocumentId(doc.id);
      if (doc.title) setTitle(doc.title);
      setStep(1);
      queryClient.invalidateQueries({ queryKey: ['documents'] });
    },
  });

  const saveTypeMutation = useMutation({
    mutationFn: async () => {
      if (!documentId) throw new Error('Documento não encontrado.');
      return api.updateDocument(documentId, {
        title: title.trim() || undefined,
        signatureType,
      });
    },
    onSuccess: () => setStep(2),
  });

  const saveSignersMutation = useMutation({
    mutationFn: async () => {
      if (!documentId) throw new Error('Documento não encontrado.');
      const cleaned = signers
        .map((s) => ({ name: s.name.trim(), email: s.email.trim().toLowerCase() }))
        .filter((s) => s.name && s.email);
      if (cleaned.length === 0) {
        throw new Error('Adicione pelo menos um signatário.');
      }
      return api.updateSigners(documentId, cleaned);
    },
    onSuccess: (doc) => {
      const nextFields =
        doc.fields?.length === doc.signers?.length
          ? doc.fields
          : (doc.signers ?? []).map((s) => ({
              signerEmail: s.email,
              ...DEFAULT_FIELD,
            }));
      setFields(nextFields ?? []);
      setStep(3);
    },
  });

  const saveFieldsMutation = useMutation({
    mutationFn: async () => {
      if (!documentId) throw new Error('Documento não encontrado.');
      return api.updateFields(documentId, fields);
    },
    onSuccess: () => setStep(4),
  });

  const sendMutation = useMutation({
    mutationFn: async () => {
      if (!documentId) throw new Error('Documento não encontrado.');
      return api.sendDocument(documentId);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['documents'] });
      navigate('/app');
    },
  });

  const isBusy =
    uploadMutation.isPending ||
    saveTypeMutation.isPending ||
    saveSignersMutation.isPending ||
    saveFieldsMutation.isPending ||
    sendMutation.isPending;

  function addSigner() {
    setSigners((prev) => [...prev, { name: '', email: '' }]);
  }

  function removeSigner(index: number) {
    setSigners((prev) => prev.filter((_, i) => i !== index));
  }

  function updateSigner(index: number, key: keyof SignerDraft, value: string) {
    setSigners((prev) =>
      prev.map((s, i) => (i === index ? { ...s, [key]: value } : s)),
    );
  }

  function updateField(index: number, patch: Partial<SignatureField>) {
    setFields((prev) =>
      prev.map((f, i) => (i === index ? { ...f, ...patch } : f)),
    );
  }

  async function handleUpload(e: FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await uploadMutation.mutateAsync();
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }

  async function handleSaveDraft() {
    setError(null);
    setSuccess(null);
    try {
      if (!documentId) {
        if (!file) throw new Error('Envie um PDF primeiro.');
        await uploadMutation.mutateAsync();
        setSuccess('Rascunho salvo. Continue quando quiser.');
        return;
      }
      if (title.trim()) {
        await api.updateDocument(documentId, { title: title.trim() });
      }
      if (step >= 1) {
        await api.updateDocument(documentId, { signatureType });
      }
      if (step >= 2) {
        const cleaned = signers
          .map((s) => ({ name: s.name.trim(), email: s.email.trim().toLowerCase() }))
          .filter((s) => s.name && s.email);
        if (cleaned.length) await api.updateSigners(documentId, cleaned);
      }
      if (step >= 3 && fields.length) {
        await api.updateFields(documentId, fields);
      }
      queryClient.invalidateQueries({ queryKey: ['documents'] });
      setSuccess('Rascunho salvo com sucesso.');
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }

  async function goNext() {
    setError(null);
    setSuccess(null);
    try {
      if (step === 0) {
        if (documentId && !file) {
          setStep(1);
          return;
        }
        await uploadMutation.mutateAsync();
        return;
      }
      if (step === 1) {
        await saveTypeMutation.mutateAsync();
        return;
      }
      if (step === 2) {
        await saveSignersMutation.mutateAsync();
        return;
      }
      if (step === 3) {
        await saveFieldsMutation.mutateAsync();
        return;
      }
      if (step === 4) {
        if (certificateBlocked) {
          setError(
            'Cadastre um certificado A1 antes de enviar documentos com assinatura ICP.',
          );
          return;
        }
        await sendMutation.mutateAsync();
      }
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }

  function goBack() {
    setError(null);
    setStep((s) => Math.max(0, s - 1));
  }

  return (
    <div>
      <div className="page-hero row-between">
        <div>
          <h1>Novo documento</h1>
          <p className="muted" style={{ marginBottom: 0 }}>
            Envie um PDF e configure quem deve assinar.
          </p>
        </div>
        <Link to="/app" className="btn btn-ghost">
          Voltar
        </Link>
      </div>

      <div className="wizard-steps" aria-label="Etapas">
        {STEPS.map((label, index) => (
          <span
            key={label}
            className={`wizard-step ${index === step ? 'active' : ''} ${index < step ? 'done' : ''}`}
          >
            {index + 1}. {label}
          </span>
        ))}
      </div>

      {error && (
        <div className="alert alert-error" role="alert">
          {error}
        </div>
      )}
      {success && (
        <div className="alert alert-success" role="status">
          {success}
        </div>
      )}

      <div className="card">
        {step === 0 && (
          <form className="stack" onSubmit={handleUpload}>
            <h2 className="card-title">Upload do PDF</h2>
            <div className="form-field">
              <label htmlFor="title">Título do documento (opcional)</label>
              <input
                id="title"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="Ex.: Contrato de prestação de serviços"
              />
            </div>
            <div className="form-field">
              <label htmlFor="pdf">Arquivo PDF</label>
              <input
                id="pdf"
                type="file"
                accept="application/pdf,.pdf"
                required={!documentId}
                onChange={(e) => setFile(e.target.files?.[0] ?? null)}
              />
            </div>
            {documentId && (
              <p className="muted">
                Documento em rascunho: <code>{documentId}</code>
                {docQuery.data?.fileName ? ` — ${docQuery.data.fileName}` : ''}
              </p>
            )}
          </form>
        )}

        {step === 1 && (
          <div className="stack">
            <h2 className="card-title">Tipo de assinatura</h2>
            <div className="choice-grid">
              <button
                type="button"
                className={`choice-card ${signatureType === 'SIMPLE' ? 'selected' : ''}`}
                onClick={() => setSignatureType('SIMPLE')}
              >
                <h3>Assinatura simples</h3>
                <p className="muted" style={{ margin: 0 }}>
                  O signatário confirma identidade digitando o nome e, se quiser,
                  desenha a rubrica. Ideal para acordos internos com trilha de
                  auditoria básica — não usa certificado ICP-Brasil.
                </p>
              </button>
              <button
                type="button"
                className={`choice-card ${signatureType === 'CERTIFICATE' ? 'selected' : ''}`}
                onClick={() => setSignatureType('CERTIFICATE')}
              >
                <h3>Assinatura com certificado (ICP-Brasil)</h3>
                <p className="muted" style={{ margin: 0 }}>
                  Cada signatário assina com certificado digital A1 (.pfx) vinculado
                  ao CPF/CNPJ. Oferece validade jurídica reforçada e integridade
                  criptográfica do PDF.
                </p>
              </button>
            </div>

            {signatureType === 'CERTIFICATE' && (
              <div className="stack">
                {certsQuery.isLoading && (
                  <p className="muted">Verificando certificados da sua conta…</p>
                )}
                {certificateBlocked && (
                  <div className="alert alert-warning">
                    <strong>Certificado necessário para enviar</strong>
                    <p style={{ margin: '0.5rem 0 0' }}>
                      Para documentos ICP-Brasil, você precisa cadastrar um certificado
                      A1 (.pfx) na sua conta antes de disparar convites. Enquanto isso,
                      você pode salvar este fluxo como rascunho.
                    </p>
                    <Link
                      to="/app/settings/certificate"
                      className="btn btn-secondary btn-sm"
                      style={{ marginTop: '0.75rem' }}
                    >
                      Cadastrar certificado
                    </Link>
                  </div>
                )}
                {!certificateBlocked && activeCerts.length > 0 && (
                  <div className="alert alert-info">
                    Certificado ativo encontrado:{' '}
                    {activeCerts[0].subject ?? activeCerts[0].id}
                  </div>
                )}
                <div className="card" style={{ background: '#fafbfc' }}>
                  <h3 style={{ fontSize: '1rem' }}>Como obter certificado A1</h3>
                  <ol className="muted" style={{ margin: 0, paddingLeft: '1.25rem' }}>
                    <li>Contrate um certificado A1 com uma Autoridade Certificadora credenciada ICP-Brasil.</li>
                    <li>Exporte o certificado no formato .pfx (PKCS#12) com senha de proteção.</li>
                    <li>
                      Em <Link to="/app/settings/certificate">Configurações → Certificado</Link>,
                      faça upload do arquivo e informe a senha.
                    </li>
                    <li>
                      Signatários externos devem usar o mesmo e-mail da conta e cadastrar
                      o próprio .pfx para assinar via link.
                    </li>
                  </ol>
                </div>
              </div>
            )}
          </div>
        )}

        {step === 2 && (
          <div className="stack">
            <h2 className="card-title">Signatários</h2>
            <p className="muted">
              Informe nome e e-mail de cada pessoa que deve assinar. Enviaremos um link
              exclusivo por e-mail após a confirmação.
            </p>
            {signers.map((signer, index) => (
              <div className="signer-row" key={`signer-${index}`}>
                <div className="form-field">
                  <label htmlFor={`signer-name-${index}`}>Nome</label>
                  <input
                    id={`signer-name-${index}`}
                    required
                    value={signer.name}
                    onChange={(e) => updateSigner(index, 'name', e.target.value)}
                  />
                </div>
                <div className="form-field">
                  <label htmlFor={`signer-email-${index}`}>E-mail</label>
                  <input
                    id={`signer-email-${index}`}
                    type="email"
                    required
                    value={signer.email}
                    onChange={(e) => updateSigner(index, 'email', e.target.value)}
                  />
                </div>
                <button
                  type="button"
                  className="btn btn-ghost btn-sm"
                  disabled={signers.length === 1}
                  onClick={() => removeSigner(index)}
                >
                  Remover
                </button>
              </div>
            ))}
            <button type="button" className="btn btn-secondary" onClick={addSigner}>
              Adicionar signatário
            </button>
          </div>
        )}

        {step === 3 && (
          <div className="stack">
            <h2 className="card-title">Posição dos campos</h2>
            <p className="muted">
              Defina página e coordenadas (pontos PDF) de cada campo de assinatura.
              Valores padrão colocam o campo no canto inferior da página 1.
            </p>
            {fields.map((field, index) => (
              <fieldset className="field-block" key={field.signerEmail}>
                <legend>{field.signerEmail}</legend>
                <div className="form-grid form-grid-2">
                  <div className="form-field">
                    <label htmlFor={`page-${index}`}>Página</label>
                    <input
                      id={`page-${index}`}
                      type="number"
                      min={1}
                      value={field.page}
                      onChange={(e) =>
                        updateField(index, { page: Number(e.target.value) || 1 })
                      }
                    />
                  </div>
                  <div className="form-field">
                    <label htmlFor={`x-${index}`}>X</label>
                    <input
                      id={`x-${index}`}
                      type="number"
                      value={field.x}
                      onChange={(e) =>
                        updateField(index, { x: Number(e.target.value) || 0 })
                      }
                    />
                  </div>
                  <div className="form-field">
                    <label htmlFor={`y-${index}`}>Y</label>
                    <input
                      id={`y-${index}`}
                      type="number"
                      value={field.y}
                      onChange={(e) =>
                        updateField(index, { y: Number(e.target.value) || 0 })
                      }
                    />
                  </div>
                  <div className="form-field">
                    <label htmlFor={`w-${index}`}>Largura</label>
                    <input
                      id={`w-${index}`}
                      type="number"
                      min={10}
                      value={field.width}
                      onChange={(e) =>
                        updateField(index, { width: Number(e.target.value) || 10 })
                      }
                    />
                  </div>
                  <div className="form-field">
                    <label htmlFor={`h-${index}`}>Altura</label>
                    <input
                      id={`h-${index}`}
                      type="number"
                      min={10}
                      value={field.height}
                      onChange={(e) =>
                        updateField(index, { height: Number(e.target.value) || 10 })
                      }
                    />
                  </div>
                </div>
              </fieldset>
            ))}
          </div>
        )}

        {step === 4 && (
          <div className="stack">
            <h2 className="card-title">Revisão e envio</h2>
            <dl className="stack-sm" style={{ margin: 0 }}>
              <div>
                <dt className="muted">Título</dt>
                <dd>{title || docQuery.data?.fileName || '—'}</dd>
              </div>
              <div>
                <dt className="muted">Tipo</dt>
                <dd>
                  {signatureType === 'CERTIFICATE'
                    ? 'Certificado ICP-Brasil'
                    : 'Assinatura simples'}
                </dd>
              </div>
              <div>
                <dt className="muted">Signatários</dt>
                <dd>
                  <ul style={{ margin: 0, paddingLeft: '1.25rem' }}>
                    {signers
                      .filter((s) => s.name && s.email)
                      .map((s) => (
                        <li key={s.email}>
                          {s.name} — {s.email}
                        </li>
                      ))}
                  </ul>
                </dd>
              </div>
            </dl>
            {certificateBlocked && (
              <div className="alert alert-warning">
                Envio bloqueado até cadastrar certificado A1. Salve como rascunho e
                retome depois.
              </div>
            )}
          </div>
        )}

        <div className="row-between" style={{ marginTop: '1.5rem' }}>
          <button
            type="button"
            className="btn btn-ghost"
            disabled={step === 0 || isBusy}
            onClick={goBack}
          >
            Anterior
          </button>
          <div className="row">
            <button
              type="button"
              className="btn btn-secondary"
              disabled={isBusy}
              onClick={handleSaveDraft}
            >
              Salvar rascunho
            </button>
            <button
              type="button"
              className="btn btn-primary"
              disabled={isBusy || (step === 4 && certificateBlocked)}
              onClick={goNext}
            >
              {step === 4
                ? sendMutation.isPending
                  ? 'Enviando…'
                  : 'Enviar convites'
                : 'Continuar'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
