-- V9: Supplement-Peer-Review (REQ-SUPP-004 / P3.S4 Slice 2).
--
-- Datenmodell:
--   supplements_public     — globaler Katalog (Lese-API für alle User).
--   supplement_suggestions — User-Vorschläge, durchlaufen ADMIN-Review.
--                            Bei APPROVED wird eine Zeile in supplements_public
--                            erzeugt und in suggestion.public_id referenziert.

CREATE TABLE IF NOT EXISTS supplements_public (
    id                    UUID PRIMARY KEY,
    name_de               TEXT NOT NULL,
    brand                 TEXT,
    unit_label            TEXT NOT NULL,
    default_dose          DOUBLE PRECISION NOT NULL,
    kcal_per_dose         DOUBLE PRECISION,
    protein_per_dose      DOUBLE PRECISION,
    carbs_per_dose        DOUBLE PRECISION,
    fat_per_dose          DOUBLE PRECISION,
    micronutrients_json   JSONB,
    notes                 TEXT,
    created_by            UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_supplements_public_name
    ON supplements_public (LOWER(name_de));

CREATE TABLE IF NOT EXISTS supplement_suggestions (
    id                    UUID PRIMARY KEY,
    proposer_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name_de               TEXT NOT NULL,
    brand                 TEXT,
    unit_label            TEXT NOT NULL,
    default_dose          DOUBLE PRECISION NOT NULL,
    kcal_per_dose         DOUBLE PRECISION,
    protein_per_dose      DOUBLE PRECISION,
    carbs_per_dose        DOUBLE PRECISION,
    fat_per_dose          DOUBLE PRECISION,
    micronutrients_json   JSONB,
    notes                 TEXT,
    status                TEXT NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    reviewer_id           UUID REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at           TIMESTAMPTZ,
    review_note           TEXT,
    public_id             UUID REFERENCES supplements_public(id) ON DELETE SET NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_supp_suggestions_status_created
    ON supplement_suggestions (status, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_supp_suggestions_proposer
    ON supplement_suggestions (proposer_id);
