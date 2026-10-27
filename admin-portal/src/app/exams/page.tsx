"use client";

import React, { useEffect, useState } from "react";
import AdminLayoutShell from "@/components/AdminLayoutShell";
import { createSupabaseBrowserClient } from "@/lib/supabase/client";
import type { Department, ExamEvent, PublicationStatus } from "@/types/database";

export default function ExamsPage() {
  const [exams, setExams] = useState<ExamEvent[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [departmentId, setDepartmentId] = useState("");
  const [semester, setSemester] = useState(4);
  const [examTitle, setExamTitle] = useState("End-Semester Theory Exam");
  const [examType, setExamType] = useState<ExamEvent["exam_type"]>("END_SEM");
  const [subjectCode, setSubjectCode] = useState("");
  const [subjectName, setSubjectName] = useState("");
  const [examDate, setExamDate] = useState("");
  const [startTime, setStartTime] = useState("10:00 AM");
  const [endTime, setEndTime] = useState("01:00 PM");
  const [venue, setVenue] = useState("Chauras Exam Hall A");
  const [instructions, setInstructions] = useState("");
  const [status, setStatus] = useState<PublicationStatus>("published");
  const [feedback, setFeedback] = useState<string | null>(null);

  async function loadData() {
    const supabase = createSupabaseBrowserClient();
    const [exRes, deptRes] = await Promise.all([
      supabase.from("exam_events").select("*").order("exam_date", { ascending: true }),
      supabase.from("departments").select("*").order("code"),
    ]);
    if (exRes.error) setFeedback(exRes.error.message);
    else setExams((exRes.data ?? []) as ExamEvent[]);
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
    const { error } = await supabase.from("exam_events").insert({
      exam_title: examTitle.trim(),
      exam_type: examType,
      department_id: targetDept,
      semester,
      subject_code: subjectCode.trim().toUpperCase(),
      subject_name: subjectName.trim(),
      exam_date: examDate || new Date(Date.now() + 14 * 86400000).toISOString().slice(0, 10),
      start_time: startTime.trim(),
      end_time: endTime.trim(),
      venue: venue.trim(),
      instructions: instructions.trim(),
      status,
      created_by: creatorId,
    });
    if (error) setFeedback(`Error: ${error.message}`);
    else {
      setSubjectCode("");
      setSubjectName("");
      setFeedback("Exam schedule saved.");
      loadData();
    }
  }

  async function deleteExam(id: string) {
    if (!window.confirm("Delete this exam event?")) return;
    const supabase = createSupabaseBrowserClient();
    const { error } = await supabase.from("exam_events").delete().eq("id", id);
    if (error) setFeedback(`Error: ${error.message}`);
    else loadData();
  }

  return (
    <AdminLayoutShell
      title="University Exam Calendar & Schedules"
      requiredRoles={["super_admin", "department_admin", "exam_admin"]}
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
            <h2 className="text-lg font-semibold">Add Exam Event</h2>
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
              <select
                value={examType}
                onChange={(e) => setExamType(e.target.value as ExamEvent["exam_type"])}
                className="px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              >
                <option value="END_SEM">END_SEM</option>
                <option value="MID_SEM">MID_SEM</option>
                <option value="PRACTICAL">PRACTICAL</option>
                <option value="BACK_PAPER">BACK_PAPER</option>
              </select>
              <input
                type="date"
                value={examDate}
                onChange={(e) => setExamDate(e.target.value)}
                className="px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              />
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
                placeholder="Venue"
                value={venue}
                onChange={(e) => setVenue(e.target.value)}
                className="px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              />
              <input
                required
                placeholder="Exam Title"
                value={examTitle}
                onChange={(e) => setExamTitle(e.target.value)}
                className="px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
              <input
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
                placeholder="Start Time"
                className="px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              />
              <input
                value={endTime}
                onChange={(e) => setEndTime(e.target.value)}
                placeholder="End Time"
                className="px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              />
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
                Save Exam Event
              </button>
            </div>
          </form>

          <div className="space-y-3">
            {exams.map((ex) => (
              <div
                key={ex.id}
                className="p-4 rounded-2xl bg-slate-900 border border-slate-800 flex items-center justify-between"
              >
                <div>
                  <div className="text-xs text-sky-400 font-semibold">
                    {ex.exam_date} • {ex.start_time} - {ex.end_time} • {ex.exam_type} ({ex.status})
                  </div>
                  <div className="font-bold">
                    {ex.subject_code} — {ex.subject_name}
                  </div>
                  <div className="text-xs text-slate-400">
                    Venue: {ex.venue} • Sem {ex.semester}
                  </div>
                </div>
                <button
                  onClick={() => deleteExam(ex.id)}
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
