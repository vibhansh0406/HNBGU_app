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
import `in`.campusone.app.data.remote.SupabaseSession
import `in`.campusone.app.data.remote.AuthManager
import `in`.campusone.app.data.repository.AdminIdentity
import `in`.campusone.app.data.repository.CoreCampusRepository
import `in`.campusone.app.data.repository.AdminPermission
import `in`.campusone.app.data.repository.AdminRepository
import `in`.campusone.app.data.repository.AdminUserRecord
import `in`.campusone.app.data.repository.AuditEntry
import `in`.campusone.app.data.repository.ContentReportRecord
import `in`.campusone.app.data.repository.ReportPreview
import `in`.campusone.app.data.repository.FlaggedItem
import `in`.campusone.app.data.repository.PlatformSettings
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
    val repository: AdminRepository,
    private val authManager: AuthManager,
    private val coreCampusRepository: CoreCampusRepository
) : ViewModel() {

    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()

    fun emitMessage(message: String) {
        viewModelScope.launch { _messages.emit(message) }
    }

    data class AdminUiState(
        val isAdmin: Boolean = false,
        val profile: UserProfile = UserProfile(),
        val sectionKey: String = "",
        val collegeId: String = "hnbgu",
        // Server-verified identity. Everything privileged is gated on this.
        val identity: AdminIdentity = AdminIdentity.Anonymous,
        val identityLoaded: Boolean = false,
        val sessionExpired: Boolean = false,
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
        val contentReports: List<ContentReportRecord> = emptyList(),
        val reportPreview: ReportPreview? = null,
        val auditLogs: List<AuditEntry> = emptyList(),
        val settings: PlatformSettings = PlatformSettings(),
        val selectedUserAttendance: List<UserAttendanceRow> = emptyList(),
        val attendanceUserId: String = "",
        val remoteLoading: Boolean = false,
        val remoteError: String? = null
    ) {
        fun can(permission: AdminPermission): Boolean = identity.can(permission)
    }

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private var remoteLoaded = false
    private var localCollectorsStarted = false
    private var lastSessionId: String? = null

    init {
        viewModelScope.launch {
            authManager.session.collect { session ->
                val sessionId = session?.accessToken?.take(24)
                if (session == null) {
                    lastSessionId = null
                    _uiState.update {
                        it.copy(
                            identity = AdminIdentity.Anonymous,
                            identityLoaded = false,
                            sessionExpired = false,
                            users = emptyList(),
                            roleRequests = emptyList(),
                            flaggedContent = emptyList(),
                            contentReports = emptyList(),
                            auditLogs = emptyList()
                        )
                    }
                    remoteLoaded = false
                    return@collect
                }
                if (sessionId != lastSessionId) {
                    lastSessionId = sessionId
                    remoteLoaded = false
                    refreshIdentity()
                }
                if (isSessionExpired(session)) {
                    _uiState.update { it.copy(sessionExpired = true) }
                }
            }
        }
    }

    private fun isSessionExpired(session: SupabaseSession): Boolean =
        session.expiresAt > 0L && System.currentTimeMillis() >= session.expiresAt

    fun init(profile: UserProfile) {
        val sectionKey = profile.sectionKey
        val collegeId = profile.collegeId
        val isAdmin = profile.role == UserRole.ADMIN || profile.role == UserRole.MODERATOR

        _uiState.update {
            it.copy(
                isAdmin = isAdmin,
                profile = profile,
                sectionKey = sectionKey,
                collegeId = collegeId
            )
        }

        if (!localCollectorsStarted) {
            localCollectorsStarted = true
            startLocalCollectors(sectionKey, collegeId)
        }

        viewModelScope.launch {
            refreshIdentity()
            val identity = _uiState.value.identity
            if (identity.isOwner || identity.permissions.isNotEmpty()) {
                loadRemoteData()
            }
        }
    }

    private fun startLocalCollectors(sectionKey: String, collegeId: String) {
        viewModelScope.launch {
            repository.observeSubjects(sectionKey).collect { subjects ->
                _uiState.update { it.copy(subjects = subjects, subjectCount = subjects.size) }
            }
        }
        viewModelScope.launch {
            repository.observeTimetableSlots(sectionKey).collect { slots ->
                _uiState.update { it.copy(timetableSlots = slots) }
            }
        }
        viewModelScope.launch {
            repository.observeNotices(sectionKey).collect { notices ->
                _uiState.update { it.copy(notices = notices, noticeCount = notices.size) }
            }
        }
        viewModelScope.launch {
            repository.observeAssignments().collect { assignments ->
                _uiState.update { it.copy(assignments = assignments, assignmentCount = assignments.size) }
            }
        }
        viewModelScope.launch {
            repository.observeExams().collect { exams ->
                _uiState.update { it.copy(exams = exams, examCount = exams.size) }
            }
        }
        viewModelScope.launch {
            repository.observeSyllabusTopics().collect { topics ->
                _uiState.update { it.copy(syllabusTopics = topics) }
            }
        }
        viewModelScope.launch {
            repository.observeStudyResources(sectionKey).collect { resources ->
                _uiState.update { it.copy(studyResources = resources) }
            }
        }
        viewModelScope.launch {
            repository.observeClubs(collegeId).collect { clubs ->
                _uiState.update { it.copy(clubs = clubs, clubCount = clubs.size) }
            }
        }
        viewModelScope.launch {
            repository.observeEvents(collegeId).collect { events ->
                _uiState.update { it.copy(events = events, eventCount = events.size) }
            }
        }
        viewModelScope.launch {
            repository.observeLostFound(collegeId).collect { lostFound ->
                _uiState.update { it.copy(lostFound = lostFound) }
            }
        }
        viewModelScope.launch {
            repository.observeDirectory(collegeId).collect { directory ->
                _uiState.update { it.copy(directory = directory) }
            }
        }
    }

    // =========================================================================
    // IDENTITY
    // =========================================================================
    fun refreshIdentity() {
        viewModelScope.launch {
            val session = authManager.currentSession()
            if (session == null) {
                _uiState.update { it.copy(identity = AdminIdentity.Anonymous, identityLoaded = true) }
                return@launch
            }
            if (isSessionExpired(session)) {
                // Try a refresh first; a revoked session fails here and the
                // privileged UI collapses to the anonymous state.
                val refreshed = runCatching { authManager.ensureFreshToken() }.getOrDefault(false)
                if (!refreshed) {
                    _uiState.update {
                        it.copy(
                            identity = AdminIdentity.Anonymous,
                            identityLoaded = true,
                            sessionExpired = true,
                            users = emptyList(),
                            roleRequests = emptyList(),
                            flaggedContent = emptyList(),
                            contentReports = emptyList(),
                            auditLogs = emptyList()
                        )
                    }
                    remoteLoaded = false
                    return@launch
                }
            }
            val identity = runCatching { repository.loadIdentity() }
                .getOrDefault(AdminIdentity.Anonymous)
            _uiState.update {
                it.copy(
                    identity = identity,
                    identityLoaded = true,
                    sessionExpired = false
                )
            }
            if (identity.permissions.isEmpty() && !identity.isOwner) {
                _uiState.update { it.copy(users = emptyList(), auditLogs = emptyList()) }
                return@launch
            }
            if (!remoteLoaded) {
                remoteLoaded = true
                loadRemoteData()
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
            val ok = repository.deleteSubjectRemote(subjectId)
            _messages.emit(if (ok) "Subject removed" else "Subject removed on this device, but cloud deletion failed. Check connection and retry.")
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

    fun publishTimetable(
        courseCode: String,
        courseName: String,
        semester: Int,
        section: String,
        academicYear: String,
        fileName: String,
        rows: List<Map<String, Any>>
    ) {
        viewModelScope.launch {
            val outcome = repository.publishTimetable(courseCode, courseName, semester, section, academicYear, fileName, rows)
            _messages.emit(outcome.message)
        }
    }

    fun loadTimetableImports() {
        viewModelScope.launch {
            val result = repository.listTimetableImports()
            if (result.isSuccess) {
                // TODO: expose in UI state
            } else {
                _messages.emit("Failed to load timetable history")
            }
        }
    }

    fun deleteTimetableImport(importId: String) {
        viewModelScope.launch {
            val outcome = repository.deleteTimetableImport(importId)
            val ok = repository.deleteTimetableImportRemote(importId)
            _messages.emit(if (ok) outcome.message else "Import removed on this device, but cloud deletion failed. Check connection and retry.")
        }
    }

    // =========================================================================
    // NOTICES
    // =========================================================================
    suspend fun uploadNoticeAttachment(contentUri: String, mimeType: String): Result<String> =
        coreCampusRepository.uploadNoticeAttachment(contentUri, mimeType)

    fun saveNotice(notice: NoticeEntity) {
        viewModelScope.launch {
            repository.upsertNotices(listOf(notice))
            _messages.emit("Notice published: ${notice.title}")
        }
    }

    fun saveNoticeRemote(notice: NoticeEntity, shouldNotify: Boolean = true) {
        viewModelScope.launch {
            val (synced, pushed) = repository.saveNoticeRemote(notice, shouldNotify)
            _messages.emit(
                when {
                    !synced -> "Notice could not be saved to cloud"
                    shouldNotify && pushed -> "Notice published and push notification sent"
                    shouldNotify -> "Notice saved, but push notification failed. Check FCM setup."
                    else -> "Notice changes synced to cloud"
                }
            )
        }
    }

    fun deleteNoticeRemote(noticeId: String) {
        viewModelScope.launch {
            // Optimistically remove from Room first so the notice disappears
            // immediately from the admin list; network deletion runs afterward.
            repository.deleteNotice(noticeId)
            val ok = repository.deleteNoticeRemote(noticeId)
            _messages.emit(
                if (ok) "Notice deleted"
                else "Notice removed on this device, but cloud deletion failed. Check connection and retry."
            )
        }
    }

    // =========================================================================
    // ASSIGNMENTS
    // =========================================================================
    fun saveAssignment(assignment: AssignmentEntity) {
        viewModelScope.launch {
            repository.upsertAssignments(listOf(assignment))
            val (synced, pushed) = repository.publishAssignmentRemote(assignment)
            _messages.emit(
                when {
                    !synced -> "Assignment saved locally, but cloud publishing failed"
                    pushed -> "Assignment published to its section and push notification sent"
                    else -> "Assignment published to its section; push not delivered (check FCM setup/devices)"
                }
            )
        }
    }

    fun deleteAssignment(id: String) {
        viewModelScope.launch {
            repository.deleteAssignment(id)
            val ok = repository.deleteAssignmentRemote(id)
            _messages.emit(if (ok) "Assignment deleted" else "Assignment removed on this device, but cloud deletion failed. Check connection and retry.")
        }
    }

    // =========================================================================
    // EXAMS
    // =========================================================================
    fun saveExam(exam: ExamEntity) {
        viewModelScope.launch {
            repository.upsertExams(listOf(exam))
            val (synced, pushed) = repository.publishExamRemote(exam)
            _messages.emit(
                when {
                    !synced -> "Exam saved locally, but cloud publishing failed"
                    pushed -> "Exam published to its section and push notification sent"
                    else -> "Exam published to its section; push not delivered (check FCM setup/devices)"
                }
            )
        }
    }

    fun deleteExam(id: String) {
        viewModelScope.launch {
            repository.deleteExam(id)
            val ok = repository.deleteExamRemote(id)
            _messages.emit(if (ok) "Exam deleted" else "Exam removed on this device, but cloud deletion failed. Check connection and retry.")
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

    fun deleteSyllabusTopic(id: String) {
        viewModelScope.launch {
            repository.deleteSyllabusTopic(id)
            val ok = repository.deleteSyllabusTopicRemote(id)
            _messages.emit(if (ok) "Syllabus topic removed" else "Topic removed on this device, but cloud deletion failed. Check connection and retry.")
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
            val ok = repository.deleteStudyResourceRemote(id)
            _messages.emit(if (ok) "Resource removed" else "Resource removed on this device, but cloud deletion failed. Check connection and retry.")
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

    fun deleteClub(clubId: String) {
        viewModelScope.launch {
            repository.deleteClub(clubId)
            val ok = repository.deleteClubRemote(clubId)
            _messages.emit(if (ok) "Club deleted" else "Club removed on this device, but cloud deletion failed. Check connection and retry.")
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
            val ok = repository.deleteEventRemote(eventId)
            _messages.emit(if (ok) "Event deleted" else "Event removed on this device, but cloud deletion failed. Check connection and retry.")
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

    fun deleteLostFound(id: String) {
        viewModelScope.launch {
            repository.deleteLostFound(id)
            val ok = repository.deleteLostFoundRemote(id)
            _messages.emit(
                if (ok) "Lost & Found item deleted"
                else "Item removed on this device, but cloud deletion failed. Check connection and retry."
            )
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

    fun deleteDirectoryContact(id: String) {
        viewModelScope.launch {
            repository.deleteDirectoryContacts(listOf(id))
            val ok = repository.deleteDirectoryContactRemote(id)
            _messages.emit(if (ok) "Directory contact deleted" else "Contact removed on this device, but cloud deletion failed. Check connection and retry.")
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
        viewModelScope.launch {
            refreshIdentity()
            loadRemoteData()
        }
    }

    private suspend fun loadRemoteData() {
        val identity = _uiState.value.identity
        if (!identity.isOwner && identity.permissions.isEmpty()) {
            _uiState.update { it.copy(remoteLoading = false, remoteError = "Admin access required") }
            return
        }
        _uiState.update { it.copy(remoteLoading = true, remoteError = null) }

        val users = if (identity.can(AdminPermission.VIEW_USERS)) {
            runCatching { repository.loadAllUsers() }.getOrElse { emptyList() }
        } else {
            emptyList()
        }
        val requests = if (identity.can(AdminPermission.MANAGE_ROLES)) {
            runCatching { repository.loadRoleRequests() }.getOrElse { emptyList() }
        } else {
            emptyList()
        }
        val flagged = if (identity.can(AdminPermission.MODERATE_REPORTS)) {
            runCatching { repository.loadFlaggedContent() }.getOrElse { emptyList() }
        } else {
            emptyList()
        }
        val reports = if (identity.can(AdminPermission.MODERATE_REPORTS)) {
            runCatching { repository.loadContentReports() }.getOrElse { emptyList() }
        } else {
            emptyList()
        }
        val logs = if (identity.can(AdminPermission.VIEW_AUDIT_LOGS)) {
            runCatching { repository.loadAuditLogs() }.getOrElse { emptyList() }
        } else {
            emptyList()
        }
        val settings = if (identity.can(AdminPermission.MANAGE_SYSTEM_SETTINGS)) {
            runCatching { repository.loadPlatformSettings() }.getOrDefault(PlatformSettings())
        } else {
            _uiState.value.settings
        }

        _uiState.update {
            it.copy(
                users = users,
                roleRequests = requests,
                flaggedContent = flagged,
                contentReports = reports,
                auditLogs = logs,
                settings = settings,
                remoteLoading = false
            )
        }
    }

    fun changeUserRole(uid: String, role: UserRole) {
        val identity = _uiState.value.identity
        if (!identity.can(AdminPermission.MANAGE_ROLES)) {
            viewModelScope.launch { _messages.emit("You do not have permission to change roles.") }
            return
        }
        val target = _uiState.value.users.firstOrNull { it.uid == uid }
        if (target != null && target.role == UserRole.ADMIN && !identity.isOwner) {
            viewModelScope.launch { _messages.emit("Only the platform owner can change an admin role.") }
            return
        }
        if (role == UserRole.ADMIN && !identity.isOwner) {
            viewModelScope.launch { _messages.emit("ADMIN can only be granted by the platform owner.") }
            return
        }
        viewModelScope.launch {
            val result = repository.updateUserRole(uid, role)
            if (result.success) {
                _uiState.update { state ->
                    state.copy(users = state.users.map { if (it.uid == uid) it.copy(role = role) else it })
                }
                _messages.emit("Role updated to ${role.name}")
                loadAuditLogs()
            } else {
                _messages.emit(result.message)
            }
        }
    }

    fun setUserSuspended(uid: String, suspended: Boolean) {
        viewModelScope.launch {
            val result = repository.setUserSuspended(uid, suspended)
            if (result.success) {
                _uiState.update { state ->
                    state.copy(users = state.users.map {
                        if (it.uid == uid) {
                            it.copy(
                                isSuspended = suspended,
                                role = if (suspended) UserRole.STUDENT else it.role
                            )
                        } else {
                            it
                        }
                    })
                }
                _messages.emit(if (suspended) "Account suspended" else "Account reactivated")
                loadAuditLogs()
            } else {
                _messages.emit(result.message)
            }
        }
    }

    fun removeUser(uid: String) {
        viewModelScope.launch {
            val result = repository.removeUser(uid)
            if (result.success) {
                _uiState.update { state -> state.copy(users = state.users.filterNot { it.uid == uid }) }
                _messages.emit("User removed")
                loadAuditLogs()
            } else {
                _messages.emit(result.message)
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
            val result = repository.approveRoleRequest(request)
            if (result.success) {
                _uiState.update { state ->
                    state.copy(
                        roleRequests = state.roleRequests.filterNot { it.id == request.id },
                        users = state.users.map {
                            if (it.uid == request.userId) it.copy(role = request.requestedRole) else it
                        }
                    )
                }
                _messages.emit("Approved ${request.email} as ${request.requestedRole.name}")
                loadAuditLogs()
            } else {
                _messages.emit(result.message)
            }
        }
    }

    fun rejectRequest(request: RoleRequestRecord) {
        viewModelScope.launch {
            val result = repository.rejectRoleRequest(request)
            if (result.success) {
                _uiState.update { state ->
                    state.copy(roleRequests = state.roleRequests.filterNot { it.id == request.id })
                }
                _messages.emit("Request rejected")
                loadAuditLogs()
            } else {
                _messages.emit(result.message)
            }
        }
    }

    fun moderateFlagged(item: FlaggedItem, action: String) {
        viewModelScope.launch {
            val result = repository.moderateFlagged(item, action)
            if (result.success) {
                _uiState.update { state ->
                    state.copy(
                        flaggedContent = state.flaggedContent.filterNot {
                            it.id == item.id && it.kind == item.kind
                        }
                    )
                }
                _messages.emit(moderationMessage(action))
                loadAuditLogs()
            } else {
                _messages.emit(result.message)
            }
        }
    }

    fun moderateReport(report: ContentReportRecord, action: String) {
        viewModelScope.launch {
            val result = repository.moderateReport(report, action)
            if (result.success) {
                _uiState.update { state ->
                    state.copy(
                        contentReports = state.contentReports.filterNot { it.id == report.id },
                        flaggedContent = state.flaggedContent.filterNot {
                            it.id == report.targetId
                        },
                        reportPreview = state.reportPreview?.takeUnless { it.reportId == report.id }
                    )
                }
                _messages.emit(moderationMessage(action))
                loadAuditLogs()
            } else {
                _messages.emit(result.message)
            }
        }
    }

    fun loadReportPreview(report: ContentReportRecord) {
        viewModelScope.launch {
            val preview = runCatching { repository.loadReportPreview(report) }.getOrNull()
            if (preview == null) {
                _messages.emit("Could not load this content preview.")
            } else {
                _uiState.update { it.copy(reportPreview = preview) }
            }
        }
    }

    fun clearReportPreview() {
        _uiState.update { it.copy(reportPreview = null) }
    }

    private fun moderationMessage(action: String): String = when (action) {
        "HIDE" -> "Content hidden"
        "RESTORE" -> "Content restored"
        "DELETE" -> "Content deleted"
        else -> "Report dismissed"
    }

    fun saveSettings(settings: PlatformSettings) {
        viewModelScope.launch {
            val result = repository.savePlatformSettings(settings)
            if (result.success) {
                _uiState.update { it.copy(settings = settings) }
                _messages.emit("Platform settings saved")
                loadAuditLogs()
            } else {
                _messages.emit(result.message)
            }
        }
    }

    fun recoverOwnerAccess() {
        viewModelScope.launch {
            val result = repository.recoverOwnerAccess()
            _messages.emit(result.message)
            if (result.success) refreshIdentity()
        }
    }

    private fun loadAuditLogs() {
        viewModelScope.launch {
            val logs = runCatching { repository.loadAuditLogs() }.getOrDefault(emptyList())
            _uiState.update { it.copy(auditLogs = logs) }
        }
    }

    // Context helper for PDF export (kept minimal)
    fun contextString(context: Context): String = context.packageName
}