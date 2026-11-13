package `in`.campusone.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.campusone.app.data.local.entity.AssignmentEntity
import `in`.campusone.app.data.local.entity.CampusEventEntity
import `in`.campusone.app.data.local.entity.ClubEntity
import `in`.campusone.app.data.local.entity.DirectoryContactEntity
import `in`.campusone.app.data.local.entity.ExamEntity
import `in`.campusone.app.data.local.entity.LostFoundEntity
import `in`.campusone.app.data.local.entity.NoticeEntity
import `in`.campusone.app.data.local.entity.StudyResourceEntity
import `in`.campusone.app.data.local.entity.SubjectEntity
import `in`.campusone.app.data.local.entity.SyllabusTopicEntity
import `in`.campusone.app.data.local.entity.TimetableSlotEntity
import `in`.campusone.app.data.repository.AdminRepository
import `in`.campusone.app.domain.model.UserProfile
import `in`.campusone.app.domain.model.UserRole
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AdminViewModel @Inject constructor(
    private val repository: AdminRepository
) : ViewModel() {

    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()

    data class AdminUiState(
        val isAdmin: Boolean = false,
        val profile: UserProfile = UserProfile(),
        val sectionKey: String = "",
        val collegeId: String = "hnbgu",
        // Stats
        val subjectCount: Int = 0,
        val noticeCount: Int = 0,
        val assignmentCount: Int = 0,
        val examCount: Int = 0,
        val clubCount: Int = 0,
        val eventCount: Int = 0,
        // Lists
        val subjects: List<SubjectEntity> = emptyList(),
        val timetableSlots: List<TimetableSlotEntity> = emptyList(),
        val notices: List<NoticeEntity> = emptyList(),
        val assignments: List<AssignmentEntity> = emptyList(),
        val exams: List<ExamEntity> = emptyList(),
        val syllabusTopics: List<SyllabusTopicEntity> = emptyList(),
        val studyResources: List<StudyResourceEntity> = emptyList(),
        val clubs: List<ClubEntity> = emptyList(),
        val events: List<CampusEventEntity> = emptyList(),
        val lostFound: List<LostFoundEntity> = emptyList(),
        val directory: List<DirectoryContactEntity> = emptyList()
    )

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    fun init(profile: UserProfile) {
        viewModelScope.launch {
            val sectionKey = profile.sectionKey
            val collegeId = profile.collegeId
            val isAdmin = profile.role == UserRole.ADMIN || profile.role == UserRole.TEACHER ||
                profile.role == UserRole.CR

            combine(
                repository.observeSubjects(sectionKey),
                repository.observeTimetableSlots(sectionKey),
                repository.observeNotices(sectionKey),
                repository.observeAssignments(),
                repository.observeExams(),
                repository.observeSyllabusTopics(),
                repository.observeStudyResources(sectionKey),
                repository.observeClubs(collegeId),
                repository.observeEvents(collegeId),
                repository.observeLostFound(collegeId),
                repository.observeDirectory(collegeId)
            ) { subjects, slots, notices, assignments, exams, syllabus, resources, clubs, events, lostFound, directory ->
                AdminUiState(
                    isAdmin = isAdmin,
                    profile = profile,
                    sectionKey = sectionKey,
                    collegeId = collegeId,
                    subjects = subjects,
                    timetableSlots = slots,
                    notices = notices,
                    assignments = assignments,
                    exams = exams,
                    syllabusTopics = syllabus,
                    studyResources = resources,
                    clubs = clubs,
                    events = events,
                    lostFound = lostFound,
                    directory = directory,
                    subjectCount = subjects.size,
                    noticeCount = notices.size,
                    assignmentCount = assignments.size,
                    examCount = exams.size,
                    clubCount = clubs.size,
                    eventCount = events.size
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = AdminUiState(isAdmin = isAdmin, profile = profile, sectionKey = sectionKey, collegeId = collegeId)
            ).collect { _uiState.value = it }
        }
    }

    // =========================================================================
    // SUBJECTS
    // =========================================================================
    fun saveSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            repository.upsertSubjects(listOf(subject))
            _messages.emit("Subject saved: ${subject.subjectName}")
        }
    }

    fun deleteSubject(subjectId: String) {
        viewModelScope.launch {
            repository.deleteSubject(subjectId)
            _messages.emit("Subject removed")
        }
    }

    // =========================================================================
    // TIMETABLE
    // =========================================================================
    fun saveTimetableSlot(slot: TimetableSlotEntity) {
        viewModelScope.launch {
            repository.upsertTimetableSlots(listOf(slot))
            _messages.emit("Timetable slot saved: ${slot.subjectName}")
        }
    }

    // =========================================================================
    // NOTICES
    // =========================================================================
    fun saveNotice(notice: NoticeEntity) {
        viewModelScope.launch {
            repository.upsertNotices(listOf(notice))
            _messages.emit("Notice published: ${notice.title}")
        }
    }

    // =========================================================================
    // ASSIGNMENTS
    // =========================================================================
    fun saveAssignment(assignment: AssignmentEntity) {
        viewModelScope.launch {
            repository.upsertAssignments(listOf(assignment))
            _messages.emit("Assignment saved: ${assignment.title}")
        }
    }

    fun deleteAssignment(id: String) {
        viewModelScope.launch {
            repository.deleteAssignment(id)
            _messages.emit("Assignment deleted")
        }
    }

    // =========================================================================
    // EXAMS
    // =========================================================================
    fun saveExam(exam: ExamEntity) {
        viewModelScope.launch {
            repository.upsertExams(listOf(exam))
            _messages.emit("Exam saved: ${exam.subjectName}")
        }
    }

    fun deleteExam(id: String) {
        viewModelScope.launch {
            repository.deleteExam(id)
            _messages.emit("Exam deleted")
        }
    }

    // =========================================================================
    // SYLLABUS
    // =========================================================================
    fun saveSyllabusTopic(topic: SyllabusTopicEntity) {
        viewModelScope.launch {
            repository.upsertSyllabusTopics(listOf(topic))
            _messages.emit("Syllabus topic saved")
        }
    }

    // =========================================================================
    // STUDY RESOURCES
    // =========================================================================
    fun saveStudyResource(resource: StudyResourceEntity) {
        viewModelScope.launch {
            repository.upsertStudyResources(listOf(resource))
            _messages.emit("Resource saved: ${resource.title}")
        }
    }

    fun deleteStudyResource(id: String) {
        viewModelScope.launch {
            repository.deleteStudyResource(id)
            _messages.emit("Resource removed")
        }
    }

    // =========================================================================
    // CLUBS
    // =========================================================================
    fun saveClub(club: ClubEntity) {
        viewModelScope.launch {
            repository.upsertClubs(listOf(club))
            _messages.emit("Club saved: ${club.name}")
        }
    }

    fun approveClub(clubId: String) {
        viewModelScope.launch {
            repository.approveClub(clubId)
            _messages.emit("Club approved")
        }
    }

    // =========================================================================
    // EVENTS
    // =========================================================================
    fun saveEvent(event: CampusEventEntity) {
        viewModelScope.launch {
            repository.upsertEvents(listOf(event))
            _messages.emit("Event saved: ${event.title}")
        }
    }

    fun approveEvent(eventId: String) {
        viewModelScope.launch {
            repository.approveEvent(eventId)
            _messages.emit("Event approved")
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            repository.deleteEvent(eventId)
            _messages.emit("Event deleted")
        }
    }

    // =========================================================================
    // LOST & FOUND
    // =========================================================================
    fun saveLostFound(item: LostFoundEntity) {
        viewModelScope.launch {
            repository.upsertLostFound(listOf(item))
            _messages.emit("Lost & Found item saved: ${item.itemTitle}")
        }
    }

    fun resolveLostFound(id: String) {
        viewModelScope.launch {
            repository.resolveLostFound(id)
            _messages.emit("Item marked resolved")
        }
    }

    fun hideLostFound(id: String) {
        viewModelScope.launch {
            repository.hideLostFound(id)
            _messages.emit("Item hidden")
        }
    }

    // =========================================================================
    // DIRECTORY
    // =========================================================================
    fun saveDirectoryContact(contact: DirectoryContactEntity) {
        viewModelScope.launch {
            repository.upsertDirectory(listOf(contact))
            _messages.emit("Directory contact saved: ${contact.name}")
        }
    }

    // =========================================================================
    // PROFILE / ROLE
    // =========================================================================
    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.saveProfile(profile)
            _messages.emit("Profile updated")
        }
    }

    // Context helper for PDF export (kept minimal)
    fun contextString(context: Context): String = context.packageName
}
