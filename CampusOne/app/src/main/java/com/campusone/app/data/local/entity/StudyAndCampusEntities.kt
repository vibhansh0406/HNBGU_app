package com.campusone.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "assignments",
    indices = [Index(value = ["dueEpochMillis", "isCompleted"])]
)
data class AssignmentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subjectName: String,
    val dueEpochMillis: Long,
    val priority: String,
    val notes: String,
    val isCompleted: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "exams",
    indices = [Index(value = ["examEpochMillis"])]
)
data class ExamEntity(
    @PrimaryKey val id: String,
    val subjectName: String,
    val subjectCode: String,
    val examTitle: String,
    val examEpochMillis: Long,
    val timeSlot: String,
    val venue: String,
    val notes: String
)

@Entity(
    tableName = "syllabus_topics",
    indices = [Index(value = ["subjectName", "unitNumber"])]
)
data class SyllabusTopicEntity(
    @PrimaryKey val id: String,
    val subjectName: String,
    val unitNumber: Int,
    val topicTitle: String,
    val isCompleted: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "course_grades",
    indices = [Index(value = ["semester"])]
)
data class CourseGradeEntity(
    @PrimaryKey val id: String,
    val semester: Int,
    val subjectName: String,
    val credits: Int,
    val gradePointOrMarks: Double
)

@Entity(
    tableName = "study_materials",
    indices = [Index(value = ["subjectName", "semester", "isBookmarked"])]
)
data class StudyMaterialEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subjectName: String,
    val semester: Int,
    val type: String,
    val year: Int,
    val localFilePath: String,
    val mimeType: String,
    val fileSizeBytes: Long,
    val isBookmarked: Boolean = false,
    val upvotes: Int = 1,
    val addedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "directory_contacts",
    indices = [Index(value = ["category", "department"])]
)
data class DirectoryContactEntity(
    @PrimaryKey val id: String,
    val name: String,
    val designation: String,
    val department: String,
    val category: String,
    val phone: String,
    val email: String,
    val officeLocation: String,
    val officeHours: String,
    val latitude: Double = 30.2224,
    val longitude: Double = 78.7834
)
