import { useState } from 'react';
import {
  Accordion, AccordionSummary, AccordionDetails,
  Box, Button, Dialog, DialogActions, DialogContent, DialogTitle,
  Grid, MenuItem, Slider, TextField, Typography,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import type { SupplementCrud } from '../api/client';
import { buildFullMicronutrients } from '../api/nutrientDefaults';

const UNIT_OPTIONS = ['Tablette', 'Kapsel', 'ml', 'g', 'Portion'];

interface Props {
  supplement: SupplementCrud | null;
  onClose: () => void;
  onSave: (id: string, data: Record<string, unknown>) => void;
}

export default function SupplementDetailDialog({ supplement, onClose, onSave }: Props) {
  if (!supplement) return null;

  // ── Stammdaten ──
  const [nameDe, setNameDe] = useState(supplement.name_de);
  const [brand, setBrand] = useState(supplement.brand ?? '');
  const [unitLabel, setUnitLabel] = useState(supplement.unit_label);
  const [defaultDose, setDefaultDose] = useState(String(supplement.default_dose));
  const [notes, setNotes] = useState(supplement.notes ?? '');

  // ── Nährwerte ──
  const [kcal, setKcal] = useState(supplement.kcal_per_dose ?? 0);
  const [protein, setProtein] = useState(supplement.protein_per_dose ?? 0);
  const [carbs, setCarbs] = useState(supplement.carbs_per_dose ?? 0);
  const [fat, setFat] = useState(supplement.fat_per_dose ?? 0);

  // ── Mikronährstoffe ──
  const [micros, setMicros] = useState<Record<string, number>>(() => {
    const full = buildFullMicronutrients(supplement.micronutrients_json);
    return full;
  });

  const [warningAccepted, setWarningAccepted] = useState(false);

  const handleSave = () => {
    if (!warningAccepted) return;
    onSave(supplement.id, {
      name_de: nameDe.trim(),
      brand: brand.trim() || null,
      unit_label: unitLabel,
      default_dose: Number(defaultDose) || 1,
      kcal_per_dose: kcal > 0 ? kcal : null,
      protein_per_dose: protein > 0 ? protein : null,
      carbs_per_dose: carbs > 0 ? carbs : null,
      fat_per_dose: fat > 0 ? fat : null,
      micronutrients_json: JSON.stringify(micros),
      notes: notes.trim() || null,
    });
  };

  return (
    <Dialog open={!!supplement} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Supplement bearbeiten: {supplement.name_de}</DialogTitle>
      <DialogContent sx={{ pt: 2 }}>
        {/* ── Stammdaten ── */}
        <Accordion defaultExpanded>
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Typography fontWeight={600}>Stammdaten</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={8}>
                <TextField fullWidth label="Name" value={nameDe}
                  onChange={(e) => setNameDe(e.target.value)} required />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField fullWidth label="Marke" value={brand}
                  onChange={(e) => setBrand(e.target.value)} />
              </Grid>
              <Grid item xs={6}>
                <TextField select fullWidth label="Einheit" value={unitLabel}
                  onChange={(e) => setUnitLabel(e.target.value)}>
                  {UNIT_OPTIONS.map((u) => (
                    <MenuItem key={u} value={u}>{u}</MenuItem>
                  ))}
                </TextField>
              </Grid>
              <Grid item xs={6}>
                <TextField fullWidth label="Standard-Dosis" value={defaultDose}
                  onChange={(e) => setDefaultDose(e.target.value)}
                  type="number" inputProps={{ step: 0.5, min: 0 }} required />
              </Grid>
              <Grid item xs={12}>
                <TextField fullWidth label="Notizen" value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  multiline minRows={2} />
              </Grid>
            </Grid>
          </AccordionDetails>
        </Accordion>

        {/* ── Nährwerte ── */}
        <Accordion defaultExpanded>
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Typography fontWeight={600}>Nährwerte (pro Dosis)</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <Grid container spacing={1.5}>
              {[
                ['Kalorien', kcal, setKcal, 0, 500, 'kcal'],
                ['Eiweiß', protein, setProtein, 0, 100, 'g'],
                ['Kohlenhydrate', carbs, setCarbs, 0, 100, 'g'],
                ['Fett', fat, setFat, 0, 100, 'g'],
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
              {VITAMIN_KEYS2.map((key) => {
                const val = micros[key] ?? 0;
                const meta = MICRO_META2[key] ?? [0, 100, 'mg'];
                return (
                  <Grid item xs={12} key={key}>
                    <DetailSliderRow label={MICRO_LABEL2[key] ?? key} value={val}
                      onChange={(v) => setMicros({ ...micros, [key]: v })}
                      min={meta[0]} max={meta[1]} unit={meta[2]} />
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
                    <DetailSliderRow label={MICRO_LABEL2[key] ?? key} value={val}
                      onChange={(v) => setMicros({ ...micros, [key]: v })}
                      min={meta[0]} max={meta[1]} unit={meta[2]} />
                  </Grid>
                );
              })}
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
              disabled={!nameDe.trim() || !unitLabel.trim()}>
              Speichern
            </Button>
          </>
        )}
      </DialogActions>
    </Dialog>
  );
}

// ── P7.S5 — Shared helpers ──

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
