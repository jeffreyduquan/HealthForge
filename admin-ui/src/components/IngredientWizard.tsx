import { useState } from 'react';
import {
  Box,
  Chip,
  Dialog,
  Grid,
  MenuItem,
  TextField,
  Typography,
} from '@mui/material';
import WizardLayout from './WizardLayout';

const STEP_LABELS = ['Name', 'Nährwerte', 'Mikronährstoffe', 'Vorschau'];

const ALLERGENS = [
  'GLUTEN', 'CRUSTACEANS', 'EGGS', 'FISH', 'PEANUT', 'SOY',
  'MILK', 'NUTS', 'CELERY', 'MUSTARD', 'SESAME', 'SULPHITES',
  'LUPIN', 'MOLLUSCS',
];

const FODMAPS = ['FRUCTOSE', 'LACTOSE', 'FRUCTANS', 'GOS', 'POLYOLS'];

interface Props {
  open: boolean;
  onClose: () => void;
  onSave: (data: Record<string, unknown>) => void;
  saving?: boolean;
}

export default function IngredientWizard({ open, onClose, onSave, saving }: Props) {
  const [step, setStep] = useState(0);

  // Step 0: Name
  const [nameDe, setNameDe] = useState('');
  const [brand, setBrand] = useState('');
  const [barcode, setBarcode] = useState('');

  // Step 1: Macros
  const [kcal, setKcal] = useState('');
  const [protein, setProtein] = useState('');
  const [carbs, setCarbs] = useState('');
  const [sugar, setSugar] = useState('');
  const [fat, setFat] = useState('');
  const [satfat, setSatfat] = useState('');
  const [fiber, setFiber] = useState('');
  const [salt, setSalt] = useState('');

  // Step 2: Micronutrients + allergens
  const [histamineScore, setHistamineScore] = useState('');
  const [selectedAllergens, setSelectedAllergens] = useState<string[]>([]);
  const [selectedFodmaps, setSelectedFodmaps] = useState<string[]>([]);

  const canStep0 = nameDe.trim().length >= 2;

  // Toggle allergen
  const toggleAllergen = (a: string) => {
    setSelectedAllergens((prev) =>
      prev.includes(a) ? prev.filter((x) => x !== a) : [...prev, a]
    );
  };

  const toggleFodmap = (f: string) => {
    setSelectedFodmaps((prev) =>
      prev.includes(f) ? prev.filter((x) => x !== f) : [...prev, f]
    );
  };

  const handleSave = () => {
    const data: Record<string, unknown> = {
      name_de: nameDe.trim(),
      brand: brand.trim() || null,
      barcode: barcode.trim() || null,
      energy_kcal_per_100g: kcal ? parseFloat(kcal) : null,
      protein_g_per_100g: protein ? parseFloat(protein) : null,
      carbs_g_per_100g: carbs ? parseFloat(carbs) : null,
      sugar_g_per_100g: sugar ? parseFloat(sugar) : null,
      fat_g_per_100g: fat ? parseFloat(fat) : null,
      satfat_g_per_100g: satfat ? parseFloat(satfat) : null,
      fiber_g_per_100g: fiber ? parseFloat(fiber) : null,
      salt_g_per_100g: salt ? parseFloat(salt) : null,
      histamine_score: histamineScore ? parseInt(histamineScore) : null,
      allergens_json: JSON.stringify(selectedAllergens),
      fodmap_flags_json: JSON.stringify(selectedFodmaps),
      micronutrients_json: '{}',
      source: 'MANUAL',
    };
    onSave(data);
  };

  const handleNext = () => setStep((s) => Math.min(s + 1, 3));
  const handleBack = () => {
    if (step === 0) onClose();
    else setStep((s) => s - 1);
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <WizardLayout
        title="Neue Zutat"
        step={step}
        totalSteps={4}
        stepLabels={STEP_LABELS}
        onBack={handleBack}
        onNext={handleNext}
        onSave={handleSave}
        canNext={
          (step === 0 && canStep0) ||
          step === 1 ||
          step === 2
        }
        canSave={canStep0}
        saving={saving}
      >
        {/* STEP 0: Name + Brand + Barcode */}
        {step === 0 && (
              <Grid container spacing={2}>
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    label="Name (Deutsch)"
                    value={nameDe}
                    onChange={(e) => setNameDe(e.target.value)}
                    placeholder="z.B. Apfel, Brot, Tomate…"
                    required
                    autoFocus
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    label="Marke"
                    value={brand}
                    onChange={(e) => setBrand(e.target.value)}
                    placeholder="Optional"
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    label="Barcode"
                    value={barcode}
                    onChange={(e) => setBarcode(e.target.value)}
                    placeholder="Optional"
                  />
                </Grid>
              </Grid>
            )}

            {/* STEP 1: Nährwerte */}
            {step === 1 && (
              <Grid container spacing={2}>
                <Grid item xs={12}>
                  <Typography variant="subtitle1" fontWeight={600}>
                    Nährwerte (pro 100 g)
                  </Typography>
                </Grid>
                {[
                  ['kcal', 'Kalorien (kcal)', kcal, setKcal],
                  ['protein', 'Eiweiß (g)', protein, setProtein],
                  ['carbs', 'Kohlenhydrate (g)', carbs, setCarbs],
                  ['sugar', 'Zucker (g)', sugar, setSugar],
                  ['fat', 'Fett (g)', fat, setFat],
                  ['satfat', 'Gesättigte Fette (g)', satfat, setSatfat],
                  ['fiber', 'Ballaststoffe (g)', fiber, setFiber],
                  ['salt', 'Salz (g)', salt, setSalt],
                ].map(([, label, val, setter]) => (
                  <Grid item xs={6} sm={4} md={3} key={label as string}>
                    <TextField
                      fullWidth
                      size="small"
                      label={label as string}
                      value={val as string}
                      onChange={(e) => (setter as (v: string) => void)(e.target.value)}
                      type="number"
                      inputProps={{ step: 0.1, min: 0 }}
                    />
                  </Grid>
                ))}
              </Grid>
            )}

            {/* STEP 2: Mikronährstoffe + Allergene */}
            {step === 2 && (
              <Grid container spacing={3}>
                <Grid item xs={12}>
                  <TextField
                    select
                    fullWidth
                    size="small"
                    label="Histamin-Score (SIGHI)"
                    value={histamineScore}
                    onChange={(e) => setHistamineScore(e.target.value)}
                  >
                    <MenuItem value="">Keine Angabe</MenuItem>
                    {[0, 1, 2, 3].map((s) => (
                      <MenuItem key={s} value={s}>{s}</MenuItem>
                    ))}
                  </TextField>
                </Grid>
                <Grid item xs={12}>
                  <Typography variant="subtitle2" gutterBottom>Allergene (EU-14)</Typography>
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                    {ALLERGENS.map((a) => (
                      <Chip
                        key={a}
                        label={a}
                        onClick={() => toggleAllergen(a)}
                        color={selectedAllergens.includes(a) ? 'error' : 'default'}
                        variant={selectedAllergens.includes(a) ? 'filled' : 'outlined'}
                        size="small"
                      />
                    ))}
                  </Box>
                </Grid>
                <Grid item xs={12}>
                  <Typography variant="subtitle2" gutterBottom>FODMAP</Typography>
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                    {FODMAPS.map((f) => (
                      <Chip
                        key={f}
                        label={f}
                        onClick={() => toggleFodmap(f)}
                        color={selectedFodmaps.includes(f) ? 'warning' : 'default'}
                        variant={selectedFodmaps.includes(f) ? 'filled' : 'outlined'}
                        size="small"
                      />
                    ))}
                  </Box>
                </Grid>
              </Grid>
            )}

            {/* STEP 3: Vorschau */}
            {step === 3 && (
              <Box>
                <Typography variant="h6" gutterBottom>{nameDe || '(kein Name)'}</Typography>
                {brand && <Typography color="text.secondary">{brand}</Typography>}
                {barcode && <Typography variant="caption">Barcode: {barcode}</Typography>}

                <Box sx={{ mt: 2, display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                  {kcal && <Chip label={`${kcal} kcal`} size="small" color="primary" variant="outlined" />}
                  {protein && <Chip label={`${protein}g Eiweiß`} size="small" variant="outlined" />}
                  {carbs && <Chip label={`${carbs}g KH`} size="small" variant="outlined" />}
                  {fat && <Chip label={`${fat}g Fett`} size="small" variant="outlined" />}
                  {histamineScore && <Chip label={`SIGHI ${histamineScore}`} size="small" color="warning" />}
                </Box>

                {selectedAllergens.length > 0 && (
                  <Box sx={{ mt: 1 }}>
                    <Typography variant="caption">Allergene:</Typography>
                    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mt: 0.5 }}>
                      {selectedAllergens.map((a) => (
                        <Chip key={a} label={a} size="small" color="error" />
                      ))}
                    </Box>
                  </Box>
                )}

                <Typography variant="body2" sx={{ mt: 2 }} color="text.secondary">
                  Quelle: MANUAL — wird direkt in die Datenbank eingetragen.
                </Typography>
              </Box>
            )}
      </WizardLayout>
    </Dialog>
  );
}
