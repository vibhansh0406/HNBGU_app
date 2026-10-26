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

    private object Keys {
        val IS_ONBOARDED = booleanPreferencesKey("is_onboarded")
        val USER_PROFILE_JSON = stringPreferencesKey("user_profile_json")
        val LANGUAGE_TAG = stringPreferencesKey("language_tag") // "en" or "hi"
        val DARK_MODE_OVERRIDE = stringPreferencesKey("dark_mode_override") // "SYSTEM", "LIGHT", "DARK"
        val LAST_TIMETABLE_SYNC = longPreferencesKey("last_timetable_sync")
        val LAST_NOTICE_SYNC = longPreferencesKey("last_notice_sync")
        val TARGET_ATTENDANCE_PCT = intPreferencesKey("target_attendance_pct")
        val REMINDER_LEAD_MINS = intPreferencesKey("reminder_lead_mins")
    }

    val isOnboarded: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_ONBOARDED] ?: false }

    val userProfile: Flow<UserProfile> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.USER_PROFILE_JSON]
        if (raw.isNullOrBlank()) {
            UserProfile()
        } else {
            runCatching { json.decodeFromString<UserProfile>(raw) }.getOrDefault(UserProfile())
        }
    }

    val languageTag: Flow<String> = context.dataStore.data.map { it[Keys.LANGUAGE_TAG] ?: "en" }

    suspend fun saveUserProfile(profile: UserProfile, markOnboarded: Boolean = true) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_PROFILE_JSON] = json.encodeToString(profile)
            prefs[Keys.TARGET_ATTENDANCE_PCT] = profile.targetAttendancePct
            prefs[Keys.REMINDER_LEAD_MINS] = profile.reminderLeadMinutes
            if (markOnboarded) {
                prefs[Keys.IS_ONBOARDED] = true
            }
        }
    }

    suspend fun updateTargetAttendance(targetPct: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.USER_PROFILE_JSON]?.let {
                runCatching { json.decodeFromString<UserProfile>(it) }.getOrNull()
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
