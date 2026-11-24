package `in`.campusone.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Explicit, data-preserving Room migrations for every shipped database version.
 *
 * These migrations rebuild only the affected tables and copy every existing row.
 * They intentionally avoid SQLite column DEFAULT clauses that Room would later
 * consider part of the schema even though the entity fields have no @ColumnInfo default.
 */
object CampusOneMigrations {

    /**
     * Version 3 -> 4: StudyResourceEntity gained fileHash.
     * Rebuild the table so the new NOT NULL column has no persistent SQL default.
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `study_resources_new` (
                    `id` TEXT NOT NULL,
                    `sectionKey` TEXT NOT NULL,
                    `subjectId` TEXT NOT NULL,
                    `subjectName` TEXT NOT NULL,
                    `semester` INTEGER NOT NULL,
                    `title` TEXT NOT NULL,
                    `category` TEXT NOT NULL,
                    `examYear` INTEGER NOT NULL,
                    `fileUrl` TEXT NOT NULL,
                    `fileHash` TEXT NOT NULL,
                    `localFilePath` TEXT,
                    `fileType` TEXT NOT NULL,
                    `fileSizeBytes` INTEGER NOT NULL,
                    `uploaderUid` TEXT NOT NULL,
                    `uploaderName` TEXT NOT NULL,
                    `showUploaderName` INTEGER NOT NULL,
                    `upvotes` INTEGER NOT NULL,
                    `isUpvotedByMe` INTEGER NOT NULL,
                    `isBookmarkedOffline` INTEGER NOT NULL,
                    `reportCount` INTEGER NOT NULL,
                    `isFlagged` INTEGER NOT NULL,
                    `isHidden` INTEGER NOT NULL,
                    `isApprovedByCr` INTEGER NOT NULL,
                    `uploadStatus` TEXT NOT NULL,
                    `uploadProgressPct` INTEGER NOT NULL,
                    `createdAtMillis` INTEGER NOT NULL,
                    `updatedAtMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `study_resources_new` (
                    `id`, `sectionKey`, `subjectId`, `subjectName`, `semester`,
                    `title`, `category`, `examYear`, `fileUrl`, `fileHash`,
                    `localFilePath`, `fileType`, `fileSizeBytes`, `uploaderUid`,
                    `uploaderName`, `showUploaderName`, `upvotes`, `isUpvotedByMe`,
                    `isBookmarkedOffline`, `reportCount`, `isFlagged`, `isHidden`,
                    `isApprovedByCr`, `uploadStatus`, `uploadProgressPct`,
                    `createdAtMillis`, `updatedAtMillis`
                )
                SELECT
                    `id`, `sectionKey`, `subjectId`, `subjectName`, `semester`,
                    `title`, `category`, `examYear`, `fileUrl`, '',
                    `localFilePath`, `fileType`, `fileSizeBytes`, `uploaderUid`,
                    `uploaderName`, `showUploaderName`, `upvotes`, `isUpvotedByMe`,
                    `isBookmarkedOffline`, `reportCount`, `isFlagged`, `isHidden`,
                    `isApprovedByCr`, `uploadStatus`, `uploadProgressPct`,
                    `createdAtMillis`, `updatedAtMillis`
                FROM `study_resources`
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `study_resources`")
            db.execSQL("ALTER TABLE `study_resources_new` RENAME TO `study_resources`")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_study_resources_sectionKey_subjectId_semester` " +
                    "ON `study_resources` (`sectionKey`, `subjectId`, `semester`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_study_resources_isBookmarkedOffline` " +
                    "ON `study_resources` (`isBookmarkedOffline`)"
            )
        }
    }

    /**
     * Version 4 -> 5: notices gained three audience/display fields and three local tables
     * were introduced. Existing notice rows are copied with safe legacy defaults.
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `notices_new` (
                    `id` TEXT NOT NULL,
                    `sectionKey` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `body` TEXT NOT NULL,
                    `attachmentUrl` TEXT,
                    `attachmentType` TEXT NOT NULL,
                    `isPinned` INTEGER NOT NULL,
                    `category` TEXT NOT NULL,
                    `priority` TEXT NOT NULL,
                    `isOfficial` INTEGER NOT NULL,
                    `authorUid` TEXT NOT NULL,
                    `authorName` TEXT NOT NULL,
                    `authorRole` TEXT NOT NULL,
                    `createdAtMillis` INTEGER NOT NULL,
                    `isRead` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `notices_new` (
                    `id`, `sectionKey`, `title`, `body`, `attachmentUrl`,
                    `attachmentType`, `isPinned`, `category`, `priority`, `isOfficial`,
                    `authorUid`, `authorName`, `authorRole`, `createdAtMillis`, `isRead`
                )
                SELECT
                    `id`, `sectionKey`, `title`, `body`, `attachmentUrl`,
                    `attachmentType`, `isPinned`, 'GENERAL', 'NORMAL', 0,
                    `authorUid`, `authorName`, `authorRole`, `createdAtMillis`, `isRead`
                FROM `notices`
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `notices`")
            db.execSQL("ALTER TABLE `notices_new` RENAME TO `notices`")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_notices_sectionKey_isPinned_createdAtMillis` " +
                    "ON `notices` (`sectionKey`, `isPinned`, `createdAtMillis`)"
            )

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
}
