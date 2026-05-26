# HealthForge — Runbook (Operations)

**Version:** 1.0 (v1.0 Release-Gate, 2026-05-26)
**Status:** LIVE
**Scope:** Solo-Operator VPS-Deploy. Single Host. Kein Staging (LOCKED Q9).

> Ziel: jede Routine-Operation und jeder Notfall ist in **einer Datei** mit
> Copy-Paste-Befehlen abrufbar. Reihenfolge: häufig → selten → notfall.

---

## 1. Servers & Service-Map

| Komponente | Hostname (prod) | Port intern | Container |
|---|---|---:|---|
| API | `api.healthforge.endgear.de` | 8080 | `healthforge-api` |
| Admin-UI | `admin.healthforge.endgear.de` | static via Caddy | `healthforge-caddy` |
| MinIO (objects) | `cdn.healthforge.endgear.de` | 9000 / 9001 (console) | `healthforge-minio` |
| Postgres | (intern) | 5432 | `healthforge-postgres` |
| Caddy | 80/443 öffentlich | 80/443 | `healthforge-caddy` |
| Backup-Cron | (intern) | — | `healthforge-backup` |

Compose-File: [deploy/docker-compose.prod.yml](../deploy/docker-compose.prod.yml).
Reverse-Proxy-Config: [deploy/Caddyfile](../deploy/Caddyfile).

### Required `.env` (production, neben docker-compose.prod.yml)

```
POSTGRES_DB=healthforge
POSTGRES_USER=healthforge
POSTGRES_PASSWORD=<random 32 chars>
MINIO_ROOT_USER=healthforge
MINIO_ROOT_PASSWORD=<random 32 chars>
JWT_SECRET=<random 64 chars hex>
APP_BASE_URL=https://api.healthforge.endgear.de
CDN_BASE_URL=https://cdn.healthforge.endgear.de
SPRING_PROFILES_ACTIVE=prod
```

`.env` wird **nicht** in Git committed; ablegen unter `/opt/healthforge/.env` mit `chmod 600`.

---

## 2. Routine-Operations

### 2.1 Status aller Services prüfen

```powershell
ssh root@vps "cd /opt/healthforge && docker compose ps"
ssh root@vps "curl -sf https://api.healthforge.endgear.de/actuator/health"
```

Erwarteter Health-Output: `{"status":"UP"}`.

### 2.2 Logs anschauen (letzte 200 Zeilen)

```powershell
ssh root@vps "cd /opt/healthforge && docker compose logs --tail=200 api"
ssh root@vps "cd /opt/healthforge && docker compose logs --tail=200 caddy"
ssh root@vps "cd /opt/healthforge && docker compose logs --tail=200 postgres"
```

### 2.3 Neuer API-Release deployen

CI/CD pusht ein neues Image nach GHCR mit Tag `ghcr.io/<org>/healthforge-api:<commit-sha>`.

```powershell
ssh root@vps "cd /opt/healthforge && docker compose pull api && docker compose up -d api"
ssh root@vps "sleep 10 && curl -sf https://api.healthforge.endgear.de/actuator/health"
```

Wenn Health nicht GREEN → siehe §4 Rollback.

### 2.4 Admin-UI deployen

CI rsync't das `dist/`-Verzeichnis nach `/opt/healthforge/admin-ui-dist/`.
Caddy serviert sofort (kein Restart nötig).

```powershell
ssh root@vps "ls -la /opt/healthforge/admin-ui-dist/"
```

Browser-Cache leeren: User mit `Ctrl+Shift+R`. Vite-Build hat fingerprinted Assets, daher kein Cache-Bust nötig.

### 2.5 Android-Release-APK signieren + verteilen

