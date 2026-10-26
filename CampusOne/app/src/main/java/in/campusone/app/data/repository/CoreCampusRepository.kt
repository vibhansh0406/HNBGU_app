package `in`.campusone.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoreCampusRepository @Inject constructor(
    private val attendanceDao: AttendanceDao,
    private val timetableDao: TimetableDao,
    private val noticeDao: NoticeDao,
    private val prefs: UserPreferencesRepository,
    private val firestore: FirebaseFirestore?
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
        // Subscribe to section FCM topic for zero-cost push notifications
        runCatching {
            FirebaseMessaging.getInstance().subscribeToTopic("sec_$sectionKey")
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
     * CR / Teacher action: Publish or update a timetable slot for the section.
     * Updates Room immediately (offline-first) and pushes single-document timetable array to Firestore.
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

        // Sync whole-section timetable as a single Firestore document (1 read/write optimization)
        val allSectionSlots = timetableDao.getTimetableSnapshot(profile.sectionKey)
        firestore?.collection("sections")
            ?.document(profile.sectionKey)
            ?.collection("meta")
            ?.document("timetable")
            ?.set(
                mapOf(
                    "sectionKey" to profile.sectionKey,
                    "updatedBy" to profile.name,
                    "updatedAt" to System.currentTimeMillis(),
                    "slots" to allSectionSlots.map {
                        mapOf(
                            "id" to it.id,
                            "subjectId" to it.subjectId,
                            "subjectName" to it.subjectName,
                            "subjectCode" to it.subjectCode,
                            "teacherName" to it.teacherName,
                            "roomNumber" to it.roomNumber,
                            "dayOfWeek" to it.dayOfWeek,
                            "startTime" to it.startTime,
                            "endTime" to it.endTime,
                            "isElective" to it.isElective
                        )
                    }
                ),
                SetOptions.merge()
            )?.await()
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

        firestore?.collection("sections")
            ?.document(profile.sectionKey)
            ?.collection("notices")
            ?.document(noticeId)
            ?.set(
                mapOf(
                    "id" to entity.id,
                    "sectionKey" to entity.sectionKey,
                    "title" to entity.title,
                    "body" to entity.body,
                    "attachmentUrl" to entity.attachmentUrl,
                    "attachmentType" to entity.attachmentType.name,
                    "isPinned" to entity.isPinned,
                    "authorUid" to entity.authorUid,
                    "authorName" to entity.authorName,
                    "authorRole" to entity.authorRole.name,
                    "createdAt" to now,
                    "updatedAt" to now
                )
            )?.await()
    }

    /**
     * Background delta sync invoked by WorkManager when network is connected.
     * Minimizes Firestore reads using `updatedAt > lastSyncTimestamp`.
     */
    suspend fun performDeltaSync() {
        val db = firestore ?: return
        val profile = prefs.userProfile.first()
        val sectionKey = profile.sectionKey
        val (lastTtSync, lastNoticeSync) = prefs.getLastSyncTimestamps()

        // 1. Push unsynced personal attendance logs (aggregated per subject to save writes)
        if (profile.syncAttendanceToCloud && profile.uid.isNotBlank()) {
            val unsynced = attendanceDao.getUnsyncedLogs()
            if (unsynced.isNotEmpty()) {
                val bySubj = unsynced.groupBy { it.subjectId }
                for ((subjectId, logs) in bySubj) {
                    val historyPatch = logs.associate { "${it.dateIso}_${it.slotId}" to it.status.name }
                    runCatching {
                        db.collection("users")
                            .document(profile.uid)
                            .collection("attendance")
                            .document(subjectId)
                            .set(
                                mapOf(
                                    "subjectId" to subjectId,
                                    "history" to historyPatch,
                                    "updatedAt" to System.currentTimeMillis()
                                ),
                                SetOptions.merge()
                            ).await()
                    }
                }
                attendanceDao.markLogsSynced(unsynced.map { it.id })
            }
        }

        // 2. Pull section timetable only if remote `updatedAt > lastTtSync` (1 document read max)
        runCatching {
            val ttDoc = db.collection("sections")
                .document(sectionKey)
                .collection("meta")
                .document("timetable")
                .get()
                .await()
            val remoteUpdated = ttDoc.getLong("updatedAt") ?: 0L
            if (ttDoc.exists() && remoteUpdated > lastTtSync) {
                @Suppress("UNCHECKED_CAST")
                val rawSlots = ttDoc.get("slots") as? List<Map<String, Any?>> ?: emptyList()
                val mapped = rawSlots.mapNotNull { m ->
                    val id = m["id"] as? String ?: return@mapNotNull null
                    TimetableSlotEntity(
                        id = id,
                        sectionKey = sectionKey,
                        subjectId = m["subjectId"] as? String ?: "",
                        subjectName = m["subjectName"] as? String ?: "",
                        subjectCode = m["subjectCode"] as? String ?: "",
                        teacherName = m["teacherName"] as? String ?: "",
                        roomNumber = m["roomNumber"] as? String ?: "",
                        dayOfWeek = (m["dayOfWeek"] as? Number)?.toInt() ?: 1,
                        startTime = m["startTime"] as? String ?: "09:00",
                        endTime = m["endTime"] as? String ?: "10:00",
                        isElective = m["isElective"] as? Boolean ?: false,
                        updatedAtMillis = remoteUpdated
                    )
                }
                if (mapped.isNotEmpty()) {
                    timetableDao.replaceSectionTimetable(sectionKey, mapped)
                    prefs.updateSyncTimestamps(timetableSync = remoteUpdated)
                }
            }
        }

        // 3. Pull only new/updated notices since `lastNoticeSync`
        runCatching {
            val readIds = noticeDao.getReadNoticeIds(sectionKey).toSet()
            val snapshot = db.collection("sections")
                .document(sectionKey)
                .collection("notices")
                .whereGreaterThan("updatedAt", lastNoticeSync)
                .limit(25)
                .get()
                .await()

            var maxUpdated = lastNoticeSync
            val entities = snapshot.documents.mapNotNull { doc ->
                val updated = doc.getLong("updatedAt") ?: 0L
                if (updated > maxUpdated) maxUpdated = updated
                NoticeEntity(
                    id = doc.id,
                    sectionKey = sectionKey,
                    title = doc.getString("title") ?: return@mapNotNull null,
                    body = doc.getString("body") ?: "",
                    attachmentUrl = doc.getString("attachmentUrl"),
                    attachmentType = runCatching {
                        AttachmentType.valueOf(doc.getString("attachmentType") ?: "NONE")
                    }.getOrDefault(AttachmentType.NONE),
                    isPinned = doc.getBoolean("isPinned") ?: false,
                    authorUid = doc.getString("authorUid") ?: "",
                    authorName = doc.getString("authorName") ?: "Faculty / CR",
                    authorRole = runCatching {
                        UserRole.valueOf(doc.getString("authorRole") ?: "CR")
                    }.getOrDefault(UserRole.CR),
                    createdAtMillis = doc.getLong("createdAt") ?: updated,
                    isRead = doc.id in readIds
                )
            }
            if (entities.isNotEmpty()) {
                noticeDao.upsertNotices(entities)
                prefs.updateSyncTimestamps(noticeSync = maxUpdated)
            }
        }
    }
}
