# CampusOne Project — Engineering Instructions (permanent)

## What this repo is
- **CampusOne/** — Android app `in.campusone.app` for HNBGU campus: Kotlin 2.0.21, AGP 8.7.2, Jetpack Compose (BOM 2024.11.00, Material3), Hilt 2.52, Room 2.6.1, KSP, WorkManager, Glance widget, DataStore, kotlinx.serialization. Backend: Supabase (REST + auth). Min SDK 26, target/compile SDK 35, JVM 17.
- **supabase/schema.sql** — authoritative DB schema; sync app-side schema changes with it.
- **aura/** — zero-dependency Python (server.py) + vanilla JS web app; do not add npm/pip deps to it.
- Debug builds use the Android Gradle Plugin standard debug signing identity. Release builds use the permanent dedicated release keystore decoded from GitHub Actions secrets.

## Permanent project constants (immutable — never change)
- **Application ID / package name: `in.campusone.app`** — locked in `namespace` + `applicationId` (`CampusOne/app/build.gradle.kts`). No renames, migrations, flavors, or `applicationIdSuffix` in any module or build type, ever.
- **Permanent release identity:** SHA-1 `50:34:1B:DA:AE:1E:1C:FC:C7:E2:68:93:D3:CF:8A:1F:7A:FC:35:A2`. The release keystore is held in GitHub Actions secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) and decoded by `.github/workflows/build.yml`. Never regenerate the key, change its alias or credentials, delete or overwrite these secrets, or add a release-signing fallback.
- Debug signing uses the Android Gradle Plugin standard debug identity and is separate from the permanent release certificate. Debug and Release SHA-1 values must not be confused.
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

## Skill routing (project-level, `.opencode/skills/`)
Load skills on demand, one per task — never bulk-load. Preference order when several cover a task:
- **Design critique / visual polish / DESIGN.md**: `impeccable` (dedicated design authority).
- **Jetpack Compose specifics** (state/effects, performance, animations, component design, UI testing): `compose-*` skills (chrisbanes) first, then `android-production-ultimate` Compose sections.
- **Official platform APIs / workflows** (edge-to-edge, adaptive, Navigation 3, AGP upgrades, ANR, R8, security/permissions, Glance, CameraX, profiling): Google `android/skills` set.
- **Production architecture, security hardening, Play policy, release readiness, anti-AI-slop design taste**: `android-production-ultimate` (routed slices via its references/INDEX.md — do not load whole corpus).
- **Lifecycle/process workflows** (spec, plan, TDD, review, ship, debugging, git workflow): `agent-skills-android` set (`spec-driven-development`, `test-driven-development`, `code-review-and-quality`, `debugging-and-error-recovery`, ...).
- **Kotlin language design** (API design, coroutines/Flow, control flow): `kotlin-*` skills (chrisbanes).

## Installed Android engineering skills (`.agent/skills/`)

The project ships a curated, verbatim copy of open-source Android engineering skills in
`.agent/skills/` (see `.agent/skills/README.md` for provenance and licenses):

- `.agent/skills/google/` — Google `android/skills` (Apache-2.0): `jetpack-compose-performance`,
  `adaptive`, `edge-to-edge`, `navigation-3`, `navigation-event`, `glance-*`, `android-cli`,
  `android-profiler`, `anr-audit-and-fix`, `r8-analyzer`, `agp-9-upgrade`,
  `android-intent-security`, `android-permissions-security`, `testing-setup`, `play-policy-insights`.
- `.agent/skills/agent-skills-android/` — `GuillemRoca/agent-skills-android` (MIT):
  `android-ui-engineering`, `android-architecture`, `android-accessibility`,
  `android-background-work`, `android-data-persistence`, `android-device-testing`,
  `android-e2e-verification` plus the process skills (`spec-driven-development`,
  `test-driven-development`, `code-review-and-quality`, `debugging-and-error-recovery`,
  `git-workflow-and-versioning`, `shipping-and-launch`, ...).

**MANDATORY:** AndCode does not auto-discover `.agent/skills/`. Before any Android
engineering task, explicitly read the relevant `SKILL.md` file(s) with the Read tool
(e.g. `Read .agent/skills/google/jetpack-compose-performance/SKILL.md`) and follow them.
Read only the skill(s) the task actually needs — never bulk-load the whole set.
Keep each skill's `SKILL.md` and its `LICENSE`/`LICENSE.txt` unmodified when copying or
re-installing it.


## Anti-slop quality gate (installed from miqdadbadjuber/anti-slop)

For UI, copy, accessibility, mobile-layout, or code-comment cleanup tasks, use the relevant project-local skills in `.agents/skills/`. The core rules live at `.agents/skills/antislop/antislop.md`; the core skill is `.agents/skills/antislop/SKILL.md`.

- UI / Compose / web visual work: read `.agents/skills/antislop/SKILL.md` and `.agents/skills/antislop-ui/SKILL.md`.
- Product copy: add `.agents/skills/antislop-copywriting/SKILL.md`.
- Accessibility and interaction states: add `.agents/skills/antislop-human/SKILL.md`.
- Responsive/mobile layout: add `.agents/skills/antislop-layoutmobile/SKILL.md`.
- Comment-only cleanup: use `.agents/skills/antislop-code/SKILL.md` and do not change executable code under that skill.

`DESIGN.md` is the approved visual authority. Anti-slop removes generic patterns; it must not invent an unrelated brand identity or override the currently approved Campus Spectrum system. Preserve product behavior, navigation, accessibility, localization, and offline-first constraints. Audit first, make focused changes, and report findings plus verification honestly.
