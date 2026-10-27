package com.campusone.app.data.remote

import com.campusone.app.config.SupabaseRuntimeConfig
import com.campusone.app.domain.model.RemoteAssignmentDto
import com.campusone.app.domain.model.RemoteAttendanceRecordDto
import com.campusone.app.domain.model.RemoteAuditLogDto
import com.campusone.app.domain.model.RemoteDepartmentDto
import com.campusone.app.domain.model.RemoteExamEventDto
import com.campusone.app.domain.model.RemoteNoticeDto
import com.campusone.app.domain.model.RemoteProfileDto
import com.campusone.app.domain.model.RemoteTimetableEntryDto
import com.campusone.app.domain.model.SupabaseAuthSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

@Serializable
private data class SupabaseTokenResponse(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("expires_in") val expiresIn: Long = 3600L,
    @SerialName("expires_at") val expiresAt: Long? = null,
    val user: SupabaseUserObject? = null
)

@Serializable
private data class SupabaseUserObject(
    val id: String = "",
    val email: String = ""
)

@Serializable
private data class SupabaseErrorPayload(
    val message: String? = null,
    val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
    val hint: String? = null
)

/**
 * Reusable, thread-safe Supabase Auth & PostgREST client for CampusOne Android.
 * Uses only the public SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY; all access control
 * is enforced server-side by PostgreSQL Row Level Security (RLS).
 */
class SupabaseCampusClient {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    suspend fun signInWithGoogleIdToken(
        config: SupabaseRuntimeConfig,
        idToken: String,
        rawNonce: String? = null
    ): Result<SupabaseAuthSession> = withContext(Dispatchers.IO) {
        runCatching {
            require(config.isSupabaseConfigured) { config.missingConfigDiagnosis() }
            val endpoint = "${config.supabaseUrl}/auth/v1/token?grant_type=id_token"
            val payload = buildJsonObject {
                put("provider", "google")
                put("id_token", idToken)
                if (!rawNonce.isNullOrBlank()) {
                    put("nonce", rawNonce)
                }
            }
            val rawResponse = executeJsonRequest(
                url = endpoint,
                method = "POST",
                publishableKey = config.supabasePublishableKey,
                bearerToken = null,
                bodyJson = json.encodeToString(JsonObject.serializer(), payload)
            )
            val parsed = json.decodeFromString<SupabaseTokenResponse>(rawResponse)
            val userId = parsed.user?.id.orEmpty()
            if (parsed.accessToken.isBlank() || userId.isBlank()) {
                throw IllegalStateException("Supabase Auth did not return a valid session token.")
            }
            val nowSec = System.currentTimeMillis() / 1000L
            SupabaseAuthSession(
                accessToken = parsed.accessToken,
                refreshToken = parsed.refreshToken,
                expiresAtEpochSec = parsed.expiresAt ?: (nowSec + parsed.expiresIn),
                userId = userId,
                email = parsed.user?.email.orEmpty()
            )
        }
    }

    suspend fun refreshSession(
        config: SupabaseRuntimeConfig,
        refreshToken: String
    ): Result<SupabaseAuthSession> = withContext(Dispatchers.IO) {
        runCatching {
            require(config.isSupabaseConfigured) { config.missingConfigDiagnosis() }
            val endpoint = "${config.supabaseUrl}/auth/v1/token?grant_type=refresh_token"
            val payload = buildJsonObject {
                put("refresh_token", refreshToken)
            }
            val rawResponse = executeJsonRequest(
                url = endpoint,
                method = "POST",
                publishableKey = config.supabasePublishableKey,
                bearerToken = null,
                bodyJson = json.encodeToString(JsonObject.serializer(), payload)
            )
            val parsed = json.decodeFromString<SupabaseTokenResponse>(rawResponse)
            val userId = parsed.user?.id.orEmpty()
            val nowSec = System.currentTimeMillis() / 1000L
            SupabaseAuthSession(
                accessToken = parsed.accessToken,
                refreshToken = parsed.refreshToken.ifBlank { refreshToken },
                expiresAtEpochSec = parsed.expiresAt ?: (nowSec + parsed.expiresIn),
                userId = userId,
                email = parsed.user?.email.orEmpty()
            )
        }
    }

