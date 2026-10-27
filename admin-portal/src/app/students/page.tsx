"use client";

import React, { useEffect, useState } from "react";
import AdminLayoutShell from "@/components/AdminLayoutShell";
import { createSupabaseBrowserClient } from "@/lib/supabase/client";
import type {
  AttendanceRecord,
  Department,
  Profile,
  StudentEnrollment,
} from "@/types/database";

export default function StudentsPage() {
  const [enrollments, setEnrollments] = useState<StudentEnrollment[]>([]);
  const [students, setStudents] = useState<Profile[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [attendanceRecords, setAttendanceRecords] = useState<AttendanceRecord[]>([]);
  const [selectedStudentId, setSelectedStudentId] = useState("");
  const [departmentId, setDepartmentId] = useState("");
  const [semester, setSemester] = useState(4);
  const [subjectCode, setSubjectCode] = useState("CSE-401");
  const [subjectName, setSubjectName] = useState("Design & Analysis of Algorithms");
  const [attendanceDate, setAttendanceDate] = useState(
    new Date().toISOString().slice(0, 10)
  );
  const [attStatus, setAttStatus] = useState<"PRESENT" | "ABSENT" | "CANCELLED">("PRESENT");
  const [feedback, setFeedback] = useState<string | null>(null);

  async function loadData() {
    const supabase = createSupabaseBrowserClient();
    const [enrRes, profRes, deptRes, attRes] = await Promise.all([
      supabase.from("student_enrollments").select("*").order("created_at", { ascending: false }),
      supabase.from("profiles").select("*").eq("role", "student").limit(100),
      supabase.from("departments").select("*").order("code"),
      supabase
        .from("attendance_records")
        .select("*")
        .order("attendance_date", { ascending: false })
        .limit(50),
    ]);
    setEnrollments((enrRes.data ?? []) as StudentEnrollment[]);
    const stuList = (profRes.data ?? []) as Profile[];
    setStudents(stuList);
    if (!selectedStudentId && stuList.length > 0) setSelectedStudentId(stuList[0].id);
    const depts = (deptRes.data ?? []) as Department[];
    setDepartments(depts);
    if (!departmentId && depts.length > 0) setDepartmentId(depts[0].id);
    setAttendanceRecords((attRes.data ?? []) as AttendanceRecord[]);
  }

  useEffect(() => {
    loadData();
  }, []);

  async function verifyEnrollment(
    enrollmentId: string,
    newStatus: "verified" | "rejected",
    adminId: string
  ) {
    const supabase = createSupabaseBrowserClient();
    const { error } = await supabase
      .from("student_enrollments")
      .update({
        verification_status: newStatus,
        verified_by: adminId,
        verified_at: new Date().toISOString(),
      })
      .eq("id", enrollmentId);
    if (error) setFeedback(`Error: ${error.message}`);
    else {
      setFeedback(`Enrollment marked ${newStatus}.`);
      loadData();
    }
  }

  async function recordOfficialAttendance(e: React.FormEvent, adminId: string) {
    e.preventDefault();
    setFeedback(null);
    if (!selectedStudentId || !departmentId) {
      setFeedback("Select a student and department first.");
      return;
    }
    const supabase = createSupabaseBrowserClient();
    const { error } = await supabase.from("attendance_records").upsert(
      {
        student_profile_id: selectedStudentId,
        department_id: departmentId,
        semester,
        subject_code: subjectCode.trim().toUpperCase(),
        subject_name: subjectName.trim(),
        attendance_date: attendanceDate,
        status: attStatus,
        recorded_by: adminId,
      },
      { onConflict: "student_profile_id,subject_code,attendance_date" }
    );
    if (error) setFeedback(`Error: ${error.message}`);
    else {
      setFeedback("Official attendance record saved.");
      loadData();
    }
  }

  return (
    <AdminLayoutShell
      title="Student Enrollments & Official Attendance"
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
            onSubmit={(e) => recordOfficialAttendance(e, profile.id)}
            className="p-6 rounded-2xl bg-slate-900 border border-slate-800 space-y-4"
          >
            <h2 className="text-lg font-semibold">Record Official Student Attendance</h2>
            <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
              <select
                value={selectedStudentId}
                onChange={(e) => setSelectedStudentId(e.target.value)}
                className="px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              >
                <option value="">Select Student Profile</option>
                {students.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.full_name} ({s.email})
                  </option>
                ))}
              </select>
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
                type="date"
                value={attendanceDate}
                onChange={(e) => setAttendanceDate(e.target.value)}
                className="px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              />
              <select
                value={attStatus}
                onChange={(e) => setAttStatus(e.target.value as AttendanceRecord["status"])}
                className="px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-sm"
              >
                <option value="PRESENT">PRESENT</option>
                <option value="ABSENT">ABSENT</option>
                <option value="CANCELLED">CANCELLED</option>
              </select>
              <button
                type="submit"
                className="py-2.5 px-5 rounded-xl bg-sky-600 hover:bg-sky-500 text-sm font-semibold"
              >
                Save Official Attendance
              </button>
            </div>
          </form>

          <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 space-y-3">
            <h2 className="text-lg font-semibold">Student Enrollment Verification Queue</h2>
            {enrollments.length === 0 ? (
              <p className="text-sm text-slate-400">No student enrollment requests pending.</p>
            ) : (
              enrollments.map((enr) => (
                <div
                  key={enr.id}
                  className="p-4 rounded-xl bg-slate-950 border border-slate-800 flex items-center justify-between"
                >
                  <div>
                    <div className="font-semibold">
                      Roll/ID: {enr.student_identifier} • Sem {enr.semester} ({enr.section})
                    </div>
                    <div className="text-xs text-slate-400">
                      Status: {enr.verification_status.toUpperCase()}
                    </div>
                  </div>
                  <div className="flex gap-2">
                    <button
                      onClick={() => verifyEnrollment(enr.id, "verified", profile.id)}
                      className="px-3 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-xs font-semibold"
                    >
                      Approve
                    </button>
                    <button
                      onClick={() => verifyEnrollment(enr.id, "rejected", profile.id)}
                      className="px-3 py-1.5 rounded-xl bg-red-600/20 hover:bg-red-600/40 text-red-300 text-xs"
                    >
                      Reject
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>

          <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 space-y-3">
            <h2 className="text-lg font-semibold">Recent Official Attendance Records</h2>
            {attendanceRecords.map((rec) => (
              <div
                key={rec.id}
                className="p-3.5 rounded-xl bg-slate-950 border border-slate-800 flex items-center justify-between text-sm"
              >
                <div>
                  <b>{rec.subject_code}</b> ({rec.subject_name}) — {rec.attendance_date}
                </div>
                <span className="font-semibold text-sky-300">{rec.status}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </AdminLayoutShell>
  );
}
