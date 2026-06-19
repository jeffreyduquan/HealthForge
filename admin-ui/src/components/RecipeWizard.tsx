import { useState, useCallback } from 'react';
import {
  Box, Button, Checkbox, Chip, Dialog, FormControlLabel, Grid,
  IconButton, Slider, TextField, Typography,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import AddIcon from '@mui/icons-material/Add';
import WizardLayout from './WizardLayout';
import { api } from '../api/client';

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

interface IngredientLine {
  name: string;
  quantity: string;
  unit: string;
}

interface IngredientSuggestion {
  name_de: string;
  energy_kcal_per_100g?: number;
}

interface StepLine {
  text: string;
}

export default function RecipeWizard({ open, onClose, onSave, saving }: Props) {
  const [step, setStep] = useState(0);

  // Step 0: Name + Foto
  const [title, setTitle] = useState('');
  const [imageKey, setImageKey] = useState('');

  // Step 1: Mahlzeit
  const [selectedSlots, setSelectedSlots] = useState<string[]>([]);

  // Step 2: Zutaten (search from API + quantity/unit)
  const [ingredientQuery, setIngredientQuery] = useState('');
  const [ingredientSuggestions, setIngredientSuggestions] = useState<IngredientSuggestion[]>([]);
  const [ingredients, setIngredients] = useState<IngredientLine[]>([]);

  const searchIngredients = useCallback(async (q: string) => {
    if (q.trim().length < 1) { setIngredientSuggestions([]); return; }
    try {
      const { data } = await api.get<IngredientSuggestion[]>(`/v1/ingredients?q=${encodeURIComponent(q)}&limit=8`);
      setIngredientSuggestions(data);
    } catch { /* ignore */ }
  }, []);

  // Step 3: Portionen + Zeit (sliders like app)
  const [servings, setServings] = useState(2);
  const [prepMinutes, setPrepMinutes] = useState(30);
  const [cookMinutes, setCookMinutes] = useState(0);

  // Step 4: Zubereitung (numbered steps)
  const [steps, setSteps] = useState<StepLine[]>([{ text: '' }]);

  // Validation
  const canStep0 = title.trim().length >= 2 && imageKey.trim().length > 0;
  const canStep1 = selectedSlots.length > 0;
  const canStep2 = ingredients.length > 0;
  const canStep3 = prepMinutes > 0;
  const canAdvance = (s: number) =>
    (s === 0 && canStep0) || (s === 1 && canStep1) ||
    (s === 2 && canStep2) || (s === 3 && canStep3) || s === 4;

  const toggleSlot = (code: string) =>
    setSelectedSlots((p) => p.includes(code) ? p.filter((x) => x !== code) : [...p, code]);

  const handleSave = () => {
    const instrText = steps.map((s, i) => `${i + 1}. ${s.text}`).join('\n');
    onSave({
      title: title.trim(),
      image_key: imageKey.trim(),
      description: instrText || null,
      slot_tags: selectedSlots,
      servings,
      prep_minutes: prepMinutes,
      cook_minutes: cookMinutes > 0 ? cookMinutes : null,
      visibility: 'PUBLIC',
      status: 'PUBLISHED',
      ingredient_lines: ingredients.filter((i) => i.name),
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
        canSave={canStep0 && canStep1 && canStep2 && canStep3} saving={saving}
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
              <TextField fullWidth label="Bild-Key *" value={imageKey}
                onChange={(e) => setImageKey(e.target.value)}
                placeholder="z.B. recipes/abc123.jpg" required />
            </Grid>
            <Grid item xs={12}>
              {imageKey.trim() ? (
                <Typography variant="body2" color="text.secondary">📷 Bild hinterlegt: {imageKey.trim()}</Typography>
              ) : (
                <Typography variant="body2" color="error">Bitte wähle ein Foto aus (Pflicht)</Typography>
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

        {/* Step 2: Zutaten — Suche aus DB */}
        {step === 2 && (
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <Typography variant="subtitle1" fontWeight={600}>Was kommt rein?</Typography>
              <Typography variant="body2" color="text.secondary">
                Such ein Lebensmittel und füge es hinzu. Dann die Menge in Gramm anpassen.
              </Typography>
            </Grid>
            <Grid item xs={12}>
              <TextField fullWidth label="Lebensmittel suchen…" value={ingredientQuery}
                onChange={(e) => { setIngredientQuery(e.target.value); searchIngredients(e.target.value); }}
                autoFocus />
            </Grid>
            {ingredientSuggestions.map((ing, i) => (
              <Grid item xs={12} key={i}>
                <Box sx={{ border: 1, borderColor: 'divider', borderRadius: 1, p: 1, cursor: 'pointer' }}
                  onClick={() => {
                    setIngredients([...ingredients, { name: ing.name_de, quantity: '', unit: 'g' }]);
                    setIngredientQuery('');
                    setIngredientSuggestions([]);
                  }}>
                  <Typography fontWeight={600}>{ing.name_de}</Typography>
                  {ing.energy_kcal_per_100g && (
                    <Typography variant="body2" color="text.secondary">
                      {Math.round(ing.energy_kcal_per_100g)} kcal / 100 g
                    </Typography>
                  )}
                </Box>
              </Grid>
            ))}
            {ingredients.length > 0 && (
              <Grid item xs={12}>
                <Typography fontWeight={600} sx={{ mt: 1 }}>Hinzugefügt ({ingredients.length})</Typography>
              </Grid>
            )}
            {ingredients.map((line, idx) => (
              <Grid item xs={12} key={idx}>
                <Box sx={{ border: 1, borderColor: 'divider', borderRadius: 1, p: 1 }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Typography fontWeight={600}>{line.name}</Typography>
                    <IconButton size="small" onClick={() => setIngredients(ingredients.filter((_, i) => i !== idx))}>
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </Box>
                  <Box sx={{ mt: 0.5 }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                      <Typography variant="body2" fontWeight={600}>Menge</Typography>
                      <Typography variant="body2" color="text.secondary">
                        {line.quantity || '100'} g
                      </Typography>
                    </Box>
                    <Slider
                      value={Number(line.quantity) || 100}
                      onChange={(_, v) => {
                        const next = [...ingredients];
                        next[idx] = { ...line, quantity: String(Math.round(v as number)), unit: 'g' };
                        setIngredients(next);
                      }}
                      min={0} max={1000} step={10}
                      size="small"
                    />
                  </Box>
                </Box>
              </Grid>
            ))}
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

        {/* Step 4: Zubereitung — nummerierte Schritte */}
        {step === 4 && (
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <Typography variant="subtitle1" fontWeight={600}>Zubereitung</Typography>
              <Typography variant="body2" color="text.secondary">
                Schritt für Schritt. Du kannst weitere Schritte hinzufügen.
              </Typography>
            </Grid>
            {steps.map((s, idx) => (
              <Grid item xs={12} key={idx}>
                <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1 }}>
                  <Box sx={{
                    width: 28, height: 28, borderRadius: '50%',
                    bgcolor: 'primary.main', color: 'white',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontWeight: 700, fontSize: 14, mt: 1, flexShrink: 0,
                  }}>
                    {idx + 1}
                  </Box>
                  <TextField fullWidth label={`Schritt ${idx + 1}`} value={s.text}
                    onChange={(e) => {
                      const next = [...steps];
                      next[idx] = { text: e.target.value };
                      setSteps(next);
                    }}
                    multiline minRows={2}
                    sx={{ flex: 1 }} />
                  {steps.length > 1 && (
                    <IconButton onClick={() => setSteps(steps.filter((_, i) => i !== idx))} sx={{ mt: 1 }}>
                      <DeleteIcon />
                    </IconButton>
                  )}
                </Box>
              </Grid>
            ))}
            <Grid item xs={12}>
              <Button startIcon={<AddIcon />} onClick={() => setSteps([...steps, { text: '' }])}
                variant="outlined" fullWidth>
                Weiterer Schritt
              </Button>
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

              {ingredients.filter(i => i.name).length > 0 && (
                <Box sx={{ mt: 2 }}>
                  <Typography variant="body2" fontWeight={600}>Zutaten:</Typography>
                  {ingredients.filter(i => i.name).map((line, i) => (
                    <Typography key={i} variant="body2" color="text.secondary">
                      • {line.name}{line.quantity ? ` — ${line.quantity} g` : ''}
                    </Typography>
                  ))}
                </Box>
              )}

              {steps.some(s => s.text.trim()) && (
                <Box sx={{ mt: 2 }}>
                  <Typography variant="body2" fontWeight={600}>Zubereitung:</Typography>
                  {steps.filter(s => s.text.trim()).map((s, i) => (
                    <Typography key={i} variant="body2" color="text.secondary">
                      {i + 1}. {s.text.trim()}
                    </Typography>
                  ))}
                </Box>
              )}
            </Box>
          </Box>
        )}
      </WizardLayout>
    </Dialog>
  );
}
