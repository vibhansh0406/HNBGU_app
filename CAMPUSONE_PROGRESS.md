# CAMPUSONE_PROGRESS.md — CRASH INVESTIGATION

## CRASH-ON-LAUNCH BUG — ROOT CAUSE IDENTIFIED

### Root Cause
The VectorDrawable `ic_campusone_c_mark.xml` (and `ic_launcher_foreground.xml`) had **spaces after path commands** in the `android:pathData` attribute:
- My format: `M 327.73,8.50 L 305.32,19.83` (space after `M`, `L`)
- Correct Android format: `M327.73,8.50 L305.32,19.83` (no space after command)

Android's `PathParser` (used by VectorDrawable inflation) is strict about path data format. Spaces after path commands cause a `RuntimeException`/`XmlPullParserException` during drawable inflation.

### Why it crashes on launch
This VectorDrawable is referenced by `windowBackground` in the theme:
- `res/values/themes.xml`: `android:windowBackground="@drawable/campusone_splash_background"`
- `campusone_splash_background.xml`: `<bitmap android:src="@drawable/ic_campusone_c_mark" />`

The `windowBackground` drawable is inflated during `Activity` creation (before `setContentView()`), before any UI is shown. An invalid VectorDrawable causes a fatal exception immediately on app open — matching the "crash immediately when opened" symptom exactly.

### Files Fixed
1. `CampusOne/app/src/main/res/drawable/ic_campusone_c_mark.xml` — Fixed path data (removed spaces after commands)
2. `CampusOne/app/src/main/res/mipmap-anydpi-v26/ic_launcher_foreground.xml` — Fixed path data (removed spaces after commands)

### Files Referencing the Fixed Drawable (no changes needed, they reference by ID)
- `res/mipmap-anydpi-v26/ic_launcher.xml` — references `@drawable/ic_campusone_c_mark`
- `res/mipmap-anydpi-v26/ic_launcher_round.xml` — references `@drawable/ic_campusone_c_mark`
- `res/drawable/campusone_splash_background.xml` — references `@drawable/ic_campusone_c_mark`
- `MainActivity.kt` — `painterResource(id = R.drawable.ic_campusone_c_mark)`

### Verification Plan
- Push fix to `phase-3` branch → CI `assembleDebug` + `assembleRelease` must pass
- CI build passing confirms compilation, but runtime crash can only be verified on a device/emulator (not available in this environment)

## Phase status
- Phase 0 (Repository & Baseline Audit): COMPLETE
- Phase 1 (Data/Backend Truthfulness): COMPLETE
- Phase 2-3 (Design System / Buttons): EXISTING
- Phase 4 (Hub Redesign): PARTIAL
- Phase 5 (Notes/Learning): PARTIAL
- Phase 6-7 (Other Screens / Responsive): PARTIAL
- Phase 8 (Brand Assets/Icon/Silent Intro): COMPLETE (VectorDrawable path data FIXED)
- Phase 9 (Micro-interactions): PARTIAL
- Phase 10 (Testing/QA): IN PROGRESS (crash fix verification)
- Phase 11 (Final Delivery): PENDING

## Build status
- CI build on phase-3: Was GREEN before crash fix (commit `ef988d0`, run `44938140406`)
- CI build after crash fix: PENDING (about to push)

## Blockers
- Cannot run Gradle locally (JVM restriction per AGENTS.md)
- No device/emulator available for runtime crash verification
- CI workflow only triggers on `main` and `phase-3` branches

## Next steps
1. Push VectorDrawable fix to phase-3
2. Verify CI build passes
3. Report root cause and fix to user
4. Recommend user test on physical device/emulator
