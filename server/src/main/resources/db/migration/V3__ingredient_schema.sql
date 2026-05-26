-- HealthForge Ingredient + ETL schema (Sprint P1.S4 — scaffolding without seed data yet)
-- LOCKED: full-text search uses 'german' dictionary + unaccent (V1 enabled both extensions).

-- ===================== immutable unaccent wrapper =====================
-- Postgres' built-in unaccent() is STABLE (not IMMUTABLE) because it depends on a
-- mutable dictionary catalogue; thus it cannot be used directly in an index expression.
-- We wrap the explicit `unaccent` dictionary (which IS effectively immutable for our
-- purposes since we never modify it at runtime) in an IMMUTABLE SQL function. The
-- application MUST use this same function in queries so the GIN index is hit.
CREATE OR REPLACE FUNCTION hf_immutable_unaccent(text)
RETURNS text
LANGUAGE sql
IMMUTABLE PARALLEL SAFE STRICT
AS $$ SELECT public.unaccent('public.unaccent'::regdictionary, $1) $$;

-- ===================== ingredients =====================
-- Master ingredient catalogue. Filled later by BLS / SIGHI / Open Food Facts importers.
-- `source` distinguishes origin; `locked = true` for canonical ETL entries (read-only),
-- `locked = false` for entries promoted from user suggestions (REQ-INGR-005ff).
CREATE TABLE ingredients (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    name_de                 TEXT            NOT NULL,
    brand                   TEXT,
    barcode                 TEXT,
    source                  TEXT            NOT NULL CHECK (source IN ('BLS','SIGHI','OFF','USER','MANUAL')),
    source_id               TEXT,           -- foreign key into source dataset
    energy_kcal_per_100g    NUMERIC(8,2),
    protein_g_per_100g      NUMERIC(8,2),
    carbs_g_per_100g        NUMERIC(8,2),
    sugar_g_per_100g        NUMERIC(8,2),
    fat_g_per_100g          NUMERIC(8,2),
    satfat_g_per_100g       NUMERIC(8,2),
    fiber_g_per_100g        NUMERIC(8,2),
    salt_g_per_100g         NUMERIC(8,2),
    histamine_score         SMALLINT        CHECK (histamine_score BETWEEN 0 AND 3),
    -- denormalized JSON arrays for fast read; canonical workflow stays via field-PRs (siehe REQ-INGR-008)
    allergens_json          TEXT            NOT NULL DEFAULT '[]',
    fodmap_flags_json       TEXT            NOT NULL DEFAULT '[]',
    locked                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT now(),
    UNIQUE (source, source_id)
);

-- FTS index on German-stemmed + unaccented name + brand (REQ-INGR-002).
CREATE INDEX idx_ingredients_fts
    ON ingredients
    USING GIN (
        to_tsvector('german', hf_immutable_unaccent(coalesce(name_de, '') || ' ' || coalesce(brand, '')))
    );

CREATE INDEX idx_ingredients_barcode ON ingredients(barcode) WHERE barcode IS NOT NULL;
CREATE INDEX idx_ingredients_source ON ingredients(source);

-- ===================== ingredient_field_pr =====================
-- Field-level pull-requests (siehe REQ-INGR-008): user-proposed change for a single column.
-- P1.S4 only provisions the table; review workflow is implemented in P3.S2.
CREATE TABLE ingredient_field_pr (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    ingredient_id   UUID            NOT NULL REFERENCES ingredients(id) ON DELETE CASCADE,
    proposer_id     UUID            REFERENCES users(id) ON DELETE SET NULL,
    field_name      TEXT            NOT NULL,
    old_value       TEXT,
    new_value       TEXT            NOT NULL,
    reason          TEXT,
    status          TEXT            NOT NULL DEFAULT 'PENDING'
                                    CHECK (status IN ('PENDING','APPROVED','REJECTED','WITHDRAWN')),
    reviewer_id     UUID            REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at     TIMESTAMPTZ,
    review_note     TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_ingr_pr_status ON ingredient_field_pr(status);
CREATE INDEX idx_ingr_pr_ingredient ON ingredient_field_pr(ingredient_id);

-- ===================== ingredient_user_suggestions =====================
-- New-ingredient proposals (siehe REQ-INGR-006). P1.S4 only provisions the table.
CREATE TABLE ingredient_user_suggestions (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    proposer_id     UUID            REFERENCES users(id) ON DELETE SET NULL,
    name_de         TEXT            NOT NULL,
    brand           TEXT,
    barcode         TEXT,
    payload_json    TEXT            NOT NULL, -- serialized partial Ingredient
    status          TEXT            NOT NULL DEFAULT 'PENDING'
                                    CHECK (status IN ('PENDING','APPROVED','REJECTED','WITHDRAWN')),
    reviewer_id     UUID            REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at     TIMESTAMPTZ,
    review_note     TEXT,
    approved_ingredient_id UUID     REFERENCES ingredients(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_ingr_sugg_status ON ingredient_user_suggestions(status);

-- ===================== etl_runs =====================
-- Audit log for each ETL import (REQ-ADMIN-004).
CREATE TABLE etl_runs (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    source          TEXT            NOT NULL CHECK (source IN ('BLS','SIGHI','OFF')),
    started_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    finished_at     TIMESTAMPTZ,
    status          TEXT            NOT NULL DEFAULT 'RUNNING'
                                    CHECK (status IN ('RUNNING','SUCCESS','FAILED','SKIPPED_NO_FILE')),
    rows_inserted   INTEGER         NOT NULL DEFAULT 0,
    rows_updated    INTEGER         NOT NULL DEFAULT 0,
    rows_skipped    INTEGER         NOT NULL DEFAULT 0,
    error_message   TEXT,
    triggered_by    UUID            REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_etl_runs_source_started ON etl_runs(source, started_at DESC);

-- ===================== FCM removal =====================
-- FCM was vetoed (siehe ReqSpec → REMOVED markers). Drop the leftover table.
DROP TABLE IF EXISTS devices;
