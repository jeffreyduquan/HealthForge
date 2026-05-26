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
  Typography,
} from '@mui/material';
import {
  banUser,
  deleteUser,
  listUsers,
  unbanUser,
  type AdminUser,
  type UserStatus,
} from '../api/client';

type Confirm =
  | { kind: 'ban'; user: AdminUser }
  | { kind: 'unban'; user: AdminUser }
  | { kind: 'delete'; user: AdminUser };

function statusColor(s: UserStatus): 'success' | 'warning' | 'error' | 'default' {
  switch (s) {
    case 'ACTIVE':
      return 'success';
    case 'PENDING_VERIFICATION':
      return 'warning';
    case 'BANNED':
      return 'error';
    case 'DELETED':
      return 'default';
  }
}

export default function UsersPage() {
  const qc = useQueryClient();
  const [confirm, setConfirm] = useState<Confirm | null>(null);
  const [snack, setSnack] = useState<string | null>(null);

  const q = useQuery({ queryKey: ['users'], queryFn: listUsers });

  const banM = useMutation({
    mutationFn: (id: string) => banUser(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['users'] });
      setSnack('Nutzer gebannt');
    },
    onError: (e: unknown) => setSnack(`Fehler: ${(e as Error).message}`),
  });
  const unbanM = useMutation({
    mutationFn: (id: string) => unbanUser(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['users'] });
      setSnack('Bann aufgehoben');
    },
    onError: (e: unknown) => setSnack(`Fehler: ${(e as Error).message}`),
  });
  const delM = useMutation({
    mutationFn: (id: string) => deleteUser(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['users'] });
      setSnack('Nutzer gelöscht');
    },
    onError: (e: unknown) => setSnack(`Fehler: ${(e as Error).message}`),
  });

  const performConfirm = () => {
    if (!confirm) return;
    if (confirm.kind === 'ban') banM.mutate(confirm.user.id);
    if (confirm.kind === 'unban') unbanM.mutate(confirm.user.id);
    if (confirm.kind === 'delete') delM.mutate(confirm.user.id);
    setConfirm(null);
  };

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" sx={{ mb: 2 }}>
        Nutzer
      </Typography>
      {q.isLoading && <CircularProgress />}
      {q.isError && <Alert severity="error">Fehler beim Laden</Alert>}
      {q.data && (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Email</TableCell>
                <TableCell>Name</TableCell>
                <TableCell>Rolle</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Erstellt</TableCell>
                <TableCell>Letzter Login</TableCell>
                <TableCell align="right">Aktionen</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {q.data.map((u) => {
                const isAdmin = u.role === 'ADMIN';
                const isDeleted = u.status === 'DELETED';
                return (
                  <TableRow key={u.id} hover>
                    <TableCell>{u.email}</TableCell>
                    <TableCell>{u.displayName}</TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        label={u.role}
                        color={isAdmin ? 'primary' : 'default'}
                      />
                    </TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        label={u.status}
                        color={statusColor(u.status)}
                      />
                    </TableCell>
                    <TableCell>
                      {new Date(u.createdAt).toLocaleDateString('de-DE')}
                    </TableCell>
                    <TableCell>
                      {u.lastLoginAt
                        ? new Date(u.lastLoginAt).toLocaleString('de-DE')
                        : '—'}
                    </TableCell>
                    <TableCell align="right">
                      {isAdmin || isDeleted ? (
                        <Typography variant="caption" color="text.secondary">
                          —
                        </Typography>
                      ) : (
                        <Stack direction="row" spacing={1} justifyContent="flex-end">
                          {u.status === 'BANNED' ? (
                            <Button
                              size="small"
                              variant="outlined"
                              color="success"
                              onClick={() => setConfirm({ kind: 'unban', user: u })}
                            >
                              Unban
                            </Button>
                          ) : (
                            <Button
                              size="small"
                              variant="outlined"
                              color="warning"
                              onClick={() => setConfirm({ kind: 'ban', user: u })}
                            >
                              Ban
                            </Button>
                          )}
                          <Button
                            size="small"
                            variant="contained"
                            color="error"
                            onClick={() => setConfirm({ kind: 'delete', user: u })}
                          >
                            Löschen
                          </Button>
                        </Stack>
                      )}
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <Dialog open={!!confirm} onClose={() => setConfirm(null)}>
        <DialogTitle>
          {confirm?.kind === 'delete' ? 'Nutzer löschen?' : 'Aktion bestätigen'}
        </DialogTitle>
        <DialogContent>
          <DialogContentText>
            {confirm?.kind === 'ban' &&
              `Nutzer ${confirm.user.email} bannen. Login wird abgelehnt, alle Refresh-Tokens revoked. Hinweis: aktive Access-Tokens bleiben bis zu ~15 min gültig.`}
            {confirm?.kind === 'unban' &&
              `Bann für ${confirm.user.email} aufheben (Status → ACTIVE).`}
            {confirm?.kind === 'delete' &&
              `Nutzer ${confirm.user.email} wird soft-deleted (Status → DELETED). Alle Refresh-Tokens werden revoked.`}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirm(null)}>Abbrechen</Button>
          <Button
            onClick={performConfirm}
            variant="contained"
            color={
              confirm?.kind === 'delete'
                ? 'error'
                : confirm?.kind === 'ban'
                  ? 'warning'
                  : 'primary'
            }
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
