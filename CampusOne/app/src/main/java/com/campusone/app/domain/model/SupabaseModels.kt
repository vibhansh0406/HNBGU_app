package com.campusone.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class CampusAppRole(val dbValue: String, val displayLabel: String) {
    STUDENT("student", "Student"),
    DEPARTMENT_ADMIN("department_admin", "Department Admin"),
    NOTICE_ADMIN("notice_admin", "Notice Admin"),
    EXAM_ADMIN("exam_admin", "Exam Admin"),
    SUPER_ADMIN("super_admin", "Super Admin");

    val isAdminRole: Boolean
        get() = this != STUDENT

    companion object {
        fun fromDbValue(raw: String?): CampusAppRole {
            return entries.firstOrNull { it.dbValue.equals(raw?.trim(), ignoreCase = true) }
                ?: STUDENT
        }
    }
}

@Serializable
enum class AccountStatus(val dbValue: String) {
    ACTIVE("active"),
    SUSPENDED("suspended"),
    PENDING("pending");

    companion object {
        fun fromDbValue(raw: String?): AccountStatus {
            return entries.firstOrNull { it.dbValue.equals(raw?.trim(), ignoreCase = true) }
                ?: ACTIVE
        }
    }
}

@Serializable
enum class LoginIntentPath {
    STUDENT,
    ADMIN
}

@Serializable
data class SupabaseAuthSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSec: Long,
    val userId: String,
    val email: String
)

@Serializable
data class RemoteDepartmentDto(
    val id: String,
    val name: String,
    val code: String,
    @SerialName("campus_name") val campusName: String = "Chauras Campus, HNBGU",
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class RemoteProfileDto(
    val id: String,
    @SerialName("full_name") val fullName: String = "",
    val email: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val role: String = "student",
    @SerialName("department_id") val departmentId: String? = null,
    @SerialName("account_status") val accountStatus: String = "active"
) {
    val parsedRole: CampusAppRole
        get() = CampusAppRole.fromDbValue(role)

    val parsedStatus: AccountStatus
        get() = AccountStatus.fromDbValue(accountStatus)
}

@Serializable
data class RemoteNoticeDto(
    val id: String = "",
    val title: String,
    val content: String,
    @SerialName("department_id") val departmentId: String? = null,
    val priority: String = "MEDIUM",
    @SerialName("is_pinned") val isPinned: Boolean = false,
    val status: String = "published",
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class RemoteTimetableEntryDto(
    val id: String = "",
    @SerialName("department_id") val departmentId: String,
    val semester: Int,
    val section: String = "A",
    @SerialName("subject_code") val subjectCode: String,
    @SerialName("subject_name") val subjectName: String,
    @SerialName("faculty_name") val facultyName: String,
    val room: String,
    val weekday: Int,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    @SerialName("color_hex") val colorHex: String = "#0F4C81",
    @SerialName("is_published") val isPublished: Boolean = true
)

@Serializable
data class RemoteAssignmentDto(
    val id: String = "",
    val title: String,
    val description: String = "",
    @SerialName("department_id") val departmentId: String,
    val semester: Int,
    val section: String = "A",
    @SerialName("subject_code") val subjectCode: String = "",
    @SerialName("subject_name") val subjectName: String,
    @SerialName("due_at") val dueAt: String,
    val priority: String = "MEDIUM",
    val status: String = "published"
)

@Serializable
data class RemoteExamEventDto(
    val id: String = "",
    @SerialName("exam_title") val examTitle: String,
    @SerialName("exam_type") val examType: String = "END_SEM",
    @SerialName("department_id") val departmentId: String,
    val semester: Int,
    @SerialName("subject_code") val subjectCode: String,
    @SerialName("subject_name") val subjectName: String,
    @SerialName("exam_date") val examDate: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val venue: String,
    val instructions: String = "",
    val status: String = "published"
)

@Serializable
data class RemoteAttendanceRecordDto(
    val id: String = "",
    @SerialName("student_profile_id") val studentProfileId: String,
    @SerialName("department_id") val departmentId: String,
    val semester: Int,
    @SerialName("subject_code") val subjectCode: String,
    @SerialName("subject_name") val subjectName: String,
    @SerialName("attendance_date") val attendanceDate: String,
    val status: String
)

@Serializable
data class RemoteAuditLogDto(
    val id: String = "",
    @SerialName("actor_email") val actorEmail: String? = null,
    @SerialName("actor_role") val actorRole: String? = null,
    val action: String,
    @SerialName("entity_type") val entityType: String,
    @SerialName("entity_id") val entityId: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
