export interface User {
  id: string;
  name: string;
  email: string;
  companyName?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
}

export type DocumentStatus =
  | 'DRAFT'
  | 'PENDING'
  | 'PARTIALLY_SIGNED'
  | 'COMPLETED'
  | 'CANCELLED'
  | string;

export type SignatureType = 'SIMPLE' | 'CERTIFICATE';

export interface DocumentSigner {
  id?: string;
  name: string;
  email: string;
  status?: string;
  signedAt?: string | null;
  sortOrder?: number;
  accessToken?: string | null;
  signUrl?: string | null;
}

export interface SignatureField {
  signerEmail: string;
  page: number;
  x: number;
  y: number;
  width: number;
  height: number;
}

export interface DocumentSummary {
  id: string;
  title?: string;
  fileName?: string;
  status: DocumentStatus;
  signatureType?: SignatureType;
  createdAt?: string;
  updatedAt?: string;
  signers?: DocumentSigner[];
}

export interface DocumentDetail extends DocumentSummary {
  signers?: DocumentSigner[];
  fields?: SignatureField[];
}

export interface Certificate {
  id: string;
  subject?: string;
  issuer?: string;
  serialNumber?: string;
  validFrom?: string;
  validTo?: string;
  revokedAt?: string | null;
  active?: boolean;
}

export interface SignSession {
  token: string;
  documentTitle?: string;
  fileName?: string;
  signerName?: string;
  signerEmail?: string;
  signatureType: SignatureType;
  canSignWithCertificate?: boolean;
  alreadySigned?: boolean;
  status?: string;
}

export interface ApiErrorBody {
  message?: string;
  error?: string;
}
