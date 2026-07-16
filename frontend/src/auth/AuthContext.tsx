// Auth state shared across the whole app via React Context, so any component can
// ask "who's logged in?" without passing the session down through every prop.
// The session is persisted to localStorage so a refresh keeps you logged in.

import { createContext, useContext, useMemo, useState, type ReactNode } from 'react';
import { mockApi } from '../mock/api';
import type { AuthSession } from '../types';

const STORAGE_KEY = 'clinic.session';

interface AuthContextValue {
  session: AuthSession | null;
  login: (email: string, password: string) => Promise<AuthSession>;
  register: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function loadSession(): AuthSession | null {
  const raw = localStorage.getItem(STORAGE_KEY);
  return raw ? (JSON.parse(raw) as AuthSession) : null;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  // Lazy initializer: read localStorage once on first render, not every render.
  const [session, setSession] = useState<AuthSession | null>(loadSession);

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      async login(email, password) {
        const next = await mockApi.login(email, password);
        localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
        setSession(next);
        return next;
      },
      async register(email, password) {
        await mockApi.register(email, password);
      },
      logout() {
        localStorage.removeItem(STORAGE_KEY);
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
