package `in`.campusone.app.data.repository

import `in`.campusone.app.data.local.HnbguSeedData
import `in`.campusone.app.data.local.dao.AttendanceDao
import `in`.campusone.app.data.local.dao.NoticeDao
import `in`.campusone.app.data.local.dao.TimetableDao
import `in`.campusone.app.data.remote.AuthManager
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoreCampusRepository @Inject constructor(
    private val attendanceDao: AttendanceDao,
    private val timetableDao: TimetableDao,
    private val noticeDao: NoticeDao,
    private val prefs: UserPreferencesRepository,
    private val supabaseClient: SupabaseClient,
    private val supabaseSync: SupabaseSyncRepository,
    private val authManager: AuthManager
) {

    /**
     * Ensures that if the database is empty (first install or offline pilot),
     * HNB Garhwal University default section timetable, subjects, and notices are seeded.
     */
    companion object {
        const val OWNER_EMAIL = "vvibhaansh@gmail.com"
    }

    suspend fun syncProfileToCloud(profile: UserProfile) {
        if (!supabaseSync.isSyncedEnabled()) return
        supabaseSync.uploadProfile(
            uid = profile.uid,
            name = profile.name,
            email = profile.email,
            role = profile.role.name,
            collegeId = profile.collegeId,
            collegeName = profile.collegeName,
            courseId = profile.courseId,
            courseName = profile.courseName,
            semester = profile.semester,
            section = profile.section,
            moderatorScope = profile.moderatorScope,
            isModeratorApproved = profile.role == UserRole.MODERATOR || profile.role == UserRole.ADMIN
        )
    }

    suspend fun fetchRemoteProfile(uid: String): UserProfile? {
        val remote = supabaseSync.downloadProfile(uid) ?: return null
        val courseId = remote["course_id"] as? String ?: ""
        if (courseId.isBlank()) return null
        val remoteRole = remote["role"] as? String ?: ""
        val role = UserRole.entries.firstOrNull { it.name == remoteRole }
            ?: if ((remote["email"] as? String)?.equals(OWNER_EMAIL, ignoreCase = true) == true) {
                UserRole.ADMIN
            } else {
                UserRole.STUDENT
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
            moderatorScope = remote["moderator_scope"] as? String ?: ""
        )
    }

    suspend fun ensureSeededForSection(sectionKey: String) {
        val profile = prefs.userProfile.first()
        // Seeding/initialization is allowed for everyone; the scope check only
        // applies to moderator/admin write actions, not to local bootstrap.
        if (profile.role == UserRole.MODERATOR || profile.role == UserRole.ADMIN) {
            SecurityUtils.requireScopeMatch(
                moderatorScope = profile.moderatorScope,
                targetScope = profile.courseId,
                role = profile.role
            ).getOrElse { }
        }

        val currentSlots = timetableDao.getTimetableSnapshot(sectionKey)
        if (currentSlots.isEmpty()) {
            attendanceDao.upsertSubjects(HnbguSeedData.defaultSubjects(sectionKey))
            timetableDao.upsertSlots(HnbguSeedData.defaultTimetable(sectionKey))
        }

        // Sync with Supabase if user is signed in (permanent backend)
        if (supabaseSync.isSyncedEnabled()) {
            // Pull timetable & notices from Supabase
            val remoteSlots = supabaseSync.downloadTimetable(sectionKey)
            if (remoteSlots.isNotEmpty()) {
                timetableDao.replaceSectionTimetable(sectionKey, remoteSlots.map { map ->
                    TimetableSlotEntity(
                        id = map["id"] as? String ?: "${sectionKey}_${System.currentTimeMillis()}",
                        sectionKey = sectionKey,
                        subjectId = map["subject_id"] as? String ?: "",
                        subjectName = map["subject_name"] as? String ?: "",
                        subjectCode = map["subject_code"] as? String ?: "",
                        teacherName = map["teacher_name"] as? String ?: "",
                        roomNumber = map["room_number"] as? String ?: "",
                        dayOfWeek = (map["day_of_week"] as? Number)?.toInt() ?: 1,
                        startTime = map["start_time"] as? String ?: "09:00",
                        endTime = map["end_time"] as? String ?: "10:00",
                        isElective = map["is_elective"] as? Boolean ?: false,
                        updatedAtMillis = System.currentTimeMillis()
                    )
                })
            }
            val remoteNotices = supabaseSync.downloadNotices(sectionKey) +
                supabaseSync.downloadNotices("global")
            if (remoteNotices.isNotEmpty()) {
                noticeDao.upsertNotices(remoteNotices.map { map ->
                    NoticeEntity(
                        id = map["id"] as? String ?: "notice_${System.currentTimeMillis()}",
                        sectionKey = sectionKey,
                        title = map["title"] as? String ?: "",
                        body = map["body"] as? String ?: "",
                        attachmentUrl = map["attachment_url"] as? String?,
                        attachmentType = runCatchingDefault({
                            AttachmentType.valueOf(map["attachment_type"] as? String ?: "NONE")
                        }, AttachmentType.NONE),
                        isPinned = map["is_pinned"] as? Boolean ?: false,
                        authorUid = map["author_uid"] as? String ?: "",
                        authorName = map["author_name"] as? String ?: "Faculty",
                        authorRole = runCatchingDefault({
                            UserRole.valueOf(map["author_role"] as? String ?: "CR")
                        }, UserRole.CR),
                        createdAtMillis = TimestampUtils.toEpochMillis(map["created_at"]) ?: System.currentTimeMillis(),
                        isRead = map["is_read"] as? Boolean ?: false
                    )
                })
            }
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

    /**
     * CR / Teacher action: Publish or update a timetable slot for the section.
     * Updates Room immediately (offline-first) and pushes to Supabase (permanent backend).
     */
    suspend fun saveAndPublishTimetableSlot(
        profile: UserProfile,
        slot: TimetableSlot
    ): kotlin.Result<Unit> = runCatching {
        require(profile.role == UserRole.CR || profile.role == UserRole.TEACHER) {
            "Only Class Representatives (CR) or Teachers can publish section timetables."
        }
        val entity = TimetableSlotEntity(
            id = slot.id,
            sectionKey = profile.sectionKey,
            subjectId = slot.subjectId,
            subjectName = slot.subjectName,
            subjectCode = slot.subjectCode,
            teacherName = slot.teacherName,
            roomNumber = slot.roomNumber,
            dayOfWeek = slot.dayOfWeek,
            startTime = slot.startTime,
            endTime = slot.endTime,
            isElective = slot.isElective,
            updatedAtMillis = System.currentTimeMillis()
        )
        timetableDao.upsertSlots(listOf(entity))

        // Supabase permanent sync
        if (supabaseSync.isSyncedEnabled()) {
            val allSectionSlots = timetableDao.getTimetableSnapshot(profile.sectionKey)
            supabaseSync.uploadTimetable(profile.sectionKey, allSectionSlots.map { it ->
                mapOf(
                    "id" to it.id,
                    "section_key" to it.sectionKey,
                    "subject_id" to it.subjectId,
                    "subject_name" to it.subjectName,
                    "subject_code" to it.subjectCode,
                    "teacher_name" to it.teacherName,
                    "room_number" to it.roomNumber,
                    "day_of_week" to it.dayOfWeek,
                    "start_time" to it.startTime,
                    "end_time" to it.endTime,
                    "is_elective" to it.isElective,
                    "updated_at" to System.currentTimeMillis()
                )
            })
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
     * CR / Teacher action: Post a new announcement to the section Notice Board.
     */
    suspend fun postNotice(
        profile: UserProfile,
        title: String,
        body: String,
        attachmentUrl: String?,
        attachmentType: AttachmentType,
        isPinned: Boolean
    ): kotlin.Result<Unit> = runCatching {
        require(
            profile.role == UserRole.CR ||
                profile.role == UserRole.TEACHER ||
                profile.role == UserRole.ADMIN ||
                profile.role == UserRole.MODERATOR
        ) {
            "Only CRs, Teachers, Moderators or Admins can post announcements."
        }
        val targetSectionKey = if (profile.role == UserRole.ADMIN) "global" else profile.sectionKey
        val now = System.currentTimeMillis()
        val noticeId = "notice_${now}_${profile.uid.take(6)}"
        val entity = NoticeEntity(
            id = noticeId,
            sectionKey = profile.sectionKey,
            title = title.trim(),
            body = body.trim(),
            attachmentUrl = attachmentUrl?.takeIf { it.isNotBlank() },
            attachmentType = attachmentType,
            isPinned = isPinned,
            authorUid = profile.uid.ifBlank { "local_author" },
            authorName = profile.name.ifBlank { "CR / Faculty" },
            authorRole = profile.role,
            createdAtMillis = now,
            isRead = true
        )
        noticeDao.upsertNotices(listOf(entity))

        // Supabase permanent sync
        if (supabaseSync.isSyncedEnabled()) {
            supabaseSync.uploadNotices(profile.sectionKey, listOf(mapOf<String, Any>(
                "id" to entity.id,
                "section_key" to targetSectionKey,
                "title" to entity.title,
                "body" to entity.body,
                "attachment_url" to (entity.attachmentUrl ?: ""),
                "attachment_type" to entity.attachmentType.name,
                "is_pinned" to entity.isPinned,
                "author_uid" to entity.authorUid,
                "author_name" to entity.authorName,
                "author_role" to entity.authorRole.name,
                "created_at" to entity.createdAtMillis,
                "updated_at" to now,
                "is_read" to entity.isRead
            )))
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
