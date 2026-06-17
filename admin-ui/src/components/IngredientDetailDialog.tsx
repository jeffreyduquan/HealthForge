import { useState } from 'react';
import {
  Accordion, AccordionSummary, AccordionDetails,
  Box, Button, Chip, Dialog, DialogActions, DialogContent, DialogTitle,
  Grid, IconButton, MenuItem, TextField, Typography,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import type { IngredientCrud } from '../api/client';

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
  const [kcal, setKcal] = useState(ingredient.energy_kcal_per_100g ?? '');
  const [protein, setProtein] = useState(ingredient.protein_g_per_100g ?? '');
  const [carbs, setCarbs] = useState(ingredient.carbs_g_per_100g ?? '');
  const [sugar, setSugar] = useState(ingredient.sugar_g_per_100g ?? '');
  const [fat, setFat] = useState(ingredient.fat_g_per_100g ?? '');
  const [satfat, setSatfat] = useState(ingredient.satfat_g_per_100g ?? '');
  const [fiber, setFiber] = useState(ingredient.fiber_g_per_100g ?? '');
  const [salt, setSalt] = useState(ingredient.salt_g_per_100g ?? '');

  // ── Mikronährstoffe ──
  const [micros, setMicros] = useState<{ key: string; value: string }[]>(() => {
    try {
      const obj = JSON.parse(ingredient.micronutrients_json ?? '{}');
      return Object.entries(obj).map(([k, v]) => ({ key: k, value: String(v) }));
    } catch { return []; }
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

  const addMicro = () => setMicros([...micros, { key: '', value: '' }]);
  const removeMicro = (i: number) => setMicros(micros.filter((_, idx) => idx !== i));
  const updateMicroKey = (i: number, k: string) => {
    const m = [...micros];
    m[i] = { ...m[i], key: k };
    setMicros(m);
  };
  const updateMicroVal = (i: number, v: string) => {
    const m = [...micros];
    m[i] = { ...m[i], value: v };
    setMicros(m);
  };

  const handleSave = () => {
    if (!warningAccepted) return;
    // Build micronutrients JSON from key-value pairs
    const microObj: Record<string, number> = {};
    micros.forEach(({ key, value }) => {
      if (key.trim() && value) microObj[key.trim()] = Number(value);
    });
    onSave(ingredient.id, {
      name_de: nameDe,
      brand: brand || null,
      barcode: barcode || null,
      status,
      locked,
      energy_kcal_per_100g: kcal !== '' ? Number(kcal) : null,
      protein_g_per_100g: protein !== '' ? Number(protein) : null,
      carbs_g_per_100g: carbs !== '' ? Number(carbs) : null,
      sugar_g_per_100g: sugar !== '' ? Number(sugar) : null,
      fat_g_per_100g: fat !== '' ? Number(fat) : null,
      satfat_g_per_100g: satfat !== '' ? Number(satfat) : null,
      fiber_g_per_100g: fiber !== '' ? Number(fiber) : null,
      salt_g_per_100g: salt !== '' ? Number(salt) : null,
      histamine_score: histScore !== '' ? Number(histScore) : null,
      allergens_json: JSON.stringify(allergens),
      fodmap_flags_json: JSON.stringify(fodmaps),
      micronutrients_json: JSON.stringify(microObj),
    });
  };

  const macroFields: [string, string, string, (v: string) => void][] = [
    ['energy_kcal_per_100g', 'Kalorien (kcal)', String(kcal), setKcal],
    ['protein_g_per_100g', 'Protein (g)', String(protein), setProtein],
    ['carbs_g_per_100g', 'Kohlenhydrate (g)', String(carbs), setCarbs],
    ['sugar_g_per_100g', 'Zucker (g)', String(sugar), setSugar],
    ['fat_g_per_100g', 'Fett (g)', String(fat), setFat],
    ['satfat_g_per_100g', 'Ges. Fettsäuren (g)', String(satfat), setSatfat],
    ['fiber_g_per_100g', 'Ballaststoffe (g)', String(fiber), setFiber],
    ['salt_g_per_100g', 'Salz (g)', String(salt), setSalt],
  ];

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
              {macroFields.map(([key, label, val, setter]) => (
                <Grid item xs={6} sm={4} md={3} key={key}>
                  <TextField fullWidth size="small" label={label} value={val}
                    onChange={(e) => setter(e.target.value)}
                    type="number" inputProps={{ step: 0.1 }} />
                </Grid>
              ))}
            </Grid>
          </AccordionDetails>
        </Accordion>

        {/* ── Mikronährstoffe ── */}
        <Accordion>
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Typography fontWeight={600}>Mikronährstoffe ({micros.length})</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <Grid container spacing={1}>
              {micros.map((m, i) => (
                <Grid item xs={12} key={i}>
                  <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                    <TextField size="small" label="Key" value={m.key}
                      onChange={(e) => updateMicroKey(i, e.target.value)}
                      placeholder="z.B. vitamin_c" sx={{ width: 180 }} />
                    <TextField size="small" label="Wert / 100g" value={m.value}
                      onChange={(e) => updateMicroVal(i, e.target.value)}
                      type="number" inputProps={{ step: 0.01 }} sx={{ flex: 1 }} />
                    <IconButton size="small" color="error" onClick={() => removeMicro(i)}>
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </Box>
                </Grid>
              ))}
              <Grid item xs={12}>
                <Button size="small" startIcon={<AddIcon />} onClick={addMicro}>
                  Mikronährstoff hinzufügen
                </Button>
              </Grid>
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
