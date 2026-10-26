package `in`.campusone.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import `in`.campusone.app.data.local.entity.AttendanceLogEntity
import `in`.campusone.app.data.local.entity.NoticeEntity
import `in`.campusone.app.data.local.entity.SubjectEntity
import `in`.campusone.app.data.local.entity.TimetableSlotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM subjects WHERE sectionKey = :sectionKey AND isEnrolled = 1 ORDER BY subjectName ASC")
    fun observeEnrolledSubjects(sectionKey: String): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE sectionKey = :sectionKey ORDER BY subjectName ASC")
    fun observeAllSectionSubjects(sectionKey: String): Flow<List<SubjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubjects(subjects: List<SubjectEntity>)

    @Query("UPDATE subjects SET isEnrolled = :enrolled WHERE subjectId = :subjectId")
    suspend fun setSubjectEnrollment(subjectId: String, enrolled: Boolean)

    @Query("SELECT * FROM attendance_logs ORDER BY dateEpochDay DESC, updatedAtMillis DESC")
    fun observeAllLogs(): Flow<List<AttendanceLogEntity>>

    @Query("SELECT * FROM attendance_logs WHERE subjectId = :subjectId ORDER BY dateEpochDay DESC")
    fun observeLogsForSubject(subjectId: String): Flow<List<AttendanceLogEntity>>

    @Query("SELECT * FROM attendance_logs WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY dateEpochDay DESC")
    fun observeLogsInDateRange(startEpochDay: Long, endEpochDay: Long): Flow<List<AttendanceLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttendanceLog(log: AttendanceLogEntity)

    @Query("DELETE FROM attendance_logs WHERE id = :logId")
    suspend fun deleteAttendanceLog(logId: String)

    @Query("SELECT * FROM attendance_logs WHERE isSynced = 0")
    suspend fun getUnsyncedLogs(): List<AttendanceLogEntity>

    @Query("UPDATE attendance_logs SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markLogsSynced(ids: List<String>)
}

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable_slots WHERE sectionKey = :sectionKey ORDER BY dayOfWeek ASC, startTime ASC")
    fun observeTimetable(sectionKey: String): Flow<List<TimetableSlotEntity>>

    @Query("SELECT * FROM timetable_slots WHERE sectionKey = :sectionKey ORDER BY dayOfWeek ASC, startTime ASC")
    suspend fun getTimetableSnapshot(sectionKey: String): List<TimetableSlotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSlots(slots: List<TimetableSlotEntity>)

    @Query("DELETE FROM timetable_slots WHERE sectionKey = :sectionKey")
    suspend fun clearSectionTimetable(sectionKey: String)

    @Transaction
    suspend fun replaceSectionTimetable(sectionKey: String, newSlots: List<TimetableSlotEntity>) {
        clearSectionTimetable(sectionKey)
        upsertSlots(newSlots)
    }
}

@Dao
interface NoticeDao {
    @Query("SELECT * FROM notices WHERE sectionKey = :sectionKey ORDER BY isPinned DESC, createdAtMillis DESC")
    fun observeNotices(sectionKey: String): Flow<List<NoticeEntity>>

    @Query("SELECT COUNT(*) FROM notices WHERE sectionKey = :sectionKey AND isRead = 0")
    fun observeUnreadCount(sectionKey: String): Flow<Int>

    @Query("SELECT id FROM notices WHERE sectionKey = :sectionKey AND isRead = 1")
    suspend fun getReadNoticeIds(sectionKey: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotices(notices: List<NoticeEntity>)

    @Query("UPDATE notices SET isRead = 1 WHERE id = :noticeId")
    suspend fun markNoticeAsRead(noticeId: String)

    @Query("UPDATE notices SET isRead = 1 WHERE sectionKey = :sectionKey")
    suspend fun markAllAsRead(sectionKey: String)
}
