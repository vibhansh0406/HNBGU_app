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
import `in`.campusone.app.data.local.dao.CommunityDao
import `in`.campusone.app.data.local.dao.NoticeDao
import `in`.campusone.app.data.local.dao.StudyDao
import `in`.campusone.app.data.local.dao.TimetableDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Version 3 -> 4: add the content hash used for offline resource deduplication.
     */
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE study_resources ADD COLUMN fileHash TEXT NOT NULL DEFAULT ''"
            )
        }
    }

    /**
     * Version 4 -> 5: community and in-app notifications were added.
     *
     * Keep this migration additive. Existing attendance, timetable, notices,
     * study resources and campus data must remain untouched during an update.
     */
    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `community_posts` (
                    `id` TEXT NOT NULL,
                    `collegeId` TEXT NOT NULL,
                    `authorUid` TEXT NOT NULL,
                    `authorName` TEXT NOT NULL,
                    `authorRole` TEXT NOT NULL,
                    `category` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `body` TEXT NOT NULL,
                    `likeCount` INTEGER NOT NULL,
                    `commentCount` INTEGER NOT NULL,
                    `isLiked` INTEGER NOT NULL,
                    `createdAtMillis` INTEGER NOT NULL,
                    `updatedAtMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_community_posts_createdAtMillis` " +
                    "ON `community_posts` (`createdAtMillis`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_community_posts_category_createdAtMillis` " +
                    "ON `community_posts` (`category`, `createdAtMillis`)"
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `community_comments` (
                    `id` TEXT NOT NULL,
                    `postId` TEXT NOT NULL,
                    `authorUid` TEXT NOT NULL,
                    `authorName` TEXT NOT NULL,
                    `authorRole` TEXT NOT NULL,
                    `body` TEXT NOT NULL,
                    `createdAtMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_community_comments_postId_createdAtMillis` " +
                    "ON `community_comments` (`postId`, `createdAtMillis`)"
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `app_notifications` (
                    `id` TEXT NOT NULL,
                    `userId` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `body` TEXT NOT NULL,
                    `targetType` TEXT NOT NULL,
                    `targetId` TEXT NOT NULL,
                    `isRead` INTEGER NOT NULL,
                    `createdAtMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_app_notifications_userId_isRead_createdAtMillis` " +
                    "ON `app_notifications` (`userId`, `isRead`, `createdAtMillis`)"
            )
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CampusOneDatabase {
        return Room.databaseBuilder(
            context,
            CampusOneDatabase::class.java,
            "campusone_offline.db"
        )
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
            // Do not use fallbackToDestructiveMigration(): it deletes offline user data.
            // A missing migration must fail visibly rather than wipe attendance or saved work.
            .build()
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

    @Provides
    fun provideCommunityDao(db: CampusOneDatabase): CommunityDao = db.communityDao()
}
