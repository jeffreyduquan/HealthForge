import { useState } from 'react';
import {
  Box, Checkbox, Chip, Dialog, FormControlLabel, Grid,
  Slider, TextField, Typography,
} from '@mui/material';
import WizardLayout from './WizardLayout';

const STEP_LABELS = ['Name & Foto', 'Mahlzeit', 'Zutaten', 'Portionen & Zeit', 'Zubereitung', 'Vorschau'];

const SLOTS = [
  { code: 'BREAKFAST', label: '🌅 Frühstück' },
  { code: 'LUNCH', label: '☀️ Mittagessen' },
  { code: 'DINNER', label: '🌙 Abendessen' },
  { code: 'SNACK', label: '🍿 Snack' },
];

interface Props {
  open: boolean;
  onClose: () => void;
  onSave: (data: Record<string, unknown>) => void;
  saving?: boolean;
}

export default function RecipeWizard({ open, onClose, onSave, saving }: Props) {
  const [step, setStep] = useState(0);

  // Step 0: Name + Foto
  const [title, setTitle] = useState('');
  const [imageKey, setImageKey] = useState('');

  // Step 1: Mahlzeit
  const [selectedSlots, setSelectedSlots] = useState<string[]>([]);

  // Step 2: Zutaten (freitext, kein Server-Search im Admin)
  const [ingredientsText, setIngredientsText] = useState('');

  // Step 3: Portionen + Zeit (sliders like app)
  const [servings, setServings] = useState(2);
  const [prepMinutes, setPrepMinutes] = useState(30);
  const [cookMinutes, setCookMinutes] = useState(0);

  // Step 4: Zubereitung
  const [instructionsText, setInstructionsText] = useState('');

  // Validation
  const canStep0 = title.trim().length >= 2;
  const canStep1 = selectedSlots.length > 0;
  const canStep2 = true; // ingredients are optional for admin
  const canStep3 = prepMinutes > 0;
  const canAdvance = (s: number) =>
    (s === 0 && canStep0) || (s === 1 && canStep1) ||
    (s === 2 && canStep2) || (s === 3 && canStep3) || s === 4;

  const toggleSlot = (code: string) =>
    setSelectedSlots((p) => p.includes(code) ? p.filter((x) => x !== code) : [...p, code]);

  const handleSave = () => {
    onSave({
      title: title.trim(),
      image_key: imageKey.trim() || null,
      description: instructionsText.trim() || null,
      slot_tags: selectedSlots,
      servings,
      prep_minutes: prepMinutes,
      cook_minutes: cookMinutes > 0 ? cookMinutes : null,
      visibility: 'PUBLIC',
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
        canNext={canAdvance(step)}
        canSave={canStep0 && canStep1 && canStep3} saving={saving}
      >
        {/* Step 0: Name + Foto */}
        {step === 0 && (
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <Typography variant="subtitle1" fontWeight={600}>Wie heißt dein Rezept?</Typography>
            </Grid>
            <Grid item xs={12}>
              <TextField fullWidth label="Name *" value={title}
                onChange={(e) => setTitle(e.target.value)} autoFocus />
            </Grid>
            <Grid item xs={12}>
              <TextField fullWidth label="Bild-Key (optional)" value={imageKey}
                onChange={(e) => setImageKey(e.target.value)}
                placeholder="z.B. recipes/abc123.jpg" />
            </Grid>
            <Grid item xs={12}>
              {imageKey.trim() ? (
                <Typography variant="body2" color="text.secondary">📷 Bild hinterlegt: {imageKey.trim()}</Typography>
              ) : (
                <Typography variant="body2" color="error">Bitte wähle ein Foto aus (Pflicht in der App)</Typography>
              )}
            </Grid>
          </Grid>
        )}

        {/* Step 1: Mahlzeit */}
        {step === 1 && (
          <Grid container spacing={1}>
            <Grid item xs={12}>
              <Typography variant="subtitle1" fontWeight={600}>Für welche Mahlzeit?</Typography>
              <Typography variant="body2" color="text.secondary">Wähle mindestens eine Mahlzeit aus.</Typography>
            </Grid>
            {SLOTS.map(({ code, label }) => (
              <Grid item xs={12} key={code}>
                <Box sx={{ border: 1, borderColor: 'divider', borderRadius: 1, p: 1 }}>
                  <FormControlLabel
                    control={<Checkbox checked={selectedSlots.includes(code)}
                      onChange={() => toggleSlot(code)} />}
                    label={<Typography fontWeight={600}>{label}</Typography>}
                  />
                </Box>
              </Grid>
            ))}
            {selectedSlots.length === 0 && (
              <Grid item xs={12}>
                <Typography variant="body2" color="error">Bitte wähle mindestens eine Mahlzeit</Typography>
              </Grid>
            )}
          </Grid>
        )}

        {/* Step 2: Zutaten */}
        {step === 2 && (
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <Typography variant="subtitle1" fontWeight={600}>Was kommt rein?</Typography>
              <Typography variant="body2" color="text.secondary">
                Gib die Zutaten als Freitext ein (Admin-Ansicht).
              </Typography>
            </Grid>
            <Grid item xs={12}>
              <TextField fullWidth label="Zutatenliste" value={ingredientsText}
                onChange={(e) => setIngredientsText(e.target.value)}
                multiline minRows={4}
                placeholder="200g Mehl&#10;2 Eier&#10;100ml Milch&#10;…" />
            </Grid>
          </Grid>
        )}

        {/* Step 3: Portionen + Zeit */}
        {step === 3 && (
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <Typography variant="subtitle1" fontWeight={600}>Portionen & Zeit</Typography>
            </Grid>
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
                <Typography fontWeight={600}>Kochzeit (optional)</Typography>
                <Typography color="text.secondary">{cookMinutes > 0 ? `${cookMinutes} min` : '—'}</Typography>
              </Box>
              <Slider value={cookMinutes} onChange={(_, v) => setCookMinutes(Math.round((v as number) / 5) * 5)}
                min={0} max={240} step={5} valueLabelDisplay="auto" />
            </Grid>
          </Grid>
        )}

        {/* Step 4: Zubereitung */}
        {step === 4 && (
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <Typography variant="subtitle1" fontWeight={600}>Zubereitung</Typography>
              <Typography variant="body2" color="text.secondary">
                Schritt für Schritt empfohlen. Du kannst weitere Schritte hinzufügen.
              </Typography>
            </Grid>
            <Grid item xs={12}>
              <TextField fullWidth label="Zubereitungsschritte" value={instructionsText}
                onChange={(e) => setInstructionsText(e.target.value)}
                multiline minRows={5}
                placeholder="1. Zutaten vorbereiten&#10;2. Alles vermengen&#10;3. Bei 180°C backen&#10;…" />
            </Grid>
          </Grid>
        )}

        {/* Step 5: Vorschau */}
        {step === 5 && (
          <Box>
            <Typography variant="subtitle1" fontWeight={600} gutterBottom>Vorschau</Typography>
            <Box sx={{ border: 1, borderColor: 'divider', borderRadius: 2, p: 2 }}>
              <Typography variant="h6" fontWeight={600}>{title || '(kein Titel)'}</Typography>

              {imageKey.trim() && (
                <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                  📷 Bild: {imageKey.trim()}
                </Typography>
              )}

              <Box sx={{ mt: 1, display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                <Chip label={`${servings} Portionen`} size="small" color="primary" variant="outlined" />
                <Chip label={`⏱ ${prepMinutes} min`} size="small" variant="outlined" />
                {cookMinutes > 0 && <Chip label={`🔥 ${cookMinutes} min`} size="small" variant="outlined" />}
                {selectedSlots.map((s) => (
                  <Chip key={s} size="small" color="secondary"
                    label={SLOTS.find((o) => o.code === s)?.label ?? s} />
                ))}
              </Box>

              {ingredientsText.trim() && (
                <Box sx={{ mt: 2 }}>
                  <Typography variant="body2" fontWeight={600}>Zutaten:</Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ whiteSpace: 'pre-wrap' }}>
                    {ingredientsText.trim()}
                  </Typography>
                </Box>
              )}

              {instructionsText.trim() && (
                <Box sx={{ mt: 2 }}>
                  <Typography variant="body2" fontWeight={600}>Zubereitung:</Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ whiteSpace: 'pre-wrap' }}>
                    {instructionsText.trim()}
                  </Typography>
                </Box>
              )}
            </Box>
          </Box>
        )}
      </WizardLayout>
    </Dialog>
  );
}