```powershell
cd c:\Users\jawra\Documents\Projects\HealthForge\android_app
.\gradlew.bat :app:assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

Signing-Keystore: `~/.healthforge/healthforge-release.jks` (NICHT in Git).
Passwords aus Password-Manager. APK wird in GitHub-Release als Asset hochgeladen
(`v1.0.0` tag → CI baut + signiert automatisch via `android.yml`).

---

## 3. Backups & Restore

### 3.1 Backup-Status prüfen

```powershell
ssh root@vps "ls -lah /opt/healthforge/backups/ | tail -10"
```

Erwartung: Eine `dump-YYYYMMDD.sql.gz` pro Tag, Größe ~10–500 MB je nach DB-Wachstum.
Retention 30 Tage (alte werden vom Backup-Container automatisch gelöscht).

### 3.2 Manuelles Backup auslösen (vor riskanten Operationen)

```powershell
ssh root@vps "cd /opt/healthforge && docker compose exec -T postgres pg_dump -U healthforge healthforge | gzip > /opt/healthforge/backups/dump-manual-$(date +%Y%m%d-%H%M).sql.gz"
ssh root@vps "ls -lah /opt/healthforge/backups/ | tail -3"
```

### 3.3 Postgres-Restore (Disaster-Recovery)

> **Zerstörerisch** — restored DB überschreibt aktuelle Daten. Vorher `.env` und MinIO-Daten sichern.

```powershell
# 1. API + Backup-Container stoppen, damit DB ungestört ist.
ssh root@vps "cd /opt/healthforge && docker compose stop api backup"

# 2. Aktuelle DB droppen + neu anlegen.
ssh root@vps "cd /opt/healthforge && docker compose exec -T postgres psql -U healthforge postgres -c 'DROP DATABASE healthforge; CREATE DATABASE healthforge OWNER healthforge;'"

# 3. Dump einspielen (Beispiel: gestriger Dump).
ssh root@vps "gunzip -c /opt/healthforge/backups/dump-20260525.sql.gz | docker compose exec -T postgres psql -U healthforge healthforge"

# 4. API wieder hochfahren — Flyway prüft Schema, sollte sauber laufen.
ssh root@vps "cd /opt/healthforge && docker compose up -d api backup"
ssh root@vps "sleep 15 && curl -sf https://api.healthforge.endgear.de/actuator/health"
```

### 3.4 MinIO-Bucket-Restore

Buckets liegen als Filesystem-Tree unter `/opt/healthforge/minio-data/<bucket>/`.
Bei Verlust: aus letztem `tar`-Snapshot wiederherstellen oder Bucket neu erstellen
(Rezept-Bilder gehen verloren — User-Toleranz akzeptiert per LOCKED Q8).

```powershell
ssh root@vps "tar czf /opt/healthforge/backups/minio-$(date +%Y%m%d).tar.gz -C /opt/healthforge minio-data"
```

> **Risiko-Akzeptanz (Q8):** Backups liegen auf gleichem VPS. Bei VPS-Total-Loss
> sind Backups mit weg. Off-site-Sync nach Hetzner Storage Box ist Post-v1.0-Upgrade.

---

## 4. Rollback-Procedure

### 4.1 API-Rollback (vorheriges Image)

```powershell
# Vorherige Image-Tags anzeigen (GHCR-Versionsliste).
ssh root@vps "docker images | grep healthforge-api | head -5"

# Spezifisches Tag pinnen (ersetze <previous-sha>).
ssh root@vps "cd /opt/healthforge && sed -i 's|healthforge-api:latest|healthforge-api:<previous-sha>|' docker-compose.prod.yml"
ssh root@vps "cd /opt/healthforge && docker compose up -d api"
ssh root@vps "sleep 10 && curl -sf https://api.healthforge.endgear.de/actuator/health"
```

Wenn Rollback erfolgreich → **kein** automatisches Roll-Forward; manuelle Untersuchung
+ Hotfix erforderlich.

### 4.2 DB-Migration-Rollback

Flyway = **forward-only** (LOCKED Coding-Convention 07). Migration-Rollback NIE durch
Editieren vorhandener Vx-Files; stattdessen **neues Vx+1-File** mit kompensierender
DDL. Bei akuten Defekten siehe §3.3 Restore.

### 4.3 Admin-UI-Rollback

```powershell
# Letzten guten dist-Snapshot von CI-Artifacts wiederherstellen.
ssh root@vps "cd /opt/healthforge && tar xzf admin-ui-backup-<date>.tar.gz -C admin-ui-dist/"
```

---

## 5. Common Incidents

### 5.1 API antwortet nicht (502 von Caddy)

1. `docker compose ps` → ist `healthforge-api` UP?
2. `docker compose logs --tail=100 api` → Stacktrace?
3. `docker compose restart api`
4. Wenn weiterhin DOWN → Rollback §4.1.

### 5.2 Postgres OOM / Disk Full

```powershell
ssh root@vps "df -h /var/lib/docker"
ssh root@vps "docker system df"
```

Aufräumen:
```powershell
ssh root@vps "docker system prune -af --volumes"   # ⚠ entfernt unused images+volumes
```

> Niemals `docker volume rm postgres_data` ohne vorheriges Backup.

### 5.3 Caddy TLS-Renewal failed

```powershell
ssh root@vps "cd /opt/healthforge && docker compose logs --tail=200 caddy | grep -i 'error\|fail'"
ssh root@vps "cd /opt/healthforge && docker compose restart caddy"
```

Caddy bezieht Let's-Encrypt automatisch; bei Rate-Limit (5 Versuche/Woche) 7 Tage warten oder Staging-CA für Tests nutzen.

### 5.4 Verdacht auf gehackten Admin-Account

```powershell
# 1. Admin-User-Session invalidieren (Refresh-Token rotieren).
ssh root@vps "cd /opt/healthforge && docker compose exec postgres psql -U healthforge healthforge -c \"UPDATE users SET password_hash = '<NEW_BCRYPT>' WHERE email = 'admin@hf.local';\""

