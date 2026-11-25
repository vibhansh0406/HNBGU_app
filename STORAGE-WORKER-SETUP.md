# CampusOne Storage Worker setup

The Android app supports an optional CampusOne Storage Worker for study-resource uploads.

## Enable the Worker

After deployment, add the Worker URL to `CampusOne/local.properties`:

```properties
storage.api.url=https://YOUR-WORKER.workers.dev
```

Rebuild the app. When the value is present, new study-resource uploads use the Worker. Existing Supabase Storage URLs remain downloadable through the legacy path. When the value is blank, the app continues using the Supabase Storage adapter.

## Security model

- Android sends the current Supabase access token to the Worker.
- The Worker verifies the token with Supabase Auth and does not trust a user ID supplied by the app.
- The GitHub token and Supabase service-role key exist only as Cloudflare Worker secrets.
- Do not add either secret to local properties, BuildConfig, Android assets, or GitHub source files.
- Study-resource objects are served only to authenticated users while the related study resource record is visible, unflagged, and published.
- The Worker stores the object ID on the related study resource record during metadata sync.

## Deployment status

The Worker code is maintained in a separate private repository. It is not deployed yet. Configure the Cloudflare Worker variables and secrets, deploy the Worker, then set its URL in the Android app.

GitHub is only a constrained pilot adapter. Move to R2 before production-scale usage.
