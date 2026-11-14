package `in`.campusone.app.data.remote

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
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
        isModeratorApproved: Boolean = false
    ) {
        if (!isSyncedEnabled()) return
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
            "updated_at" to System.currentTimeMillis()
        ))
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
        val response = supabaseClient.rpc(
            "campusone_bootstrap_profile",
            mapOf("p_name" to name, "p_email" to email)
        )
        return response.isSuccess && (response.data as? Map<String, Any>)?.get("ok") == true
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

    suspend fun downloadRoleRequests(): List<Map<String, Any>> {
        if (!isSyncedEnabled()) return emptyList()
        val result = supabaseClient.from("role_requests")
            .select("*")
            .order("created_at", ascending = false)
            .limit(ADMIN_PAGE_LIMIT)
            .execute()
        return result.data as? List<Map<String, Any>> ?: emptyList()
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
    suspend fun uploadNotices(sectionKey: String, notices: List<Map<String, Any>>) {
        if (!isSyncedEnabled() || notices.isEmpty()) return
        supabaseClient.from("notices").upsert(notices)
    }

    suspend fun downloadNotices(sectionKey: String): List<Map<String, Any>> {
        if (!isSyncedEnabled()) return emptyList()
        val result = supabaseClient.from("notices")
            .select("*")
            .eq("section_key", sectionKey)
            .order("created_at", ascending = false)
            .execute()
        return result.data as? List<Map<String, Any>> ?: emptyList()
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
    suspend fun uploadTimetable(sectionKey: String, slots: List<Map<String, Any>>) {
        if (!isSyncedEnabled() || slots.isEmpty()) return
        supabaseClient.from("timetable_slots").upsert(slots)
    }

    suspend fun downloadTimetable(sectionKey: String): List<Map<String, Any>> {
        if (!isSyncedEnabled()) return emptyList()
        val result = supabaseClient.from("timetable_slots")
            .select("*")
            .eq("section_key", sectionKey)
            .order("day_of_week", ascending = true)
            .order("start_time", ascending = true)
            .execute()
        return result.data as? List<Map<String, Any>> ?: emptyList()
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
}
