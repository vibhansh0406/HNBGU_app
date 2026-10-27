package com.campusone.app.util

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.campusone.app.data.local.CampusOneDatabase
import com.campusone.app.data.local.entity.PersonalNoticeEntity
import com.campusone.app.data.local.entity.SubjectEntity
import com.campusone.app.data.local.entity.SyllabusTopicEntity
import com.campusone.app.data.local.entity.TimetableSlotEntity
import com.campusone.app.data.preferences.UserPreferencesRepository
import com.campusone.app.domain.model.DepartmentPackPayload
import com.campusone.app.domain.model.DepartmentPackSubject
import com.campusone.app.domain.model.PersonalNotice
import com.campusone.app.domain.model.SyllabusTopic
import com.campusone.app.domain.model.TimetableSlot
import com.campusone.app.domain.usecase.DepartmentPackValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DepartmentPackManager(
    private val context: Context,
    private val database: CampusOneDatabase,
    private val userPreferences: UserPreferencesRepository
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun exportPackToUri(targetUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val coreDao = database.coreDao()
            val studyDao = database.studyAndCampusDao()
            val profile = userPreferences.getProfileOnce()

            val subjects = coreDao.getAllSubjectsOnce().map {
                DepartmentPackSubject(
                    id = it.id,
                    code = it.code,
                    name = it.name,
                    professor = it.professor,
                    semester = it.semester,
                    targetPercentage = it.targetPercentage,
                    colorHex = it.colorHex
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
                    section = it.section,
                    colorHex = it.colorHex
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

            val pack = DepartmentPackPayload(
                version = DepartmentPackValidator.CURRENT_SCHEMA_VERSION,
                departmentName = profile.college.ifBlank { "HNB Garhwal University" },
                course = profile.course.ifBlank { "B.Tech CSE" },
                semester = profile.semester,
                section = profile.section.ifBlank { "A" },
                exportedAtMillis = System.currentTimeMillis(),
                subjects = subjects,
                timetableSlots = slots,
                notices = notices,
                syllabusTopics = syllabus
            )

            val validation = DepartmentPackValidator.validate(pack)
            if (!validation.isValid) {
                throw IllegalArgumentException(validation.errorMessage ?: "Invalid department pack data.")
            }

            val serialized = json.encodeToString(pack)
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                out.write(serialized.toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException("Unable to open destination file for Department Pack.")

            validation.summaryLabel
        }
    }

    suspend fun importPackFromUri(sourceUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val rawJson = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            } ?: throw IllegalArgumentException("Could not read selected file. Please pick a valid .json Department Pack.")

            val parsed = try {
                json.decodeFromString<DepartmentPackPayload>(rawJson)
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid JSON format in Department Pack: ${e.message?.take(100)}")
            }

            val validation = DepartmentPackValidator.validate(parsed)
            if (!validation.isValid) {
                throw IllegalArgumentException(validation.errorMessage ?: "Department Pack validation failed.")
            }

            database.withTransaction {
                val coreDao = database.coreDao()
                val studyDao = database.studyAndCampusDao()

                for (sub in parsed.subjects) {
                    val existing = coreDao.getSubjectById(sub.id)
                    coreDao.upsertSubject(
                        SubjectEntity(
                            id = sub.id,
                            code = sub.code.trim(),
                            name = sub.name.trim(),
                            professor = sub.professor.trim(),
                            semester = sub.semester,
                            attendedCount = existing?.attendedCount ?: 0,
                            totalClassesCount = existing?.totalClassesCount ?: 0,
                            cancelledCount = existing?.cancelledCount ?: 0,
                            targetPercentage = sub.targetPercentage.coerceIn(50, 95),
                            colorHex = sub.colorHex.ifBlank { "#0F4C81" },
                            updatedAtMillis = System.currentTimeMillis()
                        )
                    )
                }

                coreDao.upsertTimetableSlots(
                    parsed.timetableSlots.map { slot ->
                        TimetableSlotEntity(
                            id = slot.id,
                            subjectId = slot.subjectId,
                            subjectName = slot.subjectName,
                            subjectCode = slot.subjectCode,
                            dayOfWeek = slot.dayOfWeek,
                            startTime = slot.startTime,
                            endTime = slot.endTime,
                            room = slot.room,
                            teacherName = slot.teacherName,
                            semester = slot.semester,
                            section = slot.section,
                            colorHex = slot.colorHex.ifBlank { "#0F4C81" }
                        )
                    }
                )

                coreDao.upsertPersonalNotices(
                    parsed.notices.map { n ->
                        PersonalNoticeEntity(
                            id = n.id,
                            title = n.title,
                            body = n.body,
                            category = n.category,
                            isPinned = n.isPinned,
                            isRead = false,
                            createdAtMillis = n.createdAtMillis
                        )
                    }
                )

                studyDao.upsertSyllabusTopics(
                    parsed.syllabusTopics.map { t ->
                        SyllabusTopicEntity(
                            id = t.id,
                            subjectName = t.subjectName,
                            unitNumber = t.unitNumber,
                            topicTitle = t.topicTitle,
                            isCompleted = t.isCompleted,
                            updatedAtMillis = System.currentTimeMillis()
                        )
                    }
                )
            }

            validation.summaryLabel
        }
    }
}
