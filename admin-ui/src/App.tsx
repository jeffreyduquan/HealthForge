import { AppBar, Box, Button, Container, Stack, Toolbar, Typography } from '@mui/material';
import { Routes, Route, Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import InvitesPage from './pages/InvitesPage';
import { tokens, logout } from './api/client';

function RequireAuth({ children }: { children: React.ReactNode }) {
  const location = useLocation();
  if (!tokens.isLoggedIn()) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }
  return <>{children}</>;
}

function DashboardPlaceholder() {
  return (
    <Container sx={{ py: 4 }}>
      <Typography variant="h4" gutterBottom>HealthForge Admin</Typography>
      <Typography>
        Willkommen. Nutze die Navigation oben, um Einladungen zu verwalten.
        Weitere Module (Zutaten, Jobs, Rezepte, Reports) folgen in den nächsten Sprints.
      </Typography>
    </Container>
  );
}

function Shell({ children }: { children: React.ReactNode }) {
  const navigate = useNavigate();
  const onLogout = async () => {
    await logout();
    navigate('/login');
  };
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <AppBar position="sticky" color="primary">
        <Toolbar>
          <Typography
            variant="h6"
            component={Link}
            to="/"
            sx={{ color: 'inherit', textDecoration: 'none', flexGrow: 1 }}
          >
            HealthForge Admin
          </Typography>
          <Stack direction="row" spacing={2}>
            <Button component={Link} to="/" color="inherit">Übersicht</Button>
            <Button component={Link} to="/invites" color="inherit">Einladungen</Button>
            <Button onClick={onLogout} color="inherit">Abmelden</Button>
          </Stack>
        </Toolbar>
      </AppBar>
      <Box component="main" sx={{ flexGrow: 1 }}>{children}</Box>
    </Box>
  );
}

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/*"
        element={
          <RequireAuth>
            <Shell>
              <Routes>
                <Route path="/" element={<DashboardPlaceholder />} />
                <Route path="/invites" element={<InvitesPage />} />
                <Route path="*" element={<DashboardPlaceholder />} />
              </Routes>
            </Shell>
          </RequireAuth>
        }
      />
    </Routes>
  );
}
