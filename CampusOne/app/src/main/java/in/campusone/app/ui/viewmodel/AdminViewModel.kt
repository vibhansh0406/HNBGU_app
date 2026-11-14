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
import `in`.campusone.app.data.repository.AdminUserRecord
import `in`.campusone.app.data.repository.FlaggedItem
import `in`.campusone.app.data.repository.RoleRequestRecord
import `in`.campusone.app.data.repository.UserAttendanceRow
import `in`.campusone.app.domain.model.UserProfile
import `in`.campusone.app.domain.model.UserRole
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
        val directory: List<DirectoryContactEntity> = emptyList(),
        // Remote admin data
        val users: List<AdminUserRecord> = emptyList(),
        val roleRequests: List<RoleRequestRecord> = emptyList(),
        val flaggedContent: List<FlaggedItem> = emptyList(),
        val selectedUserAttendance: List<UserAttendanceRow> = emptyList(),
        val attendanceUserId: String = "",
        val remoteLoading: Boolean = false
    )

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private var remoteLoaded = false

    fun init(profile: UserProfile) {
        viewModelScope.launch {
            val sectionKey = profile.sectionKey
            val collegeId = profile.collegeId
            val isAdmin = profile.role == UserRole.ADMIN || profile.role == UserRole.TEACHER ||
                profile.role == UserRole.CR

            _uiState.value = AdminUiState(
                isAdmin = isAdmin,
                profile = profile,
                sectionKey = sectionKey,
                collegeId = collegeId,
                users = _uiState.value.users,
                roleRequests = _uiState.value.roleRequests,
                flaggedContent = _uiState.value.flaggedContent,
                selectedUserAttendance = _uiState.value.selectedUserAttendance,
                attendanceUserId = _uiState.value.attendanceUserId
            )

            if (!remoteLoaded) {
                remoteLoaded = true
                launch { loadRemoteData() }
            }

            launch {
                repository.observeSubjects(sectionKey).collect { subjects ->
                    _uiState.update { it.copy(subjects = subjects, subjectCount = subjects.size) }
                }
            }
            launch {
                repository.observeTimetableSlots(sectionKey).collect { slots ->
                    _uiState.update { it.copy(timetableSlots = slots) }
                }
            }
            launch {
                repository.observeNotices(sectionKey).collect { notices ->
                    _uiState.update { it.copy(notices = notices, noticeCount = notices.size) }
                }
            }
            launch {
                repository.observeAssignments().collect { assignments ->
                    _uiState.update { it.copy(assignments = assignments, assignmentCount = assignments.size) }
                }
            }
            launch {
                repository.observeExams().collect { exams ->
                    _uiState.update { it.copy(exams = exams, examCount = exams.size) }
                }
            }
            launch {
                repository.observeSyllabusTopics().collect { topics ->
                    _uiState.update { it.copy(syllabusTopics = topics) }
                }
            }
            launch {
                repository.observeStudyResources(sectionKey).collect { resources ->
                    _uiState.update { it.copy(studyResources = resources) }
                }
            }
            launch {
                repository.observeClubs(collegeId).collect { clubs ->
                    _uiState.update { it.copy(clubs = clubs, clubCount = clubs.size) }
                }
            }
            launch {
                repository.observeEvents(collegeId).collect { events ->
                    _uiState.update { it.copy(events = events, eventCount = events.size) }
                }
            }
            launch {
                repository.observeLostFound(collegeId).collect { lostFound ->
                    _uiState.update { it.copy(lostFound = lostFound) }
                }
            }
            launch {
                repository.observeDirectory(collegeId).collect { directory ->
                    _uiState.update { it.copy(directory = directory) }
                }
            }
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

    // =========================================================================
    // REMOTE USERS / ROLE REQUESTS / MODERATION
    // =========================================================================
    fun refreshRemoteData() {
        viewModelScope.launch { loadRemoteData() }
    }

    private suspend fun loadRemoteData() {
        _uiState.update { it.copy(remoteLoading = true) }
        val users = runCatching { repository.loadAllUsers() }.getOrDefault(emptyList())
        val requests = runCatching { repository.loadRoleRequests() }.getOrDefault(emptyList())
        val flagged = runCatching { repository.loadFlaggedContent() }.getOrDefault(emptyList())
        _uiState.update {
            it.copy(
                users = users,
                roleRequests = requests,
                flaggedContent = flagged,
                remoteLoading = false
            )
        }
    }

    fun changeUserRole(uid: String, role: UserRole) {
        viewModelScope.launch {
            val ok = runCatching { repository.updateUserRole(uid, role) }.getOrDefault(false)
            if (ok) {
                _uiState.update { state ->
                    state.copy(users = state.users.map { if (it.uid == uid) it.copy(role = role) else it })
                }
                _messages.emit("Role updated to ${role.name}")
            } else {
                _messages.emit("Could not update role. Admin access is required.")
            }
        }
    }

    fun removeUser(uid: String) {
        viewModelScope.launch {
            val ok = runCatching { repository.removeUser(uid) }.getOrDefault(false)
            if (ok) {
                _uiState.update { state -> state.copy(users = state.users.filterNot { it.uid == uid }) }
                _messages.emit("User removed")
            } else {
                _messages.emit("Could not remove user")
            }
        }
    }

    fun loadUserAttendance(uid: String) {
        viewModelScope.launch {
            val rows = runCatching { repository.loadUserAttendance(uid) }.getOrDefault(emptyList())
            _uiState.update { it.copy(attendanceUserId = uid, selectedUserAttendance = rows) }
        }
    }

    fun approveRequest(request: RoleRequestRecord) {
        viewModelScope.launch {
            val reviewer = _uiState.value.profile.email
            val ok = runCatching {
                repository.approveRoleRequest(request.id, request.userId, request.requestedRole, reviewer)
            }.getOrDefault(false)
            if (ok) {
                _uiState.update { state ->
                    state.copy(
                        roleRequests = state.roleRequests.filterNot { it.id == request.id },
                        users = state.users.map {
                            if (it.uid == request.userId) it.copy(role = request.requestedRole) else it
                        }
                    )
                }
                _messages.emit("Approved ${request.email} as ${request.requestedRole.name}")
            } else {
                _messages.emit("Could not approve request")
            }
        }
    }

    fun rejectRequest(request: RoleRequestRecord) {
        viewModelScope.launch {
            val reviewer = _uiState.value.profile.email
            val ok = runCatching { repository.rejectRoleRequest(request.id, reviewer) }
                .getOrDefault(false)
            if (ok) {
                _uiState.update { state ->
                    state.copy(roleRequests = state.roleRequests.filterNot { it.id == request.id })
                }
                _messages.emit("Request rejected")
            } else {
                _messages.emit("Could not reject request")
            }
        }
    }

    fun moderateFlagged(item: FlaggedItem, hide: Boolean) {
        viewModelScope.launch {
            runCatching { repository.moderateFlagged(item, hide) }
            _uiState.update { state ->
                state.copy(
                    flaggedContent = state.flaggedContent.filterNot {
                        it.id == item.id && it.kind == item.kind
                    }
                )
            }
            _messages.emit(if (hide) "Content hidden" else "Content approved")
        }
    }

    // Context helper for PDF export (kept minimal)
    fun contextString(context: Context): String = context.packageName
}
