package `in`.campusone.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import `in`.campusone.app.data.local.dao.CoreDao
import `in`.campusone.app.data.local.dao.StudyAndCampusDao
import `in`.campusone.app.data.local.entity.AssignmentEntity
import `in`.campusone.app.data.local.entity.AttendanceRecordEntity
import `in`.campusone.app.data.local.entity.CourseGradeEntity
import `in`.campusone.app.data.local.entity.DirectoryContactEntity
import `in`.campusone.app.data.local.entity.ExamEntity
import `in`.campusone.app.data.local.entity.PersonalNoticeEntity
import `in`.campusone.app.data.local.entity.StudyMaterialEntity
import `in`.campusone.app.data.local.entity.SubjectEntity
import `in`.campusone.app.data.local.entity.SyllabusTopicEntity
import `in`.campusone.app.data.local.entity.TimetableSlotEntity

/**
 * On-Device Room Database for CampusOne.
 * Uses explicit non-destructive SQL migrations (`MIGRATION_1_2`) and `exportSchema = true`.
 * Uses explicit non-destructive SQL migrations so student records are never silently wiped.
 */
@Database(
    entities = [
        SubjectEntity::class,
        AttendanceRecordEntity::class,
        TimetableSlotEntity::class,
        PersonalNoticeEntity::class,
        AssignmentEntity::class,
        ExamEntity::class,
        SyllabusTopicEntity::class,
        CourseGradeEntity::class,
        StudyMaterialEntity::class,
        DirectoryContactEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class CampusOneDatabase : RoomDatabase() {

    abstract fun coreDao(): CoreDao
    abstract fun studyAndCampusDao(): StudyAndCampusDao

    companion object {
        private const val DB_NAME = "campusone_ondevice.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `directory_contacts` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `designation` TEXT NOT NULL,
                        `department` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `phone` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `officeLocation` TEXT NOT NULL,
                        `officeHours` TEXT NOT NULL,
                        `latitude` REAL NOT NULL DEFAULT 30.2224,
                        `longitude` REAL NOT NULL DEFAULT 78.7834,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_directory_contacts_category_department` ON `directory_contacts` (`category`, `department`)"
                )
            }
        }

        @Volatile
        private var INSTANCE: CampusOneDatabase? = null

        fun getInstance(context: Context): CampusOneDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CampusOneDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
