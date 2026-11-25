# Contributing to CampusOne

Thank you for considering a contribution.

CampusOne is an independent student project. Contributions should improve a real student workflow, preserve existing behaviour, and remain consistent with the product and design decisions in this repository.

## Before changing code

- Open an issue before starting a large feature or architectural change.
- Keep pull requests focused. Avoid mixing unrelated refactors with feature work.
- Check [PRODUCT.md](PRODUCT.md) for product scope and [DESIGN.md](DESIGN.md) for interface rules.
- Do not add mock data or claim a feature works unless the relevant flow has been checked.

## Development environment

Use Android Studio, JDK 17, Android SDK 35, and the Gradle Wrapper included in the project.

From the `CampusOne` directory:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Release builds require the private release signing configuration. Contributors should not need access to the release keystore.

## Engineering requirements

- Preserve the application ID `in.campusone.app`.
- Do not change the permanent release signing identity.
- Keep credentials, local configuration, keystores, and tokens out of commits.
- Enforce access to private data and privileged actions through Supabase Row Level Security and server-side checks.
- When changing database structure, add an incremental migration and keep the schema reference aligned.
- Preserve offline behaviour for core academic data.
- Keep UI changes consistent with the existing Compose design system and bilingual interface.
- Add or update tests when behaviour changes.

## Pull request checklist

- [ ] The change solves a specific, described problem.
- [ ] Existing flows and role restrictions remain intact.
- [ ] No secrets or private student information are included.
- [ ] Database changes include a migration where required.
- [ ] Build and relevant tests have been run.
- [ ] Any unverified behaviour or device-specific limitation is stated clearly.

A passing build is necessary, but it is not a substitute for testing the changed flow on a device.
