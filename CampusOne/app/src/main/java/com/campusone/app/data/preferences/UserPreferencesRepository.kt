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

    suspend fun getProfileOnce(): UserProfile = userProfileFlow.first()

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
