package com.campusone.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.campusone.app.domain.model.GradingScale
import com.campusone.app.domain.model.LoginIntentPath
import com.campusone.app.domain.model.RemoteProfileDto
import com.campusone.app.domain.model.SupabaseAuthSession
import com.campusone.app.domain.model.UserProfile
import com.campusone.app.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalDate

private val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "campusone_user_prefs"
)

data class PersistedCloudAuthState(
    val session: SupabaseAuthSession? = null,
    val remoteProfile: RemoteProfileDto? = null,
    val loginIntentPath: LoginIntentPath = LoginIntentPath.STUDENT,
    val overrideSupabaseUrl: String = "",
    val overrideSupabaseKey: String = "",
    val overrideGoogleClientId: String = "",
    val fcmDeviceToken: String = ""
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val NAME = stringPreferencesKey("student_name")
        val COLLEGE = stringPreferencesKey("college_name")
        val COURSE = stringPreferencesKey("course_name")
        val SEMESTER = intPreferencesKey("semester_num")
        val SECTION = stringPreferencesKey("section_name")
        val TARGET_ATTENDANCE = intPreferencesKey("target_attendance_pct")
        val LOW_ATT_ALERT_ENABLED = booleanPreferencesKey("low_att_alert_enabled")
        val LOW_ATT_ALERT_THRESHOLD = intPreferencesKey("low_att_alert_threshold")
        val SEMESTER_END_EPOCH_DAY = longPreferencesKey("semester_end_epoch_day")
        val REMINDER_LEAD_MINS = intPreferencesKey("reminder_lead_mins")
        val GRADING_SCALE = stringPreferencesKey("grading_scale")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val SETUP_COMPLETED = booleanPreferencesKey("setup_completed")

        // Supabase Auth & Cloud session keys
        val ACCESS_TOKEN = stringPreferencesKey("sb_access_token")
        val REFRESH_TOKEN = stringPreferencesKey("sb_refresh_token")
        val EXPIRES_AT_SEC = longPreferencesKey("sb_expires_at_sec")
        val AUTH_USER_ID = stringPreferencesKey("sb_user_id")
        val AUTH_EMAIL = stringPreferencesKey("sb_email")
        val REMOTE_ROLE = stringPreferencesKey("sb_remote_role")
        val REMOTE_DEPT_ID = stringPreferencesKey("sb_remote_dept_id")
        val REMOTE_STATUS = stringPreferencesKey("sb_remote_status")
        val LOGIN_PATH = stringPreferencesKey("sb_login_intent_path")

        val OVERRIDE_SB_URL = stringPreferencesKey("override_sb_url")
        val OVERRIDE_SB_KEY = stringPreferencesKey("override_sb_key")
        val OVERRIDE_GOOGLE_ID = stringPreferencesKey("override_google_id")
        val FCM_DEVICE_TOKEN = stringPreferencesKey("fcm_device_token")
    }

    val userProfileFlow: Flow<UserProfile> = context.userPrefsDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            val scaleName = prefs[Keys.GRADING_SCALE] ?: GradingScale.SCALE_10.name
            val scale = runCatching { GradingScale.valueOf(scaleName) }.getOrDefault(GradingScale.SCALE_10)
            val defaultEndEpoch = LocalDate.now().plusDays(60).toEpochDay()
            val targetPct = prefs[Keys.TARGET_ATTENDANCE] ?: 75
            UserProfile(
                name = prefs[Keys.NAME] ?: "",
                college = prefs[Keys.COLLEGE] ?: "HNB Garhwal University",
                course = prefs[Keys.COURSE] ?: "B.Tech CSE",
                semester = prefs[Keys.SEMESTER] ?: 4,
                section = prefs[Keys.SECTION] ?: "A",
                role = UserRole.STUDENT,
                targetAttendancePercent = targetPct,
                lowAttendanceAlertEnabled = prefs[Keys.LOW_ATT_ALERT_ENABLED] ?: true,
                lowAttendanceAlertThreshold = prefs[Keys.LOW_ATT_ALERT_THRESHOLD] ?: targetPct,
                semesterEndEpochDay = prefs[Keys.SEMESTER_END_EPOCH_DAY] ?: defaultEndEpoch,
                reminderLeadMinutes = prefs[Keys.REMINDER_LEAD_MINS] ?: 15,
                gradingScale = scale,
                languageCode = prefs[Keys.LANGUAGE_CODE] ?: "en",
                setupCompleted = prefs[Keys.SETUP_COMPLETED] ?: false
            )
        }

    val cloudAuthStateFlow: Flow<PersistedCloudAuthState> = context.userPrefsDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            val token = prefs[Keys.ACCESS_TOKEN].orEmpty()
            val userId = prefs[Keys.AUTH_USER_ID].orEmpty()
            val email = prefs[Keys.AUTH_EMAIL].orEmpty()
            val session = if (token.isNotBlank() && userId.isNotBlank()) {
                SupabaseAuthSession(
                    accessToken = token,
                    refreshToken = prefs[Keys.REFRESH_TOKEN].orEmpty(),
                    expiresAtEpochSec = prefs[Keys.EXPIRES_AT_SEC] ?: 0L,
                    userId = userId,
                    email = email
                )
            } else {
                null
            }
            val remoteProfile = if (userId.isNotBlank()) {
                RemoteProfileDto(
                    id = userId,
                    fullName = prefs[Keys.NAME].orEmpty(),
                    email = email,
                    role = prefs[Keys.REMOTE_ROLE] ?: "student",
                    departmentId = prefs[Keys.REMOTE_DEPT_ID]?.takeIf { it.isNotBlank() },
                    accountStatus = prefs[Keys.REMOTE_STATUS] ?: "active"
                )
            } else {
                null
            }
            val pathName = prefs[Keys.LOGIN_PATH] ?: LoginIntentPath.STUDENT.name
            val path = runCatching { LoginIntentPath.valueOf(pathName) }
                .getOrDefault(LoginIntentPath.STUDENT)

            PersistedCloudAuthState(
                session = session,
                remoteProfile = remoteProfile,
                loginIntentPath = path,
                overrideSupabaseUrl = prefs[Keys.OVERRIDE_SB_URL].orEmpty(),
                overrideSupabaseKey = prefs[Keys.OVERRIDE_SB_KEY].orEmpty(),
                overrideGoogleClientId = prefs[Keys.OVERRIDE_GOOGLE_ID].orEmpty(),
                fcmDeviceToken = prefs[Keys.FCM_DEVICE_TOKEN].orEmpty()
            )
        }

    suspend fun getProfileOnce(): UserProfile = userProfileFlow.first()

    suspend fun getCloudAuthOnce(): PersistedCloudAuthState = cloudAuthStateFlow.first()

    suspend fun saveSupabaseSessionAndProfile(
        session: SupabaseAuthSession,
        profile: RemoteProfileDto,
        intentPath: LoginIntentPath
    ) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = session.accessToken
            prefs[Keys.REFRESH_TOKEN] = session.refreshToken
            prefs[Keys.EXPIRES_AT_SEC] = session.expiresAtEpochSec
            prefs[Keys.AUTH_USER_ID] = session.userId
            prefs[Keys.AUTH_EMAIL] = profile.email.ifBlank { session.email }
            prefs[Keys.REMOTE_ROLE] = profile.role
            prefs[Keys.REMOTE_DEPT_ID] = profile.departmentId.orEmpty()
            prefs[Keys.REMOTE_STATUS] = profile.accountStatus
            prefs[Keys.LOGIN_PATH] = intentPath.name
            if (profile.fullName.isNotBlank()) {
                prefs[Keys.NAME] = profile.fullName
            }
            prefs[Keys.SETUP_COMPLETED] = true
        }
    }

    suspend fun updateLoginIntentPath(intentPath: LoginIntentPath) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[Keys.LOGIN_PATH] = intentPath.name
        }
    }

    suspend fun clearSupabaseSession() {
        context.userPrefsDataStore.edit { prefs ->
            prefs.remove(Keys.ACCESS_TOKEN)
            prefs.remove(Keys.REFRESH_TOKEN)
            prefs.remove(Keys.EXPIRES_AT_SEC)
            prefs.remove(Keys.AUTH_USER_ID)
            prefs.remove(Keys.AUTH_EMAIL)
            prefs.remove(Keys.REMOTE_ROLE)
            prefs.remove(Keys.REMOTE_DEPT_ID)
            prefs.remove(Keys.REMOTE_STATUS)
            prefs[Keys.LOGIN_PATH] = LoginIntentPath.STUDENT.name
        }
    }

    suspend fun saveRuntimeConfigOverrides(
        supabaseUrl: String,
        supabasePublishableKey: String,
        googleWebClientId: String
    ) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[Keys.OVERRIDE_SB_URL] = supabaseUrl.trim()
            prefs[Keys.OVERRIDE_SB_KEY] = supabasePublishableKey.trim()
            prefs[Keys.OVERRIDE_GOOGLE_ID] = googleWebClientId.trim()
        }
    }

    suspend fun saveFcmDeviceToken(token: String) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[Keys.FCM_DEVICE_TOKEN] = token.trim()
        }
    }

    suspend fun saveSetupProfile(
        name: String,
        college: String,
        course: String,
        semester: Int,
        section: String,
        targetAttendancePercent: Int
    ) {
        val clampedTarget = targetAttendancePercent.coerceIn(50, 95)
        context.userPrefsDataStore.edit { prefs ->
            prefs[Keys.NAME] = name.trim().ifEmpty { "Student" }
            prefs[Keys.COLLEGE] = college.trim().ifEmpty { "HNB Garhwal University" }
            prefs[Keys.COURSE] = course.trim().ifEmpty { "B.Tech CSE" }
            prefs[Keys.SEMESTER] = semester.coerceIn(1, 10)
            prefs[Keys.SECTION] = section.trim().ifEmpty { "A" }
            prefs[Keys.TARGET_ATTENDANCE] = clampedTarget
            prefs[Keys.LOW_ATT_ALERT_THRESHOLD] = clampedTarget
            if (prefs[Keys.SEMESTER_END_EPOCH_DAY] == null) {
                prefs[Keys.SEMESTER_END_EPOCH_DAY] = LocalDate.now().plusDays(60).toEpochDay()
            }
            prefs[Keys.SETUP_COMPLETED] = true
        }
    }

    suspend fun restoreProfile(profile: UserProfile) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[Keys.NAME] = profile.name
            prefs[Keys.COLLEGE] = profile.college
            prefs[Keys.COURSE] = profile.course
            prefs[Keys.SEMESTER] = profile.semester
            prefs[Keys.SECTION] = profile.section
            prefs[Keys.TARGET_ATTENDANCE] = profile.targetAttendancePercent
            prefs[Keys.LOW_ATT_ALERT_ENABLED] = profile.lowAttendanceAlertEnabled
            prefs[Keys.LOW_ATT_ALERT_THRESHOLD] = profile.lowAttendanceAlertThreshold
            if (profile.semesterEndEpochDay > 0L) {
                prefs[Keys.SEMESTER_END_EPOCH_DAY] = profile.semesterEndEpochDay
            }
            prefs[Keys.REMINDER_LEAD_MINS] = profile.reminderLeadMinutes
            prefs[Keys.GRADING_SCALE] = profile.gradingScale.name
            prefs[Keys.LANGUAGE_CODE] = profile.languageCode
            prefs[Keys.SETUP_COMPLETED] = true
        }
    }

    suspend fun updateTargetAttendance(targetPct: Int) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[Keys.TARGET_ATTENDANCE] = targetPct.coerceIn(50, 95)
        }
    }

    suspend fun updateLowAttendanceAlertSettings(enabled: Boolean, thresholdPct: Int) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[Keys.LOW_ATT_ALERT_ENABLED] = enabled
            prefs[Keys.LOW_ATT_ALERT_THRESHOLD] = thresholdPct.coerceIn(50, 95)
        }
    }

    suspend fun updateSemesterEndEpochDay(epochDay: Long) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[Keys.SEMESTER_END_EPOCH_DAY] = epochDay
        }
    }

    suspend fun updateReminderLeadMinutes(leadMins: Int) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[Keys.REMINDER_LEAD_MINS] = leadMins.coerceIn(5, 120)
        }
    }

    suspend fun updateGradingScale(scale: GradingScale) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[Keys.GRADING_SCALE] = scale.name
        }
    }

    suspend fun updateLanguage(languageCode: String) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[Keys.LANGUAGE_CODE] = languageCode
        }
    }
}
