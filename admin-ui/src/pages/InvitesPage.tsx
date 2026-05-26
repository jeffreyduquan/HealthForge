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
  DialogTitle,
  IconButton,
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
  Tooltip,
  Typography,
} from '@mui/material';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import RefreshIcon from '@mui/icons-material/Refresh';
import { createInvite, listInvites, type Invite } from '../api/client';

export default function InvitesPage() {
  const qc = useQueryClient();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [note, setNote] = useState('');
  const [validDays, setValidDays] = useState(30);
  const [copied, setCopied] = useState<string | null>(null);

  const invitesQ = useQuery({ queryKey: ['invites'], queryFn: listInvites });

  const createM = useMutation({
    mutationFn: () => createInvite(note || null, validDays),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['invites'] });
      setDialogOpen(false);
      setNote('');
      setValidDays(30);
    },
  });

  const copy = async (code: string) => {
    await navigator.clipboard.writeText(code);
    setCopied(code);
  };

  return (
    <Box sx={{ p: 3 }}>
      <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 2 }}>
        <Typography variant="h4">Einladungen</Typography>
        <Stack direction="row" spacing={1}>
          <Tooltip title="Aktualisieren">
            <IconButton onClick={() => invitesQ.refetch()}><RefreshIcon /></IconButton>
          </Tooltip>
          <Button variant="contained" onClick={() => setDialogOpen(true)}>Neue Einladung</Button>
        </Stack>
      </Stack>

      {invitesQ.isError && <Alert severity="error" sx={{ mb: 2 }}>Fehler beim Laden der Einladungen.</Alert>}

      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Code</TableCell>
              <TableCell>Notiz</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Erstellt</TableCell>
              <TableCell>Läuft ab</TableCell>
              <TableCell />
            </TableRow>
          </TableHead>
          <TableBody>
            {invitesQ.isLoading && (
              <TableRow><TableCell colSpan={6} align="center"><CircularProgress size={24} /></TableCell></TableRow>
            )}
            {invitesQ.data?.map((inv: Invite) => {
              const isUsed = inv.usedAt != null;
              const isExpired = new Date(inv.expiresAt) < new Date();
              return (
                <TableRow key={inv.id}>
                  <TableCell>
                    <code style={{ fontFamily: 'monospace' }}>{inv.code}</code>
                  </TableCell>
                  <TableCell>{inv.note ?? '—'}</TableCell>
                  <TableCell>
                    {isUsed
                      ? <Chip label="Eingelöst" color="success" size="small" />
                      : isExpired
                        ? <Chip label="Abgelaufen" color="warning" size="small" />
                        : <Chip label="Offen" color="primary" size="small" />}
                  </TableCell>
                  <TableCell>{new Date(inv.createdAt).toLocaleDateString('de-DE')}</TableCell>
                  <TableCell>{new Date(inv.expiresAt).toLocaleDateString('de-DE')}</TableCell>
                  <TableCell align="right">
                    <Tooltip title="Code kopieren">
                      <IconButton size="small" onClick={() => copy(inv.code)} disabled={isUsed || isExpired}>
                        <ContentCopyIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              );
            })}
            {invitesQ.data?.length === 0 && (
              <TableRow><TableCell colSpan={6} align="center"><Typography color="text.secondary">Keine Einladungen</Typography></TableCell></TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle>Neue Einladung erstellen</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Notiz (optional)"
              value={note}
              onChange={(e) => setNote(e.target.value)}
              fullWidth
            />
            <TextField
              label="Gültig (Tage)"
              type="number"
              value={validDays}
              onChange={(e) => setValidDays(Number(e.target.value) || 30)}
              inputProps={{ min: 1, max: 365 }}
              fullWidth
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Abbrechen</Button>
          <Button onClick={() => createM.mutate()} variant="contained" disabled={createM.isPending}>
            {createM.isPending ? <CircularProgress size={20} /> : 'Erstellen'}
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={!!copied}
        autoHideDuration={2000}
        onClose={() => setCopied(null)}
        message={`Code ${copied} kopiert`}
      />
    </Box>
  );
}
