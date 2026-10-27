package `in`.campusone.app.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import `in`.campusone.app.data.preferences.UserPreferencesRepository
import `in`.campusone.app.data.repository.StudyAndCampusRepository
import `in`.campusone.app.di.AppContainer
import `in`.campusone.app.domain.model.AssignmentItem
import `in`.campusone.app.domain.model.CourseGradeEntry
import `in`.campusone.app.domain.model.DirectoryContactItem
import `in`.campusone.app.domain.model.ExamItem
import `in`.campusone.app.domain.model.GradingScale
import `in`.campusone.app.domain.model.MaterialType
import `in`.campusone.app.domain.model.PriorityLevel
import `in`.campusone.app.domain.model.SemesterGradeSummary
import `in`.campusone.app.domain.model.StudyMaterialItem
import `in`.campusone.app.domain.model.SyllabusTopic
import `in`.campusone.app.domain.usecase.GpaCalculator
import `in`.campusone.app.util.BackupRestoreManager
import `in`.campusone.app.util.CampusIntentHelper
import `in`.campusone.app.util.CrashLogManager
import `in`.campusone.app.util.StudyFileAndPdfHelper
import `in`.campusone.app.worker.ClassReminderWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StudyAndCampusUiState(
    val assignments: List<AssignmentItem> = emptyList(),
    val exams: List<ExamItem> = emptyList(),
    val syllabusTopics: List<SyllabusTopic> = emptyList(),
    val courseGrades: List<CourseGradeEntry> = emptyList(),
    val semesterSummaries: List<SemesterGradeSummary> = emptyList(),
    val cumulativeCgpa: Double = 0.0,
    val gradingScale: GradingScale = GradingScale.SCALE_10,
    val studyMaterials: List<StudyMaterialItem> = emptyList(),
    val directoryContacts: List<DirectoryContactItem> = emptyList(),
    val statusBannerMessage: String? = null,
    val lastCrashLog: String? = null
)

