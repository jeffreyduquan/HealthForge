import { useState, useCallback } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  IconButton,
  Paper,
  Snackbar,
  Stack,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Tabs,
  Tooltip,
  Typography,
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import AddIcon from '@mui/icons-material/Add';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import VisibilityIcon from '@mui/icons-material/Visibility';
import {
  listAllIngredients,
  updateIngredient,
  createIngredient,
  deleteIngredient,
  listAllSupplements,
  updateSupplement,
  createSupplement,
  deleteSupplement,
  listAllRecipes,
  updateRecipe,
  deleteRecipeCrud,
  createRecipe,
  type IngredientCrud,
  type SupplementCrud,
  type RecipeCrud,
} from '../api/client';
import IngredientWizard from '../components/IngredientWizard';
import SupplementWizard from '../components/SupplementWizard';
import RecipeWizard from '../components/RecipeWizard';
import IngredientDetailDialog from '../components/IngredientDetailDialog';
import RecipeDetailDialog from '../components/RecipeDetailDialog';

type TabValue = 'ingredients' | 'supplements' | 'recipes';

interface EditDialog {
  tab: TabValue;
  row: IngredientCrud | SupplementCrud | RecipeCrud | null; // null = create new
}

export default function DatabasePage() {
  const qc = useQueryClient();
  const [tab, setTab] = useState<TabValue>('ingredients');
  const [search, setSearch] = useState('');
  const [editDialog, setEditDialog] = useState<EditDialog | null>(null);
  const [detailIngredient, setDetailIngredient] = useState<IngredientCrud | null>(null);
  const [detailRecipe, setDetailRecipe] = useState<RecipeCrud | null>(null);
  const [deleteConfirm, setDeleteConfirm] = useState<{ tab: TabValue; id: string; label: string } | null>(null);
  const [snack, setSnack] = useState<string | null>(null);
  const [warningAccepted, setWarningAccepted] = useState(false);
  // Wizard visibility
  const [showIngredientWizard, setShowIngredientWizard] = useState(false);
  const [showSupplementWizard, setShowSupplementWizard] = useState(false);
  const [showRecipeWizard, setShowRecipeWizard] = useState(false);

  // === Queries ===
  const ingQ = useQuery({
    queryKey: ['crud-ingredients'],
    queryFn: () => listAllIngredients(),
    enabled: tab === 'ingredients',
  });
  const supQ = useQuery({
    queryKey: ['crud-supplements'],
    queryFn: () => listAllSupplements(),
    enabled: tab === 'supplements',
  });
  const recQ = useQuery({
    queryKey: ['crud-recipes'],
    queryFn: () => listAllRecipes(),
    enabled: tab === 'recipes',
  });

  // === Mutations ===
  const updateIngM = useMutation({
    mutationFn: ({ id, data }: { id: string; data: Record<string, unknown> }) => updateIngredient(id, data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['crud-ingredients'] }); setEditDialog(null); setSnack('✅ Zutat gespeichert'); },
    onError: (e) => setSnack('❌ Fehler: ' + (e as Error).message),
  });
  const createIngM = useMutation({
    mutationFn: (data: Record<string, unknown>) => createIngredient(data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['crud-ingredients'] }); setEditDialog(null); setSnack('✅ Zutat erstellt'); },
    onError: (e) => setSnack('❌ Fehler: ' + (e as Error).message),
  });
  const deleteIngM = useMutation({
    mutationFn: (id: string) => deleteIngredient(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['crud-ingredients'] }); setDeleteConfirm(null); setSnack('✅ Zutat gelöscht'); },
    onError: (e) => setSnack('❌ Fehler: ' + (e as Error).message),
  });

  const updateSupM = useMutation({
    mutationFn: ({ id, data }: { id: string; data: Record<string, unknown> }) => updateSupplement(id, data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['crud-supplements'] }); setEditDialog(null); setSnack('✅ Supplement gespeichert'); },
    onError: (e) => setSnack('❌ Fehler: ' + (e as Error).message),
  });
  const createSupM = useMutation({
    mutationFn: (data: Record<string, unknown>) => createSupplement(data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['crud-supplements'] }); setEditDialog(null); setSnack('✅ Supplement erstellt'); },
    onError: (e) => setSnack('❌ Fehler: ' + (e as Error).message),
  });
  const deleteSupM = useMutation({
    mutationFn: (id: string) => deleteSupplement(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['crud-supplements'] }); setDeleteConfirm(null); setSnack('✅ Supplement gelöscht'); },
    onError: (e) => setSnack('❌ Fehler: ' + (e as Error).message),
  });

  const updateRecM = useMutation({
    mutationFn: ({ id, data }: { id: string; data: Record<string, unknown> }) => updateRecipe(id, data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['crud-recipes'] }); setEditDialog(null); setSnack('✅ Rezept gespeichert'); },
    onError: (e) => setSnack('❌ Fehler: ' + (e as Error).message),
  });
  const deleteRecM = useMutation({
    mutationFn: (id: string) => deleteRecipeCrud(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['crud-recipes'] }); setDeleteConfirm(null); setSnack('✅ Rezept gelöscht'); },
    onError: (e) => setSnack('❌ Fehler: ' + (e as Error).message),
  });
  const createRecM = useMutation({
    mutationFn: (data: Record<string, unknown>) => createRecipe(data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['crud-recipes'] }); setSnack('✅ Rezept erstellt'); },
    onError: (e) => setSnack('❌ Fehler: ' + (e as Error).message),
  });

  const filteredIngredients = ingQ.data?.filter(
    (i) => !search || i.name_de.toLowerCase().includes(search.toLowerCase()) || i.barcode?.includes(search),
  ) ?? [];
  const filteredSupplements = supQ.data?.filter(
    (s) => !search || s.name_de.toLowerCase().includes(search.toLowerCase()),
  ) ?? [];
  const filteredRecipes = recQ.data?.filter(
    (r) => !search || r.title.toLowerCase().includes(search.toLowerCase()),
  ) ?? [];

  const handleEdit = useCallback((tab: TabValue, row: IngredientCrud | SupplementCrud | RecipeCrud | null) => {
    setWarningAccepted(false);
    setEditDialog({ tab, row });
  }, []);

  return (
    <Box sx={{ p: 3 }}>
      <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 2 }}>
        <Typography variant="h4">Datenbank-Editor</Typography>
        <Chip icon={<WarningAmberIcon />} label="ACHTUNG: Direkte DB-Änderungen!" color="warning" variant="outlined" />
      </Stack>

      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 2 }}>
        <Tab value="ingredients" label={`Zutaten (${ingQ.data?.length ?? '…'})`} />
        <Tab value="supplements" label={`Supplements (${supQ.data?.length ?? '…'})`} />
        <Tab value="recipes" label={`Rezepte (${recQ.data?.length ?? '…'})`} />
      </Tabs>

      <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
        <TextField
          size="small" placeholder="Suchen…" value={search}
          onChange={(e) => setSearch(e.target.value)}
          sx={{ minWidth: 300 }}
        />
        <Button
          variant="contained" startIcon={<AddIcon />}
          onClick={() => {
            if (tab === 'ingredients') setShowIngredientWizard(true);
            else if (tab === 'supplements') setShowSupplementWizard(true);
            else if (tab === 'recipes') setShowRecipeWizard(true);
          }}
        >
          Neu
        </Button>
      </Stack>

      {/* === INGREDIENTS TABLE === */}
      {tab === 'ingredients' && (
        <>
          {ingQ.isLoading && <CircularProgress />}
          {ingQ.isError && <Alert severity="error">Fehler beim Laden</Alert>}
          {filteredIngredients.length > 0 && (
            <TableContainer component={Paper}>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Name (DE)</TableCell>
                    <TableCell>Marke</TableCell>
                    <TableCell>Barcode</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>kcal/100g</TableCell>
                    <TableCell>Histamin</TableCell>
                    <TableCell align="right">Aktionen</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {filteredIngredients.map((r) => (
                    <TableRow
                      key={r.id}
                      hover
                      onClick={() => setDetailIngredient(r)}
                      sx={{ cursor: 'pointer' }}
                    >
                      <TableCell>{r.name_de}</TableCell>
                      <TableCell>{r.brand ?? '—'}</TableCell>
                      <TableCell>{r.barcode ?? '—'}</TableCell>
                      <TableCell><Chip size="small" label={r.status} color={r.status === 'APPROVED' ? 'success' : r.status === 'PENDING' ? 'warning' : 'error'} /></TableCell>
                      <TableCell>{r.energy_kcal_per_100g ?? '—'}</TableCell>
                      <TableCell>{r.histamine_score ?? '—'}</TableCell>
                      <TableCell align="right">
                        <Tooltip title="Details"><IconButton size="small" color="info" onClick={(e) => { e.stopPropagation(); setDetailIngredient(r); }}><VisibilityIcon fontSize="small" /></IconButton></Tooltip>
                        <Tooltip title="Löschen"><IconButton size="small" color="error" onClick={(e) => { e.stopPropagation(); setDeleteConfirm({ tab: 'ingredients', id: r.id, label: r.name_de }); }}><DeleteIcon fontSize="small" /></IconButton></Tooltip>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
          {!ingQ.isLoading && filteredIngredients.length === 0 && (
            <Alert severity="info">Keine Zutaten gefunden.</Alert>
          )}
        </>
      )}

      {/* === SUPPLEMENTS TABLE === */}
      {tab === 'supplements' && (
        <>
          {supQ.isLoading && <CircularProgress />}
          {supQ.isError && <Alert severity="error">Fehler beim Laden</Alert>}
          {filteredSupplements.length > 0 && (
            <TableContainer component={Paper}>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Name (DE)</TableCell>
                    <TableCell>Marke</TableCell>
                    <TableCell>Einheit</TableCell>
                    <TableCell>Dosis</TableCell>
                    <TableCell>kcal</TableCell>
                    <TableCell>Protein</TableCell>
                    <TableCell>Notizen</TableCell>
                    <TableCell align="right">Aktionen</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {filteredSupplements.map((r) => (
                    <TableRow key={r.id} hover>
                      <TableCell>{r.name_de}</TableCell>
                      <TableCell>{r.brand ?? '—'}</TableCell>
                      <TableCell>{r.unit_label}</TableCell>
                      <TableCell>{r.default_dose}</TableCell>
                      <TableCell>{r.kcal_per_dose ?? '—'}</TableCell>
                      <TableCell>{r.protein_per_dose ?? '—'}</TableCell>
                      <TableCell sx={{ maxWidth: 200 }}>{r.notes ?? '—'}</TableCell>
                      <TableCell align="right">
                        <Tooltip title="Bearbeiten"><IconButton size="small" color="primary" onClick={() => handleEdit('supplements', r)}><EditIcon fontSize="small" /></IconButton></Tooltip>
                        <Tooltip title="Löschen"><IconButton size="small" color="error" onClick={() => setDeleteConfirm({ tab: 'supplements', id: r.id, label: r.name_de })}><DeleteIcon fontSize="small" /></IconButton></Tooltip>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
          {!supQ.isLoading && filteredSupplements.length === 0 && (
            <Alert severity="info">Keine Supplements gefunden.</Alert>
          )}
        </>
      )}

      {/* === RECIPES TABLE === */}
      {tab === 'recipes' && (
        <>
          {recQ.isLoading && <CircularProgress />}
          {recQ.isError && <Alert severity="error">Fehler beim Laden</Alert>}
          {filteredRecipes.length > 0 && (
            <TableContainer component={Paper}>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Titel</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Sichtbarkeit</TableCell>
                    <TableCell>Portionen</TableCell>
                    <TableCell>Minuten</TableCell>
                    <TableCell>Erstellt</TableCell>
                    <TableCell align="right">Aktionen</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {filteredRecipes.map((r) => (
                    <TableRow
                      key={r.id}
                      hover
                      onClick={() => setDetailRecipe(r)}
                      sx={{ cursor: 'pointer' }}
                    >
                      <TableCell>{r.title}</TableCell>
                      <TableCell><Chip size="small" label={r.status} color={r.status === 'PUBLISHED' ? 'success' : r.status === 'PENDING_REVIEW' ? 'warning' : 'error'} /></TableCell>
                      <TableCell>{r.visibility}</TableCell>
                      <TableCell>{r.servings}</TableCell>
                      <TableCell>{r.prep_minutes}{r.cook_minutes ? `+${r.cook_minutes}` : ''}</TableCell>
                      <TableCell>{new Date(r.created_at).toLocaleDateString('de-DE')}</TableCell>
                      <TableCell align="right">
                        <Tooltip title="Bearbeiten"><IconButton size="small" color="primary" onClick={() => handleEdit('recipes', r)}><EditIcon fontSize="small" /></IconButton></Tooltip>
                        <Tooltip title="Löschen"><IconButton size="small" color="error" onClick={() => setDeleteConfirm({ tab: 'recipes', id: r.id, label: r.title })}><DeleteIcon fontSize="small" /></IconButton></Tooltip>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
          {!recQ.isLoading && filteredRecipes.length === 0 && (
            <Alert severity="info">Keine Rezepte gefunden.</Alert>
          )}
        </>
      )}

      {/* === Edit/Create Dialog === */}
      <EditDialogComponent
        dialog={editDialog}
        warningAccepted={warningAccepted}
        onWarningAccept={() => setWarningAccepted(true)}
        onClose={() => setEditDialog(null)}
        onSave={({ tab, id, data }) => {
          setSnack('⏳ Speichere…');
          if (tab === 'ingredients') {
            if (id) updateIngM.mutate({ id, data });
            else createIngM.mutate(data);
          } else if (tab === 'supplements') {
            if (id) updateSupM.mutate({ id, data });
            else createSupM.mutate(data);
          } else if (tab === 'recipes') {
            if (id) updateRecM.mutate({ id, data });
          }
        }}
      />

      {/* Delete Confirm */}
      <Dialog open={!!deleteConfirm} onClose={() => setDeleteConfirm(null)}>
        <DialogTitle>⚠️ Wirklich löschen?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            <strong>{deleteConfirm?.label}</strong> wird dauerhaft aus der Datenbank gelöscht.
            Diese Aktion kann nicht rückgängig gemacht werden.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteConfirm(null)}>Abbrechen</Button>
          <Button color="error" variant="contained" onClick={() => {
            if (!deleteConfirm) return;
            const { tab, id } = deleteConfirm;
            if (tab === 'ingredients') deleteIngM.mutate(id);
            else if (tab === 'supplements') deleteSupM.mutate(id);
            else if (tab === 'recipes') deleteRecM.mutate(id);
          }}>
            Löschen
          </Button>
        </DialogActions>
      </Dialog>

      {/* === Creation Wizards === */}
      <IngredientWizard
        open={showIngredientWizard}
        onClose={() => setShowIngredientWizard(false)}
        onSave={(data) => {
          createIngM.mutate(data);
          setShowIngredientWizard(false);
        }}
        saving={createIngM.isPending}
      />
      <SupplementWizard
        open={showSupplementWizard}
        onClose={() => setShowSupplementWizard(false)}
        onSave={(data) => {
          createSupM.mutate(data);
          setShowSupplementWizard(false);
        }}
        saving={createSupM.isPending}
      />
      <RecipeWizard
        open={showRecipeWizard}
        onClose={() => setShowRecipeWizard(false)}
        onSave={(data) => {
          createRecM.mutate(data);
          setShowRecipeWizard(false);
        }}
        saving={createRecM.isPending}
      />

      <Snackbar open={!!snack} autoHideDuration={4000} onClose={() => setSnack(null)} message={snack ?? ''} />

      {/* === Ingredient Detail Dialog (full overview) === */}
      <IngredientDetailDialog
        ingredient={detailIngredient}
        onClose={() => setDetailIngredient(null)}
        onSave={(id, data) => {
          setSnack('⏳ Speichere…');
          updateIngM.mutate({ id, data });
        }}
      />
      <RecipeDetailDialog
        recipe={detailRecipe}
        onClose={() => setDetailRecipe(null)}
        onSave={(id, data) => {
          setSnack('⏳ Speichere…');
          updateRecM.mutate({ id, data });
        }}
      />
    </Box>
  );
}

// === Edit Dialog Sub-Component ===

interface EditDialogProps {
  dialog: EditDialog | null;
  warningAccepted: boolean;
  onWarningAccept: () => void;
  onClose: () => void;
  onSave: (params: { tab: string; id: string | null; data: Record<string, unknown> }) => void;
}

function EditDialogComponent({ dialog, warningAccepted, onWarningAccept, onClose, onSave }: EditDialogProps) {
  if (!dialog) return null;
  const isNew = !dialog.row;
  const isIngredient = dialog.tab === 'ingredients';
  const isSupplement = dialog.tab === 'supplements';
  const isRecipe = dialog.tab === 'recipes';

  const [nameDe, setNameDe] = useState(isSupplement ? (dialog.row as SupplementCrud)?.name_de ?? '' : isIngredient ? (dialog.row as IngredientCrud)?.name_de ?? '' : '');
  const [brand, setBrand] = useState(isIngredient ? (dialog.row as IngredientCrud)?.brand ?? '' : isSupplement ? (dialog.row as SupplementCrud)?.brand ?? '' : '');
  const [barcode, setBarcode] = useState(isIngredient ? (dialog.row as IngredientCrud)?.barcode ?? '' : '');
  const [kcal, setKcal] = useState(isIngredient ? String((dialog.row as IngredientCrud)?.energy_kcal_per_100g ?? '') : isSupplement ? String((dialog.row as SupplementCrud)?.kcal_per_dose ?? '') : '');
  const [protein, setProtein] = useState(isIngredient ? String((dialog.row as IngredientCrud)?.protein_g_per_100g ?? '') : isSupplement ? String((dialog.row as SupplementCrud)?.protein_per_dose ?? '') : '');
  const [carbs, setCarbs] = useState(isIngredient ? String((dialog.row as IngredientCrud)?.carbs_g_per_100g ?? '') : isSupplement ? String((dialog.row as SupplementCrud)?.carbs_per_dose ?? '') : '');
  const [fat, setFat] = useState(isIngredient ? String((dialog.row as IngredientCrud)?.fat_g_per_100g ?? '') : isSupplement ? String((dialog.row as SupplementCrud)?.fat_per_dose ?? '') : '');
  const [histScore, setHistScore] = useState(isIngredient ? String((dialog.row as IngredientCrud)?.histamine_score ?? '') : '');
  const [unitLabel, setUnitLabel] = useState(isSupplement ? (dialog.row as SupplementCrud)?.unit_label ?? '' : '');
  const [defaultDose, setDefaultDose] = useState(isSupplement ? String((dialog.row as SupplementCrud)?.default_dose ?? '') : '');
  const [notes, setNotes] = useState(isSupplement ? (dialog.row as SupplementCrud)?.notes ?? '' : '');
  // Recipe fields
  const [title, setTitle] = useState(isRecipe ? (dialog.row as RecipeCrud)?.title ?? '' : '');
  const [recipeStatus, setRecipeStatus] = useState(isRecipe ? (dialog.row as RecipeCrud)?.status ?? 'PUBLISHED' : '');
  const [recipeVisibility, setRecipeVisibility] = useState(isRecipe ? (dialog.row as RecipeCrud)?.visibility ?? 'PUBLIC' : '');
  const [servings, setServings] = useState(isRecipe ? String((dialog.row as RecipeCrud)?.servings ?? '') : '');
  const [prepMin, setPrepMin] = useState(isRecipe ? String((dialog.row as RecipeCrud)?.prep_minutes ?? '') : '');

  const handleSave = () => {
    if (!warningAccepted) return;
    let data: Record<string, unknown> = {};
    if (isIngredient) {
      data = {
        name_de: nameDe, brand: brand || null, barcode: barcode || null,
        energy_kcal_per_100g: kcal ? Number(kcal) : null,
        protein_g_per_100g: protein ? Number(protein) : null,
        carbs_g_per_100g: carbs ? Number(carbs) : null,
        fat_g_per_100g: fat ? Number(fat) : null,
        histamine_score: histScore ? Number(histScore) : null,
      };
    } else if (isSupplement) {
      data = {
        name_de: nameDe, brand: brand || null,
        unit_label: unitLabel || 'g', default_dose: Number(defaultDose) || 1,
        kcal_per_dose: kcal ? Number(kcal) : null,
        protein_per_dose: protein ? Number(protein) : null,
        carbs_per_dose: carbs ? Number(carbs) : null,
        fat_per_dose: fat ? Number(fat) : null,
        notes: notes || null,
      };
    } else if (isRecipe) {
      data = {
        title, status: recipeStatus, visibility: recipeVisibility,
        servings: Number(servings) || 1, prep_minutes: Number(prepMin) || 0,
      };
    }
    onSave({ tab: dialog.tab, id: isNew ? null : dialog.row!.id, data });
  };

  return (
    <Dialog open={!!dialog} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <WarningAmberIcon color="warning" />
        {isNew ? 'Neu erstellen' : 'Bearbeiten'} — {dialog.tab === 'ingredients' ? 'Zutat' : dialog.tab === 'supplements' ? 'Supplement' : 'Rezept'}
      </DialogTitle>
      <DialogContent>
        {!warningAccepted && (
          <Alert severity="warning" sx={{ mb: 2 }} action={
            <Button size="small" color="warning" variant="outlined" onClick={onWarningAccept}>Verstanden</Button>
          }>
            <strong>Achtung:</strong> Du bearbeitest direkt die Datenbank!
            Änderungen können nicht rückgängig gemacht werden. Bitte bestätigen.
          </Alert>
        )}

        <Stack spacing={2} sx={{ mt: 1 }}>
          {isIngredient && (
            <>
              <TextField label="Name (DE)" value={nameDe} onChange={(e) => setNameDe(e.target.value)} fullWidth required />
              <TextField label="Marke" value={brand} onChange={(e) => setBrand(e.target.value)} fullWidth />
              <TextField label="Barcode" value={barcode} onChange={(e) => setBarcode(e.target.value)} fullWidth />
              <TextField label="kcal/100g" value={kcal} onChange={(e) => setKcal(e.target.value)} type="number" fullWidth />
              <TextField label="Protein g/100g" value={protein} onChange={(e) => setProtein(e.target.value)} type="number" fullWidth />
              <TextField label="Carbs g/100g" value={carbs} onChange={(e) => setCarbs(e.target.value)} type="number" fullWidth />
              <TextField label="Fat g/100g" value={fat} onChange={(e) => setFat(e.target.value)} type="number" fullWidth />
              <TextField label="Histamin Score" value={histScore} onChange={(e) => setHistScore(e.target.value)} type="number" fullWidth />
            </>
          )}
          {isSupplement && (
            <>
              <TextField label="Name (DE)" value={nameDe} onChange={(e) => setNameDe(e.target.value)} fullWidth required />
              <TextField label="Marke" value={brand} onChange={(e) => setBrand(e.target.value)} fullWidth />
              <TextField label="Einheit (z.B. g, ml)" value={unitLabel} onChange={(e) => setUnitLabel(e.target.value)} fullWidth required />
              <TextField label="Standard-Dosis" value={defaultDose} onChange={(e) => setDefaultDose(e.target.value)} type="number" fullWidth required />
              <TextField label="kcal/Dosis" value={kcal} onChange={(e) => setKcal(e.target.value)} type="number" fullWidth />
              <TextField label="Protein/Dosis" value={protein} onChange={(e) => setProtein(e.target.value)} type="number" fullWidth />
              <TextField label="Carbs/Dosis" value={carbs} onChange={(e) => setCarbs(e.target.value)} type="number" fullWidth />
              <TextField label="Fat/Dosis" value={fat} onChange={(e) => setFat(e.target.value)} type="number" fullWidth />
              <TextField label="Notizen" value={notes} onChange={(e) => setNotes(e.target.value)} multiline rows={2} fullWidth />
            </>
          )}
          {isRecipe && (
            <>
              <TextField label="Titel" value={title} onChange={(e) => setTitle(e.target.value)} fullWidth required disabled={!warningAccepted} />
              <TextField label="Status" value={recipeStatus} onChange={(e) => setRecipeStatus(e.target.value)} fullWidth disabled={!warningAccepted}
                helperText="PUBLISHED / PENDING_REVIEW / REJECTED / REMOVED" />
              <TextField label="Sichtbarkeit" value={recipeVisibility} onChange={(e) => setRecipeVisibility(e.target.value)} fullWidth disabled={!warningAccepted}
                helperText="PUBLIC / PRIVATE / GROUP" />
              <TextField label="Portionen" value={servings} onChange={(e) => setServings(e.target.value)} type="number" fullWidth disabled={!warningAccepted} />
              <TextField label="Zubereitungsminuten" value={prepMin} onChange={(e) => setPrepMin(e.target.value)} type="number" fullWidth disabled={!warningAccepted} />
            </>
          )}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Abbrechen</Button>
        <Button onClick={handleSave} variant="contained" color="warning" disabled={!warningAccepted || (isIngredient && !nameDe.trim()) || (isSupplement && (!nameDe.trim() || !unitLabel.trim())) || (isRecipe && !title.trim())}>
          {isNew ? 'Erstellen' : 'Speichern'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

// ── IngredientDetailDialog moved to ../components/IngredientDetailDialog.tsx ──
