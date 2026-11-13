package `in`.campusone.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import `in`.campusone.app.data.local.CampusOneDatabase
import `in`.campusone.app.data.local.dao.AttendanceDao
import `in`.campusone.app.data.local.dao.CampusLifeDao
import `in`.campusone.app.data.local.dao.NoticeDao
import `in`.campusone.app.data.local.dao.StudyDao
import `in`.campusone.app.data.local.dao.TimetableDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE study_resources ADD COLUMN fileHash TEXT NOT NULL DEFAULT ''")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CampusOneDatabase {
        return Room.databaseBuilder(
            context,
            CampusOneDatabase::class.java,
            "campusone_offline.db"
        ).addMigrations(MIGRATION_3_4).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideAttendanceDao(db: CampusOneDatabase): AttendanceDao = db.attendanceDao()

    @Provides
    fun provideTimetableDao(db: CampusOneDatabase): TimetableDao = db.timetableDao()

    @Provides
    fun provideNoticeDao(db: CampusOneDatabase): NoticeDao = db.noticeDao()

    @Provides
    fun provideStudyDao(db: CampusOneDatabase): StudyDao = db.studyDao()

    @Provides
    fun provideCampusLifeDao(db: CampusOneDatabase): CampusLifeDao = db.campusLifeDao()
}
