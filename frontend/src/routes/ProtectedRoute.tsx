// A gate for routes. If nobody's logged in, bounce to /login. If a role list is
// given and the user isn't in it, send them to their own home instead. <Outlet />
// renders the matched child route when access is allowed.

import { Navigate, Outlet } from 'react-router-dom';
import { homePathForRole, useAuth } from '../auth/AuthContext';
import type { Role } from '../types';

export function ProtectedRoute({ allow }: { allow?: Role[] }) {
  const { session } = useAuth();

  if (!session) {
    return <Navigate to="/login" replace />;
  }
  if (allow && !allow.includes(session.role)) {
    return <Navigate to={homePathForRole(session.role)} replace />;
  }
  return <Outlet />;
}
