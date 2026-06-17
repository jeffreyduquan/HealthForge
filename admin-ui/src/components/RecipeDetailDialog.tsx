import { useState } from 'react';
import {
  Accordion, AccordionSummary, AccordionDetails,
  Box, Button, Chip, Dialog, DialogActions, DialogContent, DialogTitle,
  Grid, MenuItem, Slider, TextField, Typography,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import type { RecipeCrud } from '../api/client';

const SLOTS = [
  { code: 'BREAKFAST', label: '🌅 Frühstück' },
  { code: 'LUNCH', label: '☀️ Mittagessen' },
  { code: 'DINNER', label: '🌙 Abendessen' },
  { code: 'SNACK', label: '🍿 Snack' },
];

interface Props {
  recipe: RecipeCrud | null;
  onClose: () => void;
  onSave: (id: string, data: Record<string, unknown>) => void;
}

export default function RecipeDetailDialog({ recipe, onClose, onSave }: Props) {
  if (!recipe) return null;

  // ── Stammdaten ──
  const [title, setTitle] = useState(recipe.title);
  const [description, setDescription] = useState(recipe.description ?? '');
  const [imageKey, setImageKey] = useState(recipe.image_key ?? '');
  const [status, setStatus] = useState(recipe.status);
  const [visibility, setVisibility] = useState(recipe.visibility);

  // ── Portionen & Zeit ──
  const [servings, setServings] = useState(recipe.servings);
  const [prepMinutes, setPrepMinutes] = useState(recipe.prep_minutes);
  const [cookMinutes, setCookMinutes] = useState(recipe.cook_minutes ?? 0);

  // ── Mahlzeit ──
  const [selectedSlots, setSelectedSlots] = useState<string[]>(recipe.slot_tags ?? []);

  const [warningAccepted, setWarningAccepted] = useState(false);

  const toggleSlot = (code: string) =>
    setSelectedSlots((p) => p.includes(code) ? p.filter((x) => x !== code) : [...p, code]);

  const handleSave = () => {
    if (!warningAccepted) return;
    onSave(recipe.id, {
      title: title.trim(),
      description: description.trim() || null,
      image_key: imageKey.trim() || null,
      status,
      visibility,
      servings,
      prep_minutes: prepMinutes,
      cook_minutes: cookMinutes > 0 ? cookMinutes : null,
      slot_tags: selectedSlots,
    });
  };

  return (
    <Dialog open={!!recipe} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Rezept bearbeiten: {recipe.title}</DialogTitle>
      <DialogContent sx={{ pt: 2 }}>
        {/* ── Stammdaten ── */}
        <Accordion defaultExpanded>
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Typography fontWeight={600}>Stammdaten</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <Grid container spacing={2}>
              <Grid item xs={12}>
                <TextField fullWidth label="Titel" value={title}
                  onChange={(e) => setTitle(e.target.value)} required />
              </Grid>
              <Grid item xs={12}>
                <TextField fullWidth label="Beschreibung" value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  multiline minRows={2} />
              </Grid>
              <Grid item xs={12}>
                <TextField fullWidth label="Bild-Key" value={imageKey}
                  onChange={(e) => setImageKey(e.target.value)}
                  placeholder="z.B. recipes/abc123.jpg" />
              </Grid>
              <Grid item xs={6}>
                <TextField select fullWidth label="Status" value={status}
                  onChange={(e) => setStatus(e.target.value)}>
                  <MenuItem value="PUBLISHED">PUBLISHED</MenuItem>
                  <MenuItem value="PENDING_REVIEW">PENDING_REVIEW</MenuItem>
                  <MenuItem value="REJECTED">REJECTED</MenuItem>
                  <MenuItem value="REMOVED">REMOVED</MenuItem>
                </TextField>
              </Grid>
              <Grid item xs={6}>
                <TextField select fullWidth label="Sichtbarkeit" value={visibility}
                  onChange={(e) => setVisibility(e.target.value)}>
                  <MenuItem value="PUBLIC">PUBLIC</MenuItem>
                  <MenuItem value="PRIVATE">PRIVATE</MenuItem>
                  <MenuItem value="GROUP">GROUP</MenuItem>
                </TextField>
              </Grid>
            </Grid>
          </AccordionDetails>
        </Accordion>

        {/* ── Portionen & Zeit ── */}
        <Accordion defaultExpanded>
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Typography fontWeight={600}>Portionen & Zeit</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <Grid container spacing={2}>
              <Grid item xs={12}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography fontWeight={600}>Portionen</Typography>
                  <Typography color="text.secondary">{servings}</Typography>
                </Box>
                <Slider value={servings} onChange={(_, v) => setServings(v as number)}
                  min={1} max={20} step={1} marks valueLabelDisplay="auto" />
              </Grid>
              <Grid item xs={12}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography fontWeight={600}>Zubereitungszeit</Typography>
                  <Typography color="text.secondary">{prepMinutes} min</Typography>
                </Box>
                <Slider value={prepMinutes} onChange={(_, v) => setPrepMinutes(Math.round((v as number) / 5) * 5)}
                  min={0} max={240} step={5} valueLabelDisplay="auto" />
              </Grid>
              <Grid item xs={12}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography fontWeight={600}>Kochzeit</Typography>
                  <Typography color="text.secondary">{cookMinutes > 0 ? `${cookMinutes} min` : '—'}</Typography>
                </Box>
                <Slider value={cookMinutes} onChange={(_, v) => setCookMinutes(Math.round((v as number) / 5) * 5)}
                  min={0} max={240} step={5} valueLabelDisplay="auto" />
              </Grid>
            </Grid>
          </AccordionDetails>
        </Accordion>

        {/* ── Mahlzeit ── */}
        <Accordion>
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Typography fontWeight={600}>Mahlzeit ({selectedSlots.length})</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
              {SLOTS.map(({ code, label }) => (
                <Chip key={code} label={label}
                  onClick={() => toggleSlot(code)}
                  color={selectedSlots.includes(code) ? 'primary' : 'default'}
                  variant={selectedSlots.includes(code) ? 'filled' : 'outlined'} />
              ))}
            </Box>
          </AccordionDetails>
        </Accordion>
      </DialogContent>
      <DialogActions>
        {!warningAccepted ? (
          <Button color="warning" variant="outlined" onClick={() => setWarningAccepted(true)}>
            ⚠️ Ich weiß, dass ich die DB direkt bearbeite
          </Button>
        ) : (
          <>
            <Button onClick={onClose}>Abbrechen</Button>
            <Button onClick={handleSave} variant="contained" color="warning"
              disabled={!title.trim()}>
              Speichern
            </Button>
          </>
        )}
      </DialogActions>
    </Dialog>
  );
}
