"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import {
  createSupabaseBrowserClient,
  isSupabaseEnvConfigured,
} from "@/lib/supabase/client";
import type { Profile, UserRole } from "@/types/database";

const NAV_ITEMS: Array<{
  href: string;
  label: string;
  allowedRoles: UserRole[];
}> = [
  {
    href: "/dashboard",
    label: "Dashboard",
    allowedRoles: ["super_admin", "department_admin", "notice_admin", "exam_admin"],
  },
  {
    href: "/departments",
    label: "Departments",
    allowedRoles: ["super_admin", "department_admin"],
  },
  {
    href: "/notices",
    label: "Notices",
    allowedRoles: ["super_admin", "department_admin", "notice_admin"],
  },
  {
    href: "/timetable",
    label: "Timetable",
    allowedRoles: ["super_admin", "department_admin"],
  },
  {
    href: "/assignments",
    label: "Assignments",
    allowedRoles: ["super_admin", "department_admin"],
  },
  {
    href: "/exams",
    label: "Exams",
    allowedRoles: ["super_admin", "department_admin", "exam_admin"],
  },
  {
    href: "/students",
    label: "Students & Attendance",
    allowedRoles: ["super_admin", "department_admin"],
  },
  {
    href: "/admins",
    label: "Admins & Roles",
    allowedRoles: ["super_admin"],
  },
  {
    href: "/notifications",
    label: "Push Campaigns",
    allowedRoles: ["super_admin", "department_admin", "notice_admin"],
  },
  {
    href: "/audit-logs",
    label: "Audit Logs",
    allowedRoles: ["super_admin"],
  },
  {
    href: "/settings",
    label: "Settings",
    allowedRoles: ["super_admin", "department_admin", "notice_admin", "exam_admin"],
  },
];

export function useAdminSession() {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const configured = isSupabaseEnvConfigured();

  useEffect(() => {
    if (!configured) {
      setLoading(false);
      return;
    }
    const supabase = createSupabaseBrowserClient();
    let mounted = true;

    async function load() {
      try {
        const { data: authData, error: authErr } = await supabase.auth.getUser();
        if (authErr || !authData?.user) {
          if (mounted) {
            setProfile(null);
            setLoading(false);
          }
          return;
        }
        const { data: prof, error: profErr } = await supabase
          .from("profiles")
          .select("*")
          .eq("id", authData.user.id)
          .single();

        if (mounted) {
          if (profErr || !prof) {
            setError(
              profErr?.message ??
                "Profile record not found in public.profiles. Execute migration 001 first."
            );
          } else {
            setProfile(prof as Profile);
          }
          setLoading(false);
        }
      } catch (e) {
        if (mounted) {
          setError(e instanceof Error ? e.message : String(e));
          setLoading(false);
        }
      }
    }

    load();
    return () => {
      mounted = false;
    };
  }, [configured]);

  return { profile, loading, error, configured };
}

