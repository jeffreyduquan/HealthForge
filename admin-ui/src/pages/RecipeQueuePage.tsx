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
  TextField,
  Typography,
} from '@mui/material';
import {
  approveRecipe,
  listRecipeQueue,
  rejectRecipe,
  type RecipeQueueEntry,
} from '../api/client';

type Confirm =
  | { kind: 'approve'; row: RecipeQueueEntry }
  | { kind: 'reject'; row: RecipeQueueEntry };

function statusColor(s: string): 'warning' | 'success' | 'error' | 'info' | 'default' {
  switch (s) {
    case 'PENDING_REVIEW': return 'warning';
    case 'PUBLISHED': return 'success';
    case 'REJECTED': return 'error';
    case 'REMOVED': return 'default';
    default: return 'default';
  }
}

export default function RecipeQueuePage() {
  const qc = useQueryClient();
  const [onlyPending, setOnlyPending] = useState(true);
  const [confirm, setConfirm] = useState<Confirm | null>(null);
  const [rejectNote, setRejectNote] = useState('');
  const [snack, setSnack] = useState<string | null>(null);

  const q = useQuery({
    queryKey: ['recipe-queue', onlyPending],
    queryFn: () => listRecipeQueue(onlyPending),
  });

  const approveM = useMutation({
    mutationFn: (id: string) => approveRecipe(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['recipe-queue'] });
      setSnack('Rezept genehmigt — jetzt öffentlich sichtbar');
    },
    onError: () => setSnack('Genehmigung fehlgeschlagen'),
  });

  const rejectM = useMutation({
    mutationFn: ({ id, note }: { id: string; note: string }) =>
      rejectRecipe(id, note.trim() ? note.trim() : undefined),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['recipe-queue'] });
      setSnack('Rezept abgelehnt');
    },
    onError: () => setSnack('Ablehnung fehlgeschlagen'),
  });

  const openConfirm = (c: Confirm) => {
    setRejectNote('');
    setConfirm(c);
  };

  const performConfirm = () => {
    if (!confirm) return;
    if (confirm.kind === 'approve') approveM.mutate(confirm.row.id);
    if (confirm.kind === 'reject') rejectM.mutate({ id: confirm.row.id, note: rejectNote });
    setConfirm(null);
  };

  return (
    <Box sx={{ p: 3 }}>
      <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 2 }}>
        <Typography variant="h4">Rezept-Review</Typography>
        <FormControlLabel
          control={<Switch checked={onlyPending} onChange={(_, v) => setOnlyPending(v)} />}
          label="Nur ausstehende"
        />
      </Stack>

      {q.isLoading && <CircularProgress />}
      {q.isError && <Alert severity="error">Fehler beim Laden</Alert>}
      {q.data && q.data.length === 0 && (
        <Alert severity="info">Keine Rezepte gefunden.</Alert>
      )}
      {q.data && q.data.length > 0 && (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Datum</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Titel</TableCell>
                <TableCell>Sichtbarkeit</TableCell>
                <TableCell>Slots</TableCell>
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
                  <TableCell>{r.title}</TableCell>
                  <TableCell>{r.visibility}</TableCell>
                  <TableCell>{r.slotTags.join(', ')}</TableCell>
                  <TableCell align="right">
                    {r.status === 'PENDING_REVIEW' ? (
                      <Stack direction="row" spacing={1} justifyContent="flex-end">
                        <Button
                          size="small" color="success" variant="contained"
                          onClick={() => openConfirm({ kind: 'approve', row: r })}
                        >
                          Genehmigen
                        </Button>
                        <Button
                          size="small" color="error" variant="outlined"
                          onClick={() => openConfirm({ kind: 'reject', row: r })}
                        >
                          Ablehnen
                        </Button>
                      </Stack>
                    ) : (
                      <Chip size="small" label={r.status === 'PUBLISHED' ? 'Veröffentlicht' : r.status === 'REJECTED' ? 'Abgelehnt' : r.status} />
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <Dialog open={!!confirm} onClose={() => setConfirm(null)} fullWidth maxWidth="sm">
        <DialogTitle>
          {confirm?.kind === 'approve' ? 'Rezept genehmigen?' : 'Rezept ablehnen?'}
        </DialogTitle>
        <DialogContent>
          <DialogContentText sx={{ mb: 2 }}>
            {confirm && `"${confirm.row.title}" wird ${confirm.kind === 'approve' ? 'veröffentlicht' : 'als REJECTED markiert'}.`}
          </DialogContentText>
          {confirm?.kind === 'reject' && (
            <TextField
              autoFocus fullWidth multiline minRows={2}
              label="Begründung (optional)"
              inputProps={{ maxLength: 500 }}
              value={rejectNote}
              onChange={(e) => setRejectNote(e.target.value)}
            />
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirm(null)}>Abbrechen</Button>
          <Button onClick={performConfirm} variant="contained" color={confirm?.kind === 'approve' ? 'success' : 'error'}>
            {confirm?.kind === 'approve' ? 'Genehmigen' : 'Ablehnen'}
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={!!snack} autoHideDuration={4000} onClose={() => setSnack(null)} message={snack ?? ''} />
    </Box>
  );
}
