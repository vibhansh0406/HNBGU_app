package `in`.campusone.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.campusone.app.data.local.entity.AssignmentEntity
import `in`.campusone.app.data.local.entity.CampusEventEntity
import `in`.campusone.app.data.local.entity.CampusMapPlaceEntity
import `in`.campusone.app.data.local.entity.ClubEntity
import `in`.campusone.app.data.local.entity.DirectoryContactEntity
import `in`.campusone.app.data.local.entity.DirectorySuggestionEntity
import `in`.campusone.app.data.local.entity.ExamEntity
import `in`.campusone.app.data.local.entity.LostFoundContactRequestEntity
import `in`.campusone.app.data.local.entity.LostFoundEntity
import `in`.campusone.app.data.local.entity.StudyResourceEntity
import `in`.campusone.app.data.local.entity.SyllabusTopicEntity
import `in`.campusone.app.data.preferences.UserPreferencesRepository
import `in`.campusone.app.data.repository.StudyAndCampusRepository
import `in`.campusone.app.domain.model.EventRsvpStatus
import `in`.campusone.app.domain.model.UserProfile
import `in`.campusone.app.domain.model.UserRole
import `in`.campusone.app.domain.usecase.CourseGradeInput
import `in`.campusone.app.domain.usecase.GpaCalculationResult
import `in`.campusone.app.domain.usecase.GpaCalculator
import `in`.campusone.app.domain.usecase.GradingScaleType
import `in`.campusone.app.domain.usecase.TargetCgpaRequirement
import `in`.campusone.app.util.StudyFileAndPdfHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudyAndCampusUiState(
    val profile: UserProfile = UserProfile(),
    // Phase 2: Study
    val resources: List<StudyResourceEntity> = emptyList(),
    val moderationQueue: List<StudyResourceEntity> = emptyList(),
    val assignments: List<AssignmentEntity> = emptyList(),
    val nextThreeDeadlines: List<AssignmentEntity> = emptyList(),
    val exams: List<ExamEntity> = emptyList(),
    val nextUpcomingExam: ExamEntity? = null,
    val syllabusTopics: List<SyllabusTopicEntity> = emptyList(),
    val gpaScaleType: GradingScaleType = GradingScaleType.TEN_POINT_INDIAN,
    val gpaCourses: List<CourseGradeInput> = emptyList(),
    val gpaMultiplier: Double = 9.5,
    val gpaResult: GpaCalculationResult = GpaCalculator.calculateSgpaAndCgpa(currentSemesterCourses = emptyList()),
    val targetCgpaInput: Double = 8.5,
    val upcomingCreditsInput: Int = 20,
    val targetCgpaRequirement: TargetCgpaRequirement = GpaCalculator.calculateRequiredSgpaForTargetCgpa(0, 0.0, 20, 8.5),
    // Phase 3: Campus Life
    val clubs: List<ClubEntity> = emptyList(),
    val events: List<CampusEventEntity> = emptyList(),
    val lostFoundItems: List<LostFoundEntity> = emptyList(),
    val contactRequests: List<LostFoundContactRequestEntity> = emptyList(),
    val directoryContacts: List<DirectoryContactEntity> = emptyList(),
    val directorySuggestions: List<DirectorySuggestionEntity> = emptyList(),
    val campusMapPlaces: List<CampusMapPlaceEntity> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StudyAndCampusViewModel @Inject constructor(
    private val repository: StudyAndCampusRepository,
    private val prefs: UserPreferencesRepository
) : ViewModel() {

    private val _statusMessages = MutableSharedFlow<String>()
    val statusMessages = _statusMessages.asSharedFlow()

    private val _gpaScaleType = MutableStateFlow(GradingScaleType.TEN_POINT_INDIAN)
    private val _gpaCourses = MutableStateFlow(
        listOf(
            CourseGradeInput("c1", "SOET-CS-301", "Data Structures & Algorithms", 4, 9.0),
            CourseGradeInput("c2", "SOET-CS-302", "Operating Systems", 4, 8.0),
            CourseGradeInput("c3", "SOET-CS-303", "Database Management Systems", 4, 9.0),
            CourseGradeInput("c4", "SOS-MATH-301", "Discrete Mathematics (GE)", 4, 8.0),
            CourseGradeInput("c5", "SOET-SEC-301", "Android App Development (SEC)", 2, 10.0)
        )
    )
    private val _gpaMultiplier = MutableStateFlow(9.5)
    private val _targetCgpa = MutableStateFlow(8.75)
    private val _upcomingCredits = MutableStateFlow(20)

    val uiState: StateFlow<StudyAndCampusUiState> = prefs.userProfile.flatMapLatest { profile ->
        viewModelScope.launch {
            repository.ensureStudyAndCampusSeeded(profile.sectionKey, profile.collegeId)
        }
        val studyFlows = combine(
            repository.observeResources(profile.sectionKey),
            repository.observeModerationQueue(profile.sectionKey),
            repository.observeAssignments(),
            repository.observeExams(),
            repository.observeSyllabusTopics()
        ) { resources, modQueue, assignments, exams, syllabus ->
            Quintuple(resources, modQueue, assignments, exams, syllabus)
        }

        val campusFlows1 = combine(
            repository.observeSavedGpaSemesters(),
            repository.observeClubs(profile.collegeId),
            repository.observeEvents(profile.collegeId),
            repository.observeLostFound(profile.collegeId),
            repository.observeContactRequests()
        ) { savedSems, clubs, events, lostFound, contactReqs ->
            Quintuple(savedSems, clubs, events, lostFound, contactReqs)
        }

        val campusFlows2 = combine(
            repository.observeDirectory(profile.collegeId),
            repository.observeDirectorySuggestions(profile.collegeId),
            repository.observeCampusMapPlaces(profile.collegeId)
        ) { directory, suggestions, mapPlaces ->
            Triple(directory, suggestions, mapPlaces)
        }

        val gpaInputFlows = combine(
            _gpaScaleType,
            _gpaCourses,
            _gpaMultiplier,
            _targetCgpa,
            _upcomingCredits
        ) { scale, courses, mult, targetCgpa, upCredits ->
            Quintuple(scale, courses, mult, targetCgpa, upCredits)
        }

        combine(studyFlows, campusFlows1, campusFlows2, gpaInputFlows) { studyPart, campus1, campus2, gpaInputs ->
            val scale = gpaInputs.a
            val courses = gpaInputs.b
            val mult = gpaInputs.c
            val targetCgpa = gpaInputs.d
            val upCredits = gpaInputs.e

            val gpaResult = GpaCalculator.calculateSgpaAndCgpa(
                currentSemesterNumber = profile.semester,
                currentSemesterCourses = courses,
                pastSemesters = campus1.a,
                scaleType = scale,
                percentageMultiplier = mult
            )
            val targetReq = GpaCalculator.calculateRequiredSgpaForTargetCgpa(
                completedCredits = gpaResult.totalCredits,
                currentCgpa = gpaResult.cgpa,
                upcomingSemesterCredits = upCredits,
                targetCgpa = targetCgpa,
                scaleType = scale
            )

            val now = System.currentTimeMillis()
            val pendingDeadlines = studyPart.c
                .filter { !it.isCompleted && it.isAddedToPersonalList }
                .sortedBy { it.dueDateEpochMillis }
                .take(3)
            val nextExam = studyPart.d
                .filter { it.examDateEpochMillis >= now - 86_400_000L }
                .minByOrNull { it.examDateEpochMillis }

            StudyAndCampusUiState(
                profile = profile,
                resources = studyPart.a,
                moderationQueue = studyPart.b,
                assignments = studyPart.c,
                nextThreeDeadlines = pendingDeadlines,
                exams = studyPart.d,
                nextUpcomingExam = nextExam,
                syllabusTopics = studyPart.e,
                gpaScaleType = scale,
                gpaCourses = courses,
                gpaMultiplier = mult,
                gpaResult = gpaResult,
                targetCgpaInput = targetCgpa,
                upcomingCreditsInput = upCredits,
                targetCgpaRequirement = targetReq,
                clubs = campus1.b,
                events = campus1.c,
                lostFoundItems = campus1.d,
                contactRequests = campus1.e,
                directoryContacts = campus2.first,
                directorySuggestions = campus2.second,
                campusMapPlaces = campus2.third
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StudyAndCampusUiState()
    )

    fun saveLocalProfile(profile: UserProfile) {
        viewModelScope.launch {
            prefs.saveUserProfile(
                profile.copy(
                    uid = "local_demo_${System.currentTimeMillis()}",
                    verifiedRole = false
                ),
                markOnboarded = true
            )
            _statusMessages.emit("Demo profile saved on this device only.")
        }
    }

    // =========================================================================
    // Phase 2 Actions (Notes/PYQ, Assignments, Exams/Syllabus, GPA/PDF)
    // =========================================================================
    fun saveResourceLocally(
        subjectId: String,
        subjectName: String,
        semester: Int,
        title: String,
        category: String,
        examYear: Int,
        fileUrl: String,
        fileType: String,
        fileSizeBytes: Long,
        showUploaderName: Boolean
    ) {
        val p = uiState.value.profile
        viewModelScope.launch {
            val result = repository.saveStudyResourceLocally(
                sectionKey = p.sectionKey,
                subjectId = subjectId,
                subjectName = subjectName,
                semester = semester,
                title = title,
                category = category,
                examYear = examYear,
                fileUrl = fileUrl,
                fileType = fileType,
                fileSizeBytes = fileSizeBytes,
                uploaderUid = p.uid,
                uploaderName = p.name,
                showUploaderName = showUploaderName
            )
            if (result.isSuccess) {
                _statusMessages.emit("File saved on this device (local demo only; no remote upload).")
            } else {
                _statusMessages.emit(result.exceptionOrNull()?.message ?: "Could not save file")
            }
        }
    }

    fun upvoteResource(id: String) = viewModelScope.launch { repository.upvoteResource(id) }
    fun toggleBookmark(id: String, bookmarked: Boolean) =
        viewModelScope.launch { repository.toggleBookmarkOffline(id, bookmarked) }

    fun downloadResourceOffline(resource: StudyResourceEntity) {
        viewModelScope.launch {
            runCatching { repository.downloadResourceToLocalStorage(resource) }
                .onSuccess { path -> _statusMessages.emit("Saved locally: ${path.substringAfterLast('/')}") }
                .onFailure { error -> _statusMessages.emit("Could not save file: ${error.message}") }
        }
    }

    fun reportResource(id: String) {
        viewModelScope.launch {
            repository.reportResourceAbuse(id)
            _statusMessages.emit("Reported to local demo moderation queue.")
        }
    }

    fun crHideResource(id: String, hide: Boolean) =
        viewModelScope.launch { repository.moderateHideResource(id, hide) }

    fun crDeleteResource(id: String) =
        viewModelScope.launch { repository.moderateDeleteResource(id) }

    fun crDismissReport(id: String) =
        viewModelScope.launch { repository.moderateDismissReport(id) }

    fun saveAssignment(
        id: String? = null,
        subjectId: String,
        subjectTag: String,
        title: String,
        description: String,
        priority: String,
        dueOffsetDays: Int,
        dueDateFormatted: String,
        remind1Day: Boolean,
        remind2Hours: Boolean,
        postToSection: Boolean
    ) {
        val p = uiState.value.profile
        val dueMillis = System.currentTimeMillis() + (dueOffsetDays * 86_400_000L)
        viewModelScope.launch {
            repository.saveAssignment(
                id = id,
                sectionKey = p.sectionKey,
                subjectId = subjectId,
                subjectTag = subjectTag,
                title = title,
                description = description,
                priority = priority,
                dueDateEpochMillis = dueMillis,
                dueDateFormatted = dueDateFormatted,
                remind1Day = remind1Day,
                remind2Hours = remind2Hours,
                isSharedByCrOrTeacher = postToSection,
                authorName = if (postToSection) "${p.name} (${p.role.name})" else "Self"
            )
            _statusMessages.emit("Assignment saved with offline reminders!")
        }
    }

    fun addSharedAssignmentToMyList(id: String) {
        viewModelScope.launch {
            repository.addSharedAssignmentToPersonalList(id)
            _statusMessages.emit("Added shared section assignment to your personal list!")
        }
    }

    fun toggleAssignment(id: String, completed: Boolean) =
        viewModelScope.launch { repository.toggleAssignmentCompleted(id, completed) }

    fun deleteAssignment(id: String) =
        viewModelScope.launch { repository.deleteAssignment(id) }

    fun addExam(
        subjectCode: String,
        subjectName: String,
        examTitle: String,
        examDateIso: String,
        examTime: String,
        venue: String,
        daysFromNow: Int
    ) {
        val p = uiState.value.profile
        val isCrOrTeacher = p.role != UserRole.STUDENT
        viewModelScope.launch {
            repository.addOrPublishExam(
                sectionKey = p.sectionKey,
                subjectId = subjectCode,
                subjectName = subjectName,
                subjectCode = subjectCode,
                examTitle = examTitle,
                examDateIso = examDateIso,
                examTimeFormatted = examTime,
                venue = venue,
                daysFromNow = daysFromNow,
                publishedByCr = isCrOrTeacher
            )
            _statusMessages.emit("Exam added with countdown & reminder!")
        }
    }

    fun addSyllabusTopic(
        subjectId: String,
        subjectName: String,
        unitNumber: Int,
        unitTitle: String,
        topicTitle: String
    ) {
        val p = uiState.value.profile
        viewModelScope.launch {
            repository.addSyllabusTopic(
                sectionKey = p.sectionKey,
                subjectId = subjectId,
                subjectName = subjectName,
                unitNumber = unitNumber,
                unitTitle = unitTitle,
                topicTitle = topicTitle,
                publishedByCr = p.role != UserRole.STUDENT
            )
        }
    }

    fun toggleSyllabusTopic(id: String, completed: Boolean) =
        viewModelScope.launch { repository.toggleSyllabusTopic(id, completed) }

    fun setGradingScale(scale: GradingScaleType) { _gpaScaleType.value = scale }

    fun updateCourseGradePoint(index: Int, newGradePoint: Double) {
        val maxPt = _gpaScaleType.value.maxScalePoint
        val current = _gpaCourses.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(gradePoint = newGradePoint.coerceIn(0.0, maxPt))
            _gpaCourses.value = current
        }
    }

    fun addGpaCourse(code: String, name: String, credits: Int, gradePoint: Double) {
        val current = _gpaCourses.value.toMutableList()
        current.add(
            CourseGradeInput(
                id = "c_${System.currentTimeMillis()}",
                subjectCode = code.trim(),
                subjectName = name.trim(),
                credits = credits.coerceIn(1, 10),
                gradePoint = gradePoint
            )
        )
        _gpaCourses.value = current
    }

    fun updateTargetCgpa(target: Double, upcomingCredits: Int) {
        _targetCgpa.value = target
        _upcomingCredits.value = upcomingCredits.coerceAtLeast(1)
    }

    fun saveSemesterToHistory() {
        val s = uiState.value
        viewModelScope.launch {
            repository.saveCurrentSemesterGpaToHistory(
                semesterNumber = s.profile.semester,
                scaleName = s.gpaScaleType.name,
                courses = s.gpaCourses,
                sgpa = s.gpaResult.sgpa
            )
            _statusMessages.emit("Semester ${s.profile.semester} SGPA (${s.gpaResult.formattedSgpa}) saved to local history!")
        }
    }

    fun exportGpaPdf(context: Context) {
        val s = uiState.value
        viewModelScope.launch {
            val res = StudyFileAndPdfHelper.exportGpaReportToPdf(context, s.profile, s.gpaResult)
            if (res.isSuccess) {
                _statusMessages.emit("PDF exported: ${res.getOrNull()?.name}")
            } else {
                _statusMessages.emit("PDF export error: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    // =========================================================================
    // Phase 3 Actions (Clubs, 3-State Events RSVP, Privacy Lost & Found, Directory & Map)
    // =========================================================================
    fun toggleFollowClub(clubId: String, follow: Boolean) {
        viewModelScope.launch {
            repository.toggleFollowClub(clubId, follow)
            _statusMessages.emit(
                if (follow) "Following club in this local demo."
                else "Unfollowed club."
            )
        }
    }

    fun createOrProposeClub(
        name: String,
        logoEmoji: String,
        description: String,
        category: String,
        email: String,
        socialLink: String
    ) {
        val p = uiState.value.profile
        val isAutoApproved = p.role == UserRole.CR || p.role == UserRole.TEACHER
        viewModelScope.launch {
            val res = repository.createOrProposeClub(
                collegeId = p.collegeId,
                name = name,
                logoEmoji = logoEmoji,
                description = description,
                category = category,
                email = email,
                socialLink = socialLink,
                creatorUid = p.uid,
                creatorName = p.name,
                isAutoApproved = isAutoApproved
            )
            if (res.isSuccess) {
                _statusMessages.emit(
                    if (isAutoApproved) "Club saved locally in this demo."
                    else "Club saved as a local demo proposal."
                )
            } else {
                _statusMessages.emit(res.exceptionOrNull()?.message ?: "Could not create club")
            }
        }
    }

    fun updateClubPage(clubId: String, description: String, email: String, socialLink: String) {
        viewModelScope.launch {
            repository.updateClubPage(clubId, description, email, socialLink)
            _statusMessages.emit("Club page updated!")
        }
    }

    fun approveClub(clubId: String) {
        viewModelScope.launch {
            repository.approveClub(clubId)
            _statusMessages.emit("Club approved in the local demo.")
        }
    }

    fun updateEventRsvp(eventId: String, newStatus: EventRsvpStatus) {
        viewModelScope.launch {
            repository.updateEventRsvp(eventId, newStatus)
        }
    }

    fun createClubEvent(
        clubId: String,
        clubName: String,
        category: String,
        title: String,
        description: String,
        venue: String,
        dateFormatted: String,
        daysFromNow: Int
    ) {
        val p = uiState.value.profile
        val isAutoApproved = p.role == UserRole.CR || p.role == UserRole.TEACHER
        viewModelScope.launch {
            val res = repository.createClubEvent(
                collegeId = p.collegeId,
                clubId = clubId,
                clubName = clubName,
                category = category,
                title = title,
                description = description,
                venue = venue,
                dateFormatted = dateFormatted,
                daysFromNow = daysFromNow,
                isAutoApproved = isAutoApproved
            )
            if (res.isSuccess) {
                _statusMessages.emit(
                    if (isAutoApproved) "Event saved locally in this demo."
                    else "Event saved as a local demo proposal."
                )
            } else {
                _statusMessages.emit(res.exceptionOrNull()?.message ?: "Rate limit active")
            }
        }
    }

    fun approveEvent(eventId: String) {
        viewModelScope.launch {
            repository.approveEvent(eventId)
            _statusMessages.emit("Event approved in the local demo.")
        }
    }

    fun reportEvent(eventId: String) {
        viewModelScope.launch {
            repository.reportEvent(eventId)
            _statusMessages.emit("Event reported to CR / Faculty moderation queue.")
        }
    }

    fun addLostFound(
        type: String,
        category: String,
        title: String,
        desc: String,
        location: String,
        preferredContactMethod: String
    ) {
        val p = uiState.value.profile
        viewModelScope.launch {
            val res = repository.addLostFoundItem(
                collegeId = p.collegeId,
                type = type,
                category = category,
                itemTitle = title,
                description = desc,
                location = location,
                posterUid = p.uid,
                posterName = p.name,
                preferredContactMethod = preferredContactMethod
            )
            if (res.isSuccess) {
                _statusMessages.emit("Listing saved locally. Contact details are not shared with other students.")
            } else {
                _statusMessages.emit(res.exceptionOrNull()?.message ?: "Could not post item")
            }
        }
    }

    fun sendClaimRequest(item: LostFoundEntity, message: String) {
        val p = uiState.value.profile
        viewModelScope.launch {
            repository.sendLostFoundClaimRequest(
                item = item,
                requesterUid = p.uid.ifBlank { "uid_requester" },
                requesterName = p.name,
                message = message
            )
            _statusMessages.emit("Claim saved locally; it was not sent to ${item.posterName}.")
        }
    }

    fun respondToClaimRequest(request: LostFoundContactRequestEntity, accept: Boolean) {
        viewModelScope.launch {
            repository.respondToContactRequest(request, accept)
            _statusMessages.emit(
                if (accept) "Request accepted! Your chosen contact method is now shared with ${request.requesterName}."
                else "Request ignored. Your contact details remain private."
            )
        }
    }

    fun resolveLostFound(id: String) =
        viewModelScope.launch { repository.markLostFoundResolved(id) }

    fun reportLostFound(id: String) {
        viewModelScope.launch {
            repository.reportLostFound(id)
            _statusMessages.emit("Reported to local demo moderation queue.")
        }
    }

    fun hideLostFoundByCr(id: String) =
        viewModelScope.launch { repository.hideLostFoundByCr(id) }

    fun submitDirectoryCorrection(contactId: String, contactName: String, proposedChange: String) {
        val p = uiState.value.profile
        viewModelScope.launch {
            repository.submitDirectorySuggestion(
                collegeId = p.collegeId,
                contactId = contactId,
                contactName = contactName,
                suggesterUid = p.uid,
                suggesterName = p.name,
                proposedChange = proposedChange
            )
            _statusMessages.emit("Correction saved in the local demo queue.")
        }
    }

    fun resolveDirectorySuggestion(id: String, approve: Boolean) {
        viewModelScope.launch {
            repository.resolveDirectorySuggestion(id, approve)
            _statusMessages.emit(if (approve) "Suggestion approved!" else "Suggestion dismissed.")
        }
    }
}

private data class Quintuple<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
