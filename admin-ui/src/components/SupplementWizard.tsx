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
import { ALL_MICRONUTRIENT_KEYS } from '../api/nutrientDefaults';

const STEP_LABELS = ['Name', 'Dosierung', 'Nährwerte', 'Mikronährstoffe', 'Vorschau'];

const UNIT_OPTIONS = ['Tablette', 'Kapsel', 'ml', 'g', 'Portion'];

interface Props {
  open: boolean;
  onClose: () => void;
  onSave: (data: Record<string, unknown>) => void;
  saving?: boolean;
}

export default function SupplementWizard({ open, onClose, onSave, saving }: Props) {
  const [step, setStep] = useState(0);

  // Step 0: Name
  const [nameDe, setNameDe] = useState('');
  const [brand, setBrand] = useState('');

  // Step 1: Dosierung
  const [unitLabel, setUnitLabel] = useState('Tablette');
  const [defaultDose, setDefaultDose] = useState('1');

  // Step 2: Nährwerte (per dose)
  const [kcal, setKcal] = useState('');
  const [protein, setProtein] = useState('');
  const [carbs, setCarbs] = useState('');
  const [fat, setFat] = useState('');
  const [micros, setMicros] = useState<Record<string, number>>({});

  const canStep0 = nameDe.trim().length >= 2;
  const canStep1 = defaultDose.trim().length > 0 && parseFloat(defaultDose) > 0;

  const handleSave = () => {
    const data: Record<string, unknown> = {
      name_de: nameDe.trim(),
      brand: brand.trim() || null,
      unit_label: unitLabel,
      default_dose: parseFloat(defaultDose),
      kcal_per_dose: kcal ? parseFloat(kcal) : null,
      protein_per_dose: protein ? parseFloat(protein) : null,
      carbs_per_dose: carbs ? parseFloat(carbs) : null,
      fat_per_dose: fat ? parseFloat(fat) : null,
      micronutrients_json: JSON.stringify(micros),
      notes: null,
    };
    onSave(data);
  };

  const handleNext = () => setStep((s) => Math.min(s + 1, 4));
  const handleBack = () => {
    if (step === 0) onClose();
    else setStep((s) => s - 1);
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <WizardLayout
        title="Neues Supplement"
        step={step}
        totalSteps={5}
        stepLabels={STEP_LABELS}
        onBack={handleBack}
        onNext={handleNext}
        onSave={handleSave}
        canNext={
          (step === 0 && canStep0) ||
          (step === 1 && canStep1) ||
          step === 2 || step === 3
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
                label="Name"
                value={nameDe}
                onChange={(e) => setNameDe(e.target.value)}
                placeholder="z.B. Vitamin D3, Magnesium…"
                required
                autoFocus
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Marke"
                value={brand}
                onChange={(e) => setBrand(e.target.value)}
                placeholder="Optional"
              />
            </Grid>
          </Grid>
        )}

        {/* STEP 1: Dosierung */}
        {step === 1 && (
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                Dosierung
              </Typography>
            </Grid>
            <Grid item xs={6}>
              <TextField
                select
                fullWidth
                label="Einheit"
                value={unitLabel}
                onChange={(e) => setUnitLabel(e.target.value)}
              >
                {UNIT_OPTIONS.map((u) => (
                  <MenuItem key={u} value={u}>{u}</MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={6}>
              <TextField
                fullWidth
                label="Standard-Dosis"
                value={defaultDose}
                onChange={(e) => setDefaultDose(e.target.value)}
                type="number"
                inputProps={{ step: 0.5, min: 0 }}
                required
              />
            </Grid>
          </Grid>
        )}

        {/* STEP 2: Nährwerte */}
        {step === 2 && (
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <Typography variant="subtitle1" fontWeight={600}>
                Nährwerte (pro Dosis)
              </Typography>
            </Grid>
            {[
              ['kcal', 'Kalorien (kcal)', kcal, setKcal],
              ['protein', 'Eiweiß (g)', protein, setProtein],
              ['carbs', 'Kohlenhydrate (g)', carbs, setCarbs],
              ['fat', 'Fett (g)', fat, setFat],
            ].map(([, label, val, setter]) => (
              <Grid item xs={6} key={label as string}>
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

        {/* STEP 3: Mikronährstoffe */}
        {step === 3 && (
          <Grid container spacing={1}>
            {ALL_MICRONUTRIENT_KEYS.map((key) => {
              const val = micros[key] ?? 0;
              return (
                <Grid item xs={6} sm={4} key={key}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                    <Typography variant="caption" sx={{ width: 90, textAlign: 'right' }}>{key}:</Typography>
                    <TextField size="small" type="number" value={val || ''}
                      onChange={(e) => setMicros({ ...micros, [key]: Number(e.target.value) || 0 })}
                      inputProps={{ step: 0.1, min: 0 }} sx={{ flex: 1 }} />
                  </Box>
                </Grid>
              );
            })}
          </Grid>
        )}

        {/* STEP 4: Vorschau */}
        {step === 4 && (
          <Box>
            <Typography variant="h6" gutterBottom>{nameDe || '(kein Name)'}</Typography>
            {brand && <Typography color="text.secondary">{brand}</Typography>}
            <Box sx={{ mt: 2, display: 'flex', flexWrap: 'wrap', gap: 1 }}>
              <Chip label={`${defaultDose} ${unitLabel}`} size="small" color="primary" variant="outlined" />
              {kcal && <Chip label={`${kcal} kcal`} size="small" variant="outlined" />}
              {protein && <Chip label={`${protein}g Eiweiß`} size="small" variant="outlined" />}
              {carbs && <Chip label={`${carbs}g KH`} size="small" variant="outlined" />}
              {fat && <Chip label={`${fat}g Fett`} size="small" variant="outlined" />}
            </Box>
          </Box>
        )}
      </WizardLayout>
    </Dialog>
  );
}
