import { useState, useRef } from 'react';
import {
  Alert,
  Box,
  Button,
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
  Typography,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import DownloadIcon from '@mui/icons-material/Download';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  listReleases,
  uploadRelease,
  deleteRelease,
  getReleaseDownloadUrl,
} from '../api/client';

function formatSize(bytes: number): string {
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleString('de-DE');
}

export default function ReleasesPage() {
  const qc = useQueryClient();
  const fileRef = useRef<HTMLInputElement>(null);
  const [uploadOpen, setUploadOpen] = useState(false);
  const [version, setVersion] = useState('');
  const [changelog, setChangelog] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [deleteConfirm, setDeleteConfirm] = useState<string | null>(null);
  const [snack, setSnack] = useState<string | null>(null);

  const q = useQuery({ queryKey: ['releases'], queryFn: listReleases });

  const uploadM = useMutation({
    mutationFn: () => uploadRelease(file!, version, changelog),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['releases'] });
      setUploadOpen(false);
      setVersion('');
      setChangelog('');
      setFile(null);
      setSnack('Release hochgeladen');
    },
    onError: () => setSnack('Upload fehlgeschlagen'),
  });

  const deleteM = useMutation({
    mutationFn: (id: string) => deleteRelease(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['releases'] });
      setDeleteConfirm(null);
      setSnack('Release gelöscht');
    },
    onError: () => setSnack('Löschen fehlgeschlagen'),
  });

  const downloadM = useMutation({
    mutationFn: (id: string) => getReleaseDownloadUrl(id),
    onSuccess: (data) => {
      // Zuverlässiger Download via temporärem Anchor statt window.open (Popup-Blocker)
      const a = document.createElement('a');
      a.href = data.url;
      a.download = data.filename;
      a.style.display = 'none';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
    },
    onError: () => setSnack('Download fehlgeschlagen'),
  });

  return (
    <Box sx={{ p: 3 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
        <Typography variant="h4">APK Releases</Typography>
        <Button variant="contained" startIcon={<CloudUploadIcon />} onClick={() => setUploadOpen(true)}>
          APK hochladen
        </Button>
      </Stack>

      {q.isLoading && <CircularProgress />}
      {q.isError && <Alert severity="error">Fehler beim Laden</Alert>}
      {q.data && q.data.length === 0 && (
        <Alert severity="info">Noch keine Releases. Lade deine erste APK hoch!</Alert>
      )}
      {q.data && q.data.length > 0 && (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Version</TableCell>
                <TableCell>Dateiname</TableCell>
                <TableCell>Größe</TableCell>
                <TableCell>Changelog</TableCell>
                <TableCell>Hochgeladen</TableCell>
                <TableCell align="right">Aktionen</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {q.data.map((r) => (
                <TableRow key={r.id} hover>
                  <TableCell><strong>{r.version}</strong></TableCell>
                  <TableCell>{r.filename}</TableCell>
                  <TableCell>{formatSize(r.fileSize)}</TableCell>
                  <TableCell sx={{ maxWidth: 300, whiteSpace: 'pre-wrap' }}>{r.changelog ?? '—'}</TableCell>
                  <TableCell>{formatDate(r.createdAt)}</TableCell>
                  <TableCell align="right">
                    <IconButton
                      size="small"
                      color="primary"
                      onClick={() => downloadM.mutate(r.id)}
                      title="Download"
                    >
                      <DownloadIcon />
                    </IconButton>
                    <IconButton
                      size="small"
                      color="error"
                      onClick={() => setDeleteConfirm(r.id)}
                      title="Löschen"
                    >
                      <DeleteIcon />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {/* Upload Dialog */}
      <Dialog open={uploadOpen} onClose={() => setUploadOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>APK hochladen</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Version"
              value={version}
              onChange={(e) => setVersion(e.target.value)}
              placeholder="z.B. 1.0.0"
              required
            />
            <TextField
              label="Changelog"
              value={changelog}
              onChange={(e) => setChangelog(e.target.value)}
              placeholder="Was ist neu?"
              multiline
              rows={3}
            />
            <Button
              variant="outlined"
              component="label"
              startIcon={<CloudUploadIcon />}
            >
              {file ? file.name : 'APK-Datei auswählen'}
              <input
                ref={fileRef}
                type="file"
                hidden
                accept=".apk"
                onChange={(e) => setFile(e.target.files?.[0] ?? null)}
              />
            </Button>
            {file && (
              <Typography variant="caption" color="text.secondary">
                {file.name} — {formatSize(file.size)}
              </Typography>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setUploadOpen(false)}>Abbrechen</Button>
          <Button
            variant="contained"
            onClick={() => uploadM.mutate()}
            disabled={!file || !version.trim() || uploadM.isPending}
          >
            {uploadM.isPending ? <CircularProgress size={20} /> : 'Hochladen'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Delete Confirm Dialog */}
      <Dialog open={!!deleteConfirm} onClose={() => setDeleteConfirm(null)}>
        <DialogTitle>Release löschen?</DialogTitle>
        <DialogActions>
          <Button onClick={() => setDeleteConfirm(null)}>Abbrechen</Button>
          <Button
            color="error"
            variant="contained"
            onClick={() => deleteConfirm && deleteM.mutate(deleteConfirm)}
          >
            Löschen
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={!!snack}
        autoHideDuration={3000}
        onClose={() => setSnack(null)}
        message={snack}
      />
    </Box>
  );
}
