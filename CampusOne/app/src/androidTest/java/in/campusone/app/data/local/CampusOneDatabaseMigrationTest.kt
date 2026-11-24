package `in`.campusone.app.data.local

import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import `in`.campusone.app.data.local.entity.*
import `in`.campusone.app.domain.model.AttachmentType
import `in`.campusone.app.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Mirrors the schema shipped with database version 3 (before fileHash,
 * notice audience fields, community tables and local notifications).
 */
@Entity(
    tableName = "study_resources",
    indices = [
        Index(value = ["sectionKey", "subjectId", "semester"]),
        Index(value = ["isBookmarkedOffline"])
    ]
)
data class LegacyStudyResourceEntityV3(
    @PrimaryKey val id: String,
    val sectionKey: String,
    val subjectId: String,
    val subjectName: String,
    val semester: Int,
    val title: String,
    val category: String,
    val examYear: Int = 2025,
    val fileUrl: String,
    val localFilePath: String? = null,
    val fileType: String,
    val fileSizeBytes: Long = 1_450_000L,
    val uploaderUid: String,
    val uploaderName: String,
    val showUploaderName: Boolean = true,
    val upvotes: Int = 0,
    val isUpvotedByMe: Boolean = false,
    val isBookmarkedOffline: Boolean = false,
    val reportCount: Int = 0,
    val isFlagged: Boolean = false,
    val isHidden: Boolean = false,
    val isApprovedByCr: Boolean = true,
    val uploadStatus: String = "COMPLETED",
    val uploadProgressPct: Int = 100,
    val createdAtMillis: Long = 1L,
    val updatedAtMillis: Long = 1L
)

@Entity(
    tableName = "notices",
    indices = [Index(value = ["sectionKey", "isPinned", "createdAtMillis"])]
)
data class LegacyNoticeEntityV4(
    @PrimaryKey val id: String,
    val sectionKey: String,
    val title: String,
    val body: String,
    val attachmentUrl: String? = null,
    val attachmentType: AttachmentType = AttachmentType.NONE,
    val isPinned: Boolean = false,
    val authorUid: String,
    val authorName: String,
    val authorRole: UserRole,
    val createdAtMillis: Long,
    val isRead: Boolean = false
)

@Database(
    entities = [
        SubjectEntity::class,
        AttendanceLogEntity::class,
        TimetableSlotEntity::class,
        LegacyNoticeEntityV4::class,
        LegacyStudyResourceEntityV3::class,
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
abstract class LegacyCampusOneDatabaseV3 : RoomDatabase()

@RunWith(AndroidJUnit4::class)
class CampusOneDatabaseMigrationTest {

    @Test
    fun version3To5MigrationPreservesOfflineRowsAndAddsNewSchema() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "campusone-v3-to-v5-migration-test.db"
        context.deleteDatabase(databaseName)

        val legacy = Room.databaseBuilder(
            context,
            LegacyCampusOneDatabaseV3::class.java,
            databaseName
        ).allowMainThreadQueries().build()

        legacy.openHelper.writableDatabase.execSQL(
            """
            INSERT INTO subjects (
                subjectId, sectionKey, subjectName, subjectCode, teacherName,
                isElective, isEnrolled, credits
            ) VALUES (
                'subject-1', 'section-A', 'Biology', 'BIO-101', 'Faculty', 0, 1, 4
            )
            """.trimIndent()
        )
        legacy.openHelper.writableDatabase.execSQL(
            """
            INSERT INTO study_resources (
                id, sectionKey, subjectId, subjectName, semester, title, category,
                examYear, fileUrl, localFilePath, fileType, fileSizeBytes, uploaderUid,
                uploaderName, showUploaderName, upvotes, isUpvotedByMe,
                isBookmarkedOffline, reportCount, isFlagged, isHidden, isApprovedByCr,
                uploadStatus, uploadProgressPct, createdAtMillis, updatedAtMillis
            ) VALUES (
                'resource-1', 'section-A', 'subject-1', 'Biology', 1, 'Unit Notes',
                'NOTES', 2026, 'https://example.invalid/notes.pdf', NULL, 'PDF', 1234,
                'student-1', 'Student', 1, 2, 0, 1, 0, 0, 0, 1, 'COMPLETED', 100, 123, 124
            )
            """.trimIndent()
        )
        legacy.openHelper.writableDatabase.execSQL(
            """
            INSERT INTO notices (
                id, sectionKey, title, body, attachmentUrl, attachmentType, isPinned,
                authorUid, authorName, authorRole, createdAtMillis, isRead
            ) VALUES (
                'notice-1', 'section-A', 'Exam notice', 'Bring your admit card',
                NULL, 'NONE', 1, 'admin-1', 'Admin', 'ADMIN', 125, 0
            )
            """.trimIndent()
        )
        legacy.close()

        val upgraded = Room.databaseBuilder(
            context,
            CampusOneDatabase::class.java,
            databaseName
        )
            .addMigrations(
                CampusOneMigrations.MIGRATION_3_4,
                CampusOneMigrations.MIGRATION_4_5
            )
            .allowMainThreadQueries()
            .build()

        try {
            // Opening the current database performs Room's full schema validation.
            val database = upgraded.openHelper.writableDatabase

            database.query(
                "SELECT subjectName FROM subjects WHERE subjectId = 'subject-1'"
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Biology", cursor.getString(0))
            }

            database.query(
                "SELECT title, fileHash FROM study_resources WHERE id = 'resource-1'"
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Unit Notes", cursor.getString(0))
                assertEquals("", cursor.getString(1))
            }

            database.query(
                """
                SELECT title, body, category, priority, isOfficial
                FROM notices WHERE id = 'notice-1'
                """.trimIndent()
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Exam notice", cursor.getString(0))
                assertEquals("Bring your admit card", cursor.getString(1))
                assertEquals("GENERAL", cursor.getString(2))
                assertEquals("NORMAL", cursor.getString(3))
                assertEquals(0, cursor.getInt(4))
            }

            listOf("community_posts", "community_comments", "app_notifications").forEach { table ->
                database.query(
                    "SELECT name FROM sqlite_master WHERE type = 'table' AND name = '$table'"
                ).use { cursor ->
                    assertTrue("Missing migrated table: $table", cursor.moveToFirst())
                }
            }
        } finally {
            upgraded.close()
            context.deleteDatabase(databaseName)
        }
    }
}
