# Android engineering skills (`.agent/skills/`)

Curated, verbatim copies of open-source Android engineering skills for CampusOne.
These files are **not** auto-discovered by AndCode. Load them explicitly (see AGENTS.md).

## Provenance and licenses

| Directory | Upstream repository | Upstream license | Local license copy |
|---|---|---|---|
| `google/` | https://github.com/android/skills (Google, official) | Apache License 2.0 (`LICENSE.txt`) | `google/<skill>/LICENSE.txt` |
| `agent-skills-android/` | https://github.com/GuillemRoca/agent-skills-android | MIT License | `agent-skills-android/<skill>/LICENSE` |

Every installed skill directory is a byte-identical copy of the upstream skill
directory (SKILL.md + any `references/`, `scripts/`, `assets/` files). Only
`LICENSE` / `LICENSE.txt` was added inside each skill folder so the license
travels with the skill. Verified with `diff` against a fresh clone of both
upstream repositories.

`.opencode/skills/` holds the same upstream content (flattened) for AndCode's
own skill discovery. Both trees are intentionally kept: `.opencode/skills/` is
the agent-discoverable copy, `.agent/skills/` is the explicit, license-carrying
copy requested for this setup.

## Installed skills — `google/` (Apache-2.0)

Compose / UI:
- `jetpack-compose-performance` — recomposition, stability, lazy layouts, graphics perf
- `adaptive` — adaptive layouts for large screens, foldables, window size classes
- `edge-to-edge` — insets, system bars, API 35 enforcement

Navigation:
- `navigation-3` — Navigation 3 (type-safe routes) for future navigation rework
- `navigation-event` — navigation events, deep links, back stack semantics

Platform:
- `glance-appwidget-updates-and-memory` — Glance widget (CampusOne ships a widget)
- `glance-canonical-layouts` — canonical Glance layouts
- `android-cli` — adb-driven device/app inspection
- `android-profiler` — profiling, memory, startup

Performance / build:
- `anr-audit-and-fix` — ANR diagnosis and fixes
- `r8-analyzer` — R8 / shrinker analysis
- `agp-9-upgrade` — AGP 9 migration path (relevant to the Liquid Glass evaluation)

Security / testing / release:
- `android-intent-security` — intent, deep link, exported component safety
- `android-permissions-security` — permission model, scoped storage, runtime perms
- `testing-setup` — unit/UI/instrumentation test scaffolding
- `play-policy-insights` — Play policy and release readiness

## Installed skills — `agent-skills-android/` (MIT)

- `android-ui-engineering` — Compose component design, state hoisting, theming, previews
- `android-architecture` — module/layering/DI structure guidance
- `android-accessibility` — a11y semantics, touch targets, contrast
- `android-background-work` — WorkManager (CampusOne uses it for reminders)
- `android-data-persistence` — Room/DataStore (CampusOne uses both)
- `android-device-testing` — on-device test practice
- `android-e2e-verification` — end-to-end verification flows
- `spec-driven-development`, `test-driven-development`, `code-review-and-quality`,
  `debugging-and-error-recovery`, `git-workflow-and-versioning`, `shipping-and-launch`,
  `security-and-hardening`, `performance-optimization`, `deprecation-and-migration`,
  `documentation-and-adrs`, `ci-cd-and-automation`, `api-and-interface-design`,
  `code-simplification` — process skills used throughout the mission workflow

## Deliberately excluded

Not applicable to CampusOne, so not installed: `camera/`, `cars/`, `device-ai/`,
`media/` (cast), `tv/`, `wear/`, `xr/`, `ink/`, `identity/`, `play-billing`,
`migrate-xml-views-to-jetpack-compose` (CampusOne has no XML layouts).
