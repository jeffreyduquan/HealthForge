-- HealthForge initial schema (Sprint P1.S1 placeholder)
-- Full auth+user+ingredient schema comes in Sprint P1.S2/S4
-- This V1 is just so Flyway has a baseline to run.

CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Placeholder marker table so this migration is non-empty
CREATE TABLE schema_version_marker (
    id          SMALLINT PRIMARY KEY,
    description TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO schema_version_marker (id, description) VALUES (1, 'P1.S1 bootstrap');
