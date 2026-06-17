import { useState } from 'react';
import {
  Box,
  Chip,
  Dialog,
  Grid,
  MenuItem,
  Slider,
  TextField,
  Typography,
} from '@mui/material';
import WizardLayout from './WizardLayout';

const STEP_LABELS = ['Name', 'Details', 'Status', 'Vorschau'];

const SLOT_OPTIONS = [
  { value: 'BREAKFAST', label: 'Frühstück' },
  { value: 'LUNCH', label: 'Mittagessen' },
  { value: 'DINNER', label: 'Abendessen' },
  { value: 'SNACK', label: 'Snack' },
];

interface Props {
  open: boolean;
  onClose: () => void;
  onSave: (data: Record<string, unknown>) => void;
  saving?: boolean;
}

export default function RecipeWizard({ open, onClose, onSave, saving }: Props) {
  const [step, setStep] = useState(0);

  // Step 0: Name
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');

  // Step 1: Details
  const [servings, setServings] = useState(2);
  const [prepMinutes, setPrepMinutes] = useState('30');
  const [cookMinutes, setCookMinutes] = useState('');
  const [selectedSlots, setSelectedSlots] = useState<string[]>([]);

  // Step 2: Status + Visibility
  const [visibility, setVisibility] = useState('PUBLIC');

  const canStep0 = title.trim().length >= 2;
  const canStep1 = prepMinutes.trim().length > 0 && parseInt(prepMinutes) > 0;

  const toggleSlot = (slot: string) => {
    setSelectedSlots((prev) =>
      prev.includes(slot) ? prev.filter((s) => s !== slot) : [...prev, slot]
    );
  };

  const handleSave = () => {
    const data: Record<string, unknown> = {
      title: title.trim(),
      description: description.trim() || null,
      servings,
      prep_minutes: parseInt(prepMinutes),
      cook_minutes: cookMinutes ? parseInt(cookMinutes) : null,
      slot_tags: selectedSlots,
      visibility,
      status: 'APPROVED',
    };
    onSave(data);
  };

  const handleNext = () => setStep((s) => Math.min(s + 1, 3));
  const handleBack = () => {
    if (step === 0) onClose();
    else setStep((s) => s - 1);
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <WizardLayout
        title="Neues Rezept"
        step={step}
        totalSteps={4}
        stepLabels={STEP_LABELS}
        onBack={handleBack}
        onNext={handleNext}
        onSave={handleSave}
        canNext={
          (step === 0 && canStep0) ||
          (step === 1 && canStep1) ||
          step === 2
        }
        canSave={canStep0 && canStep1}
        saving={saving}
      >
        {/* STEP 0: Name */}
        {step === 0 && (
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Titel"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="z.B. Spaghetti Bolognese…"
                required
                autoFocus
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Beschreibung"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Optional"
                multiline
                minRows={2}
              />
            </Grid>
          </Grid>
        )}

        {/* STEP 1: Portionen + Zeit + Mahlzeit */}
        {step === 1 && (
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <Typography gutterBottom>
                Portionen: <strong>{servings}</strong>
              </Typography>
              <Slider
                value={servings}
                onChange={(_, v) => setServings(v as number)}
                min={1}
                max={12}
                step={1}
                marks
                valueLabelDisplay="auto"
              />
            </Grid>
            <Grid item xs={6}>
              <TextField
                fullWidth
                label="Zubereitungszeit (min)"
                value={prepMinutes}
                onChange={(e) => setPrepMinutes(e.target.value)}
                type="number"
                inputProps={{ min: 1 }}
                required
              />
            </Grid>
            <Grid item xs={6}>
              <TextField
                fullWidth
                label="Kochzeit (min)"
                value={cookMinutes}
                onChange={(e) => setCookMinutes(e.target.value)}
                type="number"
                inputProps={{ min: 0 }}
              />
            </Grid>
            <Grid item xs={12}>
              <Typography variant="subtitle2" gutterBottom>Mahlzeit</Typography>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                {SLOT_OPTIONS.map(({ value, label }) => (
                  <Chip
                    key={value}
                    label={label}
                    onClick={() => toggleSlot(value)}
                    color={selectedSlots.includes(value) ? 'primary' : 'default'}
                    variant={selectedSlots.includes(value) ? 'filled' : 'outlined'}
                  />
                ))}
              </Box>
            </Grid>
          </Grid>
        )}

        {/* STEP 2: Status + Visibility */}
        {step === 2 && (
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <TextField
                select
                fullWidth
                label="Sichtbarkeit"
                value={visibility}
                onChange={(e) => setVisibility(e.target.value)}
              >
                <MenuItem value="PUBLIC">Öffentlich</MenuItem>
                <MenuItem value="PRIVATE">Privat</MenuItem>
              </TextField>
            </Grid>
            <Grid item xs={12}>
              <Typography variant="body2" color="text.secondary">
                Status wird automatisch auf APPROVED gesetzt (Admin).
              </Typography>
            </Grid>
          </Grid>
        )}

        {/* STEP 3: Vorschau */}
        {step === 3 && (
          <Box>
            <Typography variant="h6" gutterBottom>{title || '(kein Titel)'}</Typography>
            {description && <Typography color="text.secondary" sx={{ mb: 2 }}>{description}</Typography>}

            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
              <Chip label={`${servings} Portionen`} size="small" color="primary" variant="outlined" />
              <Chip label={`${prepMinutes} min`} size="small" variant="outlined" />
              {cookMinutes && <Chip label={`${cookMinutes} min Kochzeit`} size="small" variant="outlined" />}
              {selectedSlots.map((s) => (
                <Chip
                  key={s}
                  label={SLOT_OPTIONS.find((o) => o.value === s)?.label ?? s}
                  size="small"
                  color="secondary"
                />
              ))}
              <Chip label={visibility === 'PUBLIC' ? 'Öffentlich' : 'Privat'} size="small" />
            </Box>
          </Box>
        )}
      </WizardLayout>
    </Dialog>
  );
}
