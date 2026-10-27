package com.campusone.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.campusone.app.data.local.entity.AttendanceRecordEntity
import com.campusone.app.data.local.entity.PersonalNoticeEntity
import com.campusone.app.data.local.entity.SubjectEntity
import com.campusone.app.data.local.entity.TimetableSlotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CoreDao {

    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun observeSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects ORDER BY name ASC")
    suspend fun getAllSubjectsOnce(): List<SubjectEntity>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: String): SubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubject(subject: SubjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubjects(subjects: List<SubjectEntity>)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubject(id: String)

    @Query("SELECT * FROM attendance_records ORDER BY timestampMillis DESC LIMIT 100")
    fun observeRecentAttendanceRecords(): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records ORDER BY timestampMillis DESC")
    suspend fun getAllAttendanceRecordsOnce(): List<AttendanceRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceRecord(record: AttendanceRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceRecords(records: List<AttendanceRecordEntity>)

    @Query("SELECT * FROM timetable_slots ORDER BY dayOfWeek ASC, startTime ASC")
    fun observeTimetableSlots(): Flow<List<TimetableSlotEntity>>

    @Query("SELECT * FROM timetable_slots ORDER BY dayOfWeek ASC, startTime ASC")
    suspend fun getAllTimetableSlotsOnce(): List<TimetableSlotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTimetableSlot(slot: TimetableSlotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTimetableSlots(slots: List<TimetableSlotEntity>)

    @Query("DELETE FROM timetable_slots WHERE id = :id")
    suspend fun deleteTimetableSlot(id: String)

    @Query("SELECT * FROM personal_notices ORDER BY isPinned DESC, createdAtMillis DESC")
    fun observePersonalNotices(): Flow<List<PersonalNoticeEntity>>

    @Query("SELECT * FROM personal_notices ORDER BY isPinned DESC, createdAtMillis DESC")
    suspend fun getAllPersonalNoticesOnce(): List<PersonalNoticeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPersonalNotice(notice: PersonalNoticeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPersonalNotices(notices: List<PersonalNoticeEntity>)

    @Query("UPDATE personal_notices SET isRead = 1 WHERE id = :id")
    suspend fun markNoticeRead(id: String)

    @Query("DELETE FROM personal_notices WHERE id = :id")
    suspend fun deletePersonalNotice(id: String)
}
