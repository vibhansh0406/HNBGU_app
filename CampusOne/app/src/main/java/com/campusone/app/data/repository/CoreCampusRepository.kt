package com.campusone.app.data.repository

import com.campusone.app.data.local.dao.CoreDao
import com.campusone.app.data.local.entity.AttendanceRecordEntity
import com.campusone.app.data.local.entity.PersonalNoticeEntity
import com.campusone.app.data.local.entity.SubjectEntity
import com.campusone.app.data.local.entity.TimetableSlotEntity
import com.campusone.app.data.preferences.UserPreferencesRepository
import com.campusone.app.domain.model.AttendanceRecordItem
import com.campusone.app.domain.model.AttendanceStatus
import com.campusone.app.domain.model.PersonalNotice
import com.campusone.app.domain.model.SubjectSummary
import com.campusone.app.domain.model.TimetableSlot
import com.campusone.app.domain.model.UserProfile
import com.campusone.app.domain.usecase.AttendanceCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

interface CoreCampusRepository {
    fun observeSubjectsWithAdvice(): Flow<List<SubjectSummary>>
    fun observeRecentAttendanceHistory(): Flow<List<AttendanceRecordItem>>
    fun observeTimetableSlots(): Flow<List<TimetableSlot>>
    fun observePersonalNotices(): Flow<List<PersonalNotice>>

    suspend fun ensurePilotSeedData()
    suspend fun addSubject(
        code: String,
        name: String,
        professor: String,
        semester: Int,
        targetPercentage: Int = 75,
        colorHex: String = "#0F4C81"
    )
    suspend fun updateSubjectDetails(
        subjectId: String,
        code: String,
        name: String,
        professor: String,
        targetPercentage: Int,
        colorHex: String
    )
    suspend fun deleteSubject(subjectId: String)

    suspend fun markAttendance(
        subjectId: String,
        status: AttendanceStatus,
        epochDay: Long = LocalDate.now().toEpochDay()
    ): String?
    suspend fun deleteOrUndoAttendanceRecord(recordId: String): Boolean
    suspend fun editAttendanceRecord(
        recordId: String,
        newStatus: AttendanceStatus,
        newEpochDay: Long
    ): Boolean

    suspend fun upsertTimetableSlot(
        slotId: String? = null,
        subjectId: String = "",
        subjectName: String,
        subjectCode: String,
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        room: String,
        teacherName: String,
        semester: Int,
        section: String,
        colorHex: String = "#0F4C81"
    )
    suspend fun deleteTimetableSlot(slotId: String)

    suspend fun addPersonalNotice(title: String, body: String, isPinned: Boolean)
    suspend fun markNoticeRead(noticeId: String)
    suspend fun deletePersonalNotice(noticeId: String)
}