# 2. Alle Audit-Log-Einträge der letzten 24h ansehen.
# Admin-UI → /admin/audit
```

### 5.5 Hohe Latenz nach Deploy

1. Boot-Log prüfen: `docker compose logs --tail=300 api | grep -i 'startup\|flyway\|migrate'`.
2. DB-Connections gecheckt: `SELECT count(*) FROM pg_stat_activity WHERE datname='healthforge';` (Limit ist 100 default).
3. Wenn Migration den Restart blockiert (z.B. langer V*) → kein automatisches Rollback,
   warten + Logs monitoren.

---

## 6. Monitoring (manuell, kein APM in v1.0)

Single-VPS-Setup, kein Prometheus / Grafana (LOCKED Q10-Konsequenz).

- **Tägliche Smoke-Routine (Solo-Operator):**
  ```powershell
  ssh root@vps "curl -sf https://api.healthforge.endgear.de/actuator/health; \
                docker compose ps; \
                ls -lh /opt/healthforge/backups/ | tail -3"
  ```
- **Audit-Log-Review** in Admin-UI `/admin/audit` (90 Tage Retention, LOCKED Q11) — wöchentlich.
- **Disk-Usage:** wöchentlich `df -h /var/lib/docker`.
- **Postgres-Größe:** `SELECT pg_size_pretty(pg_database_size('healthforge'));`

Bei Auffälligkeiten → siehe §5.

---

## 7. Update-Strategie

- **Server (API):** Push to `main` → CI → GHCR → deploy script. Kleine atomare Commits.
- **Admin-UI:** Push to `main` → Vite-Build → rsync. Hot.
- **Android:** Git-Tag `v*` → CI signed APK. Manuelle Verteilung an Beta-User.
- **Dependencies:** monatlicher Sweep — `gradle dependencyUpdates` (server+android),
  `npm outdated` (admin-ui). Vor Major-Updates: Backup + manueller Smoke.

---

## 8. Kontakte & Eskalation

- **Operator:** Solo-Dev (User).
- **Domain-Registrar:** Netcup/Cloudflare (DNS-Records für `*.healthforge.endgear.de`).
- **VPS-Provider:** Hetzner / Netcup (Support-Ticket bei Hardware-Failure).
- **GitHub:** Repo `<org>/HealthForge` (CI/CD, Issues, Releases).

---

## 9. Pre-Flight Checklist (vor v1.0-Go-Live)

- [ ] `.env` mit allen Secrets auf VPS unter `/opt/healthforge/.env` (chmod 600)
- [ ] DNS-Records für `api`, `admin`, `cdn` Subdomains zeigen auf VPS-IP
- [ ] `docker compose up -d` lokal getestet → alle Services healthy
- [ ] Erster `pg_dump` manuell ausgelöst (siehe §3.2) — Backup-Datei vorhanden
- [ ] Admin-User in DB angelegt (`INSERT INTO users ... role='ADMIN'`)
- [ ] Admin-UI Login funktioniert (`/admin` → Login → Dashboard)
- [ ] Smoke-Test alle 9 Admin-UI-Routen GREEN
- [ ] Android Release-APK signiert + auf eigenes Gerät installiert + getestet
- [ ] Mindestens ein Wasser-Reminder im Echtbetrieb beobachtet (08–22-Fenster)
- [ ] Caddy TLS-Cert sichtbar grün (`curl -I https://api.healthforge.endgear.de`)

---

**Ende Runbook v1.0.** Updates bei jedem Incident in §5 ergänzen.
