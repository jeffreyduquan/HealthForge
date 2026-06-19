import { useState } from 'react';
import {
  Box,
  Chip,
  Dialog,
  Grid,
  Slider,
  Typography,
  TextField,
} from '@mui/material';
import WizardLayout from './WizardLayout';

const STEP_LABELS = ['Name', 'Nährwerte', 'Diäten & Histamin', 'Vorschau'];

const VITAMIN_KEYS = [
  'vitamin_a', 'vitamin_d', 'vitamin_e', 'vitamin_k',
  'vitamin_b1', 'vitamin_b2', 'vitamin_b3', 'vitamin_b5',
  'vitamin_b6', 'vitamin_b7', 'vitamin_b9', 'vitamin_b12',
  'vitamin_c',
];

const MINERAL_KEYS = [
  'calcium', 'eisen', 'magnesium', 'zink', 'kupfer',
  'mangan', 'selen', 'jod', 'kalium', 'natrium', 'phosphor',
];

const ALLERGENS: { code: string; label: string }[] = [
  { code: 'GLUTEN', label: 'Gluten' }, { code: 'CRUSTACEANS', label: 'Krebstiere' },
  { code: 'EGGS', label: 'Eier' }, { code: 'FISH', label: 'Fisch' },
  { code: 'PEANUT', label: 'Erdnuss' }, { code: 'SOY', label: 'Soja' },
  { code: 'MILK', label: 'Milch' }, { code: 'NUTS', label: 'Schalenfrüchte' },
  { code: 'CELERY', label: 'Sellerie' }, { code: 'MUSTARD', label: 'Senf' },
  { code: 'SESAME', label: 'Sesam' }, { code: 'SULPHITES', label: 'Sulphite' },
  { code: 'LUPIN', label: 'Lupine' }, { code: 'MOLLUSCS', label: 'Weichtiere' },
];

const FODMAPS: { code: string; label: string }[] = [
  { code: 'FRUCTOSE', label: 'Fructose' }, { code: 'LACTOSE', label: 'Lactose' },
  { code: 'FRUCTANS', label: 'Fructane' }, { code: 'GOS', label: 'GOS' },
  { code: 'POLYOLS', label: 'Polyole' },
];

interface Props {
  open: boolean;
  onClose: () => void;
  onSave: (data: Record<string, unknown>) => void;
  saving?: boolean;
}

