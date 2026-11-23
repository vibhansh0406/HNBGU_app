package `in`.campusone.app.data.repository

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.campusone.app.data.local.dao.AttendanceDao
import `in`.campusone.app.data.local.dao.CampusLifeDao
import `in`.campusone.app.data.local.dao.NoticeDao
import `in`.campusone.app.data.local.dao.StudyDao
import `in`.campusone.app.data.local.dao.TimetableDao
import `in`.campusone.app.data.local.entity.AssignmentEntity
import `in`.campusone.app.data.local.entity.CampusEventEntity
import `in`.campusone.app.data.local.entity.ClubEntity
import `in`.campusone.app.data.local.entity.DirectoryContactEntity
import `in`.campusone.app.data.local.entity.ExamEntity
import `in`.campusone.app.data.local.entity.LostFoundEntity
import `in`.campusone.app.data.local.entity.NoticeEntity
import `in`.campusone.app.data.local.entity.StudyResourceEntity
import `in`.campusone.app.data.local.entity.SubjectEntity
import `in`.campusone.app.data.local.entity.SyllabusTopicEntity
import `in`.campusone.app.data.local.entity.TimetableSlotEntity
import `in`.campusone.app.data.preferences.UserPreferencesRepository
import `in`.campusone.app.data.remote.SupabaseSyncRepository
import `in`.campusone.app.domain.model.UserProfile
import `in`.campusone.app.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

data class AdminUserRecord(
    val uid: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val collegeName: String,
    val courseName: String,
    val semester: Int,
    val section: String,
    val isSuspended: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

data class RoleRequestRecord(
    val id: String,
    val userId: String,
    val email: String,
    val requestedRole: UserRole,
    val note: String,
    val status: String,
    val createdAtMillis: Long
)

data class UserAttendanceRow(
    val subjectId: String,
    val present: Int,
    val absent: Int,
    val cancelled: Int
)

enum class FlaggedKind { RESOURCE, CLUB, EVENT, LOST_FOUND, POST, COMMENT }

data class FlaggedItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val kind: FlaggedKind
)

data class AuditEntry(
    val id: String,
    val actorEmail: String,
    val action: String,
    val targetType: String,
    val targetId: String,
    val detail: String,
    val result: String,
    val createdAtMillis: Long
)

data class ContentReportRecord(
    val id: String,
    val targetType: String,
    val targetId: String,
    val reason: String,
    val reporterEmail: String,
    val status: String,
    val createdAtMillis: Long
)

data class ReportPreview(
    val reportId: String,
    val title: String,
    val body: String
)

data class PlatformSettings(
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "",
    val announcementBanner: String = "",
    val announcementEnabled: Boolean = false,
    val registrationEnabled: Boolean = true,
    val resourceUploadsEnabled: Boolean = true,
    val featureNotices: Boolean = true,
    val featureResources: Boolean = true,
    val featureAssignments: Boolean = true,
    val featureEvents: Boolean = true,
    val featureAttendance: Boolean = true
) {
    fun toMap(): Map<String, Any> = mapOf(
        "maintenance_mode" to maintenanceMode,
        "maintenance_message" to maintenanceMessage,
        "announcement_banner" to announcementBanner,
        "announcement_enabled" to announcementEnabled,
        "registration_enabled" to registrationEnabled,
        "resource_uploads_enabled" to resourceUploadsEnabled,
        "feature_notices" to featureNotices,
        "feature_resources" to featureResources,
        "feature_assignments" to featureAssignments,
        "feature_events" to featureEvents,
        "feature_attendance" to featureAttendance
    )

    companion object {
        fun from(map: Map<String, Any>?): PlatformSettings {
            if (map == null) return PlatformSettings()
            fun bool(key: String, default: Boolean): Boolean = when (val v = map[key]) {
                is Boolean -> v
                is String -> v.equals("true", ignoreCase = true)
                else -> default
            }
            fun str(key: String): String = map[key] as? String ?: ""
            return PlatformSettings(
                maintenanceMode = bool("maintenance_mode", false),
                maintenanceMessage = str("maintenance_message"),
                announcementBanner = str("announcement_banner"),
                announcementEnabled = bool("announcement_enabled", false),
                registrationEnabled = bool("registration_enabled", true),
                resourceUploadsEnabled = bool("resource_uploads_enabled", true),
                featureNotices = bool("feature_notices", true),
                featureResources = bool("feature_resources", true),
                featureAssignments = bool("feature_assignments", true),
                featureEvents = bool("feature_events", true),
                featureAttendance = bool("feature_attendance", true)
            )
        }
    }
}

