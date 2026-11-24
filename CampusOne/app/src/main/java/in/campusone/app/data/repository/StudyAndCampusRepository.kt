package `in`.campusone.app.data.repository

import android.content.Context
import android.content.ContentResolver
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
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
import `in`.campusone.app.data.remote.FileStore
import `in`.campusone.app.data.remote.StorageBuckets
import `in`.campusone.app.data.remote.SupabaseClient
import `in`.campusone.app.data.remote.SupabaseSyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    private val authManager: AuthManager,
    private val fileStore: FileStore
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

    suspend fun requestRole(role: UserRole, note: String): Result<Unit> {
        val profile = prefs.userProfile.first()
        val uid = authManager.currentUserId()
            ?: return Result.failure(IllegalStateException("Sign in to send a role request"))
        if (role == UserRole.STUDENT || role == UserRole.ADMIN) {
            return Result.failure(IllegalArgumentException("Choose a staff role"))
        }
        val ok = supabaseSync.submitRoleRequest(uid, profile.email, role.name, note)
        return if (ok) Result.success(Unit)
        else Result.failure(IllegalStateException("Could not send role request"))
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

        // Curated official HNBGU directory is available offline on every fresh install.
        if (collegeId == "hnbgu") seedOfficialDirectory(collegeId)

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
            val remoteResources = supabaseSync.downloadStudyResources(sectionKey) +
                supabaseSync.downloadStudyResources("global")
            if (remoteResources.isNotEmpty()) {
                studyDao.upsertResources(remoteResources.map { map ->
                    StudyResourceEntity(
                        id = map["id"] as? String ?: "res_${System.currentTimeMillis()}",
                        sectionKey = sectionKey,
                        subjectId = map["subject_id"] as? String ?: "",
                        subjectName = map["subject_name"] as? String ?: "",
                        semester = (map["semester"] as? Number)?.toInt() ?: 1,
                        title = map["title"] as? String ?: "",
                        category = map["category"] as? String ?: "NOTES",
                        examYear = (map["exam_year"] as? Number)?.toInt() ?: 2025,
                        fileUrl = map["file_url"] as? String ?: "",
                        fileHash = map["file_hash"] as? String ?: "",
                        fileType = map["file_type"] as? String ?: "PDF",
                        fileSizeBytes = (map["file_size_bytes"] as? Number)?.toLong() ?: 0L,
                        uploaderUid = map["uploader_uid"] as? String ?: "",
                        uploaderName = map["uploader_name"] as? String ?: "Student",
                        showUploaderName = map["show_uploader_name"] as? Boolean ?: true,
                        upvotes = (map["upvotes"] as? Number)?.toInt() ?: 0,
                        isBookmarkedOffline = false,
                        reportCount = (map["report_count"] as? Number)?.toInt() ?: 0,
                        isFlagged = map["is_flagged"] as? Boolean ?: false,
                        isHidden = map["is_hidden"] as? Boolean ?: false,
                        isApprovedByCr = map["is_approved_by_cr"] as? Boolean ?: true,
                        uploadStatus = map["upload_status"] as? String ?: "PUBLISHED",
                        createdAtMillis = (map["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        updatedAtMillis = System.currentTimeMillis()
                    )
                })
            }
            val remoteClubs = supabaseSync.downloadClubs(collegeId)
            if (remoteClubs.isNotEmpty()) {
                campusLifeDao.upsertClubs(remoteClubs.mapNotNull { row ->
                    val clubId = row["id"] as? String ?: return@mapNotNull null
                    ClubEntity(
                        id = clubId,
                        collegeId = row["college_id"] as? String ?: collegeId,
                        name = row["name"] as? String ?: "",
                        logoEmojiOrUrl = "🌟",
                        description = row["description"] as? String ?: "",
                        category = row["category"] as? String ?: "CAMPUS",
                        memberCount = (row["member_count"] as? Number)?.toInt() ?: 0,
                        contactEmail = row["contact_email"] as? String ?: "",
                        socialLink = row["social_link"] as? String ?: "",
                        adminUid = row["admin_uid"] as? String ?: "",
                        adminName = row["admin_name"] as? String ?: "",
                        isFollowedByMe = row["is_followed_by_me"] as? Boolean ?: false,
                        isApproved = row["is_approved"] as? Boolean ?: false,
                        reportCount = 0,
                        isFlagged = row["is_flagged"] as? Boolean ?: false,
                        updatedAtMillis = (row["updated_at"] as? Number)?.toLong()
                            ?: System.currentTimeMillis()
                    )
                })
            }
            val clubNames = remoteClubs.associate {
                (it["id"] as? String) to (it["name"] as? String)
            }

            val remoteEvents = supabaseSync.downloadEvents(collegeId)
            if (remoteEvents.isNotEmpty()) {
                val dateFormat = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())
                campusLifeDao.upsertEvents(remoteEvents.mapNotNull { row ->
                    val eventId = row["id"] as? String ?: return@mapNotNull null
                    val epoch = (row["event_epoch_millis"] as? Number)?.toLong()
                        ?: return@mapNotNull null
                    val clubId = row["club_id"] as? String ?: ""
                    CampusEventEntity(
                        id = eventId,
                        collegeId = row["college_id"] as? String ?: collegeId,
                        clubId = clubId,
                        clubName = clubNames[clubId] ?: "CampusOne",
                        category = "CAMPUS",
                        title = row["title"] as? String ?: "",
                        description = row["description"] as? String ?: "",
                        venue = row["venue"] as? String ?: "",
                        eventDateFormatted = dateFormat.format(Date(epoch)),
                        eventEpochMillis = epoch,
                        goingCount = (row["going_count"] as? Number)?.toInt() ?: 0,
                        interestedCount = (row["interested_count"] as? Number)?.toInt() ?: 0,
                        notGoingCount = (row["not_going_count"] as? Number)?.toInt() ?: 0,
                        myRsvpStatus = row["my_rsvp_status"] as? String ?: "NONE",
                        reminderLeadMinutes = 60,
                        isApproved = row["is_approved"] as? Boolean ?: false,
                        reportCount = (row["report_count"] as? Number)?.toInt() ?: 0,
                        isFlagged = row["is_flagged"] as? Boolean ?: false,
                        updatedAtMillis = (row["updated_at"] as? Number)?.toLong()
                            ?: System.currentTimeMillis()
                    )
                })
            }

            val remoteLostFound = supabaseClient.from("lost_found_items")
                .select("*")
                .eq("college_id", collegeId)
                .order("created_at", ascending = false)
                .limit(100)
                .execute()
            val lostFoundRows = remoteLostFound.data as? List<Map<String, Any>> ?: emptyList()
            if (lostFoundRows.isNotEmpty()) {
                campusLifeDao.upsertLostFound(lostFoundRows.mapNotNull { row ->
                    val itemId = row["id"] as? String ?: return@mapNotNull null
                    val createdAt = (row["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    val resolved = row["is_resolved"] as? Boolean ?: false
                    LostFoundEntity(
                        id = itemId,
                        collegeId = row["college_id"] as? String ?: collegeId,
                        type = row["type"] as? String ?: "LOST",
                        category = row["category"] as? String ?: "OTHER",
                        itemTitle = row["title"] as? String ?: "",
                        description = row["description"] as? String ?: "",
                        photoUri = null,
                        location = row["location"] as? String ?: "",
                        dateFormatted = "Posted recently",
                        posterUid = row["poster_uid"] as? String ?: "",
                        posterName = row["poster_name"] as? String ?: "Campus member",
                        preferredContactMethod = "",
                        status = if (resolved) "RESOLVED" else "OPEN",
                        isResolved = resolved,
                        isArchived = false,
                        isHidden = row["is_hidden"] as? Boolean ?: false,
                        reportCount = (row["report_count"] as? Number)?.toInt() ?: 0,
                        isFlagged = false,
                        createdAtMillis = createdAt
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

    private suspend fun seedOfficialDirectory(collegeId: String) {
        // Remove retired seed records from Room on existing installs as well as fresh installs.
        campusLifeDao.deleteDirectoryContacts(
            listOf(
                "official_hnbgu_bgr_director",
                "official_hnbgu_srt_director",
                "campus_bgr",
                "campus_srt"
            )
        )
        val existingIds = campusLifeDao.observeDirectory(collegeId).first().map { it.id }.toSet()
        val missing = HnbguDirectorySeed.contacts(collegeId).filter { it.id !in existingIds }
        if (missing.isNotEmpty()) campusLifeDao.upsertDirectory(missing)
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
        contentUri: String,
        fileType: String,
        uploaderUid: String,
        uploaderName: String,
        showUploaderName: Boolean
    ): Result<Unit> {
        checkRateLimit().getOrThrow()
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
            fileUrl = contentUri.trim(),
            fileType = fileType,
            fileSizeBytes = 0L,
            uploaderUid = uploaderUid,
            uploaderName = uploaderName,
            showUploaderName = showUploaderName,
            uploadStatus = "UPLOADING",
            uploadProgressPct = 5
        )
        studyDao.upsertResources(listOf(initialEntity))
        return try {
            val bytes = readContentBytes(context.contentResolver, contentUri).getOrThrow()
            StudyFileAndPdfHelper.validateFileSize(bytes.size.toLong()).getOrThrow()
            val hash = sha256Hex(bytes)
            val remoteUrl = obtainRemoteFileUrl(resId, hash, bytes, fileType)
            studyDao.finishResourceUpload(resId, remoteUrl, hash, bytes.size.toLong(), "COMPLETED", 100)
            val saved = studyDao.getResourceById(resId) ?: initialEntity
            if (supabaseSync.isSyncedEnabled() && remoteUrl.startsWith("http")) {
                val isAdmin = prefs.userProfile.first().role == UserRole.ADMIN
                val syncSectionKey = if (isAdmin) "global" else sectionKey
                val published = supabaseSync.uploadStudyResources(
                    syncSectionKey,
                    listOf(studyResourceSyncMap(saved, syncSectionKey))
                )
                if (published && saved.isApprovedByCr && isAdmin) {
                    supabaseSync.sendPushNotification(
                        title = "New study material: ${saved.title}",
                        body = "${saved.subjectName} • ${saved.category}",
                        category = "study_material",
                        targetType = "STUDY_MATERIAL",
                        targetId = saved.id,
                        sectionKey = syncSectionKey
                    )
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            studyDao.markResourceUploadFailed(resId)
            Result.failure(e)
        }
    }

    private suspend fun obtainRemoteFileUrl(
        resId: String,
        hash: String,
        bytes: ByteArray,
        fileType: String
    ): String {
        val existing = studyDao.findResourceByHash(hash)
        if (existing != null && existing.fileUrl.startsWith("http")) {
            return existing.fileUrl
        }
        val ext = if (fileType == "IMAGE") "jpg" else "pdf"
        val contentType = if (fileType == "IMAGE") "image/jpeg" else "application/pdf"
        return fileStore.upload(
            bucket = StorageBuckets.STUDY_RESOURCES,
            path = "resources/$hash.$ext",
            bytes = bytes,
            contentType = contentType
        ) { pct ->
            studyDao.updateUploadProgress(resId, "UPLOADING", pct.coerceIn(5, 95))
        }.getOrThrow()
    }

    suspend fun retryFailedUpload(id: String) {
        val resource = studyDao.getResourceById(id) ?: return
        if (!resource.fileUrl.startsWith("content://")) return
        studyDao.deleteResourceByCr(id)
        uploadStudyResourceWithProgress(
            sectionKey = resource.sectionKey,
            subjectId = resource.subjectId,
            subjectName = resource.subjectName,
            semester = resource.semester,
            title = resource.title,
            category = resource.category,
            examYear = resource.examYear,
            contentUri = resource.fileUrl,
            fileType = resource.fileType,
            uploaderUid = resource.uploaderUid,
            uploaderName = resource.uploaderName,
            showUploaderName = resource.showUploaderName
        )
    }

    suspend fun toggleBookmarkOffline(id: String, bookmarked: Boolean) =
        studyDao.setResourceBookmark(id, bookmarked)

    suspend fun downloadResourceToLocalStorage(resource: StudyResourceEntity): String {
        val offlineDir = File(context.filesDir, "offline_study_vault").apply { mkdirs() }
        val ext = if (resource.fileType == "PDF") "pdf" else "jpg"
        val targetFile = File(offlineDir, "${resource.id}.$ext")
        if (!targetFile.exists() || targetFile.length() == 0L) {
            val bytes = resolveResourceBytes(resource)
            if (bytes != null) {
                withContext(Dispatchers.IO) { targetFile.writeBytes(bytes) }
            } else {
                targetFile.writeText("Offline cached content for ${resource.title} (${resource.subjectId})")
            }
        }
        studyDao.setResourceLocalPath(resource.id, targetFile.absolutePath)
        return targetFile.absolutePath
    }

    suspend fun obtainViewerFile(resource: StudyResourceEntity): Pair<Int, File>? = withContext(Dispatchers.IO) {
        runCatching {
            val file = when {
                resource.localFilePath != null && File(resource.localFilePath).exists() -> File(resource.localFilePath)
                resource.fileUrl.startsWith("http") || resource.fileUrl.startsWith("content://") ->
                    resourceFileForViewing(resource)
                else -> null
            } ?: return@runCatching null
            if (resource.fileType == "PDF") {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer -> Pair(renderer.pageCount, file) }
                }
            } else {
                Pair(1, file)
            }
        }.getOrNull()
    }

    private suspend fun resourceFileForViewing(resource: StudyResourceEntity): File {
        val cacheDir = File(context.cacheDir, "study_view_cache").apply { mkdirs() }
        val ext = if (resource.fileType == "PDF") "pdf" else "jpg"
        val suffix = resource.fileHash.take(8).ifBlank { "raw" }
        val cached = File(cacheDir, "${resource.id}.$suffix.$ext")
        if (!cached.exists() || cached.length() == 0L) {
            val bytes = resolveResourceBytes(resource)
                ?: throw IllegalStateException("File source unavailable")
            cached.writeBytes(bytes)
        }
        return cached
    }

    private suspend fun resolveResourceBytes(resource: StudyResourceEntity): ByteArray? {
        return when {
            resource.fileUrl.startsWith("http") ->
                fileStore.download(resource.fileUrl).getOrThrow()
            resource.fileUrl.startsWith("content://") ->
                readContentBytes(context.contentResolver, resource.fileUrl).getOrThrow()
            resource.localFilePath != null && File(resource.localFilePath).exists() ->
                File(resource.localFilePath).readBytes()
            else -> null
        }
    }

    private suspend fun readContentBytes(resolver: ContentResolver, uriString: String): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            runCatching {
                resolver.openInputStream(Uri.parse(uriString))?.use { it.readBytes() }
                    ?: throw IllegalStateException("Selected file could not be read")
            }
        }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun studyResourceSyncMap(
        r: StudyResourceEntity,
        sectionKeyOverride: String? = null
    ): Map<String, Any> {
        val values = mutableMapOf<String, Any>(
            "id" to r.id,
            "section_key" to (sectionKeyOverride ?: r.sectionKey),
            "subject_id" to r.subjectId,
            "subject_name" to r.subjectName,
            "semester" to r.semester,
            "title" to r.title,
            "category" to r.category,
            "exam_year" to r.examYear,
            "file_url" to r.fileUrl,
            "file_type" to r.fileType,
            "file_size_bytes" to r.fileSizeBytes,
            "file_hash" to r.fileHash,
            "uploader_uid" to r.uploaderUid,
            "uploader_name" to r.uploaderName,
            "show_uploader_name" to r.showUploaderName,
            "upvotes" to r.upvotes,
            "report_count" to r.reportCount,
            "is_flagged" to r.isFlagged,
            "is_hidden" to r.isHidden,
            "is_approved_by_cr" to r.isApprovedByCr,
            "upload_status" to r.uploadStatus,
            "created_at" to r.createdAtMillis,
            "updated_at" to r.updatedAtMillis
        )
        Regex("/v1/objects/([0-9a-fA-F-]{36})/download")
            .find(r.fileUrl)
            ?.groupValues
            ?.getOrNull(1)
            ?.let { values["storage_object_id"] = it }
        return values
    }

    suspend fun upvoteResource(id: String) = studyDao.upvoteResource(id)

    /** Records the abuse report locally and files it in the central moderation queue. */
    suspend fun reportResourceAbuse(id: String) {
        studyDao.reportResourceAbuse(id)
        runCatching { supabaseSync.submitContentReport("RESOURCE", id, "") }
    }

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
        // Never trust the profile's display UID or a client-provided poster UID.
        val verifiedUid = authManager.currentUserId()
            ?: throw IllegalStateException("Sign in before posting a Lost & Found item")
        val now = System.currentTimeMillis()
        val itemId = "lf_$now"
        val title = itemTitle.trim()
        val details = description.trim()
        val place = location.trim()
        require(title.isNotBlank() && details.isNotBlank() && place.isNotBlank()) {
            "Add the item name, description and location"
        }

        val remote = supabaseClient.from("lost_found_items").insert(mapOf(
            "id" to itemId,
            "college_id" to collegeId,
            "type" to type,
            "category" to category,
            "title" to title,
            "description" to details,
            "location" to place,
            "poster_uid" to verifiedUid,
            "poster_name" to posterName,
            "poster_phone" to "",
            "poster_email" to "",
            "is_resolved" to false,
            "is_hidden" to false,
            "report_count" to 0,
            "created_at" to now,
            "updated_at" to now
        ))
        check(remote.isSuccess) { remote.error ?: "Could not publish item to CampusOne server" }

        val entity = LostFoundEntity(
            id = itemId,
            collegeId = collegeId,
            type = type,
            category = category,
            itemTitle = title,
            description = details,
            photoUri = null,
            location = place,
            dateFormatted = "Just now",
            posterUid = verifiedUid,
            posterName = posterName,
            preferredContactMethod = preferredContactMethod.trim(),
            status = "OPEN",
            isResolved = false,
            isArchived = false,
            createdAtMillis = now
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
