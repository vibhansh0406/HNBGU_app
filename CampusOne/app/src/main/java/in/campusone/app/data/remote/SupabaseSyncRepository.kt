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
            "updated_at" to java.time.Instant.now().toString()
        ))
    }

    suspend fun downloadProfile(uid: String): Map<String, Any>? {
        if (!isSyncedEnabled()) return null
        val result = supabaseClient.from("profiles").select("*").eq("id", uid).single().execute()
        return result.data as? Map<String, Any>
    }

    // =========================================================================
    // NOTICES SYNC
    // =========================================================================
    suspend fun uploadNotices(sectionKey: String, notices: List<Map<String, Any>>) {
        if (!isSyncedEnabled() || notices.isEmpty()) return
        supabaseClient.from("notices").upsert(notices).execute()
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

    // =========================================================================
    // TIMETABLE SYNC
    // =========================================================================
    suspend fun uploadTimetable(sectionKey: String, slots: List<Map<String, Any>>) {
        if (!isSyncedEnabled() || slots.isEmpty()) return
        supabaseClient.from("timetable_slots").upsert(slots).execute()
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
        supabaseClient.from("assignments").upsert(assignments).execute()
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
        supabaseClient.from("exams").upsert(exams).execute()
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
        supabaseClient.from("study_resources").upsert(resources).execute()
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
        supabaseClient.from("clubs").upsert(clubs).execute()
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
        supabaseClient.from("campus_events").upsert(events).execute()
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
        supabaseClient.from("lost_found_items").upsert(items).execute()
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
        supabaseClient.from("directory_contacts").upsert(contacts).execute()
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
        supabaseClient.from("moderators").upsert(moderators).execute()
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
