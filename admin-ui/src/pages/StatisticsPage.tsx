import { Alert, Box, Card, CardContent, CircularProgress, Container, Grid, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { getAdminStatistics } from '../api/client';

function Stat({ label, value }: { label: string; value: number | string }) {
  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Typography variant="overline" color="text.secondary">{label}</Typography>
        <Typography variant="h5">{value}</Typography>
      </CardContent>
    </Card>
  );
}

export default function StatisticsPage() {
  const q = useQuery({ queryKey: ['admin-statistics'], queryFn: getAdminStatistics });
  return (
    <Container sx={{ py: 4 }}>
      <Typography variant="h4" gutterBottom>Statistik</Typography>
      {q.isLoading && <Box display="flex" justifyContent="center" my={4}><CircularProgress /></Box>}
      {q.isError && <Alert severity="error">Konnte Statistik nicht laden.</Alert>}
      {q.data && (
        <Grid container spacing={2}>
          <Grid item xs={12} sm={6} md={3}><Stat label="Nutzer" value={q.data.users} /></Grid>
          <Grid item xs={12} sm={6} md={3}><Stat label="Rezepte" value={q.data.recipes} /></Grid>
          <Grid item xs={12} sm={6} md={3}><Stat label="Zutaten" value={q.data.ingredients} /></Grid>
          <Grid item xs={12} sm={6} md={3}><Stat label="Supplements" value={q.data.supplements} /></Grid>
          <Grid item xs={12} sm={6} md={3}><Stat label="Approved Zutaten" value={q.data.approved_ingredients} /></Grid>
          <Grid item xs={12} sm={6} md={3}><Stat label="Rejected Zutaten" value={q.data.rejected_ingredients} /></Grid>
          <Grid item xs={12} sm={6} md={3}><Stat label="Approved Supplements" value={q.data.approved_supplements} /></Grid>
          <Grid item xs={12} sm={6} md={3}><Stat label="Rejected Supplements" value={q.data.rejected_supplements} /></Grid>
        </Grid>
      )}
    </Container>
  );
}
