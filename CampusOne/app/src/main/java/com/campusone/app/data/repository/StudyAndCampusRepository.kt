package com.campusone.app.data.repository

import android.content.Context
import com.campusone.app.data.local.dao.StudyAndCampusDao
import com.campusone.app.data.local.entity.AssignmentEntity
import com.campusone.app.data.local.entity.CourseGradeEntity
import com.campusone.app.data.local.entity.DirectoryContactEntity
import com.campusone.app.data.local.entity.ExamEntity
import com.campusone.app.data.local.entity.StudyMaterialEntity
import com.campusone.app.data.local.entity.SyllabusTopicEntity
import com.campusone.app.domain.model.AssignmentItem
import com.campusone.app.domain.model.CourseGradeEntry
import com.campusone.app.domain.model.DirectoryContactItem
import com.campusone.app.domain.model.ExamItem
import com.campusone.app.domain.model.MaterialType
import com.campusone.app.domain.model.PriorityLevel
import com.campusone.app.domain.model.StudyMaterialItem
import com.campusone.app.domain.model.SyllabusTopic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
private data class BundledDirectoryAsset(
    val university: String = "",
    val contacts: List<DirectoryContactItem> = emptyList()
)

interface StudyAndCampusRepository {
    fun observeAssignments(): Flow<List<AssignmentItem>>
    fun observeExams(): Flow<List<ExamItem>>
    fun observeSyllabusTopics(): Flow<List<SyllabusTopic>>
    fun observeCourseGrades(): Flow<List<CourseGradeEntry>>
    fun observeStudyMaterials(): Flow<List<StudyMaterialItem>>
    fun observeDirectoryContacts(): Flow<List<DirectoryContactItem>>

    suspend fun ensureStudyAndDirectorySeed()
    suspend fun addAssignment(title: String, subjectName: String, dueEpochMillis: Long, priority: PriorityLevel, notes: String)
    suspend fun toggleAssignmentCompleted(id: String, completed: Boolean)
    suspend fun deleteAssignment(id: String)

    suspend fun addExam(subjectName: String, subjectCode: String, examTitle: String, examEpochMillis: Long, timeSlot: String, venue: String, notes: String)
    suspend fun deleteExam(id: String)

    suspend fun addSyllabusTopic(subjectName: String, unitNumber: Int, topicTitle: String)
    suspend fun toggleSyllabusTopic(id: String, completed: Boolean)
    suspend fun deleteSyllabusTopic(id: String)

    suspend fun addCourseGrade(semester: Int, subjectName: String, credits: Int, gradePointOrMarks: Double)
    suspend fun deleteCourseGrade(id: String)

    suspend fun addImportedStudyMaterial(
        title: String,
        subjectName: String,
        semester: Int,
        type: MaterialType,
        year: Int,
        localFilePath: String,
        mimeType: String,
        fileSizeBytes: Long
    )
    suspend fun toggleBookmarkMaterial(id: String, bookmarked: Boolean)
    suspend fun deleteStudyMaterial(id: String)
}

