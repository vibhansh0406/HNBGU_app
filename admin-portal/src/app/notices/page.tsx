"use client";

import React, { useEffect, useState } from "react";
import AdminLayoutShell from "@/components/AdminLayoutShell";
import { createSupabaseBrowserClient } from "@/lib/supabase/client";
import type { Department, Notice, PublicationStatus } from "@/types/database";

export default function NoticesPage() {
  const [notices, setNotices] = useState<Notice[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [search, setSearch] = useState("");
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [departmentId, setDepartmentId] = useState<string>("");
  const [priority, setPriority] = useState<"LOW" | "MEDIUM" | "HIGH" | "URGENT">("MEDIUM");
  const [isPinned, setIsPinned] = useState(false);
  const [status, setStatus] = useState<PublicationStatus>("published");
  const [expiresAt, setExpiresAt] = useState("");
  const [feedback, setFeedback] = useState<string | null>(null);

  async function loadData() {
    const supabase = createSupabaseBrowserClient();
    const [notRes, deptRes] = await Promise.all([
      supabase.from("notices").select("*").order("created_at", { ascending: false }).limit(100),
      supabase.from("departments").select("*").order("code"),
    ]);
    if (notRes.error) setFeedback(notRes.error.message);
    else setNotices((notRes.data ?? []) as Notice[]);
    setDepartments((deptRes.data ?? []) as Department[]);
  }

  useEffect(() => {
    loadData();
  }, []);

  async function handleSaveNotice(e: React.FormEvent, creatorId: string, defaultDept: string | null) {
    e.preventDefault();
    setFeedback(null);
    const supabase = createSupabaseBrowserClient();
    const resolvedDept = departmentId || defaultDept || null;
    const { error } = await supabase.from("notices").insert({
      title: title.trim(),
      content: content.trim(),
      department_id: resolvedDept,
      priority,
      is_pinned: isPinned,
      status,
      created_by: creatorId,
      published_at: status === "published" ? new Date().toISOString() : null,
      expires_at: expiresAt ? new Date(expiresAt).toISOString() : null,
    });
    if (error) setFeedback(`Error: ${error.message}`);
    else {
      setTitle("");
      setContent("");
      setFeedback(`Notice saved as ${status}.`);
      loadData();
    }
  }

  async function updateNoticeStatus(id: string, nextStatus: PublicationStatus) {
    const supabase = createSupabaseBrowserClient();
    const { error } = await supabase
      .from("notices")
      .update({
        status: nextStatus,
        published_at: nextStatus === "published" ? new Date().toISOString() : null,
      })
      .eq("id", id);
    if (error) setFeedback(`Error: ${error.message}`);
    else loadData();
  }

  async function deleteNotice(id: string) {
    if (!window.confirm("Delete this notice permanently?")) return;
    const supabase = createSupabaseBrowserClient();
    const { error } = await supabase.from("notices").delete().eq("id", id);
    if (error) setFeedback(`Error: ${error.message}`);
    else loadData();
  }

  const filtered = notices.filter((n) => {
    const matchesStatus = statusFilter === "ALL" || n.status === statusFilter;
    const matchesQuery =
      !search ||
      n.title.toLowerCase().includes(search.toLowerCase()) ||
      n.content.toLowerCase().includes(search.toLowerCase());
    return matchesStatus && matchesQuery;
  });

  return (
    <AdminLayoutShell
      title="Notices & Circulars Management"
      requiredRoles={["super_admin", "department_admin", "notice_admin"]}
    >
      {({ profile }) => (
        <div className="space-y-6">
          {feedback && (
            <div className="p-3.5 rounded-xl bg-slate-900 border border-sky-500/40 text-sm text-sky-300">
              {feedback}
            </div>
          )}

          <form
            onSubmit={(e) => handleSaveNotice(e, profile.id, profile.department_id)}
            className="p-6 rounded-2xl bg-slate-900 border border-slate-800 space-y-4"
          >
            <h2 className="text-lg font-semibold">Create Notice / Circular</h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              <input
                required
                placeholder="Notice Title"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                className="md:col-span-2 px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              />
              <select
                value={departmentId}
                onChange={(e) => setDepartmentId(e.target.value)}
                className="px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              >
                <option value="">
                  {profile.role === "super_admin" ? "All Departments (Campus-Wide)" : "My Assigned Department"}
                </option>
                {departments.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.code} — {d.name}
                  </option>
                ))}
              </select>
            </div>

            <textarea
              required
              rows={3}
              placeholder="Notice body / official instructions..."
              value={content}
              onChange={(e) => setContent(e.target.value)}
              className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
            />

            <div className="grid grid-cols-1 md:grid-cols-4 gap-3 items-center">
              <select
                value={priority}
                onChange={(e) => setPriority(e.target.value as Notice["priority"])}
                className="px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              >
                <option value="LOW">Priority: LOW</option>
                <option value="MEDIUM">Priority: MEDIUM</option>
                <option value="HIGH">Priority: HIGH</option>
                <option value="URGENT">Priority: URGENT</option>
              </select>

              <select
                value={status}
                onChange={(e) => setStatus(e.target.value as PublicationStatus)}
                className="px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              >
                <option value="published">Publish Immediately</option>
                <option value="draft">Save as Draft</option>
              </select>

              <input
                type="date"
                value={expiresAt}
                onChange={(e) => setExpiresAt(e.target.value)}
                className="px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              />

              <label className="flex items-center gap-2 text-sm text-slate-300">
                <input
                  type="checkbox"
                  checked={isPinned}
                  onChange={(e) => setIsPinned(e.target.checked)}
                />
                Pin to Top
              </label>
            </div>

            <button
              type="submit"
              className="py-2.5 px-5 rounded-xl bg-sky-600 hover:bg-sky-500 text-sm font-semibold"
            >
              {status === "published" ? "Publish Notice" : "Save Draft"}
            </button>
          </form>

          <div className="flex flex-col sm:flex-row gap-3">
            <input
              placeholder="Search notices..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="flex-1 px-4 py-2.5 rounded-xl bg-slate-900 border border-slate-800 text-sm"
            />
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="px-4 py-2.5 rounded-xl bg-slate-900 border border-slate-800 text-sm"
            >
              <option value="ALL">All Statuses</option>
              <option value="published">Published</option>
              <option value="draft">Drafts (Hidden from Students)</option>
              <option value="archived">Archived</option>
            </select>
          </div>

          <div className="space-y-3">
            {filtered.map((n) => (
              <div
                key={n.id}
                className="p-5 rounded-2xl bg-slate-900 border border-slate-800 flex flex-col md:flex-row md:items-center justify-between gap-4"
              >
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="px-2 py-0.5 rounded text-xs font-semibold bg-slate-800 text-sky-300">
                      {n.status.toUpperCase()}
                    </span>
                    <span className="px-2 py-0.5 rounded text-xs font-semibold bg-amber-500/20 text-amber-300">
                      {n.priority}
                    </span>
                    {n.is_pinned && (
                      <span className="px-2 py-0.5 rounded text-xs font-semibold bg-emerald-500/20 text-emerald-300">
                        PINNED
                      </span>
                    )}
                  </div>
                  <div className="font-bold text-base">{n.title}</div>
                  <div className="text-sm text-slate-300">{n.content}</div>
                </div>
                <div className="flex gap-2 shrink-0">
                  {n.status !== "published" && (
                    <button
                      onClick={() => updateNoticeStatus(n.id, "published")}
                      className="px-3 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-xs font-semibold"
                    >
                      Publish
                    </button>
                  )}
                  {n.status !== "archived" && (
                    <button
                      onClick={() => updateNoticeStatus(n.id, "archived")}
                      className="px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-xs"
                    >
                      Archive
                    </button>
                  )}
                  <button
                    onClick={() => deleteNotice(n.id)}
                    className="px-3 py-1.5 rounded-xl bg-red-600/20 hover:bg-red-600/40 text-red-300 text-xs"
                  >
                    Delete
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
