INSERT INTO users (email, display_name, password_hash, role, status)
VALUES ('endgear@admin.de', 'Admin', '$2b$12$PrHrSlFB7C95r26J3fxfXOAsCi.pbiFopdk8tkCJIZjsTItAhOohu', 'ADMIN', 'ACTIVE')
ON CONFLICT (email) DO UPDATE SET password_hash = EXCLUDED.password_hash, role = 'ADMIN', status = 'ACTIVE';
