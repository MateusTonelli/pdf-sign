import type { DocumentStatus } from '../api/types';

const LABELS: Record<string, string> = {
  DRAFT: 'Rascunho',
  PENDING: 'Aguardando assinaturas',
  PARTIALLY_SIGNED: 'Parcialmente assinado',
  COMPLETED: 'Concluído',
  CANCELLED: 'Cancelado',
};

function badgeClass(status: DocumentStatus): string {
  const normalized = (status || '').toUpperCase();
  switch (normalized) {
    case 'DRAFT':
      return 'badge badge-draft';
    case 'PENDING':
    case 'PARTIALLY_SIGNED':
      return 'badge badge-pending';
    case 'COMPLETED':
      return 'badge badge-completed';
    case 'CANCELLED':
      return 'badge badge-cancelled';
    default:
      return 'badge badge-default';
  }
}

export function StatusBadge({ status }: { status: DocumentStatus }) {
  const normalized = (status || '').toUpperCase();
  const label = LABELS[normalized] ?? status ?? '—';
  return <span className={badgeClass(status)}>{label}</span>;
}
