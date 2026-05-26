-- HealthForge — switch ingredient search from FTS (which doesn't decompound German
-- words: 'brot' won't match 'Vollkornbrot') to pg_trgm-backed ILIKE substring search.
-- Sprint P1.S5 fix.

-- The legacy FTS index is kept for future ranking/snippet use, but the hot search path
-- now uses these trigram-GIN indexes on a normalised (unaccent + lower) projection.

CREATE INDEX IF NOT EXISTS idx_ingredients_name_trgm
    ON ingredients USING GIN (hf_immutable_unaccent(lower(name_de)) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_ingredients_brand_trgm
    ON ingredients USING GIN (hf_immutable_unaccent(lower(coalesce(brand,''))) gin_trgm_ops);
