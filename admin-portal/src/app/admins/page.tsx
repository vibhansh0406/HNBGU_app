"use client";

import React, { useEffect, useState } from "react";
import AdminLayoutShell from "@/components/AdminLayoutShell";
import { createSupabaseBrowserClient } from "@/lib/supabase/client";
import type { AccountStatus, Department, Profile, UserRole } from "@/types/database";

const ROLES: UserRole[] = [
  "student",
  "department_admin",
  "notice_admin",
  "exam_admin",
  "super_admin",
];

export default function AdminsPage() {
  const [profiles, setProfiles] = useState<Profile[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [search, setSearch] = useState("");
  const [feedback, setFeedback] = useState<string | null>(null);

  async function loadProfiles() {
    const supabase = createSupabaseBrowserClient();
    const [profRes, deptRes] = await Promise.all([
      supabase.from("profiles").select("*").order("created_at", { ascending: false }).limit(200),
      supabase.from("departments").select("*").order("code"),
    ]);
    if (profRes.error) setFeedback(profRes.error.message);
    else setProfiles((profRes.data ?? []) as Profile[]);
    setDepartments((deptRes.data ?? []) as Department[]);
  }

  useEffect(() => {
    loadProfiles();
  }, []);

  async function assignRoleAndStatus(
    targetUser: Profile,
    newRole: UserRole,
    newDeptId: string | null,
    newStatus: AccountStatus
  ) {
    if (
      !window.confirm(
        `Update ${targetUser.email} to role='${newRole}', status='${newStatus}'? This action is logged to audit_logs.`
      )
    ) {
      return;
    }
    setFeedback(null);
    const supabase = createSupabaseBrowserClient();
    const { error } = await supabase.rpc("admin_assign_user_role", {
      target_user_id: targetUser.id,
      new_role: newRole,
      new_department_id: newDeptId,
      new_account_status: newStatus,
    });
    if (error) {
      setFeedback(`RPC Error: ${error.message}`);
    } else {
      setFeedback(`Updated ${targetUser.email} -> ${newRole} (${newStatus}).`);
      loadProfiles();
    }
  }

  const filtered = profiles.filter(
    (p) =>
      p.email.toLowerCase().includes(search.toLowerCase()) ||
      p.full_name.toLowerCase().includes(search.toLowerCase()) ||
      p.role.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <AdminLayoutShell
      title="Administrator & Role Management (Super Admin Only)"
      requiredRoles={["super_admin"]}
    >
      {() => (
        <div className="space-y-6">
          {feedback && (
            <div className="p-3.5 rounded-xl bg-slate-900 border border-sky-500/40 text-sm text-sky-300">
              {feedback}
            </div>
          )}

          <input
            placeholder="Search accounts by email, name, or role..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full px-4 py-2.5 rounded-xl bg-slate-900 border border-slate-800 text-sm"
          />

          <div className="space-y-3">
            {filtered.map((user) => (
              <div
                key={user.id}
                className="p-5 rounded-2xl bg-slate-900 border border-slate-800 flex flex-col lg:flex-row lg:items-center justify-between gap-4"
              >
                <div>
                  <div className="font-bold">
                    {user.full_name || "User"} ({user.email})
                  </div>
                  <div className="text-xs text-slate-400">
                    UUID: <code>{user.id}</code> • Role:{" "}
                    <span className="text-sky-300 font-semibold">{user.role}</span> • Status:{" "}
                    <span className="text-emerald-300 font-semibold">{user.account_status}</span>
                  </div>
                </div>

                <div className="flex flex-wrap items-center gap-2">
                  <select
                    value={user.role}
                    onChange={(e) =>
                      assignRoleAndStatus(
                        user,
                        e.target.value as UserRole,
                        user.department_id,
                        user.account_status
                      )
                    }
                    className="px-3 py-1.5 rounded-xl bg-slate-950 border border-slate-800 text-xs"
                  >
                    {ROLES.map((r) => (
                      <option key={r} value={r}>
                        Role: {r}
                      </option>
                    ))}
                  </select>

                  <select
                    value={user.department_id ?? ""}
                    onChange={(e) =>
                      assignRoleAndStatus(
                        user,
                        user.role,
                        e.target.value || null,
                        user.account_status
                      )
                    }
                    className="px-3 py-1.5 rounded-xl bg-slate-950 border border-slate-800 text-xs"
                  >
                    <option value="">No Department / Global</option>
                    {departments.map((d) => (
                      <option key={d.id} value={d.id}>
                        Dept: {d.code}
                      </option>
                    ))}
                  </select>

                  <button
                    onClick={() =>
                      assignRoleAndStatus(
                        user,
                        user.role,
                        user.department_id,
                        user.account_status === "suspended" ? "active" : "suspended"
                      )
                    }
                    className={`px-3 py-1.5 rounded-xl text-xs font-semibold ${
                      user.account_status === "suspended"
                        ? "bg-emerald-600 hover:bg-emerald-500 text-white"
                        : "bg-red-600/20 hover:bg-red-600/40 text-red-300"
                    }`}
                  >
                    {user.account_status === "suspended" ? "Reactivate" : "Suspend Account"}
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </AdminLayoutShell>
  );
}
