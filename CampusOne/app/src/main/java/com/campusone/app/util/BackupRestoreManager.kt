package com.campusone.app.util

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.campusone.app.data.local.CampusOneDatabase
import com.campusone.app.data.local.entity.AssignmentEntity
import com.campusone.app.data.local.entity.AttendanceRecordEntity
import com.campusone.app.data.local.entity.CourseGradeEntity
import com.campusone.app.data.local.entity.ExamEntity
import com.campusone.app.data.local.entity.PersonalNoticeEntity
import com.campusone.app.data.local.entity.StudyMaterialEntity
import com.campusone.app.data.local.entity.SubjectEntity
import com.campusone.app.data.local.entity.SyllabusTopicEntity
import com.campusone.app.data.local.entity.TimetableSlotEntity
import com.campusone.app.data.preferences.UserPreferencesRepository
import com.campusone.app.domain.model.AssignmentItem
import com.campusone.app.domain.model.AttendanceRecordItem
import com.campusone.app.domain.model.AttendanceStatus
import com.campusone.app.domain.model.CourseGradeEntry
import com.campusone.app.domain.model.ExamItem
import com.campusone.app.domain.model.MaterialType
import com.campusone.app.domain.model.PersonalNotice
import com.campusone.app.domain.model.PriorityLevel
import com.campusone.app.domain.model.StudyMaterialItem
import com.campusone.app.domain.model.SyllabusTopic
import com.campusone.app.domain.model.TimetableSlot
import com.campusone.app.domain.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupSubjectRecord(
    val id: String,
    val code: String,
    val name: String,
    val professor: String,
    val semester: Int,
    val attendedCount: Int,
    val totalClassesCount: Int,
    val cancelledCount: Int,
    val updatedAtMillis: Long
)

@Serializable
data class CampusBackupPayload(
    val schemaVersion: Int = 2,
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val userProfile: UserProfile,
    val subjects: List<BackupSubjectRecord> = emptyList(),
    val attendanceRecords: List<AttendanceRecordItem> = emptyList(),
    val timetableSlots: List<TimetableSlot> = emptyList(),
    val personalNotices: List<PersonalNotice> = emptyList(),
    val assignments: List<AssignmentItem> = emptyList(),
    val exams: List<ExamItem> = emptyList(),
    val syllabusTopics: List<SyllabusTopic> = emptyList(),
    val courseGrades: List<CourseGradeEntry> = emptyList(),
    val studyMaterials: List<StudyMaterialItem> = emptyList()
)

