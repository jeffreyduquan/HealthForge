import { useState } from 'react';
import { Box, Tab, Tabs, Typography } from '@mui/material';
import SupplementsQueuePage from './SupplementsQueuePage';
import IngredientQueuePage from './IngredientQueuePage';
import FieldPrPage from './FieldPrPage';
import RecipeQueuePage from './RecipeQueuePage';

type TabValue = 'supplements' | 'ingredients' | 'field-prs' | 'recipes';

const TABS: { value: TabValue; label: string }[] = [
  { value: 'supplements', label: 'Supplements' },
  { value: 'ingredients', label: 'Zutaten' },
  { value: 'field-prs', label: 'Field-PRs' },
  { value: 'recipes', label: 'Rezepte' },
];

export default function PendingPage() {
  const [tab, setTab] = useState<TabValue>('supplements');

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" sx={{ mb: 2 }}>Ausstehend</Typography>
      <Tabs value={tab} onChange={(_, v: TabValue) => setTab(v)} sx={{ mb: 2 }}>
        {TABS.map((t) => (
          <Tab key={t.value} value={t.value} label={t.label} />
        ))}
      </Tabs>

      {tab === 'supplements' && <SupplementsQueuePage />}
      {tab === 'ingredients' && <IngredientQueuePage />}
      {tab === 'field-prs' && <FieldPrPage />}
      {tab === 'recipes' && <RecipeQueuePage />}
    </Box>
  );
}
