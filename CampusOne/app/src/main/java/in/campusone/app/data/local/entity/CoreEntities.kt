package `in`.campusone.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import `in`.campusone.app.domain.model.AttachmentType
import `in`.campusone.app.domain.model.AttendanceStatus
import `in`.campusone.app.domain.model.UserRole

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val subjectId: String,
    val sectionKey: String,
    val subjectName: String,
    val subjectCode: String,
    val teacherName: String,
    val isElective: Boolean = false,
    val isEnrolled: Boolean = true,
    val credits: Int = 4
)

@Entity(
    tableName = "attendance_logs",
    indices = [
        Index(value = ["subjectId", "dateIso", "slotId"], unique = true),
        Index(value = ["dateEpochDay"])
    ]
)
data class AttendanceLogEntity(
    @PrimaryKey val id: String, // Deterministic: "${subjectId}_${dateIso}_${slotId}" prevents duplicates
    val subjectId: String,
    val dateEpochDay: Long,
    val dateIso: String,
    val slotId: String,
    val status: AttendanceStatus,
    val note: String = "",
    val isSynced: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "timetable_slots",
    indices = [Index(value = ["sectionKey", "dayOfWeek"])]
)
data class TimetableSlotEntity(
    @PrimaryKey val id: String,
    val sectionKey: String,
    val subjectId: String,
    val subjectName: String,
    val subjectCode: String,
    val teacherName: String,
    val roomNumber: String,
    val dayOfWeek: Int, // 1 = Monday .. 7 = Sunday
    val startTime: String, // "09:00"
    val endTime: String,   // "10:00"
    val isElective: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "notices",
    indices = [Index(value = ["sectionKey", "isPinned", "createdAtMillis"])]
)
data class NoticeEntity(
    @PrimaryKey val id: String,
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
