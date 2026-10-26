package `in`.campusone.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    STUDENT, CR, TEACHER, CLUB_ADMIN
}

@Serializable
enum class AttendanceStatus {
    PRESENT, ABSENT, CANCELLED
}

@Serializable
enum class AttachmentType {
    NONE, IMAGE, PDF, LINK
}

@Serializable
enum class EventRsvpStatus {
    NONE, GOING, INTERESTED, NOT_GOING
}

@Serializable
enum class ContactRequestStatus {
    PENDING, ACCEPTED, IGNORED
}

@Serializable
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: UserRole = UserRole.STUDENT,
    val verifiedRole: Boolean = false,
    val collegeId: String = "hnbgu",
    val collegeName: String = "HNB Garhwal University (A Central University)",
    val courseId: String = "bsc_cs",
    val courseName: String = "B.Sc. Computer Science (NEP)",
    val semester: Int = 3,
    val section: String = "A",
    val targetAttendancePct: Int = 75,
    val reminderLeadMinutes: Int = 10,
    val syncAttendanceToCloud: Boolean = true,
    val assignedSectionKeys: List<String> = emptyList(),
    val selectedSubjectIds: List<String> = emptyList(),
    val managedClubIds: List<String> = listOf("club_gdsc_hnbgu"),
    val lastPostTimestampMillis: Long = 0L
) {
    val sectionKey: String
        get() = "${collegeId}_${courseId}_sem${semester}_sec${section}"
}

@Serializable
data class SubjectAttendanceSummary(
    val subjectId: String,
    val subjectName: String,
    val subjectCode: String,
    val isElective: Boolean = false,
    val attendedCount: Int = 0,
    val totalClasses: Int = 0, // Excludes CANCELLED classes
    val cancelledCount: Int = 0
)

@Serializable
data class AttendanceLogItem(
    val id: String,
    val subjectId: String,
    val dateEpochDay: Long,
    val dateIso: String,
    val slotId: String,
    val status: AttendanceStatus,
    val note: String = "",
    val synced: Boolean = false,
    val timestampMillis: Long = System.currentTimeMillis()
)

@Serializable
data class TimetableSlot(
    val id: String,
    val subjectId: String,
    val subjectName: String,
    val subjectCode: String,
    val teacherName: String,
    val roomNumber: String,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val isElective: Boolean = false
)

@Serializable
data class NoticeItem(
    val id: String,
    val sectionKey: String,
    val title: String,
    val body: String,
    val attachmentUrl: String? = null,
    val attachmentType: AttachmentType = AttachmentType.NONE,
    val isPinned: Boolean = false,
    val authorUid: String,
    val authorName: String,
    val authorRole: UserRole,
    val createdAtMillis: Long,
    val isRead: Boolean = false
)
