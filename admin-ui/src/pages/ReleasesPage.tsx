import { useState, useRef } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardActions,
  CardContent,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  IconButton,
  InputAdornment,
  Snackbar,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import DownloadIcon from '@mui/icons-material/Download';
import LinkIcon from '@mui/icons-material/Link';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import DeleteIcon from '@mui/icons-material/Delete';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createDownloadLink,
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

function copyToClipboard(text: string) {
  // Modern approach (works on HTTPS or with user gesture)
  if (navigator.clipboard && window.isSecureContext) {
    navigator.clipboard.writeText(text).catch(() => fallbackCopy(text));
  } else {
    fallbackCopy(text);
  }
}

function fallbackCopy(text: string) {
  const ta = document.createElement('textarea');
  ta.value = text;
  ta.style.position = 'fixed';
  ta.style.left = '-9999px';
  document.body.appendChild(ta);
  ta.select();
  try { document.execCommand('copy'); } catch { /* ignore */ }
  document.body.removeChild(ta);
}

export default function ReleasesPage() {
  const qc = useQueryClient();
  const fileRef = useRef<HTMLInputElement>(null);
  const [uploadOpen, setUploadOpen] = useState(false);
  const [version, setVersion] = useState('');
  const [changelog, setChangelog] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [deleteConfirm, setDeleteConfirm] = useState<string | null>(null);
  const [downloadLink, setDownloadLink] = useState<{ url: string; filename: string } | null>(null);
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
      // Presigned-URL im neuen Tab öffnen – der Browser lädt die APK direkt
      window.open(data.url, '_blank');
    },
    onError: () => setSnack('Download fehlgeschlagen'),
  });

  const downloadLinkM = useMutation({
    mutationFn: (id: string) => createDownloadLink(id),
    onSuccess: (data) => {
      setDownloadLink({ url: data.url, filename: data.filename });
    },
    onError: () => setSnack('Link-Generierung fehlgeschlagen'),
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
        <Stack spacing={2}>
          <Alert severity="info" sx={{ mb: 1 }}>
            Es ist immer nur <strong>ein</strong> Release aktiv. Beim Hochladen einer neuen APK wird die vorherige automatisch ersetzt.
          </Alert>
          {q.data.map((r) => (
            <Card key={r.id} variant="outlined" sx={{ borderRadius: 3 }}>
              <CardContent>
                <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                  <Box>
                    <Typography variant="h5" fontWeight={700} gutterBottom>
                      {r.version}
                    </Typography>
                    <Typography variant="body2" color="text.secondary" gutterBottom>
                      {r.filename} · {formatSize(r.fileSize)}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      Hochgeladen: {formatDate(r.createdAt)}
                    </Typography>
                  </Box>
                </Stack>
                {r.changelog && (
                  <Box sx={{ mt: 2, p: 1.5, bgcolor: 'rgba(255,255,255,0.04)', borderRadius: 2, whiteSpace: 'pre-wrap' }}>
                    <Typography variant="body2" fontWeight={600} gutterBottom>
                      Changelog
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      {r.changelog}
                    </Typography>
                  </Box>
                )}
              </CardContent>
              <CardActions sx={{ px: 2, pb: 2, gap: 1 }}>
                <Button
                  size="small"
                  variant="contained"
                  startIcon={<DownloadIcon />}
                  onClick={() => downloadM.mutate(r.id)}
                  disabled={downloadM.isPending}
                >
                  APK herunterladen
                </Button>
                <Button
                  size="small"
                  variant="outlined"
                  startIcon={<LinkIcon />}
                  onClick={() => downloadLinkM.mutate(r.id)}
                  disabled={downloadLinkM.isPending}
                >
                  Einmal-Link
                </Button>
                <Button
                  size="small"
                  variant="outlined"
                  color="error"
                  startIcon={<DeleteIcon />}
                  onClick={() => setDeleteConfirm(r.id)}
                >
                  Löschen
                </Button>
              </CardActions>
            </Card>
          ))}
        </Stack>
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

      {/* Download Link Dialog */}
      <Dialog open={!!downloadLink} onClose={() => setDownloadLink(null)} maxWidth="md" fullWidth>
        <DialogTitle>🔗 Einmaliger Download-Link</DialogTitle>
        <DialogContent>
          <DialogContentText sx={{ mb: 2 }}>
            Der Link ist <strong>7 Tage gültig</strong> und kann nur <strong>einmal</strong> verwendet werden.
            Klicke auf das Kopier-Icon, um ihn in die Zwischenablage zu kopieren.
          </DialogContentText>
          {downloadLink && (
            <TextField
              fullWidth
              value={downloadLink.url}
              InputProps={{
                readOnly: true,
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton onClick={() => { copyToClipboard(downloadLink.url); setSnack('🔗 Link kopiert'); }}>
                      <ContentCopyIcon />
                    </IconButton>
                  </InputAdornment>
                ),
              }}
            />
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDownloadLink(null)}>Schließen</Button>
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
