import { useState } from 'react';
import {
  Accordion, AccordionSummary, AccordionDetails,
  Box, Button, Chip, Dialog, DialogActions, DialogContent, DialogTitle,
  Grid, MenuItem, Slider, TextField, Typography,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import type { IngredientCrud } from '../api/client';
import { buildFullMicronutrients } from '../api/nutrientDefaults';

const ALLERGENS = [
  'GLUTEN', 'CRUSTACEANS', 'EGGS', 'FISH', 'PEANUT', 'SOY',
  'MILK', 'NUTS', 'CELERY', 'MUSTARD', 'SESAME', 'SULPHITES',
  'LUPIN', 'MOLLUSCS',
];

const FODMAPS = ['FRUCTOSE', 'LACTOSE', 'FRUCTANS', 'GOS', 'POLYOLS'];

const HISTAMINE_OPTIONS = [
  { value: '', label: 'Keine Angabe' },
  { value: '0', label: '0 — unbedenklich' },
  { value: '1', label: '1 — niedrig' },
  { value: '2', label: '2 — mittel' },
  { value: '3', label: '3 — hoch' },
];

interface Props {
  ingredient: IngredientCrud | null;
  onClose: () => void;
  onSave: (id: string, data: Record<string, unknown>) => void;
}

export default function IngredientDetailDialog({ ingredient, onClose, onSave }: Props) {
  if (!ingredient) return null;

  // ── Stammdaten ──
  const [nameDe, setNameDe] = useState(ingredient.name_de);
  const [brand, setBrand] = useState(ingredient.brand ?? '');
  const [barcode, setBarcode] = useState(ingredient.barcode ?? '');
  const [status, setStatus] = useState(ingredient.status);
  const [locked, setLocked] = useState(ingredient.locked);

  // ── Makros ──
  const [kcal, setKcal] = useState(ingredient.energy_kcal_per_100g ?? 0);
  const [protein, setProtein] = useState(ingredient.protein_g_per_100g ?? 0);
  const [carbs, setCarbs] = useState(ingredient.carbs_g_per_100g ?? 0);
  const [sugar, setSugar] = useState(ingredient.sugar_g_per_100g ?? 0);
  const [fat, setFat] = useState(ingredient.fat_g_per_100g ?? 0);
  const [satfat, setSatfat] = useState(ingredient.satfat_g_per_100g ?? 0);
  const [fiber, setFiber] = useState(ingredient.fiber_g_per_100g ?? 0);
  const [salt, setSalt] = useState(ingredient.salt_g_per_100g ?? 0);

  // ── Mikronährstoffe ──
  const [micros, setMicros] = useState<Record<string, number>>(() => {
    const full = buildFullMicronutrients(ingredient.micronutrients_json);
    return full;
  });

  // ── Diäten ──
  const [histScore, setHistScore] = useState(ingredient.histamine_score ?? '');
  const [allergens, setAllergens] = useState<string[]>(() => {
    try { return JSON.parse(ingredient.allergens_json ?? '[]'); } catch { return []; }
  });
  const [fodmaps, setFodmaps] = useState<string[]>(() => {
    try { return JSON.parse(ingredient.fodmap_flags_json ?? '[]'); } catch { return []; }
  });

  const [warningAccepted, setWarningAccepted] = useState(false);

  const toggleChip = (list: string[], setter: (v: string[]) => void, code: string) =>
    setter(list.includes(code) ? list.filter((x) => x !== code) : [...list, code]);

  const handleSave = () => {
    if (!warningAccepted) return;
    onSave(ingredient.id, {
      name_de: nameDe,
      brand: brand || null,
      barcode: barcode || null,
      status,
      locked,
      energy_kcal_per_100g: kcal > 0 ? kcal : null,
      protein_g_per_100g: protein > 0 ? protein : null,
      carbs_g_per_100g: carbs > 0 ? carbs : null,
      sugar_g_per_100g: sugar > 0 ? sugar : null,
      fat_g_per_100g: fat > 0 ? fat : null,
      satfat_g_per_100g: satfat > 0 ? satfat : null,
      fiber_g_per_100g: fiber > 0 ? fiber : null,
      salt_g_per_100g: salt > 0 ? salt : null,
      histamine_score: histScore !== '' ? Number(histScore) : null,
      allergens_json: JSON.stringify(allergens),
      fodmap_flags_json: JSON.stringify(fodmaps),
      micronutrients_json: JSON.stringify(micros),
    });
  };

  return (
    <Dialog open={!!ingredient} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>Zutat bearbeiten: {ingredient.name_de}</DialogTitle>
      <DialogContent sx={{ pt: 2 }}>
        {/* ── Stammdaten ── */}
        <Accordion defaultExpanded>
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Typography fontWeight={600}>Stammdaten</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth label="Name (Deutsch)" value={nameDe}
                  onChange={(e) => setNameDe(e.target.value)} required />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth label="Marke" value={brand}
                  onChange={(e) => setBrand(e.target.value)} />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField fullWidth label="Barcode" value={barcode}
                  onChange={(e) => setBarcode(e.target.value)} />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField select fullWidth label="Status" value={status}
                  onChange={(e) => setStatus(e.target.value)}>
                  <MenuItem value="APPROVED">APPROVED</MenuItem>
                  <MenuItem value="PENDING">PENDING</MenuItem>
                  <MenuItem value="REJECTED">REJECTED</MenuItem>
                </TextField>
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField select fullWidth label="Gesperrt" value={String(locked)}
                  onChange={(e) => setLocked(e.target.value === 'true')}>
                  <MenuItem value="true">Ja (locked)</MenuItem>
                  <MenuItem value="false">Nein (editierbar)</MenuItem>
                </TextField>
              </Grid>
            </Grid>
          </AccordionDetails>
        </Accordion>

        {/* ── Nährwerte ── */}
        <Accordion defaultExpanded>
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Typography fontWeight={600}>Nährwerte (pro 100 g)</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <Grid container spacing={1.5}>
              {[
                ['Kalorien', kcal, setKcal, 0, 900, 'kcal'],
                ['Protein', protein, setProtein, 0, 100, 'g'],
                ['Kohlenhydrate', carbs, setCarbs, 0, 100, 'g'],
                ['Zucker', sugar, setSugar, 0, 100, 'g'],
                ['Fett', fat, setFat, 0, 100, 'g'],
                ['Ges. Fettsäuren', satfat, setSatfat, 0, 100, 'g'],
                ['Ballaststoffe', fiber, setFiber, 0, 30, 'g'],
                ['Salz', salt, setSalt, 0, 10, 'g'],
              ].map(([label, val, setter, min, max, unit]) => (
                <Grid item xs={12} key={label as string}>
                  <DetailSliderRow label={label as string} value={val as number}
                    onChange={setter as (v: number) => void}
                    min={min as number} max={max as number} unit={unit as string} />
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
                    <DetailSliderRow label={MICRO_LABEL[key] ?? key} value={val}
                      onChange={(v) => setMicros({ ...micros, [key]: v })}
                      min={meta[0]} max={meta[1]} unit={meta[2]} />
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
                    <DetailSliderRow label={MICRO_LABEL[key] ?? key} value={val}
                      onChange={(v) => setMicros({ ...micros, [key]: v })}
                      min={meta[0]} max={meta[1]} unit={meta[2]} />
                  </Grid>
                );
              })}
            </Grid>
          </AccordionDetails>
        </Accordion>

        {/* ── Diäten ── */}
        <Accordion>
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Typography fontWeight={600}>Diäten & Verträglichkeit</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <Grid container spacing={2}>
              {/* Histamin */}
              <Grid item xs={12}>
                <TextField select fullWidth size="small" label="Histamin-Score (SIGHI)"
                  value={String(histScore)} onChange={(e) => setHistScore(e.target.value)}>
                  {HISTAMINE_OPTIONS.map(({ value, label }) => (
                    <MenuItem key={value} value={value}>{label}</MenuItem>
                  ))}
                </TextField>
              </Grid>

              {/* Allergene */}
              <Grid item xs={12}>
                <Typography variant="body2" fontWeight={600} gutterBottom>Allergene (EU-14)</Typography>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.8 }}>
                  {ALLERGENS.map((a) => (
                    <Chip key={a} size="small" label={a}
                      onClick={() => toggleChip(allergens, setAllergens, a)}
                      color={allergens.includes(a) ? 'error' : 'default'}
                      variant={allergens.includes(a) ? 'filled' : 'outlined'} />
                  ))}
                </Box>
              </Grid>

              {/* FODMAP */}
              <Grid item xs={12}>
                <Typography variant="body2" fontWeight={600} gutterBottom>FODMAP</Typography>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.8 }}>
                  {FODMAPS.map((f) => (
                    <Chip key={f} size="small" label={f}
                      onClick={() => toggleChip(fodmaps, setFodmaps, f)}
                      color={fodmaps.includes(f) ? 'warning' : 'default'}
                      variant={fodmaps.includes(f) ? 'filled' : 'outlined'} />
                  ))}
                </Box>
              </Grid>
            </Grid>
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
              disabled={!nameDe.trim()}>
              Speichern
            </Button>
          </>
        )}
      </DialogActions>
    </Dialog>
  );
}

// ── P7.S5 — Shared helpers ──

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

function DetailSliderRow({ label, value, onChange, min, max, unit }: {
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
