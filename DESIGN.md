---
name: CampusOne
description: A student-first campus super-app with the Campus Spectrum design system
colors:
  brand-blue: "#3156D9"
  on-brand-blue: "#FFFFFF"
  brand-blue-container: "#E5EAFE"
  on-brand-blue-container: "#172B70"
  brand-violet: "#6849C8"
  on-brand-violet: "#FFFFFF"
  brand-violet-container: "#EEE9FF"
  on-brand-violet-container: "#2D1B63"
  brand-coral: "#D65F4B"
  on-brand-coral: "#3A1712"
  brand-coral-container: "#FFE7E1"
  on-brand-coral-container: "#5A2118"
  background: "#F6F7FB"
  on-background: "#1B2030"
  surface: "#FFFFFF"
  surface-variant: "#ECEEF5"
  on-surface-variant: "#596176"
  outline: "#C8CDDA"
  outline-variant: "#E3E6EF"
  info: "#087EAC"
  info-container: "#D8F1FA"
  success: "#2E7D5B"
  success-container: "#DDEBE2"
  warning: "#B45309"
  warning-container: "#F7ECD9"
  danger: "#B42318"
  danger-container: "#FBE5E2"
  dark-background: "#11131D"
  dark-surface: "#191D2B"
  dark-surface-variant: "#242A3A"
  dark-primary: "#A9B9FF"
  dark-secondary: "#C9B8FF"
  dark-tertiary: "#FFB5A5"
typography:
  families:
    display: "Manrope variable (bundled, OFL)"
    text: "Inter variable (bundled, OFL)"
    hindi-fallback: "Noto Sans Devanagari (bundled, OFL)"
  display-large: { fontFamily: "Manrope", fontSize: "40sp", fontWeight: 700, lineHeight: "46sp", letterSpacing: "-1sp" }
  display-medium: { fontFamily: "Manrope", fontSize: "34sp", fontWeight: 700, lineHeight: "40sp", letterSpacing: "-0.8sp" }
  headline-large: { fontFamily: "Manrope", fontSize: "26sp", fontWeight: 700, lineHeight: "32sp" }
  headline-medium: { fontFamily: "Manrope", fontSize: "22sp", fontWeight: 600, lineHeight: "28sp" }
  title-large: { fontFamily: "Manrope", fontSize: "18sp", fontWeight: 600, lineHeight: "24sp" }
  title-medium: { fontFamily: "Inter", fontSize: "16sp", fontWeight: 600, lineHeight: "22sp" }
  body-large: { fontFamily: "Inter", fontSize: "16sp", fontWeight: 400, lineHeight: "24sp" }
  body-medium: { fontFamily: "Inter", fontSize: "14sp", fontWeight: 400, lineHeight: "21sp" }
  body-small: { fontFamily: "Inter", fontSize: "13sp", fontWeight: 400, lineHeight: "19sp" }
rounded: { xs: "10dp", sm: "14dp", md: "18dp", lg: "24dp", xl: "30dp", pill: "50%" }
spacing: { xs: "4dp", sm: "8dp", md: "12dp", lg: "16dp", xl: "20dp", xxl: "24dp", xxxl: "32dp" }
---

# CampusOne — Campus Spectrum

## Product character
CampusOne is a practical student super-app: quick to scan between classes, reliable on mid-range Android devices, and useful even when connectivity is poor. **Campus Spectrum** brings a confident royal-blue identity, a violet secondary voice, coral emphasis, cyan information cues and warm-neutral surfaces. The palette is energetic without turning every tile into a different colour.

## Colour system
| Role | Light | Dark | Intended use |
|---|---|---|---|
| Primary / royal blue | `#3156D9` | `#A9B9FF` | Main actions, selected navigation, key progress |
| Secondary / violet | `#6849C8` | `#C9B8FF` | Supporting actions, resource and community accents |
| Tertiary / coral | `#D65F4B` | `#FFB5A5` | Important emphasis, deadlines, featured moments |
| Info / cyan | `#087EAC` | `#8DDCF1` | Informational status and neutral updates |
| Success | `#2E7D5B` | `#8CC7A6` | Attendance health and completed actions |
| Warning | `#B45309` | `#E5B36B` | Pending or attention-needed states |
| Danger | `#B42318` | `#FF8A7A` | Errors and destructive actions |
| Background | `#F6F7FB` | `#11131D` | App canvas |
| Surface | `#FFFFFF` | `#191D2B` | Cards, sheets and forms |
| Surface variant | `#ECEEF5` | `#242A3A` | Grouped content and low-emphasis controls |

