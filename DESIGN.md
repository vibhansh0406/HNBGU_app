---
name: CampusOne
description: Offline-first companion for HNBGU campus life — Forest & Ivory design system
colors:
  forest-primary: "#183D32"
  forest-on-primary: "#FFFFFF"
  forest-primary-container: "#DCE7DF"
  forest-on-primary-container: "#102D25"
  moss-secondary: "#315C4B"
  moss-secondary-container: "#E2EAE4"
  oak-tertiary: "#7A5C50"
  oak-tertiary-container: "#F0E4DE"
  ivory-bg: "#F7F6F0"
  ivory-on-bg: "#202923"
  paper-surface: "#FFFFFF"
  paper-surface-variant: "#EFEEE6"
  surface-container-low: "#F2F1E9"
  surface-container: "#EDECE3"
  surface-container-high: "#E7E6DC"
  surface-container-highest: "#E1E0D5"
  outline-hairline: "#E6E6DC"
  outline-variant: "#D8D8CC"
  text-muted: "#5E665E"
  success: "#2E7D5B"
  success-container: "#DDEBE2"
  warning: "#B45309"
  warning-container: "#F7ECD9"
  danger: "#B42318"
  danger-container: "#FBE5E2"
  info: "#2C6E8A"
  info-container: "#DCEAF0"
  night-bg: "#101B17"
  night-surface: "#182720"
  night-primary: "#83A995"
  night-on-bg: "#F1F2EA"
  night-success: "#8CC7A6"
  night-warning: "#E5B36B"
  night-danger: "#FF8A7A"
  night-info: "#8FC3D9"
typography:
  headline-large:
    fontFamily: "System Sans (Roboto)"
    fontSize: "32sp"
    fontWeight: 700
    lineHeight: "38sp"
    letterSpacing: "-0.5sp"
  headline-medium:
    fontFamily: "System Sans (Roboto)"
    fontSize: "26sp"
    fontWeight: 700
    lineHeight: "32sp"
    letterSpacing: "-0.4sp"
  headline-small:
    fontFamily: "System Sans (Roboto)"
    fontSize: "20sp"
    fontWeight: 600
    lineHeight: "26sp"
    letterSpacing: "-0.2sp"
  title-large:
    fontFamily: "System Sans (Roboto)"
    fontSize: "19sp"
    fontWeight: 600
    lineHeight: "26sp"
    letterSpacing: "-0.2sp"
  title-medium:
    fontFamily: "System Sans (Roboto)"
    fontSize: "16sp"
    fontWeight: 600
    lineHeight: "22sp"
    letterSpacing: "-0.1sp"
  body-large:
    fontFamily: "System Sans (Roboto)"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "24sp"
    letterSpacing: "-0.1sp"
  body-medium:
    fontFamily: "System Sans (Roboto)"
    fontSize: "14sp"
    fontWeight: 400
    lineHeight: "20sp"
    letterSpacing: "-0.05sp"
  label-large:
    fontFamily: "System Sans (Roboto)"
    fontSize: "14sp"
    fontWeight: 600
    lineHeight: "20sp"
  label-medium:
    fontFamily: "System Sans (Roboto)"
    fontSize: "12sp"
    fontWeight: 500
    lineHeight: "16sp"
  label-small:
    fontFamily: "System Sans (Roboto)"
    fontSize: "11sp"
    fontWeight: 500
    lineHeight: "14sp"
    letterSpacing: "0.2sp"
rounded:
  xs: "10dp"
  sm: "14dp"
  md: "18dp"
  lg: "24dp"
  xl: "30dp"
  pill: "50%"
spacing:
  xs: "4dp"
  sm: "8dp"
  md: "12dp"
  lg: "16dp"
  xl: "20dp"
  xxl: "24dp"
  xxxl: "32dp"
components:
  button-primary:
    backgroundColor: "{colors.forest-primary}"
    textColor: "{colors.forest-on-primary}"
    typography: label-large
    rounded: "{rounded.sm}"
    padding: "12px 20px"
    height: "48dp"
  button-tonal:
    backgroundColor: "{colors.moss-secondary-container}"
    textColor: "{colors.forest-on-primary-container}"
    typography: label-large
    rounded: "{rounded.sm}"
    padding: "12px 20px"
    height: "48dp"
  button-outlined:
    backgroundColor: "transparent"
    textColor: "{colors.forest-primary}"
    rounded: "{rounded.sm}"
    height: "48dp"
  button-danger:
    backgroundColor: "{colors.danger-container}"
    textColor: "#4A0E08"
    rounded: "{rounded.sm}"
    height: "48dp"
  input-field:
    backgroundColor: "{colors.paper-surface}"
    textColor: "{colors.ivory-on-bg}"
    rounded: "{rounded.sm}"
    height: "56dp"
  status-chip:
    backgroundColor: "{colors.surface-container-high}"
    textColor: "{colors.text-muted}"
    typography: label-medium
    rounded: "{rounded.pill}"
    padding: "4px 12px"
  stat-tile:
    backgroundColor: "{colors.surface-container-low}"
    textColor: "{colors.forest-primary}"
    rounded: "{rounded.md}"
    padding: "12px 16px"
  banner-info:
    backgroundColor: "{colors.info-container}"
    textColor: "#0E2E3B"
    rounded: "{rounded.sm}"
    padding: "12px"
---

# CampusOne — Forest & Ivory Design System

