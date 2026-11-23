package `in`.campusone.app.data.remote

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseSyncRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabaseClient: SupabaseClient,
    private val authManager: AuthManager
) {

    private val currentUserId: String?
        get() = authManager.currentUserId()

    /** Check if Supabase is configured and user is signed in. */
    fun isSyncedEnabled(): Boolean = currentUserId != null

    companion object {
        /** Upper bound on rows pulled for any admin listing. */
        const val ADMIN_PAGE_LIMIT = 500
        private const val TAG = "CampusOneSync"
        private val LEGACY_NOTICE_COLUMNS = setOf(
            "id", "section_key", "title", "body", "attachment_url",
            "attachment_type", "is_pinned", "author_uid", "author_name",
            "author_role", "created_at", "is_read"
        )
    }

    /** Authoritative identity/permission snapshot returned by the database. */
    data class AdminState(
        val role: String,
        val isOwner: Boolean,
        val isSuspended: Boolean,
        val permissions: Set<String>
    ) {
        fun can(permission: String): Boolean = isOwner || permission in permissions
    }

    // =========================================================================
    // PROFILES SYNC
    // =========================================================================
    suspend fun uploadProfile(
        uid: String,
        name: String,
        email: String,
        role: String,
        collegeId: String,
        collegeName: String,
        courseId: String,
        courseName: String,
        semester: Int,
        section: String,
        moderatorScope: String = "",
        isModeratorApproved: Boolean = false,
        onboardingCompleted: Boolean = false
    ): Boolean {
        if (!isSyncedEnabled()) return false
        return runCatching {
            supabaseClient.from("profiles").upsert(mapOf(
                "id" to uid,
                "full_name" to name,
                "email" to email,
                "role" to role,
                "college_id" to collegeId,
                "college_name" to collegeName,
                "course_id" to courseId,
                "course_name" to courseName,
                "semester" to semester,
                "section" to section,
                "moderator_scope" to moderatorScope,
                "is_moderator_approved" to isModeratorApproved,
                "onboarding_completed" to onboardingCompleted,
                "updated_at" to System.currentTimeMillis()
            ))
            true
        }.getOrDefault(false)
    }

    suspend fun downloadProfile(uid: String): Map<String, Any>? {
        if (!isSyncedEnabled()) return null
        val result = supabaseClient.from("profiles").select("*").eq("id", uid).execute()
        return (result.data as? List<Map<String, Any>>)?.firstOrNull()
    }

    // =========================================================================
    // SERVER-VERIFIED ADMIN OPERATIONS
    // Every privileged mutation goes through a SECURITY DEFINER function that
    // checks the caller's permission server-side and writes an audit row.
    // =========================================================================

    /** Reads the caller's authoritative role/permission set from the database. */
    suspend fun fetchAdminState(): AdminState? {
        if (!isSyncedEnabled()) return null
        val response = supabaseClient.rpc("campusone_my_admin_state")
        if (!response.isSuccess) return null
        val map = response.data as? Map<String, Any> ?: return null
        if (map["signedIn"] != true) return null
        val permissions = (map["permissions"] as? List<*>)?.mapNotNull { it as? String }?.toSet()
            ?: emptySet()
        return AdminState(
            role = map["role"] as? String ?: "STUDENT",
            isOwner = map["isOwner"] == true,
            isSuspended = map["isSuspended"] == true,
            permissions = permissions
        )
    }

    suspend fun bootstrapProfile(name: String, email: String): Boolean {
        if (!isSyncedEnabled()) return false
        return runCatching {
            val response = supabaseClient.rpc(
                "campusone_bootstrap_profile",
                mapOf("p_name" to name, "p_email" to email)
            )
            response.isSuccess && (response.data as? Map<String, Any>)?.get("ok") == true
        }.getOrDefault(false)
    }

    suspend fun adminSetRole(uid: String, role: String): Boolean =
        callAdmin("admin_set_role", mapOf("p_uid" to uid, "p_role" to role))

    suspend fun adminApproveRoleRequest(requestId: String): Boolean =
        callAdmin("admin_approve_role_request", mapOf("p_request" to requestId))

    suspend fun adminRejectRoleRequest(requestId: String): Boolean =
        callAdmin("admin_reject_role_request", mapOf("p_request" to requestId))

    suspend fun adminSetSuspended(uid: String, suspended: Boolean): Boolean =
        callAdmin("admin_set_suspended", mapOf("p_uid" to uid, "p_suspended" to suspended))

    suspend fun adminDeleteUser(uid: String): Boolean =
        callAdmin("admin_delete_user", mapOf("p_uid" to uid))

    suspend fun adminModerateContent(targetType: String, targetId: String, action: String): Boolean =
        callAdmin(
            "admin_moderate_content",
            mapOf("p_target_type" to targetType, "p_target_id" to targetId, "p_action" to action)
        )

    suspend fun adminUpdateSettings(settings: Map<String, Any>): Boolean =
        callAdmin("admin_update_settings", mapOf("p_settings" to settings))

    suspend fun ownerRecoverAdmin(): Boolean =
        callAdmin("owner_recover_admin")

    private suspend fun callAdmin(function: String, params: Map<String, Any> = emptyMap()): Boolean {
        if (!isSyncedEnabled()) return false
        val response = supabaseClient.rpc(function, params)
        if (!response.isSuccess) return false
        return (response.data as? Map<String, Any>)?.get("ok") == true
    }

    suspend fun downloadAuditLogs(limit: Int = 200): List<Map<String, Any>> {
        if (!isSyncedEnabled()) return emptyList()
        val result = supabaseClient.from("audit_logs")
            .select("*")
            .order("created_at", ascending = false)
            .limit(limit)
            .execute()
        return result.data as? List<Map<String, Any>> ?: emptyList()
    }

    suspend fun downloadContentReports(): List<Map<String, Any>> {
        if (!isSyncedEnabled()) return emptyList()
        val result = supabaseClient.from("content_reports")
            .select("*")
            .order("created_at", ascending = false)
            .limit(ADMIN_PAGE_LIMIT)
            .execute()
        return result.data as? List<Map<String, Any>> ?: emptyList()
    }

    /** Files a report against a content item in the central moderation queue. */
    suspend fun submitContentReport(targetType: String, targetId: String, reason: String): Boolean {
        if (!isSyncedEnabled()) return false
        val uid = currentUserId ?: return false
        val result = supabaseClient.from("content_reports").insert(mapOf(
            "target_type" to targetType,
            "target_id" to targetId,
            "reason" to reason,
            "reporter_id" to uid,
            "reporter_email" to (authManager.currentUserEmail() ?: ""),
            "status" to "PENDING",
            "created_at" to System.currentTimeMillis()
        ))
        return result.isSuccess
    }

    suspend fun downloadPlatformSettings(): Map<String, Any>? {
        if (!isSyncedEnabled()) return null
        val result = supabaseClient.from("platform_settings")
            .select("*")
            .eq("id", "global")
            .execute()
        return (result.data as? List<Map<String, Any>>)?.firstOrNull()
    }

    suspend fun downloadAllProfiles(): List<Map<String, Any>> {
        if (!isSyncedEnabled()) return emptyList()
        val result = supabaseClient.from("profiles")
            .select("*")
            .order("updated_at", ascending = false)
            .limit(ADMIN_PAGE_LIMIT)
            .execute()
        return result.data as? List<Map<String, Any>> ?: emptyList()
    }

    /**
     * Repairs the profile directory so every registered auth user has a row.
     * Server-side, permission-checked and idempotent; a missing function is a
     * no-op so older databases keep working until the migration is applied.
     */
    suspend fun syncRegisteredProfiles(): Boolean =
        callAdmin("campusone_admin_sync_profiles")

    /**
     * Read-only view of the legacy roster kept by the previous backend. Used to
     * avoid under-counting registrations that have no profile row yet.
     */
    suspend fun downloadLegacyStudents(): List<Map<String, Any>> {
        if (!isSyncedEnabled()) return emptyList()
        val result = supabaseClient.from("students")
            .select("id,email,full_name,role,course,created_at,updated_at")
            .order("created_at", ascending = false)
            .limit(ADMIN_PAGE_LIMIT)
            .execute()
        if (!result.isSuccess) return emptyList()
        return result.data as? List<Map<String, Any>> ?: emptyList()
    }

    suspend fun downloadRoleRequests(): List<Map<String, Any>> {
        if (!isSyncedEnabled()) return emptyList()
        val result = supabaseClient.from("role_requests")
            .select("*")
            .order("created_at", ascending = false)
            .limit(ADMIN_PAGE_LIMIT)
            .execute()
        return result.data as? List<Map<String, Any>> ?: emptyList()
    }

    /** Files a role-change request for the signed-in user; admins approve it via RPC. */
    suspend fun submitRoleRequest(uid: String, email: String, role: String, note: String): Boolean {
        if (!isSyncedEnabled()) return false
        val result = supabaseClient.from("role_requests").insert(mapOf(
            "user_id" to uid,
            "email" to email,
            "requested_role" to role,
            "note" to note,
            "status" to "PENDING",
            "created_at" to System.currentTimeMillis()
        ))
        return result.isSuccess
    }

    suspend fun downloadFlaggedResources(): List<Map<String, Any>> {
        if (!isSyncedEnabled()) return emptyList()
        val result = supabaseClient.from("study_resources")
            .select("*")
            .or("is_flagged.eq.true,report_count.gt.0")
            .order("report_count", ascending = false)
            .execute()
        return result.data as? List<Map<String, Any>> ?: emptyList()
    }

    suspend fun downloadFlaggedClubs(): List<Map<String, Any>> {
        if (!isSyncedEnabled()) return emptyList()
        val result = supabaseClient.from("clubs")
            .select("*")
            .eq("is_flagged", true)
            .execute()
        return result.data as? List<Map<String, Any>> ?: emptyList()
    }

    suspend fun downloadFlaggedEvents(): List<Map<String, Any>> {
        if (!isSyncedEnabled()) return emptyList()
        val result = supabaseClient.from("campus_events")
            .select("*")
            .eq("is_flagged", true)
            .execute()
        return result.data as? List<Map<String, Any>> ?: emptyList()
    }

    suspend fun downloadFlaggedCommunityPosts(): List<Map<String, Any>> {
        if (!isSyncedEnabled()) return emptyList()
        val result = supabaseClient.from("community_posts")
            .select("*")
            .or("report_count.gt.0,is_hidden.eq.true")
            .order("report_count", ascending = false)
            .limit(ADMIN_PAGE_LIMIT)
            .execute()
        return result.data as? List<Map<String, Any>> ?: emptyList()
    }

    suspend fun downloadFlaggedLostFound(): List<Map<String, Any>> {
        if (!isSyncedEnabled()) return emptyList()
        val result = supabaseClient.from("lost_found_items")
            .select("*")
            .or("is_hidden.eq.true,report_count.gt.0")
            .order("report_count", ascending = false)
            .execute()
        return result.data as? List<Map<String, Any>> ?: emptyList()
    }

    // =========================================================================
    // NOTICES SYNC
    // =========================================================================
    suspend fun uploadNotices(sectionKey: String, notices: List<Map<String, Any>>): Boolean {
        if (!isSyncedEnabled() || notices.isEmpty()) return false
        val result = supabaseClient.from("notices").upsert(notices)
        if (result.isSuccess) return true
        logFailure("notice upsert", result.error)
        // Older deployments have not run the announcements migration yet.
        // Retry with only the columns every version of the table has so an
        // announcement is never silently lost.
        val legacy = notices.map { notice ->
            notice.filterKeys { it in LEGACY_NOTICE_COLUMNS }
        }
        val retry = supabaseClient.from("notices").upsert(legacy)
        if (!retry.isSuccess) logFailure("notice upsert (legacy columns)", retry.error)
        return retry.isSuccess
    }

    suspend fun deleteNotice(noticeId: String): Boolean {
        if (!isSyncedEnabled()) return false
        val result = supabaseClient.from("notices").eq("id", noticeId).delete()
        if (!result.isSuccess) logFailure("notice delete", result.error)
        return result.isSuccess
    }

    suspend fun downloadNotices(sectionKey: String): Result<List<Map<String, Any>>> {
        if (!isSyncedEnabled()) return Result.success(emptyList())
        val result = supabaseClient.from("notices")
            .select("*")
            .eq("section_key", sectionKey)
            .order("created_at", ascending = false)
            .execute()
        if (!result.isSuccess) {
            logFailure("notice download", result.error)
            return Result.failure(IllegalStateException(safeError(result.error)))
        }
        return Result.success(result.data as? List<Map<String, Any>> ?: emptyList())
    }

    suspend fun downloadAttendanceLogs(uid: String): List<Map<String, Any>> {
        if (!isSyncedEnabled()) return emptyList()
        val result = supabaseClient.from("attendance_logs")
            .select("*")
            .eq("user_id", uid)
            .execute()
        return result.data as? List<Map<String, Any>> ?: emptyList()
    }

    // =========================================================================
    // TIMETABLE SYNC
    // =========================================================================
    /**
     * Returns the published timetable for exactly one student scope. A Result is
     * used so callers can distinguish "no timetable published" (success, empty)
     * from a network/server failure (failure) and keep cached rows on failure.
     */
    suspend fun downloadStudentTimetable(
        courseId: String,
        courseName: String,
        semester: Int,
        section: String
    ): Result<List<Map<String, Any>>> {
        if (!isSyncedEnabled()) return Result.success(emptyList())
        if (courseId.isBlank() && courseName.isBlank()) return Result.success(emptyList())

        val byCode = if (courseId.isNotBlank()) {
            queryTimetableScope("course_code", courseId, semester, section)
        } else {
            null
        }
        if (byCode != null && !byCode.isSuccess) {
            logFailure("timetable download", byCode.error)
            return Result.failure(IllegalStateException(safeError(byCode.error)))
        }
        var rows = byCode?.data as? List<Map<String, Any>> ?: emptyList()

        if (rows.isEmpty() && courseName.isNotBlank()) {
            val byName = queryTimetableScope("course_name", courseName, semester, section)
            if (!byName.isSuccess) {
                logFailure("timetable download by course name", byName.error)
                return Result.failure(IllegalStateException(safeError(byName.error)))
            }
            rows = byName.data as? List<Map<String, Any>> ?: emptyList()
        }
        return Result.success(rows.distinctBy { it["id"] })
    }

    private suspend fun queryTimetableScope(
        column: String,
        value: String,
        semester: Int,
        section: String
    ): SupabaseResponse =
        supabaseClient.from("timetable_slots")
            .select("*")
            .eq(column, value)
            .eq("semester", semester)
            .eq("section", section.uppercase())
            .order("day_of_week", ascending = true)
            .order("start_time", ascending = true)
            .execute()

    suspend fun listTimetableImports(): Result<List<Map<String, Any>>> {
        if (!isSyncedEnabled()) return Result.success(emptyList())
        val result = supabaseClient.from("timetable_imports")
            .select("*")
            .order("created_at", ascending = false)
            .limit(ADMIN_PAGE_LIMIT)
            .execute()
        if (!result.isSuccess) {
            logFailure("timetable import list", result.error)
            return Result.failure(IllegalStateException(safeError(result.error)))
        }
        return Result.success(result.data as? List<Map<String, Any>> ?: emptyList())
    }

    suspend fun publishTimetable(
        courseCode: String,
        courseName: String,
        semester: Int,
        section: String,
        academicYear: String,
        fileName: String,
        rows: List<Map<String, Any>>
    ): TimetablePublishOutcome {
        if (!isSyncedEnabled()) {
            return TimetablePublishOutcome(false, "Sign in to publish a timetable")
        }
        val result = supabaseClient.rpc(
            "campusone_publish_timetable",
            mapOf(
                "p_course_code" to courseCode,
                "p_course_name" to courseName,
                "p_semester" to semester,
                "p_section" to section,
                "p_academic_year" to academicYear,
                "p_file_name" to fileName,
                "p_slots" to rows
            )
        )
        if (!result.isSuccess) {
            logFailure("timetable publish", result.error)
            return TimetablePublishOutcome(false, safeError(result.error))
        }
        val payload = result.data as? Map<String, Any>
        if (payload == null || payload["ok"] != true) {
            val message = payload?.get("error") as? String ?: "The server rejected the timetable"
            return TimetablePublishOutcome(false, message)
        }
        val count = (payload["rowCount"] as? Number)?.toInt() ?: rows.size
        return TimetablePublishOutcome(true, "Published $count timetable rows", count)
    }

    suspend fun deleteTimetableImport(importId: String): TimetablePublishOutcome {
        if (!isSyncedEnabled()) {
            return TimetablePublishOutcome(false, "Sign in to delete a timetable")
        }
        val result = supabaseClient.rpc(
            "campusone_delete_timetable",
            mapOf("p_import_id" to importId)
        )
        if (!result.isSuccess) {
            logFailure("timetable delete", result.error)
            return TimetablePublishOutcome(false, safeError(result.error))
        }
        val payload = result.data as? Map<String, Any>
        if (payload?.get("ok") != true) {
            return TimetablePublishOutcome(
                false,
                payload?.get("error") as? String ?: "The server rejected the delete"
            )
        }
        return TimetablePublishOutcome(true, "Timetable deleted")
    }

    private fun logFailure(operation: String, error: String?) {
        Log.w(TAG, "$operation failed: ${safeError(error)}")
    }

    private fun safeError(error: String?): String {
        val redacted = (error ?: "unknown error")
            .replace(Regex("[A-Za-z0-9_\\-]{32,}"), "***")
        return redacted.take(240)
    }

    // =========================================================================
    // ASSIGNMENTS SYNC
    // =========================================================================
    suspend fun uploadAssignments(sectionKey: String, assignments: List<Map<String, Any>>) {
        if (!isSyncedEnabled() || assignments.isEmpty()) return
        supabaseClient.from("assignments").upsert(assignments)
    }

    suspend fun downloadAssignments(sectionKey: String): List<Map<String, Any>> {
        if (!isSyncedEnabled()) return emptyList()
        val result = supabaseClient.from("assignments")
            .select("*")
            .eq("section_key", sectionKey)
            .order("due_date_epoch_millis", ascending = true)
            .execute()
        return result.data as? List<Map<String, Any>> ?: emptyList()
    }

    // =========================================================================
    // EXAMS SYNC
    // =========================================================================
    suspend fun uploadExams(sectionKey: String, exams: List<Map<String, Any>>) {
        if (!isSyncedEnabled() || exams.isEmpty()) return
        supabaseClient.from("exams").upsert(exams)
    }

    suspend fun downloadExams(sectionKey: String): List<Map<String, Any>> {
        if (!isSyncedEnabled()) return emptyList()
        val result = supabaseClient.from("exams")
            .select("*")
            .eq("section_key", sectionKey)
            .order("exam_date_epoch_millis", ascending = true)
            .execute()
        return result.data as? List<Map<String, Any>> ?: emptyList()
    }

    // =========================================================================
    // STUDY RESOURCES SYNC
    // =========================================================================
    suspend fun uploadStudyResources(sectionKey: String, resources: List<Map<String, Any>>) {
        if (!isSyncedEnabled() || resources.isEmpty()) return
        supabaseClient.from("study_resources").upsert(resources)
    }

    suspend fun downloadStudyResources(sectionKey: String): List<Map<String, Any>> {
        if (!isSyncedEnabled()) return emptyList()
        val result = supabaseClient.from("study_resources")
            .select("*")
            .eq("section_key", sectionKey)
            .order("created_at", ascending = false)
            .execute()
        return result.data as? List<Map<String, Any>> ?: emptyList()
    }

    // =========================================================================
    // CLUBS SYNC
    // =========================================================================
    suspend fun uploadClubs(collegeId: String, clubs: List<Map<String, Any>>) {
        if (!isSyncedEnabled() || clubs.isEmpty()) return
        supabaseClient.from("clubs").upsert(clubs)
    }

    suspend fun downloadClubs(collegeId: String): List<Map<String, Any>> {
        if (!isSyncedEnabled()) return emptyList()
        val result = supabaseClient.from("clubs")
            .select("*")
            .eq("college_id", collegeId)
            .execute()
        return result.data as? List<Map<String, Any>> ?: emptyList()
    }

    // =========================================================================
    // EVENTS SYNC
    // =========================================================================
    suspend fun uploadEvents(collegeId: String, events: List<Map<String, Any>>) {
        if (!isSyncedEnabled() || events.isEmpty()) return
        supabaseClient.from("campus_events").upsert(events)
    }

    suspend fun downloadEvents(collegeId: String): List<Map<String, Any>> {
        if (!isSyncedEnabled()) return emptyList()
        val result = supabaseClient.from("campus_events")
            .select("*")
            .eq("college_id", collegeId)
            .execute()
        return result.data as? List<Map<String, Any>> ?: emptyList()
    }

    // =========================================================================
    // LOST & FOUND SYNC
    // =========================================================================
    suspend fun uploadLostFound(collegeId: String, items: List<Map<String, Any>>) {
        if (!isSyncedEnabled() || items.isEmpty()) return
        supabaseClient.from("lost_found_items").upsert(items)
    }

    suspend fun downloadLostFound(collegeId: String): List<Map<String, Any>> {
        if (!isSyncedEnabled()) return emptyList()
        val result = supabaseClient.from("lost_found_items")
            .select("*")
            .eq("college_id", collegeId)
            .execute()
        return result.data as? List<Map<String, Any>> ?: emptyList()
    }

    // =========================================================================
    // DIRECTORY SYNC
    // =========================================================================
    suspend fun uploadDirectory(collegeId: String, contacts: List<Map<String, Any>>) {
        if (!isSyncedEnabled() || contacts.isEmpty()) return
        supabaseClient.from("directory_contacts").upsert(contacts)
    }

    suspend fun downloadDirectory(collegeId: String): List<Map<String, Any>> {
        if (!isSyncedEnabled()) return emptyList()
        val result = supabaseClient.from("directory_contacts")
            .select("*")
            .eq("college_id", collegeId)
            .execute()
        return result.data as? List<Map<String, Any>> ?: emptyList()
    }

    // =========================================================================
    // MODERATORS SYNC
    // =========================================================================
    suspend fun uploadModerators(collegeId: String, moderators: List<Map<String, Any>>) {
        if (!isSyncedEnabled() || moderators.isEmpty()) return
        supabaseClient.from("moderators").upsert(moderators)
    }

    suspend fun downloadModerators(collegeId: String): List<Map<String, Any>> {
        if (!isSyncedEnabled()) return emptyList()
        val result = supabaseClient.from("moderators")
            .select("*")
            .eq("college_id", collegeId)
            .execute()
        return result.data as? List<Map<String, Any>> ?: emptyList()
    }

    // =========================================================================
    // COMMUNITY SYNC
    // =========================================================================
    suspend fun downloadCommunityPosts(
        collegeId: String,
        category: String? = null,
        beforeCreatedAt: Long? = null,
        limit: Int = 20
    ): List<Map<String, Any>> {
        if (!isSyncedEnabled()) return emptyList()
        var query = supabaseClient.from("community_posts")
            .select("*")
            .eq("college_id", collegeId)
        if (category != null) query = query.eq("category", category)
        if (beforeCreatedAt != null) query = query.lt("created_at", beforeCreatedAt)
        val result = query
            .order("created_at", ascending = false)
            .limit(limit)
            .execute()
        result.error?.let { throw IllegalStateException(it) }
        return result.data as? List<Map<String, Any>> ?: emptyList()
    }

    suspend fun downloadCommunityPost(postId: String): Map<String, Any>? {
        if (!isSyncedEnabled()) return null
        val result = supabaseClient.from("community_posts")
            .select("*")
            .eq("id", postId)
            .limit(1)
            .execute()
        return (result.data as? List<Map<String, Any>>)?.firstOrNull()
    }

    suspend fun downloadCommunityComments(postId: String, limit: Int = 100): List<Map<String, Any>> {
        if (!isSyncedEnabled()) return emptyList()
        val result = supabaseClient.from("community_comments")
            .select("*")
            .eq("post_id", postId)
            .order("created_at", ascending = true)
            .limit(limit)
            .execute()
        result.error?.let { throw IllegalStateException(it) }
        return result.data as? List<Map<String, Any>> ?: emptyList()
    }

    suspend fun downloadCommunityComment(commentId: String): Map<String, Any>? {
        if (!isSyncedEnabled()) return null
        val result = supabaseClient.from("community_comments")
            .select("*")
            .eq("id", commentId)
            .limit(1)
            .execute()
        return (result.data as? List<Map<String, Any>>)?.firstOrNull()
    }

    suspend fun downloadMyCommunityLikes(uid: String, postIds: List<String>): Set<String> {
        if (!isSyncedEnabled() || postIds.isEmpty()) return emptySet()
        val result = supabaseClient.from("community_likes")
            .select("post_id")
            .eq("user_id", uid)
            .inArray("post_id", postIds)
            .execute()
        val rows = result.data as? List<Map<String, Any>> ?: return emptySet()
        return rows.mapNotNull { it["post_id"] as? String }.toSet()
    }

    suspend fun uploadCommunityPost(post: Map<String, Any>): Boolean {
        if (!isSyncedEnabled()) return false
        val result = supabaseClient.from("community_posts").upsert(post)
        return result.isSuccess
    }

    suspend fun updateCommunityPost(
        postId: String,
        title: String,
        body: String,
        updatedAtMillis: Long
    ): Boolean {
        if (!isSyncedEnabled()) return false
        val result = supabaseClient.from("community_posts")
            .eq("id", postId)
            .update(mapOf("title" to title, "body" to body, "updated_at" to updatedAtMillis))
        return result.isSuccess
    }

    suspend fun deleteCommunityPost(postId: String): Boolean {
        if (!isSyncedEnabled()) return false
        val result = supabaseClient.from("community_posts").eq("id", postId).delete()
        return result.isSuccess
    }

    suspend fun uploadCommunityComment(comment: Map<String, Any>): Boolean {
        if (!isSyncedEnabled()) return false
        val result = supabaseClient.from("community_comments").upsert(comment)
        return result.isSuccess
    }

    suspend fun deleteCommunityComment(commentId: String): Boolean {
        if (!isSyncedEnabled()) return false
        val result = supabaseClient.from("community_comments").eq("id", commentId).delete()
        return result.isSuccess
    }

    suspend fun setCommunityLike(postId: String, uid: String, liked: Boolean): Boolean {
        if (!isSyncedEnabled()) return false
        return if (liked) {
            supabaseClient.from("community_likes").insert(mapOf(
                "post_id" to postId,
                "user_id" to uid,
                "created_at" to System.currentTimeMillis()
            )).isSuccess
        } else {
            supabaseClient.from("community_likes")
                .eq("post_id", postId)
                .eq("user_id", uid)
                .delete()
                .isSuccess
        }
    }

    suspend fun adminModerateCommunity(targetType: String, targetId: String, action: String): Boolean =
        callAdmin(
            "admin_moderate_community",
            mapOf("p_target_type" to targetType, "p_target_id" to targetId, "p_action" to action)
        )

    // =========================================================================
    // NOTIFICATIONS SYNC
    // =========================================================================
    suspend fun downloadNotifications(uid: String, limit: Int = 100): List<Map<String, Any>> {
        if (!isSyncedEnabled()) return emptyList()
        val result = supabaseClient.from("notifications")
            .select("*")
            .eq("user_id", uid)
            .order("created_at", ascending = false)
            .limit(limit)
            .execute()
        result.error?.let { throw IllegalStateException(it) }
        return result.data as? List<Map<String, Any>> ?: emptyList()
    }

    suspend fun markNotificationRead(notificationId: String): Boolean {
        if (!isSyncedEnabled()) return false
        val result = supabaseClient.from("notifications")
            .eq("id", notificationId)
            .update(mapOf("is_read" to true))
        return result.isSuccess
    }

    suspend fun markAllNotificationsRead(uid: String): Boolean {
        if (!isSyncedEnabled()) return false
        val result = supabaseClient.from("notifications")
            .eq("user_id", uid)
            .update(mapOf("is_read" to true))
        return result.isSuccess
    }

    /**
     * Sends an authenticated admin broadcast through the server-side FCM gateway.
     * The Firebase service-account key never enters the Android application.
     */
    suspend fun sendPushNotification(
        title: String,
        body: String,
        category: String,
        targetType: String,
        targetId: String
    ): Boolean {
        val edgeUrl = authManager.config.edgeFunctionUrl
        val sessionToken = authManager.currentSession()?.accessToken ?: return false
        if (edgeUrl.isBlank()) return false
        return try {
            val connection = (URL("$edgeUrl/send-notification").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", authManager.config.anonKey)
                setRequestProperty("Authorization", "Bearer $sessionToken")
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 15000
                readTimeout = 30000
                doOutput = true
            }
            val payload = org.json.JSONObject()
                .put("title", title)
                .put("body", body)
                .put("category", category)
                .put("target_type", targetType)
                .put("target_id", targetId)
            connection.outputStream.use {
                it.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.use { it.readBytes().toString(StandardCharsets.UTF_8) }.orEmpty()
            code in 200..299 && runCatching {
                val json = org.json.JSONObject(response)
                json.optBoolean("ok", false) && json.optInt("sent", 0) > 0
            }.getOrDefault(false)
        } catch (error: Exception) {
            Log.w(TAG, "FCM broadcast failed: " + error.message)
            false
        }
    }

    /**
     * Registers an FCM device token with Supabase via Edge Function.
     * The Edge Function validates the caller's JWT and upserts the token
     * into the device_tokens table with RLS enforcement.
     */
    suspend fun registerDeviceToken(
        uid: String,
        fcmToken: String,
        devicePlatform: String,
        deviceName: String,
        email: String
    ): Boolean {
        val edgeUrl = authManager.config.edgeFunctionUrl
        if (edgeUrl.isBlank()) return false

        val session = authManager.currentSession()
        val token = session?.accessToken
            ?: return false
        val anonKey = authManager.config.anonKey

        return try {
            val url = URL("$edgeUrl/register-device-token")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", anonKey)
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.doOutput = true

            val body = org.json.JSONObject()
                .put("uid", uid)
                .put("fcm_token", fcmToken)
                .put("device_platform", devicePlatform)
                .put("device_name", deviceName)
                .put("email", email)
                .toString()

            conn.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }

            val responseCode = conn.responseCode
            val responseStream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val responseBody = responseStream?.use { it.readBytes() } ?: ByteArray(0)
            val response = String(responseBody, StandardCharsets.UTF_8)

            responseCode in 200..299 && runCatching {
                org.json.JSONObject(response).optBoolean("ok", false)
            }.getOrDefault(false)
        } catch (e: Exception) {
            Log.w(TAG, "registerDeviceToken failed: ${e.message}")
            false
        }
    }

    /**
     * Deactivates an FCM device token (called on sign-out).
     * Sets is_active = false so the token is no longer used for notifications.
     */
    suspend fun deactivateDeviceToken(fcmToken: String): Boolean {
        if (!isSyncedEnabled()) return false
        val result = supabaseClient.from("device_tokens")
            .eq("fcm_token", fcmToken)
            .update(mapOf("is_active" to false, "updated_at" to System.currentTimeMillis()))
        return result.isSuccess
    }

    /**
     * Deletes an FCM device token record.
     */
    suspend fun deleteDeviceToken(fcmToken: String): Boolean {
        if (!isSyncedEnabled()) return false
        val result = supabaseClient.from("device_tokens")
            .eq("fcm_token", fcmToken)
            .delete()
        return result.isSuccess
    }

    /**
     * Gets all active device tokens for a user (admin use).
     */
    suspend fun getDeviceTokensForUser(uid: String): List<String> {
        if (!isSyncedEnabled()) return emptyList()
        val result = supabaseClient.from("device_tokens")
            .select("fcm_token")
            .eq("user_id", uid)
            .eq("is_active", true)
            .execute()
        return (result.data as? List<Map<String, Any>>)
            ?.mapNotNull { it["fcm_token"] as? String }
            ?: emptyList()
    }
}

data class TimetablePublishOutcome(
    val success: Boolean,
    val message: String,
    val rowCount: Int = 0
)
