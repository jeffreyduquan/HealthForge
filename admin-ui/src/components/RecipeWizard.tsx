import { useState } from 'react';
import {
  Box,
  Button,
  Chip,
  Dialog,
  Grid,
  MenuItem,
  Slider,
  TextField,
  Typography,
} from '@mui/material';
import WizardLayout from './WizardLayout';

const STEP_LABELS = ['Name', 'Mahlzeit', 'Portionen', 'Zubereitung', 'Status', 'Vorschau'];

const SLOTS = [
  { code: 'BREAKFAST', label: 'Frühstück' },
  { code: 'LUNCH', label: 'Mittagessen' },
  { code: 'DINNER', label: 'Abendessen' },
  { code: 'SNACK', label: 'Snack' },
];

interface Props {
  open: boolean;
  onClose: () => void;
  onSave: (data: Record<string, unknown>) => void;
  saving?: boolean;
}

export default function RecipeWizard({ open, onClose, onSave, saving }: Props) {
  const [step, setStep] = useState(0);

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');

  const [selectedSlots, setSelectedSlots] = useState<string[]>([]);

  const [servings, setServings] = useState(2);
  const [prepMinutes, setPrepMinutes] = useState(30);
  const [cookMinutes, setCookMinutes] = useState(0);

  const [stepsText, setStepsText] = useState('');

  const [visibility, setVisibility] = useState('PUBLIC');

  const canStep0 = title.trim().length >= 2;

  const toggleSlot = (s: string) =>
    setSelectedSlots((p) => p.includes(s) ? p.filter((x) => x !== s) : [...p, s]);

  const handleSave = () => {
    onSave({
      title: title.trim(),
      description: description.trim() || null,
      slot_tags: selectedSlots,
      servings,
      prep_minutes: prepMinutes,
      cook_minutes: cookMinutes > 0 ? cookMinutes : null,
      visibility,
      status: 'PUBLISHED',
    });
  };

  const handleNext = () => setStep((s) => Math.min(s + 1, 5));
  const handleBack = () => { if (step === 0) onClose(); else setStep((s) => s - 1); };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <WizardLayout
        title="Rezept erstellen"
        step={step} totalSteps={6} stepLabels={STEP_LABELS}
        onBack={handleBack} onNext={handleNext} onSave={handleSave}
        canNext={
          (step === 0 && canStep0) ||
          (step >= 1 && step <= 4)
        }
        canSave={canStep0} saving={saving}
      >
        {/* Step 0: Name */}
        {step === 0 && (
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <Typography variant="subtitle1" fontWeight={600}>Wie heißt dein Rezept?</Typography>
            </Grid>
            <Grid item xs={12}>
              <TextField fullWidth label="Titel *" value={title}
                onChange={(e) => setTitle(e.target.value)} autoFocus />
            </Grid>
            <Grid item xs={12}>
              <TextField fullWidth label="Beschreibung (optional)" value={description}
                onChange={(e) => setDescription(e.target.value)} multiline minRows={2} />
            </Grid>
          </Grid>
        )}

        {/* Step 1: Mahlzeit */}
        {step === 1 && (
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <Typography variant="subtitle1" fontWeight={600}>Für welche Mahlzeit?</Typography>
              <Typography variant="body2" color="text.secondary">Mehrfachauswahl möglich.</Typography>
            </Grid>
            <Grid item xs={12}>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                {SLOTS.map(({ code, label }) => (
                  <Chip key={code} label={label}
                    onClick={() => toggleSlot(code)}
                    color={selectedSlots.includes(code) ? 'primary' : 'default'}
                    variant={selectedSlots.includes(code) ? 'filled' : 'outlined'} />
                ))}
              </Box>
            </Grid>
          </Grid>
        )}

        {/* Step 2: Portionen + Zeit */}
        {step === 2 && (
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <Typography variant="subtitle1" fontWeight={600}>Portionen & Zeit</Typography>
            </Grid>
            <Grid item xs={12}>
              <Typography variant="body2" gutterBottom>
                Portionen: <strong>{servings}</strong>
              </Typography>
              <Slider value={servings} onChange={(_, v) => setServings(v as number)}
                min={1} max={12} step={1} marks valueLabelDisplay="auto" />
            </Grid>
            <Grid item xs={6}>
              <TextField fullWidth label="Zubereitungszeit (min)" value={prepMinutes}
                onChange={(e) => setPrepMinutes(Number(e.target.value) || 0)}
                type="number" inputProps={{ min: 1 }} required />
            </Grid>
            <Grid item xs={6}>
              <TextField fullWidth label="Kochzeit (min)" value={cookMinutes}
                onChange={(e) => setCookMinutes(Number(e.target.value) || 0)}
                type="number" inputProps={{ min: 0 }} />
            </Grid>
          </Grid>
        )}

        {/* Step 3: Zubereitung */}
        {step === 3 && (
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <Typography variant="subtitle1" fontWeight={600}>Zubereitung</Typography>
              <Typography variant="body2" color="text.secondary">
                Schritt für Schritt beschreiben (optional).
              </Typography>
            </Grid>
            <Grid item xs={12}>
              <TextField fullWidth label="Zubereitungsschritte" value={stepsText}
                onChange={(e) => setStepsText(e.target.value)}
                multiline minRows={4} placeholder="1. Zutaten vorbereiten&#10;2. …" />
            </Grid>
          </Grid>
        )}

        {/* Step 4: Status */}
        {step === 4 && (
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <Typography variant="subtitle1" fontWeight={600}>Sichtbarkeit</Typography>
            </Grid>
            <Grid item xs={12}>
              <TextField select fullWidth label="Sichtbarkeit" value={visibility}
                onChange={(e) => setVisibility(e.target.value)}>
                <MenuItem value="PUBLIC">Öffentlich</MenuItem>
                <MenuItem value="PRIVATE">Privat</MenuItem>
              </TextField>
            </Grid>
            <Grid item xs={12}>
              <Typography variant="body2" color="text.secondary">
                Admin erstellte Rezepte erhalten Status PUBLISHED.
              </Typography>
            </Grid>
          </Grid>
        )}

        {/* Step 5: Vorschau */}
        {step === 5 && (
          <Box>
            <Typography variant="subtitle1" fontWeight={600} gutterBottom>Vorschau</Typography>
            <Box sx={{ border: 1, borderColor: 'divider', borderRadius: 2, p: 2 }}>
              <Typography fontWeight={600}>{title || '(kein Titel)'}</Typography>
              {description && <Typography variant="body2" color="text.secondary">{description}</Typography>}
              <Box sx={{ mt: 1, display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                <Chip label={`${servings} Portionen`} size="small" color="primary" variant="outlined" />
                <Chip label={`${prepMinutes} min`} size="small" variant="outlined" />
                {cookMinutes > 0 && <Chip label={`${cookMinutes} min Kochzeit`} size="small" variant="outlined" />}
                {selectedSlots.map((s) => (
                  <Chip key={s} size="small" color="secondary"
                    label={SLOTS.find((o) => o.code === s)?.label ?? s} />
                ))}
                <Chip label={visibility === 'PUBLIC' ? 'Öffentlich' : 'Privat'} size="small" />
              </Box>
              {stepsText && (
                <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                  📝 Zubereitung ist hinterlegt.
                </Typography>
              )}
            </Box>
          </Box>
        )}
      </WizardLayout>
    </Dialog>
  );
}
