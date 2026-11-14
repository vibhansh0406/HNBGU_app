# CampusOne Project — Engineering Instructions (permanent)

## What this repo is
- **CampusOne/** — Android app `in.campusone.app` for HNBGU campus: Kotlin 2.0.21, AGP 8.7.2, Jetpack Compose (BOM 2024.11.00, Material3), Hilt 2.52, Room 2.6.1, KSP, WorkManager, Glance widget, DataStore, kotlinx.serialization. Backend: Supabase (REST + auth). Min SDK 26, target/compile SDK 35, JVM 17.
- **supabase/schema.sql** — authoritative DB schema; sync app-side schema changes with it.
- **aura/** — zero-dependency Python (server.py) + vanilla JS web app; do not add npm/pip deps to it.
- **CampusOne/ci/debug.keystore** — stable debug keystore pinned for Google Sign-In SHA-1. NEVER regenerate, move, or delete it.

## Permanent project constants (immutable — never change)
- **Application ID / package name: `in.campusone.app`** — locked in `namespace` + `applicationId` (`CampusOne/app/build.gradle.kts`). No renames, migrations, flavors, or `applicationIdSuffix` in any module or build type, ever.
- **Signing identity: `CampusOne/ci/debug.keystore`** (alias `androiddebugkey`) — the permanent signing identity for BOTH debug and release builds. SHA-1 `A9:A0:15:5D:1C:57:5A:4A:66:5E:15:7C:C7:23:DE:81:7B:39:CC:DE`. Never regenerate or replace; the Google OAuth Android client is registered against exactly this package + SHA-1.
- CI mirrors the keystore via GitHub Secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`); workflow decodes it and exports `KEYSTORE_FILE`/`KEYSTORE_PASSWORD`/`KEY_ALIAS`/`KEY_PASSWORD` for the release signing config.
- Every workflow must keep verifying the built APK's package name and cert fingerprint (`apksigner verify --print-certs` + `aapt dump badging`) so a signature drift fails loudly.

## Environment reality (read first, every session)
- Running on Android/PRoot (aarch64). Java 21 exists but **the JVM cannot execute here** (proot memory restriction).
- Therefore: **no local `gradle`/`./gradlew` builds or JVM tests can run in this environment.** Do not attempt or claim them.
- Verification paths that actually work here:
  1. `git` + GitHub Actions CI (`.github/workflows/build.yml` builds `assembleDebug` on push to main/phase-3). Check with `gh run list/watch` when available.
  2. Static verification: careful re-read of changed code paths, `grep` for symbol usages, consistency of imports/navigation/DI wiring.
  3. Python files: `python3 -m py_compile`; SQL: sanity-check syntax against schema.sql conventions.
- Build config lives in `CampusOne/gradle/libs.versions.toml` (version catalog). Add dependencies there, not inline.

## Token-efficiency rules (mandatory)
- NEVER scan the whole repo. Locate first: `glob`/`grep` by symbol, then `read` only the relevant file/offset range.
- Read large Compose screens with `offset`/`limit`; don't pull 2000-line files whole unless editing across them.
- Batch independent tool calls in parallel (multiple reads/greps per turn).
- Reuse what you already read in-session; keep a mental map of touched files instead of re-reading.
- No speculative tests, no throwaway scripts, no echo-debugging. Prefer reasoning + one targeted check.
- Keep diffs minimal: change root cause, not symptoms; one coherent change per task.

## Code conventions
- Follow existing package layout: `data/local` (Room), `data/remote` (Supabase), `data/repository`, `di`, `domain/model`, `domain/usecase`, `ui/screens`, `ui/components`, `worker`, `service`, `widget`.
- Compose: follow current screen styles (state hoisting, `collectAsStateWithLifecycle`, Navigation Compose routes in the existing nav graph file).
- Room: bump/exported schemas go to `CampusOne/app/schemas`; new entities need DAO + migration or fresh-install fallback consistent with existing `CampusOneDatabase`.
- No comments unless asked; no emojis; match surrounding style.

## Context discipline (per session)
- Load once at session start: AGENTS.md + the specific area of the task. Nothing else.
- Never reread a file already read in the session unless you changed it or have reason to believe it changed.
- After finishing a task, retain only: files changed + why, symbols touched, open follow-ups. Discard exploration details.
- Build dirs (`build-output/`, `CampusOne/app/build/`, `.gradle/`) are watcher-ignored and never read.
- Ask clarifying questions early; do not burn tokens exploring to resolve ambiguity you can resolve by asking.

## Failure recovery
- On a failed command: read the actual error, diagnose the cause, retry at most once and only for transient failures (network, timeout). Never loop retries.
- Never change approach randomly on failure; fix the root cause or stop.
- Never hide or soften failed verification. State what failed and why, exactly.

## Discipline
- Fix root causes; never stack random patches on a symptom.
- Never claim success without a real check (CI green, compile-consistent code, or executed test where possible). If unverifiable here, say exactly what remains to verify.
- Secrets (`local.properties`, keys, tokens): never print, commit, or move. `local.properties` is gitignored.
- Don't delete files or overwrite working settings without explicit instruction.
- No paid APIs, no new provider config, no key creation.
- Commit only when asked. Prefer `git status`/`git diff` review before any commit.
