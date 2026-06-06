-- P7.S4: Invite download tracking (one-time APK download via invite code)
ALTER TABLE invites ADD COLUMN IF NOT EXISTS download_used BOOLEAN DEFAULT FALSE;
ALTER TABLE invites ADD COLUMN IF NOT EXISTS download_used_at TIMESTAMP;
