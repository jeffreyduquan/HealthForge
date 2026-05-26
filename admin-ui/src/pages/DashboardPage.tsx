import { Alert, Box, Card, CardContent, CircularProgress, Container, Grid, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { getAdminDashboard } from '../api/client';

interface MetricProps {
  label: string;
  value: number | string;
  highlight?: boolean;
}

function Metric({ label, value, highlight }: MetricProps) {
  return (
    <Card sx={{ height: '100%', borderColor: highlight ? 'warning.main' : undefined, borderWidth: highlight ? 2 : 1, borderStyle: 'solid' }}>
      <CardContent>
        <Typography variant="overline" color="text.secondary">{label}</Typography>
        <Typography variant="h4">{value}</Typography>
      </CardContent>
    </Card>
  );
}

export default function DashboardPage() {
  const q = useQuery({ queryKey: ['admin-dashboard'], queryFn: getAdminDashboard });
  return (
    <Container sx={{ py: 4 }}>
      <Typography variant="h4" gutterBottom>Dashboard</Typography>
      {q.isLoading && <Box display="flex" justifyContent="center" my={4}><CircularProgress /></Box>}
      {q.isError && <Alert severity="error">Konnte Dashboard nicht laden.</Alert>}
      {q.data && (
        <Grid container spacing={2}>
          <Grid item xs={12} sm={6} md={3}><Metric label="Nutzer" value={q.data.user_count} /></Grid>
          <Grid item xs={12} sm={6} md={3}><Metric label="Rezepte" value={q.data.recipe_count} /></Grid>
          <Grid item xs={12} sm={6} md={3}><Metric label="Zutaten" value={q.data.ingredient_count} /></Grid>
          <Grid item xs={12} sm={6} md={3}><Metric label="Supplements" value={q.data.supplement_count} /></Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Metric label="Pending Zutaten" value={q.data.pending_ingredients} highlight={q.data.pending_ingredients > 0} />
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Metric label="Pending Field-PRs" value={q.data.pending_field_prs} highlight={q.data.pending_field_prs > 0} />
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Metric label="Pending Supplements" value={q.data.pending_supplements} highlight={q.data.pending_supplements > 0} />
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Metric label="Offene Reports" value={q.data.open_recipe_reports} highlight={q.data.open_recipe_reports > 0} />
          </Grid>
        </Grid>
      )}
    </Container>
  );
}
