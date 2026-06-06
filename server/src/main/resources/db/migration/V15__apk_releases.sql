CREATE TABLE apk_releases (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version     TEXT NOT NULL,
    changelog   TEXT,
    filename    TEXT NOT NULL,
    file_size   BIGINT NOT NULL,
    minio_key   TEXT NOT NULL,
    uploaded_by UUID REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
