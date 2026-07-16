// The low-level HTTP client every real API call goes through. It:
//  - prefixes the backend base URL (VITE_API_URL, default localhost:8080),
//  - attaches the access token as a Bearer header,
//  - turns non-2xx responses into a thrown ApiError carrying the server's message,
//  - transparently refreshes the access token once on a 401 and retries.

import type { AuthSession } from '../types';
import { clearSession, getSession, saveSession } from './session';

const BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

interface RequestOptions {
  method?: string;
  body?: unknown; // serialized as JSON
  auth?: boolean; // attach the Bearer token (default true)
}

async function send(
  path: string,
  opts: RequestOptions,
  accessToken: string | null,
): Promise<Response> {
  const headers: Record<string, string> = {};
  if (opts.body !== undefined) headers['Content-Type'] = 'application/json';
  if (opts.auth !== false && accessToken) headers.Authorization = `Bearer ${accessToken}`;

  return fetch(`${BASE_URL}${path}`, {
    method: opts.method ?? 'GET',
    headers,
    body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined,
  });
}

/** Exchange the refresh token for a new session, or null if it's no longer valid. */
async function tryRefresh(session: AuthSession): Promise<AuthSession | null> {
  const res = await fetch(`${BASE_URL}/api/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken: session.refreshToken }),
  });
  if (!res.ok) return null;
  const next = (await res.json()) as AuthSession;
  saveSession(next);
  return next;
}

async function messageFrom(res: Response): Promise<string> {
  try {
    const data = await res.json();
    return data.message ?? data.error ?? `Request failed (${res.status})`;
  } catch {
    return `Request failed (${res.status})`;
  }
}

export async function request<T>(path: string, opts: RequestOptions = {}): Promise<T> {
  const session = getSession();
  let res = await send(path, opts, session?.token ?? null);

  // If the access token expired, refresh once and retry the original request.
  if (res.status === 401 && opts.auth !== false && session?.refreshToken) {
    const refreshed = await tryRefresh(session);
    if (refreshed) {
      res = await send(path, opts, refreshed.token);
    } else {
      clearSession(); // refresh failed -> force re-login
    }
  }

  if (!res.ok) {
    throw new ApiError(res.status, await messageFrom(res));
  }
  if (res.status === 204) {
    return undefined as T;
  }
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

/**
 * Multipart file upload (reports). Uses FormData, so we must NOT set a
 * Content-Type header — the browser adds it with the correct multipart boundary.
 * Same Bearer + refresh-on-401 behaviour as request().
 */
export async function upload(path: string, file: File): Promise<void> {
  const session = getSession();

  const doUpload = (token: string | null) => {
    const form = new FormData();
    form.append('file', file);
    return fetch(`${BASE_URL}${path}`, {
      method: 'POST',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: form,
    });
  };

  let res = await doUpload(session?.token ?? null);
  if (res.status === 401 && session?.refreshToken) {
    const refreshed = await tryRefresh(session);
    if (refreshed) {
      res = await doUpload(refreshed.token);
    } else {
      clearSession();
    }
  }
  if (!res.ok) {
    throw new ApiError(res.status, await messageFrom(res));
  }
}
