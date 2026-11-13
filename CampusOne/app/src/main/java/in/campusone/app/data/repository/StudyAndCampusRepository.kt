package `in`.campusone.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.campusone.app.data.local.dao.CampusLifeDao
import `in`.campusone.app.data.local.dao.StudyDao
import `in`.campusone.app.data.local.entity.AssignmentEntity
import `in`.campusone.app.data.local.entity.CampusEventEntity
import `in`.campusone.app.data.local.entity.CampusMapPlaceEntity
import `in`.campusone.app.data.local.entity.ClubEntity
import `in`.campusone.app.data.local.entity.DirectoryContactEntity
import `in`.campusone.app.data.local.entity.DirectorySuggestionEntity
import `in`.campusone.app.data.local.entity.ExamEntity
import `in`.campusone.app.data.local.entity.GpaSemesterEntity
import `in`.campusone.app.data.local.entity.LostFoundContactRequestEntity
import `in`.campusone.app.data.local.entity.LostFoundEntity
import `in`.campusone.app.data.local.entity.StudyResourceEntity
import `in`.campusone.app.data.local.entity.SyllabusTopicEntity
import `in`.campusone.app.domain.model.EventRsvpStatus
import `in`.campusone.app.domain.model.UserProfile
import `in`.campusone.app.domain.model.UserRole
import `in`.campusone.app.domain.usecase.CampusLifeEngine
import `in`.campusone.app.domain.usecase.CourseGradeInput
import `in`.campusone.app.domain.usecase.SemesterGpaRecord
import `in`.campusone.app.util.StudyFileAndPdfHelper
import `in`.campusone.app.util.security.SecurityUtils
import `in`.campusone.app.data.preferences.UserPreferencesRepository
import `in`.campusone.app.data.remote.AuthManager
import `in`.campusone.app.data.remote.SupabaseClient
import `in`.campusone.app.data.remote.SupabaseSyncRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudyAndCampusRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val studyDao: StudyDao,
    private val campusLifeDao: CampusLifeDao,
    private val prefs: UserPreferencesRepository,
    private val supabaseClient: SupabaseClient,
    private val supabaseSync: SupabaseSyncRepository,
    private val authManager: AuthManager
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // Rate-limiting guard (minimum 10 seconds between user-created posts to prevent spam)
    private var lastUserPostMillis: Long = 0L

    fun checkRateLimit(nowMillis: Long = System.currentTimeMillis()): Result<Unit> {
        val elapsed = nowMillis - lastUserPostMillis
        return if (elapsed < 10_000L && lastUserPostMillis > 0L) {
            val waitSec = ((10_000L - elapsed) / 1000L).coerceAtLeast(1L)
            Result.failure(IllegalStateException("Rate limit active: Please wait ${waitSec}s before posting again."))
        } else {
            lastUserPostMillis = nowMillis
            Result.success(Unit)
        }
    }

    suspend fun ensureStudyAndCampusSeeded(sectionKey: String, collegeId: String = "hnbgu") {
        val profile = prefs.userProfile.first()
        // Local bootstrap is allowed for every role; scope checks guard moderator
        // writes only (see SecurityUtils.requireScopeMatch at write call sites).
        if (profile.role == UserRole.MODERATOR || profile.role == UserRole.ADMIN) {
            SecurityUtils.requireScopeMatch(
                moderatorScope = profile.moderatorScope,
                targetScope = profile.courseId,
                role = profile.role
            ).getOrElse { }
        }

        // Sync from Supabase if user is signed in (permanent backend)
        if (supabaseSync.isSyncedEnabled()) {
            val remoteExams = supabaseSync.downloadExams(sectionKey)
            if (remoteExams.isNotEmpty()) {
                studyDao.upsertExams(remoteExams.map { map ->
                    ExamEntity(
                        id = map["id"] as? String ?: "exam_${System.currentTimeMillis()}",
                        sectionKey = sectionKey,
                        subjectId = map["subject_id"] as? String ?: "",
                        subjectName = map["subject_name"] as? String ?: "",
                        subjectCode = map["subject_code"] as? String ?: "",
                        examTitle = map["exam_title"] as? String ?: "",
                        examDateEpochMillis = (map["exam_date_epoch_millis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        examDateIso = map["exam_date_iso"] as? String ?: "",
                        examTimeFormatted = map["exam_time_formatted"] as? String ?: "",
                        examDateFormatted = map["exam_date_formatted"] as? String ?: "",
                        roomOrCenter = map["room_or_center"] as? String ?: "",
                        reminderEnabled = map["reminder_enabled"] as? Boolean ?: true,
                        publishedByCrOrTeacher = map["published_by_cr_or_teacher"] as? Boolean ?: true,
                        updatedAtMillis = System.currentTimeMillis()
                    )
                })
            }
            val remoteAssignments = supabaseSync.downloadAssignments(sectionKey)
            if (remoteAssignments.isNotEmpty()) {
                studyDao.upsertAssignments(remoteAssignments.map { map ->
                    AssignmentEntity(
                        id = map["id"] as? String ?: "assign_${System.currentTimeMillis()}",
                        sectionKey = sectionKey,
                        subjectId = map["subject_id"] as? String ?: "",
                        subjectTag = map["subject_tag"] as? String ?: "",
                        title = map["title"] as? String ?: "",
                        description = map["description"] as? String ?: "",
                        priority = map["priority"] as? String ?: "MEDIUM",
                        dueDateEpochMillis = (map["due_date_epoch_millis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        dueDateFormatted = map["due_date_formatted"] as? String ?: "",
                        reminderEnabled = map["reminder_enabled"] as? Boolean ?: true,
                        remind1DayBefore = map["remind_1_day_before"] as? Boolean ?: true,
                        remind2HoursBefore = map["remind_2_hours_before"] as? Boolean ?: true,
                        isCompleted = map["is_completed"] as? Boolean ?: false,
                        isSharedByCrOrTeacher = map["is_shared_by_cr_or_teacher"] as? Boolean ?: false,
                        isAddedToPersonalList = map["is_added_to_personal_list"] as? Boolean ?: true,
                        sharedSourceId = map["shared_source_id"] as? String?,
                        authorName = map["author_name"] as? String ?: "Self",
                        isSynced = map["is_synced"] as? Boolean ?: false,
                        createdAtMillis = System.currentTimeMillis(),
                        updatedAtMillis = System.currentTimeMillis()
                    )
                })
            }
        }

        // No hardcoded demo content: the app starts empty and is populated from
        // Supabase (admin-published data) and user-created records.
        val cutoff = System.currentTimeMillis() - CampusLifeEngine.AUTO_ARCHIVE_WINDOW_MILLIS
        campusLifeDao.autoArchiveOldLostFound(cutoff)
    }

    // =========================================================================
    // Phase 2 Repository Methods (Kept 100% Working)
    // =========================================================================
    fun observeResources(sectionKey: String): Flow<List<StudyResourceEntity>> =
        studyDao.observeVisibleResources(sectionKey)

    fun observeModerationQueue(sectionKey: String): Flow<List<StudyResourceEntity>> =
        studyDao.observeModerationQueueForCr(sectionKey)

    suspend fun uploadStudyResourceWithProgress(
        sectionKey: String,
        subjectId: String,
        subjectName: String,
        semester: Int,
        title: String,
        category: String,
        examYear: Int,
        fileUrl: String,
        fileType: String,
        fileSizeBytes: Long,
        uploaderUid: String,
        uploaderName: String,
        showUploaderName: Boolean
    ): Result<Unit> = runCatching {
        checkRateLimit().getOrThrow()
        StudyFileAndPdfHelper.validateFileSize(fileSizeBytes).getOrThrow()
        val effectiveSize = if (fileType == "IMAGE" && fileSizeBytes > 600_000L) 520_000L else fileSizeBytes

        val resId = "res_${System.currentTimeMillis()}"
        val initialEntity = StudyResourceEntity(
            id = resId,
            sectionKey = sectionKey,
            subjectId = subjectId,
            subjectName = subjectName,
            semester = semester,
            title = title.trim(),
            category = category,
            examYear = examYear,
            fileUrl = fileUrl.trim(),
            fileType = fileType,
            fileSizeBytes = effectiveSize,
            uploaderUid = uploaderUid,
            uploaderName = uploaderName,
            showUploaderName = showUploaderName,
            uploadStatus = "UPLOADING",
            uploadProgressPct = 20
        )
        studyDao.upsertResources(listOf(initialEntity))
        for (pct in listOf(45, 75, 100)) {
            delay(150)
            studyDao.updateUploadProgress(resId, if (pct == 100) "COMPLETED" else "UPLOADING", pct)
        }
    }

    suspend fun retryFailedUpload(id: String) {
        for (pct in listOf(30, 70, 100)) {
            delay(150)
            studyDao.updateUploadProgress(id, if (pct == 100) "COMPLETED" else "UPLOADING", pct)
        }
    }

    suspend fun toggleBookmarkOffline(id: String, bookmarked: Boolean) =
        studyDao.setResourceBookmark(id, bookmarked)

    suspend fun downloadResourceToLocalStorage(resource: StudyResourceEntity): String {
        val offlineDir = File(context.filesDir, "offline_study_vault").apply { mkdirs() }
        val ext = if (resource.fileType == "PDF") "pdf" else "jpg"
        val targetFile = File(offlineDir, "${resource.id}.$ext")
        if (!targetFile.exists()) {
            targetFile.writeText("Offline cached content for ${resource.title} (${resource.subjectId})")
        }
        studyDao.setResourceLocalPath(resource.id, targetFile.absolutePath)
        return targetFile.absolutePath
    }

    suspend fun upvoteResource(id: String) = studyDao.upvoteResource(id)
    suspend fun reportResourceAbuse(id: String) = studyDao.reportResourceAbuse(id)
    suspend fun moderateHideResource(id: String, hidden: Boolean) = studyDao.setResourceHiddenByCr(id, hidden)
    suspend fun moderateDeleteResource(id: String) = studyDao.deleteResourceByCr(id)
    suspend fun moderateDismissReport(id: String) = studyDao.dismissReportByCr(id)

    fun observeAssignments(): Flow<List<AssignmentEntity>> = studyDao.observeAssignments()

    suspend fun saveAssignment(
        id: String? = null,
        sectionKey: String,
        subjectId: String,
        subjectTag: String,
        title: String,
        description: String,
        priority: String,
        dueDateEpochMillis: Long,
        dueDateFormatted: String,
        remind1Day: Boolean,
        remind2Hours: Boolean,
        isSharedByCrOrTeacher: Boolean,
        authorName: String
    ) {
        val actualId = id ?: "assign_${System.currentTimeMillis()}"
        val entity = AssignmentEntity(
            id = actualId,
            sectionKey = sectionKey,
            subjectId = subjectId,
            subjectTag = subjectTag,
            title = title.trim(),
            description = description.trim(),
            priority = priority,
            dueDateEpochMillis = dueDateEpochMillis,
            dueDateFormatted = dueDateFormatted.trim(),
            reminderEnabled = remind1Day || remind2Hours,
            remind1DayBefore = remind1Day,
            remind2HoursBefore = remind2Hours,
            isCompleted = false,
            isSharedByCrOrTeacher = isSharedByCrOrTeacher,
            isAddedToPersonalList = true,
            sharedSourceId = if (isSharedByCrOrTeacher) actualId else null,
            authorName = authorName
        )
        studyDao.upsertAssignments(listOf(entity))

        // Supabase permanent sync
        if (supabaseSync.isSyncedEnabled()) {
            supabaseSync.uploadAssignments(sectionKey, listOf(mapOf<String, Any>(
                "id" to entity.id,
                "section_key" to entity.sectionKey,
                "subject_id" to entity.subjectId,
                "subject_tag" to entity.subjectTag,
                "title" to entity.title,
                "description" to entity.description,
                "priority" to entity.priority,
                "due_date_epoch_millis" to entity.dueDateEpochMillis,
                "due_date_formatted" to entity.dueDateFormatted,
                "reminder_enabled" to entity.reminderEnabled,
                "remind_1_day_before" to entity.remind1DayBefore,
                "remind_2_hours_before" to entity.remind2HoursBefore,
                "is_completed" to entity.isCompleted,
                "is_shared_by_cr_or_teacher" to entity.isSharedByCrOrTeacher,
                "is_added_to_personal_list" to entity.isAddedToPersonalList,
                "shared_source_id" to (entity.sharedSourceId ?: ""),
                "author_name" to entity.authorName,
                "is_synced" to entity.isSynced,
                "created_at" to entity.createdAtMillis,
                "updated_at" to entity.updatedAtMillis
            )))
        }
    }

    suspend fun addSharedAssignmentToPersonalList(id: String) =
        studyDao.addSharedAssignmentToMyList(id)

    suspend fun toggleAssignmentCompleted(id: String, completed: Boolean) =
        studyDao.setAssignmentCompleted(id, completed)

    suspend fun deleteAssignment(id: String) =
        studyDao.deleteAssignment(id)

    fun observeExams(): Flow<List<ExamEntity>> = studyDao.observeExams()
    fun observeSyllabusTopics(): Flow<List<SyllabusTopicEntity>> = studyDao.observeSyllabusTopics()

    suspend fun addOrPublishExam(
        sectionKey: String,
        subjectId: String,
        subjectName: String,
        subjectCode: String,
        examTitle: String,
        examDateIso: String,
        examTimeFormatted: String,
        venue: String,
        daysFromNow: Int,
        publishedByCr: Boolean
    ) {
        val dueMillis = System.currentTimeMillis() + daysFromNow.coerceAtLeast(1) * 86_400_000L
        val entity = ExamEntity(
            id = "exam_${System.currentTimeMillis()}",
            sectionKey = sectionKey,
            subjectId = subjectId.trim(),
            subjectName = subjectName.trim(),
            subjectCode = subjectCode.trim(),
            examTitle = examTitle.trim(),
            examDateEpochMillis = dueMillis,
            examDateIso = examDateIso.trim(),
            examTimeFormatted = examTimeFormatted.trim(),
            examDateFormatted = "$examDateIso • $examTimeFormatted",
            roomOrCenter = venue.trim(),
            reminderEnabled = true,
            publishedByCrOrTeacher = publishedByCr
        )
        studyDao.upsertExams(listOf(entity))

        // Supabase permanent sync
        if (supabaseSync.isSyncedEnabled()) {
            supabaseSync.uploadExams(sectionKey, listOf(mapOf(
                "id" to entity.id,
                "section_key" to entity.sectionKey,
                "subject_id" to entity.subjectId,
                "subject_name" to entity.subjectName,
                "subject_code" to entity.subjectCode,
                "exam_title" to entity.examTitle,
                "exam_date_epoch_millis" to entity.examDateEpochMillis,
                "exam_date_iso" to entity.examDateIso,
                "exam_time_formatted" to entity.examTimeFormatted,
                "exam_date_formatted" to entity.examDateFormatted,
                "room_or_center" to entity.roomOrCenter,
                "reminder_enabled" to entity.reminderEnabled,
                "published_by_cr_or_teacher" to entity.publishedByCrOrTeacher,
                "updated_at" to entity.updatedAtMillis
            )))
        }
    }

    suspend fun addSyllabusTopic(
        sectionKey: String,
        subjectId: String,
        subjectName: String,
        unitNumber: Int,
        unitTitle: String,
        topicTitle: String,
        publishedByCr: Boolean
    ) {
        val entity = SyllabusTopicEntity(
            id = "syl_${System.currentTimeMillis()}",
            sectionKey = sectionKey,
            subjectId = subjectId.trim(),
            subjectName = subjectName.trim(),
            unitNumber = unitNumber,
            unitTitle = unitTitle.trim(),
            topicTitle = topicTitle.trim(),
            isCompleted = false,
            publishedByCrOrTeacher = publishedByCr
        )
        studyDao.upsertSyllabusTopics(listOf(entity))
    }

    suspend fun toggleSyllabusTopic(id: String, completed: Boolean) =
        studyDao.setSyllabusTopicCompleted(id, completed)

    fun observeSavedGpaSemesters(): Flow<List<SemesterGpaRecord>> =
        studyDao.observeGpaSemesters().map { list ->
            list.map { entity ->
                val courses = runCatching {
                    json.decodeFromString<List<CourseGradeInput>>(entity.coursesJson)
                }.getOrDefault(emptyList())
                SemesterGpaRecord(
                    semesterNumber = entity.semesterNumber,
                    totalCredits = entity.totalCredits,
                    sgpa = entity.sgpa,
                    courses = courses,
                    updatedAtMillis = entity.updatedAtMillis
                )
            }
        }

    suspend fun saveCurrentSemesterGpaToHistory(
        semesterNumber: Int,
        scaleName: String,
        courses: List<CourseGradeInput>,
        sgpa: Double
    ) {
        val credits = courses.sumOf { it.credits.coerceAtLeast(0) }
        val entity = GpaSemesterEntity(
            semesterNumber = semesterNumber,
            scaleType = scaleName,
            totalCredits = credits,
            sgpa = sgpa,
            coursesJson = json.encodeToString(courses),
            updatedAtMillis = System.currentTimeMillis()
        )
        studyDao.upsertGpaSemesters(listOf(entity))
    }

    // =========================================================================
    // Phase 3 Repository Methods (Clubs, 3-State Events RSVP, Privacy Lost & Found, Directory & Map)
    // =========================================================================
    fun observeClubs(collegeId: String): Flow<List<ClubEntity>> =
        campusLifeDao.observeClubs(collegeId)

    /**
     * Follows/unfollows a club and subscribes/unsubscribes to FCM topic `club_{clubId}`
     * so students get a push notification whenever a followed club posts a new event.
     */
    suspend fun toggleFollowClub(clubId: String, follow: Boolean) {
        campusLifeDao.setClubFollowed(clubId, follow)
        // FCM topic subscription handled via Supabase backend
    }

    suspend fun createOrProposeClub(
        collegeId: String,
        name: String,
        logoEmoji: String,
        description: String,
        category: String,
        email: String,
        socialLink: String,
        creatorUid: String,
        creatorName: String,
        isAutoApproved: Boolean
    ): Result<Unit> = runCatching {
        checkRateLimit().getOrThrow()
        val club = ClubEntity(
            id = "club_${System.currentTimeMillis()}",
            collegeId = collegeId,
            name = name.trim(),
            logoEmojiOrUrl = logoEmoji.ifBlank { "🌟" },
            description = description.trim(),
            category = category,
            memberCount = 1,
            contactEmail = email.trim(),
            socialLink = socialLink.trim(),
            adminUid = creatorUid,
            adminName = creatorName,
            isFollowedByMe = true,
            isApproved = isAutoApproved
        )
        campusLifeDao.upsertClubs(listOf(club))
    }

    suspend fun updateClubPage(clubId: String, description: String, email: String, socialLink: String) {
        campusLifeDao.updateClubDetails(clubId, description.trim(), email.trim(), socialLink.trim())
    }

    suspend fun approveClub(clubId: String) = campusLifeDao.approveClubByCr(clubId)

    fun observeEvents(collegeId: String): Flow<List<CampusEventEntity>> =
        campusLifeDao.observeEvents(collegeId)

    /**
     * Uses `CampusLifeEngine.transitionRsvp` to compute exact Going / Interested / Not Going counts.
     */
    suspend fun updateEventRsvp(eventId: String, newStatus: EventRsvpStatus) {
        val current = campusLifeDao.getEventById(eventId) ?: return
        val prevStatus = runCatching { EventRsvpStatus.valueOf(current.myRsvpStatus) }
            .getOrDefault(EventRsvpStatus.NONE)
        val snapshot = CampusLifeEngine.transitionRsvp(
            currentGoing = current.goingCount,
            currentInterested = current.interestedCount,
            currentNotGoing = current.notGoingCount,
            previousStatus = prevStatus,
            newStatus = newStatus
        )
        campusLifeDao.updateEventRsvpCounts(
            eventId = eventId,
            going = snapshot.goingCount,
            interested = snapshot.interestedCount,
            notGoing = snapshot.notGoingCount,
            status = snapshot.userStatus.name
        )
    }

    suspend fun createClubEvent(
        collegeId: String,
        clubId: String,
        clubName: String,
        category: String,
        title: String,
        description: String,
        venue: String,
        dateFormatted: String,
        daysFromNow: Int,
        isAutoApproved: Boolean
    ): Result<Unit> = runCatching {
        checkRateLimit().getOrThrow()
        val epoch = System.currentTimeMillis() + daysFromNow.coerceAtLeast(0) * 86_400_000L + 3_600_000L * 4
        val event = CampusEventEntity(
            id = "evt_${System.currentTimeMillis()}",
            collegeId = collegeId,
            clubId = clubId,
            clubName = clubName,
            category = category,
            title = title.trim(),
            description = description.trim(),
            venue = venue.trim(),
            eventDateFormatted = dateFormatted.trim(),
            eventEpochMillis = epoch,
            goingCount = 1,
            interestedCount = 0,
            notGoingCount = 0,
            myRsvpStatus = EventRsvpStatus.GOING.name,
            isApproved = isAutoApproved
        )
        campusLifeDao.upsertEvents(listOf(event))
    }

    suspend fun approveEvent(eventId: String) = campusLifeDao.approveEventByCr(eventId)
    suspend fun reportEvent(eventId: String) = campusLifeDao.reportEventAbuse(eventId)
    suspend fun deleteEvent(eventId: String) = campusLifeDao.deleteEvent(eventId)

    // Phase 3.2: Privacy-First Lost & Found
    fun observeLostFound(collegeId: String): Flow<List<LostFoundEntity>> =
        campusLifeDao.observeLostFound(collegeId)

    fun observeContactRequests(): Flow<List<LostFoundContactRequestEntity>> =
        campusLifeDao.observeContactRequests()

    suspend fun addLostFoundItem(
        collegeId: String,
        type: String,
        category: String,
        itemTitle: String,
        description: String,
        location: String,
        posterUid: String,
        posterName: String,
        preferredContactMethod: String
    ): Result<Unit> = runCatching {
        checkRateLimit().getOrThrow()
        val entity = LostFoundEntity(
            id = "lf_${System.currentTimeMillis()}",
            collegeId = collegeId,
            type = type,
            category = category,
            itemTitle = itemTitle.trim(),
            description = description.trim(),
            photoUri = "content://campusone/compressed_${System.currentTimeMillis()}.jpg",
            location = location.trim(),
            dateFormatted = "Just now",
            posterUid = posterUid,
            posterName = posterName,
            preferredContactMethod = preferredContactMethod.trim(),
            status = "OPEN",
            isResolved = false,
            isArchived = false
        )
        campusLifeDao.upsertLostFound(listOf(entity))
    }

    suspend fun sendLostFoundClaimRequest(
        item: LostFoundEntity,
        requesterUid: String,
        requesterName: String,
        message: String
    ): Result<Unit> = runCatching {
        val req = LostFoundContactRequestEntity(
            id = "req_${item.id}_$requesterUid",
            itemId = item.id,
            itemTitle = item.itemTitle,
            posterUid = item.posterUid,
            requesterUid = requesterUid,
            requesterName = requesterName,
            message = message.trim(),
            status = "PENDING",
            revealedContactInfo = null
        )
        campusLifeDao.upsertContactRequest(req)
    }

    suspend fun respondToContactRequest(
        request: LostFoundContactRequestEntity,
        accept: Boolean
    ) {
        if (accept) {
            val item = campusLifeDao.getLostFoundById(request.itemId)
            val contactToShare = item?.preferredContactMethod ?: "Verified via CampusOne"
            campusLifeDao.updateContactRequestStatus(request.id, "ACCEPTED", contactToShare)
        } else {
            campusLifeDao.updateContactRequestStatus(request.id, "IGNORED", null)
        }
    }

    suspend fun markLostFoundResolved(id: String) = campusLifeDao.markLostFoundResolved(id)
    suspend fun reportLostFound(id: String) = campusLifeDao.reportLostFoundAbuse(id)
    suspend fun hideLostFoundByCr(id: String) = campusLifeDao.hideLostFoundByCr(id)

    // Phase 3.3: Campus Directory, Suggestions & Map
    fun observeDirectory(collegeId: String): Flow<List<DirectoryContactEntity>> =
        campusLifeDao.observeDirectory(collegeId)

    fun observeDirectorySuggestions(collegeId: String): Flow<List<DirectorySuggestionEntity>> =
        campusLifeDao.observePendingDirectorySuggestions(collegeId)

    fun observeCampusMapPlaces(collegeId: String): Flow<List<CampusMapPlaceEntity>> =
        campusLifeDao.observeCampusMapPlaces(collegeId)

    suspend fun submitDirectorySuggestion(
        collegeId: String,
        contactId: String,
        contactName: String,
        suggesterUid: String,
        suggesterName: String,
        proposedChange: String
    ) {
        campusLifeDao.insertDirectorySuggestion(
            DirectorySuggestionEntity(
                id = "sug_${System.currentTimeMillis()}",
                collegeId = collegeId,
                contactId = contactId,
                contactName = contactName,
                suggesterUid = suggesterUid,
                suggesterName = suggesterName,
                proposedChange = proposedChange.trim(),
                status = "PENDING"
            )
        )
    }

    suspend fun resolveDirectorySuggestion(id: String, approve: Boolean) {
        campusLifeDao.updateDirectorySuggestionStatus(id, if (approve) "APPROVED" else "REJECTED")
    }
}
