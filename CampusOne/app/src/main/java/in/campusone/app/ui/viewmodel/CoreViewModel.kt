package `in`.campusone.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.campusone.app.data.local.entity.AttendanceLogEntity
import `in`.campusone.app.data.preferences.UserPreferencesRepository
import `in`.campusone.app.data.remote.AuthManager
import `in`.campusone.app.data.repository.CoreCampusRepository
import `in`.campusone.app.domain.model.AttachmentType
import `in`.campusone.app.domain.model.AttendanceStatus
import `in`.campusone.app.domain.model.NoticeItem
import `in`.campusone.app.domain.model.SubjectAttendanceSummary
import `in`.campusone.app.domain.model.TimetableSlot
import `in`.campusone.app.domain.model.UserProfile
import `in`.campusone.app.domain.model.UserRole
import `in`.campusone.app.domain.usecase.AttendanceCalculator
import `in`.campusone.app.domain.usecase.AttendanceInsight
import `in`.campusone.app.domain.usecase.NextClassResult
import `in`.campusone.app.domain.usecase.TimetableEngine
import `in`.campusone.app.domain.usecase.EvaluatedSlot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class SubjectWithInsight(
    val summary: SubjectAttendanceSummary,
    val insight: AttendanceInsight
)

data class CoreUiState(
    val isLoading: Boolean = true,
    val isOnboarded: Boolean = false,
    val profile: UserProfile = UserProfile(),
    val languageTag: String = "en",
    val overallInsight: AttendanceInsight = AttendanceCalculator.calculate(0, 0),
    val subjectInsights: List<SubjectWithInsight> = emptyList(),
    val recentAttendanceLogs: List<AttendanceLogEntity> = emptyList(),
    val allTimetableSlots: List<TimetableSlot> = emptyList(),
    val todaySchedule: List<EvaluatedSlot> = emptyList(),
    val nextClass: NextClassResult? = null,
    val notices: List<NoticeItem> = emptyList(),
    val unreadNoticeCount: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CoreViewModel @Inject constructor(
    private val repository: CoreCampusRepository,
    private val prefs: UserPreferencesRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val _uiMessages = MutableSharedFlow<String>()
    val uiMessages = _uiMessages.asSharedFlow()

    private val _noticesSyncError = MutableStateFlow(false)
    val noticesSyncError: StateFlow<Boolean> = _noticesSyncError.asStateFlow()

    val introSeen: StateFlow<Boolean?> = prefs.hasSeenIntro
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun markIntroSeen() {
        viewModelScope.launch { prefs.markIntroSeen() }
    }

    init {
        viewModelScope.launch {
            authManager.session.collect { session ->
                if (session != null) {
                    runCatching {
                        syncProfileOnSignIn(
                            uid = session.user?.id,
                            sessionEmail = session.user?.email
                        )
                    }
                }
            }
        }
    }

    private suspend fun syncProfileOnSignIn(uid: String?, sessionEmail: String?) {
        val resolvedUid = uid ?: authManager.currentUserId() ?: return
        val email = sessionEmail ?: authManager.currentUserEmail()
        val isOwner = email?.equals(CoreCampusRepository.OWNER_EMAIL, ignoreCase = true) == true
        val remote = runCatching { repository.fetchRemoteProfile(resolvedUid) }.getOrNull()

        if (remote == null) {
            // No remote profile exists — create one with onboarding_completed = FALSE.
            // The server-side trigger or bootstrap_profile RPC handles defaults.
            val local = prefs.userProfile.first()
            val resolvedEmail = email?.takeIf { it.isNotBlank() } ?: local.email
            // Server has no profile: mark local as not onboarded so onboarding is shown.
            prefs.saveUserProfile(local.copy(onboardingCompleted = false), markOnboarded = false)
            authManager.ensureUserProfile(
                uid = resolvedUid,
                email = resolvedEmail,
                name = local.name,
                role = if (isOwner) UserRole.ADMIN.name else UserRole.STUDENT.name
            )
            return
        }

        // Remote profile exists. Server's onboardingCompleted is the source of truth.
        val remoteRole = if (isOwner) UserRole.ADMIN else remote.role

        if (!remote.onboardingCompleted) {
            // Not onboarded yet — sync profile data but keep local flag as not onboarded.
            // The UI will show the onboarding screen.
            prefs.saveUserProfile(remote.copy(role = remoteRole), markOnboarded = false)
            return
        }

        // Onboarded — sync local state to match the server.
        val local = prefs.userProfile.first()
        val resolvedEmail = email?.takeIf { it.isNotBlank() } ?: local.email
        if (local.role != remoteRole || local.name != remote.name || local.email != resolvedEmail || !local.onboardingCompleted) {
            prefs.saveUserProfile(
                local.copy(role = remoteRole, name = remote.name, email = resolvedEmail, onboardingCompleted = true),
                markOnboarded = true
            )
        }
    }

    val uiState: StateFlow<CoreUiState> = combine(
        prefs.isOnboarded,
        prefs.userProfile,
        prefs.languageTag
    ) { onboarded, profile, lang ->
        Triple(onboarded, profile, lang)
    }.flatMapLatest { (onboarded, profile, lang) ->
        viewModelScope.launch {
            _noticesSyncError.value = repository.ensureSeededForSection(profile.sectionKey).isFailure
        }
        combine(
            repository.observeSubjectAttendanceSummaries(profile.sectionKey),
            repository.observeAllAttendanceLogs(),
            repository.observeTimetable(profile.sectionKey),
            repository.observeNotices(profile.sectionKey),
            repository.observeUnreadNoticeCount(profile.sectionKey)
        ) { summaries, logs, slots, notices, unreadCount ->
            val targetPct = profile.targetAttendancePct
            val subjectInsights = summaries.map { summary ->
                SubjectWithInsight(
                    summary = summary,
                    insight = AttendanceCalculator.calculate(
                        attended = summary.attendedCount,
                        totalConducted = summary.totalClasses,
                        cancelled = summary.cancelledCount,
                        targetPercentage = targetPct
                    )
                )
            }
            val totalAttended = summaries.sumOf { it.attendedCount }
            val totalConducted = summaries.sumOf { it.totalClasses }
            val totalCancelled = summaries.sumOf { it.cancelledCount }
            val overall = AttendanceCalculator.calculate(
                attended = totalAttended,
                totalConducted = totalConducted,
                cancelled = totalCancelled,
                targetPercentage = targetPct
            )

            val now = LocalDateTime.now()
            val todayEval = TimetableEngine.evaluateTodaySchedule(slots, now)
            val nextClass = TimetableEngine.findNextOrOngoingClass(slots, now)

            CoreUiState(
                isLoading = false,
                isOnboarded = onboarded,
                profile = profile,
                languageTag = lang,
                overallInsight = overall,
                subjectInsights = subjectInsights,
                recentAttendanceLogs = logs.take(60),
                allTimetableSlots = slots,
                todaySchedule = todayEval,
                nextClass = nextClass,
                notices = notices,
                unreadNoticeCount = unreadCount
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CoreUiState()
    )

    fun completeOnboarding(
        name: String,
        email: String,
        collegeName: String,
        courseId: String,
        courseName: String,
        semester: Int,
        section: String,
        role: UserRole
    ) {
        viewModelScope.launch {
            val uid = authManager.currentUserId() ?: "local_${email.hashCode().toUInt()}"
            val signedInEmail = authManager.currentUserEmail().orEmpty()
            val isOwner = signedInEmail.equals(CoreCampusRepository.OWNER_EMAIL, ignoreCase = true) ||
                email.trim().equals(CoreCampusRepository.OWNER_EMAIL, ignoreCase = true)
            val requestedRole = role
            val effectiveRole = if (isOwner) UserRole.ADMIN else UserRole.STUDENT
            val profileEmail = signedInEmail.ifBlank { email.trim() }
            val profile = UserProfile(
                uid = uid,
                name = name.ifBlank { "HNBGU Student" },
                email = profileEmail.ifBlank { "student@hnbgu.ac.in" },
                role = effectiveRole,
                verifiedRole = true,
                collegeId = "hnbgu",
                collegeName = collegeName,
                courseId = courseId,
                courseName = courseName,
                semester = semester,
                section = section,
                onboardingCompleted = false // Not yet persisted to server
            )
            // Save locally first (not yet onboarded) so data isn't lost if server fails.
            prefs.saveUserProfile(profile, markOnboarded = false)
            repository.ensureSeededForSection(profile.sectionKey)

            // Persist to server: bootstrap profile + upload full details.
            val bootstrapResult = authManager.ensureUserProfile(
                uid = uid,
                email = profileEmail,
                name = name,
                role = effectiveRole.name
            )
            if (!bootstrapResult.isSuccess) {
                _uiMessages.emit("Could not save profile to server. Please try again.")
                return@launch
            }

            // Sync full profile (including onboarding_completed = true) to cloud.
            val cloudProfile = profile.copy(onboardingCompleted = true)
            repository.syncProfileToCloud(cloudProfile)

            // Only now mark as onboarded locally — server persistence succeeded.
            prefs.saveUserProfile(cloudProfile, markOnboarded = true)

            if (!isOwner && requestedRole != UserRole.STUDENT) {
                repository.requestRoleChange(requestedRole, "Requested during onboarding")
                    .onSuccess { _uiMessages.emit("Role request sent for admin approval.") }
                    .onFailure { _uiMessages.emit("Could not send role request.") }
            }
        }
    }

    fun markAttendance(subjectId: String, status: AttendanceStatus, slotId: String = "manual") {
        viewModelScope.launch {
            repository.markAttendance(
                subjectId = subjectId,
                date = LocalDate.now(),
                slotId = slotId,
                status = status
            )
        }
    }

    fun updateTargetPercentage(targetPct: Int) {
        viewModelScope.launch {
            prefs.updateTargetAttendance(targetPct)
        }
    }

    fun updateReminderLeadMinutes(minutes: Int) {
        viewModelScope.launch {
            prefs.updateReminderLeadTime(minutes)
        }
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            val nextLang = if (uiState.value.languageTag == "hi") "en" else "hi"
            prefs.switchLanguage(nextLang)
        }
    }

    fun updateDisplayName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val profile = prefs.userProfile.first().copy(name = trimmed)
            prefs.saveUserProfile(profile, markOnboarded = true)
            repository.syncProfileToCloud(profile)
            _uiMessages.emit("Profile updated.")
        }
    }

    fun postNotice(
        title: String,
        body: String,
        attachmentUrl: String?,
        attachmentType: AttachmentType,
        isPinned: Boolean,
        category: String = "GENERAL",
        priority: String = "NORMAL",
        isOfficial: Boolean = false
    ) {
        viewModelScope.launch {
            val result = repository.postNotice(
                profile = uiState.value.profile,
                title = title,
                body = body,
                attachmentUrl = attachmentUrl,
                attachmentType = attachmentType,
                isPinned = isPinned,
                category = category,
                priority = priority,
                isOfficial = isOfficial
            )
            if (result.isSuccess) {
                _uiMessages.emit("Notice published to section!")
            } else {
                _uiMessages.emit(result.exceptionOrNull()?.message ?: "Failed to post notice")
            }
        }
    }

    fun markNoticeRead(noticeId: String) {
        viewModelScope.launch {
            repository.markNoticeRead(noticeId)
        }
    }

    fun markAllNoticesRead() {
        viewModelScope.launch {
            repository.markAllNoticesRead(uiState.value.profile.sectionKey)
        }
    }

    fun refreshNotices() {
        viewModelScope.launch {
            val sectionKey = prefs.userProfile.first().sectionKey
            _noticesSyncError.value =
                repository.ensureSeededForSection(sectionKey, forceRefresh = true).isFailure
        }
    }

    fun refreshTimetable() {
        viewModelScope.launch {
            val sectionKey = prefs.userProfile.first().sectionKey
            val result = repository.ensureSeededForSection(sectionKey, forceRefresh = true)
            _noticesSyncError.value = result.isFailure
            if (result.isFailure) {
                _uiMessages.emit("Could not refresh timetable. Check your connection.")
            }
        }
    }

    /** Current Supabase authenticated user's avatar URL (avatar_url / picture from auth metadata). */
    val currentUserAvatarUrl: String?
        get() = authManager.currentUserAvatarUrl()
}
