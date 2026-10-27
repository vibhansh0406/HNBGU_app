package `in`.campusone.app.ui.viewmodel

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import `in`.campusone.app.data.preferences.UserPreferencesRepository
import `in`.campusone.app.data.repository.CoreCampusRepository
import `in`.campusone.app.di.AppContainer
import `in`.campusone.app.domain.model.AttendanceRecordItem
import `in`.campusone.app.domain.model.AttendanceStatus
import `in`.campusone.app.domain.model.PersonalNotice
import `in`.campusone.app.domain.model.SubjectSummary
import `in`.campusone.app.domain.model.TimetableSlot
import `in`.campusone.app.domain.model.UserProfile
import `in`.campusone.app.domain.usecase.AttendanceCalculator
import `in`.campusone.app.domain.usecase.TimetableEngine
import `in`.campusone.app.worker.ClassReminderWorker
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
    val personalNotices: List<PersonalNotice> = emptyList()
)

class CoreViewModel(
    private val coreRepository: CoreCampusRepository,
    private val userPreferences: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<CoreUiState> = combine(
        userPreferences.userProfileFlow,
        coreRepository.observeSubjectsWithAdvice(),
        coreRepository.observeRecentAttendanceHistory(),
        coreRepository.observeTimetableSlots(),
        coreRepository.observePersonalNotices()
    ) { profile, subjects, history, slots, notices ->
        val totalAttended = subjects.sumOf { it.attendedCount }
        val totalConducted = subjects.sumOf { it.totalClassesCount }
        val overallAdvice = AttendanceCalculator.evaluate(
            attended = totalAttended,
            totalConducted = totalConducted,
            targetPercentage = profile.targetAttendancePercent
        )
        val todayDow = LocalDate.now().dayOfWeek.value
        val nowMinutes = LocalTime.now().let { it.hour * 60 + it.minute }
        val nextSlot = TimetableEngine.findNextClassToday(slots, todayDow, nowMinutes)

        CoreUiState(
            isLoading = false,
            profile = profile,
            subjects = subjects,
            overallPercentage = overallAdvice.currentPercentage,
            recentHistory = history,
            timetableSlots = slots,
            nextClassToday = nextSlot,
            personalNotices = notices
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CoreUiState()
    )

    init {
        viewModelScope.launch {
            coreRepository.ensurePilotSeedData()
        }
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

    fun markAttendance(subjectId: String, status: AttendanceStatus) {
        viewModelScope.launch {
            coreRepository.markAttendance(subjectId, status)
        }
    }

    fun addSubject(code: String, name: String, professor: String, semester: Int) {
        viewModelScope.launch {
            coreRepository.addSubject(code, name, professor, semester)
        }
    }

    fun updateTargetAttendance(targetPct: Int) {
        viewModelScope.launch {
            userPreferences.updateTargetAttendance(targetPct)
        }
    }

    fun addTimetableSlot(
        context: Context,
        subjectName: String,
        subjectCode: String,
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        room: String,
        teacherName: String
    ) {
        viewModelScope.launch {
            val p = uiState.value.profile
            coreRepository.addTimetableSlot(
                subjectName = subjectName,
                subjectCode = subjectCode,
                dayOfWeek = dayOfWeek,
                startTime = startTime,
                endTime = endTime,
                room = room,
                teacherName = teacherName,
                semester = p.semester,
                section = p.section
            )
            ClassReminderWorker.scheduleOneTimeReminder(
                context = context,
                uniqueWorkName = "class_rem_${subjectCode}_${dayOfWeek}_$startTime",
                title = "Upcoming Class: $subjectName",
                message = "$startTime - $endTime in $room ($teacherName)",
                delayMillis = 15 * 60_000L
            )
        }
    }

    fun deleteTimetableSlot(slotId: String) {
        viewModelScope.launch {
            coreRepository.deleteTimetableSlot(slotId)
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

    companion object {
        fun provideFactory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CoreViewModel(
                        coreRepository = container.coreCampusRepository,
                        userPreferences = container.userPreferencesRepository
                    ) as T
                }
            }
    }
}
