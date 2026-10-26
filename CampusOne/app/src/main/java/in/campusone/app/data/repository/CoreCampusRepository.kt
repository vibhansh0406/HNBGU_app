package `in`.campusone.app.data.repository

import `in`.campusone.app.data.local.HnbguSeedData
import `in`.campusone.app.data.local.dao.AttendanceDao
import `in`.campusone.app.data.local.dao.NoticeDao
import `in`.campusone.app.data.local.dao.TimetableDao
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoreCampusRepository @Inject constructor(
    private val attendanceDao: AttendanceDao,
    private val timetableDao: TimetableDao,
    private val noticeDao: NoticeDao,
    private val prefs: UserPreferencesRepository
) {

    /**
     * Ensures that if the database is empty (first install or offline pilot),
     * HNB Garhwal University default section timetable, subjects, and notices are seeded.
     */
    suspend fun ensureSeededForSection(sectionKey: String) {
        val currentSlots = timetableDao.getTimetableSnapshot(sectionKey)
        if (currentSlots.isEmpty()) {
            attendanceDao.upsertSubjects(HnbguSeedData.defaultSubjects(sectionKey))
            timetableDao.upsertSlots(HnbguSeedData.defaultTimetable(sectionKey))
            noticeDao.upsertNotices(HnbguSeedData.defaultNotices(sectionKey))
        }
    }

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
     * CR / Teacher demo action: save a timetable slot to this device's Room database.
     */
    suspend fun saveAndPublishTimetableSlot(
        profile: UserProfile,
        slot: TimetableSlot
    ): Result<Unit> = runCatching {
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
    ): Result<Unit> = runCatching {
        require(profile.role == UserRole.CR || profile.role == UserRole.TEACHER) {
            "Only Class Representatives (CR) or Teachers can post announcements."
        }
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

    }
}
