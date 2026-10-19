import type { ApiErrorBody } from './types';

const ACCESS_TOKEN_KEY = 'pdf_sign_access_token';
const REFRESH_TOKEN_KEY = 'pdf_sign_refresh_token';

export class ApiError extends Error {
  status: number;
  body?: ApiErrorBody;

  constructor(status: number, message: string, body?: ApiErrorBody) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
  }
}

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function setTokens(accessToken: string, refreshToken: string): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
}

export function clearTokens(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

type RequestOptions = Omit<RequestInit, 'body'> & {
  body?: unknown;
  auth?: boolean;
  rawBody?: BodyInit;
};

async function parseError(res: Response): Promise<ApiError> {
  let body: ApiErrorBody | undefined;
  try {
    body = (await res.json()) as ApiErrorBody;
  } catch {
    body = undefined;
  }
  const message =
    body?.message || body?.error || res.statusText || 'Erro na requisição';
  return new ApiError(res.status, message, body);
}

export async function apiRequest<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const { auth = true, body, rawBody, headers: initHeaders, ...rest } = options;

  const headers = new Headers(initHeaders);

  if (auth) {
    const token = getAccessToken();
    if (token) {
      headers.set('Authorization', `Bearer ${token}`);
    }
  }

  let payload: BodyInit | undefined = rawBody;
  if (body !== undefined && rawBody === undefined) {
    headers.set('Content-Type', 'application/json');
    payload = JSON.stringify(body);
  }

  const res = await fetch(path, {
    ...rest,
    headers,
    body: payload,
  });

  if (!res.ok) {
    throw await parseError(res);
  }

  if (res.status === 204) {
    return undefined as T;
  }

  const contentType = res.headers.get('content-type') ?? '';
  if (contentType.includes('application/json')) {
    return (await res.json()) as T;
  }

  return res as unknown as T;
}

export async function apiBlob(
  path: string,
  options: RequestOptions = {},
): Promise<Blob> {
  const { auth = true, body, rawBody, headers: initHeaders, ...rest } = options;

  const headers = new Headers(initHeaders);

  if (auth) {
    const token = getAccessToken();
    if (token) {
      headers.set('Authorization', `Bearer ${token}`);
    }
  }

  let payload: BodyInit | undefined = rawBody;
  if (body !== undefined && rawBody === undefined) {
    headers.set('Content-Type', 'application/json');
    payload = JSON.stringify(body);
  }

  const res = await fetch(path, {
    ...rest,
    headers,
    body: payload,
  });

  if (!res.ok) {
    throw await parseError(res);
  }

  return res.blob();
}
