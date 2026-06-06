import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import AuditLogPage from './pages/AuditLogPage';
import RecipeReportsPage from './pages/RecipeReportsPage';
import SupplementsQueuePage from './pages/SupplementsQueuePage';
import IngredientQueuePage from './pages/IngredientQueuePage';
import ReleasesPage from './pages/ReleasesPage';
import FieldPrPage from './pages/FieldPrPage';
import UsersPage from './pages/UsersPage';
import RecipeQueuePage from './pages/RecipeQueuePage';
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
                <Route path="/supplements" element={<SupplementsQueuePage />} />
                <Route path="/ingredients" element={<IngredientQueuePage />} />
                <Route path="/field-prs" element={<FieldPrPage />} />
                <Route path="/users" element={<UsersPage />} />
                <Route path="/releases" element={<ReleasesPage />} />
                <Route path="/recipes" element={<RecipeQueuePage />} />
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
