# CampusOne Storage Worker setup

The Android app's SupabaseFileStore now supports the CampusOne Storage Worker.

## Enable the Worker transport
After the Worker is deployed, add this non-secret URL to CampusOne/local.properties:

storage.api.url=https://YOUR-WORKER.workers.dev

Rebuild the app. When the value is non-empty, new study-resource uploads use the Worker. Existing Supabase Storage URLs remain downloadable through the legacy path. If the value is blank, the app continues using the existing Supabase Storage adapter.

## Security model
- Android sends the current Supabase access token to the Worker.
- Worker verifies the token with Supabase Auth; it never trusts a user ID supplied by the app.
- GitHub token and Supabase service-role key exist only as Cloudflare Worker secrets.
- Do not add either secret to local.properties, BuildConfig, Android assets, or GitHub source files.
- Study-resource objects are served only to authenticated users while the linked study_resources row is visible, unflagged and completed/published.
- The Worker writes the object UUID into study_resources.storage_object_id during the app's metadata sync.

## Current deployment prerequisites
The Worker code is in the separate private repository vibhansh0406/campusone-storage-infrastructure. It is not deployed yet. Create a dedicated private GitHub repository for binary objects, configure Cloudflare Worker variables/secrets, deploy, then set storage.api.url above.

GitHub is only a constrained pilot adapter. Move to R2 before production-scale usage.
