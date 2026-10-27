package com.campusone.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subjects",
    indices = [Index(value = ["code"], unique = false)]
)
data class SubjectEntity(
    @PrimaryKey val id: String,
    val code: String,
    val name: String,
    val professor: String,
    val semester: Int,
    val attendedCount: Int = 0,
    val totalClassesCount: Int = 0,
    val cancelledCount: Int = 0,
    val targetPercentage: Int = 75,
    val colorHex: String = "#0F4C81",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "attendance_records",
    indices = [Index(value = ["subjectId", "epochDay"])]
)
data class AttendanceRecordEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val subjectName: String,
    val epochDay: Long,
    val dateLabel: String,
    val status: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "timetable_slots",
    indices = [Index(value = ["dayOfWeek", "startTime"])]
)
data class TimetableSlotEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val subjectName: String,
    val subjectCode: String,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val room: String,
    val teacherName: String,
    val semester: Int,
    val section: String,
    val colorHex: String = "#0F4C81"
)

@Entity(tableName = "personal_notices")
data class PersonalNoticeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val category: String = "PERSONAL",
    val isPinned: Boolean = false,
    val isRead: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)
