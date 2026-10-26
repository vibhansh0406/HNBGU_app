package `in`.campusone.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "study_resources",
    indices = [
        Index(value = ["sectionKey", "subjectId", "semester"]),
        Index(value = ["isBookmarkedOffline"])
    ]
)
data class StudyResourceEntity(
    @PrimaryKey val id: String,
    val sectionKey: String,
    val subjectId: String,
    val subjectName: String,
    val semester: Int,
    val title: String,
    val category: String, // "NOTES", "PYQ", "ASSIGNMENT", "OTHER"
    val examYear: Int = 2025,
    val fileUrl: String,
    val localFilePath: String? = null,
    val fileType: String, // "PDF" or "IMAGE"
    val fileSizeBytes: Long = 1_450_000L,
    val uploaderUid: String,
    val uploaderName: String,
    val showUploaderName: Boolean = true,
    val upvotes: Int = 0,
    val isUpvotedByMe: Boolean = false,
    val isBookmarkedOffline: Boolean = false,
    val reportCount: Int = 0,
    val isFlagged: Boolean = false,
    val isHidden: Boolean = false,
    val isApprovedByCr: Boolean = true,
    val uploadStatus: String = "COMPLETED",
    val uploadProgressPct: Int = 100,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "assignments",
    indices = [
        Index(value = ["dueDateEpochMillis", "isCompleted"]),
        Index(value = ["sharedSourceId"])
    ]
)
data class AssignmentEntity(
    @PrimaryKey val id: String,
    val sectionKey: String = "hnbgu_bsc_cs_sem3_secA",
    val subjectId: String,
    val subjectTag: String,
    val title: String,
    val description: String,
    val priority: String = "MEDIUM", // "HIGH", "MEDIUM", "LOW"
    val dueDateEpochMillis: Long,
    val dueDateFormatted: String,
    val reminderEnabled: Boolean = true,
    val remind1DayBefore: Boolean = true,
    val remind2HoursBefore: Boolean = true,
    val isCompleted: Boolean = false,
    val isSharedByCrOrTeacher: Boolean = false,
    val isAddedToPersonalList: Boolean = true,
    val sharedSourceId: String? = null,
    val authorName: String = "Self",
    val isSynced: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey val id: String,
    val sectionKey: String = "hnbgu_bsc_cs_sem3_secA",
    val subjectId: String,
    val subjectName: String,
    val subjectCode: String,
    val examTitle: String,
    val examDateEpochMillis: Long,
    val examDateIso: String = "2026-11-14",
    val examTimeFormatted: String = "10:00 AM – 01:00 PM",
    val examDateFormatted: String,
    val roomOrCenter: String,
    val reminderEnabled: Boolean = true,
    val publishedByCrOrTeacher: Boolean = true,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "syllabus_topics",
    indices = [Index(value = ["subjectId", "unitNumber"])]
)
data class SyllabusTopicEntity(
    @PrimaryKey val id: String,
    val sectionKey: String = "hnbgu_bsc_cs_sem3_secA",
    val subjectId: String,
    val subjectName: String,
    val unitNumber: Int,
    val unitTitle: String = "Unit 1",
    val topicTitle: String,
    val isCompleted: Boolean = false,
    val publishedByCrOrTeacher: Boolean = true,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "gpa_semesters")