class BackupRestoreManager(
    private val context: Context,
    private val database: CampusOneDatabase,
    private val userPreferences: UserPreferencesRepository
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun exportToUri(targetUri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val coreDao = database.coreDao()
            val studyDao = database.studyAndCampusDao()
            val profile = userPreferences.getProfileOnce()

            val subjects = coreDao.getAllSubjectsOnce().map {
                BackupSubjectRecord(
                    id = it.id,
                    code = it.code,
                    name = it.name,
                    professor = it.professor,
                    semester = it.semester,
                    attendedCount = it.attendedCount,
                    totalClassesCount = it.totalClassesCount,
                    cancelledCount = it.cancelledCount,
                    updatedAtMillis = it.updatedAtMillis
                )
            }
            val records = coreDao.getAllAttendanceRecordsOnce().map {
                AttendanceRecordItem(
                    id = it.id,
                    subjectId = it.subjectId,
                    subjectName = it.subjectName,
                    epochDay = it.epochDay,
                    dateLabel = it.dateLabel,
                    status = runCatching { AttendanceStatus.valueOf(it.status) }.getOrDefault(AttendanceStatus.PRESENT),
                    timestampMillis = it.timestampMillis
                )
            }
            val slots = coreDao.getAllTimetableSlotsOnce().map {
                TimetableSlot(
                    id = it.id,
                    subjectId = it.subjectId,
                    subjectName = it.subjectName,
                    subjectCode = it.subjectCode,
                    dayOfWeek = it.dayOfWeek,
                    startTime = it.startTime,
                    endTime = it.endTime,
                    room = it.room,
                    teacherName = it.teacherName,
                    semester = it.semester,
                    section = it.section
                )
            }
            val notices = coreDao.getAllPersonalNoticesOnce().map {
                PersonalNotice(
                    id = it.id,
                    title = it.title,
                    body = it.body,
                    category = it.category,
                    isPinned = it.isPinned,
                    isRead = it.isRead,
                    createdAtMillis = it.createdAtMillis
                )
            }
            val assignments = studyDao.getAllAssignmentsOnce().map {
                AssignmentItem(
                    id = it.id,
                    title = it.title,
                    subjectName = it.subjectName,
                    dueEpochMillis = it.dueEpochMillis,
                    priority = runCatching { PriorityLevel.valueOf(it.priority) }.getOrDefault(PriorityLevel.MEDIUM),
                    notes = it.notes,
                    isCompleted = it.isCompleted,
                    updatedAtMillis = it.updatedAtMillis
                )
            }
            val exams = studyDao.getAllExamsOnce().map {
                ExamItem(
                    id = it.id,
                    subjectName = it.subjectName,
                    subjectCode = it.subjectCode,
                    examTitle = it.examTitle,
                    examEpochMillis = it.examEpochMillis,
                    timeSlot = it.timeSlot,
                    venue = it.venue,
                    notes = it.notes
                )
            }
            val syllabus = studyDao.getAllSyllabusTopicsOnce().map {
                SyllabusTopic(
                    id = it.id,
                    subjectName = it.subjectName,
                    unitNumber = it.unitNumber,
                    topicTitle = it.topicTitle,
                    isCompleted = it.isCompleted,
                    updatedAtMillis = it.updatedAtMillis
                )
            }
            val grades = studyDao.getAllCourseGradesOnce().map {
                CourseGradeEntry(
                    id = it.id,
                    semester = it.semester,
                    subjectName = it.subjectName,
                    credits = it.credits,
                    gradePointOrMarks = it.gradePointOrMarks
                )
            }
            val materials = studyDao.getAllStudyMaterialsOnce().map {
                StudyMaterialItem(
                    id = it.id,
                    title = it.title,
                    subjectName = it.subjectName,
                    semester = it.semester,
                    type = runCatching { MaterialType.valueOf(it.type) }.getOrDefault(MaterialType.NOTES),
                    year = it.year,
                    localFilePath = it.localFilePath,
                    mimeType = it.mimeType,
                    fileSizeBytes = it.fileSizeBytes,
                    isBookmarked = it.isBookmarked,
                    upvotes = it.upvotes,
                    addedAtMillis = it.addedAtMillis
                )
            }

            val payload = CampusBackupPayload(
                userProfile = profile,
                subjects = subjects,
                attendanceRecords = records,
                timetableSlots = slots,
                personalNotices = notices,
                assignments = assignments,
                exams = exams,
                syllabusTopics = syllabus,
                courseGrades = grades,
                studyMaterials = materials
            )

            val serialized = json.encodeToString(payload)
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                out.write(serialized.toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException("Could not open backup destination URI.")

            subjects.size + assignments.size + exams.size + syllabus.size + grades.size
        }
    }

    suspend fun importFromUri(sourceUri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val rawJson = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            } ?: throw IllegalStateException("Could not read selected backup file.")

            val payload = json.decodeFromString<CampusBackupPayload>(rawJson)
            userPreferences.restoreProfile(payload.userProfile)

            database.withTransaction {
                val coreDao = database.coreDao()
                val studyDao = database.studyAndCampusDao()

                coreDao.upsertSubjects(payload.subjects.map {
                    SubjectEntity(
                        id = it.id,
                        code = it.code,
                        name = it.name,
                        professor = it.professor,
                        semester = it.semester,
                        attendedCount = it.attendedCount,
                        totalClassesCount = it.totalClassesCount,
                        cancelledCount = it.cancelledCount,
                        updatedAtMillis = it.updatedAtMillis
                    )
                })
                coreDao.insertAttendanceRecords(payload.attendanceRecords.map {
                    AttendanceRecordEntity(
                        id = it.id,
                        subjectId = it.subjectId,
                        subjectName = it.subjectName,
                        epochDay = it.epochDay,
                        dateLabel = it.dateLabel,
                        status = it.status.name,
                        timestampMillis = it.timestampMillis
                    )
                })
                coreDao.upsertTimetableSlots(payload.timetableSlots.map {
                    TimetableSlotEntity(
                        id = it.id,
                        subjectId = it.subjectId,
                        subjectName = it.subjectName,
                        subjectCode = it.subjectCode,
                        dayOfWeek = it.dayOfWeek,
                        startTime = it.startTime,
                        endTime = it.endTime,
                        room = it.room,
                        teacherName = it.teacherName,
                        semester = it.semester,
                        section = it.section
                    )
                })
                coreDao.upsertPersonalNotices(payload.personalNotices.map {
                    PersonalNoticeEntity(
                        id = it.id,
                        title = it.title,
                        body = it.body,
                        category = it.category,
                        isPinned = it.isPinned,
                        isRead = it.isRead,
                        createdAtMillis = it.createdAtMillis
                    )
                })
                studyDao.upsertAssignments(payload.assignments.map {
                    AssignmentEntity(
                        id = it.id,
                        title = it.title,
                        subjectName = it.subjectName,
                        dueEpochMillis = it.dueEpochMillis,
                        priority = it.priority.name,
                        notes = it.notes,
                        isCompleted = it.isCompleted,
                        updatedAtMillis = it.updatedAtMillis
                    )
                })
                studyDao.upsertExams(payload.exams.map {
                    ExamEntity(
                        id = it.id,
                        subjectName = it.subjectName,
                        subjectCode = it.subjectCode,
                        examTitle = it.examTitle,
                        examEpochMillis = it.examEpochMillis,
                        timeSlot = it.timeSlot,
                        venue = it.venue,
                        notes = it.notes
                    )
                })
                studyDao.upsertSyllabusTopics(payload.syllabusTopics.map {
                    SyllabusTopicEntity(
                        id = it.id,
                        subjectName = it.subjectName,
                        unitNumber = it.unitNumber,
                        topicTitle = it.topicTitle,
                        isCompleted = it.isCompleted,
                        updatedAtMillis = it.updatedAtMillis
                    )
                })
                studyDao.upsertCourseGrades(payload.courseGrades.map {
                    CourseGradeEntity(
                        id = it.id,
                        semester = it.semester,
                        subjectName = it.subjectName,
                        credits = it.credits,
                        gradePointOrMarks = it.gradePointOrMarks
                    )
                })
                studyDao.upsertStudyMaterials(payload.studyMaterials.map {
                    StudyMaterialEntity(
                        id = it.id,
                        title = it.title,
                        subjectName = it.subjectName,
                        semester = it.semester,
                        type = it.type.name,
                        year = it.year,
                        localFilePath = it.localFilePath,
                        mimeType = it.mimeType,
                        fileSizeBytes = it.fileSizeBytes,
                        isBookmarked = it.isBookmarked,
                        upvotes = it.upvotes,
                        addedAtMillis = it.addedAtMillis
                    )
                })
            }

            payload.subjects.size + payload.assignments.size + payload.exams.size +
                payload.syllabusTopics.size + payload.courseGrades.size
        }
    }
}
