import { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Container,
  Paper,
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
import { useQuery } from '@tanstack/react-query';
import { listAuditLog, type AuditQuery } from '../api/client';

export default function AuditLogPage() {
  const [actor, setActor] = useState('');
  const [action, setAction] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [committed, setCommitted] = useState<AuditQuery>({});

  const q = useQuery({
    queryKey: ['admin-audit', committed],
    queryFn: () => listAuditLog(committed),
  });

  const apply = () => {
    setCommitted({
      actor: actor.trim() || undefined,
      action: action.trim() || undefined,
      from: from.trim() || undefined,
      to: to.trim() || undefined,
      limit: 200,
    });
  };

  return (
    <Container sx={{ py: 4 }}>
      <Typography variant="h4" gutterBottom>Audit-Log</Typography>
      <Paper sx={{ p: 2, mb: 2 }}>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems="center">
          <TextField
            label="Actor (USER/ADMIN/SYSTEM oder UUID)"
            size="small"
            value={actor}
            onChange={(e) => setActor(e.target.value)}
            sx={{ minWidth: 240 }}
          />
          <TextField
            label="Action"
            size="small"
            value={action}
            onChange={(e) => setAction(e.target.value)}
          />
          <TextField
            label="Von (ISO-8601)"
            size="small"
            placeholder="2026-05-01T00:00:00Z"
            value={from}
            onChange={(e) => setFrom(e.target.value)}
          />
          <TextField
            label="Bis (ISO-8601)"
            size="small"
            placeholder="2026-05-31T23:59:59Z"
            value={to}
            onChange={(e) => setTo(e.target.value)}
          />
          <Button variant="contained" onClick={apply}>Filtern</Button>
        </Stack>
      </Paper>
      {q.isLoading && <Box display="flex" justifyContent="center" my={4}><CircularProgress /></Box>}
      {q.isError && <Alert severity="error">Audit-Log konnte nicht geladen werden.</Alert>}
      {q.data && (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Zeitpunkt</TableCell>
                <TableCell>Actor</TableCell>
                <TableCell>Action</TableCell>
                <TableCell>Target</TableCell>
                <TableCell>IP</TableCell>
                <TableCell>Detail</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {q.data.map((row) => (
                <TableRow key={row.id}>
                  <TableCell>{row.occurred_at}</TableCell>
                  <TableCell>{row.actor_kind}{row.actor_user_id ? ` / ${row.actor_user_id.slice(0, 8)}` : ''}</TableCell>
                  <TableCell>{row.action}</TableCell>
                  <TableCell>{row.target_type ? `${row.target_type}:${row.target_id ?? '?'}` : '—'}</TableCell>
                  <TableCell>{row.ip_address ?? '—'}</TableCell>
                  <TableCell sx={{ maxWidth: 320, whiteSpace: 'pre-wrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{row.detail ?? '—'}</TableCell>
                </TableRow>
              ))}
              {q.data.length === 0 && (
                <TableRow><TableCell colSpan={6} align="center">Keine Einträge.</TableCell></TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Container>
  );
}
