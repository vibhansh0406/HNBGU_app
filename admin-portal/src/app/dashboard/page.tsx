"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import AdminLayoutShell from "@/components/AdminLayoutShell";
import { createSupabaseBrowserClient } from "@/lib/supabase/client";

interface DashboardMetrics {
  departments: number;
  publishedNotices: number;
  timetableEntries: number;
  assignments: number;
  exams: number;
  pendingEnrollments: number;
  campaigns: number;
}

export default function DashboardPage() {
  const [metrics, setMetrics] = useState<DashboardMetrics>({
    departments: 0,
    publishedNotices: 0,
    timetableEntries: 0,
    assignments: 0,
    exams: 0,
    pendingEnrollments: 0,
    campaigns: 0,
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const supabase = createSupabaseBrowserClient();
    async function fetchCounts() {
      const [dept, not, tt, asg, ex, enr, camp] = await Promise.all([
        supabase.from("departments").select("id", { count: "exact", head: true }),
        supabase
          .from("notices")
          .select("id", { count: "exact", head: true })
          .eq("status", "published"),
        supabase.from("timetable_entries").select("id", { count: "exact", head: true }),
        supabase.from("assignments").select("id", { count: "exact", head: true }),
        supabase.from("exam_events").select("id", { count: "exact", head: true }),
        supabase
          .from("student_enrollments")
          .select("id", { count: "exact", head: true })
          .eq("verification_status", "pending"),
        supabase.from("notification_campaigns").select("id", { count: "exact", head: true }),
      ]);
      setMetrics({
        departments: dept.count ?? 0,
        publishedNotices: not.count ?? 0,
        timetableEntries: tt.count ?? 0,
        assignments: asg.count ?? 0,
        exams: ex.count ?? 0,
        pendingEnrollments: enr.count ?? 0,
        campaigns: camp.count ?? 0,
      });
      setLoading(false);
    }
    fetchCounts();
  }, []);

  return (
    <AdminLayoutShell title="Administrative Overview">
      {({ profile }) => (
        <div className="space-y-6">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {[
              { label: "Active Departments", value: metrics.departments, href: "/departments" },
              { label: "Published Notices", value: metrics.publishedNotices, href: "/notices" },
              { label: "Timetable Slots", value: metrics.timetableEntries, href: "/timetable" },
              { label: "Assignments", value: metrics.assignments, href: "/assignments" },
              { label: "Exam Events", value: metrics.exams, href: "/exams" },
              { label: "Pending Enrollments", value: metrics.pendingEnrollments, href: "/students" },
              { label: "Push Campaigns", value: metrics.campaigns, href: "/notifications" },
            ].map((card) => (
              <Link
                key={card.label}
                href={card.href}
                className="p-5 rounded-2xl bg-slate-900 border border-slate-800 hover:border-sky-500/50 transition"
              >
                <div className="text-xs text-slate-400">{card.label}</div>
                <div className="mt-2 text-3xl font-bold text-sky-400">
                  {loading ? "—" : card.value}
                </div>
              </Link>
            ))}
          </div>

          <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 space-y-2">
            <h2 className="text-lg font-semibold">Signed-In Administrator Context</h2>
            <p className="text-sm text-slate-300">
              Name: <b>{profile.full_name}</b> ({profile.email}) • Role:{" "}
              <code className="text-sky-300">{profile.role}</code> • Status:{" "}
              <code className="text-emerald-300">{profile.account_status}</code>
            </p>
            <p className="text-xs text-slate-400">
              All queries and mutations in this portal execute under your authenticated Supabase
              JWT and are strictly enforced by PostgreSQL Row Level Security.
            </p>
          </div>
        </div>
      )}
    </AdminLayoutShell>
  );
}