    suspend fun signOut(
        config: SupabaseRuntimeConfig,
        accessToken: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!config.isSupabaseConfigured || accessToken.isBlank()) return@runCatching
            val endpoint = "${config.supabaseUrl}/auth/v1/logout"
            executeJsonRequest(
                url = endpoint,
                method = "POST",
                publishableKey = config.supabasePublishableKey,
                bearerToken = accessToken,
                bodyJson = null
            )
        }
    }

    suspend fun fetchCurrentUserProfile(
        config: SupabaseRuntimeConfig,
        accessToken: String,
        userId: String
    ): Result<RemoteProfileDto> = withContext(Dispatchers.IO) {
        runCatching {
            require(config.isSupabaseConfigured) { config.missingConfigDiagnosis() }
            val encodedId = URLEncoder.encode("eq.$userId", "UTF-8")
            val endpoint = "${config.supabaseUrl}/rest/v1/profiles?id=$encodedId&select=*"
            val raw = executeJsonRequest(
                url = endpoint,
                method = "GET",
                publishableKey = config.supabasePublishableKey,
                bearerToken = accessToken
            )
            val list = json.decodeFromString<List<RemoteProfileDto>>(raw)
            list.firstOrNull()
                ?: throw IllegalStateException("Profile row not found for user $userId.")
        }
    }

    suspend fun fetchDepartments(
        config: SupabaseRuntimeConfig,
        accessToken: String
    ): Result<List<RemoteDepartmentDto>> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = "${config.supabaseUrl}/rest/v1/departments?select=*&order=code.asc"
            val raw = executeJsonRequest(
                url = endpoint,
                method = "GET",
                publishableKey = config.supabasePublishableKey,
                bearerToken = accessToken
            )
            json.decodeFromString<List<RemoteDepartmentDto>>(raw)
        }
    }

    suspend fun fetchPublishedNotices(
        config: SupabaseRuntimeConfig,
        accessToken: String,
        limit: Int = 50
    ): Result<List<RemoteNoticeDto>> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = "${config.supabaseUrl}/rest/v1/notices?select=*&order=is_pinned.desc,created_at.desc&limit=$limit"
            val raw = executeJsonRequest(
                url = endpoint,
                method = "GET",
                publishableKey = config.supabasePublishableKey,
                bearerToken = accessToken
            )
            json.decodeFromString<List<RemoteNoticeDto>>(raw)
        }
    }

    suspend fun fetchPublishedTimetable(
        config: SupabaseRuntimeConfig,
        accessToken: String,
        semester: Int
    ): Result<List<RemoteTimetableEntryDto>> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = "${config.supabaseUrl}/rest/v1/timetable_entries?semester=eq.$semester&select=*&order=weekday.asc,start_time.asc&limit=100"
            val raw = executeJsonRequest(
                url = endpoint,
                method = "GET",
                publishableKey = config.supabasePublishableKey,
                bearerToken = accessToken
            )
            json.decodeFromString<List<RemoteTimetableEntryDto>>(raw)
        }
    }

    suspend fun fetchPublishedAssignments(
        config: SupabaseRuntimeConfig,
        accessToken: String,
        semester: Int
    ): Result<List<RemoteAssignmentDto>> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = "${config.supabaseUrl}/rest/v1/assignments?semester=eq.$semester&select=*&order=due_at.asc&limit=50"
            val raw = executeJsonRequest(
                url = endpoint,
                method = "GET",
                publishableKey = config.supabasePublishableKey,
                bearerToken = accessToken
            )
            json.decodeFromString<List<RemoteAssignmentDto>>(raw)
        }
    }

    suspend fun fetchPublishedExams(
        config: SupabaseRuntimeConfig,
        accessToken: String,
        semester: Int
    ): Result<List<RemoteExamEventDto>> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = "${config.supabaseUrl}/rest/v1/exam_events?semester=eq.$semester&select=*&order=exam_date.asc&limit=50"
            val raw = executeJsonRequest(
                url = endpoint,
                method = "GET",
                publishableKey = config.supabasePublishableKey,
                bearerToken = accessToken
            )
            json.decodeFromString<List<RemoteExamEventDto>>(raw)
        }
    }

    suspend fun fetchStudentOwnAttendance(
        config: SupabaseRuntimeConfig,
        accessToken: String,
        studentProfileId: String
    ): Result<List<RemoteAttendanceRecordDto>> = withContext(Dispatchers.IO) {
        runCatching {
            val encodedId = URLEncoder.encode("eq.$studentProfileId", "UTF-8")
            val endpoint = "${config.supabaseUrl}/rest/v1/attendance_records?student_profile_id=$encodedId&select=*&order=attendance_date.desc&limit=200"
            val raw = executeJsonRequest(
                url = endpoint,
                method = "GET",
                publishableKey = config.supabasePublishableKey,
                bearerToken = accessToken
            )
            json.decodeFromString<List<RemoteAttendanceRecordDto>>(raw)
        }
    }

    suspend fun createNoticeAsAdmin(
        config: SupabaseRuntimeConfig,
        accessToken: String,
        title: String,
        content: String,
        departmentId: String?,
        priority: String,
        isPinned: Boolean,
        publishImmediately: Boolean,
        creatorId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching<Unit> {
            val endpoint = "${config.supabaseUrl}/rest/v1/notices"
            val payload = buildJsonObject {
                put("title", title.trim())
                put("content", content.trim())
                if (!departmentId.isNullOrBlank()) {
                    put("department_id", departmentId)
                }
                put("priority", priority)
                put("is_pinned", isPinned)
                put("status", if (publishImmediately) "published" else "draft")
                put("created_by", creatorId)
                if (publishImmediately) {
                    put("published_at", java.time.Instant.now().toString())
                }
            }
            executeJsonRequest(
                url = endpoint,
                method = "POST",
                publishableKey = config.supabasePublishableKey,
                bearerToken = accessToken,
                bodyJson = json.encodeToString(JsonObject.serializer(), payload),
                preferHeader = "return=minimal"
            )
        }
    }

    suspend fun fetchProfilesForAdmin(
        config: SupabaseRuntimeConfig,
        accessToken: String
    ): Result<List<RemoteProfileDto>> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = "${config.supabaseUrl}/rest/v1/profiles?select=*&order=created_at.desc&limit=100"
            val raw = executeJsonRequest(
                url = endpoint,
                method = "GET",
                publishableKey = config.supabasePublishableKey,
                bearerToken = accessToken
            )
            json.decodeFromString<List<RemoteProfileDto>>(raw)
        }
    }

    suspend fun assignUserRoleAsSuperAdmin(
        config: SupabaseRuntimeConfig,
        accessToken: String,
        targetUserId: String,
        newRole: String,
        newDepartmentId: String?,
        newAccountStatus: String
    ): Result<RemoteProfileDto> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = "${config.supabaseUrl}/rest/v1/rpc/admin_assign_user_role"
            val payload = buildJsonObject {
                put("target_user_id", targetUserId)
                put("new_role", newRole)
                if (!newDepartmentId.isNullOrBlank()) {
                    put("new_department_id", newDepartmentId)
                }
                put("new_account_status", newAccountStatus)
            }
            val raw = executeJsonRequest(
                url = endpoint,
                method = "POST",
                publishableKey = config.supabasePublishableKey,
                bearerToken = accessToken,
                bodyJson = json.encodeToString(JsonObject.serializer(), payload)
            )
            json.decodeFromString<RemoteProfileDto>(raw)
        }
    }

    suspend fun fetchAuditLogsForSuperAdmin(
        config: SupabaseRuntimeConfig,
        accessToken: String
    ): Result<List<RemoteAuditLogDto>> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = "${config.supabaseUrl}/rest/v1/audit_logs?select=*&order=created_at.desc&limit=50"
            val raw = executeJsonRequest(
                url = endpoint,
                method = "GET",
                publishableKey = config.supabasePublishableKey,
                bearerToken = accessToken
            )
            json.decodeFromString<List<RemoteAuditLogDto>>(raw)
        }
    }

    suspend fun upsertDeviceToken(
        config: SupabaseRuntimeConfig,
        accessToken: String,
        userId: String,
        fcmToken: String,
        departmentId: String?,
        semester: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!config.isSupabaseConfigured || fcmToken.isBlank()) return@runCatching
            val endpoint = "${config.supabaseUrl}/rest/v1/device_tokens?on_conflict=fcm_token"
            val payload = buildJsonObject {
                put("user_id", userId)
                put("fcm_token", fcmToken)
                put("platform", "android")
                if (!departmentId.isNullOrBlank()) {
                    put("department_id", departmentId)
                }
                put("semester", semester.coerceIn(1, 10))
                put("is_active", true)
                put("last_seen_at", java.time.Instant.now().toString())
            }
            executeJsonRequest(
                url = endpoint,
                method = "POST",
                publishableKey = config.supabasePublishableKey,
                bearerToken = accessToken,
                bodyJson = json.encodeToString(JsonObject.serializer(), payload),
                preferHeader = "resolution=merge-duplicates,return=minimal"
            )
        }
    }

    private fun executeJsonRequest(
        url: String,
        method: String,
        publishableKey: String,
        bearerToken: String?,
        bodyJson: String? = null,
        preferHeader: String? = null
    ): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("apikey", publishableKey)
            setRequestProperty("Authorization", "Bearer ${bearerToken ?: publishableKey}")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            if (!preferHeader.isNullOrBlank()) {
                setRequestProperty("Prefer", preferHeader)
            }
            if (bodyJson != null) {
                doOutput = true
            }
        }

        try {
            if (bodyJson != null) {
                conn.outputStream.use { out ->
                    out.write(bodyJson.toByteArray(Charsets.UTF_8))
                }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = if (stream != null) {
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            } else {
                ""
            }
            if (code !in 200..299) {
                val parsedErr = runCatching {
                    json.decodeFromString<SupabaseErrorPayload>(text)
                }.getOrNull()
                val friendly = parsedErr?.errorDescription
                    ?: parsedErr?.message
                    ?: parsedErr?.error
                    ?: text.take(220).ifBlank { "HTTP $code from Supabase" }
                throw IllegalStateException(friendly)
            }
            return text
        } finally {
            conn.disconnect()
        }
    }
}
