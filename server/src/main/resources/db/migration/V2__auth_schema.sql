-- HealthForge Auth schema (Sprint P1.S2)
-- LOCKED Q6: JWT HS512, Access 15min, Refresh 30d.

-- ===================== users =====================
CREATE TABLE users (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    email               TEXT            NOT NULL UNIQUE,
    display_name        TEXT            NOT NULL,
    password_hash       TEXT            NOT NULL, -- bcrypt cost 12
    role                TEXT            NOT NULL DEFAULT 'USER' CHECK (role IN ('USER','ADMIN')),
    status              TEXT            NOT NULL DEFAULT 'PENDING_VERIFICATION'
                                        CHECK (status IN ('PENDING_VERIFICATION','ACTIVE','BANNED','DELETED')),
    email_verified_at   TIMESTAMPTZ,
    last_login_at       TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_role ON users(role);

-- ===================== invites =====================
CREATE TABLE invites (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    code            TEXT            NOT NULL UNIQUE,
    created_by      UUID            REFERENCES users(id),
    note            TEXT,
    used_by         UUID            REFERENCES users(id),
    used_at         TIMESTAMPTZ,
    expires_at      TIMESTAMPTZ     NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_invites_code ON invites(code) WHERE used_at IS NULL;

-- ===================== refresh_tokens =====================
-- Rotation: bei Refresh wird altes Token revoked und ein neues ausgegeben.
CREATE TABLE refresh_tokens (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash      TEXT            NOT NULL UNIQUE, -- SHA-256 hash, never store plain
    device_label    TEXT,
    issued_at       TIMESTAMPTZ     NOT NULL DEFAULT now(),
    expires_at      TIMESTAMPTZ     NOT NULL,
    revoked_at      TIMESTAMPTZ,
    replaced_by     UUID            REFERENCES refresh_tokens(id),
    ip_address      TEXT,
    user_agent      TEXT
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id) WHERE revoked_at IS NULL;
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at);

-- ===================== email_verification_tokens =====================
CREATE TABLE email_verification_tokens (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash      TEXT            NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ     NOT NULL,
    used_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_email_verification_user ON email_verification_tokens(user_id);

-- ===================== password_reset_tokens =====================
CREATE TABLE password_reset_tokens (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash      TEXT            NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ     NOT NULL,
    used_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_password_reset_user ON password_reset_tokens(user_id);

-- ===================== devices (FCM tokens) =====================
CREATE TABLE devices (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    fcm_token       TEXT            NOT NULL UNIQUE,
    platform        TEXT            NOT NULL DEFAULT 'android' CHECK (platform IN ('android')),
    app_version     TEXT,
    last_seen_at    TIMESTAMPTZ     NOT NULL DEFAULT now(),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_devices_user ON devices(user_id);

-- ===================== audit_log =====================
-- LOCKED Q11: 90-Tage-Rolling-Retention via cron cleanup.
CREATE TABLE audit_log (
    id              BIGSERIAL       PRIMARY KEY,
    occurred_at     TIMESTAMPTZ     NOT NULL DEFAULT now(),
    actor_user_id   UUID            REFERENCES users(id) ON DELETE SET NULL,
    actor_kind      TEXT            NOT NULL CHECK (actor_kind IN ('USER','ADMIN','SYSTEM')),
    action          TEXT            NOT NULL, -- e.g. 'AUTH_LOGIN', 'INGREDIENT_EDIT', 'USER_BAN'
    target_type     TEXT,           -- e.g. 'USER','RECIPE','INGREDIENT'
    target_id       TEXT,
    ip_address      TEXT,
    detail          TEXT   -- serialized JSON; not queried, plain TEXT avoids JSONB binding overhead
);

CREATE INDEX idx_audit_occurred_at ON audit_log(occurred_at);
CREATE INDEX idx_audit_actor ON audit_log(actor_user_id);
CREATE INDEX idx_audit_action ON audit_log(action);
