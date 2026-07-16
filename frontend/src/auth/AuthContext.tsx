// Auth state shared across the app via React Context. It mirrors the persisted
// session (in localStorage, managed by src/api/session) in React state so the UI
// re-renders on login/logout. The actual HTTP calls live in src/api/auth.

import { createContext, useContext, useMemo, useState, type ReactNode } from 'react';
import {
  login as apiLogin,
  logout as apiLogout,
  register as apiRegister,
} from '../api/auth';
import { getSession } from '../api/session';
import type { AuthSession } from '../types';

interface AuthContextValue {
  session: AuthSession | null;
  login: (email: string, password: string) => Promise<AuthSession>;
  register: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  // Read any persisted session once on first render.
  const [session, setSession] = useState<AuthSession | null>(getSession);

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      async login(email, password) {
        const next = await apiLogin(email, password);
        setSession(next);
        return next;
      },
      async register(email, password) {
        await apiRegister(email, password);
      },
      logout() {
        void apiLogout(); // revoke the refresh token server-side, best-effort
        setSession(null);
      },
    }),
    [session],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

/** Hook to read auth state; throws if used outside <AuthProvider> (a wiring bug). */
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
}

/** Where a given role lands after login. */
export function homePathForRole(role: AuthSession['role']): string {
  switch (role) {
    case 'PATIENT':
      return '/patient';
    case 'STAFF':
      return '/staff';
    case 'ADMIN':
      return '/admin';
  }
}