data class GpaSemesterEntity(
    @PrimaryKey val semesterNumber: Int,
    val scaleType: String = "TEN_POINT_INDIAN",
    val totalCredits: Int,
    val sgpa: Double,
    val coursesJson: String,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

// =========================================================================
// PHASE 3 — CAMPUS LIFE ENTITIES
// =========================================================================

@Entity(
    tableName = "clubs",
    indices = [Index(value = ["collegeId", "category"])]
)
data class ClubEntity(
    @PrimaryKey val id: String,
    val collegeId: String,
    val name: String,
    val logoEmojiOrUrl: String = "💻",
    val description: String,
    val category: String, // "TECH", "CULTURAL", "SPORTS", "LITERARY", "SOCIAL"
    val memberCount: Int = 100,
    val contactEmail: String,
    val socialLink: String,
    val adminUid: String,
    val adminName: String,
    val isFollowedByMe: Boolean = false,
    val isApproved: Boolean = true, // Moderation: new clubs require CR/Teacher approval
    val reportCount: Int = 0,
    val isFlagged: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "campus_events",
    indices = [Index(value = ["collegeId", "clubId", "eventEpochMillis"])]
)
data class CampusEventEntity(
    @PrimaryKey val id: String,
    val collegeId: String,
    val clubId: String = "club_gdsc_hnbgu",
    val clubName: String,
    val category: String = "TECH", // "TECH", "CULTURAL", "SPORTS", "WORKSHOP"
    val title: String,
    val description: String,
    val posterUrl: String? = null,
    val venue: String,
    val eventDateFormatted: String,
    val eventEpochMillis: Long,
    val goingCount: Int = 0,
    val interestedCount: Int = 0,
    val notGoingCount: Int = 0,
    val myRsvpStatus: String = "NONE", // "NONE", "GOING", "INTERESTED", "NOT_GOING"
    val reminderLeadMinutes: Int = 60, // Configurable event reminder (e.g., 60m before)
    val isApproved: Boolean = true,     // Moderation: new events require CR/Teacher approval
    val reportCount: Int = 0,
    val isFlagged: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "lost_found_items",
    indices = [Index(value = ["collegeId", "status", "createdAtMillis"])]
)
data class LostFoundEntity(
    @PrimaryKey val id: String,
    val collegeId: String,
    val type: String, // "LOST" or "FOUND"
    val category: String = "ELECTRONICS", // "ELECTRONICS", "ID_CARD", "BOOKS", "KEYS", "OTHER"
    val itemTitle: String,
    val description: String,
    val photoUri: String? = null, // Compressed before upload (<= 600 KB)
    val location: String,
    val dateFormatted: String = "Today",
    val posterUid: String = "uid_poster",
    val posterName: String,
    val preferredContactMethod: String, // Private! Only revealed when a Claim/Contact Request is ACCEPTED
    val status: String = "OPEN", // "OPEN", "RESOLVED", "ARCHIVED"
    val isResolved: Boolean = false,
    val isArchived: Boolean = false, // Auto-archived after 30 days
    val isHidden: Boolean = false,   // CR Moderation
    val reportCount: Int = 0,
    val isFlagged: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "lost_found_contact_requests",
    indices = [Index(value = ["itemId", "requesterUid"])]
)
data class LostFoundContactRequestEntity(
    @PrimaryKey val id: String,
    val itemId: String,
    val itemTitle: String,
    val posterUid: String,
    val requesterUid: String,
    val requesterName: String,
    val message: String, // e.g., "Hi, I have my roll number sticker on the back of this calculator"
    val status: String = "PENDING", // "PENDING", "ACCEPTED", "IGNORED"
    val revealedContactInfo: String? = null, // Populated once poster accepts
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "directory_contacts",
    indices = [Index(value = ["collegeId", "category", "department"])]
)
data class DirectoryContactEntity(
    @PrimaryKey val id: String,
    val collegeId: String,
    val category: String = "FACULTY", // "FACULTY", "IMPORTANT_OFFICE", "EMERGENCY", "HOSTEL_MEDICAL"
    val department: String,
    val name: String,
    val designation: String,
    val phone: String,
    val email: String,
    val officeLocation: String = "Chauras Campus",
    val officeHours: String,
    val isEmergencyOrImportant: Boolean = false
)

@Entity(tableName = "directory_suggestions")
data class DirectorySuggestionEntity(
    @PrimaryKey val id: String,
    val collegeId: String,
    val contactId: String,
    val contactName: String,
    val suggesterUid: String,
    val suggesterName: String,
    val proposedChange: String, // e.g., "Updated Office Hours: Mon-Fri 2:00 PM - 4:00 PM, Room 204"
    val status: String = "PENDING", // "PENDING", "APPROVED", "REJECTED"
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "campus_map_places")
data class CampusMapPlaceEntity(
    @PrimaryKey val id: String,
    val collegeId: String,
    val name: String,
    val category: String, // "LIBRARY", "CANTEEN", "DEPARTMENT", "HOSTEL", "ADMIN", "MEDICAL"
    val campusName: String, // "Chauras Campus", "Birla Campus", "BGR Pauri"
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val mapQueryLabel: String
)
