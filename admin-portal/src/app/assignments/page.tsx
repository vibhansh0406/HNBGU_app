"use client";

import React, { useEffect, useState } from "react";
import AdminLayoutShell from "@/components/AdminLayoutShell";
import { createSupabaseBrowserClient } from "@/lib/supabase/client";
import type { Assignment, Department, PublicationStatus } from "@/types/database";

export default function AssignmentsPage() {
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [departmentId, setDepartmentId] = useState("");
  const [semester, setSemester] = useState(4);
  const [section, setSection] = useState("A");
  const [subjectCode, setSubjectCode] = useState("");
  const [subjectName, setSubjectName] = useState("");
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [dueAt, setDueAt] = useState("");
  const [priority, setPriority] = useState<"LOW" | "MEDIUM" | "HIGH">("HIGH");
  const [status, setStatus] = useState<PublicationStatus>("published");
  const [feedback, setFeedback] = useState<string | null>(null);

  async function loadData() {
    const supabase = createSupabaseBrowserClient();
    const [asgRes, deptRes] = await Promise.all([
      supabase.from("assignments").select("*").order("due_at", { ascending: true }),
      supabase.from("departments").select("*").order("code"),
    ]);
    if (asgRes.error) setFeedback(asgRes.error.message);
    else setAssignments((asgRes.data ?? []) as Assignment[]);
    const depts = (deptRes.data ?? []) as Department[];
    setDepartments(depts);
    if (!departmentId && depts.length > 0) setDepartmentId(depts[0].id);
  }

  useEffect(() => {
    loadData();
  }, []);

  async function handleCreate(e: React.FormEvent, creatorId: string, fallbackDept: string | null) {
    e.preventDefault();
    setFeedback(null);
    const targetDept = departmentId || fallbackDept;
    if (!targetDept) {
      setFeedback("Select a department first.");
      return;
    }
    const supabase = createSupabaseBrowserClient();
    const { error } = await supabase.from("assignments").insert({
      title: title.trim(),
      description: description.trim(),
      department_id: targetDept,
      semester,
      section: section.trim().toUpperCase(),
      subject_code: subjectCode.trim().toUpperCase(),
      subject_name: subjectName.trim(),
      due_at: dueAt ? new Date(dueAt).toISOString() : new Date(Date.now() + 3 * 86400000).toISOString(),
      priority,
      status,
      created_by: creatorId,
    });
    if (error) setFeedback(`Error: ${error.message}`);
    else {
      setTitle("");
      setDescription("");
      setFeedback("Assignment saved.");
      loadData();
    }
  }

  async function deleteAssignment(id: string) {
    if (!window.confirm("Delete this assignment?")) return;
    const supabase = createSupabaseBrowserClient();
    const { error } = await supabase.from("assignments").delete().eq("id", id);
    if (error) setFeedback(`Error: ${error.message}`);
    else loadData();
  }

  return (
    <AdminLayoutShell
      title="Department Assignments & Deadlines"
      requiredRoles={["super_admin", "department_admin"]}
    >
      {({ profile }) => (
        <div className="space-y-6">
          {feedback && (
            <div className="p-3.5 rounded-xl bg-slate-900 border border-sky-500/40 text-sm text-sky-300">
              {feedback}
            </div>
          )}

          <form
            onSubmit={(e) => handleCreate(e, profile.id, profile.department_id)}
            className="p-6 rounded-2xl bg-slate-900 border border-slate-800 space-y-4"
          >
            <h2 className="text-lg font-semibold">Publish Section Assignment</h2>
            <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
              <select
                value={departmentId}
                onChange={(e) => setDepartmentId(e.target.value)}
                className="px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              >
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
                value={semester}
                onChange={(e) => setSemester(Number(e.target.value))}
                placeholder="Semester"
                className="px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              />
              <input
                required
                placeholder="Subject Code (CSE-401)"
                value={subjectCode}
                onChange={(e) => setSubjectCode(e.target.value)}
                className="px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              />
              <input
                required
                placeholder="Subject Name"
                value={subjectName}
                onChange={(e) => setSubjectName(e.target.value)}
                className="px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              <input
                required
                placeholder="Assignment Title"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                className="md:col-span-2 px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              />
              <input
                type="datetime-local"
                value={dueAt}
                onChange={(e) => setDueAt(e.target.value)}
                className="px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              />
            </div>

            <textarea
              rows={2}
              placeholder="Assignment instructions..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
            />

            <div className="flex gap-3">
              <select
                value={priority}
                onChange={(e) => setPriority(e.target.value as Assignment["priority"])}
                className="px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              >
                <option value="HIGH">Priority: HIGH</option>
                <option value="MEDIUM">Priority: MEDIUM</option>
                <option value="LOW">Priority: LOW</option>
              </select>
              <select
                value={status}
                onChange={(e) => setStatus(e.target.value as PublicationStatus)}
                className="px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              >
                <option value="published">Publish Now</option>
                <option value="draft">Save Draft</option>
              </select>
              <button
                type="submit"
                className="py-2.5 px-5 rounded-xl bg-sky-600 hover:bg-sky-500 text-sm font-semibold"
              >
                Save Assignment
              </button>
            </div>
          </form>

          <div className="space-y-3">
            {assignments.map((a) => (
              <div
                key={a.id}
                className="p-4 rounded-2xl bg-slate-900 border border-slate-800 flex items-center justify-between"
              >
                <div>
                  <div className="text-xs text-sky-400 font-semibold">
                    {a.subject_code} • Sem {a.semester} ({a.section}) • Due:{" "}
                    {new Date(a.due_at).toLocaleString()}
                  </div>
                  <div className="font-bold">{a.title}</div>
                  <div className="text-xs text-slate-400">{a.description}</div>
                </div>
                <button
                  onClick={() => deleteAssignment(a.id)}
                  className="px-3 py-1.5 rounded-xl bg-red-600/20 hover:bg-red-600/40 text-red-300 text-xs"
                >
                  Delete
                </button>
              </div>
            ))}
          </div>
        </div>
      )}
    </AdminLayoutShell>
  );
}
