import { apiBlob, apiRequest } from './client';
import type {
  AuthResponse,
  Certificate,
  DocumentDetail,
  DocumentSummary,
  SignSession,
  SignatureField,
  SignatureType,
  User,
} from './types';

export function register(body: {
  name: string;
  email: string;
  password: string;
  companyName: string;
}) {
  return apiRequest<AuthResponse>('/api/auth/register', {
    method: 'POST',
    body,
    auth: false,
  });
}

export function login(body: { email: string; password: string }) {
  return apiRequest<AuthResponse>('/api/auth/login', {
    method: 'POST',
    body,
    auth: false,
  });
}

export function fetchMe() {
  return apiRequest<User>('/api/me');
}

export function listCertificates() {
  return apiRequest<Certificate[]>('/api/me/certificates');
}

export function uploadCertificate(file: File, password: string) {
  const form = new FormData();
  form.append('file', file);
  form.append('password', password);
  return apiRequest<Certificate>('/api/me/certificates', {
    method: 'POST',
    rawBody: form,
  });
}

export function revokeCertificate(id: string) {
  return apiRequest<void>(`/api/me/certificates/${id}`, {
    method: 'DELETE',
  });
}

export function listDocuments() {
  return apiRequest<DocumentSummary[]>('/api/documents');
}

export function uploadDocument(file: File, title?: string) {
  const form = new FormData();
  form.append('file', file);
  if (title) {
    form.append('title', title);
  }
  return apiRequest<DocumentDetail>('/api/documents', {
    method: 'POST',
    rawBody: form,
  });
}

export function getDocument(id: string) {
  return apiRequest<DocumentDetail>(`/api/documents/${id}`);
}

export function updateDocument(
  id: string,
  data: { title?: string; signatureType?: SignatureType },
) {
  return apiRequest<DocumentDetail>(`/api/documents/${id}`, {
    method: 'PUT',
    body: data,
  });
}

export function updateSigners(
  id: string,
  signers: { name: string; email: string }[],
) {
  return apiRequest<DocumentDetail>(`/api/documents/${id}/signers`, {
    method: 'PUT',
    body: signers,
  });
}

export function updateFields(id: string, fields: SignatureField[]) {
  return apiRequest<DocumentDetail>(`/api/documents/${id}/fields`, {
    method: 'PUT',
    body: fields,
  });
}

export function sendDocument(id: string) {
  return apiRequest<DocumentDetail>(`/api/documents/${id}/send`, {
    method: 'POST',
  });
}

export function downloadDocument(id: string) {
  return apiBlob(`/api/documents/${id}/download`);
}

export function getSignSession(token: string) {
  return apiRequest<SignSession>(`/api/sign/${token}`, { auth: false });
}

export function signSimple(
  token: string,
  data: { typedName: string; signatureImageBase64?: string },
) {
  return apiRequest<void>(`/api/sign/${token}/simple`, {
    method: 'POST',
    body: data,
    auth: false,
  });
}

export function signCertificate(token: string, pfxPassword: string) {
  return apiRequest<void>(`/api/sign/${token}/certificate`, {
    method: 'POST',
    body: { pfxPassword },
    auth: false,
  });
}

export function downloadSignedDocument(token: string) {
  return apiBlob(`/api/sign/${token}/download`, { auth: false });
}
