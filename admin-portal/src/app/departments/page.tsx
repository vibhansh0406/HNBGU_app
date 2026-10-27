"use client";

import React, { useEffect, useState } from "react";
import AdminLayoutShell from "@/components/AdminLayoutShell";
import { createSupabaseBrowserClient } from "@/lib/supabase/client";
import type { Department } from "@/types/database";

export default function DepartmentsPage() {
  const [departments, setDepartments] = useState<Department[]>([]);
  const [search, setSearch] = useState("");
  const [name, setName] = useState("");
  const [code, setCode] = useState("");
  const [campusName, setCampusName] = useState("Chauras Campus, HNBGU");
  const [feedback, setFeedback] = useState<string | null>(null);

  async function loadDepartments() {
    const supabase = createSupabaseBrowserClient();
    const { data, error } = await supabase
      .from("departments")
      .select("*")
      .order("code", { ascending: true });
    if (error) setFeedback(error.message);
    else setDepartments((data ?? []) as Department[]);
  }

  useEffect(() => {
    loadDepartments();
  }, []);

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    setFeedback(null);
    const supabase = createSupabaseBrowserClient();
    const { error } = await supabase.from("departments").insert({
      name: name.trim(),
      code: code.trim().toUpperCase(),
      campus_name: campusName.trim(),
      is_active: true,
    });
    if (error) {
      setFeedback(`Error: ${error.message}`);
    } else {
      setName("");
      setCode("");
      setFeedback("Department created.");
      loadDepartments();
    }
  }

  async function toggleActive(dept: Department) {
    const supabase = createSupabaseBrowserClient();
    const { error } = await supabase
      .from("departments")
      .update({ is_active: !dept.is_active })
      .eq("id", dept.id);
    if (error) setFeedback(`Error: ${error.message}`);
    else loadDepartments();
  }

  const filtered = departments.filter(
    (d) =>
      d.name.toLowerCase().includes(search.toLowerCase()) ||
      d.code.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <AdminLayoutShell
      title="University Departments"
      requiredRoles={["super_admin", "department_admin"]}
    >
      {({ profile }) => (
        <div className="space-y-6">
          {feedback && (
            <div className="p-3.5 rounded-xl bg-slate-900 border border-sky-500/40 text-sm text-sky-300">
              {feedback}
            </div>
          )}

          {profile.role === "super_admin" && (
            <form
              onSubmit={handleCreate}
              className="p-5 rounded-2xl bg-slate-900 border border-slate-800 grid grid-cols-1 md:grid-cols-4 gap-3"
            >
              <input
                required
                placeholder="Department Code (e.g. CSE)"
                value={code}
                onChange={(e) => setCode(e.target.value)}
                className="px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              />
              <input
                required
                placeholder="Department Full Name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              />
              <input
                required
                placeholder="Campus Name"
                value={campusName}
                onChange={(e) => setCampusName(e.target.value)}
                className="px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              />
              <button
                type="submit"
                className="py-2.5 px-4 rounded-xl bg-sky-600 hover:bg-sky-500 text-sm font-semibold"
              >
                Add Department
              </button>
            </form>
          )}

          <input
            placeholder="Search departments by code or name..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full px-4 py-2.5 rounded-xl bg-slate-900 border border-slate-800 text-sm"
          />

          <div className="space-y-3">
            {filtered.map((dept) => (
              <div
                key={dept.id}
                className="p-4 rounded-2xl bg-slate-900 border border-slate-800 flex items-center justify-between"
              >
                <div>
                  <div className="font-semibold">
                    {dept.code} — {dept.name}
                  </div>
                  <div className="text-xs text-slate-400">
                    {dept.campus_name} • ID: <code>{dept.id}</code>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <span
                    className={`px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                      dept.is_active
                        ? "bg-emerald-500/20 text-emerald-300"
                        : "bg-slate-800 text-slate-400"
                    }`}
                  >
                    {dept.is_active ? "Active" : "Inactive"}
                  </span>
                  {profile.role === "super_admin" && (
                    <button
                      onClick={() => toggleActive(dept)}
                      className="px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-xs"
                    >
                      Toggle Active
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </AdminLayoutShell>
  );
}
