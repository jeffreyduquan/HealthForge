-- HealthForge Recipe + Community-Rating schema (Sprint P2.S1).
-- REQ-RECIPE-001..009, REQ-RATING-002/003/005.
-- Notes:
--  * `visibility` is the locked 3-state enum (REQ-RECIPE-003).
--  * `slot_tags` is a NOT NULL array with cardinality >= 1 (REQ-RECIPE-005).
--  * Nutrition is NOT stored on the recipe row (REQ-RECIPE-007 — computed live).
--  * `status = REMOVED` is the soft-delete signal that keeps client-side intake-log
--    snapshots resolvable (REQ-RECIPE-009).
--  * `recipe_reports` schema is provisioned now; controller/endpoints arrive in P3.
--  * FTS index uses the same hf_immutable_unaccent wrapper introduced in V3.

-- ===================== recipes =====================
CREATE TABLE recipes (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id       UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title           TEXT            NOT NULL CHECK (length(btrim(title)) > 0),
    description     TEXT,
    image_key       TEXT,           -- MinIO key under bucket 'recipes' (sized variants share base key)
    servings        INT             NOT NULL DEFAULT 1 CHECK (servings >= 1),
    prep_minutes    INT             NOT NULL CHECK (prep_minutes >= 0),
    cook_minutes    INT             CHECK (cook_minutes IS NULL OR cook_minutes >= 0),
    -- REQ-RECIPE-005: ≥1 slot tag. Values from {BREAKFAST, LUNCH, DINNER, SNACK}.
    slot_tags       TEXT[]          NOT NULL CHECK (
                                        cardinality(slot_tags) >= 1
                                        AND slot_tags <@ ARRAY['BREAKFAST','LUNCH','DINNER','SNACK']::text[]
                                    ),
    status          TEXT            NOT NULL DEFAULT 'PUBLISHED'
                                    CHECK (status IN ('PUBLISHED','REMOVED')),
    -- REQ-RECIPE-003: visibility enum. group_id MUST be set iff visibility='GROUP'.
    visibility      TEXT            NOT NULL DEFAULT 'PUBLIC'
                                    CHECK (visibility IN ('PUBLIC','PRIVATE','GROUP')),
    group_id        UUID,           -- FK added in P3 when `groups` table exists
    is_official     BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT recipes_group_id_consistency CHECK (
        (visibility = 'GROUP' AND group_id IS NOT NULL)
        OR (visibility <> 'GROUP' AND group_id IS NULL)
    )
);

CREATE INDEX idx_recipes_browse ON recipes(status, visibility, created_at DESC);
CREATE INDEX idx_recipes_author ON recipes(author_id);
CREATE INDEX idx_recipes_slot_tags ON recipes USING GIN (slot_tags);
CREATE INDEX idx_recipes_fts
    ON recipes
    USING GIN (
        to_tsvector('german', hf_immutable_unaccent(coalesce(title, '') || ' ' || coalesce(description, '')))
    );

-- ===================== recipe_ingredients =====================
-- M:N recipe -> ingredient with quantity/unit. `position` keeps display order.
-- ON DELETE RESTRICT for ingredient_id so deleting a referenced ingredient is blocked
-- (snapshots on client protect *historic intake* but recipes still need the live ref).
CREATE TABLE recipe_ingredients (
    recipe_id       UUID            NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    position        INT             NOT NULL CHECK (position >= 0),
    ingredient_id   UUID            NOT NULL REFERENCES ingredients(id) ON DELETE RESTRICT,
    quantity        NUMERIC(10,3)   NOT NULL CHECK (quantity > 0),
    unit            TEXT            NOT NULL CHECK (length(btrim(unit)) > 0),
    is_optional     BOOLEAN         NOT NULL DEFAULT FALSE,
    note            TEXT,
    PRIMARY KEY (recipe_id, position)
);

CREATE INDEX idx_recipe_ingredients_ingredient ON recipe_ingredients(ingredient_id);

-- ===================== recipe_steps =====================
CREATE TABLE recipe_steps (
    recipe_id       UUID            NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    position        INT             NOT NULL CHECK (position >= 0),
    text            TEXT            NOT NULL CHECK (length(btrim(text)) > 0),
    image_key       TEXT,
    PRIMARY KEY (recipe_id, position)
);

-- ===================== recipe_likes =====================
-- REQ-RECIPE-004: "like" == add reference to user's saved-list (no copy).
CREATE TABLE recipe_likes (
    recipe_id       UUID            NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    user_id         UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    PRIMARY KEY (recipe_id, user_id)
);

CREATE INDEX idx_recipe_likes_user ON recipe_likes(user_id);

-- ===================== recipe_reports (P3 endpoint, P2 schema) =====================
CREATE TABLE recipe_reports (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id       UUID            NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    reporter_id     UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason          TEXT            NOT NULL CHECK (length(btrim(reason)) > 0),
    status          TEXT            NOT NULL DEFAULT 'OPEN'
                                    CHECK (status IN ('OPEN','RESOLVED','DISMISSED')),
    resolved_by     UUID            REFERENCES users(id) ON DELETE SET NULL,
    resolved_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_recipe_reports_status ON recipe_reports(status);
CREATE INDEX idx_recipe_reports_recipe ON recipe_reports(recipe_id);

-- ===================== recipe_ratings_community =====================
-- REQ-RATING-002/003/005: server-side public votes; per-user mutable/revocable.
CREATE TABLE recipe_ratings_community (
    recipe_id       UUID            NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    user_id         UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    value           TEXT            NOT NULL CHECK (value IN ('RECOMMEND','NOT_RECOMMEND')),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    PRIMARY KEY (recipe_id, user_id)
);

CREATE INDEX idx_recipe_ratings_recipe ON recipe_ratings_community(recipe_id);

-- ===================== ingredient_ratings_community =====================
-- Mirror table for ingredient-level community votes (schema-only in P2.S1;
-- endpoints land alongside ingredient-detail screen, latest P3).
CREATE TABLE ingredient_ratings_community (
    ingredient_id   UUID            NOT NULL REFERENCES ingredients(id) ON DELETE CASCADE,
    user_id         UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    value           TEXT            NOT NULL CHECK (value IN ('RECOMMEND','NOT_RECOMMEND')),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    PRIMARY KEY (ingredient_id, user_id)
);

CREATE INDEX idx_ingredient_ratings_ingredient ON ingredient_ratings_community(ingredient_id);

-- ===================== updated_at trigger =====================
-- Maintain updated_at automatically on recipes + community-rating rows.
CREATE OR REPLACE FUNCTION hf_touch_updated_at() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_recipes_updated_at
    BEFORE UPDATE ON recipes
    FOR EACH ROW EXECUTE FUNCTION hf_touch_updated_at();

CREATE TRIGGER trg_recipe_ratings_community_updated_at
    BEFORE UPDATE ON recipe_ratings_community
    FOR EACH ROW EXECUTE FUNCTION hf_touch_updated_at();

CREATE TRIGGER trg_ingredient_ratings_community_updated_at
    BEFORE UPDATE ON ingredient_ratings_community
    FOR EACH ROW EXECUTE FUNCTION hf_touch_updated_at();
