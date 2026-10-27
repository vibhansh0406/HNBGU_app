export type UserRole =
  | "student"
  | "department_admin"
  | "notice_admin"
  | "exam_admin"
  | "super_admin";

export type AccountStatus = "active" | "suspended" | "pending";
export type PublicationStatus = "draft" | "published" | "archived";

export interface Department {
  id: string;
  name: string;
  code: string;
  campus_name: string;
  is_active: boolean;
  created_at: string;
  updated_at: string;
}

export interface Profile {
  id: string;
  full_name: string;
  email: string;
  avatar_url: string | null;
  role: UserRole;
  department_id: string | null;
  account_status: AccountStatus;
  created_at: string;
  updated_at: string;
}

export interface StudentEnrollment {
  id: string;
  student_profile_id: string;
  student_identifier: string;
  department_id: string;
  semester: number;
  section: string;
  verification_status: "pending" | "verified" | "rejected";
  verified_by: string | null;
  verified_at: string | null;
  created_at: string;
}

export interface Notice {
  id: string;
  title: string;
  content: string;
  department_id: string | null;
  target_semesters: number[];
  target_sections: string[];
  priority: "LOW" | "MEDIUM" | "HIGH" | "URGENT";
  is_pinned: boolean;
  attachment_path: string | null;
  status: PublicationStatus;
  created_by: string | null;
  published_at: string | null;
  expires_at: string | null;
  created_at: string;
}

export interface TimetableEntry {
  id: string;
  department_id: string;
  semester: number;
  section: string;
  subject_code: string;
  subject_name: string;
  faculty_name: string;
  room: string;
  weekday: number;
  start_time: string;
  end_time: string;
  color_hex: string;
  effective_from: string;
  effective_until: string | null;
  is_published: boolean;
  created_at: string;
}

export interface Assignment {
  id: string;
  title: string;
  description: string;
  department_id: string;
  semester: number;
  section: string;
  subject_code: string;
  subject_name: string;
  due_at: string;
  priority: "LOW" | "MEDIUM" | "HIGH";
  attachment_path: string | null;
  status: PublicationStatus;
  created_at: string;
}

export interface ExamEvent {
  id: string;
  exam_title: string;
  exam_type: "MID_SEM" | "END_SEM" | "PRACTICAL" | "BACK_PAPER" | "QUIZ" | "OTHER";
  department_id: string;
  semester: number;
  subject_code: string;
  subject_name: string;
  exam_date: string;
  start_time: string;
  end_time: string;
  venue: string;
  instructions: string;
  status: PublicationStatus;
  created_at: string;
}

export interface AttendanceRecord {
  id: string;
  student_profile_id: string;
  department_id: string;
  semester: number;
  subject_code: string;
  subject_name: string;
  attendance_date: string;
  status: "PRESENT" | "ABSENT" | "CANCELLED";
  created_at: string;
}

export interface NotificationCampaign {
  id: string;
  title: string;
  body: string;
  target_department_id: string | null;
  target_semester: number | null;
  target_section: string | null;
  related_entity_type: string | null;
  scheduled_for: string | null;
  delivery_status: "draft" | "scheduled" | "sending" | "sent" | "failed";
  total_targeted: number;
  total_delivered: number;
  last_error: string | null;
  sent_at: string | null;
  created_at: string;
}

export interface AuditLog {
  id: string;
  actor_id: string | null;
  actor_email: string | null;
  actor_role: string | null;
  action: string;
  entity_type: string;
  entity_id: string | null;
  metadata: Record<string, unknown>;
  created_at: string;
}
