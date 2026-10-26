package `in`.campusone.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import `in`.campusone.app.data.local.dao.AttendanceDao
import `in`.campusone.app.data.local.dao.CampusLifeDao
import `in`.campusone.app.data.local.dao.NoticeDao
import `in`.campusone.app.data.local.dao.StudyDao
import `in`.campusone.app.data.local.dao.TimetableDao
import `in`.campusone.app.data.local.entity.AssignmentEntity
import `in`.campusone.app.data.local.entity.AttendanceLogEntity
import `in`.campusone.app.data.local.entity.CampusEventEntity
import `in`.campusone.app.data.local.entity.CampusMapPlaceEntity
import `in`.campusone.app.data.local.entity.ClubEntity
import `in`.campusone.app.data.local.entity.DirectoryContactEntity
import `in`.campusone.app.data.local.entity.DirectorySuggestionEntity
import `in`.campusone.app.data.local.entity.ExamEntity
import `in`.campusone.app.data.local.entity.GpaSemesterEntity
import `in`.campusone.app.data.local.entity.LostFoundContactRequestEntity
import `in`.campusone.app.data.local.entity.LostFoundEntity
import `in`.campusone.app.data.local.entity.NoticeEntity
import `in`.campusone.app.data.local.entity.StudyResourceEntity
import `in`.campusone.app.data.local.entity.SubjectEntity
import `in`.campusone.app.data.local.entity.SyllabusTopicEntity
import `in`.campusone.app.data.local.entity.TimetableSlotEntity
import `in`.campusone.app.domain.model.AttachmentType
import `in`.campusone.app.domain.model.AttendanceStatus
import `in`.campusone.app.domain.model.UserRole

class RoomConverters {
    @TypeConverter
    fun fromAttendanceStatus(value: AttendanceStatus): String = value.name

    @TypeConverter
    fun toAttendanceStatus(value: String): AttendanceStatus =
        runCatching { AttendanceStatus.valueOf(value) }.getOrDefault(AttendanceStatus.PRESENT)

    @TypeConverter
    fun fromAttachmentType(value: AttachmentType): String = value.name

    @TypeConverter
    fun toAttachmentType(value: String): AttachmentType =
        runCatching { AttachmentType.valueOf(value) }.getOrDefault(AttachmentType.NONE)

    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name

    @TypeConverter
    fun toUserRole(value: String): UserRole =
        runCatching { UserRole.valueOf(value) }.getOrDefault(UserRole.STUDENT)
}

@Database(
    entities = [
        SubjectEntity::class,
        AttendanceLogEntity::class,
        TimetableSlotEntity::class,
        NoticeEntity::class,
        StudyResourceEntity::class,
        AssignmentEntity::class,
        ExamEntity::class,
        SyllabusTopicEntity::class,
        GpaSemesterEntity::class,
        ClubEntity::class,
        CampusEventEntity::class,
        LostFoundEntity::class,
        LostFoundContactRequestEntity::class,
        DirectoryContactEntity::class,
        DirectorySuggestionEntity::class,
        CampusMapPlaceEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class CampusOneDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao
    abstract fun timetableDao(): TimetableDao
    abstract fun noticeDao(): NoticeDao
    abstract fun studyDao(): StudyDao
    abstract fun campusLifeDao(): CampusLifeDao
}