class OfflineStudyAndCampusRepository(
    private val appContext: Context,
    private val dao: StudyAndCampusDao
) : StudyAndCampusRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun observeAssignments(): Flow<List<AssignmentItem>> =
        dao.observeAssignments().map { list ->
            list.map { a ->
                AssignmentItem(
                    id = a.id,
                    title = a.title,
                    subjectName = a.subjectName,
                    dueEpochMillis = a.dueEpochMillis,
                    priority = runCatching { PriorityLevel.valueOf(a.priority) }.getOrDefault(PriorityLevel.MEDIUM),
                    notes = a.notes,
                    isCompleted = a.isCompleted,
                    updatedAtMillis = a.updatedAtMillis
                )
            }
        }

    override fun observeExams(): Flow<List<ExamItem>> =
        dao.observeExams().map { list ->
            list.map { e ->
                ExamItem(
                    id = e.id,
                    subjectName = e.subjectName,
                    subjectCode = e.subjectCode,
                    examTitle = e.examTitle,
                    examEpochMillis = e.examEpochMillis,
                    timeSlot = e.timeSlot,
                    venue = e.venue,
                    notes = e.notes
                )
            }
        }

    override fun observeSyllabusTopics(): Flow<List<SyllabusTopic>> =
        dao.observeSyllabusTopics().map { list ->
            list.map { t ->
                SyllabusTopic(
                    id = t.id,
                    subjectName = t.subjectName,
                    unitNumber = t.unitNumber,
                    topicTitle = t.topicTitle,
                    isCompleted = t.isCompleted,
                    updatedAtMillis = t.updatedAtMillis
                )
            }
        }

    override fun observeCourseGrades(): Flow<List<CourseGradeEntry>> =
        dao.observeCourseGrades().map { list ->
            list.map { g ->
                CourseGradeEntry(
                    id = g.id,
                    semester = g.semester,
                    subjectName = g.subjectName,
                    credits = g.credits,
                    gradePointOrMarks = g.gradePointOrMarks
                )
            }
        }

    override fun observeStudyMaterials(): Flow<List<StudyMaterialItem>> =
        dao.observeStudyMaterials().map { list ->
            list.map { m ->
                StudyMaterialItem(
                    id = m.id,
                    title = m.title,
                    subjectName = m.subjectName,
                    semester = m.semester,
                    type = runCatching { MaterialType.valueOf(m.type) }.getOrDefault(MaterialType.NOTES),
                    year = m.year,
                    localFilePath = m.localFilePath,
                    mimeType = m.mimeType,
                    fileSizeBytes = m.fileSizeBytes,
                    isBookmarked = m.isBookmarked,
                    upvotes = m.upvotes,
                    addedAtMillis = m.addedAtMillis
                )
            }
        }

    override fun observeDirectoryContacts(): Flow<List<DirectoryContactItem>> =
        dao.observeDirectoryContacts().map { list ->
            list.map { c ->
                DirectoryContactItem(
                    id = c.id,
                    name = c.name,
                    designation = c.designation,
                    department = c.department,
                    category = c.category,
                    phone = c.phone,
                    email = c.email,
                    officeLocation = c.officeLocation,
                    officeHours = c.officeHours,
                    latitude = c.latitude,
                    longitude = c.longitude
                )
            }
        }

    override suspend fun ensureStudyAndDirectorySeed() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L

        if (dao.getAllAssignmentsOnce().isEmpty()) {
            dao.upsertAssignments(
                listOf(
                    AssignmentEntity("asg_1", "Implement Dijkstra & Bellman-Ford Lab Record", "Design & Analysis of Algorithms", now + 2 * dayMs, PriorityLevel.HIGH.name, "Submit PDF + C++/Kotlin source in Lab 2", false, now),
                    AssignmentEntity("asg_2", "BCNF & 3NF Normalization Problem Sheet", "Database Management Systems", now + 4 * dayMs, PriorityLevel.MEDIUM.name, "Solve Q1-Q8 from Korth Ch. 7", false, now),
                    AssignmentEntity("asg_3", "Linux Kernel Process Scheduling Report", "Operating Systems", now + 6 * dayMs, PriorityLevel.LOW.name, "Compare CFS vs Round Robin", false, now)
                )
            )
        }

        if (dao.getAllExamsOnce().isEmpty()) {
            dao.upsertExams(
                listOf(
                    ExamEntity("ex_1", "Design & Analysis of Algorithms", "CSE-401", "End-Sem Theory Exam", now + 14 * dayMs, "10:00 AM - 01:00 PM", "Chauras Examination Block Hall A", "Bring university ID & non-programmable calculator"),
                    ExamEntity("ex_2", "Database Management Systems", "CSE-402", "End-Sem Theory Exam", now + 17 * dayMs, "10:00 AM - 01:00 PM", "Chauras Examination Block Hall B", "SQL & Normalization units")
                )
            )
        }

        if (dao.getAllSyllabusTopicsOnce().isEmpty()) {
            dao.upsertSyllabusTopics(
                listOf(
                    SyllabusTopicEntity("syl_1", "Design & Analysis of Algorithms", 1, "Asymptotic Notation, Recurrences & Master Theorem", true, now),
                    SyllabusTopicEntity("syl_2", "Design & Analysis of Algorithms", 2, "Divide & Conquer: Merge Sort, Quick Sort, Strassen Matrix", true, now),
                    SyllabusTopicEntity("syl_3", "Design & Analysis of Algorithms", 3, "Greedy & Dynamic Programming: Knapsack, LCS, Floyd-Warshall", false, now),
                    SyllabusTopicEntity("syl_4", "Database Management Systems", 1, "ER Modeling & Relational Algebra Calculus", true, now),
                    SyllabusTopicEntity("syl_5", "Database Management Systems", 2, "Functional Dependencies, 3NF, BCNF & Lossless Joins", false, now),
                    SyllabusTopicEntity("syl_6", "Operating Systems", 1, "CPU Scheduling, Deadlocks & Banker Algorithm", true, now)
                )
            )
        }

        if (dao.getAllCourseGradesOnce().isEmpty()) {
            dao.upsertCourseGrades(
                listOf(
                    CourseGradeEntity("grd_1", 1, "Engineering Mathematics-I", 4, 8.5),
                    CourseGradeEntity("grd_2", 1, "Programming for Problem Solving", 4, 9.0),
                    CourseGradeEntity("grd_3", 2, "Data Structures", 4, 8.8),
                    CourseGradeEntity("grd_4", 2, "Digital Logic Design", 3, 8.2),
                    CourseGradeEntity("grd_5", 3, "Object Oriented Programming", 4, 9.1),
                    CourseGradeEntity("grd_6", 3, "Computer Organization & Architecture", 4, 8.6)
                )
            )
        }

        if (dao.countDirectoryContacts() == 0) {
            runCatching {
                val rawJson = appContext.assets.open("campus_directory.json")
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
                val parsed = json.decodeFromString<BundledDirectoryAsset>(rawJson)
                dao.upsertDirectoryContacts(
                    parsed.contacts.map { c ->
                        DirectoryContactEntity(
                            id = c.id,
                            name = c.name,
                            designation = c.designation,
                            department = c.department,
                            category = c.category,
                            phone = c.phone,
                            email = c.email,
                            officeLocation = c.officeLocation,
                            officeHours = c.officeHours,
                            latitude = c.latitude,
                            longitude = c.longitude
                        )
                    }
                )
            }
        }
    }

    override suspend fun addAssignment(
        title: String,
        subjectName: String,
        dueEpochMillis: Long,
        priority: PriorityLevel,
        notes: String
    ) {
        if (title.isBlank()) return
        dao.upsertAssignment(
            AssignmentEntity(
                id = "asg_${UUID.randomUUID()}",
                title = title.trim(),
                subjectName = subjectName.trim().ifEmpty { "General" },
                dueEpochMillis = dueEpochMillis,
                priority = priority.name,
                notes = notes.trim(),
                isCompleted = false,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    override suspend fun toggleAssignmentCompleted(id: String, completed: Boolean) {
        dao.setAssignmentCompleted(id, completed, System.currentTimeMillis())
    }

    override suspend fun deleteAssignment(id: String) {
        dao.deleteAssignment(id)
    }

    override suspend fun addExam(
        subjectName: String,
        subjectCode: String,
        examTitle: String,
        examEpochMillis: Long,
        timeSlot: String,
        venue: String,
        notes: String
    ) {
        if (subjectName.isBlank()) return
        dao.upsertExam(
            ExamEntity(
                id = "ex_${UUID.randomUUID()}",
                subjectName = subjectName.trim(),
                subjectCode = subjectCode.trim().ifEmpty { "EXM" },
                examTitle = examTitle.trim().ifEmpty { "Semester Exam" },
                examEpochMillis = examEpochMillis,
                timeSlot = timeSlot.trim().ifEmpty { "10:00 AM - 01:00 PM" },
                venue = venue.trim().ifEmpty { "Main Exam Hall" },
                notes = notes.trim()
            )
        )
    }

    override suspend fun deleteExam(id: String) {
        dao.deleteExam(id)
    }

    override suspend fun addSyllabusTopic(subjectName: String, unitNumber: Int, topicTitle: String) {
        if (topicTitle.isBlank()) return
        dao.upsertSyllabusTopic(
            SyllabusTopicEntity(
                id = "syl_${UUID.randomUUID()}",
                subjectName = subjectName.trim().ifEmpty { "General" },
                unitNumber = unitNumber.coerceIn(1, 10),
                topicTitle = topicTitle.trim(),
                isCompleted = false,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    override suspend fun toggleSyllabusTopic(id: String, completed: Boolean) {
        dao.setSyllabusTopicCompleted(id, completed, System.currentTimeMillis())
    }

    override suspend fun deleteSyllabusTopic(id: String) {
        dao.deleteSyllabusTopic(id)
    }

    override suspend fun addCourseGrade(
        semester: Int,
        subjectName: String,
        credits: Int,
        gradePointOrMarks: Double
    ) {
        if (subjectName.isBlank()) return
        dao.upsertCourseGrade(
            CourseGradeEntity(
                id = "grd_${UUID.randomUUID()}",
                semester = semester.coerceIn(1, 10),
                subjectName = subjectName.trim(),
                credits = credits.coerceIn(1, 10),
                gradePointOrMarks = gradePointOrMarks
            )
        )
    }

    override suspend fun deleteCourseGrade(id: String) {
        dao.deleteCourseGrade(id)
    }

    override suspend fun addImportedStudyMaterial(
        title: String,
        subjectName: String,
        semester: Int,
        type: MaterialType,
        year: Int,
        localFilePath: String,
        mimeType: String,
        fileSizeBytes: Long
    ) {
        dao.upsertStudyMaterial(
            StudyMaterialEntity(
                id = "mat_${UUID.randomUUID()}",
                title = title.trim().ifEmpty { "Study Note" },
                subjectName = subjectName.trim().ifEmpty { "General" },
                semester = semester.coerceIn(1, 10),
                type = type.name,
                year = year,
                localFilePath = localFilePath,
                mimeType = mimeType,
                fileSizeBytes = fileSizeBytes,
                isBookmarked = false,
                upvotes = 1,
                addedAtMillis = System.currentTimeMillis()
            )
        )
    }

    override suspend fun toggleBookmarkMaterial(id: String, bookmarked: Boolean) {
        dao.setMaterialBookmarked(id, bookmarked)
    }

    override suspend fun deleteStudyMaterial(id: String) {
        dao.deleteStudyMaterial(id)
    }
}
