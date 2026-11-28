package `in`.campusone.app.data.preferences

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.campusone.app.domain.model.UserProfile
import `in`.campusone.app.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "campusone_prefs")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // Normalize roles from older app versions before deserializing the saved profile.
    // Teacher and Club Admin are retired; their legacy profile records map to Moderator.
    private fun decodeUserProfile(raw: String): UserProfile {
        val normalized = Regex("\"role\"\\s*:\\s*\"(?:TEACHER|CLUB_ADMIN)\"")
            .replace(raw, "\"role\":\"MODERATOR\"")
        return runCatching { json.decodeFromString<UserProfile>(normalized) }
            .getOrDefault(UserProfile())
    }

    private object Keys {
        val IS_ONBOARDED = booleanPreferencesKey("is_onboarded")
        val INTRO_SEEN = booleanPreferencesKey("intro_seen")
        val USER_PROFILE_JSON = stringPreferencesKey("user_profile_json")
        val LANGUAGE_TAG = stringPreferencesKey("language_tag") // "en" or "hi"
        val THEME_MODE = stringPreferencesKey("theme_mode") // "system", "light" or "dark"
        val LAST_TIMETABLE_SYNC = longPreferencesKey("last_timetable_sync")
        val LAST_NOTICE_SYNC = longPreferencesKey("last_notice_sync")
        val TARGET_ATTENDANCE_PCT = intPreferencesKey("target_attendance_pct")
        val REMINDER_LEAD_MINS = intPreferencesKey("reminder_lead_mins")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val ANNOUNCEMENTS_ENABLED = booleanPreferencesKey("announcements_enabled")
        val NOTICES_ENABLED = booleanPreferencesKey("notices_enabled")
        val EXAM_UPDATES_ENABLED = booleanPreferencesKey("exam_updates_enabled")
        val ASSIGNMENT_UPDATES_ENABLED = booleanPreferencesKey("assignment_updates_enabled")
        val STUDY_MATERIAL_UPDATES_ENABLED = booleanPreferencesKey("study_material_updates_enabled")
        val CLASS_REMINDERS_ENABLED = booleanPreferencesKey("class_reminders_enabled")
        val ASSIGNMENT_REMINDERS_ENABLED = booleanPreferencesKey("assignment_reminders_enabled")
        val EXAM_REMINDERS_ENABLED = booleanPreferencesKey("exam_reminders_enabled")
        val EVENT_REMINDERS_ENABLED = booleanPreferencesKey("event_reminders_enabled")
    }

    val isOnboarded: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_ONBOARDED] ?: false }

    val hasSeenIntro: Flow<Boolean> = context.dataStore.data.map { it[Keys.INTRO_SEEN] ?: false }

    suspend fun markIntroSeen() {
        context.dataStore.edit { prefs -> prefs[Keys.INTRO_SEEN] = true }
    }

    val userProfile: Flow<UserProfile> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.USER_PROFILE_JSON]
        if (raw.isNullOrBlank()) {
            UserProfile()
        } else {
            decodeUserProfile(raw)
        }
    }

    val languageTag: Flow<String> = context.dataStore.data.map { it[Keys.LANGUAGE_TAG] ?: "en" }
    val themeMode: Flow<String> = context.dataStore.data.map { it[Keys.THEME_MODE] ?: "system" }
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.NOTIFICATIONS_ENABLED] ?: true
    }
    val announcementsEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.ANNOUNCEMENTS_ENABLED] ?: true }
    val noticesEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.NOTICES_ENABLED] ?: true }
    val examUpdatesEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.EXAM_UPDATES_ENABLED] ?: true }
    val assignmentUpdatesEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.ASSIGNMENT_UPDATES_ENABLED] ?: true }
    val studyMaterialUpdatesEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.STUDY_MATERIAL_UPDATES_ENABLED] ?: true }
    val classRemindersEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.CLASS_REMINDERS_ENABLED] ?: true }
    val assignmentRemindersEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.ASSIGNMENT_REMINDERS_ENABLED] ?: true }
    val examRemindersEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.EXAM_REMINDERS_ENABLED] ?: true }
    val eventRemindersEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.EVENT_REMINDERS_ENABLED] ?: true }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setNotificationCategoryEnabled(category: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            when (category) {
                "announcement" -> prefs[Keys.ANNOUNCEMENTS_ENABLED] = enabled
                "notice" -> prefs[Keys.NOTICES_ENABLED] = enabled
                "exam" -> prefs[Keys.EXAM_UPDATES_ENABLED] = enabled
                "assignment" -> prefs[Keys.ASSIGNMENT_UPDATES_ENABLED] = enabled
                "study_material" -> prefs[Keys.STUDY_MATERIAL_UPDATES_ENABLED] = enabled
            }
        }
    }

    suspend fun isNotificationCategoryEnabled(category: String): Boolean {
        val settings = context.dataStore.data.map { prefs ->
            (prefs[Keys.NOTIFICATIONS_ENABLED] ?: true) && when (category) {
                "announcement" -> prefs[Keys.ANNOUNCEMENTS_ENABLED] ?: true
                "notice" -> prefs[Keys.NOTICES_ENABLED] ?: true
                "exam" -> prefs[Keys.EXAM_UPDATES_ENABLED] ?: true
                "assignment" -> prefs[Keys.ASSIGNMENT_UPDATES_ENABLED] ?: true
                "study_material" -> prefs[Keys.STUDY_MATERIAL_UPDATES_ENABLED] ?: true
                else -> true
            }
        }
        return settings.first()
    }

    suspend fun setLocalReminderEnabled(category: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            when (category) {
                "class" -> prefs[Keys.CLASS_REMINDERS_ENABLED] = enabled
                "assignment" -> prefs[Keys.ASSIGNMENT_REMINDERS_ENABLED] = enabled
                "exam" -> prefs[Keys.EXAM_REMINDERS_ENABLED] = enabled
                "event" -> prefs[Keys.EVENT_REMINDERS_ENABLED] = enabled
            }
        }
    }

    suspend fun saveUserProfile(profile: UserProfile, markOnboarded: Boolean = true) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_PROFILE_JSON] = json.encodeToString(profile)
            prefs[Keys.TARGET_ATTENDANCE_PCT] = profile.targetAttendancePct
            prefs[Keys.REMINDER_LEAD_MINS] = profile.reminderLeadMinutes
            // The server's onboarding_completed flag is the source of truth.
            // markOnboarded=true (e.g. after local onboarding completion) also sets it.
            prefs[Keys.IS_ONBOARDED] = profile.onboardingCompleted || markOnboarded
        }
    }

    suspend fun updateTargetAttendance(targetPct: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.USER_PROFILE_JSON]?.let {
                runCatching { decodeUserProfile(it) }.getOrNull()
            } ?: UserProfile()
            val updated = current.copy(targetAttendancePct = targetPct.coerceIn(50, 100))
            prefs[Keys.USER_PROFILE_JSON] = json.encodeToString(updated)
            prefs[Keys.TARGET_ATTENDANCE_PCT] = updated.targetAttendancePct
        }
    }

    suspend fun updateReminderLeadTime(minutes: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.USER_PROFILE_JSON]?.let {
                runCatching { json.decodeFromString<UserProfile>(it) }.getOrNull()
            } ?: UserProfile()
            val updated = current.copy(reminderLeadMinutes = minutes.coerceIn(5, 60))
            prefs[Keys.USER_PROFILE_JSON] = json.encodeToString(updated)
            prefs[Keys.REMINDER_LEAD_MINS] = updated.reminderLeadMinutes
        }
    }

    suspend fun switchLanguage(langTag: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE_TAG] = langTag
        }
        val appLocale = LocaleListCompat.forLanguageTags(langTag)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[Keys.THEME_MODE] = mode }
    }

    suspend fun getLastSyncTimestamps(): Pair<Long, Long> {
        var ttSync = 0L
        var noticeSync = 0L
        context.dataStore.edit { prefs ->
            ttSync = prefs[Keys.LAST_TIMETABLE_SYNC] ?: 0L
            noticeSync = prefs[Keys.LAST_NOTICE_SYNC] ?: 0L
        }
        return ttSync to noticeSync
    }

    suspend fun updateSyncTimestamps(timetableSync: Long? = null, noticeSync: Long? = null) {
        context.dataStore.edit { prefs ->
            timetableSync?.let { prefs[Keys.LAST_TIMETABLE_SYNC] = it }
            noticeSync?.let { prefs[Keys.LAST_NOTICE_SYNC] = it }
        }
    }
}
