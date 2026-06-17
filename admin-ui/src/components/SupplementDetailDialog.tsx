import { useState } from 'react';
import {
  Accordion, AccordionSummary, AccordionDetails,
  Box, Button, Dialog, DialogActions, DialogContent, DialogTitle,
  Grid, IconButton, MenuItem, TextField, Typography,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
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
  const [kcal, setKcal] = useState(supplement.kcal_per_dose ?? '');
  const [protein, setProtein] = useState(supplement.protein_per_dose ?? '');
  const [carbs, setCarbs] = useState(supplement.carbs_per_dose ?? '');
  const [fat, setFat] = useState(supplement.fat_per_dose ?? '');

  // ── Mikronährstoffe ──
  const [micros, setMicros] = useState<{ key: string; value: string }[]>(() => {
    const full = buildFullMicronutrients(supplement.micronutrients_json);
    return Object.entries(full).map(([k, v]) => ({ key: k, value: String(v) }));
  });

  const [warningAccepted, setWarningAccepted] = useState(false);

  const addMicro = () => setMicros([...micros, { key: '', value: '' }]);
  const removeMicro = (i: number) => setMicros(micros.filter((_, idx) => idx !== i));
  const updateMicroKey = (i: number, k: string) => {
    const m = [...micros];
    const item = m[i];
    if (item) { m[i] = { key: k, value: item.value }; setMicros(m); }
  };
  const updateMicroVal = (i: number, v: string) => {
    const m = [...micros];
    const item = m[i];
    if (item) { m[i] = { key: item.key, value: v }; setMicros(m); }
  };

  const handleSave = () => {
    if (!warningAccepted) return;
    const microObj: Record<string, number> = {};
    micros.forEach(({ key, value }) => {
      if (key.trim() && value) microObj[key.trim()] = Number(value);
    });
    onSave(supplement.id, {
      name_de: nameDe.trim(),
      brand: brand.trim() || null,
      unit_label: unitLabel,
      default_dose: Number(defaultDose) || 1,
      kcal_per_dose: kcal !== '' ? Number(kcal) : null,
      protein_per_dose: protein !== '' ? Number(protein) : null,
      carbs_per_dose: carbs !== '' ? Number(carbs) : null,
      fat_per_dose: fat !== '' ? Number(fat) : null,
      micronutrients_json: JSON.stringify(microObj),
      notes: notes.trim() || null,
    });
  };

  const nutrientFields: [string, string, string, (v: string) => void][] = [
    ['kcal_per_dose', 'Kalorien (kcal)', String(kcal), setKcal],
    ['protein_per_dose', 'Protein (g)', String(protein), setProtein],
    ['carbs_per_dose', 'Kohlenhydrate (g)', String(carbs), setCarbs],
    ['fat_per_dose', 'Fett (g)', String(fat), setFat],
  ];

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
              {nutrientFields.map(([key, label, val, setter]) => (
                <Grid item xs={6} key={key}>
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
                      placeholder="z.B. vitamin_d3" sx={{ width: 180 }} />
                    <TextField size="small" label="Wert / Dosis" value={m.value}
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
