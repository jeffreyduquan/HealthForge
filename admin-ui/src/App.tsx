import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import AuditLogPage from './pages/AuditLogPage';
import RecipeReportsPage from './pages/RecipeReportsPage';
import PendingPage from './pages/PendingPage';
import ReleasesPage from './pages/ReleasesPage';
import UsersPage from './pages/UsersPage';
import DatabasePage from './pages/DatabasePage';
import Layout from './components/Layout';
import { tokens } from './api/client';

function RequireAuth({ children }: { children: React.ReactNode }) {
  const location = useLocation();
  if (!tokens.isLoggedIn()) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }
  return <>{children}</>;
}

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/*"
        element={
          <RequireAuth>
            <Layout>
              <Routes>
                <Route path="/" element={<DashboardPage />} />
                <Route path="/audit" element={<AuditLogPage />} />
                <Route path="/reports" element={<RecipeReportsPage />} />
                <Route path="/pending" element={<PendingPage />} />
                <Route path="/users" element={<UsersPage />} />
                <Route path="/releases" element={<ReleasesPage />} />
                <Route path="/database" element={<DatabasePage />} />
                <Route path="*" element={<DashboardPage />} />
              </Routes>
            </Layout>
          </RequireAuth>
        }
      />
    </Routes>
  );
}
