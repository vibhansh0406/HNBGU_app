package `in`.campusone.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import `in`.campusone.app.data.local.entity.AssignmentEntity
import `in`.campusone.app.data.local.entity.CourseGradeEntity
import `in`.campusone.app.data.local.entity.DirectoryContactEntity
import `in`.campusone.app.data.local.entity.ExamEntity
import `in`.campusone.app.data.local.entity.StudyMaterialEntity
import `in`.campusone.app.data.local.entity.SyllabusTopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyAndCampusDao {

    // Assignments
    @Query("SELECT * FROM assignments ORDER BY isCompleted ASC, dueEpochMillis ASC")
    fun observeAssignments(): Flow<List<AssignmentEntity>>

    @Query("SELECT * FROM assignments ORDER BY dueEpochMillis ASC")
    suspend fun getAllAssignmentsOnce(): List<AssignmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAssignment(assignment: AssignmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAssignments(assignments: List<AssignmentEntity>)

    @Query("UPDATE assignments SET isCompleted = :completed, updatedAtMillis = :updatedAt WHERE id = :id")
    suspend fun setAssignmentCompleted(id: String, completed: Boolean, updatedAt: Long)

    @Query("DELETE FROM assignments WHERE id = :id")
    suspend fun deleteAssignment(id: String)

    // Exams
    @Query("SELECT * FROM exams ORDER BY examEpochMillis ASC")
    fun observeExams(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams ORDER BY examEpochMillis ASC")
    suspend fun getAllExamsOnce(): List<ExamEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExam(exam: ExamEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExams(exams: List<ExamEntity>)

    @Query("DELETE FROM exams WHERE id = :id")
    suspend fun deleteExam(id: String)

    // Syllabus Topics
    @Query("SELECT * FROM syllabus_topics ORDER BY subjectName ASC, unitNumber ASC")
    fun observeSyllabusTopics(): Flow<List<SyllabusTopicEntity>>

    @Query("SELECT * FROM syllabus_topics ORDER BY subjectName ASC, unitNumber ASC")
    suspend fun getAllSyllabusTopicsOnce(): List<SyllabusTopicEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyllabusTopic(topic: SyllabusTopicEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyllabusTopics(topics: List<SyllabusTopicEntity>)

    @Query("UPDATE syllabus_topics SET isCompleted = :completed, updatedAtMillis = :updatedAt WHERE id = :id")
    suspend fun setSyllabusTopicCompleted(id: String, completed: Boolean, updatedAt: Long)

    // Course Grades
    @Query("SELECT * FROM course_grades ORDER BY semester ASC, subjectName ASC")
    fun observeCourseGrades(): Flow<List<CourseGradeEntity>>

    @Query("SELECT * FROM course_grades ORDER BY semester ASC, subjectName ASC")
    suspend fun getAllCourseGradesOnce(): List<CourseGradeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCourseGrade(entry: CourseGradeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCourseGrades(entries: List<CourseGradeEntity>)

    @Query("DELETE FROM course_grades WHERE id = :id")
    suspend fun deleteCourseGrade(id: String)

    // Study Materials (Personal Notes & PYQ Vault)
    @Query("SELECT * FROM study_materials ORDER BY addedAtMillis DESC")
    fun observeStudyMaterials(): Flow<List<StudyMaterialEntity>>

    @Query("SELECT * FROM study_materials ORDER BY addedAtMillis DESC")
    suspend fun getAllStudyMaterialsOnce(): List<StudyMaterialEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStudyMaterial(material: StudyMaterialEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStudyMaterials(materials: List<StudyMaterialEntity>)

    @Query("UPDATE study_materials SET isBookmarked = :bookmarked WHERE id = :id")
    suspend fun setMaterialBookmarked(id: String, bookmarked: Boolean)

    @Query("DELETE FROM study_materials WHERE id = :id")
    suspend fun deleteStudyMaterial(id: String)

    // Campus Directory
    @Query("SELECT * FROM directory_contacts ORDER BY category ASC, name ASC")
    fun observeDirectoryContacts(): Flow<List<DirectoryContactEntity>>

    @Query("SELECT COUNT(*) FROM directory_contacts")
    suspend fun countDirectoryContacts(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDirectoryContacts(contacts: List<DirectoryContactEntity>)
}
