"use client";

import React, { useEffect, useState } from "react";
import AdminLayoutShell from "@/components/AdminLayoutShell";
import { createSupabaseBrowserClient } from "@/lib/supabase/client";
import type { AuditLog } from "@/types/database";

export default function AuditLogsPage() {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [search, setSearch] = useState("");
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  useEffect(() => {
    const supabase = createSupabaseBrowserClient();
    supabase
      .from("audit_logs")
      .select("*")
      .order("created_at", { ascending: false })
      .limit(100)
      .then(({ data, error }) => {
        if (error) setErrorMsg(error.message);
        else setLogs((data ?? []) as AuditLog[]);
      });
  }, []);

  const filtered = logs.filter(
    (l) =>
      !search ||
      l.action.toLowerCase().includes(search.toLowerCase()) ||
      (l.actor_email ?? "").toLowerCase().includes(search.toLowerCase()) ||
      l.entity_type.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <AdminLayoutShell
      title="Administrative Audit Logs (Immutable)"
      requiredRoles={["super_admin"]}
    >
      {() => (
        <div className="space-y-5">
          {errorMsg && (
            <div className="p-3.5 rounded-xl bg-red-500/10 border border-red-500/30 text-sm text-red-300">
              {errorMsg}
            </div>
          )}

          <input
            placeholder="Filter audit logs by actor email, action, or entity..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full px-4 py-2.5 rounded-xl bg-slate-900 border border-slate-800 text-sm"
          />

          <div className="space-y-2.5">
            {filtered.map((log) => (
              <div
                key={log.id}
                className="p-4 rounded-xl bg-slate-900 border border-slate-800 font-mono text-xs space-y-1"
              >
                <div className="flex items-center justify-between text-sky-300">
                  <span>
                    [{log.action}] on {log.entity_type} ({log.entity_id ?? "n/a"})
                  </span>
                  <span className="text-slate-400">
                    {new Date(log.created_at).toLocaleString()}
                  </span>
                </div>
                <div className="text-slate-300">
                  Actor: {log.actor_email ?? "system"} ({log.actor_role ?? "admin"})
                </div>
                <div className="text-slate-400">
                  Metadata: {JSON.stringify(log.metadata)}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </AdminLayoutShell>
  );
}
