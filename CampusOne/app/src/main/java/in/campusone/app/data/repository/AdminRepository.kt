package `in`.campusone.app.data.repository

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.campusone.app.data.local.dao.AttendanceDao
import `in`.campusone.app.data.local.dao.CampusLifeDao
import `in`.campusone.app.data.local.dao.NoticeDao
import `in`.campusone.app.data.local.dao.StudyDao
import `in`.campusone.app.data.local.dao.TimetableDao
import `in`.campusone.app.data.local.entity.AssignmentEntity
import `in`.campusone.app.data.local.entity.CampusEventEntity
import `in`.campusone.app.data.local.entity.ClubEntity
import `in`.campusone.app.data.local.entity.DirectoryContactEntity
import `in`.campusone.app.data.local.entity.ExamEntity
import `in`.campusone.app.data.local.entity.LostFoundEntity
import `in`.campusone.app.data.local.entity.NoticeEntity
import `in`.campusone.app.data.local.entity.StudyResourceEntity
import `in`.campusone.app.data.local.entity.SubjectEntity
import `in`.campusone.app.data.local.entity.SyllabusTopicEntity
import `in`.campusone.app.data.local.entity.TimetableSlotEntity
import `in`.campusone.app.data.preferences.UserPreferencesRepository
import `in`.campusone.app.domain.model.UserProfile
import `in`.campusone.app.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val attendanceDao: AttendanceDao,
    private val timetableDao: TimetableDao,
    private val noticeDao: NoticeDao,
    private val studyDao: StudyDao,
    private val campusLifeDao: CampusLifeDao,
    private val prefs: UserPreferencesRepository
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // =========================================================================
    // USERS / PROFILES
    // =========================================================================
    fun observeAllProfiles(): Flow<List<UserProfile>> =
        prefs.userProfile.map { listOf(it) }

    suspend fun saveProfile(profile: UserProfile) {
        prefs.saveUserProfile(profile, markOnboarded = true)
    }

    suspend fun updateProfileRole(uid: String, role: UserRole) {
        // Re-encode the current profile with the new role
        val current = prefs.userProfile.first()
        prefs.saveUserProfile(current.copy(role = role), markOnboarded = true)
    }

    // =========================================================================
    // SUBJECTS
    // =========================================================================
    fun observeSubjects(sectionKey: String): Flow<List<SubjectEntity>> =
        attendanceDao.observeAllSectionSubjects(sectionKey)

    suspend fun upsertSubjects(subjects: List<SubjectEntity>) =
        attendanceDao.upsertSubjects(subjects)

    suspend fun deleteSubject(subjectId: String) {
        // Room has no delete-by-id on AttendanceDao; mark unenrolled instead
        attendanceDao.setSubjectEnrollment(subjectId, false)
    }

    // =========================================================================
    // TIMETABLE
    // =========================================================================
    fun observeTimetableSlots(sectionKey: String): Flow<List<TimetableSlotEntity>> =
        timetableDao.observeTimetable(sectionKey)

    suspend fun upsertTimetableSlots(slots: List<TimetableSlotEntity>) =
        timetableDao.upsertSlots(slots)

    suspend fun deleteTimetableSlot(slotId: String, sectionKey: String) {
        timetableDao.deleteSlot(slotId)
    }

    // =========================================================================
    // NOTICES
    // =========================================================================
    fun observeNotices(sectionKey: String): Flow<List<NoticeEntity>> =
        noticeDao.observeNotices(sectionKey)

    suspend fun upsertNotices(notices: List<NoticeEntity>) =
        noticeDao.upsertNotices(notices)

    suspend fun deleteNotice(noticeId: String) =
        noticeDao.deleteNotice(noticeId)

    // =========================================================================
    // ASSIGNMENTS
    // =========================================================================
    fun observeAssignments(): Flow<List<AssignmentEntity>> =
        studyDao.observeAssignments()

    suspend fun upsertAssignments(items: List<AssignmentEntity>) =
        studyDao.upsertAssignments(items)

    suspend fun deleteAssignment(id: String) =
        studyDao.deleteAssignment(id)

    // =========================================================================
    // EXAMS
    // =========================================================================
    fun observeExams(): Flow<List<ExamEntity>> =
        studyDao.observeExams()

    suspend fun upsertExams(items: List<ExamEntity>) =
        studyDao.upsertExams(items)

    suspend fun deleteExam(id: String) =
        studyDao.deleteExam(id)

    // =========================================================================
    // SYLLABUS TOPICS
    // =========================================================================
    fun observeSyllabusTopics(): Flow<List<SyllabusTopicEntity>> =
        studyDao.observeSyllabusTopics()

    suspend fun upsertSyllabusTopics(items: List<SyllabusTopicEntity>) =
        studyDao.upsertSyllabusTopics(items)

    suspend fun deleteSyllabusTopic(id: String) =
        studyDao.deleteSyllabusTopic(id)

    // =========================================================================
    // STUDY RESOURCES
    // =========================================================================
    fun observeStudyResources(sectionKey: String): Flow<List<StudyResourceEntity>> =
        studyDao.observeVisibleResources(sectionKey)

    suspend fun upsertStudyResources(items: List<StudyResourceEntity>) =
        studyDao.upsertResources(items)

    suspend fun deleteStudyResource(id: String) =
        studyDao.deleteResourceByCr(id)

    // =========================================================================
    // CLUBS
    // =========================================================================
    fun observeClubs(collegeId: String): Flow<List<ClubEntity>> =
        campusLifeDao.observeClubs(collegeId)

    suspend fun upsertClubs(items: List<ClubEntity>) =
        campusLifeDao.upsertClubs(items)

    suspend fun approveClub(clubId: String) =
        campusLifeDao.approveClubByCr(clubId)

    suspend fun deleteClub(clubId: String) =
        campusLifeDao.deleteClub(clubId)

    // =========================================================================
    // EVENTS
    // =========================================================================
    fun observeEvents(collegeId: String): Flow<List<CampusEventEntity>> =
        campusLifeDao.observeEvents(collegeId)

    suspend fun upsertEvents(items: List<CampusEventEntity>) =
        campusLifeDao.upsertEvents(items)

    suspend fun approveEvent(eventId: String) =
        campusLifeDao.approveEventByCr(eventId)

    suspend fun deleteEvent(eventId: String) =
        campusLifeDao.deleteEvent(eventId)

    // =========================================================================
    // LOST & FOUND
    // =========================================================================
    fun observeLostFound(collegeId: String): Flow<List<LostFoundEntity>> =
        campusLifeDao.observeLostFound(collegeId)

    suspend fun upsertLostFound(items: List<LostFoundEntity>) =
        campusLifeDao.upsertLostFound(items)

    suspend fun resolveLostFound(id: String) =
        campusLifeDao.markLostFoundResolved(id)

    suspend fun hideLostFound(id: String) =
        campusLifeDao.hideLostFoundByCr(id)

    // =========================================================================
    // DIRECTORY
    // =========================================================================
    fun observeDirectory(collegeId: String): Flow<List<DirectoryContactEntity>> =
        campusLifeDao.observeDirectory(collegeId)

    suspend fun upsertDirectory(items: List<DirectoryContactEntity>) =
        campusLifeDao.upsertDirectory(items)

    // =========================================================================
    // DASHBOARD STATS
    // =========================================================================
    suspend fun getSubjectCount(sectionKey: String): Int =
        attendanceDao.observeAllSectionSubjects(sectionKey).first().size

    suspend fun getNoticeCount(sectionKey: String): Int =
        noticeDao.observeNotices(sectionKey).first().size

    suspend fun getAssignmentCount(): Int =
        studyDao.observeAssignments().first().size

    suspend fun getExamCount(): Int =
        studyDao.observeExams().first().size

    suspend fun getClubCount(collegeId: String): Int =
        campusLifeDao.observeClubs(collegeId).first().size

    suspend fun getEventCount(collegeId: String): Int =
        campusLifeDao.observeEvents(collegeId).first().size
}
