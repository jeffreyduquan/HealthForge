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

// P7.S5 — Vitamin/Mineral keys matching App NutrientCatalog
const VITAMIN_KEYS2 = [
  'vitamin_a', 'vitamin_d', 'vitamin_e', 'vitamin_k',
  'vitamin_b1', 'vitamin_b2', 'vitamin_b3', 'vitamin_b5',
  'vitamin_b6', 'vitamin_b7', 'vitamin_b9', 'vitamin_b12',
  'vitamin_c',
];
const MINERAL_KEYS2 = [
  'calcium', 'eisen', 'magnesium', 'zink', 'kupfer',
  'mangan', 'selen', 'jod', 'kalium', 'natrium', 'phosphor',
];

// P7.S5 — Micro metadata
const MICRO_META2: Record<string, [number, number, string]> = {
  vitamin_a: [0, 3000, 'µg'], vitamin_d: [0, 100, 'µg'], vitamin_e: [0, 50, 'mg'],
  vitamin_k: [0, 200, 'µg'], vitamin_b1: [0, 10, 'mg'], vitamin_b2: [0, 10, 'mg'],
  vitamin_b3: [0, 50, 'mg'], vitamin_b5: [0, 20, 'mg'], vitamin_b6: [0, 10, 'mg'],
  vitamin_b7: [0, 200, 'µg'], vitamin_b9: [0, 1000, 'µg'], vitamin_b12: [0, 20, 'µg'],
  vitamin_c: [0, 500, 'mg'],
  calcium: [0, 2000, 'mg'], eisen: [0, 30, 'mg'], magnesium: [0, 800, 'mg'],
  zink: [0, 30, 'mg'], kupfer: [0, 5, 'mg'], mangan: [0, 10, 'mg'],
  selen: [0, 200, 'µg'], jod: [0, 300, 'µg'], kalium: [0, 5000, 'mg'],
  natrium: [0, 3000, 'mg'], phosphor: [0, 2000, 'mg'],
};
const MICRO_LABEL2: Record<string, string> = {
  vitamin_a: 'Vitamin A', vitamin_d: 'Vitamin D', vitamin_e: 'Vitamin E',
  vitamin_k: 'Vitamin K', vitamin_b1: 'Vitamin B1', vitamin_b2: 'Vitamin B2',
  vitamin_b3: 'Vitamin B3', vitamin_b5: 'Vitamin B5', vitamin_b6: 'Vitamin B6',
  vitamin_b7: 'Vitamin B7', vitamin_b9: 'Vitamin B9', vitamin_b12: 'Vitamin B12',
  vitamin_c: 'Vitamin C',
  calcium: 'Calcium', eisen: 'Eisen', magnesium: 'Magnesium', zink: 'Zink',
  kupfer: 'Kupfer', mangan: 'Mangan', selen: 'Selen', jod: 'Jod',
  kalium: 'Kalium', natrium: 'Natrium', phosphor: 'Phosphor',
};

