"use client";

import React, { useState } from "react";
import {
  createSupabaseBrowserClient,
  isSupabaseEnvConfigured,
} from "@/lib/supabase/client";

export default function LoginPage() {
  const [busy, setBusy] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const configured = isSupabaseEnvConfigured();

  async function handleGoogleLogin() {
    setBusy(true);
    setErrorMsg(null);
    try {
      const supabase = createSupabaseBrowserClient();
      const redirectTo = `${window.location.origin}/auth/callback?next=/dashboard`;
      const { error } = await supabase.auth.signInWithOAuth({
        provider: "google",
        options: { redirectTo },
      });
      if (error) {
        setErrorMsg(error.message);
        setBusy(false);
      }
    } catch (e) {
      setErrorMsg(e instanceof Error ? e.message : String(e));
      setBusy(false);
    }
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex items-center justify-center p-6">
      <div className="max-w-md w-full bg-slate-900 border border-slate-800 rounded-2xl p-8 space-y-6">
        <div>
          <span className="px-3 py-1 rounded-full text-xs font-semibold bg-sky-500/20 text-sky-300 border border-sky-500/30">
            HNB Garhwal University
          </span>
          <h1 className="mt-3 text-2xl font-bold">CampusOne Admin Portal</h1>
          <p className="mt-1 text-sm text-slate-400">
            Sign in with your Google account. Role and department permissions are verified
            server-side via Supabase Row Level Security.
          </p>
        </div>

        {!configured && (
          <div className="p-4 rounded-xl bg-amber-500/10 border border-amber-500/30 text-xs text-amber-300 space-y-1">
            <div className="font-bold">Missing Environment Variables</div>
            <div>
              Configure <code>NEXT_PUBLIC_SUPABASE_URL</code> and{" "}
              <code>NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY</code> in{" "}
              <code>/admin-portal/.env.local</code>.
            </div>
          </div>
        )}

        {errorMsg && (
          <div className="p-3.5 rounded-xl bg-red-500/10 border border-red-500/30 text-xs text-red-300">
            {errorMsg}
          </div>
        )}

        <button
          disabled={!configured || busy}
          onClick={handleGoogleLogin}
          className="w-full py-3 px-4 rounded-xl bg-sky-600 hover:bg-sky-500 disabled:opacity-50 font-semibold text-sm transition"
        >
          {busy ? "Redirecting to Google..." : "Sign in with Google"}
        </button>

        <div className="text-xs text-slate-500 leading-relaxed">
          Ordinary student accounts default to the <code>student</code> role and cannot access
          administrative modules. Only accounts promoted by a <code>super_admin</code> can
          manage campus data.
        </div>
      </div>
    </div>
  );
}
