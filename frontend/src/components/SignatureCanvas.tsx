import { useEffect, useRef, useState } from 'react';

interface SignatureCanvasProps {
  onChange: (base64: string | undefined) => void;
}

export function SignatureCanvas({ onChange }: SignatureCanvasProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const drawing = useRef(false);
  const [empty, setEmpty] = useState(true);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const ratio = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    canvas.width = rect.width * ratio;
    canvas.height = rect.height * ratio;
    ctx.scale(ratio, ratio);
    ctx.lineWidth = 2;
    ctx.lineCap = 'round';
    ctx.strokeStyle = '#1a2332';
  }, []);

  function pos(e: PointerEvent, canvas: HTMLCanvasElement) {
    const rect = canvas.getBoundingClientRect();
    return { x: e.clientX - rect.left, y: e.clientY - rect.top };
  }

  function emitImage() {
    const canvas = canvasRef.current;
    if (!canvas || empty) {
      onChange(undefined);
      return;
    }
    const data = canvas.toDataURL('image/png');
    onChange(data);
  }

  function handlePointerDown(e: React.PointerEvent<HTMLCanvasElement>) {
    const canvas = canvasRef.current;
    if (!canvas) return;
    drawing.current = true;
    canvas.setPointerCapture(e.pointerId);
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    const { x, y } = pos(e.nativeEvent, canvas);
    ctx.beginPath();
    ctx.moveTo(x, y);
  }

  function handlePointerMove(e: React.PointerEvent<HTMLCanvasElement>) {
    if (!drawing.current) return;
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    const { x, y } = pos(e.nativeEvent, canvas);
    ctx.lineTo(x, y);
    ctx.stroke();
    setEmpty(false);
  }

  function handlePointerUp(e: React.PointerEvent<HTMLCanvasElement>) {
    if (!drawing.current) return;
    drawing.current = false;
    const canvas = canvasRef.current;
    if (canvas) {
      try {
        canvas.releasePointerCapture(e.pointerId);
      } catch {
        /* ignore */
      }
    }
    emitImage();
  }

  function clear() {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    setEmpty(true);
    onChange(undefined);
  }

  return (
    <div className="stack-sm">
      <div className="canvas-wrap">
        <canvas
          ref={canvasRef}
          className="signature-canvas"
          aria-label="Desenhe sua assinatura"
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          onPointerLeave={handlePointerUp}
        />
      </div>
      <button type="button" className="btn btn-secondary btn-sm" onClick={clear}>
        Limpar desenho
      </button>
    </div>
  );
}
