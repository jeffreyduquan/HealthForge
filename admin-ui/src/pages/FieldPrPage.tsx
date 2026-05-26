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
  approveFieldPr,
  listFieldPrs,
  rejectFieldPr,
  type FieldPrAdmin,
} from '../api/client';

type Confirm =
  | { kind: 'approve'; row: FieldPrAdmin }
  | { kind: 'reject'; row: FieldPrAdmin };

function statusColor(s: string): 'warning' | 'success' | 'error' | 'default' {
  switch (s) {
    case 'PENDING': return 'warning';
    case 'APPROVED': return 'success';
    case 'REJECTED': return 'error';
    default: return 'default';
  }
}

export default function FieldPrPage() {
  const qc = useQueryClient();
  const [onlyPending, setOnlyPending] = useState(true);
  const [confirm, setConfirm] = useState<Confirm | null>(null);
  const [rejectNote, setRejectNote] = useState('');
  const [snack, setSnack] = useState<string | null>(null);

  const q = useQuery({
    queryKey: ['field-prs', onlyPending],
    queryFn: () => listFieldPrs(onlyPending),
  });

  const approveM = useMutation({
    mutationFn: (id: string) => approveFieldPr(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['field-prs'] });
      setSnack('Field-PR genehmigt — Wert übernommen');
    },
    onError: () => setSnack('Genehmigung fehlgeschlagen'),
  });

  const rejectM = useMutation({
    mutationFn: ({ id, note }: { id: string; note: string }) =>
      rejectFieldPr(id, note.trim() ? note.trim() : undefined),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['field-prs'] });
      setSnack('Field-PR abgelehnt');
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
      <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 2 }}>
        <Typography variant="h4">Field-PRs</Typography>
        <FormControlLabel
          control={<Switch checked={onlyPending} onChange={(_, v) => setOnlyPending(v)} />}
          label="Nur ausstehende"
        />
      </Stack>
      {q.isLoading && <CircularProgress />}
      {q.isError && <Alert severity="error">Fehler beim Laden</Alert>}
      {q.data && q.data.length === 0 && <Alert severity="info">Keine Field-PRs gefunden.</Alert>}
      {q.data && q.data.length > 0 && (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Datum</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Vorschlagende:r</TableCell>
                <TableCell>Ingredient</TableCell>
                <TableCell>Feld</TableCell>
                <TableCell>Alt</TableCell>
                <TableCell>Neu</TableCell>
                <TableCell>Begründung</TableCell>
                <TableCell align="right">Aktionen</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {q.data.map((r) => (
                <TableRow key={r.id} hover>
                  <TableCell>{new Date(r.created_at).toLocaleString('de-DE')}</TableCell>
                  <TableCell><Chip size="small" color={statusColor(r.status)} label={r.status} /></TableCell>
                  <TableCell>{r.proposer_email ?? r.proposer_id}</TableCell>
                  <TableCell>{r.ingredient_name}</TableCell>
                  <TableCell><code>{r.field_name}</code></TableCell>
                  <TableCell sx={{ maxWidth: 180, color: 'text.secondary' }}>{r.old_value ?? '—'}</TableCell>
                  <TableCell sx={{ maxWidth: 180, fontWeight: 600 }}>{r.new_value}</TableCell>
                  <TableCell sx={{ maxWidth: 240 }}>{r.rationale ?? '—'}</TableCell>
                  <TableCell align="right">
                    {r.status === 'PENDING' ? (
                      <Stack direction="row" spacing={1} justifyContent="flex-end">
                        <Button size="small" color="success" variant="contained"
                          onClick={() => { setRejectNote(''); setConfirm({ kind: 'approve', row: r }); }}>
                          Übernehmen
                        </Button>
                        <Button size="small" color="error" variant="outlined"
                          onClick={() => { setRejectNote(''); setConfirm({ kind: 'reject', row: r }); }}>
                          Ablehnen
                        </Button>
                      </Stack>
                    ) : (
                      <Typography variant="caption" color="text.secondary">
                        {r.reviewed_at ? new Date(r.reviewed_at).toLocaleString('de-DE') : ''}
                      </Typography>
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
          {confirm?.kind === 'approve' ? 'Field-PR übernehmen?' : 'Field-PR ablehnen?'}
        </DialogTitle>
        <DialogContent>
          <DialogContentText sx={{ mb: 2 }}>
            {confirm && confirm.kind === 'approve' &&
              `Feld "${confirm.row.field_name}" auf "${confirm.row.ingredient_name}" wird auf "${confirm.row.new_value}" gesetzt.`}
            {confirm && confirm.kind === 'reject' &&
              `Vorschlag für "${confirm.row.ingredient_name}.${confirm.row.field_name}" wird als REJECTED markiert. Notiz optional.`}
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
            {confirm?.kind === 'approve' ? 'Übernehmen' : 'Ablehnen'}
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={!!snack} autoHideDuration={4000} onClose={() => setSnack(null)} message={snack ?? ''} />
    </Box>
  );
}
