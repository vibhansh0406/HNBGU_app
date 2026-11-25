---
name: CampusOne
description: A student-first campus super-app using the Evergreen Studio design system
colors:
  brand-evergreen: "#1E634B"
  on-brand-evergreen: "#FFFFFF"
  brand-evergreen-container: "#E4F0E7"
  on-brand-evergreen-container: "#163D2E"
  secondary-teal: "#476C78"
  on-secondary-teal: "#FFFFFF"
  secondary-container: "#E3EEF0"
  tertiary-amber: "#B97937"
  on-tertiary-amber: "#3B2615"
  tertiary-container: "#F5E8D4"
  background: "#F6F7F2"
  on-background: "#17231F"
  surface: "#FFFFFF"
  on-surface: "#17231F"
  surface-variant: "#EAF0E9"
  on-surface-variant: "#53665A"
  outline: "#CED7CE"
  outline-variant: "#E2E8E1"
  info: "#367F8D"
  info-container: "#DDEEF0"
  success: "#286B4E"
  success-container: "#DCECE1"
  warning: "#9C641F"
  warning-container: "#F5EAD5"
  danger: "#B42318"
  danger-container: "#FBE5E2"
  dark-background: "#0F1613"
  dark-surface: "#17251C"
  dark-card: "#1E3024"
  dark-selected: "#294633"
  dark-primary-highlight: "#A9D6BC"
  dark-secondary-highlight: "#B4D3D9"
  dark-tertiary-highlight: "#E7BD8A"
  dark-primary-action: "#1E634B"
  dark-text-primary: "#F6F7F2"
  dark-text-secondary: "#B8C9BF"
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
rounded: { xs: "8dp", sm: "12dp", md: "16dp", lg: "20dp", xl: "24dp", pill: "50%" }
spacing: { xs: "4dp", sm: "8dp", md: "12dp", lg: "16dp", xl: "20dp", xxl: "24dp", xxxl: "32dp" }
---

# CampusOne — Evergreen Studio

## Product character
CampusOne is a practical student super-app: quick to scan between classes, reliable on mid-range Android devices, and useful even when connectivity is poor. **Evergreen Studio** uses deep evergreen as the primary identity, sage as its dark-theme highlight, muted teal for supporting information, and warm amber for tertiary emphasis. The interface should feel calm, clear and dependable—not like a generic blue dashboard.

## Colour system
| Role | Light | Dark | Intended use |
|---|---|---|---|
| Primary action / evergreen | `#1E634B` | `#1E634B` | Main actions, launcher identity and key brand emphasis |
| Primary container | `#E4F0E7` | `#294633` | Selected surfaces, chips and low-emphasis brand fills |
| Secondary / muted teal | `#476C78` | `#476C78` | Supporting actions and secondary emphasis |
| Tertiary / warm amber | `#B97937` | `#B97937` | Important emphasis, deadlines and featured moments |
| Info | `#367F8D` | `#91B2BA` | Informational status and neutral updates |
| Success | `#286B4E` | `#9FC8AA` | Attendance health and completed actions |
| Warning | `#9C641F` | `#D49A57` | Pending or attention-needed states |
| Danger | `#B42318` | `#FF8A7A` | Errors and destructive actions |
| Background | `#F6F7F2` | `#0F1613` | App canvas |
| Surface | `#FFFFFF` | `#17251C` | Cards, sheets and forms |
| Card / surface variant | `#EAF0E9` | `#1E3024` | Grouped content and low-emphasis controls |
| Selected surface | `#E4F0E7` | `#294633` | Selected navigation and active controls |
| Text / on-surface | `#17231F` | `#F6F7F2` | Primary reading content |
| Secondary text | `#53665A` | `#B8C9BF` | Supporting copy and metadata |

The dark theme uses a neutral green-black canvas with restrained green-charcoal surfaces. Evergreen remains the logo and primary-action fill. The lighter Evergreen/Sage, Teal and Ochre tones are contrast-safe foreground highlights for links, icons and labels only. Do not turn every surface green or substitute sage for the core brand green.

Do not use brand accents as arbitrary decoration. Keep most surfaces neutral or green-charcoal. Muted teal and amber are semantic/supporting tones, not competing brand identities. Use `MaterialTheme.colorScheme` and `extendedColors`; do not introduce screen-local hex values for theme roles.

## Logo and brand lockup
- The official app mark is the supplied traced C shape; preserve its geometry and proportions.
- The launcher icon uses a solid Evergreen background (`#1E634B`) with the original C mark in white (`#FFFFFF`).
- Use the same white-on-Evergreen tile for the splash, onboarding brand row and in-app header.
- In-app logo tiles remain fixed to the core brand colours in both themes; surrounding surfaces and text adapt to light/dark mode.
- Do not recolour the launcher mark blue, place it on a white tile, stretch it, add gradients or apply shadows.
- Android adaptive icons must keep the C mark as a safe-zone foreground and the Evergreen field as a separate background resource. The monochrome layer must preserve the same C silhouette.

## Contrast
Use each semantic foreground/background pair from the theme rather than choosing text colours by eye. Keep primary button text white on Evergreen in both themes. In dark mode use the lighter semantic highlight colours for small foreground text/icons, and warm ivory/muted grey for reading content. Do not place small white text on the amber tertiary tone. Verify actual text and icon contrast whenever a new surface or badge is introduced.

## Typography
- **Manrope** is the display voice for screen titles, section headings and prominent metrics.
- **Inter** is the reading voice for body text, buttons, navigation, labels and forms.
- **Noto Sans Devanagari** is the bundled Hindi fallback.
- Keep body copy at 14–16sp where possible; supporting copy may use 13sp. Do not shrink text to solve layout problems.

## Shape, spacing and depth
- Use the 4dp spacing rhythm: 4 / 8 / 12 / 16 / 20 / 24 / 32dp.
- Page margins are 16dp on compact phones and scale with the existing window-size classes.
- Use the existing C1 radii: 8dp / 12dp / 16dp / 20dp / 24dp; pills remain fully rounded.
- Prefer tonal surface steps and 1dp borders over shadows. Elevation belongs to floating controls, menus and dialogs only.
- Interactive targets should be at least 48dp high and have clear spacing.

## Screen inventory (audited repository destinations)
- **Entry and identity:** silent intro, Google sign-in, student onboarding, profile.
- **Primary tabs:** Home, Attendance, Timetable and Notices; More is the feature library.
- **Home:** student greeting/profile, next class, attendance summary, quick actions, announcements and upcoming assignments/exam at-a-glance.
- **Study:** Notes & PYQ, Assignments, Exams & Syllabus, GPA Calculator.
- **Campus:** Community, Notifications, Events & Clubs, Lost & Found, Campus Directory.
- **Account:** Profile, Settings and Privacy Policy.
- **Administration:** Admin Hub, moderators, subjects, timetable, notices, assignments, exams, clubs, events, lost-and-found and directory management.
- **Super Admin:** users, roles, content, moderation, settings and audit.

No existing destination was removed. The role-gated administration branches remain protected by the existing role/permission checks.

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
- Use native Compose animation APIs and the shared `C1Motion` durations/easing. Feature tiles use small semantic-tint icon wells; avoid decorative motion that competes with information.
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