/** Result of a privileged admin operation. */
data class AdminActionResult(
    val success: Boolean,
    val message: String
)

private val ASSIGNABLE_ROLE_NAMES = setOf("STUDENT", "CR", "TEACHER", "CLUB_ADMIN", "MODERATOR", "ADMIN")

@Singleton
class AdminRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val attendanceDao: AttendanceDao,
    private val timetableDao: TimetableDao,
    private val noticeDao: NoticeDao,
    private val studyDao: StudyDao,
    private val campusLifeDao: CampusLifeDao,
    private val prefs: UserPreferencesRepository,
    private val supabaseSync: SupabaseSyncRepository
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // =========================================================================
    // USERS / PROFILES
    // =========================================================================
    fun observeAllProfiles(): Flow<List<UserProfile>> =
        prefs.userProfile.map { listOf(it) }

    suspend fun saveProfile(profile: UserProfile) {
        prefs.saveUserProfile(profile, markOnboarded = true)
    }

    /**
     * Reads the caller's authoritative identity from the database. The result is
     * never persisted: a cached role would survive revocation.
     */
    suspend fun loadIdentity(): AdminIdentity {
        val state = supabaseSync.fetchAdminState() ?: return AdminIdentity.Anonymous
        return AdminIdentity(
            role = state.role,
            isOwner = state.isOwner,
            isSuspended = state.isSuspended,
            permissions = state.permissions.mapNotNull { AdminPermission.from(it) }.toSet()
        )
    }

    suspend fun loadAllUsers(): List<AdminUserRecord> {
        // Registered auth users that never finished onboarding have no profile
        // row yet, so repair the directory before reading it.
        supabaseSync.syncRegisteredProfiles()
        val profiles = supabaseSync.downloadAllProfiles().mapNotNull { it.toAdminUserRecord() }
        val seenUids = profiles.map { it.uid }.toMutableSet()
        val seenEmails = mutableSetOf<String>()
        profiles.forEach { record ->
            val key = record.email.lowercase()
            if (key.isNotBlank()) seenEmails += key
        }
        val legacy = supabaseSync.downloadLegacyStudents().mapNotNull { map ->
            val uid = map["id"] as? String ?: return@mapNotNull null
            val email = (map["email"] as? String).orEmpty()
            val emailKey = email.lowercase()
            if (uid in seenUids || (emailKey.isNotBlank() && emailKey in seenEmails)) {
                return@mapNotNull null
            }
            seenUids += uid
            if (emailKey.isNotBlank()) seenEmails += emailKey
            AdminUserRecord(
                uid = uid,
                name = (map["full_name"] as? String).orEmpty(),
                email = email,
                role = roleOf(map["role"] as? String),
                collegeName = "",
                courseName = (map["course"] as? String).orEmpty(),
                semester = 1,
                section = "A",
                isSuspended = false,
                createdAtMillis = map["created_at"].toEpochMillis(),
                updatedAtMillis = map["updated_at"].toEpochMillis()
            )
        }
        return (profiles + legacy).sortedByDescending { it.createdAtMillis }
    }

    private fun Map<String, Any>.toAdminUserRecord(): AdminUserRecord? {
        val uid = this["id"] as? String ?: return null
        return AdminUserRecord(
            uid = uid,
            name = this["full_name"] as? String ?: "",
            email = this["email"] as? String ?: "",
            role = roleOf(this["role"] as? String),
            collegeName = this["college_name"] as? String ?: "",
            courseName = this["course_name"] as? String ?: "",
            semester = (this["semester"] as? Number)?.toInt() ?: 1,
            section = this["section"] as? String ?: "A",
            isSuspended = this["is_suspended"] == true,
            createdAtMillis = this["created_at"].toEpochMillis(),
            updatedAtMillis = this["updated_at"].toEpochMillis()
        )
    }

    suspend fun updateUserRole(uid: String, role: UserRole): AdminActionResult {
        if (role.name !in ASSIGNABLE_ROLE_NAMES) {
            return AdminActionResult(false, "Unsupported role")
        }
        val ok = supabaseSync.adminSetRole(uid, role.name)
        return AdminActionResult(ok, if (ok) "Role updated" else "Role change rejected by server")
    }

    suspend fun setUserSuspended(uid: String, suspended: Boolean): AdminActionResult {
        val ok = supabaseSync.adminSetSuspended(uid, suspended)
        return AdminActionResult(ok, if (ok) "Account updated" else "Account update rejected by server")
    }

    suspend fun removeUser(uid: String): AdminActionResult {
        val ok = supabaseSync.adminDeleteUser(uid)
        return AdminActionResult(ok, if (ok) "User removed" else "Removal rejected by server")
    }

    suspend fun loadRoleRequests(): List<RoleRequestRecord> =
        supabaseSync.downloadRoleRequests().mapNotNull { map ->
            val id = map["id"] as? String ?: return@mapNotNull null
            RoleRequestRecord(
                id = id,
                userId = map["user_id"] as? String ?: "",
                email = map["email"] as? String ?: "",
                requestedRole = roleOf(map["requested_role"] as? String),
                note = map["note"] as? String ?: "",
                status = map["status"] as? String ?: "PENDING",
                createdAtMillis = (map["created_at"] as? Number)?.toLong() ?: 0L
            )
        }

    suspend fun approveRoleRequest(request: RoleRequestRecord): AdminActionResult {
        val ok = supabaseSync.adminApproveRoleRequest(request.id)
        return AdminActionResult(ok, if (ok) "Request approved" else "Approval rejected by server")
    }

    suspend fun rejectRoleRequest(request: RoleRequestRecord): AdminActionResult {
        val ok = supabaseSync.adminRejectRoleRequest(request.id)
        return AdminActionResult(ok, if (ok) "Request rejected" else "Could not reject request")
    }

    suspend fun loadUserAttendance(uid: String): List<UserAttendanceRow> =
        supabaseSync.downloadAttendanceLogs(uid)
            .groupBy { it["subject_id"] as? String ?: "" }
            .map { (subjectId, rows) ->
                UserAttendanceRow(
                    subjectId = subjectId,
                    present = rows.count { it["status"] == "PRESENT" },
                    absent = rows.count { it["status"] == "ABSENT" },
                    cancelled = rows.count { it["status"] == "CANCELLED" }
                )
            }
            .sortedBy { it.subjectId }

    suspend fun loadFlaggedContent(): List<FlaggedItem> {
        val resources = supabaseSync.downloadFlaggedResources().mapNotNull { map ->
            val id = map["id"] as? String ?: return@mapNotNull null
            val reports = (map["report_count"] as? Number)?.toInt() ?: 0
            FlaggedItem(
                id = id,
                title = map["title"] as? String ?: "Untitled resource",
                subtitle = "${map["subject_name"] as? String ?: "Subject"} • $reports reports",
                kind = FlaggedKind.RESOURCE
            )
        }
        val clubs = supabaseSync.downloadFlaggedClubs().mapNotNull { map ->
            val id = map["id"] as? String ?: return@mapNotNull null
            FlaggedItem(
                id = id,
                title = map["name"] as? String ?: "Club",
                subtitle = map["category"] as? String ?: "Club",
                kind = FlaggedKind.CLUB
            )
        }
        val events = supabaseSync.downloadFlaggedEvents().mapNotNull { map ->
            val id = map["id"] as? String ?: return@mapNotNull null
            FlaggedItem(
                id = id,
                title = map["title"] as? String ?: "Event",
                subtitle = map["venue"] as? String ?: "Campus event",
                kind = FlaggedKind.EVENT
            )
        }
        val lostFound = supabaseSync.downloadFlaggedLostFound().mapNotNull { map ->
            val id = map["id"] as? String ?: return@mapNotNull null
            val reports = (map["report_count"] as? Number)?.toInt() ?: 0
            FlaggedItem(
                id = id,
                title = map["title"] as? String ?: "Lost & Found item",
                subtitle = "${map["location"] as? String ?: "Campus"} • $reports reports",
                kind = FlaggedKind.LOST_FOUND
            )
        }
        val communityPosts = runCatching {
            supabaseSync.downloadFlaggedCommunityPosts().map { map ->
                val reports = (map["report_count"] as? Number)?.toInt() ?: 0
                FlaggedItem(
                    id = map["id"] as? String ?: "",
                    title = map["title"] as? String ?: "Community post",
                    subtitle = "${map["author_name"] as? String ?: "Student"} • $reports reports",
                    kind = FlaggedKind.POST
                )
            }
        }.getOrDefault(emptyList())
        return resources + clubs + events + lostFound + communityPosts
    }

    suspend fun moderateFlagged(item: FlaggedItem, action: String): AdminActionResult {
        val targetType = when (item.kind) {
            FlaggedKind.RESOURCE -> "RESOURCE"
            FlaggedKind.CLUB -> "CLUB"
            FlaggedKind.EVENT -> "EVENT"
            FlaggedKind.LOST_FOUND -> "LOST_FOUND"
            FlaggedKind.POST -> "POST"
            FlaggedKind.COMMENT -> "COMMENT"
        }
        val ok = if (item.kind == FlaggedKind.POST || item.kind == FlaggedKind.COMMENT) {
            supabaseSync.adminModerateCommunity(targetType, item.id, action)
        } else {
            supabaseSync.adminModerateContent(targetType, item.id, action)
        }
        return AdminActionResult(ok, if (ok) "Moderation applied" else "Moderation rejected by server")
    }

    suspend fun moderateReport(report: ContentReportRecord, action: String): AdminActionResult {
        val ok = if (report.targetType == "POST" || report.targetType == "COMMENT") {
            supabaseSync.adminModerateCommunity(report.targetType, report.targetId, action)
        } else {
            supabaseSync.adminModerateContent(report.targetType, report.targetId, action)
        }
        return AdminActionResult(ok, if (ok) "Report updated" else "Moderation rejected by server")
    }

    suspend fun loadReportPreview(report: ContentReportRecord): ReportPreview {
        val map = when (report.targetType) {
            "POST" -> supabaseSync.downloadCommunityPost(report.targetId)
            "COMMENT" -> supabaseSync.downloadCommunityComment(report.targetId)
            else -> null
        }
        return ReportPreview(
            reportId = report.id,
            title = map?.get("title") as? String
                ?: map?.get("author_name")?.let { "Comment by $it" }
                ?: "${report.targetType} ${report.targetId}",
            body = map?.get("body") as? String ?: report.reason.ifBlank { "No preview available." }
        )
    }

    suspend fun loadContentReports(): List<ContentReportRecord> =
        supabaseSync.downloadContentReports().mapNotNull { map ->
            val id = map["id"] as? String ?: return@mapNotNull null
            ContentReportRecord(
                id = id,
                targetType = map["target_type"] as? String ?: "",
                targetId = map["target_id"] as? String ?: "",
                reason = map["reason"] as? String ?: "",
                reporterEmail = map["reporter_email"] as? String ?: "",
                status = map["status"] as? String ?: "PENDING",
                createdAtMillis = (map["created_at"] as? Number)?.toLong() ?: 0L
            )
        }

    suspend fun loadAuditLogs(): List<AuditEntry> =
        supabaseSync.downloadAuditLogs().mapNotNull { map ->
            val id = map["id"] as? String ?: return@mapNotNull null
            AuditEntry(
                id = id,
                actorEmail = map["actor_email"] as? String ?: "",
                action = map["action"] as? String ?: "",
                targetType = map["target_type"] as? String ?: "",
                targetId = map["target_id"] as? String ?: "",
                detail = (map["detail"] as? Map<String, Any>)?.entries?.joinToString(", ") { (k, v) ->
                    "$k=$v"
                } ?: "",
                result = map["result"] as? String ?: "SUCCESS",
                createdAtMillis = (map["created_at"] as? Number)?.toLong() ?: 0L
            )
        }

    suspend fun loadPlatformSettings(): PlatformSettings =
        PlatformSettings.from(supabaseSync.downloadPlatformSettings())

    suspend fun savePlatformSettings(settings: PlatformSettings): AdminActionResult {
        val ok = supabaseSync.adminUpdateSettings(settings.toMap())
        return AdminActionResult(ok, if (ok) "Settings saved" else "Settings change rejected by server")
    }

    suspend fun recoverOwnerAccess(): AdminActionResult {
        val ok = supabaseSync.ownerRecoverAdmin()
        return AdminActionResult(ok, if (ok) "Owner access restored" else "Recovery refused")
    }

    private fun roleOf(name: String?): UserRole =
        UserRole.entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: UserRole.STUDENT

    private fun Any?.toEpochMillis(): Long = when (this) {
        is Number -> toLong()
        is String -> runCatching { OffsetDateTime.parse(this).toInstant().toEpochMilli() }
            .recoverCatching { Instant.parse(this).toEpochMilli() }
            .getOrDefault(0L)
        else -> 0L
    }

    // =========================================================================
    // SUBJECTS
    // =========================================================================
    fun observeSubjects(sectionKey: String): Flow<List<SubjectEntity>> =
        attendanceDao.observeAllSectionSubjects(sectionKey)

    suspend fun upsertSubjects(subjects: List<SubjectEntity>) =
        attendanceDao.upsertSubjects(subjects)

    suspend fun deleteSubject(subjectId: String) {
        // Room has no delete-by-id on AttendanceDao; mark unenrolled instead
        attendanceDao.setSubjectEnrollment(subjectId, false)
    }

    // =========================================================================
    // TIMETABLE
    // =========================================================================
    fun observeTimetableSlots(sectionKey: String): Flow<List<TimetableSlotEntity>> =
        timetableDao.observeTimetable(sectionKey)

    suspend fun upsertTimetableSlots(slots: List<TimetableSlotEntity>) =
        timetableDao.upsertSlots(slots)

    suspend fun deleteTimetableSlot(slotId: String, sectionKey: String) {
        timetableDao.deleteSlot(slotId)
    }

    // Remote timetable operations (admin only)
    suspend fun publishTimetable(
        courseCode: String,
        courseName: String,
        semester: Int,
        section: String,
        academicYear: String,
        fileName: String,
        rows: List<Map<String, Any>>
    ) = supabaseSync.publishTimetable(courseCode, courseName, semester, section, academicYear, fileName, rows)

    suspend fun listTimetableImports() = supabaseSync.listTimetableImports()

    suspend fun deleteTimetableImport(importId: String) = supabaseSync.deleteTimetableImport(importId)

    // =========================================================================
    // NOTICES
    // =========================================================================
    fun observeNotices(sectionKey: String): Flow<List<NoticeEntity>> =
        noticeDao.observeNotices(sectionKey)

    suspend fun upsertNotices(notices: List<NoticeEntity>) =
        noticeDao.upsertNotices(notices)

    suspend fun deleteNotice(noticeId: String) =
        noticeDao.deleteNotice(noticeId)

    // Remote notice operations (admin only)
    suspend fun saveNoticeRemote(notice: NoticeEntity): Boolean {
        val synced = supabaseSync.uploadNotices(notice.sectionKey, listOf(noticeToRemoteMap(notice)))
        if (!synced) return false

        // Routine notices stay in the shared feed. Official/high-priority notices
        // also send a campus-wide push, with the notice ID as the tap destination.
        if (notice.isOfficial || notice.priority == "HIGH" || notice.priority == "URGENT") {
            val category = if (notice.isOfficial) "announcement" else "notice"
            supabaseSync.sendPushNotification(
                title = notice.title,
                body = notice.body,
                category = category,
                targetType = "NOTICE",
                targetId = notice.id
            )
        }
        return true
    }

    suspend fun deleteNoticeRemote(noticeId: String): Boolean =
        supabaseSync.deleteNotice(noticeId)

    // =========================================================================
    // ASSIGNMENTS
    // =========================================================================
    fun observeAssignments(): Flow<List<AssignmentEntity>> =
        studyDao.observeAssignments()

    suspend fun upsertAssignments(items: List<AssignmentEntity>) =
        studyDao.upsertAssignments(items)

    suspend fun deleteAssignment(id: String) =
        studyDao.deleteAssignment(id)

    // =========================================================================
    // EXAMS
    // =========================================================================
    fun observeExams(): Flow<List<ExamEntity>> =
        studyDao.observeExams()

    suspend fun upsertExams(items: List<ExamEntity>) =
        studyDao.upsertExams(items)

    suspend fun deleteExam(id: String) =
        studyDao.deleteExam(id)

    // =========================================================================
    // SYLLABUS TOPICS
    // =========================================================================
    fun observeSyllabusTopics(): Flow<List<SyllabusTopicEntity>> =
        studyDao.observeSyllabusTopics()

    suspend fun upsertSyllabusTopics(items: List<SyllabusTopicEntity>) =
        studyDao.upsertSyllabusTopics(items)

    suspend fun deleteSyllabusTopic(id: String) =
        studyDao.deleteSyllabusTopic(id)

    // =========================================================================
    // STUDY RESOURCES
    // =========================================================================
    fun observeStudyResources(sectionKey: String): Flow<List<StudyResourceEntity>> =
        studyDao.observeVisibleResources(sectionKey)

    suspend fun upsertStudyResources(items: List<StudyResourceEntity>) =
        studyDao.upsertResources(items)

    suspend fun deleteStudyResource(id: String) =
        studyDao.deleteResourceByCr(id)

    // =========================================================================
    // CLUBS
    // =========================================================================
    fun observeClubs(collegeId: String): Flow<List<ClubEntity>> =
        campusLifeDao.observeClubs(collegeId)

    suspend fun upsertClubs(items: List<ClubEntity>) =
        campusLifeDao.upsertClubs(items)

    suspend fun approveClub(clubId: String) =
        campusLifeDao.approveClubByCr(clubId)

    suspend fun deleteClub(clubId: String) =
        campusLifeDao.deleteClub(clubId)

    // =========================================================================
    // EVENTS
    // =========================================================================
    fun observeEvents(collegeId: String): Flow<List<CampusEventEntity>> =
        campusLifeDao.observeEvents(collegeId)

    suspend fun upsertEvents(items: List<CampusEventEntity>) =
        campusLifeDao.upsertEvents(items)

    suspend fun approveEvent(eventId: String) =
        campusLifeDao.approveEventByCr(eventId)

    suspend fun deleteEvent(eventId: String) =
        campusLifeDao.deleteEvent(eventId)

    // =========================================================================
    // LOST & FOUND
    // =========================================================================
    fun observeLostFound(collegeId: String): Flow<List<LostFoundEntity>> =
        campusLifeDao.observeLostFound(collegeId)

    suspend fun upsertLostFound(items: List<LostFoundEntity>) =
        campusLifeDao.upsertLostFound(items)

    suspend fun resolveLostFound(id: String) =
        campusLifeDao.markLostFoundResolved(id)

    suspend fun hideLostFound(id: String) =
        campusLifeDao.hideLostFoundByCr(id)

    // =========================================================================
    // DIRECTORY
    // =========================================================================
    fun observeDirectory(collegeId: String): Flow<List<DirectoryContactEntity>> =
        campusLifeDao.observeDirectory(collegeId)

    suspend fun upsertDirectory(items: List<DirectoryContactEntity>) =
        campusLifeDao.upsertDirectory(items)

    // =========================================================================
    // DASHBOARD STATS
    // =========================================================================
    suspend fun getSubjectCount(sectionKey: String): Int =
        attendanceDao.observeAllSectionSubjects(sectionKey).first().size

    suspend fun getNoticeCount(sectionKey: String): Int =
        noticeDao.observeNotices(sectionKey).first().size

    suspend fun getAssignmentCount(): Int =
        studyDao.observeAssignments().first().size

    suspend fun getExamCount(): Int =
        studyDao.observeExams().first().size

    suspend fun getClubCount(collegeId: String): Int =
        campusLifeDao.observeClubs(collegeId).first().size

    suspend fun getEventCount(collegeId: String): Int =
        campusLifeDao.observeEvents(collegeId).first().size


private fun noticeToRemoteMap(notice: NoticeEntity): Map<String, Any> = mapOf<String, Any>(
    "id" to notice.id,
    "section_key" to notice.sectionKey,
    "title" to notice.title,
    "body" to notice.body,
    "attachment_url" to notice.attachmentUrl.orEmpty(),
    "attachment_type" to notice.attachmentType.name,
    "is_pinned" to notice.isPinned,
    "category" to notice.category,
    "priority" to notice.priority,
    "is_official" to notice.isOfficial,
    "author_uid" to notice.authorUid,
    "author_name" to notice.authorName,
    "author_role" to notice.authorRole.name,
    "created_at" to notice.createdAtMillis,
    "is_read" to notice.isRead
)
}
