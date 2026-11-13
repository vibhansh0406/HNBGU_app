package `in`.campusone.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.campusone.app.data.preferences.UserPreferencesRepository
import `in`.campusone.app.domain.model.UserProfile
import `in`.campusone.app.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModeratorRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: UserPreferencesRepository
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val Context.moderatorStore by preferencesDataStore(name = "campusone_moderators")
    private object ModKeys {
        val MODERATOR_LIST_JSON = stringPreferencesKey("moderator_list_json")
    }

    /** All registered moderator profiles (local, for admin appointment). */
    val moderators: Flow<List<UserProfile>> = context.moderatorStore.data.map {
        val raw = it[ModKeys.MODERATOR_LIST_JSON]
        if (raw.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching { json.decodeFromString<List<UserProfile>>(raw) }.getOrDefault(emptyList())
        }
    }

    /** Current logged-in profile (from main prefs). */
    val currentProfile: Flow<UserProfile> = prefs.userProfile

    /**
     * Appoint a user as moderator for a given department scope.
     * Only an Admin can call this. The moderator is NOT active until `isApproved = true`.
     */
    suspend fun appointModerator(
        profile: UserProfile,
        scope: String,
        approvedByAdmin: Boolean
    ) {
        require(profile.role == UserRole.MODERATOR) { "Only MODERATOR role can be appointed with a scope." }
        val updated = profile.copy(
            role = UserRole.MODERATOR,
            moderatorScope = scope.trim(),
            isModeratorApproved = approvedByAdmin,
            verifiedRole = approvedByAdmin
        )
        saveModeratorProfile(updated)
    }

    /** Revoke moderator status (back to STUDENT). */
    suspend fun revokeModerator(uid: String) {
        val list = moderators.first()
        val target = list.find { it.uid == uid } ?: return
        val updated = target.copy(
            role = UserRole.STUDENT,
            moderatorScope = "",
            isModeratorApproved = false,
            verifiedRole = false
        )
        saveModeratorProfile(updated)
    }

    /** Approve or un-approve a moderator appointment. */
    suspend fun setModeratorApproval(uid: String, approved: Boolean) {
        val list = moderators.first()
        val target = list.find { it.uid == uid } ?: return
        val updated = target.copy(
            isModeratorApproved = approved,
            verifiedRole = approved
        )
        saveModeratorProfile(updated)
    }

    /** Update a moderator's scope. */
    suspend fun updateModeratorScope(uid: String, newScope: String) {
        val list = moderators.first()
        val target = list.find { it.uid == uid } ?: return
        val updated = target.copy(moderatorScope = newScope.trim())
        saveModeratorProfile(updated)
    }

    private suspend fun saveModeratorProfile(profile: UserProfile) {
        val list = moderators.first().toMutableList()
        list.removeAll { it.uid == profile.uid }
        list.add(profile)
        context.moderatorStore.edit { prefs ->
            prefs[ModKeys.MODERATOR_LIST_JSON] = json.encodeToString(list)
        }
    }

    /** Seed demo moderators so the admin panel is usable on first launch. */
    suspend fun seedDemoModerators() {
        val list = moderators.first()
        if (list.isNotEmpty()) return
        val demos = listOf(
            UserProfile(
                uid = "mod_lifesci_001",
                name = "Dr. Anita Rawat",
                email = "anita.rawat@hnbgu.ac.in",
                role = UserRole.MODERATOR,
                moderatorScope = "BSc Life Science",
                isModeratorApproved = true,
                verifiedRole = true,
                collegeId = "hnbgu",
                courseId = "bsc_lifesci",
                courseName = "B.Sc. Life Science (NEP)",
                semester = 3,
                section = "A"
            ),
            UserProfile(
                uid = "mod_btechcs_001",
                name = "Prof. Vikram Singh",
                email = "vikram.singh@hnbgu.ac.in",
                role = UserRole.MODERATOR,
                moderatorScope = "BTech Computer Science",
                isModeratorApproved = false,
                verifiedRole = false,
                collegeId = "hnbgu",
                courseId = "btech_cse",
                courseName = "B.Tech Computer Science & Engg.",
                semester = 3,
                section = "A"
            ),
            UserProfile(
                uid = "mod_ba_001",
                name = "Dr. Meera Negi",
                email = "meera.negi@hnbgu.ac.in",
                role = UserRole.MODERATOR,
                moderatorScope = "B.A. (Hons) Arts",
                isModeratorApproved = true,
                verifiedRole = true,
                collegeId = "hnbgu",
                courseId = "ba_hons",
                courseName = "B.A. (Hons) Arts & Humanities",
                semester = 3,
                section = "A"
            )
        )
        context.moderatorStore.edit { prefs ->
            prefs[ModKeys.MODERATOR_LIST_JSON] = json.encodeToString(demos)
        }
    }
}
