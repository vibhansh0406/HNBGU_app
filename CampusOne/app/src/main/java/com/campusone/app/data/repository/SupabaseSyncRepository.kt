package com.campusone.app.data.repository

import androidx.room.withTransaction
import com.campusone.app.config.FeatureFlags
import com.campusone.app.config.SupabaseRuntimeConfig
import com.campusone.app.data.local.CampusOneDatabase
import com.campusone.app.data.local.entity.AssignmentEntity
import com.campusone.app.data.local.entity.AttendanceRecordEntity
import com.campusone.app.data.local.entity.ExamEntity
import com.campusone.app.data.local.entity.PersonalNoticeEntity
import com.campusone.app.data.local.entity.TimetableSlotEntity
import com.campusone.app.data.preferences.UserPreferencesRepository
import com.campusone.app.data.remote.SupabaseCampusClient
import com.campusone.app.domain.model.LoginIntentPath
import com.campusone.app.domain.model.RemoteAuditLogDto
import com.campusone.app.domain.model.RemoteDepartmentDto
import com.campusone.app.domain.model.RemoteProfileDto
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SupabaseSyncRepository(
    private val client: SupabaseCampusClient,
    private val database: CampusOneDatabase,
    private val userPreferences: UserPreferencesRepository
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    suspend fun activeRuntimeConfig(): SupabaseRuntimeConfig {
        val state = userPreferences.getCloudAuthOnce()
        return FeatureFlags.resolveConfig(
            overrideUrl = state.overrideSupabaseUrl,
            overrideKey = state.overrideSupabaseKey,
            overrideGoogleClientId = state.overrideGoogleClientId
        )
    }

    suspend fun authenticateWithGoogleIdToken(
        idToken: String,
        intentPath: LoginIntentPath
    ): Result<RemoteProfileDto> {
        val config = activeRuntimeConfig()
        return runCatching {
            val session = client.signInWithGoogleIdToken(config, idToken).getOrThrow()
            val profile = client.fetchCurrentUserProfile(
                config = config,
                accessToken = session.accessToken,
                userId = session.userId
            ).getOrThrow()

            userPreferences.saveSupabaseSessionAndProfile(
                session = session,
                profile = profile,
                intentPath = intentPath
            )

            // Register FCM token if available
            val fcmToken = userPreferences.getCloudAuthOnce().fcmDeviceToken
            val localProfile = userPreferences.getProfileOnce()
            if (fcmToken.isNotBlank()) {
                client.upsertDeviceToken(
                    config = config,
                    accessToken = session.accessToken,
                    userId = session.userId,
                    fcmToken = fcmToken,
                    departmentId = profile.departmentId,
                    semester = localProfile.semester
                )
            }

            syncLiveCampusContentToRoom(session.accessToken, session.userId)
            profile
        }
    }

    suspend fun refreshSessionAndProfileIfNeeded(): Result<RemoteProfileDto?> {
        val config = activeRuntimeConfig()
        val authState = userPreferences.getCloudAuthOnce()
        val currentSession = authState.session ?: return Result.success(null)
        if (!config.isSupabaseConfigured) return Result.success(authState.remoteProfile)

        return runCatching {
            val nowSec = System.currentTimeMillis() / 1000L
            val activeSession = if (currentSession.expiresAtEpochSec > 0L &&
                nowSec >= currentSession.expiresAtEpochSec - 90L &&
                currentSession.refreshToken.isNotBlank()
            ) {
                client.refreshSession(config, currentSession.refreshToken).getOrDefault(currentSession)
            } else {
                currentSession
            }

            val freshProfile = client.fetchCurrentUserProfile(
                config = config,
                accessToken = activeSession.accessToken,
                userId = activeSession.userId
            ).getOrThrow()

            userPreferences.saveSupabaseSessionAndProfile(
                session = activeSession,
                profile = freshProfile,
                intentPath = authState.loginIntentPath
            )

            syncLiveCampusContentToRoom(activeSession.accessToken, activeSession.userId)
            freshProfile
        }
    }

    suspend fun syncLiveCampusContentToRoom(
        accessToken: String,
        userId: String
    ): Result<Int> {
        val config = activeRuntimeConfig()
        if (!config.isSupabaseConfigured || accessToken.isBlank()) {
            return Result.success(0)
        }
        return runCatching {
            val userProfile = userPreferences.getProfileOnce()
            val notices = client.fetchPublishedNotices(config, accessToken).getOrDefault(emptyList())
            val timetable = client.fetchPublishedTimetable(config, accessToken, userProfile.semester)
                .getOrDefault(emptyList())
            val assignments = client.fetchPublishedAssignments(config, accessToken, userProfile.semester)
                .getOrDefault(emptyList())
            val exams = client.fetchPublishedExams(config, accessToken, userProfile.semester)
                .getOrDefault(emptyList())
            val attendance = client.fetchStudentOwnAttendance(config, accessToken, userId)
                .getOrDefault(emptyList())

            database.withTransaction {
                val coreDao = database.coreDao()
                val studyDao = database.studyAndCampusDao()

                if (notices.isNotEmpty()) {
                    coreDao.upsertPersonalNotices(
                        notices.map { n ->
                            val createdMs = runCatching {
                                Instant.parse(n.publishedAt ?: n.createdAt ?: Instant.now().toString()).toEpochMilli()
                            }.getOrDefault(System.currentTimeMillis())
                            PersonalNoticeEntity(
                                id = "sb_notice_${n.id}",
                                title = n.title,
                                body = n.content,
                                category = n.priority,
                                isPinned = n.isPinned,
                                isRead = false,
                                createdAtMillis = createdMs
                            )
                        }
                    )
                }

                if (timetable.isNotEmpty()) {
                    coreDao.upsertTimetableSlots(
                        timetable.map { t ->
                            TimetableSlotEntity(
                                id = "sb_slot_${t.id}",
                                subjectId = "sb_sub_${t.subjectCode.lowercase()}",
                                subjectName = t.subjectName,
                                subjectCode = t.subjectCode,
                                dayOfWeek = t.weekday.coerceIn(1, 7),
                                startTime = t.startTime,
                                endTime = t.endTime,
                                room = t.room,
                                teacherName = t.facultyName,
                                semester = t.semester,
                                section = t.section,
                                colorHex = t.colorHex.ifBlank { "#0F4C81" }
                            )
                        }
                    )
                }

                if (assignments.isNotEmpty()) {
                    studyDao.upsertAssignments(
                        assignments.map { a ->
                            val dueMs = runCatching {
                                Instant.parse(a.dueAt).toEpochMilli()
                            }.getOrDefault(System.currentTimeMillis() + 86_400_000L)
                            AssignmentEntity(
                                id = "sb_asg_${a.id}",
                                title = a.title,
                                subjectName = a.subjectName,
                                dueEpochMillis = dueMs,
                                priority = a.priority,
                                notes = a.description,
                                isCompleted = false,
                                updatedAtMillis = System.currentTimeMillis()
                            )
                        }
                    )
                }

                if (exams.isNotEmpty()) {
                    studyDao.upsertExams(
                        exams.map { e ->
                            val examEpoch = runCatching {
                                LocalDate.parse(e.examDate).toEpochDay() * 86_400_000L
                            }.getOrDefault(System.currentTimeMillis() + 7 * 86_400_000L)
                            ExamEntity(
                                id = "sb_ex_${e.id}",
                                subjectName = e.subjectName,
                                subjectCode = e.subjectCode,
                                examTitle = "${e.examType}: ${e.examTitle}",
                                examEpochMillis = examEpoch,
                                timeSlot = "${e.startTime} - ${e.endTime}",
                                venue = e.venue,
                                notes = e.instructions
                            )
                        }
                    )
                }

                if (attendance.isNotEmpty()) {
                    coreDao.insertAttendanceRecords(
                        attendance.map { att ->
                            val date = runCatching { LocalDate.parse(att.attendanceDate) }
                                .getOrDefault(LocalDate.now())
                            AttendanceRecordEntity(
                                id = "sb_att_${att.id}",
                                subjectId = "sb_sub_${att.subjectCode.lowercase()}",
                                subjectName = att.subjectName,
                                epochDay = date.toEpochDay(),
                                dateLabel = date.format(dateFormatter),
                                status = att.status,
                                timestampMillis = System.currentTimeMillis()
                            )
                        }
                    )
                }
            }

            notices.size + timetable.size + assignments.size + exams.size + attendance.size
        }
    }

    suspend fun signOut() {
        val config = activeRuntimeConfig()
        val session = userPreferences.getCloudAuthOnce().session
        if (session != null) {
            client.signOut(config, session.accessToken)
        }
        userPreferences.clearSupabaseSession()
    }

    suspend fun fetchDepartmentsForAdmin(): Result<List<RemoteDepartmentDto>> {
        val config = activeRuntimeConfig()
        val token = userPreferences.getCloudAuthOnce().session?.accessToken.orEmpty()
        return client.fetchDepartments(config, token)
    }

    suspend fun createNoticeFromAdminConsole(
        title: String,
        content: String,
        departmentId: String?,
        priority: String,
        isPinned: Boolean,
        publishImmediately: Boolean
    ): Result<Unit> {
        val config = activeRuntimeConfig()
        val auth = userPreferences.getCloudAuthOnce()
        val session = auth.session ?: return Result.failure(IllegalStateException("Not authenticated."))
        return client.createNoticeAsAdmin(
            config = config,
            accessToken = session.accessToken,
            title = title,
            content = content,
            departmentId = departmentId,
            priority = priority,
            isPinned = isPinned,
            publishImmediately = publishImmediately,
            creatorId = session.userId
        ).also {
            if (it.isSuccess) {
                syncLiveCampusContentToRoom(session.accessToken, session.userId)
            }
        }
    }

    suspend fun fetchProfilesForAdmin(): Result<List<RemoteProfileDto>> {
        val config = activeRuntimeConfig()
        val token = userPreferences.getCloudAuthOnce().session?.accessToken.orEmpty()
        return client.fetchProfilesForAdmin(config, token)
    }

    suspend fun assignRoleAsSuperAdmin(
        targetUserId: String,
        newRole: String,
        newDepartmentId: String?,
        newAccountStatus: String
    ): Result<RemoteProfileDto> {
        val config = activeRuntimeConfig()
        val token = userPreferences.getCloudAuthOnce().session?.accessToken.orEmpty()
        return client.assignUserRoleAsSuperAdmin(
            config = config,
            accessToken = token,
            targetUserId = targetUserId,
            newRole = newRole,
            newDepartmentId = newDepartmentId,
            newAccountStatus = newAccountStatus
        )
    }

    suspend fun fetchAuditLogsForSuperAdmin(): Result<List<RemoteAuditLogDto>> {
        val config = activeRuntimeConfig()
        val token = userPreferences.getCloudAuthOnce().session?.accessToken.orEmpty()
        return client.fetchAuditLogsForSuperAdmin(config, token)
    }
}
