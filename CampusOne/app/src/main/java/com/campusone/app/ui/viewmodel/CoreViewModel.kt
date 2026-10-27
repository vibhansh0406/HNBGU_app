package com.campusone.app.ui.viewmodel

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.campusone.app.config.FeatureFlags
import com.campusone.app.config.SupabaseRuntimeConfig
import com.campusone.app.data.preferences.PersistedCloudAuthState
import com.campusone.app.data.preferences.UserPreferencesRepository
import com.campusone.app.data.repository.CoreCampusRepository
import com.campusone.app.data.repository.SupabaseSyncRepository
import com.campusone.app.di.AppContainer
import com.campusone.app.domain.model.AttendanceRecordItem
import com.campusone.app.domain.model.AttendanceStatus
import com.campusone.app.domain.model.LoginIntentPath
import com.campusone.app.domain.model.PersonalNotice
import com.campusone.app.domain.model.RemoteAuditLogDto
import com.campusone.app.domain.model.RemoteProfileDto
import com.campusone.app.domain.model.SubjectSummary
import com.campusone.app.domain.model.TimetableSlot
import com.campusone.app.domain.model.UserProfile
import com.campusone.app.domain.usecase.AttendanceCalculator
import com.campusone.app.domain.usecase.TimetableEngine
import com.campusone.app.util.CampusIntentHelper
import com.campusone.app.util.GoogleCredentialHelper
import com.campusone.app.util.TimetableAndAttendanceExportHelper
import com.campusone.app.widget.CampusWidgetUpdater
import com.campusone.app.worker.ClassReminderWorker
import com.campusone.app.worker.LowAttendanceAlertWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class CoreUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile = UserProfile(),
    val subjects: List<SubjectSummary> = emptyList(),
    val overallPercentage: Double = 100.0,
    val recentHistory: List<AttendanceRecordItem> = emptyList(),
    val timetableSlots: List<TimetableSlot> = emptyList(),
    val nextClassToday: TimetableSlot? = null,
    val personalNotices: List<PersonalNotice> = emptyList(),
    val lastUndoRecordId: String? = null,
    val lastUndoLabel: String? = null,
    val highlightedSubjectId: String? = null,
    val exportBannerMessage: String? = null,
    val runtimeConfig: SupabaseRuntimeConfig = FeatureFlags.resolveConfig(),
    val remoteProfile: RemoteProfileDto? = null,
    val loginIntentPath: LoginIntentPath = LoginIntentPath.STUDENT,
    val isAuthenticating: Boolean = false,
    val authErrorMessage: String? = null,
    val adminManagedProfiles: List<RemoteProfileDto> = emptyList(),
    val adminAuditLogs: List<RemoteAuditLogDto> = emptyList()
)

