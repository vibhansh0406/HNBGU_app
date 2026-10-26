package `in`.campusone.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.campusone.app.data.local.entity.AttendanceLogEntity
import `in`.campusone.app.data.preferences.UserPreferencesRepository
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
import `in`.campusone.app.domain.usecase.classEvaluatedSlot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
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
    val todaySchedule: List<classEvaluatedSlot> = emptyList(),
    val nextClass: NextClassResult? = null,
    val notices: List<NoticeItem> = emptyList(),
    val unreadNoticeCount: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CoreViewModel @Inject constructor(
    private val repository: CoreCampusRepository,
    private val prefs: UserPreferencesRepository
) : ViewModel() {

    private val _uiMessages = MutableSharedFlow<String>()
    val uiMessages = _uiMessages.asSharedFlow()

    val uiState: StateFlow<CoreUiState> = combine(
        prefs.isOnboarded,
        prefs.userProfile,
        prefs.languageTag
    ) { onboarded, profile, lang ->
        Triple(onboarded, profile, lang)
    }.flatMapLatest { (onboarded, profile, lang) ->
        viewModelScope.launch {
            repository.ensureSeededForSection(profile.sectionKey)
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
            val profile = UserProfile(
                uid = "local_demo_${email.hashCode().toUInt()}",
                name = name.ifBlank { "HNBGU Student" },
                email = email.ifBlank { "student@hnbgu.ac.in" },
                role = role,
                verifiedRole = false,
                collegeId = "hnbgu",
                collegeName = collegeName,
                courseId = courseId,
                courseName = courseName,
                semester = semester,
                section = section
            )
            prefs.saveUserProfile(profile, markOnboarded = true)
            repository.ensureSeededForSection(profile.sectionKey)
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

    fun publishTimetableSlot(slot: TimetableSlot) {
        viewModelScope.launch {
            val result = repository.saveAndPublishTimetableSlot(uiState.value.profile, slot)
            if (result.isSuccess) {
                _uiMessages.emit("Timetable saved on this device (local demo).")
            } else {
                _uiMessages.emit(result.exceptionOrNull()?.message ?: "Failed to update timetable")
            }
        }
    }

    fun postNotice(
        title: String,
        body: String,
        attachmentUrl: String?,
        attachmentType: AttachmentType,
        isPinned: Boolean
    ) {
        viewModelScope.launch {
            val result = repository.postNotice(
                profile = uiState.value.profile,
                title = title,
                body = body,
                attachmentUrl = attachmentUrl,
                attachmentType = attachmentType,
                isPinned = isPinned
            )
            if (result.isSuccess) {
                _uiMessages.emit("Notice saved on this device (local demo).")
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
}
