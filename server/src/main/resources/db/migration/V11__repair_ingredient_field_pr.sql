-- V11: idempotent repair migration.
--
-- Background: V10 uses CREATE TABLE IF NOT EXISTS for ingredient_field_pr and
-- CREATE INDEX IF NOT EXISTS for indexes. On dev databases that already had a
-- prior, incomplete schema of ingredient_field_pr (created during early P4.S1
-- iteration), V10's CREATE TABLE was skipped and the table is missing newer
-- columns added in V10's final form. On clean databases V11 is a no-op.
--
-- Rule: forward-only Flyway, never edit historical migrations.

ALTER TABLE ingredient_field_pr ADD COLUMN IF NOT EXISTS proposer_id UUID;
ALTER TABLE ingredient_field_pr ADD COLUMN IF NOT EXISTS field_name TEXT;
ALTER TABLE ingredient_field_pr ADD COLUMN IF NOT EXISTS old_value TEXT;
ALTER TABLE ingredient_field_pr ADD COLUMN IF NOT EXISTS new_value TEXT;
ALTER TABLE ingredient_field_pr ADD COLUMN IF NOT EXISTS rationale TEXT;
ALTER TABLE ingredient_field_pr ADD COLUMN IF NOT EXISTS status TEXT NOT NULL DEFAULT 'PENDING';
ALTER TABLE ingredient_field_pr ADD COLUMN IF NOT EXISTS reviewer_id UUID;
ALTER TABLE ingredient_field_pr ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMPTZ;
ALTER TABLE ingredient_field_pr ADD COLUMN IF NOT EXISTS review_note TEXT;
ALTER TABLE ingredient_field_pr ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now();
