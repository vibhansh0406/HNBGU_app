# Security Policy

CampusOne handles account information and student-facing campus data. Please report security concerns responsibly.

## Reporting a vulnerability

Do not publish an exploitable vulnerability, private student information, credentials, access tokens, or database records in a public issue.

Contact the repository maintainer privately through GitHub. Include:

- A concise description of the affected area.
- The steps needed to reproduce the issue.
- The potential impact.
- Any relevant version, commit, or environment details.

Do not include real user data or live secrets in the report.

## Security expectations

- Supabase Row Level Security must protect private records.
- Privileged operations must be authorized on the server.
- Service-role keys and release signing credentials must never be placed in the Android client or committed to Git.
- Client configuration values must not be treated as authorization controls.
- Changes to authentication, roles, storage, or database policies require focused review and verification.
