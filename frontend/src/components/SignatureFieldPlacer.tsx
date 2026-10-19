import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type PointerEvent as ReactPointerEvent,
} from 'react';
import { Document, Page, pdfjs } from 'react-pdf';
import 'react-pdf/dist/Page/AnnotationLayer.css';
import 'react-pdf/dist/Page/TextLayer.css';
import type { SignatureField } from '../api/types';
import * as api from '../api/endpoints';

// Served from /public — reliable in Vite prod + nginx (avoids broken hashed worker URLs)
pdfjs.GlobalWorkerOptions.workerSrc = `${import.meta.env.BASE_URL}pdf.worker.min.mjs`;

const SIGNER_COLORS = [
  '#0f766e',
  '#b54708',
  '#175cd3',
  '#7a2e4a',
  '#067647',
  '#6941c6',
];

type SignerInfo = { name: string; email: string };

type DragMode =
  | { type: 'create'; startX: number; startY: number }
  | { type: 'move'; email: string; offsetX: number; offsetY: number }
  | null;

type Props = {
  documentId: string;
  signers: SignerInfo[];
  fields: SignatureField[];
  onChange: (fields: SignatureField[]) => void;
};

function clamp(n: number, min: number, max: number) {
  return Math.min(max, Math.max(min, n));
}

function colorForEmail(email: string, signers: SignerInfo[]) {
  const idx = Math.max(
    0,
    signers.findIndex((s) => s.email.toLowerCase() === email.toLowerCase()),
  );
  return SIGNER_COLORS[idx % SIGNER_COLORS.length];
}

