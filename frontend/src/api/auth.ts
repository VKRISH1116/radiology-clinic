// Real auth calls against the Spring backend. login persists the session; logout
// revokes the refresh token server-side (best-effort) then clears it locally.

import type { AuthSession } from '../types';
import { request } from './client';
import { clearSession, getSession, saveSession } from './session';

export async function login(email: string, password: string): Promise<AuthSession> {
  const session = await request<AuthSession>('/api/auth/login', {
    method: 'POST',
    body: { email, password },
    auth: false,
  });
  saveSession(session);
  return session;
}

export async function register(email: string, password: string): Promise<void> {
  await request<void>('/api/auth/register', {
    method: 'POST',
    body: { email, password },
    auth: false,
  });
}

export async function logout(): Promise<void> {
  const session = getSession();
  if (session?.refreshToken) {
    try {
      await request<void>('/api/auth/logout', {
        method: 'POST',
        body: { refreshToken: session.refreshToken },
        auth: false,
      });
    } catch {
      // Even if the server call fails, we still drop the local session below.
    }
  }
  clearSession();
}
