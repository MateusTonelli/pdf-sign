import { useEffect, useMemo, useState } from 'react';
import { SignatureCanvas } from './SignatureCanvas';

export type SignatureMode = 'draw' | 'style' | 'upload';

type StyleOption = {
  id: string;
  label: string;
  fontFamily: string;
};

const STYLE_OPTIONS: StyleOption[] = [
  { id: 'dancing', label: 'Manuscrita', fontFamily: '"Dancing Script", cursive' },
  { id: 'vibes', label: 'Elegante', fontFamily: '"Great Vibes", cursive' },
  { id: 'satisfy', label: 'Fluida', fontFamily: '"Satisfy", cursive' },
  { id: 'pacifico', label: 'Marcada', fontFamily: '"Pacifico", cursive' },
];

type Props = {
  name: string;
  onChange: (imageBase64: string | undefined) => void;
};

async function ensureFontsLoaded() {
  if (!document.fonts?.load) return;
  await Promise.all(
    STYLE_OPTIONS.map((s) =>
      document.fonts.load(`48px ${s.fontFamily.split(',')[0].replace(/"/g, '')}`),
    ),
  );
}

function renderStyledSignature(name: string, fontFamily: string): string {
  const canvas = document.createElement('canvas');
  const width = 560;
  const height = 160;
  const ratio = window.devicePixelRatio || 1;
  canvas.width = width * ratio;
  canvas.height = height * ratio;
  const ctx = canvas.getContext('2d');
  if (!ctx) return '';

  ctx.scale(ratio, ratio);
  ctx.clearRect(0, 0, width, height);
  ctx.fillStyle = '#1a2332';
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';

  let fontSize = 52;
  ctx.font = `${fontSize}px ${fontFamily}`;
  while (fontSize > 22 && ctx.measureText(name).width > width - 32) {
    fontSize -= 2;
    ctx.font = `${fontSize}px ${fontFamily}`;
  }
  ctx.fillText(name, width / 2, height / 2);
  return canvas.toDataURL('image/png');
}

function fileToDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = () => reject(new Error('Falha ao ler a imagem.'));
    reader.readAsDataURL(file);
  });
}

export function SignaturePicker({ name, onChange }: Props) {
  const [mode, setMode] = useState<SignatureMode>('style');
  const [styleId, setStyleId] = useState(STYLE_OPTIONS[0].id);
  const [uploadPreview, setUploadPreview] = useState<string | undefined>();
  const [fontsReady, setFontsReady] = useState(false);

  const trimmed = name.trim();
  const selectedStyle = useMemo(
    () => STYLE_OPTIONS.find((s) => s.id === styleId) ?? STYLE_OPTIONS[0],
    [styleId],
  );

  useEffect(() => {
    let cancelled = false;
    ensureFontsLoaded()
      .then(() => {
        if (!cancelled) setFontsReady(true);
      })
      .catch(() => {
        if (!cancelled) setFontsReady(true);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (mode !== 'style') return;
    if (!trimmed) {
      onChange(undefined);
      return;
    }
    if (!fontsReady) return;
    const dataUrl = renderStyledSignature(trimmed, selectedStyle.fontFamily);
    onChange(dataUrl || undefined);
  }, [mode, trimmed, selectedStyle.fontFamily, fontsReady, onChange]);

  useEffect(() => {
    if (mode === 'upload') {
      onChange(uploadPreview);
    }
    if (mode === 'draw') {
      // SignatureCanvas will call onChange; clear until user draws
      onChange(undefined);
    }
  }, [mode]); // eslint-disable-line react-hooks/exhaustive-deps

  async function handleUpload(file: File | null) {
    if (!file) {
      setUploadPreview(undefined);
      onChange(undefined);
      return;
    }
    if (!file.type.startsWith('image/')) {
      setUploadPreview(undefined);
      onChange(undefined);
      return;
    }
    const dataUrl = await fileToDataUrl(file);
    setUploadPreview(dataUrl);
    onChange(dataUrl);
  }

  return (
    <div className="sig-picker stack">
      <div className="sig-tabs" role="tablist" aria-label="Como assinar">
        <button
          type="button"
          role="tab"
          aria-selected={mode === 'style'}
          className={`sig-tab ${mode === 'style' ? 'active' : ''}`}
          onClick={() => setMode('style')}
        >
          Estilo
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={mode === 'draw'}
          className={`sig-tab ${mode === 'draw' ? 'active' : ''}`}
          onClick={() => setMode('draw')}
        >
          Desenhar
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={mode === 'upload'}
          className={`sig-tab ${mode === 'upload' ? 'active' : ''}`}
          onClick={() => setMode('upload')}
        >
          Imagem
        </button>
      </div>

      {mode === 'style' && (
        <div className="stack-sm">
          <p className="muted" style={{ margin: 0 }}>
            Escolha um efeito de assinatura a partir do seu nome.
          </p>
          {!trimmed && (
            <div className="alert alert-info">Digite seu nome acima para ver as opções.</div>
          )}
          {trimmed && (
            <div className="sig-style-grid">
              {STYLE_OPTIONS.map((opt) => (
                <button
                  key={opt.id}
                  type="button"
                  className={`sig-style-card ${styleId === opt.id ? 'selected' : ''}`}
                  onClick={() => setStyleId(opt.id)}
                >
                  <span className="sig-style-label">{opt.label}</span>
                  <span
                    className="sig-style-preview"
                    style={{ fontFamily: opt.fontFamily }}
                  >
                    {trimmed}
                  </span>
                </button>
              ))}
            </div>
          )}
        </div>
      )}

      {mode === 'draw' && (
        <div className="stack-sm">
          <p className="muted" style={{ margin: 0 }}>
            Desenhe sua assinatura com o mouse ou o dedo.
          </p>
          <SignatureCanvas onChange={onChange} />
        </div>
      )}

      {mode === 'upload' && (
        <div className="stack-sm">
          <p className="muted" style={{ margin: 0 }}>
            Envie uma foto ou imagem da sua assinatura (PNG ou JPG).
          </p>
          <div className="form-field">
            <label htmlFor="sig-upload">Arquivo de imagem</label>
            <input
              id="sig-upload"
              type="file"
              accept="image/png,image/jpeg,image/webp"
              onChange={(e) => handleUpload(e.target.files?.[0] ?? null)}
            />
          </div>
          {uploadPreview && (
            <div className="sig-upload-preview">
              <img src={uploadPreview} alt="Prévia da assinatura" />
            </div>
          )}
        </div>
      )}
    </div>
  );
}