export default function AdminLayoutShell({
  title,
  requiredRoles = ["super_admin", "department_admin", "notice_admin", "exam_admin"],
  children,
}: {
  title: string;
  requiredRoles?: UserRole[];
  children: (ctx: { profile: Profile }) => React.ReactNode;
}) {
  const pathname = usePathname();
  const router = useRouter();
  const { profile, loading, error, configured } = useAdminSession();

  async function handleSignOut() {
    const supabase = createSupabaseBrowserClient();
    await supabase.auth.signOut();
    router.push("/login");
  }

  if (!configured) {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 p-8 flex items-center justify-center">
        <div className="max-w-2xl w-full bg-slate-900 border border-amber-500/40 rounded-2xl p-8 space-y-4">
          <h1 className="text-2xl font-bold text-amber-400">
            Supabase Environment Configuration Required
          </h1>
          <p className="text-slate-300 text-sm leading-relaxed">
            The CampusOne Admin Portal is compiled and ready, but{" "}
            <code className="text-amber-300">NEXT_PUBLIC_SUPABASE_URL</code> and{" "}
            <code className="text-amber-300">NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY</code>{" "}
            have not been set in <code className="text-amber-300">.env.local</code> yet.
          </p>
          <div className="bg-slate-950 p-4 rounded-xl font-mono text-xs text-slate-300 space-y-1">
            <div>cp .env.example .env.local</div>
            <div># Fill in your Supabase Project URL and Publishable Key</div>
            <div>npm run dev</div>
          </div>
          <p className="text-xs text-slate-400">
            See <code>SETUP_SUPABASE.md</code> at the repository root for complete instructions.
          </p>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 flex items-center justify-center">
        <div className="text-center space-y-2">
          <div className="w-8 h-8 border-2 border-sky-400 border-t-transparent rounded-full animate-spin mx-auto" />
          <p className="text-sm text-slate-400">Verifying Supabase session &amp; role policy...</p>
        </div>
      </div>
    );
  }

  if (!profile) {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 flex items-center justify-center p-6">
        <div className="max-w-md w-full bg-slate-900 border border-slate-800 rounded-2xl p-8 text-center space-y-4">
          <h2 className="text-xl font-bold">Authentication Required</h2>
          <p className="text-sm text-slate-400">
            {error ?? "Please sign in with your authorized Google administrator account."}
          </p>
          <Link
            href="/login"
            className="inline-block w-full py-3 px-4 rounded-xl bg-sky-600 hover:bg-sky-500 font-semibold text-sm"
          >
            Go to Admin Login
          </Link>
        </div>
      </div>
    );
  }

  if (profile.account_status !== "active") {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 flex items-center justify-center p-6">
        <div className="max-w-md w-full bg-slate-900 border border-red-500/40 rounded-2xl p-8 space-y-4">
          <h2 className="text-xl font-bold text-red-400">
            Account {profile.account_status.toUpperCase()}
          </h2>
          <p className="text-sm text-slate-300">
            Your account ({profile.email}) is currently <b>{profile.account_status}</b>. Protected
            administrative operations are blocked by Row Level Security.
          </p>
          <button
            onClick={handleSignOut}
            className="w-full py-2.5 px-4 rounded-xl bg-slate-800 hover:bg-slate-700 text-sm font-medium"
          >
            Sign Out
          </button>
        </div>
      </div>
    );
  }

  if (profile.role === "student" || !requiredRoles.includes(profile.role)) {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 flex items-center justify-center p-6">
        <div className="max-w-lg w-full bg-slate-900 border border-red-500/40 rounded-2xl p-8 space-y-4">
          <h2 className="text-xl font-bold text-red-400">Access Denied — Insufficient Role</h2>
          <p className="text-sm text-slate-300">
            Signed in as <b>{profile.email}</b> with role{" "}
            <code className="text-amber-300">{profile.role}</code>. This page requires one of:{" "}
            <code className="text-sky-300">{requiredRoles.join(", ")}</code>.
          </p>
          <p className="text-xs text-slate-400">
            If this is the first administrator account for your university, run{" "}
            <code>supabase/bootstrap_super_admin.sql</code> in the Supabase SQL Editor or open{" "}
            <Link href="/settings" className="text-sky-400 underline">
              /settings
            </Link>{" "}
            to invoke the one-time first-super-admin bootstrap.
          </p>
          <div className="flex gap-3">
            <Link
              href="/dashboard"
              className="flex-1 text-center py-2.5 px-4 rounded-xl bg-sky-600 hover:bg-sky-500 text-sm font-semibold"
            >
              Back to Dashboard
            </Link>
            <button
              onClick={handleSignOut}
              className="flex-1 py-2.5 px-4 rounded-xl bg-slate-800 hover:bg-slate-700 text-sm font-medium"
            >
              Sign Out
            </button>
          </div>
        </div>
      </div>
    );
  }

  const visibleNav = NAV_ITEMS.filter((item) => item.allowedRoles.includes(profile.role));

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col md:flex-row">
      <aside className="w-full md:w-64 bg-slate-900 border-b md:border-b-0 md:border-r border-slate-800 p-5 flex flex-col justify-between">
        <div>
          <div className="mb-6">
            <div className="text-lg font-bold text-sky-400">CampusOne Admin</div>
            <div className="text-xs text-slate-400 truncate">{profile.email}</div>
            <span className="inline-block mt-1 px-2.5 py-0.5 text-xs font-semibold rounded-full bg-sky-500/20 text-sky-300 border border-sky-500/30">
              {profile.role}
            </span>
          </div>
          <nav className="space-y-1">
            {visibleNav.map((item) => {
              const active = pathname === item.href;
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={`block px-3.5 py-2.5 rounded-xl text-sm font-medium transition ${
                    active
                      ? "bg-sky-600 text-white"
                      : "text-slate-300 hover:bg-slate-800 hover:text-white"
                  }`}
                >
                  {item.label}
                </Link>
              );
            })}
          </nav>
        </div>

        <button
          onClick={handleSignOut}
          className="mt-6 w-full py-2.5 px-4 rounded-xl bg-slate-800 hover:bg-slate-700 text-xs font-semibold text-slate-300"
        >
          Sign Out
        </button>
      </aside>

      <main className="flex-1 p-6 md:p-8 overflow-y-auto">
        <header className="mb-6 pb-4 border-b border-slate-800 flex items-center justify-between">
          <h1 className="text-2xl font-bold">{title}</h1>
          <div className="text-xs text-slate-400">
            Dept Scope: <span className="text-slate-200">{profile.department_id ?? "Global (All)"}</span>
          </div>
        </header>
        {children({ profile })}
      </main>
    </div>
  );
}