export function SignatureFieldPlacer({
  documentId,
  signers,
  fields,
  onChange,
}: Props) {
  const [pdfFile, setPdfFile] = useState<Blob | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [numPages, setNumPages] = useState(0);
  const [page, setPage] = useState(1);
  const [pageWidth, setPageWidth] = useState(640);
  const [activeEmail, setActiveEmail] = useState(signers[0]?.email ?? '');
  const [drag, setDrag] = useState<DragMode>(null);
  const [draft, setDraft] = useState<{
    x: number;
    y: number;
    width: number;
    height: number;
  } | null>(null);

  const overlayRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let cancelled = false;
    setLoadError(null);
    setPdfFile(null);
    api
      .downloadDocument(documentId)
      .then(async (blob) => {
        const buffer = await blob.arrayBuffer();
        if (cancelled) return;
        const bytes = new Uint8Array(buffer);
        // PDF magic header "%PDF"
        if (
          bytes.length < 5 ||
          bytes[0] !== 0x25 ||
          bytes[1] !== 0x50 ||
          bytes[2] !== 0x44 ||
          bytes[3] !== 0x46
        ) {
          throw new Error('Arquivo baixado não é um PDF válido.');
        }
        setPdfFile(new Blob([bytes], { type: 'application/pdf' }));
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          const msg =
            err instanceof Error ? err.message : 'Não foi possível carregar o PDF.';
          setLoadError(msg);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [documentId]);

  useEffect(() => {
    if (
      signers.length &&
      !signers.some((s) => s.email.toLowerCase() === activeEmail.toLowerCase())
    ) {
      setActiveEmail(signers[0].email);
    }
  }, [signers, activeEmail]);

  useEffect(() => {
    function onResize() {
      const w = Math.min(720, Math.max(320, window.innerWidth - 64));
      setPageWidth(w);
    }
    onResize();
    window.addEventListener('resize', onResize);
    return () => window.removeEventListener('resize', onResize);
  }, []);

  const fieldsOnPage = useMemo(
    () => fields.filter((f) => f.page === page),
    [fields, page],
  );

  const missing = useMemo(() => {
    const placed = new Set(fields.map((f) => f.signerEmail.toLowerCase()));
    return signers.filter((s) => !placed.has(s.email.toLowerCase()));
  }, [fields, signers]);

  const toNorm = useCallback((clientX: number, clientY: number) => {
    const el = overlayRef.current;
    if (!el) return { x: 0, y: 0 };
    const rect = el.getBoundingClientRect();
    return {
      x: clamp((clientX - rect.left) / rect.width, 0, 1),
      y: clamp((clientY - rect.top) / rect.height, 0, 1),
    };
  }, []);

  const upsertField = useCallback(
    (next: SignatureField) => {
      onChange([
        ...fields.filter(
          (f) => f.signerEmail.toLowerCase() !== next.signerEmail.toLowerCase(),
        ),
        next,
      ]);
    },
    [fields, onChange],
  );

  const removeField = useCallback(
    (email: string) => {
      onChange(
        fields.filter((f) => f.signerEmail.toLowerCase() !== email.toLowerCase()),
      );
    },
    [fields, onChange],
  );

  function onPointerDown(e: ReactPointerEvent<HTMLDivElement>) {
    if (!activeEmail) return;
    const target = e.target as HTMLElement;
    const boxEmail = target.closest('[data-field-email]')?.getAttribute('data-field-email');
    const point = toNorm(e.clientX, e.clientY);

    if (boxEmail) {
      const field = fields.find(
        (f) => f.signerEmail.toLowerCase() === boxEmail.toLowerCase(),
      );
      if (!field || field.page !== page) return;
      setActiveEmail(boxEmail);
      setDrag({
        type: 'move',
        email: boxEmail,
        offsetX: point.x - field.x,
        offsetY: point.y - field.y,
      });
      (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
      return;
    }

    setDrag({ type: 'create', startX: point.x, startY: point.y });
    setDraft({ x: point.x, y: point.y, width: 0, height: 0 });
    (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
  }

  function onPointerMove(e: ReactPointerEvent<HTMLDivElement>) {
    if (!drag) return;
    const point = toNorm(e.clientX, e.clientY);

    if (drag.type === 'create') {
      const x = Math.min(drag.startX, point.x);
      const y = Math.min(drag.startY, point.y);
      const width = Math.abs(point.x - drag.startX);
      const height = Math.abs(point.y - drag.startY);
      setDraft({ x, y, width, height });
      return;
    }

    const field = fields.find(
      (f) => f.signerEmail.toLowerCase() === drag.email.toLowerCase(),
    );
    if (!field) return;
    const x = clamp(point.x - drag.offsetX, 0, 1 - field.width);
    const y = clamp(point.y - drag.offsetY, 0, 1 - field.height);
    upsertField({ ...field, page, x, y });
  }

  function onPointerUp() {
    if (!drag) return;

    if (drag.type === 'create' && draft && activeEmail) {
      const minW = 0.12;
      const minH = 0.04;
      let { x, y, width, height } = draft;
      if (width < minW || height < minH) {
        // click = place default box centered on click
        width = 0.28;
        height = 0.06;
        x = clamp(drag.startX - width / 2, 0, 1 - width);
        y = clamp(drag.startY - height / 2, 0, 1 - height);
      } else {
        width = clamp(width, minW, 1);
        height = clamp(height, minH, 1);
        x = clamp(x, 0, 1 - width);
        y = clamp(y, 0, 1 - height);
      }
      upsertField({
        signerEmail: activeEmail,
        page,
        x,
        y,
        width,
        height,
      });
    }

    setDrag(null);
    setDraft(null);
  }

  const activeSigner = signers.find(
    (s) => s.email.toLowerCase() === activeEmail.toLowerCase(),
  );

  return (
    <div className="placer">
      <div className="placer-toolbar">
        <div>
          <p className="placer-hint">
            Selecione um signatário e <strong>clique</strong> ou{' '}
            <strong>arraste</strong> sobre o PDF para marcar onde a assinatura
            aparece. Arraste a caixa para ajustar.
          </p>
          <div className="placer-signers">
            {signers.map((s) => {
              const placed = fields.some(
                (f) => f.signerEmail.toLowerCase() === s.email.toLowerCase(),
              );
              const color = colorForEmail(s.email, signers);
              return (
                <button
                  key={s.email}
                  type="button"
                  className={`placer-signer-chip ${activeEmail === s.email ? 'active' : ''} ${placed ? 'placed' : ''}`}
                  style={{ ['--chip-color' as string]: color }}
                  onClick={() => setActiveEmail(s.email)}
                >
                  <span className="placer-signer-dot" />
                  <span>
                    {s.name || s.email}
                    {placed ? ' ✓' : ''}
                  </span>
                </button>
              );
            })}
          </div>
          {missing.length > 0 && (
            <p className="muted" style={{ margin: '0.5rem 0 0' }}>
              Faltam posicionar: {missing.map((m) => m.name || m.email).join(', ')}
            </p>
          )}
        </div>
        <div className="placer-page-nav">
          <button
            type="button"
            className="btn btn-ghost btn-sm"
            disabled={page <= 1}
            onClick={() => setPage((p) => p - 1)}
          >
            ← Página
          </button>
          <span className="muted">
            {page} / {numPages || '—'}
          </span>
          <button
            type="button"
            className="btn btn-ghost btn-sm"
            disabled={!numPages || page >= numPages}
            onClick={() => setPage((p) => p + 1)}
          >
            Página →
          </button>
        </div>
      </div>

      {loadError && (
        <div className="alert alert-error" role="alert">
          {loadError}
        </div>
      )}

      {!pdfFile && !loadError && (
        <div className="loading-center">
          <div className="spinner" /> Carregando PDF…
        </div>
      )}

      {pdfFile && (
        <div className="placer-stage">
          <div className="placer-canvas-wrap">
            <Document
              file={pdfFile}
              loading={
                <div className="loading-center">
                  <div className="spinner" /> Renderizando…
                </div>
              }
              onLoadSuccess={(info) => {
                setNumPages(info.numPages);
                setLoadError(null);
              }}
              onLoadError={(err) =>
                setLoadError(
                  `Falha ao renderizar o PDF${err?.message ? `: ${err.message}` : '.'}`,
                )
              }
            >
              <div className="placer-page">
                <Page
                  pageNumber={page}
                  width={pageWidth}
                  renderTextLayer={false}
                  renderAnnotationLayer={false}
                />
                <div
                  ref={overlayRef}
                  className="placer-overlay"
                  onPointerDown={onPointerDown}
                  onPointerMove={onPointerMove}
                  onPointerUp={onPointerUp}
                  onPointerCancel={onPointerUp}
                >
                  {fieldsOnPage.map((field) => {
                    const color = colorForEmail(field.signerEmail, signers);
                    const label =
                      signers.find(
                        (s) =>
                          s.email.toLowerCase() ===
                          field.signerEmail.toLowerCase(),
                      )?.name || field.signerEmail;
                    return (
                      <div
                        key={field.signerEmail}
                        data-field-email={field.signerEmail}
                        className={`placer-box ${activeEmail === field.signerEmail ? 'active' : ''}`}
                        style={{
                          left: `${field.x * 100}%`,
                          top: `${field.y * 100}%`,
                          width: `${field.width * 100}%`,
                          height: `${field.height * 100}%`,
                          borderColor: color,
                          background: `${color}33`,
                        }}
                        title="Arraste para mover"
                      >
                        <span className="placer-box-label" style={{ background: color }}>
                          {label}
                        </span>
                        <button
                          type="button"
                          className="placer-box-remove"
                          aria-label="Remover campo"
                          onClick={(ev) => {
                            ev.stopPropagation();
                            removeField(field.signerEmail);
                          }}
                          onPointerDown={(ev) => ev.stopPropagation()}
                        >
                          ×
                        </button>
                      </div>
                    );
                  })}
                  {draft && (
                    <div
                      className="placer-box draft"
                      style={{
                        left: `${draft.x * 100}%`,
                        top: `${draft.y * 100}%`,
                        width: `${draft.width * 100}%`,
                        height: `${draft.height * 100}%`,
                        borderColor: colorForEmail(activeEmail, signers),
                      }}
                    />
                  )}
                </div>
              </div>
            </Document>
          </div>
          {activeSigner && (
            <p className="muted placer-active-hint">
              Posicionando para <strong>{activeSigner.name}</strong> (
              {activeSigner.email})
            </p>
          )}
        </div>
      )}
    </div>
  );
}