## Overview

CampusOne is an **Operate**-mode product: students scan it in seconds between classes, often offline, often one-handed. The design language — **Forest & Ivory** — borrows its authority from deep forest green and its calm from warm ivory paper. It should feel like a well-printed university prospectus that learned to be an app: confident typography, flat surfaces separated by tone and hairlines, and color used only where it means something (attendance health, status, actions).

Three rules govern every screen:

1. **Flat-first.** Hierarchy comes from surface tone (`surfaceContainerLow` → `Highest`) and 1dp hairlines — never from drop shadows. Elevation is reserved for truly floating elements (dialogs, FABs).
2. **Color is information.** Forest green = brand and primary action. Success/warning/danger/info appear only as status. If a color carries no meaning, it is `onSurfaceVariant`.
3. **Type does the talking.** Heavy, tight-tracked titles; calm body; no all-caps shouting except tiny labels.

## Colors

Light mode is the canonical world: ivory paper (`#F7F6F0` background), white cards, deep forest (`#183D32`) for primary actions and section titles. Dark mode is a true forest-at-night translation (`#101B17` background), not an inverted filter — primary becomes a soft sage (`#83A995`) so contrast stays AA on dark surfaces.

Semantic pairs (theme-aware via `LocalExtendedColors`):

| Token | Light | Dark | Use |
|---|---|---|---|
| success | `#2E7D5B` / container `#DDEBE2` | `#8CC7A6` / `#1E3B2E` | attendance safe, approved |
| warning | `#B45309` / `#F7ECD9` | `#E5B36B` / `#3F2E14` | on-edge attendance, pending |
| danger | `#B42318` / `#FBE5E2` | `#FF8A7A` / `#4A1510` | critical, destructive, errors |
| info | `#2C6E8A` / `#DCEAF0` | `#8FC3D9` / `#1C3A47` | neutral announcements |
| hairline | `#E6E6DC` | `#2B3C32` | dividers, input borders |

Never hardcode these hexes in screens — read `MaterialTheme.colorScheme` and `extendedColors` so light/dark stay in sync.

## Typography

System sans (Roboto), SF-inspired scale: bold tight-tracked headlines, semibold titles, normal body, medium labels. Display sizes exist for the home greeting and hero numbers only. Body text never drops below 14sp; metadata uses `labelMedium` (12sp). Letter-spacing tightens as size grows (-0.5sp at 32sp → 0 at 12sp).

## Layout

- 4dp-based spacing scale; screen margins are `lg` (16dp), card padding `lg`, related-element gaps `sm`–`md`, section gaps `xl`–`xxl`.
- Content max width is the phone edge-to-edge; no centered narrow columns.
- Lists and dashboards read top-down: greeting → primary insight (attendance/next class) → sections.
- Minimum touch target: 48dp (`C1TouchTarget`).

## Elevation & Depth

| Level | Value | Where |
|---|---|---|
| Flat | 0dp | Cards, tiles, banners, chips — separated by surface tone |
| Raised | 1dp | Pressed cards, sticky headers |
| Floating | 4dp | Dialogs, menus, FAB |

Shadows never substitute for borders; if a surface needs definition on the same tone as its parent, use `outlineVariant` hairline or step one `surfaceContainer` level.

## Shapes

Radius scale: 10dp (inputs' inner elements, chips' containers), 14dp (buttons, text fields, banners), 18dp (cards, tiles), 24dp (sheets, large cards), 30dp (bottom sheets), full pill (status chips). Radius grows with surface size — never mix a 30dp card with a 10dp button inside.

## Components

Shared primitives live in `ui/components/CoreComponents.kt` and are the **only** sanctioned way to build these patterns:

- **C1Button** — intents: `PRIMARY` (filled forest), `TONAL` (moss container), `OUTLINED` (hairline border), `DANGER` (error container), `GHOST` (text-only). One primitive, no ad-hoc `Button` styling in screens.
- **C1TextField** — outlined input, 14dp radius, theme-aware borders, built-in error/supporting text.
- **C1StatusChip** — pill with semantic tone (`SUCCESS/WARNING/DANGER/INFO/NEUTRAL`).
- **C1Banner** — inline informational strip with optional icon.
- **C1SectionHeader** — bold forest title with optional trailing action.
- **C1StatTile** — label/value row on `surfaceContainerLow`.
- **C1EmptyState** — tonal icon circle + title + message.
- **AnimatedAttendanceRing** — the attendance hero component; colors come from `extendedColors`.

Icons: Material filled set, 24dp standard / 18dp inline / 26dp in empty-state circles, tinted `onSurfaceVariant` unless they carry status meaning.

## Do's and Don'ts

**Do**
- Use surface-tone steps (`surfaceContainerLow` → `Highest`) for grouping.
- Keep dark mode a first-class translation — verify every new component in both modes.
- Use semantic tones through `C1StatusChip`/`C1Banner` so light/dark pairs stay contrast-safe.
- Reserve `headlineLarge/Medium` for one hero element per screen.

**Don't**
- No drop shadows on cards, no gradients, no glassmorphism, no neon accents.
- No generic dashboard styling: no rows of identical gradient stat cards, no decorative illustrations behind content.
- No hardcoded hex colors in feature screens — tokens only.
- No all-caps body text; caps are for `labelSmall` metadata only.
- No radius mixing that breaks the size-to-radius relationship.
