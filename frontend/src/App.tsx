// Route table. Public routes (login/register) sit outside the auth gate; everything
// else is wrapped in <ProtectedRoute> (must be logged in) and <Layout> (top bar),
// with an inner gate per role so a patient can't open the admin console by URL.

import { Navigate, Route, Routes } from 'react-router-dom';
import { Layout } from './components/Layout';
import { AdminDashboard } from './pages/admin/AdminDashboard';
import { LoginPage } from './pages/LoginPage';
import { PatientDashboard } from './pages/patient/PatientDashboard';
import { RegisterPage } from './pages/RegisterPage';
import { StaffDashboard } from './pages/staff/StaffDashboard';
import { HomeRedirect } from './routes/HomeRedirect';
import { ProtectedRoute } from './routes/ProtectedRoute';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route path="/" element={<HomeRedirect />} />

          <Route element={<ProtectedRoute allow={['PATIENT']} />}>
            <Route path="/patient" element={<PatientDashboard />} />
          </Route>
          <Route element={<ProtectedRoute allow={['STAFF', 'ADMIN']} />}>
            <Route path="/staff" element={<StaffDashboard />} />
          </Route>
          <Route element={<ProtectedRoute allow={['ADMIN']} />}>
            <Route path="/admin" element={<AdminDashboard />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
