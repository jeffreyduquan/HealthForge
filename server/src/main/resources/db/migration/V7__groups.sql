-- HealthForge Groups schema (Sprint P3.S1).
-- REQ-GROUP-001..006.
-- Notes:
--  * `visibility` is the locked 2-state enum {PUBLIC, PRIVATE}.
--    Public groups are discoverable; private groups join only via invite_code.
--  * `invite_code` MUST be unique when not NULL. Generated server-side (8-char alphanumeric).
--  * `group_members.role` is OWNER | ADMIN | MEMBER. Exactly one OWNER per group enforced
--    by partial unique index (one OWNER per group_id).
--  * `recipes.group_id` FK is added here (was nullable placeholder in V6).
--  * `member_count` is denormalised for cheap discovery sort; maintained by service.

-- ===================== groups =====================
CREATE TABLE groups (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    name            TEXT            NOT NULL CHECK (length(btrim(name)) > 0),
    description     TEXT,
    visibility      TEXT            NOT NULL DEFAULT 'PRIVATE'
                                    CHECK (visibility IN ('PUBLIC','PRIVATE')),
    invite_code     TEXT            UNIQUE,
    owner_id        UUID            NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    member_count    INT             NOT NULL DEFAULT 1 CHECK (member_count >= 0),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT groups_invite_code_iff_private CHECK (
        (visibility = 'PRIVATE' AND invite_code IS NOT NULL)
        OR (visibility = 'PUBLIC' AND invite_code IS NULL)
    )
);

CREATE INDEX idx_groups_visibility ON groups(visibility, created_at DESC);
CREATE INDEX idx_groups_owner ON groups(owner_id);
CREATE INDEX idx_groups_name_search
    ON groups
    USING GIN (
        to_tsvector('german', hf_immutable_unaccent(coalesce(name, '') || ' ' || coalesce(description, '')))
    );

-- ===================== group_members =====================
CREATE TABLE group_members (
    group_id        UUID            NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id         UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role            TEXT            NOT NULL DEFAULT 'MEMBER'
                                    CHECK (role IN ('OWNER','ADMIN','MEMBER')),
    joined_at       TIMESTAMPTZ     NOT NULL DEFAULT now(),
    PRIMARY KEY (group_id, user_id)
);

CREATE INDEX idx_group_members_user ON group_members(user_id);
-- Exactly one OWNER per group.
CREATE UNIQUE INDEX uniq_group_owner ON group_members(group_id) WHERE role = 'OWNER';

-- ===================== recipes.group_id FK (added now that groups exists) =====================
ALTER TABLE recipes
    ADD CONSTRAINT recipes_group_fk
    FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE SET NULL;
