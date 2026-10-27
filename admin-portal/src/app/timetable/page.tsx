"use client";

import React, { useEffect, useState } from "react";
import AdminLayoutShell from "@/components/AdminLayoutShell";
import { createSupabaseBrowserClient } from "@/lib/supabase/client";
import type { Department, TimetableEntry } from "@/types/database";

const WEEKDAYS = [
  { value: 1, label: "Monday" },
  { value: 2, label: "Tuesday" },
  { value: 3, label: "Wednesday" },
  { value: 4, label: "Thursday" },
  { value: 5, label: "Friday" },
  { value: 6, label: "Saturday" },
];

export default function TimetablePage() {
  const [entries, setEntries] = useState<TimetableEntry[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [departmentId, setDepartmentId] = useState("");
  const [semester, setSemester] = useState(4);
  const [section, setSection] = useState("A");
  const [weekday, setWeekday] = useState(1);
  const [subjectCode, setSubjectCode] = useState("");
  const [subjectName, setSubjectName] = useState("");
  const [facultyName, setFacultyName] = useState("");
  const [room, setRoom] = useState("");
  const [startTime, setStartTime] = useState("09:30");
  const [endTime, setEndTime] = useState("10:30");
  const [feedback, setFeedback] = useState<string | null>(null);

  async function loadData() {
    const supabase = createSupabaseBrowserClient();
    const [ttRes, deptRes] = await Promise.all([
      supabase
        .from("timetable_entries")
        .select("*")
        .order("weekday", { ascending: true })
        .order("start_time", { ascending: true }),
      supabase.from("departments").select("*").order("code"),
    ]);
    if (ttRes.error) setFeedback(ttRes.error.message);
    else setEntries((ttRes.data ?? []) as TimetableEntry[]);
    const depts = (deptRes.data ?? []) as Department[];
    setDepartments(depts);
    if (!departmentId && depts.length > 0) {
      setDepartmentId(depts[0].id);
    }
  }

  useEffect(() => {
    loadData();
  }, []);

  async function handleCreateSlot(e: React.FormEvent, creatorId: string, fallbackDept: string | null) {
    e.preventDefault();
    setFeedback(null);
    if (endTime <= startTime) {
      setFeedback("Validation error: End time must be later than Start time (HH:mm).");
      return;
    }
    const targetDept = departmentId || fallbackDept;
    if (!targetDept) {
      setFeedback("Select a department first.");
      return;
    }
    const supabase = createSupabaseBrowserClient();
    const { error } = await supabase.from("timetable_entries").insert({
      department_id: targetDept,
      semester,
      section: section.trim().toUpperCase(),
      weekday,
      subject_code: subjectCode.trim().toUpperCase(),
      subject_name: subjectName.trim(),
      faculty_name: facultyName.trim(),
      room: room.trim(),
      start_time: startTime,
      end_time: endTime,
      is_published: true,
      created_by: creatorId,
    });
    if (error) setFeedback(`Error: ${error.message}`);
    else {
      setSubjectCode("");
      setSubjectName("");
      setFeedback("Timetable slot published.");
      loadData();
    }
  }

  async function deleteSlot(id: string) {
    if (!window.confirm("Remove this timetable slot?")) return;
    const supabase = createSupabaseBrowserClient();
    const { error } = await supabase.from("timetable_entries").delete().eq("id", id);
    if (error) setFeedback(`Error: ${error.message}`);
    else loadData();
  }

  return (
    <AdminLayoutShell
      title="Department Timetable Management"
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
            onSubmit={(e) => handleCreateSlot(e, profile.id, profile.department_id)}
            className="p-6 rounded-2xl bg-slate-900 border border-slate-800 space-y-4"
          >
            <h2 className="text-lg font-semibold">Add Published Timetable Slot</h2>
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
                value={section}
                onChange={(e) => setSection(e.target.value)}
                placeholder="Section (e.g. A)"
                className="px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              />
              <select
                value={weekday}
                onChange={(e) => setWeekday(Number(e.target.value))}
                className="px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              >
                {WEEKDAYS.map((w) => (
                  <option key={w.value} value={w.value}>
                    {w.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
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
              <input
                required
                placeholder="Faculty Name"
                value={facultyName}
                onChange={(e) => setFacultyName(e.target.value)}
                className="px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              />
              <input
                required
                placeholder="Room / Lab"
                value={room}
                onChange={(e) => setRoom(e.target.value)}
                className="px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              <input
                required
                placeholder="Start Time (HH:mm)"
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
                className="px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              />
              <input
                required
                placeholder="End Time (HH:mm)"
                value={endTime}
                onChange={(e) => setEndTime(e.target.value)}
                className="px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              />
              <button
                type="submit"
                className="py-2.5 px-5 rounded-xl bg-sky-600 hover:bg-sky-500 text-sm font-semibold"
              >
                Publish Timetable Slot
              </button>
            </div>
          </form>

          <div className="space-y-3">
            {entries.map((s) => (
              <div
                key={s.id}
                className="p-4 rounded-2xl bg-slate-900 border border-slate-800 flex items-center justify-between"
              >
                <div>
                  <div className="text-xs text-sky-400 font-semibold">
                    Day {s.weekday} • {s.start_time} - {s.end_time} • Sem {s.semester} ({s.section})
                  </div>
                  <div className="font-bold">
                    {s.subject_code} — {s.subject_name}
                  </div>
                  <div className="text-xs text-slate-400">
                    {s.room} • {s.faculty_name}
                  </div>
                </div>
                <button
                  onClick={() => deleteSlot(s.id)}
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
