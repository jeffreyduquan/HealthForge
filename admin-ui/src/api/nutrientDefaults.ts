// All micronutrient keys from NutrientCatalog (vitamins + minerals)
// Used to ensure every ingredient/supplement has all fields present in micronutrients_json
export const ALL_MICRONUTRIENT_KEYS = [
  // Vitamins
  'vitamin_a', 'vitamin_d', 'vitamin_e', 'vitamin_k',
  'vitamin_b1', 'vitamin_b2', 'vitamin_b3', 'vitamin_b5',
  'vitamin_b6', 'vitamin_b7', 'vitamin_b9', 'vitamin_b12',
  'vitamin_c',
  // Minerals
  'calcium', 'eisen', 'magnesium', 'zink', 'kupfer',
  'mangan', 'selen', 'jod', 'kalium', 'natrium', 'phosphor',
];

// All macros (stored as separate columns, not in micronutrients_json)
export const ALL_MACRO_KEYS = [
  'kcal', 'protein', 'carbs', 'sugar', 'fat', 'satfat', 'fiber', 'salt',
];

// All allergens (EU-14) — stored as JSON array in allergens_json
export const ALL_ALLERGEN_KEYS = [
  'GLUTEN', 'CRUSTACEANS', 'EGGS', 'FISH', 'PEANUT', 'SOY',
  'MILK', 'NUTS', 'CELERY', 'MUSTARD', 'SESAME', 'SULPHITES',
  'LUPIN', 'MOLLUSCS',
];

// All FODMAP types — stored as JSON array in fodmap_flags_json
export const ALL_FODMAP_KEYS = [
  'FRUCTOSE', 'LACTOSE', 'FRUCTANS', 'GOS', 'POLYOLS',
];

/**
 * Build a default micronutrients_json object with all keys set to 0.
 * Existing values from the DB are merged on top so non-zero values are preserved.
 */
export function buildFullMicronutrients(existingJson: string | null | undefined): Record<string, number> {
  const defaults: Record<string, number> = {};
  ALL_MICRONUTRIENT_KEYS.forEach((k) => { defaults[k] = 0; });
  try {
    const existing = JSON.parse(existingJson ?? '{}');
    return { ...defaults, ...existing };
  } catch {
    return defaults;
  }
}

/**
 * Build default micronutrients for a new entity (all zeros).
 */
export function defaultMicronutrients(): Record<string, number> {
  const obj: Record<string, number> = {};
  ALL_MICRONUTRIENT_KEYS.forEach((k) => { obj[k] = 0; });
  return obj;
}
