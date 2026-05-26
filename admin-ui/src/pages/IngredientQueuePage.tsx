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
  Paper,
  Snackbar,
  Stack,
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
  approveIngredient,
  listIngredientQueue,
  rejectIngredient,
  type IngredientQueueEntry,
} from '../api/client';

type Confirm =
  | { kind: 'approve'; row: IngredientQueueEntry }
  | { kind: 'reject'; row: IngredientQueueEntry };

export default function IngredientQueuePage() {
  const qc = useQueryClient();
  const [confirm, setConfirm] = useState<Confirm | null>(null);
  const [rejectNote, setRejectNote] = useState('');
  const [snack, setSnack] = useState<string | null>(null);

  const q = useQuery({ queryKey: ['ingredient-queue'], queryFn: listIngredientQueue });

  const approveM = useMutation({
    mutationFn: (id: string) => approveIngredient(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['ingredient-queue'] });
      setSnack('Ingredient genehmigt');
    },
    onError: () => setSnack('Genehmigung fehlgeschlagen'),
  });

  const rejectM = useMutation({
    mutationFn: ({ id, note }: { id: string; note: string }) =>
      rejectIngredient(id, note.trim() ? note.trim() : undefined),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['ingredient-queue'] });
      setSnack('Ingredient abgelehnt');
    },
    onError: () => setSnack('Ablehnung fehlgeschlagen'),
  });

  const performConfirm = () => {
    if (!confirm) return;
    if (confirm.kind === 'approve') approveM.mutate(confirm.row.id);
    if (confirm.kind === 'reject') rejectM.mutate({ id: confirm.row.id, note: rejectNote });
    setConfirm(null);
  };

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" sx={{ mb: 2 }}>Ingredient-Queue</Typography>
      {q.isLoading && <CircularProgress />}
      {q.isError && <Alert severity="error">Fehler beim Laden</Alert>}
      {q.data && q.data.length === 0 && <Alert severity="info">Keine ausstehenden Ingredients.</Alert>}
      {q.data && q.data.length > 0 && (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Datum</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Vorschlagende:r</TableCell>
                <TableCell>Name (DE)</TableCell>
                <TableCell>Marke</TableCell>
                <TableCell>Barcode</TableCell>
                <TableCell align="right">Aktionen</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {q.data.map((r) => (
                <TableRow key={r.id} hover>
                  <TableCell>{new Date(r.created_at).toLocaleString('de-DE')}</TableCell>
                  <TableCell><Chip size="small" color="warning" label={r.status} /></TableCell>
                  <TableCell>{r.submitter_email ?? r.submitted_by ?? '—'}</TableCell>
                  <TableCell>{r.name_de}</TableCell>
                  <TableCell>{r.brand ?? '—'}</TableCell>
                  <TableCell>{r.barcode ?? '—'}</TableCell>
                  <TableCell align="right">
                    <Stack direction="row" spacing={1} justifyContent="flex-end">
                      <Button size="small" color="success" variant="contained"
                        onClick={() => { setRejectNote(''); setConfirm({ kind: 'approve', row: r }); }}>
                        Genehmigen
                      </Button>
                      <Button size="small" color="error" variant="outlined"
                        onClick={() => { setRejectNote(''); setConfirm({ kind: 'reject', row: r }); }}>
                        Ablehnen
                      </Button>
                    </Stack>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <Dialog open={!!confirm} onClose={() => setConfirm(null)} fullWidth maxWidth="sm">
        <DialogTitle>
          {confirm?.kind === 'approve' ? 'Ingredient genehmigen?' : 'Ingredient ablehnen?'}
        </DialogTitle>
        <DialogContent>
          <DialogContentText sx={{ mb: 2 }}>
            {confirm && `"${confirm.row.name_de}" wird ${confirm.kind === 'approve' ? 'global sichtbar' : 'als REJECTED markiert'}.`}
          </DialogContentText>
          {confirm?.kind === 'reject' && (
            <TextField
              autoFocus fullWidth multiline minRows={2}
              label="Begründung (optional, max 500)"
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
