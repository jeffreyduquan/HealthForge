import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import StatisticsPage from './pages/StatisticsPage';
import AuditLogPage from './pages/AuditLogPage';
import InvitesPage from './pages/InvitesPage';
import RecipeReportsPage from './pages/RecipeReportsPage';
import SupplementsQueuePage from './pages/SupplementsQueuePage';
import IngredientQueuePage from './pages/IngredientQueuePage';
import FieldPrPage from './pages/FieldPrPage';
import UsersPage from './pages/UsersPage';
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
                <Route path="/statistics" element={<StatisticsPage />} />
                <Route path="/audit" element={<AuditLogPage />} />
                <Route path="/invites" element={<InvitesPage />} />
                <Route path="/reports" element={<RecipeReportsPage />} />
                <Route path="/supplements" element={<SupplementsQueuePage />} />
                <Route path="/ingredients" element={<IngredientQueuePage />} />
                <Route path="/field-prs" element={<FieldPrPage />} />
                <Route path="/users" element={<UsersPage />} />
                <Route path="*" element={<DashboardPage />} />
              </Routes>
            </Layout>
          </RequireAuth>
        }
      />
    </Routes>
  );
}