const STEP_LABELS = ['Name', 'Dosierung', 'Nährwerte', 'Vorschau'];

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

  // Step 2: Nährwerte (per dose) — P7.S5: numbers for sliders
  const [kcal, setKcal] = useState(0);
  const [protein, setProtein] = useState(0);
  const [carbs, setCarbs] = useState(0);
  const [fat, setFat] = useState(0);
  const [micros, setMicros] = useState<Record<string, number>>({});

  const canStep0 = nameDe.trim().length >= 2;
  const canStep1 = defaultDose.trim().length > 0 && parseFloat(defaultDose) > 0;

  const handleSave = () => {
    const data: Record<string, unknown> = {
      name_de: nameDe.trim(),
      brand: brand.trim() || null,
      unit_label: unitLabel,
      default_dose: parseFloat(defaultDose),
      kcal_per_dose: kcal > 0 ? kcal : null,
      protein_per_dose: protein > 0 ? protein : null,
      carbs_per_dose: carbs > 0 ? carbs : null,
      fat_per_dose: fat > 0 ? fat : null,
      micronutrients_json: JSON.stringify(micros),
      notes: null,
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
        title="Neues Supplement"
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

        {/* STEP 2: Nährwerte + Vitamine + Mineralstoffe */}
        {step === 2 && (
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <Typography variant="subtitle1" fontWeight={600}>
                Nährwerte (pro Dosis)
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Schiebe die Regler.
              </Typography>
            </Grid>
            <Grid item xs={12}>
              <MacroSliderRow label="Kalorien" value={kcal} onChange={setKcal}
                min={0} max={500} unit="kcal" />
            </Grid>
            {[
              ['Eiweiß', protein, setProtein, 0, 100, 'g'],
              ['Kohlenhydrate', carbs, setCarbs, 0, 100, 'g'],
              ['Fett', fat, setFat, 0, 100, 'g'],
            ].map(([label, val, setter, min, max, unit]) => (
              <Grid item xs={12} key={label as string}>
                <MacroSliderRow
                  label={label as string}
                  value={val as number}
                  onChange={setter as (v: number) => void}
                  min={min as number} max={max as number}
                  unit={unit as string}
                />
              </Grid>
            ))}

            {/* Vitamine */}
            <Grid item xs={12} sx={{ mt: 1 }}>
              <Typography variant="subtitle2" fontWeight={600} color="primary">Vitamine</Typography>
            </Grid>
            {VITAMIN_KEYS2.map((key) => {
              const val = micros[key] ?? 0;
              const meta = MICRO_META2[key] ?? [0, 100, 'mg'];
              return (
                <Grid item xs={12} key={key}>
                  <MicroSliderRow2
                    label={MICRO_LABEL2[key] ?? key}
                    unit={meta[2]}
                    value={val}
                    onChange={(v) => setMicros({ ...micros, [key]: v })}
                    min={meta[0]} max={meta[1]}
                  />
                </Grid>
              );
            })}

            {/* Mineralstoffe */}
            <Grid item xs={12} sx={{ mt: 1 }}>
              <Typography variant="subtitle2" fontWeight={600} color="primary">Mineralstoffe</Typography>
            </Grid>
            {MINERAL_KEYS2.map((key) => {
              const val = micros[key] ?? 0;
              const meta = MICRO_META2[key] ?? [0, 100, 'mg'];
              return (
                <Grid item xs={12} key={key}>
                  <MicroSliderRow2
                    label={MICRO_LABEL2[key] ?? key}
                    unit={meta[2]}
                    value={val}
                    onChange={(v) => setMicros({ ...micros, [key]: v })}
                    min={meta[0]} max={meta[1]}
                  />
                </Grid>
              );
            })}
          </Grid>
        )}

        {/* STEP 3: Vorschau */}
        {step === 3 && (
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

/** P7.S5 — Macro slider row for SupplementWizard. */
function MacroSliderRow({ label, value, onChange, min, max, unit }: {
  label: string; value: number; onChange: (v: number) => void;
  min: number; max: number; unit: string;
}) {
  const display = value > 0 ? `${value.toFixed(1)} ${unit}` : `— ${unit}`;
  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
        <Typography variant="body2" fontWeight={600}>{label}</Typography>
        <Typography variant="body2" color="text.secondary">{display}</Typography>
      </Box>
      <Slider value={value} onChange={(_, v) => onChange(v as number)}
        min={min} max={max} step={0.1} size="small" />
    </Box>
  );
}

/** P7.S5 — Micro slider row for SupplementWizard. */
function MicroSliderRow2({ label, unit, value, onChange, min, max }: {
  label: string; unit: string; value: number;
  onChange: (v: number) => void; min: number; max: number;
}) {
  const display = value > 0 ? `${value.toFixed(1)} ${unit}` : `— ${unit}`;
  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
        <Typography variant="body2" fontWeight={600}>{label}</Typography>
        <Typography variant="body2" color="text.secondary">{display}</Typography>
      </Box>
      <Slider value={value} onChange={(_, v) => onChange(v as number)}
        min={min} max={max} step={0.1} size="small" />
    </Box>
  );
}
