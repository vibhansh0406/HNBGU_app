package `in`.campusone.app.data.repository

import `in`.campusone.app.data.local.dao.CoreDao
import `in`.campusone.app.data.local.entity.AttendanceRecordEntity
import `in`.campusone.app.data.local.entity.PersonalNoticeEntity
import `in`.campusone.app.data.local.entity.SubjectEntity
import `in`.campusone.app.data.local.entity.TimetableSlotEntity
import `in`.campusone.app.data.preferences.UserPreferencesRepository
import `in`.campusone.app.domain.model.AttendanceRecordItem
import `in`.campusone.app.domain.model.AttendanceStatus
import `in`.campusone.app.domain.model.PersonalNotice
import `in`.campusone.app.domain.model.SubjectSummary
import `in`.campusone.app.domain.model.TimetableSlot
import `in`.campusone.app.domain.usecase.AttendanceCalculator
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
    suspend fun addSubject(code: String, name: String, professor: String, semester: Int)
    suspend fun markAttendance(subjectId: String, status: AttendanceStatus)
    suspend fun addTimetableSlot(
        subjectName: String,
        subjectCode: String,
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        room: String,
        teacherName: String,
        semester: Int,
        section: String
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
        ) { entities, profile ->
            entities.map { entity ->
                SubjectSummary(
                    id = entity.id,
                    code = entity.code,
                    name = entity.name,
                    professor = entity.professor,
                    semester = entity.semester,
                    attendedCount = entity.attendedCount,
                    totalClassesCount = entity.totalClassesCount,
                    cancelledCount = entity.cancelledCount,
                    advice = AttendanceCalculator.evaluate(
                        attended = entity.attendedCount,
                        totalConducted = entity.totalClassesCount,
                        targetPercentage = profile.targetAttendancePercent
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
                    section = s.section
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
                SubjectEntity("sub_dsa", "CSE-401", "Design & Analysis of Algorithms", "Prof. M. M. S. Rauthan", 4, 18, 22, 1),
                SubjectEntity("sub_dbms", "CSE-402", "Database Management Systems", "Dr. K. S. Bhatia", 4, 14, 20, 0),
                SubjectEntity("sub_os", "CSE-403", "Operating Systems", "Dr. Neeraj Panwar", 4, 19, 21, 1),
                SubjectEntity("sub_math", "MAT-404", "Discrete Mathematics & Graph Theory", "Dr. R. Nautiyal", 4, 12, 18, 0)
            )
            coreDao.upsertSubjects(seedSubjects)

            val seedSlots = listOf(
                TimetableSlotEntity("slot_1", "sub_dsa", "Design & Analysis of Algorithms", "CSE-401", 1, "09:30", "10:30", "CSE Block 102", "Prof. M. M. S. Rauthan", 4, "A"),
                TimetableSlotEntity("slot_2", "sub_dbms", "Database Management Systems", "CSE-402", 1, "11:00", "12:00", "Lab-2 Chauras", "Dr. K. S. Bhatia", 4, "A"),
                TimetableSlotEntity("slot_3", "sub_os", "Operating Systems", "CSE-403", 2, "10:00", "11:00", "CSE Block 104", "Dr. Neeraj Panwar", 4, "A"),
                TimetableSlotEntity("slot_4", "sub_math", "Discrete Mathematics & Graph Theory", "MAT-404", 3, "11:30", "12:30", "Lecture Hall 3", "Dr. R. Nautiyal", 4, "A"),
                TimetableSlotEntity("slot_5", "sub_dsa", "Design & Analysis of Algorithms", "CSE-401", 4, "09:30", "10:30", "CSE Block 102", "Prof. M. M. S. Rauthan", 4, "A"),
                TimetableSlotEntity("slot_6", "sub_os", "Operating Systems", "CSE-403", 5, "14:00", "15:00", "Systems Lab 1", "Dr. Neeraj Panwar", 4, "A")
            )
            coreDao.upsertTimetableSlots(seedSlots)

            coreDao.upsertPersonalNotice(
                PersonalNoticeEntity(
                    id = "notice_welcome",
                    title = "Welcome to CampusOne (100% On-Device Edition)",
                    body = "All attendance logs, timetable slots, assignments, exams, GPA calculations, and PDF notes are stored locally in Room DB. Export a JSON backup anytime from Campus -> Backup & Settings.",
                    category = "SYSTEM",
                    isPinned = true,
                    isRead = false
                )
            )
        }
    }

    override suspend fun addSubject(code: String, name: String, professor: String, semester: Int) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        val entity = SubjectEntity(
            id = "sub_${UUID.randomUUID()}",
            code = code.trim().ifEmpty { "SUB-101" },
            name = cleanName,
            professor = professor.trim().ifEmpty { "Faculty" },
            semester = semester.coerceIn(1, 10)
        )
        coreDao.upsertSubject(entity)
    }

    override suspend fun markAttendance(subjectId: String, status: AttendanceStatus) {
        val existing = coreDao.getSubjectById(subjectId) ?: return
        val updated = when (status) {
            AttendanceStatus.PRESENT -> existing.copy(
                attendedCount = existing.attendedCount + 1,
                totalClassesCount = existing.totalClassesCount + 1,
                updatedAtMillis = System.currentTimeMillis()
            )
            AttendanceStatus.ABSENT -> existing.copy(
                totalClassesCount = existing.totalClassesCount + 1,
                updatedAtMillis = System.currentTimeMillis()
            )
            AttendanceStatus.CANCELLED -> existing.copy(
                cancelledCount = existing.cancelledCount + 1,
                updatedAtMillis = System.currentTimeMillis()
            )
        }
        coreDao.upsertSubject(updated)

        val today = LocalDate.now()
        coreDao.insertAttendanceRecord(
            AttendanceRecordEntity(
                id = "att_${UUID.randomUUID()}",
                subjectId = existing.id,
                subjectName = existing.name,
                epochDay = today.toEpochDay(),
                dateLabel = today.format(dateFormatter),
                status = status.name,
                timestampMillis = System.currentTimeMillis()
            )
        )
    }

    override suspend fun addTimetableSlot(
        subjectName: String,
        subjectCode: String,
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        room: String,
        teacherName: String,
        semester: Int,
        section: String
    ) {
        if (subjectName.isBlank()) return
        coreDao.upsertTimetableSlot(
            TimetableSlotEntity(
                id = "slot_${UUID.randomUUID()}",
                subjectId = "custom_${subjectCode.lowercase()}",
                subjectName = subjectName.trim(),
                subjectCode = subjectCode.trim().ifEmpty { "CLS" },
                dayOfWeek = dayOfWeek.coerceIn(1, 7),
                startTime = startTime.trim().ifEmpty { "09:00" },
                endTime = endTime.trim().ifEmpty { "10:00" },
                room = room.trim().ifEmpty { "Room 101" },
                teacherName = teacherName.trim().ifEmpty { "Faculty" },
                semester = semester,
                section = section
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