class CoreViewModel(
    private val coreRepository: CoreCampusRepository,
    private val userPreferences: UserPreferencesRepository,
    private val supabaseSyncRepository: SupabaseSyncRepository
) : ViewModel() {

    private val undoState = MutableStateFlow<Pair<String?, String?>>(null to null)
    private val highlightedSubjectFlow = MutableStateFlow<String?>(null)
    private val exportBannerFlow = MutableStateFlow<String?>(null)
    private val authBusyFlow = MutableStateFlow(false)
    private val authErrorFlow = MutableStateFlow<String?>(null)
    private val adminProfilesFlow = MutableStateFlow<List<RemoteProfileDto>>(emptyList())
    private val adminLogsFlow = MutableStateFlow<List<RemoteAuditLogDto>>(emptyList())

    private val baseDataFlow = combine(
        userPreferences.userProfileFlow,
        coreRepository.observeSubjectsWithAdvice(),
        coreRepository.observeRecentAttendanceHistory(),
        coreRepository.observeTimetableSlots(),
        coreRepository.observePersonalNotices()
    ) { profile: UserProfile,
        subjects: List<SubjectSummary>,
        history: List<AttendanceRecordItem>,
        slots: List<TimetableSlot>,
        notices: List<PersonalNotice> ->
        BaseCoreTuple(profile, subjects, history, slots, notices)
    }

    private val uiOverlaysFlow = combine(
        undoState,
        highlightedSubjectFlow,
        exportBannerFlow,
        authBusyFlow,
        authErrorFlow
    ) { undoPair, highlightedId, exportMsg, isAuthBusy, authErr ->
        OverlayTuple(undoPair, highlightedId, exportMsg, isAuthBusy, authErr)
    }

    val uiState: StateFlow<CoreUiState> = combine(
        baseDataFlow,
        userPreferences.cloudAuthStateFlow,
        uiOverlaysFlow,
        adminProfilesFlow,
        adminLogsFlow
    ) { base: BaseCoreTuple,
        cloudAuth: PersistedCloudAuthState,
        overlay: OverlayTuple,
        adminProfiles: List<RemoteProfileDto>,
        adminLogs: List<RemoteAuditLogDto> ->
        val totalAttended = base.subjects.sumOf { it.attendedCount }
        val totalConducted = base.subjects.sumOf { it.totalClassesCount }
        val overallAdvice = AttendanceCalculator.evaluate(
            attended = totalAttended,
            totalConducted = totalConducted,
            targetPercentage = base.profile.targetAttendancePercent
        )
        val todayDow = LocalDate.now().dayOfWeek.value
        val nowMinutes = LocalTime.now().let { it.hour * 60 + it.minute }
        val nextSlot = TimetableEngine.findNextClassToday(base.slots, todayDow, nowMinutes)
        val runtimeCfg = FeatureFlags.resolveConfig(
            overrideUrl = cloudAuth.overrideSupabaseUrl,
            overrideKey = cloudAuth.overrideSupabaseKey,
            overrideGoogleClientId = cloudAuth.overrideGoogleClientId
        )

        CoreUiState(
            isLoading = false,
            profile = base.profile,
            subjects = base.subjects,
            overallPercentage = overallAdvice.currentPercentage,
            recentHistory = base.history,
            timetableSlots = base.slots,
            nextClassToday = nextSlot,
            personalNotices = base.notices,
            lastUndoRecordId = overlay.undoPair.first,
            lastUndoLabel = overlay.undoPair.second,
            highlightedSubjectId = overlay.highlightedSubId,
            exportBannerMessage = overlay.exportMsg,
            runtimeConfig = runtimeCfg,
            remoteProfile = cloudAuth.remoteProfile,
            loginIntentPath = cloudAuth.loginIntentPath,
            isAuthenticating = overlay.isAuthBusy,
            authErrorMessage = overlay.authErr,
            adminManagedProfiles = adminProfiles,
            adminAuditLogs = adminLogs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CoreUiState()
    )

    init {
        viewModelScope.launch {
            coreRepository.ensurePilotSeedData()
            supabaseSyncRepository.refreshSessionAndProfileIfNeeded()
        }
    }

    fun signInWithGoogle(activityContext: Context, intentPath: LoginIntentPath) {
        viewModelScope.launch {
            authBusyFlow.value = true
            authErrorFlow.value = null
            val cfg = supabaseSyncRepository.activeRuntimeConfig()
            if (!cfg.isSupabaseConfigured || !cfg.isGoogleAuthConfigured) {
                authBusyFlow.value = false
                authErrorFlow.value = cfg.missingConfigDiagnosis()
                return@launch
            }

            val googleTokenResult = GoogleCredentialHelper.requestGoogleIdToken(
                activityContext = activityContext,
                googleWebClientId = cfg.googleWebClientId
            )

            googleTokenResult.onSuccess { googleRes ->
                supabaseSyncRepository.authenticateWithGoogleIdToken(
                    idToken = googleRes.idToken,
                    intentPath = intentPath
                ).onSuccess {
                    authBusyFlow.value = false
                    authErrorFlow.value = null
                    CampusWidgetUpdater.refreshAllWidgets(activityContext)
                }.onFailure { err ->
                    authBusyFlow.value = false
                    authErrorFlow.value = "Supabase Auth failed: ${err.message}"
                }
            }.onFailure { err ->
                authBusyFlow.value = false
                authErrorFlow.value = err.message
            }
        }
    }

    fun switchLoginIntentPath(intentPath: LoginIntentPath) {
        viewModelScope.launch {
            userPreferences.updateLoginIntentPath(intentPath)
            authErrorFlow.value = null
        }
    }

    fun signOutOfSupabase(context: Context) {
        viewModelScope.launch {
            supabaseSyncRepository.signOut()
            adminProfilesFlow.value = emptyList()
            adminLogsFlow.value = emptyList()
            exportBannerFlow.value = "Signed out of Supabase."
            CampusWidgetUpdater.refreshAllWidgets(context)
        }
    }

    fun saveRuntimeSupabaseConfig(
        supabaseUrl: String,
        supabasePublishableKey: String,
        googleWebClientId: String
    ) {
        viewModelScope.launch {
            userPreferences.saveRuntimeConfigOverrides(
                supabaseUrl = supabaseUrl,
                supabasePublishableKey = supabasePublishableKey,
                googleWebClientId = googleWebClientId
            )
            authErrorFlow.value = null
            exportBannerFlow.value = "Supabase & Google OAuth configuration saved."
        }
    }

    fun refreshSupabaseSync(context: Context) {
        viewModelScope.launch {
            supabaseSyncRepository.refreshSessionAndProfileIfNeeded()
                .onSuccess { prof ->
                    if (prof != null) {
                        exportBannerFlow.value = "Synced live campus data for ${prof.email} (${prof.role})."
                        CampusWidgetUpdater.refreshAllWidgets(context)
                    } else {
                        exportBannerFlow.value = "Sign in with Google first to sync from Supabase."
                    }
                }
                .onFailure { err ->
                    exportBannerFlow.value = "Sync failed: ${err.message}"
                }
        }
    }

    fun publishNoticeAsAdmin(
        title: String,
        content: String,
        priority: String,
        isPinned: Boolean,
        publishImmediately: Boolean
    ) {
        viewModelScope.launch {
            val deptId = uiState.value.remoteProfile?.departmentId
            supabaseSyncRepository.createNoticeFromAdminConsole(
                title = title,
                content = content,
                departmentId = deptId,
                priority = priority,
                isPinned = isPinned,
                publishImmediately = publishImmediately
            ).onSuccess {
                exportBannerFlow.value = if (publishImmediately) {
                    "Notice published to Supabase."
                } else {
                    "Draft notice saved to Supabase."
                }
            }.onFailure { err ->
                exportBannerFlow.value = "Failed to save notice: ${err.message}"
            }
        }
    }

    fun loadAdminUsersAndAuditLogs() {
        viewModelScope.launch {
            supabaseSyncRepository.fetchProfilesForAdmin().onSuccess {
                adminProfilesFlow.value = it
            }
            supabaseSyncRepository.fetchAuditLogsForSuperAdmin().onSuccess {
                adminLogsFlow.value = it
            }
        }
    }

    fun assignRoleAsSuperAdmin(
        targetUserId: String,
        newRole: String,
        newDepartmentId: String?,
        newAccountStatus: String
    ) {
        viewModelScope.launch {
            supabaseSyncRepository.assignRoleAsSuperAdmin(
                targetUserId = targetUserId,
                newRole = newRole,
                newDepartmentId = newDepartmentId,
                newAccountStatus = newAccountStatus
            ).onSuccess { updated ->
                exportBannerFlow.value = "Updated ${updated.email} -> role=${updated.role}, status=${updated.accountStatus}"
                loadAdminUsersAndAuditLogs()
            }.onFailure { err ->
                exportBannerFlow.value = "Role update denied: ${err.message}"
            }
        }
    }

    fun setHighlightedSubject(subjectId: String?) {
        highlightedSubjectFlow.value = subjectId
    }

    fun clearExportBanner() {
        exportBannerFlow.value = null
    }

    fun dismissUndo() {
        undoState.value = null to null
    }

    fun completeSetup(
        name: String,
        college: String,
        course: String,
        semester: Int,
        section: String,
        targetPct: Int
    ) {
        viewModelScope.launch {
            userPreferences.saveSetupProfile(
                name = name,
                college = college,
                course = course,
                semester = semester,
                section = section,
                targetAttendancePercent = targetPct
            )
        }
    }

    fun markAttendance(context: Context, subjectId: String, status: AttendanceStatus) {
        viewModelScope.launch {
            val subjectName = uiState.value.subjects.firstOrNull { it.id == subjectId }?.code ?: "Subject"
            val recordId = coreRepository.markAttendance(subjectId, status)
            if (recordId != null) {
                undoState.value = recordId to "$subjectName marked ${status.name}"
            }
            CampusWidgetUpdater.refreshAllWidgets(context)
            LowAttendanceAlertWorker.triggerImmediateCheck(context)
        }
    }

    fun undoLastAttendance(context: Context) {
        val recordId = undoState.value.first ?: return
        viewModelScope.launch {
            coreRepository.deleteOrUndoAttendanceRecord(recordId)
            undoState.value = null to null
            CampusWidgetUpdater.refreshAllWidgets(context)
        }
    }

    fun editAttendanceRecord(
        context: Context,
        recordId: String,
        newStatus: AttendanceStatus,
        newEpochDay: Long
    ) {
        viewModelScope.launch {
            coreRepository.editAttendanceRecord(recordId, newStatus, newEpochDay)
            CampusWidgetUpdater.refreshAllWidgets(context)
            LowAttendanceAlertWorker.triggerImmediateCheck(context)
        }
    }

    fun deleteAttendanceRecord(context: Context, recordId: String) {
        viewModelScope.launch {
            coreRepository.deleteOrUndoAttendanceRecord(recordId)
            if (undoState.value.first == recordId) {
                undoState.value = null to null
            }
            CampusWidgetUpdater.refreshAllWidgets(context)
            LowAttendanceAlertWorker.triggerImmediateCheck(context)
        }
    }

    fun addSubject(
        context: Context,
        code: String,
        name: String,
        professor: String,
        semester: Int,
        targetPercentage: Int = 75,
        colorHex: String = "#0F4C81"
    ) {
        viewModelScope.launch {
            coreRepository.addSubject(code, name, professor, semester, targetPercentage, colorHex)
            CampusWidgetUpdater.refreshAllWidgets(context)
        }
    }

    fun updateSubjectDetails(
        context: Context,
        subjectId: String,
        code: String,
        name: String,
        professor: String,
        targetPercentage: Int,
        colorHex: String
    ) {
        viewModelScope.launch {
            coreRepository.updateSubjectDetails(subjectId, code, name, professor, targetPercentage, colorHex)
            CampusWidgetUpdater.refreshAllWidgets(context)
            LowAttendanceAlertWorker.triggerImmediateCheck(context)
        }
    }

    fun deleteSubject(context: Context, subjectId: String) {
        viewModelScope.launch {
            coreRepository.deleteSubject(subjectId)
            CampusWidgetUpdater.refreshAllWidgets(context)
        }
    }

    fun updateTargetAttendance(targetPct: Int) {
        viewModelScope.launch {
            userPreferences.updateTargetAttendance(targetPct)
        }
    }

    fun updateLowAttendanceAlerts(context: Context, enabled: Boolean, thresholdPct: Int) {
        viewModelScope.launch {
            userPreferences.updateLowAttendanceAlertSettings(enabled, thresholdPct)
            if (enabled) {
                LowAttendanceAlertWorker.scheduleDailyCheck(context)
                LowAttendanceAlertWorker.triggerImmediateCheck(context)
            }
        }
    }

    fun updateSemesterEndEpochDay(epochDay: Long) {
        viewModelScope.launch {
            userPreferences.updateSemesterEndEpochDay(epochDay)
        }
    }

    fun updateReminderLeadMinutes(leadMins: Int) {
        viewModelScope.launch {
            userPreferences.updateReminderLeadMinutes(leadMins)
        }
    }

    fun saveTimetableSlot(
        context: Context,
        slotId: String? = null,
        subjectId: String = "",
        subjectName: String,
        subjectCode: String,
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        room: String,
        teacherName: String,
        colorHex: String = "#0F4C81"
    ) {
        viewModelScope.launch {
            val p = uiState.value.profile
            coreRepository.upsertTimetableSlot(
                slotId = slotId,
                subjectId = subjectId,
                subjectName = subjectName,
                subjectCode = subjectCode,
                dayOfWeek = dayOfWeek,
                startTime = startTime,
                endTime = endTime,
                room = room,
                teacherName = teacherName,
                semester = p.semester,
                section = p.section,
                colorHex = colorHex
            )
            val todayDow = LocalDate.now().dayOfWeek.value
            val nowMins = LocalTime.now().let { it.hour * 60 + it.minute }
            val delayMillis = TimetableEngine.computeNextOccurrenceDelayMillis(
                dayOfWeek = dayOfWeek,
                startTime = startTime,
                leadMinutes = p.reminderLeadMinutes,
                currentDayOfWeek = todayDow,
                currentMinutesOfDay = nowMins
            )
            ClassReminderWorker.scheduleOneTimeReminder(
                context = context,
                uniqueWorkName = "class_rem_${subjectCode}_${dayOfWeek}_$startTime",
                title = "Upcoming Class: $subjectName",
                message = "$startTime - $endTime in $room ($teacherName)",
                delayMillis = delayMillis
            )
            CampusWidgetUpdater.refreshAllWidgets(context)
        }
    }

    fun deleteTimetableSlot(context: Context, slotId: String) {
        viewModelScope.launch {
            coreRepository.deleteTimetableSlot(slotId)
            CampusWidgetUpdater.refreshAllWidgets(context)
        }
    }

    fun shareTimetableAndAttendancePdf(context: Context) {
        viewModelScope.launch {
            val state = uiState.value
            TimetableAndAttendanceExportHelper.exportAsPdf(
                context = context,
                profile = state.profile,
                slots = state.timetableSlots,
                subjects = state.subjects
            ).onSuccess { file ->
                exportBannerFlow.value = "PDF ready: ${file.name}"
                CampusIntentHelper.shareFileViaProvider(context, file, "application/pdf")
            }.onFailure { err ->
                exportBannerFlow.value = "PDF export failed: ${err.message}"
            }
        }
    }

    fun shareTimetableAndAttendancePng(context: Context) {
        viewModelScope.launch {
            val state = uiState.value
            TimetableAndAttendanceExportHelper.exportAsPng(
                context = context,
                profile = state.profile,
                slots = state.timetableSlots,
                subjects = state.subjects
            ).onSuccess { file ->
                exportBannerFlow.value = "PNG image ready: ${file.name}"
                CampusIntentHelper.shareFileViaProvider(context, file, "image/png")
            }.onFailure { err ->
                exportBannerFlow.value = "PNG export failed: ${err.message}"
            }
        }
    }

    fun addPersonalNotice(title: String, body: String, isPinned: Boolean) {
        viewModelScope.launch {
            coreRepository.addPersonalNotice(title, body, isPinned)
        }
    }

    fun markNoticeRead(noticeId: String) {
        viewModelScope.launch {
            coreRepository.markNoticeRead(noticeId)
        }
    }

    fun deletePersonalNotice(noticeId: String) {
        viewModelScope.launch {
            coreRepository.deletePersonalNotice(noticeId)
        }
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            val current = uiState.value.profile.languageCode
            val next = if (current == "hi") "en" else "hi"
            userPreferences.updateLanguage(next)
            try {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(next))
            } catch (_: Exception) {
            }
        }
    }

    private data class BaseCoreTuple(
        val profile: UserProfile,
        val subjects: List<SubjectSummary>,
        val history: List<AttendanceRecordItem>,
        val slots: List<TimetableSlot>,
        val notices: List<PersonalNotice>
    )

    private data class OverlayTuple(
        val undoPair: Pair<String?, String?>,
        val highlightedSubId: String?,
        val exportMsg: String?,
        val isAuthBusy: Boolean,
        val authErr: String?
    )

    companion object {
        fun provideFactory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CoreViewModel(
                        coreRepository = container.coreCampusRepository,
                        userPreferences = container.userPreferencesRepository,
                        supabaseSyncRepository = container.supabaseSyncRepository
                    ) as T
                }
            }
    }
}
