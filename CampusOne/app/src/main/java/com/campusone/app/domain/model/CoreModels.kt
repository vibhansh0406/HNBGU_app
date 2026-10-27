package com.campusone.app.domain.model

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
enum class GradingScale(val displayName: String, val maxPoints: Double) {
    SCALE_10("10-Point UGC/CBCS", 10.0),
    SCALE_4("4-Point Scale", 4.0),
    PERCENTAGE("Percentage (%)", 100.0)
}

@Serializable
enum class AssignmentFilter {
    TODAY, UPCOMING, OVERDUE, COMPLETED
}

@Serializable
enum class PriorityLevel {
    LOW, MEDIUM, HIGH
}

@Serializable
enum class MaterialType {
    NOTES, PYQ, ASSIGNMENT, SYLLABUS, OTHER
}

@Serializable
enum class RsvpStatus {
    GOING, INTERESTED, NOT_GOING
}

@Serializable
data class UserProfile(
    val name: String = "",
    val college: String = "HNB Garhwal University",
    val course: String = "B.Tech CSE",
    val semester: Int = 4,
    val section: String = "A",
    val role: UserRole = UserRole.STUDENT,
    val targetAttendancePercent: Int = 75,
    val lowAttendanceAlertEnabled: Boolean = true,
    val lowAttendanceAlertThreshold: Int = 75,
    val semesterEndEpochDay: Long = 0L,
    val reminderLeadMinutes: Int = 15,
    val gradingScale: GradingScale = GradingScale.SCALE_10,
    val languageCode: String = "en",
    val setupCompleted: Boolean = false
)

@Serializable
data class AttendanceAdvice(
    val attended: Int,
    val totalConducted: Int,
    val currentPercentage: Double,
    val targetPercentage: Int,
    val safeBunksAvailable: Int,
    val classesMustAttendToRecover: Int,
    val isAboveTarget: Boolean,
    val isWarningZone: Boolean = false
)

@Serializable
data class SubjectSummary(
    val id: String,
    val code: String,
    val name: String,
    val professor: String,
    val semester: Int,
    val attendedCount: Int,
    val totalClassesCount: Int,
    val cancelledCount: Int,
    val targetPercentage: Int = 75,
    val colorHex: String = "#0F4C81",
    val advice: AttendanceAdvice
)

@Serializable
data class SafeBunkProjection(
    val subjectId: String,
    val subjectCode: String,
    val subjectName: String,
    val attended: Int,
    val totalConducted: Int,
    val targetPercentage: Int,
    val currentPercentage: Double,
    val safeBunksNow: Int,
    val mustAttendToRecoverNow: Int,
    val remainingScheduledClasses: Int,
    val projectedSafeBunksUntilEnd: Int,
    val projectedMustAttendUntilEnd: Int,
    val whatIfSkipCount: Int,
    val whatIfPercentage: Double,
    val isWhatIfAboveTarget: Boolean
)

@Serializable
data class MonthlyHeatmapDay(
    val epochDay: Long,
    val dayOfMonth: Int,
    val presentCount: Int,
    val absentCount: Int,
    val cancelledCount: Int
)

@Serializable
data class AttendanceTrendPoint(
    val epochDay: Long,
    val dateLabel: String,
    val cumulativePercentage: Double
)

@Serializable
data class AttendanceRecordItem(
    val id: String,
    val subjectId: String,
    val subjectName: String,
    val epochDay: Long,
    val dateLabel: String,
    val status: AttendanceStatus,
    val timestampMillis: Long
)

@Serializable
data class TimetableSlot(
    val id: String,
    val subjectId: String,
    val subjectName: String,
    val subjectCode: String,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val room: String,
    val teacherName: String,
    val semester: Int,
    val section: String,
    val colorHex: String = "#0F4C81"
)

@Serializable
data class TimetableClash(
    val conflictingSlot: TimetableSlot,
    val overlapMinutes: Int
)

@Serializable
data class EvaluatedSlot(
    val slot: TimetableSlot,
    val minutesUntilStart: Int,
    val minutesRemaining: Int,
    val isOngoing: Boolean
)

@Serializable
data class PersonalNotice(
    val id: String,
    val title: String,
    val body: String,
    val category: String = "PERSONAL",
    val isPinned: Boolean = false,
    val isRead: Boolean = false,
    val createdAtMillis: Long
)

@Serializable
data class AssignmentItem(
    val id: String,
    val title: String,
    val subjectName: String,
    val dueEpochMillis: Long,
    val priority: PriorityLevel,
    val notes: String,
    val isCompleted: Boolean,
    val updatedAtMillis: Long
)

@Serializable
data class ExamItem(
    val id: String,
    val subjectName: String,
    val subjectCode: String,
    val examTitle: String,
    val examEpochMillis: Long,
    val timeSlot: String,
    val venue: String,
    val notes: String
)

@Serializable
data class SyllabusTopic(
    val id: String,
    val subjectName: String,
    val unitNumber: Int,
    val topicTitle: String,
    val isCompleted: Boolean,
    val updatedAtMillis: Long
)

@Serializable
data class CourseGradeEntry(
    val id: String,
    val semester: Int,
    val subjectName: String,
    val credits: Int,
    val gradePointOrMarks: Double
)

@Serializable
data class SemesterGradeSummary(
    val semester: Int,
    val totalCredits: Int,
    val sgpa: Double,
    val courses: List<CourseGradeEntry>
)

@Serializable
data class StudyMaterialItem(
    val id: String,
    val title: String,
    val subjectName: String,
    val semester: Int,
    val type: MaterialType,
    val year: Int,
    val localFilePath: String,
    val mimeType: String,
    val fileSizeBytes: Long,
    val isBookmarked: Boolean,
    val upvotes: Int,
    val addedAtMillis: Long
)

@Serializable
data class DirectoryContactItem(
    val id: String,
    val name: String,
    val designation: String,
    val department: String,
    val category: String,
    val phone: String,
    val email: String,
    val officeLocation: String,
    val officeHours: String,
    val latitude: Double = 30.2224,
    val longitude: Double = 78.7834
)

@Serializable
data class ClubEventPreviewItem(
    val id: String,
    val title: String,
    val clubName: String,
    val category: String,
    val dateTimeLabel: String,
    val venue: String,
    val goingCount: Int,
    val interestedCount: Int,
    val userRsvp: RsvpStatus? = null
)

@Serializable
data class LostFoundPreviewItem(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val location: String,
    val isFoundItem: Boolean,
    val isResolved: Boolean,
    val postedAtMillis: Long
)

@Serializable
data class DepartmentPackSubject(
    val id: String,
    val code: String,
    val name: String,
    val professor: String,
    val semester: Int,
    val targetPercentage: Int = 75,
    val colorHex: String = "#0F4C81"
)

@Serializable
data class DepartmentPackPayload(
    val version: Int = 1,
    val departmentName: String = "Computer Science & Engineering",
    val course: String = "B.Tech CSE",
    val semester: Int = 4,
    val section: String = "A",
    val exportedAtMillis: Long = 0L,
    val subjects: List<DepartmentPackSubject> = emptyList(),
    val timetableSlots: List<TimetableSlot> = emptyList(),
    val notices: List<PersonalNotice> = emptyList(),
    val syllabusTopics: List<SyllabusTopic> = emptyList()
)
