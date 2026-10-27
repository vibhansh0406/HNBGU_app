import { createBrowserClient } from "@supabase/ssr";

export function isSupabaseEnvConfigured(): boolean {
  const url = process.env.NEXT_PUBLIC_SUPABASE_URL ?? "";
  const key = process.env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY ?? "";
  return (
    url.startsWith("https://") &&
    !url.includes("YOUR_PROJECT_REF") &&
    key.length > 20 &&
    !key.includes("YOUR_SUPABASE")
  );
}

export function createSupabaseBrowserClient() {
  const url =
    process.env.NEXT_PUBLIC_SUPABASE_URL || "https://placeholder-unconfigured.supabase.co";
  const key =
    process.env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY || "placeholder-publishable-key";
  return createBrowserClient(url, key);
}
