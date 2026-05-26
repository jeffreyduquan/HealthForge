-- P4.S1 — User-Ingredient-Submissions + Field-PR (REQ-INGR-USER-001/-002, REQ-FIELDPR-001..003)
--
-- 1) ingredients gets a peer-review lifecycle (status, submitter, reviewer, last admin touch).
-- 2) ingredient_field_pr stores per-field correction proposals.

ALTER TABLE ingredients
    ADD COLUMN IF NOT EXISTS status TEXT NOT NULL DEFAULT 'APPROVED'
        CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    ADD COLUMN IF NOT EXISTS submitted_by UUID NULL
        REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS reviewer_id UUID NULL
        REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS review_note TEXT NULL,
    ADD COLUMN IF NOT EXISTS last_admin_edit_at TIMESTAMPTZ NULL;

CREATE INDEX IF NOT EXISTS idx_ingredients_status_submitted_by
    ON ingredients (status, submitted_by);

CREATE TABLE IF NOT EXISTS ingredient_field_pr (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ingredient_id UUID NOT NULL REFERENCES ingredients(id) ON DELETE CASCADE,
    proposer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    field_name TEXT NOT NULL,
    old_value TEXT NULL,
    new_value TEXT NOT NULL,
    rationale TEXT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    reviewer_id UUID NULL REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMPTZ NULL,
    review_note TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ingredient_field_pr_status_created
    ON ingredient_field_pr (status, created_at);
CREATE INDEX IF NOT EXISTS idx_ingredient_field_pr_ingredient
    ON ingredient_field_pr (ingredient_id);
