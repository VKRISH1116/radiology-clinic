// The index route ("/") just forwards you to the dashboard for your role.
import { Navigate } from 'react-router-dom';
import { homePathForRole, useAuth } from '../auth/AuthContext';

export function HomeRedirect() {
  const { session } = useAuth();
  return <Navigate to={session ? homePathForRole(session.role) : '/login'} replace />;
}
