import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it } from 'vitest';
import { AuthProvider } from '../auth/AuthContext';
import { ProtectedRoute } from './ProtectedRoute';

function renderAt(path: string) {
  render(
    <AuthProvider>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/login" element={<div>Login page</div>} />
          <Route element={<ProtectedRoute />}>
            <Route path="/secret" element={<div>Secret area</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  );
}

beforeEach(() => localStorage.clear());

describe('ProtectedRoute', () => {
  it('redirects to /login when there is no session', () => {
    renderAt('/secret');
    expect(screen.getByText('Login page')).toBeInTheDocument();
    expect(screen.queryByText('Secret area')).not.toBeInTheDocument();
  });

  it('renders the protected content when a session exists', () => {
    localStorage.setItem(
      'clinic.session',
      JSON.stringify({ token: 't', refreshToken: 'r', email: 'p@x.com', role: 'PATIENT' }),
    );
    renderAt('/secret');
    expect(screen.getByText('Secret area')).toBeInTheDocument();
  });
});
