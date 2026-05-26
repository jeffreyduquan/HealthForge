import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  FormControlLabel,
  Paper,
  Snackbar,
  Stack,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import {
  deleteRecipe,
  dismissReport,
  listReports,
  resolveReport,
  type ReportAdmin,
} from '../api/client';

type Confirm =
  | { kind: 'resolve'; report: ReportAdmin }
  | { kind: 'dismiss'; report: ReportAdmin }
  | { kind: 'delete'; report: ReportAdmin };

function statusColor(s: string): 'warning' | 'success' | 'default' {
  switch (s) {
    case 'OPEN':
      return 'warning';
    case 'RESOLVED':
      return 'success';
    default:
      return 'default';
  }
}

export default function RecipeReportsPage() {
  const qc = useQueryClient();
  const [onlyOpen, setOnlyOpen] = useState(true);
  const [confirm, setConfirm] = useState<Confirm | null>(null);
  const [snack, setSnack] = useState<string | null>(null);

  const q = useQuery({
    queryKey: ['reports', onlyOpen],
    queryFn: () => listReports(onlyOpen),
  });

  const resolveM = useMutation({
    mutationFn: (id: string) => resolveReport(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['reports'] });
      setSnack('Report aufgelöst');
    },
  });
  const dismissM = useMutation({
    mutationFn: (id: string) => dismissReport(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['reports'] });
      setSnack('Report verworfen');
    },
  });
  const deleteM = useMutation({
    mutationFn: (recipeId: string) => deleteRecipe(recipeId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['reports'] });
      setSnack('Rezept entfernt — offene Reports automatisch resolved');
    },
  });

  const performConfirm = () => {
    if (!confirm) return;
    if (confirm.kind === 'resolve') resolveM.mutate(confirm.report.id);
    if (confirm.kind === 'dismiss') dismissM.mutate(confirm.report.id);
    if (confirm.kind === 'delete') deleteM.mutate(confirm.report.recipeId);
    setConfirm(null);
  };

  return (
    <Box sx={{ p: 3 }}>
      <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 2 }}>
        <Typography variant="h4">Reports (Rezepte)</Typography>
        <FormControlLabel
          control={
            <Switch
              checked={onlyOpen}
              onChange={(_, v) => setOnlyOpen(v)}
            />
          }
          label="Nur offene"
        />
      </Stack>

      {q.isLoading && <CircularProgress />}
      {q.isError && <Alert severity="error">Fehler beim Laden</Alert>}
      {q.data && q.data.length === 0 && (
        <Alert severity="info">Keine Reports gefunden.</Alert>
      )}
      {q.data && q.data.length > 0 && (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Datum</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Rezept</TableCell>
                <TableCell>Reporter</TableCell>
                <TableCell>Grund</TableCell>
                <TableCell align="right">Aktionen</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {q.data.map((r) => (
                <TableRow key={r.id} hover>
                  <TableCell>{new Date(r.createdAt).toLocaleString('de-DE')}</TableCell>
                  <TableCell>
                    <Chip size="small" color={statusColor(r.status)} label={r.status} />
                  </TableCell>
                  <TableCell>
                    <Stack spacing={0.5}>
                      <Typography variant="body2">{r.recipeTitle ?? '—'}</Typography>
                      <Typography variant="caption" color="text.secondary">
                        {r.recipeId} {r.recipeStatus ? `(${r.recipeStatus})` : ''}
                      </Typography>
                    </Stack>
                  </TableCell>
                  <TableCell>{r.reporterEmail ?? r.reporterId}</TableCell>
                  <TableCell sx={{ maxWidth: 320 }}>{r.reason}</TableCell>
                  <TableCell align="right">
                    {r.status === 'OPEN' ? (
                      <Stack direction="row" spacing={1} justifyContent="flex-end">
                        <Button
                          size="small"
                          color="success"
                          variant="outlined"
                          onClick={() => setConfirm({ kind: 'resolve', report: r })}
                        >
                          Resolve
                        </Button>
                        <Button
                          size="small"
                          variant="outlined"
                          onClick={() => setConfirm({ kind: 'dismiss', report: r })}
                        >
                          Dismiss
                        </Button>
                        {r.recipeStatus !== 'REMOVED' && (
                          <Button
                            size="small"
                            color="error"
                            variant="contained"
                            onClick={() => setConfirm({ kind: 'delete', report: r })}
                          >
                            Rezept löschen
                          </Button>
                        )}
                      </Stack>
                    ) : (
                      <Typography variant="caption" color="text.secondary">
                        {r.resolvedAt
                          ? new Date(r.resolvedAt).toLocaleString('de-DE')
                          : ''}
                      </Typography>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <Dialog open={!!confirm} onClose={() => setConfirm(null)}>
        <DialogTitle>
          {confirm?.kind === 'delete' ? 'Rezept entfernen?' : 'Aktion bestätigen'}
        </DialogTitle>
        <DialogContent>
          <DialogContentText>
            {confirm?.kind === 'resolve' &&
              'Report als gelöst markieren — Rezept bleibt unverändert.'}
            {confirm?.kind === 'dismiss' &&
              'Report verwerfen — kein Verstoß erkannt.'}
            {confirm?.kind === 'delete' &&
              `Das Rezept wird soft-deleted (status=REMOVED). Alle offenen Reports zu diesem Rezept werden automatisch als RESOLVED markiert. Fortfahren?`}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirm(null)}>Abbrechen</Button>
          <Button
            onClick={performConfirm}
            variant="contained"
            color={confirm?.kind === 'delete' ? 'error' : 'primary'}
          >
            Bestätigen
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={!!snack}
        autoHideDuration={3500}
        onClose={() => setSnack(null)}
        message={snack ?? ''}
      />
    </Box>
  );
}