Do not use brand accents as arbitrary decoration. A screen may use blue as its anchor, violet for a related feature, coral for a single callout and cyan for informational status. Most surfaces remain neutral. Gradients are reserved for a small number of high-value brand moments such as a welcome banner or sign-in hero; never apply a gradient to every card.

All screen code consumes `MaterialTheme.colorScheme` and `extendedColors`; do not introduce screen-local hex values for theme roles. The semantic green remains available for success, but green is no longer the brand colour.

## Contrast
The palette is designed for WCAG AA text contrast. White text on primary blue is approximately 6.06:1; white on violet is approximately 6.22:1. Coral uses a dark foreground (approximately 4.61:1 with the specified deep coral foreground). The default body text and neutral backgrounds exceed 12:1. Always verify actual foreground/background pairs when adding a new component. Do not place white small text directly on the coral brand tone.

## Typography
- **Manrope** is the display voice for screen titles, section headings and prominent metrics.
- **Inter** is the reading voice for body text, buttons, navigation, labels and forms.
- **Noto Sans Devanagari** is the bundled Hindi fallback.
- Keep body copy at 14–16sp where possible; supporting copy may use 13sp. Do not shrink text to solve layout problems.

## Shape, spacing and depth
- Use the 4dp spacing rhythm: 4 / 8 / 12 / 16 / 20 / 24 / 32dp.
- Page margins are 16dp on compact phones and scale with the existing window-size classes.
- Inputs and compact chips use 10–14dp radii; cards use 18–24dp; sheets use 24–30dp.
- Prefer tonal surface steps and 1dp borders over shadows. Elevation belongs to floating controls, menus and dialogs only.
- Interactive targets should be at least 48dp high and have clear spacing.

## Information architecture
Primary bottom navigation remains **Home / Attendance / Timetable / Notices / More**. This keeps the four highest-frequency academic destinations one tap away and avoids a risky rewrite of the existing state-based navigation.

**More** is the organised feature library:
- **Study:** Notes & PYQ, Assignments, Exams & Syllabus, GPA Calculator.
- **Campus:** Community, Events & Clubs, Lost & Found, Campus Directory.
- **Account:** Profile, Settings, Privacy Policy, Sign out.
- **Administration:** admin and super-admin destinations remain visible only to authorised roles. Never expose privileged controls to students.

Home prioritises the next actionable academic item, then attendance and today's schedule, then notices and shortcuts. Avoid a long stack of equally weighted cards; use section hierarchy and compact summaries.

## Settings behaviour
- Theme selection (System / Light / Dark) is persisted in Preferences DataStore and immediately updates the app theme.
- Language (English / Hindi) uses the existing AppCompat locale mechanism and persists through DataStore.
- Notification master and category switches are persisted and enforced in the FCM message handler.
- Local class, assignment, exam and event reminder switches are persisted and enforced by the periodic reminder worker.
- Account details are read from the authenticated profile; sign-out uses the existing auth flow.
- Privacy Policy opens the existing in-app policy screen. Android's OS notification permission remains controlled by Android.

A setting is not a setting unless it changes app behaviour and survives process restarts. Do not add decorative controls for unsupported features.

## Motion
- Use native Compose animation APIs and the shared `C1Motion` durations/easing.
- Press feedback is subtle (about 0.97 scale plus ripple, 120–160ms).
- Navigation uses a restrained fade and short horizontal movement; preserve state across tab switches.
- Animate progress only when the value changes; do not continuously animate idle dashboards.
- Prefer skeletons when content geometry is known. Avoid pulsing, bouncing or shimmer that adds no information.
- Respect system animator/reduced-motion settings where practical.

## Accessibility and responsiveness
- Use semantic headings and meaningful content descriptions for non-decorative icons.
- Maintain AA contrast, scalable typography, keyboard focus and clear error messages.
- Compact phones use one column; medium windows may use two columns; larger windows may use three only when content supports it.
- No horizontal overflow, clipped controls or bottom-navigation overlap. Test narrow widths and font scaling.

## Engineering boundaries
Preserve package `in.campusone.app`, Supabase auth and RLS, role enforcement, existing repositories/ViewModels, FCM, WorkManager, Room and storage contracts. Keep privileged actions server-authorised. Do not add mock production data or unnecessary dependencies. The release certificate is fixed at `50:34:1B:DA:AE:1E:1C:FC:C7:E2:68:93:D3:CF:8A:1F:7A:FC:35:A2`; the release pipeline and secrets are immutable.
