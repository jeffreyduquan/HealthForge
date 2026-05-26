-- V8: recipe_reports table (Fixup für P3.S3 — Entity wurde angelegt aber Migration vergessen).
-- REQ-GROUP-007: Recipe-Reports + Moderation.

CREATE TABLE IF NOT EXISTS recipe_reports (
    id           UUID PRIMARY KEY,
    recipe_id    UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    reporter_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason       TEXT NOT NULL,
    status       TEXT NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','RESOLVED','DISMISSED')),
    resolved_by  UUID REFERENCES users(id) ON DELETE SET NULL,
    resolved_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_recipe_reports_status_created
    ON recipe_reports (status, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_recipe_reports_recipe
    ON recipe_reports (recipe_id);
