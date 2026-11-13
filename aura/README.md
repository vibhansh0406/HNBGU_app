# Aura — Energy & Circadian Rhythm Companion

A zero-dependency, single-file Python backend + vanilla-JS frontend that turns your daily energy into a living generative artwork and reveals your personal circadian rhythm.

## What it is

Aura is a personal energy-logging companion. Each day you log a quick check-in (energy, focus, mood, tags, note). Your check-in doesn't just go into a table — it **sculpts your Aura**, a generative particle flow field where:

- Each check-in becomes a **vortex** positioned by the hour you logged it, sized by its energy.
- Your check-ins literally bend the particle flow around them — your data becomes the artwork.
- Mood sets the color palette, energy sets speed/intensity, focus sets particle brightness.

Over time, Aura reveals your **circadian rhythm**: a polar clock showing your energy by hour of day, your best focus window, your strongest weekday, and a weekly trend.

## Why it's useful

- **Understand your rhythm** — see when your energy peaks and guard that window for deep work.
- **Build a streak** — daily logging compounds into a visible pattern you can return to.
- **Own your data** — export everything (check-ins, settings, profile) as JSON at any time; delete your account and all data permanently.

## Run it

Requires only Python 3 (3.9+) — no `pip`, no `npm`, no external services.

```bash
cd /workspace/HNBGU_app/aura
python3 server.py
```

Then open `http://localhost:8765` on any device (phone, tablet, desktop). The app is mobile-first and works offline after the initial load (static assets are served by the same server).

## Tech

- **Backend**: Python stdlib only — `http.server` (threaded), `sqlite3` (WAL mode), `hashlib` (PBKDF2 password hashing), `secrets` (session tokens).
- **Auth**: email/password accounts, HttpOnly SameSite session cookies (30-day), password hashes never leave the server.
- **Database**: SQLite, one file (`aura.db`), WAL mode for concurrent reads.
- **Frontend**: vanilla JavaScript SPA, HTML5 Canvas generative artwork, no frameworks, no build step.
- **Design**: cinematic dark/light themes, sophisticated typography, fluid animations, mobile-first responsive layout, accessible (ARIA sliders, focus-visible).

## Endpoints

All under `/api/`, session-authenticated via `aura_session` cookie unless noted:

- `POST /api/auth/signup`, `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me`
- `POST /api/checkins`, `GET /api/checkins`, `PUT /api/checkins/:id`, `DELETE /api/checkins/:id`
- `GET /api/insights` — energy/focus by hour & weekday, best focus window, streak, weekly comparison
- `GET /api/settings`, `PUT /api/settings` — theme + daily reminder preference
- `PUT /api/onboarding` — wake/sleep/work window + timezone
- `GET /api/export` — full user data JSON
- `DELETE /api/account` — permanent account + data deletion

## Data model

- `users` — id, email, password_hash (PBKDF2), name, wake/sleep/work times, tz, created/updated
- `checkins` — id, user_id, ts (local wall-clock ISO-8601 with offset), energy/focus (1-10), mood, tags (JSON), note
- `settings` — user_id, theme, reminders
- `sessions` — token, user_id, expires_at

All timestamps are stored as the user's local wall-clock time with its UTC offset, so the server and client always agree on what hour/date "now" is — no timezone drift.
