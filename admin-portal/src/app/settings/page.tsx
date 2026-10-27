"use client";

import React, { useState } from "react";
import AdminLayoutShell from "@/components/AdminLayoutShell";
import {
  createSupabaseBrowserClient,
  isSupabaseEnvConfigured,
} from "@/lib/supabase/client";

export default function SettingsPage() {
  const [bootstrapEmail, setBootstrapEmail] = useState("");
  const [feedback, setFeedback] = useState<string | null>(null);
  const configured = isSupabaseEnvConfigured();

  async function handleFirstSuperAdminBootstrap(e: React.FormEvent) {
    e.preventDefault();
    setFeedback(null);
    const supabase = createSupabaseBrowserClient();
    const { error } = await supabase.rpc("bootstrap_first_super_admin", {
      expected_email: bootstrapEmail.trim(),
    });
    if (error) {
      setFeedback(`Bootstrap RPC result: ${error.message}`);
    } else {
      setFeedback("Success! Your account is now promoted to super_admin. Refresh the page.");
    }
  }

  return (
    <AdminLayoutShell
      title="Portal Configuration & Security Settings"
      requiredRoles={["super_admin", "department_admin", "notice_admin", "exam_admin"]}
    >
      {({ profile }) => (
        <div className="space-y-6">
          {feedback && (
            <div className="p-3.5 rounded-xl bg-slate-900 border border-sky-500/40 text-sm text-sky-300">
              {feedback}
            </div>
          )}

          <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 space-y-2">
            <h2 className="text-lg font-semibold">Environment &amp; Security Check</h2>
            <p className="text-sm text-slate-300">
              Supabase Environment Configured:{" "}
              <b className="text-emerald-400">{configured ? "YES" : "NO"}</b>
            </p>
            <p className="text-sm text-slate-300">
              Signed-in Account: <b>{profile.email}</b> • Role:{" "}
              <code className="text-sky-300">{profile.role}</code> • Department ID:{" "}
              <code>{profile.department_id ?? "All Departments"}</code>
            </p>
            <p className="text-xs text-slate-400">
              Security Guarantee: No <code>SUPABASE_SERVICE_ROLE_KEY</code> or Google OAuth client
              secret is bundled in this Next.js application. Privileged role mutations execute
              exclusively through the PostgreSQL <code>SECURITY DEFINER</code> RPC{" "}
              <code>admin_assign_user_role</code>, and push notifications execute through the{" "}
              <code>send-fcm-campaign</code> Supabase Edge Function.
            </p>
          </div>

          <form
            onSubmit={handleFirstSuperAdminBootstrap}
            className="p-6 rounded-2xl bg-slate-900 border border-slate-800 space-y-3"
          >
            <h2 className="text-lg font-semibold">
              One-Time First Super Admin Bootstrap (Locked once a Super Admin exists)
            </h2>
            <p className="text-xs text-slate-400">
              This RPC only succeeds if zero <code>super_admin</code> rows exist in{" "}
              <code>public.profiles</code> and your signed-in email matches the confirmation email.
            </p>
            <div className="flex gap-3">
              <input
                required
                type="email"
                placeholder="Confirm your signed-in Google email"
                value={bootstrapEmail}
                onChange={(e) => setBootstrapEmail(e.target.value)}
                className="flex-1 px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              />
              <button
                type="submit"
                className="py-2.5 px-5 rounded-xl bg-sky-600 hover:bg-sky-500 text-sm font-semibold"
              >
                Invoke Bootstrap RPC
              </button>
            </div>
          </form>
        </div>
      )}
    </AdminLayoutShell>
  );
}
