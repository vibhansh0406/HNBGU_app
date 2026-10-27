"use client";

import React, { useEffect, useState } from "react";
import AdminLayoutShell from "@/components/AdminLayoutShell";
import { createSupabaseBrowserClient } from "@/lib/supabase/client";
import type { Department, NotificationCampaign } from "@/types/database";

export default function NotificationsPage() {
  const [campaigns, setCampaigns] = useState<NotificationCampaign[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [targetDeptId, setTargetDeptId] = useState("");
  const [targetSemester, setTargetSemester] = useState<string>("");
  const [feedback, setFeedback] = useState<string | null>(null);
  const [dispatchingId, setDispatchingId] = useState<string | null>(null);

  async function loadData() {
    const supabase = createSupabaseBrowserClient();
    const [campRes, deptRes] = await Promise.all([
      supabase
        .from("notification_campaigns")
        .select("*")
        .order("created_at", { ascending: false })
        .limit(50),
      supabase.from("departments").select("*").order("code"),
    ]);
    if (campRes.error) setFeedback(campRes.error.message);
    else setCampaigns((campRes.data ?? []) as NotificationCampaign[]);
    setDepartments((deptRes.data ?? []) as Department[]);
  }

  useEffect(() => {
    loadData();
  }, []);

  async function handleCreateCampaign(
    e: React.FormEvent,
    creatorId: string,
    defaultDept: string | null
  ) {
    e.preventDefault();
    setFeedback(null);
    const supabase = createSupabaseBrowserClient();
    const resolvedDept = targetDeptId || defaultDept || null;
    const semNum = targetSemester ? Number(targetSemester) : null;

    const { error } = await supabase.from("notification_campaigns").insert({
      title: title.trim(),
      body: body.trim(),
      target_department_id: resolvedDept,
      target_semester: semNum,
      delivery_status: "draft",
      created_by: creatorId,
    });
    if (error) setFeedback(`Error: ${error.message}`);
    else {
      setTitle("");
      setBody("");
      setFeedback("Notification campaign saved in Supabase. Click 'Dispatch via FCM' below to send.");
      loadData();
    }
  }

  async function triggerFcmEdgeFunction(campaignId: string) {
    setDispatchingId(campaignId);
    setFeedback(null);
    try {
      const supabase = createSupabaseBrowserClient();
      const { data, error } = await supabase.functions.invoke("send-fcm-campaign", {
        body: { campaignId },
      });
      if (error) {
        setFeedback(`Edge Function Error: ${error.message}`);
      } else {
        setFeedback(
          `FCM Campaign Response: ${JSON.stringify(data)}`
        );
      }
      loadData();
    } catch (e) {
      setFeedback(e instanceof Error ? e.message : String(e));
    } finally {
      setDispatchingId(null);
    }
  }

  return (
    <AdminLayoutShell
      title="FCM Push Notification Campaigns"
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
            onSubmit={(e) => handleCreateCampaign(e, profile.id, profile.department_id)}
            className="p-6 rounded-2xl bg-slate-900 border border-slate-800 space-y-4"
          >
            <h2 className="text-lg font-semibold">Compose Push Campaign</h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              <input
                required
                placeholder="Notification Title"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                className="px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              />
              <select
                value={targetDeptId}
                onChange={(e) => setTargetDeptId(e.target.value)}
                className="px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              >
                <option value="">
                  {profile.role === "super_admin" ? "All Departments" : "My Assigned Department"}
                </option>
                {departments.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.code} — {d.name}
                  </option>
                ))}
              </select>
              <input
                type="number"
                min={1}
                max={10}
                value={targetSemester}
                onChange={(e) => setTargetSemester(e.target.value)}
                placeholder="Target Semester (Optional 1-10)"
                className="px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              />
            </div>

            <textarea
              required
              rows={2}
              placeholder="Push notification message body..."
              value={body}
              onChange={(e) => setBody(e.target.value)}
              className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
            />

            <button
              type="submit"
              className="py-2.5 px-5 rounded-xl bg-sky-600 hover:bg-sky-500 text-sm font-semibold"
            >
              Save Campaign
            </button>
          </form>

          <div className="space-y-3">
            {campaigns.map((c) => (
              <div
                key={c.id}
                className="p-5 rounded-2xl bg-slate-900 border border-slate-800 flex flex-col md:flex-row md:items-center justify-between gap-4"
              >
                <div>
                  <div className="text-xs text-sky-400 font-semibold">
                    Status: {c.delivery_status.toUpperCase()} • Targeted: {c.total_targeted} •
                    Delivered: {c.total_delivered}
                  </div>
                  <div className="font-bold">{c.title}</div>
                  <div className="text-sm text-slate-300">{c.body}</div>
                  {c.last_error && (
                    <div className="mt-1 text-xs text-amber-300">{c.last_error}</div>
                  )}
                </div>
                <button
                  disabled={c.delivery_status === "sent" || dispatchingId === c.id}
                  onClick={() => triggerFcmEdgeFunction(c.id)}
                  className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-xs font-semibold shrink-0"
                >
                  {dispatchingId === c.id
                    ? "Dispatching..."
                    : c.delivery_status === "sent"
                    ? "Delivered"
                    : "Dispatch via FCM Edge Function"}
                </button>
              </div>
            ))}
          </div>
        </div>
      )}
    </AdminLayoutShell>
  );
}
