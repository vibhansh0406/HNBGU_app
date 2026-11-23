package `in`.campusone.app.data.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.campusone.app.data.local.dao.AttendanceDao
import `in`.campusone.app.data.local.dao.NoticeDao
import `in`.campusone.app.data.local.dao.TimetableDao
import `in`.campusone.app.data.remote.AuthManager
import `in`.campusone.app.data.remote.FileStore
import `in`.campusone.app.data.remote.StorageBuckets
import `in`.campusone.app.data.remote.SupabaseClient
import `in`.campusone.app.data.remote.SupabaseSyncRepository
import `in`.campusone.app.data.local.entity.AttendanceLogEntity
import `in`.campusone.app.data.local.entity.NoticeEntity
import `in`.campusone.app.data.local.entity.SubjectEntity
import `in`.campusone.app.data.local.entity.TimetableSlotEntity
import `in`.campusone.app.data.preferences.UserPreferencesRepository
import `in`.campusone.app.domain.model.AttachmentType
import `in`.campusone.app.domain.model.AttendanceStatus
import `in`.campusone.app.domain.model.NoticeItem
import `in`.campusone.app.domain.model.SubjectAttendanceSummary
import `in`.campusone.app.domain.model.TimetableSlot
import `in`.campusone.app.domain.model.UserProfile
import `in`.campusone.app.domain.model.UserRole
import `in`.campusone.app.util.TimestampUtils
import `in`.campusone.app.util.security.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoreCampusRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val attendanceDao: AttendanceDao,
    private val timetableDao: TimetableDao,
    private val noticeDao: NoticeDao,
    private val prefs: UserPreferencesRepository,
    private val supabaseClient: SupabaseClient,
    private val supabaseSync: SupabaseSyncRepository,
    private val authManager: AuthManager,
    private val fileStore: FileStore
) {

    companion object {
        const val OWNER_EMAIL = "vvibhansh@gmail.com"
        private const val SYNC_THROTTLE_MILLIS = 60_000L
    }

    private val syncMutex = Mutex()
    private var lastSyncAtMillis = 0L
    private var lastSyncSectionKey = ""

    suspend fun syncProfileToCloud(profile: UserProfile): Result<Unit> {
        if (!supabaseSync.isSyncedEnabled()) return Result.success(Unit)
        return runCatching {
            // The row must exist before it can be updated: a plain UPDATE on a
            // missing row silently matches nothing. bootstrap_profile is
            // SECURITY DEFINER so it can insert even without an INSERT policy.
            val bootstrapped = supabaseSync.bootstrapProfile(profile.name, profile.email)
            if (!bootstrapped) {
                throw IllegalStateException("Profile bootstrap failed on server")
            }
            val uploaded = supabaseSync.updateAcademicProfile(
                uid = profile.uid,
                name = profile.name,
                collegeName = profile.collegeName,
                courseId = profile.courseId,
                courseName = profile.courseName,
                semester = profile.semester,
                section = profile.section
            )
            if (!uploaded) {
                throw IllegalStateException("Profile update failed on server")
            }
            Unit
        }
    }

    suspend fun requestRoleChange(role: UserRole, note: String): Result<Unit> {
        val profile = prefs.userProfile.first()
        val uid = authManager.currentUserId()
            ?: return Result.failure(IllegalStateException("Sign in to send a role request"))
        if (role == UserRole.STUDENT || role == UserRole.ADMIN) {
            return Result.failure(IllegalArgumentException("Choose a staff role"))
        }
        return if (supabaseSync.submitRoleRequest(uid, profile.email, role.name, note)) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Could not send role request"))
        }
    }

    suspend fun fetchRemoteProfile(uid: String): UserProfile? {
        val remote = supabaseSync.downloadProfile(uid) ?: return null
        val courseId = remote["course_id"] as? String ?: ""
        val remoteRole = remote["role"] as? String ?: ""
        val isOwner = (remote["email"] as? String)?.equals(OWNER_EMAIL, ignoreCase = true) == true
        val role = when {
            isOwner -> UserRole.ADMIN
            remoteRole == "SUPER_ADMIN" -> UserRole.ADMIN
            else -> UserRole.entries.firstOrNull { it.name == remoteRole } ?: UserRole.STUDENT
        }
        return UserProfile(
            uid = uid,
            name = remote["full_name"] as? String ?: "",
            email = remote["email"] as? String ?: "",
            role = role,
            verifiedRole = true,
            collegeId = remote["college_id"] as? String ?: "hnbgu",
            collegeName = remote["college_name"] as? String ?: "",
            courseId = courseId,
            courseName = remote["course_name"] as? String ?: "",
            semester = (remote["semester"] as? Number)?.toInt() ?: 3,
            section = remote["section"] as? String ?: "A",
            moderatorScope = remote["moderator_scope"] as? String ?: "",
            onboardingCompleted = remote["onboarding_completed"] as? Boolean ?: false
        )
    }

    /**
     * Saves only editable student profile fields. Permissions and account identity
     * remain server-controlled and cannot be changed by this profile form.
     */
    suspend fun updateAcademicProfile(profile: UserProfile): Result<Unit> {
        val uid = authManager.currentUserId()
            ?: return Result.failure(IllegalStateException("Sign in to update your profile"))
        if (profile.uid != uid) {
            return Result.failure(IllegalStateException("Profile does not belong to the signed-in account"))
        }
        return runCatching {
            val updated = supabaseSync.updateAcademicProfile(
                uid = uid,
                name = profile.name,
                collegeName = profile.collegeName,
                courseId = profile.courseId,
                courseName = profile.courseName,
                semester = profile.semester,
                section = profile.section
            )
            if (!updated) throw IllegalStateException("Could not save profile to server")
        }
    }

    suspend fun ensureSeededForSection(
        sectionKey: String,
        forceRefresh: Boolean = false
    ): Result<Unit> {
        if (!supabaseSync.isSyncedEnabled()) return Result.success(Unit)
        return syncMutex.withLock {
            val now = System.currentTimeMillis()
            if (!forceRefresh &&
                sectionKey == lastSyncSectionKey &&
                now - lastSyncAtMillis < SYNC_THROTTLE_MILLIS
            ) {
                return@withLock Result.success(Unit)
            }
            runCatching { seedSectionData(sectionKey) }.also {
                lastSyncAtMillis = System.currentTimeMillis()
                lastSyncSectionKey = sectionKey
            }
        }
    }

    private suspend fun seedSectionData(sectionKey: String) {
        val profile = prefs.userProfile.first()
        if (profile.sectionKey != sectionKey) return
        if (!supabaseSync.isSyncedEnabled()) return

        runCatching { authManager.ensureFreshToken() }
        if (!supabaseSync.isSyncedEnabled()) return

        // Download timetable independently — failure should not block notices sync
        val remoteTimetable: List<Map<String, Any>> = supabaseSync.downloadStudentTimetable(
            courseId = profile.courseId,
            courseName = profile.courseName,
            semester = profile.semester,
            section = profile.section
        ).getOrDefault(emptyList())

        val slots = remoteTimetable.map { map ->
            TimetableSlotEntity(
                id = map["id"] as? String ?: "slot_${System.currentTimeMillis()}",
                sectionKey = sectionKey,
                subjectId = map["subject_id"] as? String ?: "",
                subjectName = map["subject_name"] as? String ?: "",
                subjectCode = map["subject_code"] as? String ?: "",
                teacherName = map["teacher_name"] as? String ?: "",
                roomNumber = map["room_number"] as? String ?: "",
                dayOfWeek = (map["day_of_week"] as? Number)?.toInt() ?: 1,
                startTime = map["start_time"] as? String ?: "09:00",
                endTime = map["end_time"] as? String ?: "10:00",
                isElective = map["is_elective"] == true,
                updatedAtMillis = System.currentTimeMillis()
            )
        }
        // A successful empty response means the administrator has not published
        // this course yet: clear stale rows instead of showing a hardcoded table.
        timetableDao.replaceSectionTimetable(sectionKey, slots)

        val subjects = slots
            .filter { it.subjectId.isNotBlank() }
            .distinctBy { it.subjectId }
            .map { slot ->
                SubjectEntity(
                    subjectId = slot.subjectId,
                    sectionKey = sectionKey,
                    subjectName = slot.subjectName,
                    subjectCode = slot.subjectCode,
                    teacherName = slot.teacherName,
                    isElective = slot.isElective,
                    isEnrolled = true
                )
            }
        if (subjects.isNotEmpty()) attendanceDao.upsertSubjects(subjects)

        // Download notices independently — failure should not block timetable sync
        val sectionNotices: List<Map<String, Any>> = supabaseSync.downloadNotices(sectionKey).getOrDefault(emptyList())
        val globalNotices: List<Map<String, Any>> = supabaseSync.downloadNotices("global").getOrDefault(emptyList())
        val remoteNotices = (sectionNotices + globalNotices).distinctBy { it["id"] }
        val readIds = noticeDao.getReadNoticeIds(sectionKey).toSet()
        noticeDao.replaceSectionNotices(sectionKey, remoteNotices.map { map ->
            NoticeEntity(
                id = map["id"] as? String ?: "notice_${System.currentTimeMillis()}",
                sectionKey = sectionKey,
                title = map["title"] as? String ?: "",
                body = map["body"] as? String ?: "",
                attachmentUrl = map["attachment_url"] as? String?,
                attachmentType = runCatchingDefault({
                    AttachmentType.valueOf(map["attachment_type"] as? String ?: "NONE")
                }, AttachmentType.NONE),
                isPinned = map["is_pinned"] == true,
                category = map["category"] as? String ?: "GENERAL",
                priority = map["priority"] as? String ?: "NORMAL",
                isOfficial = map["is_official"] == true,
                authorUid = map["author_uid"] as? String ?: "",
                authorName = map["author_name"] as? String ?: "Faculty",
                authorRole = runCatchingDefault({
                    UserRole.valueOf(map["author_role"] as? String ?: "CR")
                }, UserRole.CR),
                createdAtMillis = TimestampUtils.toEpochMillis(map["created_at"]) ?: System.currentTimeMillis(),
                isRead = map["is_read"] == true || map["id"] in readIds
            )
        })

        restoreAttendanceFromCloud()
    }

    private suspend fun restoreAttendanceFromCloud() {
        val uid = authManager.currentUserId() ?: return
        val profile = prefs.userProfile.first()
        if (!profile.syncAttendanceToCloud) return
        if (attendanceDao.getAttendanceLogsSnapshot().isNotEmpty()) return
        val remoteLogs = supabaseSync.downloadAttendanceLogs(uid)
        if (remoteLogs.isEmpty()) return
        val entities = remoteLogs.mapNotNull { map ->
            val id = map["id"] as? String ?: return@mapNotNull null
            val subjectId = map["subject_id"] as? String ?: return@mapNotNull null
            val dateIso = map["date_iso"] as? String ?: return@mapNotNull null
            val epochDay = runCatching { LocalDate.parse(dateIso).toEpochDay() }.getOrNull()
                ?: return@mapNotNull null
            AttendanceLogEntity(
                id = id,
                subjectId = subjectId,
                dateEpochDay = epochDay,
                dateIso = dateIso,
                slotId = id.removePrefix("${subjectId}_${dateIso}_").ifBlank { "restored" },
                status = runCatchingDefault(
                    { AttendanceStatus.valueOf(map["status"] as? String ?: "PRESENT") },
                    AttendanceStatus.PRESENT
                ),
                note = map["note"] as? String ?: "",
                isSynced = true,
                updatedAtMillis = (map["updated_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
        if (entities.isNotEmpty()) {
            attendanceDao.upsertAttendanceLogs(entities)
        }
    }

    private fun <T> runCatchingDefault(block: () -> T, default: T): T =
        runCatching { block() }.getOrDefault(default)

    /**
     * Reactive stream of per-subject attendance summaries computed from local Room DB logs.
     */
    fun observeSubjectAttendanceSummaries(sectionKey: String): Flow<List<SubjectAttendanceSummary>> {
        return combine(
            attendanceDao.observeEnrolledSubjects(sectionKey),
            attendanceDao.observeAllLogs()
        ) { subjects, logs ->
            val logsBySubject = logs.groupBy { it.subjectId }
            subjects.map { subj ->
                val subjLogs = logsBySubject[subj.subjectId].orEmpty()
                val attended = subjLogs.count { it.status == AttendanceStatus.PRESENT }
                val absent = subjLogs.count { it.status == AttendanceStatus.ABSENT }
                val cancelled = subjLogs.count { it.status == AttendanceStatus.CANCELLED }
                SubjectAttendanceSummary(
                    subjectId = subj.subjectId,
                    subjectName = subj.subjectName,
                    subjectCode = subj.subjectCode,
                    isElective = subj.isElective,
                    attendedCount = attended,
                    totalClasses = attended + absent, // Cancelled classes never inflate total conducted
                    cancelledCount = cancelled
                )
            }
        }
    }

    fun observeAttendanceLogsForSubject(subjectId: String): Flow<List<AttendanceLogEntity>> =
        attendanceDao.observeLogsForSubject(subjectId)

    fun observeAllAttendanceLogs(): Flow<List<AttendanceLogEntity>> =
        attendanceDao.observeAllLogs()

    /**
     * Marks attendance for a subject slot on a given date.
     * Uses deterministic primary key `${subjectId}_${dateIso}_${slotId}` to prevent duplicate entries.
     */
    suspend fun markAttendance(
        subjectId: String,
        date: LocalDate = LocalDate.now(),
        slotId: String = "manual_${System.currentTimeMillis() / 3600000}",
        status: AttendanceStatus,
        note: String = ""
    ) {
        val dateIso = date.toString()
        val deterministicId = "${subjectId}_${dateIso}_${slotId}"
        val entity = AttendanceLogEntity(
            id = deterministicId,
            subjectId = subjectId,
            dateEpochDay = date.toEpochDay(),
            dateIso = dateIso,
            slotId = slotId,
            status = status,
            note = note,
            isSynced = false,
            updatedAtMillis = System.currentTimeMillis()
        )
        attendanceDao.upsertAttendanceLog(entity)
        runCatching {
            val profile = prefs.userProfile.first()
            if (!profile.syncAttendanceToCloud || !supabaseSync.isSyncedEnabled()) return@runCatching
            val uid = authManager.currentUserId() ?: return@runCatching
            supabaseClient.from("attendance_logs").upsert(listOf(mapOf<String, Any>(
                "id" to entity.id,
                "user_id" to uid,
                "subject_id" to entity.subjectId,
                "date_iso" to entity.dateIso,
                "status" to entity.status.name,
                "note" to entity.note,
                "updated_at" to entity.updatedAtMillis
            )))
            attendanceDao.markLogsSynced(listOf(entity.id))
        }
    }

    fun observeTimetable(sectionKey: String): Flow<List<TimetableSlot>> {
        return timetableDao.observeTimetable(sectionKey).combine(
            attendanceDao.observeEnrolledSubjects(sectionKey)
        ) { slots, enrolledSubjects ->
            val enrolledIds = enrolledSubjects.map { it.subjectId }.toSet()
            slots.filter { !it.isElective || it.subjectId in enrolledIds }.map { entity ->
                TimetableSlot(
                    id = entity.id,
                    subjectId = entity.subjectId,
                    subjectName = entity.subjectName,
                    subjectCode = entity.subjectCode,
                    teacherName = entity.teacherName,
                    roomNumber = entity.roomNumber,
                    dayOfWeek = entity.dayOfWeek,
                    startTime = entity.startTime,
                    endTime = entity.endTime,
                    isElective = entity.isElective
                )
            }
        }
    }

    fun observeNotices(sectionKey: String): Flow<List<NoticeItem>> {
        return noticeDao.observeNotices(sectionKey).combine(prefs.userProfile) { list, _ ->
            list.map { entity ->
                NoticeItem(
                    id = entity.id,
                    sectionKey = entity.sectionKey,
                    title = entity.title,
                    body = entity.body,
                    attachmentUrl = entity.attachmentUrl,
                    attachmentType = entity.attachmentType,
                    isPinned = entity.isPinned,
                    category = entity.category,
                    priority = entity.priority,
                    isOfficial = entity.isOfficial,
                    authorUid = entity.authorUid,
                    authorName = entity.authorName,
                    authorRole = entity.authorRole,
                    createdAtMillis = entity.createdAtMillis,
                    isRead = entity.isRead
                )
            }
        }
    }

    fun observeUnreadNoticeCount(sectionKey: String): Flow<Int> =
        noticeDao.observeUnreadCount(sectionKey)

    suspend fun markNoticeRead(noticeId: String) {
        noticeDao.markNoticeAsRead(noticeId)
    }

    suspend fun markAllNoticesRead(sectionKey: String) {
        noticeDao.markAllAsRead(sectionKey)
    }

    /**
     * Upload a notice attachment into the existing authenticated CampusOne storage bucket.
     * The uploaded URL is saved with the notice row so every user sees the same attachment.
     */
    suspend fun uploadNoticeAttachment(contentUri: String, mimeType: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val uid = authManager.currentUserId()?.takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("Sign in before uploading an attachment")
                val uri = Uri.parse(contentUri)
                val suppliedType = mimeType.substringBefore(';').trim().lowercase()
                val isPdf = suppliedType == "application/pdf" ||
                    uri.lastPathSegment?.endsWith(".pdf", ignoreCase = true) == true
                val isImage = suppliedType.startsWith("image/")
                require(isPdf || isImage) { "Choose a PDF or image file" }

                val contentType = if (isPdf) "application/pdf" else suppliedType.ifBlank { "image/jpeg" }
                val extension = if (isPdf) {
                    "pdf"
                } else {
                    when (contentType.substringAfter('/', "jpeg").substringBefore('+')) {
                        "jpeg", "jpg" -> "jpg"
                        "png" -> "png"
                        "webp" -> "webp"
                        else -> "img"
                    }
                }
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("The selected file could not be read")
                require(bytes.isNotEmpty()) { "The selected file is empty" }
                require(bytes.size <= 10 * 1024 * 1024) { "Notice attachments must be 10 MB or smaller" }

                val path = "notices/$uid/notice_${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}.$extension"
                fileStore.upload(
                    bucket = StorageBuckets.STUDY_RESOURCES,
                    path = path,
                    bytes = bytes,
                    contentType = contentType
                ) { }.getOrThrow()
            }
        }

    /**
     * CR / Teacher action: Post a new announcement to the section Notice Board.
     * The creator identity is derived from the active Supabase authenticated
     * user UUID (auth.uid()), never from a cached profile ID, so a user can
     * never impersonate another uploader.
     */
    suspend fun postNotice(
        profile: UserProfile,
        title: String,
        body: String,
        attachmentUrl: String?,
        attachmentType: AttachmentType,
        isPinned: Boolean,
        category: String = "GENERAL",
        priority: String = "NORMAL",
        isOfficial: Boolean = false
    ): kotlin.Result<Unit> = runCatching {
        require(
            profile.role == UserRole.CR ||
                profile.role == UserRole.ADMIN ||
                profile.role == UserRole.MODERATOR
        ) {
            "Only CRs, Moderators or Admins can post announcements."
        }
        require(!isOfficial || profile.role == UserRole.ADMIN) {
            "Only administrators can publish official announcements."
        }
        val targetSectionKey = if (profile.role == UserRole.ADMIN || profile.role == UserRole.MODERATOR) "global" else profile.sectionKey
        val now = System.currentTimeMillis()
        val noticeId = java.util.UUID.randomUUID().toString()
        // Derive creator identity from the active Supabase authenticated user UUID,
        // never from a stale cached profile ID. This prevents impersonation.
        val authUid = authManager.currentUserId() ?: throw IllegalStateException("Not signed in to Supabase")
        val entity = NoticeEntity(
            id = noticeId,
            sectionKey = targetSectionKey,
            title = title.trim(),
            body = body.trim(),
            attachmentUrl = attachmentUrl?.takeIf { it.isNotBlank() },
            attachmentType = attachmentType,
            isPinned = isPinned,
            category = category,
            priority = priority,
            isOfficial = isOfficial,
            authorUid = authUid,
            authorName = profile.name.ifBlank { "CampusOne" },
            authorRole = profile.role,
            createdAtMillis = now,
            isRead = true
        )
        noticeDao.upsertNotices(listOf(entity))

        // Supabase is the shared source of truth for notices.
        if (supabaseSync.isSyncedEnabled()) {
            val uploaded = supabaseSync.uploadNotices(targetSectionKey, listOf(mapOf<String, Any>(
                "id" to entity.id,
                "section_key" to targetSectionKey,
                "title" to entity.title,
                "body" to entity.body,
                "attachment_url" to (entity.attachmentUrl ?: ""),
                "attachment_type" to entity.attachmentType.name,
                "is_pinned" to entity.isPinned,
                "category" to entity.category,
                "priority" to entity.priority,
                "is_official" to entity.isOfficial,
                "author_uid" to authUid,
                "author_name" to entity.authorName,
                "author_role" to entity.authorRole.name,
                "created_at" to entity.createdAtMillis,
                "updated_at" to now,
                "is_read" to entity.isRead
            )))
            check(uploaded) { "Notice could not be published to the shared campus feed" }

            // Admin-published notices are time-sensitive for students; send a
            // section-scoped push for every notice, not only pinned/high-priority ones.
            val shouldPush = profile.role == UserRole.ADMIN
            if (shouldPush) {
                supabaseSync.sendPushNotification(
                    title = entity.title,
                    body = entity.body,
                    category = if (isOfficial) "announcement" else "notice",
                    targetType = "NOTICE",
                    targetId = entity.id,
                    sectionKey = targetSectionKey
                )
            }
        }
    }

    /**
     * Background delta sync invoked by WorkManager when network is connected.
     * Syncs unsynced attendance logs to Supabase.
     */
    suspend fun performDeltaSync() {
        val profile = prefs.userProfile.first()
        val sectionKey = profile.sectionKey

        // Push unsynced personal attendance logs to Supabase
        if (profile.syncAttendanceToCloud && profile.uid.isNotBlank() && supabaseSync.isSyncedEnabled()) {
            val unsynced = attendanceDao.getUnsyncedLogs()
            if (unsynced.isNotEmpty()) {
                val uid = authManager.currentUserId()
                if (uid != null) {
                    supabaseClient.from("attendance_logs").upsert(
                        unsynced.map { log ->
                            mapOf(
                                "id" to log.id,
                                "user_id" to uid,
                                "subject_id" to log.subjectId,
                                "date_iso" to log.dateIso,
                                "status" to log.status.name,
                                "note" to log.note,
                                "updated_at" to log.updatedAtMillis
                            )
                        }
                    )
                    attendanceDao.markLogsSynced(unsynced.map { it.id })
                }
            }
        }
    }
}
