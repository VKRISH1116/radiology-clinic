// The single place that reads/writes the logged-in session (access + refresh
// tokens + identity) in localStorage. Both the AuthContext and the HTTP client
// use these helpers so there's one source of truth for the tokens.

import type { AuthSession } from '../types';

const KEY = 'clinic.session';

export function getSession(): AuthSession | null {
  const raw = localStorage.getItem(KEY);
  return raw ? (JSON.parse(raw) as AuthSession) : null;
}

export function saveSession(session: AuthSession): void {
  localStorage.setItem(KEY, JSON.stringify(session));
}

export function clearSession(): void {
  localStorage.removeItem(KEY);
}