class OfflineCoreCampusRepository(
    private val coreDao: CoreDao,
    private val userPrefs: UserPreferencesRepository
) : CoreCampusRepository {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    override fun observeSubjectsWithAdvice(): Flow<List<SubjectSummary>> {
        return combine(
            coreDao.observeSubjects(),
            userPrefs.userProfileFlow
        ) { entities: List<SubjectEntity>, profile: UserProfile ->
            entities.map { entity ->
                val effectiveTarget = if (entity.targetPercentage in 1..99) {
                    entity.targetPercentage
                } else {
                    profile.targetAttendancePercent
                }
                SubjectSummary(
                    id = entity.id,
                    code = entity.code,
                    name = entity.name,
                    professor = entity.professor,
                    semester = entity.semester,
                    attendedCount = entity.attendedCount,
                    totalClassesCount = entity.totalClassesCount,
                    cancelledCount = entity.cancelledCount,
                    targetPercentage = effectiveTarget,
                    colorHex = entity.colorHex.ifBlank { "#0F4C81" },
                    advice = AttendanceCalculator.evaluate(
                        attended = entity.attendedCount,
                        totalConducted = entity.totalClassesCount,
                        targetPercentage = effectiveTarget
                    )
                )
            }
        }
    }

    override fun observeRecentAttendanceHistory(): Flow<List<AttendanceRecordItem>> {
        return coreDao.observeRecentAttendanceRecords().map { list ->
            list.map { rec ->
                AttendanceRecordItem(
                    id = rec.id,
                    subjectId = rec.subjectId,
                    subjectName = rec.subjectName,
                    epochDay = rec.epochDay,
                    dateLabel = rec.dateLabel,
                    status = runCatching { AttendanceStatus.valueOf(rec.status) }
                        .getOrDefault(AttendanceStatus.PRESENT),
                    timestampMillis = rec.timestampMillis
                )
            }
        }
    }

    override fun observeTimetableSlots(): Flow<List<TimetableSlot>> {
        return coreDao.observeTimetableSlots().map { list ->
            list.map { s ->
                TimetableSlot(
                    id = s.id,
                    subjectId = s.subjectId,
                    subjectName = s.subjectName,
                    subjectCode = s.subjectCode,
                    dayOfWeek = s.dayOfWeek,
                    startTime = s.startTime,
                    endTime = s.endTime,
                    room = s.room,
                    teacherName = s.teacherName,
                    semester = s.semester,
                    section = s.section,
                    colorHex = s.colorHex.ifBlank { "#0F4C81" }
                )
            }
        }
    }

    override fun observePersonalNotices(): Flow<List<PersonalNotice>> {
        return coreDao.observePersonalNotices().map { list ->
            list.map { n ->
                PersonalNotice(
                    id = n.id,
                    title = n.title,
                    body = n.body,
                    category = n.category,
                    isPinned = n.isPinned,
                    isRead = n.isRead,
                    createdAtMillis = n.createdAtMillis
                )
            }
        }
    }

    override suspend fun ensurePilotSeedData() {
        if (coreDao.getAllSubjectsOnce().isEmpty()) {
            val seedSubjects = listOf(
                SubjectEntity("sub_dsa", "CSE-401", "Design & Analysis of Algorithms", "Prof. M. M. S. Rauthan", 4, 18, 22, 1, 75, "#0F4C81"),
                SubjectEntity("sub_dbms", "CSE-402", "Database Management Systems", "Dr. K. S. Bhatia", 4, 14, 20, 0, 75, "#006B5E"),
                SubjectEntity("sub_os", "CSE-403", "Operating Systems", "Dr. Neeraj Panwar", 4, 19, 21, 1, 75, "#7C3AED"),
                SubjectEntity("sub_math", "MAT-404", "Discrete Mathematics & Graph Theory", "Dr. R. Nautiyal", 4, 12, 18, 0, 75, "#D97706")
            )
            coreDao.upsertSubjects(seedSubjects)

            val today = LocalDate.now()
            val seedRecords = listOf(
                AttendanceRecordEntity("att_seed_1", "sub_dsa", "Design & Analysis of Algorithms", today.minusDays(5).toEpochDay(), today.minusDays(5).format(dateFormatter), AttendanceStatus.PRESENT.name, System.currentTimeMillis() - 5 * 86_400_000L),
                AttendanceRecordEntity("att_seed_2", "sub_dbms", "Database Management Systems", today.minusDays(4).toEpochDay(), today.minusDays(4).format(dateFormatter), AttendanceStatus.ABSENT.name, System.currentTimeMillis() - 4 * 86_400_000L),
                AttendanceRecordEntity("att_seed_3", "sub_os", "Operating Systems", today.minusDays(3).toEpochDay(), today.minusDays(3).format(dateFormatter), AttendanceStatus.PRESENT.name, System.currentTimeMillis() - 3 * 86_400_000L),
                AttendanceRecordEntity("att_seed_4", "sub_math", "Discrete Mathematics & Graph Theory", today.minusDays(2).toEpochDay(), today.minusDays(2).format(dateFormatter), AttendanceStatus.ABSENT.name, System.currentTimeMillis() - 2 * 86_400_000L),
                AttendanceRecordEntity("att_seed_5", "sub_dsa", "Design & Analysis of Algorithms", today.minusDays(1).toEpochDay(), today.minusDays(1).format(dateFormatter), AttendanceStatus.PRESENT.name, System.currentTimeMillis() - 86_400_000L)
            )
            coreDao.insertAttendanceRecords(seedRecords)

            val seedSlots = listOf(
                TimetableSlotEntity("slot_1", "sub_dsa", "Design & Analysis of Algorithms", "CSE-401", 1, "09:30", "10:30", "CSE Block 102", "Prof. M. M. S. Rauthan", 4, "A", "#0F4C81"),
                TimetableSlotEntity("slot_2", "sub_dbms", "Database Management Systems", "CSE-402", 1, "11:00", "12:00", "Lab-2 Chauras", "Dr. K. S. Bhatia", 4, "A", "#006B5E"),
                TimetableSlotEntity("slot_3", "sub_os", "Operating Systems", "CSE-403", 2, "10:00", "11:00", "CSE Block 104", "Dr. Neeraj Panwar", 4, "A", "#7C3AED"),
                TimetableSlotEntity("slot_4", "sub_math", "Discrete Mathematics & Graph Theory", "MAT-404", 3, "11:30", "12:30", "Lecture Hall 3", "Dr. R. Nautiyal", 4, "A", "#D97706"),
                TimetableSlotEntity("slot_5", "sub_dsa", "Design & Analysis of Algorithms", "CSE-401", 4, "09:30", "10:30", "CSE Block 102", "Prof. M. M. S. Rauthan", 4, "A", "#0F4C81"),
                TimetableSlotEntity("slot_6", "sub_os", "Operating Systems", "CSE-403", 5, "14:00", "15:00", "Systems Lab 1", "Dr. Neeraj Panwar", 4, "A", "#7C3AED")
            )
            coreDao.upsertTimetableSlots(seedSlots)

            coreDao.upsertPersonalNotice(
                PersonalNoticeEntity(
                    id = "notice_welcome",
                    title = "Welcome to CampusOne (100% On-Device Edition)",
                    body = "All attendance logs, safe-bunk projections, timetable slots, department packs, assignments, exams, GPA calculations, and PDF notes are stored locally in Room DB.",
                    category = "SYSTEM",
                    isPinned = true,
                    isRead = false
                )
            )
        }
    }

    override suspend fun addSubject(
        code: String,
        name: String,
        professor: String,
        semester: Int,
        targetPercentage: Int,
        colorHex: String
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        val entity = SubjectEntity(
            id = "sub_${UUID.randomUUID()}",
            code = code.trim().ifEmpty { "SUB-101" },
            name = cleanName,
            professor = professor.trim().ifEmpty { "Faculty" },
            semester = semester.coerceIn(1, 10),
            targetPercentage = targetPercentage.coerceIn(50, 95),
            colorHex = colorHex.ifBlank { "#0F4C81" }
        )
        coreDao.upsertSubject(entity)
    }

    override suspend fun updateSubjectDetails(
        subjectId: String,
        code: String,
        name: String,
        professor: String,
        targetPercentage: Int,
        colorHex: String
    ) {
        val existing = coreDao.getSubjectById(subjectId) ?: return
        val cleanColor = colorHex.ifBlank { existing.colorHex }
        val updated = existing.copy(
            code = code.trim().ifEmpty { existing.code },
            name = name.trim().ifEmpty { existing.name },
            professor = professor.trim().ifEmpty { existing.professor },
            targetPercentage = targetPercentage.coerceIn(50, 95),
            colorHex = cleanColor,
            updatedAtMillis = System.currentTimeMillis()
        )
        coreDao.upsertSubject(updated)
        coreDao.updateSlotsColorForSubject(
            subjectId = updated.id,
            subjectCode = updated.code,
            colorHex = cleanColor
        )
    }

    override suspend fun deleteSubject(subjectId: String) {
        coreDao.deleteSubject(subjectId)
        coreDao.deleteAttendanceRecordsForSubject(subjectId)
    }

    override suspend fun markAttendance(
        subjectId: String,
        status: AttendanceStatus,
        epochDay: Long
    ): String? {
        val existing = coreDao.getSubjectById(subjectId) ?: return null
        val updated = applyStatusIncrement(existing, status)
        coreDao.upsertSubject(updated)

        val date = LocalDate.ofEpochDay(epochDay)
        val recordId = "att_${UUID.randomUUID()}"
        coreDao.insertAttendanceRecord(
            AttendanceRecordEntity(
                id = recordId,
                subjectId = existing.id,
                subjectName = existing.name,
                epochDay = epochDay,
                dateLabel = date.format(dateFormatter),
                status = status.name,
                timestampMillis = System.currentTimeMillis()
            )
        )
        return recordId
    }

    override suspend fun deleteOrUndoAttendanceRecord(recordId: String): Boolean {
        val record = coreDao.getAttendanceRecordById(recordId) ?: return false
        val subject = coreDao.getSubjectById(record.subjectId)
        if (subject != null) {
            val oldStatus = runCatching { AttendanceStatus.valueOf(record.status) }
                .getOrDefault(AttendanceStatus.PRESENT)
            val reverted = applyStatusDecrement(subject, oldStatus)
            coreDao.upsertSubject(reverted)
        }
        coreDao.deleteAttendanceRecordById(recordId)
        return true
    }

    override suspend fun editAttendanceRecord(
        recordId: String,
        newStatus: AttendanceStatus,
        newEpochDay: Long
    ): Boolean {
        val record = coreDao.getAttendanceRecordById(recordId) ?: return false
        val oldStatus = runCatching { AttendanceStatus.valueOf(record.status) }
            .getOrDefault(AttendanceStatus.PRESENT)
        val subject = coreDao.getSubjectById(record.subjectId)
        if (subject != null && oldStatus != newStatus) {
            val decremented = applyStatusDecrement(subject, oldStatus)
            val incremented = applyStatusIncrement(decremented, newStatus)
            coreDao.upsertSubject(incremented)
        }
        val newDate = LocalDate.ofEpochDay(newEpochDay)
        coreDao.insertAttendanceRecord(
            record.copy(
                status = newStatus.name,
                epochDay = newEpochDay,
                dateLabel = newDate.format(dateFormatter),
                timestampMillis = System.currentTimeMillis()
            )
        )
        return true
    }

    private fun applyStatusIncrement(subject: SubjectEntity, status: AttendanceStatus): SubjectEntity {
        return when (status) {
            AttendanceStatus.PRESENT -> subject.copy(
                attendedCount = subject.attendedCount + 1,
                totalClassesCount = subject.totalClassesCount + 1,
                updatedAtMillis = System.currentTimeMillis()
            )
            AttendanceStatus.ABSENT -> subject.copy(
                totalClassesCount = subject.totalClassesCount + 1,
                updatedAtMillis = System.currentTimeMillis()
            )
            AttendanceStatus.CANCELLED -> subject.copy(
                cancelledCount = subject.cancelledCount + 1,
                updatedAtMillis = System.currentTimeMillis()
            )
        }
    }

    private fun applyStatusDecrement(subject: SubjectEntity, status: AttendanceStatus): SubjectEntity {
        return when (status) {
            AttendanceStatus.PRESENT -> subject.copy(
                attendedCount = (subject.attendedCount - 1).coerceAtLeast(0),
                totalClassesCount = (subject.totalClassesCount - 1).coerceAtLeast(0),
                updatedAtMillis = System.currentTimeMillis()
            )
            AttendanceStatus.ABSENT -> subject.copy(
                totalClassesCount = (subject.totalClassesCount - 1).coerceAtLeast(0),
                updatedAtMillis = System.currentTimeMillis()
            )
            AttendanceStatus.CANCELLED -> subject.copy(
                cancelledCount = (subject.cancelledCount - 1).coerceAtLeast(0),
                updatedAtMillis = System.currentTimeMillis()
            )
        }
    }

    override suspend fun upsertTimetableSlot(
        slotId: String?,
        subjectId: String,
        subjectName: String,
        subjectCode: String,
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        room: String,
        teacherName: String,
        semester: Int,
        section: String,
        colorHex: String
    ) {
        if (subjectName.isBlank()) return
        val resolvedId = slotId?.takeIf { it.isNotBlank() } ?: "slot_${UUID.randomUUID()}"
        val resolvedSubjectId = subjectId.ifBlank { "custom_${subjectCode.trim().lowercase()}" }
        val cleanColor = colorHex.ifBlank { "#0F4C81" }
        coreDao.upsertTimetableSlot(
            TimetableSlotEntity(
                id = resolvedId,
                subjectId = resolvedSubjectId,
                subjectName = subjectName.trim(),
                subjectCode = subjectCode.trim().ifEmpty { "CLS" },
                dayOfWeek = dayOfWeek.coerceIn(1, 7),
                startTime = startTime.trim().ifEmpty { "09:00" },
                endTime = endTime.trim().ifEmpty { "10:00" },
                room = room.trim().ifEmpty { "Room 101" },
                teacherName = teacherName.trim().ifEmpty { "Faculty" },
                semester = semester,
                section = section,
                colorHex = cleanColor
            )
        )
    }

    override suspend fun deleteTimetableSlot(slotId: String) {
        coreDao.deleteTimetableSlot(slotId)
    }

    override suspend fun addPersonalNotice(title: String, body: String, isPinned: Boolean) {
        if (title.isBlank()) return
        coreDao.upsertPersonalNotice(
            PersonalNoticeEntity(
                id = "pnotice_${UUID.randomUUID()}",
                title = title.trim(),
                body = body.trim(),
                category = "PERSONAL",
                isPinned = isPinned,
                isRead = false,
                createdAtMillis = System.currentTimeMillis()
            )
        )
    }

    override suspend fun markNoticeRead(noticeId: String) {
        coreDao.markNoticeRead(noticeId)
    }

    override suspend fun deletePersonalNotice(noticeId: String) {
        coreDao.deletePersonalNotice(noticeId)
    }
}