export default function IngredientWizard({ open, onClose, onSave, saving }: Props) {
  const [step, setStep] = useState(0);

  const [nameDe, setNameDe] = useState('');
  const [brand, setBrand] = useState('');
  const [barcode, setBarcode] = useState('');

  const [kcal, setKcal] = useState(0);
  const [protein, setProtein] = useState(0);
  const [carbs, setCarbs] = useState(0);
  const [fat, setFat] = useState(0);
  const [sugar, setSugar] = useState(0);
  const [satfat, setSatfat] = useState(0);
  const [fiber, setFiber] = useState(0);
  const [salt, setSalt] = useState(0);
  const [micros, setMicros] = useState<Record<string, number>>({});

  const [histamineScore, setHistamineScore] = useState<number | null>(null);
  const [selectedAllergens, setSelectedAllergens] = useState<string[]>([]);
  const [selectedFodmaps, setSelectedFodmaps] = useState<string[]>([]);

  const canStep0 = nameDe.trim().length >= 2;

  const handleSave = () => {
    const microObj: Record<string, number> = {};
    Object.entries(micros).forEach(([k, v]) => { if (v) microObj[k] = v; });
    onSave({
      name_de: nameDe.trim(),
      brand: brand.trim() || null,
      barcode: barcode.trim() || null,
      energy_kcal_per_100g: kcal > 0 ? kcal : null,
      protein_g_per_100g: protein > 0 ? protein : null,
      carbs_g_per_100g: carbs > 0 ? carbs : null,
      sugar_g_per_100g: sugar > 0 ? sugar : null,
      fat_g_per_100g: fat > 0 ? fat : null,
      satfat_g_per_100g: satfat > 0 ? satfat : null,
      fiber_g_per_100g: fiber > 0 ? fiber : null,
      salt_g_per_100g: salt > 0 ? salt : null,
      histamine_score: histamineScore,
      allergens_json: JSON.stringify(selectedAllergens),
      fodmap_flags_json: JSON.stringify(selectedFodmaps),
      micronutrients_json: JSON.stringify(microObj),
    });
  };

  const toggleAllergen = (c: string) =>
    setSelectedAllergens((p) => p.includes(c) ? p.filter((x) => x !== c) : [...p, c]);
  const toggleFodmap = (c: string) =>
    setSelectedFodmaps((p) => p.includes(c) ? p.filter((x) => x !== c) : [...p, c]);

  const handleNext = () => setStep((s) => Math.min(s + 1, 3));
  const handleBack = () => { if (step === 0) onClose(); else setStep((s) => s - 1); };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <WizardLayout
        title="Lebensmittel vorschlagen"
        step={step} totalSteps={4} stepLabels={STEP_LABELS}
        onBack={handleBack} onNext={handleNext} onSave={handleSave}
        canNext={(step === 0 && canStep0) || step === 1 || step === 2}
        canSave={canStep0} saving={saving}
      >
        {step === 0 && (
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <Typography variant="subtitle1" fontWeight={600}>Was möchtest du eintragen?</Typography>
              <Typography variant="body2" color="text.secondary">
                Vorschläge sind nur für dich sichtbar, bis ein Admin sie freigibt.
              </Typography>
            </Grid>
            <Grid item xs={12}>
              <TextField fullWidth label="Name (Deutsch) *" value={nameDe}
                onChange={(e) => setNameDe(e.target.value)} autoFocus />
            </Grid>
            <Grid item xs={12}>
              <TextField fullWidth label="Marke (optional)" value={brand}
                onChange={(e) => setBrand(e.target.value)} />
            </Grid>
            <Grid item xs={12}>
              <TextField fullWidth label="Barcode (optional)" value={barcode}
                onChange={(e) => setBarcode(e.target.value)} />
            </Grid>
          </Grid>
        )}

        {step === 1 && (
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <Typography variant="subtitle1" fontWeight={600}>Nährwerte pro 100 g</Typography>
              <Typography variant="body2" color="text.secondary">
                Schiebe die Regler. Genauere Werte kannst du später verbessern.
              </Typography>
            </Grid>
            {[
              ['Kalorien', `${Math.round(kcal)} kcal`, kcal, setKcal, 0, 900],
              ['Protein', `${protein.toFixed(1)} g`, protein, setProtein, 0, 100],
              ['Kohlenhydrate', `${carbs.toFixed(1)} g`, carbs, setCarbs, 0, 100],
              ['Fett', `${fat.toFixed(1)} g`, fat, setFat, 0, 100],
              ['Zucker', `${sugar.toFixed(1)} g`, sugar, setSugar, 0, 100],
              ['Ges. Fettsäuren', `${satfat.toFixed(1)} g`, satfat, setSatfat, 0, 100],
              ['Ballaststoffe', `${fiber.toFixed(1)} g`, fiber, setFiber, 0, 30],
              ['Salz', `${salt.toFixed(1)} g`, salt, setSalt, 0, 10],
            ].map(([label, display, val, setter, min, max]) => (
              <Grid item xs={12} key={label as string}>
                <SliderRow label={label as string} display={display as string}
                  value={val as number} onChange={setter as (v: number) => void}
                  min={min as number} max={max as number} />
              </Grid>
            ))}

            {/* Vitamine */}
            <Grid item xs={12} sx={{ mt: 1 }}>
              <Typography variant="subtitle2" fontWeight={600} color="primary">Vitamine</Typography>
            </Grid>
            {VITAMIN_KEYS.map((key) => {
              const val = micros[key] ?? 0;
              const meta = MICRO_META[key] ?? [0, 100, 'mg'];
              return (
                <Grid item xs={12} key={key}>
                  <MicroSliderRow
                    label={MICRO_LABEL[key] ?? key}
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
            {MINERAL_KEYS.map((key) => {
              const val = micros[key] ?? 0;
              const meta = MICRO_META[key] ?? [0, 100, 'mg'];
              return (
                <Grid item xs={12} key={key}>
                  <MicroSliderRow
                    label={MICRO_LABEL[key] ?? key}
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

        {/* STEP 2: Diäten & Histamin */}
        {step === 2 && (
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <Typography variant="subtitle1" fontWeight={600}>Diäten & Histamin</Typography>
            </Grid>
            <Grid item xs={12}>
              <Typography variant="body2" fontWeight={600} gutterBottom>Histamin-Stufe (SIGHI)</Typography>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                {[0, 1, 2, 3].map((s) => (
                  <Chip key={s} size="small"
                    label={['0 — unbedenklich', '1 — niedrig', '2 — mittel', '3 — hoch'][s]}
                    onClick={() => setHistamineScore(histamineScore === s ? null : s)}
                    color={histamineScore === s ? 'warning' : 'default'}
                    variant={histamineScore === s ? 'filled' : 'outlined'} />
                ))}
              </Box>
            </Grid>
            <Grid item xs={12}>
              <Typography variant="body2" fontWeight={600} gutterBottom>Allergene (EU-14)</Typography>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                {ALLERGENS.map(({ code, label }) => (
                  <Chip key={code} size="small" label={label}
                    onClick={() => toggleAllergen(code)}
                    color={selectedAllergens.includes(code) ? 'error' : 'default'}
                    variant={selectedAllergens.includes(code) ? 'filled' : 'outlined'} />
                ))}
              </Box>
            </Grid>
            <Grid item xs={12}>
              <Typography variant="body2" fontWeight={600} gutterBottom>FODMAP-Flags</Typography>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                {FODMAPS.map(({ code, label }) => (
                  <Chip key={code} size="small" label={label}
                    onClick={() => toggleFodmap(code)}
                    color={selectedFodmaps.includes(code) ? 'warning' : 'default'}
                    variant={selectedFodmaps.includes(code) ? 'filled' : 'outlined'} />
                ))}
              </Box>
            </Grid>
          </Grid>
        )}

        {/* STEP 3: Vorschau */}
        {step === 3 && (
          <Box>
            <Typography variant="subtitle1" fontWeight={600} gutterBottom>Vorschau</Typography>
            <Box sx={{ border: 1, borderColor: 'divider', borderRadius: 2, p: 2 }}>
              <Typography fontWeight={600}>{nameDe || '(kein Name)'}</Typography>
              {brand && <Typography variant="body2" color="text.secondary">{brand}</Typography>}
              {barcode && <Typography variant="body2" color="text.secondary">Barcode: {barcode}</Typography>}
              <Typography variant="body2" fontWeight={600} sx={{ mt: 1 }}>Pro 100 g:</Typography>
              <Typography variant="body2" color="text.secondary">
                {Math.round(kcal)} kcal — {protein.toFixed(1)} g P / {carbs.toFixed(1)} g KH / {fat.toFixed(1)} g F
              </Typography>
              {histamineScore !== null && (
                <Typography variant="body2" color="text.secondary">Histamin: {histamineScore} / 3</Typography>
              )}
              {selectedAllergens.length > 0 && (
                <Typography variant="body2" color="text.secondary">
                  Allergene: {selectedAllergens.map((c) => ALLERGENS.find((a) => a.code === c)?.label ?? c).join(', ')}
                </Typography>
              )}
              {selectedFodmaps.length > 0 && (
                <Typography variant="body2" color="text.secondary">
                  FODMAP: {selectedFodmaps.map((c) => FODMAPS.find((f) => f.code === c)?.label ?? c).join(', ')}
                </Typography>
              )}
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                Nach &quot;Speichern&quot; wird das Lebensmittel direkt in der Datenbank angelegt (Status APPROVED).
              </Typography>
            </Box>
          </Box>
        )}
      </WizardLayout>
    </Dialog>
  );
}

function SliderRow({ label, display, value, onChange, min, max }: {
  label: string; display: string; value: number;
  onChange: (v: number) => void; min: number; max: number;
}) {
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

/** P7.S5 — Slider for micronutrients replacing TextField. */
function MicroSliderRow({ label, unit, value, onChange, min, max }: {
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

/** Lookup for micro range: [min, max, unit] per NutrientCatalog key. */
const MICRO_META: Record<string, [number, number, string]> = {
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

/** Display name lookup for micro keys. */
const MICRO_LABEL: Record<string, string> = {
  vitamin_a: 'Vitamin A', vitamin_d: 'Vitamin D', vitamin_e: 'Vitamin E',
  vitamin_k: 'Vitamin K', vitamin_b1: 'Vitamin B1', vitamin_b2: 'Vitamin B2',
  vitamin_b3: 'Vitamin B3', vitamin_b5: 'Vitamin B5', vitamin_b6: 'Vitamin B6',
  vitamin_b7: 'Vitamin B7', vitamin_b9: 'Vitamin B9', vitamin_b12: 'Vitamin B12',
  vitamin_c: 'Vitamin C',
  calcium: 'Calcium', eisen: 'Eisen', magnesium: 'Magnesium', zink: 'Zink',
  kupfer: 'Kupfer', mangan: 'Mangan', selen: 'Selen', jod: 'Jod',
  kalium: 'Kalium', natrium: 'Natrium', phosphor: 'Phosphor',
};