class StudyAndCampusViewModel(
    private val studyRepository: StudyAndCampusRepository,
    private val userPreferences: UserPreferencesRepository,
    private val backupRestoreManager: BackupRestoreManager
) : ViewModel() {

    private val statusBanner = MutableStateFlow<String?>(null)
    private val crashLogFlow = MutableStateFlow<String?>(null)

    private val combinedStudyFlow = combine(
        studyRepository.observeAssignments(),
        studyRepository.observeExams(),
        studyRepository.observeSyllabusTopics(),
        studyRepository.observeCourseGrades(),
        studyRepository.observeStudyMaterials()
    ) { assignments, exams, syllabus, grades, materials ->
        StudyDataTuple(assignments, exams, syllabus, grades, materials)
    }

    val uiState: StateFlow<StudyAndCampusUiState> = combine(
        combinedStudyFlow,
        studyRepository.observeDirectoryContacts(),
        userPreferences.userProfileFlow,
        statusBanner,
        crashLogFlow
    ) { studyTuple, directory, profile, banner, crashLog ->
        val summaries = GpaCalculator.summarizeBySemester(studyTuple.grades, profile.gradingScale)
        val cgpa = GpaCalculator.calculateCgpa(summaries)
        StudyAndCampusUiState(
            assignments = studyTuple.assignments,
            exams = studyTuple.exams,
            syllabusTopics = studyTuple.syllabus,
            courseGrades = studyTuple.grades,
            semesterSummaries = summaries,
            cumulativeCgpa = cgpa,
            gradingScale = profile.gradingScale,
            studyMaterials = studyTuple.materials,
            directoryContacts = directory,
            statusBannerMessage = banner,
            lastCrashLog = crashLog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StudyAndCampusUiState()
    )

    init {
        viewModelScope.launch {
            studyRepository.ensureStudyAndDirectorySeed()
        }
    }

    fun clearBanner() {
        statusBanner.value = null
    }

    fun refreshCrashLog(context: Context) {
        crashLogFlow.value = CrashLogManager.readLastCrashLog(context)
    }

    fun clearCrashLog(context: Context) {
        CrashLogManager.clearCrashLog(context)
        crashLogFlow.value = null
        statusBanner.value = "Crash log cleared."
    }

    fun addAssignment(
        context: Context,
        title: String,
        subjectName: String,
        dueInDays: Int,
        priority: PriorityLevel,
        notes: String
    ) {
        viewModelScope.launch {
            val dueMillis = System.currentTimeMillis() + (dueInDays.toLong() * 86_400_000L)
            studyRepository.addAssignment(title, subjectName, dueMillis, priority, notes)
            ClassReminderWorker.scheduleOneTimeReminder(
                context = context,
                uniqueWorkName = "asg_rem_${title.hashCode()}",
                title = "Assignment Due: $title",
                message = "$subjectName • Priority: ${priority.name}",
                delayMillis = (dueMillis - System.currentTimeMillis() - 7_200_000L).coerceAtLeast(60_000L)
            )
            statusBanner.value = "Assignment saved with offline reminder."
        }
    }

    fun toggleAssignment(id: String, completed: Boolean) {
        viewModelScope.launch {
            studyRepository.toggleAssignmentCompleted(id, completed)
        }
    }

    fun deleteAssignment(id: String) {
        viewModelScope.launch {
            studyRepository.deleteAssignment(id)
        }
    }

    fun addExam(
        subjectName: String,
        subjectCode: String,
        examTitle: String,
        daysFromNow: Int,
        timeSlot: String,
        venue: String,
        notes: String
    ) {
        viewModelScope.launch {
            val epochMillis = System.currentTimeMillis() + (daysFromNow.toLong() * 86_400_000L)
            studyRepository.addExam(subjectName, subjectCode, examTitle, epochMillis, timeSlot, venue, notes)
            statusBanner.value = "Exam added to countdown calendar."
        }
    }

    fun deleteExam(id: String) {
        viewModelScope.launch {
            studyRepository.deleteExam(id)
        }
    }

    fun addSyllabusTopic(subjectName: String, unitNumber: Int, topicTitle: String) {
        viewModelScope.launch {
            studyRepository.addSyllabusTopic(subjectName, unitNumber, topicTitle)
        }
    }

    fun toggleSyllabusTopic(id: String, completed: Boolean) {
        viewModelScope.launch {
            studyRepository.toggleSyllabusTopic(id, completed)
        }
    }

    fun setGradingScale(scale: GradingScale) {
        viewModelScope.launch {
            userPreferences.updateGradingScale(scale)
        }
    }

    fun addCourseGrade(semester: Int, subjectName: String, credits: Int, gradePointOrMarks: Double) {
        viewModelScope.launch {
            studyRepository.addCourseGrade(semester, subjectName, credits, gradePointOrMarks)
        }
    }

    fun deleteCourseGrade(id: String) {
        viewModelScope.launch {
            studyRepository.deleteCourseGrade(id)
        }
    }

    fun exportGpaPdf(context: Context) {
        viewModelScope.launch {
            val profile = userPreferences.getProfileOnce()
            val state = uiState.value
            val result = StudyFileAndPdfHelper.exportGpaReportPdf(
                context = context,
                profile = profile,
                summaries = state.semesterSummaries,
                cgpa = state.cumulativeCgpa
            )
            result.onSuccess { file ->
                statusBanner.value = "PDF Report saved: ${file.name}"
                CampusIntentHelper.shareFileViaProvider(context, file, "application/pdf")
            }.onFailure { err ->
                statusBanner.value = "PDF export failed: ${err.message}"
            }
        }
    }

    fun importStudyMaterialFromUri(
        context: Context,
        uri: Uri,
        subjectName: String,
        semester: Int,
        type: MaterialType
    ) {
        viewModelScope.launch {
            val imported = StudyFileAndPdfHelper.importFromSafUri(context, uri)
            imported.onSuccess { file ->
                studyRepository.addImportedStudyMaterial(
                    title = file.displayName,
                    subjectName = subjectName.ifBlank { "General" },
                    semester = semester,
                    type = type,
                    year = 2025,
                    localFilePath = file.localPath,
                    mimeType = file.mimeType,
                    fileSizeBytes = file.sizeBytes
                )
                statusBanner.value = "Imported '${file.displayName}' into offline Study Vault."
            }.onFailure { err ->
                statusBanner.value = "Import error: ${err.message}"
            }
        }
    }

    fun toggleBookmarkMaterial(id: String, bookmarked: Boolean) {
        viewModelScope.launch {
            studyRepository.toggleBookmarkMaterial(id, bookmarked)
        }
    }

    fun deleteStudyMaterial(id: String) {
        viewModelScope.launch {
            studyRepository.deleteStudyMaterial(id)
        }
    }

    fun exportBackupToUri(targetUri: Uri) {
        viewModelScope.launch {
            backupRestoreManager.exportToUri(targetUri)
                .onSuccess { count ->
                    statusBanner.value = "Backup exported ($count records saved to JSON)."
                }
                .onFailure { err ->
                    statusBanner.value = "Backup export failed: ${err.message}"
                }
        }
    }

    fun importBackupFromUri(sourceUri: Uri) {
        viewModelScope.launch {
            backupRestoreManager.importFromUri(sourceUri)
                .onSuccess { count ->
                    statusBanner.value = "Backup restored ($count records imported)."
                }
                .onFailure { err ->
                    statusBanner.value = "Restore failed: ${err.message}"
                }
        }
    }

    private data class StudyDataTuple(
        val assignments: List<AssignmentItem>,
        val exams: List<ExamItem>,
        val syllabus: List<SyllabusTopic>,
        val grades: List<CourseGradeEntry>,
        val materials: List<StudyMaterialItem>
    )

    companion object {
        fun provideFactory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StudyAndCampusViewModel(
                        studyRepository = container.studyAndCampusRepository,
                        userPreferences = container.userPreferencesRepository,
                        backupRestoreManager = container.backupRestoreManager
                    ) as T
                }
            }
    }
}
