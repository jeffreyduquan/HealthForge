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
  approveSupplementSuggestion,
  listSupplementSuggestions,
  rejectSupplementSuggestion,
  type SupplementSuggestionAdmin,
} from '../api/client';

type Confirm =
  | { kind: 'approve'; row: SupplementSuggestionAdmin }
  | { kind: 'reject'; row: SupplementSuggestionAdmin };

function statusColor(s: string): 'warning' | 'success' | 'error' | 'default' {
  switch (s) {
    case 'PENDING':
      return 'warning';
    case 'APPROVED':
      return 'success';
    case 'REJECTED':
      return 'error';
    default:
      return 'default';
  }
}

export default function SupplementsQueuePage() {
  const qc = useQueryClient();
  const [onlyPending, setOnlyPending] = useState(true);
  const [confirm, setConfirm] = useState<Confirm | null>(null);
  const [rejectNote, setRejectNote] = useState('');
  const [snack, setSnack] = useState<string | null>(null);

  const q = useQuery({
    queryKey: ['supplement-suggestions', onlyPending],
    queryFn: () => listSupplementSuggestions(onlyPending),
  });

  const approveM = useMutation({
    mutationFn: (id: string) => approveSupplementSuggestion(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['supplement-suggestions'] });
      setSnack('Vorschlag genehmigt — Eintrag im globalen Katalog erstellt');
    },
    onError: () => setSnack('Genehmigung fehlgeschlagen'),
  });

  const rejectM = useMutation({
    mutationFn: ({ id, note }: { id: string; note: string }) =>
      rejectSupplementSuggestion(id, note.trim() ? note.trim() : undefined),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['supplement-suggestions'] });
      setSnack('Vorschlag abgelehnt');
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
        <Typography variant="h4">Supplement-Vorschläge</Typography>
        <FormControlLabel
          control={<Switch checked={onlyPending} onChange={(_, v) => setOnlyPending(v)} />}
          label="Nur ausstehende"
        />
      </Stack>

      {q.isLoading && <CircularProgress />}
      {q.isError && <Alert severity="error">Fehler beim Laden</Alert>}
      {q.data && q.data.length === 0 && (
        <Alert severity="info">Keine Vorschläge gefunden.</Alert>
      )}
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
                <TableCell>Dosis</TableCell>
                <TableCell>kcal/Dosis</TableCell>
                <TableCell>Notizen</TableCell>
                <TableCell align="right">Aktionen</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {q.data.map((r) => (
                <TableRow key={r.id} hover>
                  <TableCell>{new Date(r.created_at).toLocaleString('de-DE')}</TableCell>
                  <TableCell>
                    <Chip size="small" color={statusColor(r.status)} label={r.status} />
                  </TableCell>
                  <TableCell>{r.proposer_email ?? r.proposer_id}</TableCell>
                  <TableCell>{r.name_de}</TableCell>
                  <TableCell>{r.brand ?? '—'}</TableCell>
                  <TableCell>
                    {r.default_dose} {r.unit_label}
                  </TableCell>
                  <TableCell>{r.kcal_per_dose ?? '—'}</TableCell>
                  <TableCell sx={{ maxWidth: 240 }}>{r.notes ?? '—'}</TableCell>
                  <TableCell align="right">
                    {r.status === 'PENDING' ? (
                      <Stack direction="row" spacing={1} justifyContent="flex-end">
                        <Button
                          size="small"
                          color="success"
                          variant="contained"
                          onClick={() => openConfirm({ kind: 'approve', row: r })}
                        >
                          Genehmigen
                        </Button>
                        <Button
                          size="small"
                          color="error"
                          variant="outlined"
                          onClick={() => openConfirm({ kind: 'reject', row: r })}
                        >
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
          {confirm?.kind === 'approve' ? 'Vorschlag genehmigen?' : 'Vorschlag ablehnen?'}
        </DialogTitle>
        <DialogContent>
          <DialogContentText sx={{ mb: 2 }}>
            {confirm?.kind === 'approve' &&
              `"${confirm.row.name_de}" wird in den globalen Katalog (supplements_public) übernommen. ` +
                'Alle User können diesen Eintrag dann lesen.'}
            {confirm?.kind === 'reject' &&
              `"${confirm.row.name_de}" wird als REJECTED markiert. Notiz optional.`}
          </DialogContentText>
          {confirm?.kind === 'reject' && (
            <TextField
              autoFocus
              fullWidth
              multiline
              minRows={2}
              label="Begründung (optional, max 500)"
              inputProps={{ maxLength: 500 }}
              value={rejectNote}
              onChange={(e) => setRejectNote(e.target.value)}
            />
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirm(null)}>Abbrechen</Button>
          <Button
            onClick={performConfirm}
            variant="contained"
            color={confirm?.kind === 'approve' ? 'success' : 'error'}
            disabled={approveM.isPending || rejectM.isPending}
          >
            Bestätigen
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={!!snack}
        autoHideDuration={4000}
        onClose={() => setSnack(null)}
        message={snack ?? ''}
      />
    </Box>
  );
}
