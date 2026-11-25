package `in`.campusone.app.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CampusOneDatabaseMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseNames = mutableListOf<String>()

    @After
    fun cleanUp() {
        databaseNames.forEach { name -> context.deleteDatabase(name) }
    }

    @Test
    fun upgradeFromVersionFourKeepsCachedNoticeAndCreatesNewTables() {
        val database = upgradeFrom(4)
        try {
            assertNoticeDefaults(database)
            assertNewTablesAndIndexes(database)
        } finally {
            database.close()
        }
    }

    @Test
    fun upgradeFromVersionThreeRunsBothMigrationsWithoutLosingData() {
        val database = upgradeFrom(3)
        try {
            assertNoticeDefaults(database)
            assertNewTablesAndIndexes(database)

            database.query(
                "SELECT fileHash FROM study_resources WHERE id = ?",
                arrayOf("resource-1")
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("", cursor.getString(0))
            }
        } finally {
            database.close()
        }
    }

    private fun upgradeFrom(sourceVersion: Int): SupportSQLiteDatabase {
        val name = "campusone-migration-$sourceVersion.db"
        databaseNames += name
        context.deleteDatabase(name)

        val factory = FrameworkSQLiteOpenHelperFactory()
        val legacyHelper = factory.create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(sourceVersion) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        createLegacyNoticesTable(db)
                        if (sourceVersion == 3) {
                            db.execSQL(
                                """
                                CREATE TABLE study_resources (
                                    id TEXT NOT NULL PRIMARY KEY,
                                    title TEXT NOT NULL
                                )
                                """.trimIndent()
                            )
                            db.execSQL(
                                "INSERT INTO study_resources (id, title) VALUES ('resource-1', 'Saved notes')"
                            )
                        }
                        db.execSQL(
                            """
                            INSERT INTO notices (
                                id, sectionKey, title, body, attachmentUrl, attachmentType,
                                isPinned, authorUid, authorName, authorRole, createdAtMillis, isRead
                            ) VALUES (
                                'notice-1', 'section-a', 'Saved notice', 'Keep this notice',
                                NULL, 'NONE', 0, 'user-1', 'Student', 'STUDENT', 1000, 0
                            )
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int
                    ) = error("Unexpected legacy database upgrade")
                })
                .build()
        )
        legacyHelper.writableDatabase
        legacyHelper.close()

        val currentHelper = factory.create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(5) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        error("Expected to upgrade the existing database")
                    }

                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int
                    ) {
                        when (oldVersion) {
                            3 -> {
                                MigrationThreeToFour.migrate(db)
                                MigrationFourToFive.migrate(db)
                            }
                            4 -> MigrationFourToFive.migrate(db)
                            else -> error("No test migration for database version $oldVersion")
                        }
                    }
                })
                .build()
        )
        return currentHelper.writableDatabase
    }

    private fun createLegacyNoticesTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE notices (
                id TEXT NOT NULL PRIMARY KEY,
                sectionKey TEXT NOT NULL,
                title TEXT NOT NULL,
                body TEXT NOT NULL,
                attachmentUrl TEXT,
                attachmentType TEXT NOT NULL,
                isPinned INTEGER NOT NULL,
                authorUid TEXT NOT NULL,
                authorName TEXT NOT NULL,
                authorRole TEXT NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                isRead INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX index_notices_sectionKey_isPinned_createdAtMillis " +
                "ON notices(sectionKey, isPinned, createdAtMillis)"
        )
    }

    private fun assertNoticeDefaults(db: SupportSQLiteDatabase) {
        db.query(
            "SELECT category, priority, isOfficial, title FROM notices WHERE id = ?",
            arrayOf("notice-1")
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("GENERAL", cursor.getString(0))
            assertEquals("NORMAL", cursor.getString(1))
            assertEquals(0, cursor.getInt(2))
            assertEquals("Saved notice", cursor.getString(3))
        }
    }

    private fun assertNewTablesAndIndexes(db: SupportSQLiteDatabase) {
        listOf("community_posts", "community_comments", "app_notifications").forEach { table ->
            db.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
                arrayOf(table)
            ).use { cursor ->
                assertTrue("Missing table: $table", cursor.moveToFirst())
            }
        }

        listOf(
            "index_community_posts_createdAtMillis",
            "index_community_posts_category_createdAtMillis",
            "index_community_comments_postId_createdAtMillis",
            "index_app_notifications_userId_isRead_createdAtMillis"
        ).forEach { index ->
            db.query(
                "SELECT name FROM sqlite_master WHERE type = 'index' AND name = ?",
                arrayOf(index)
            ).use { cursor ->
                assertTrue("Missing index: $index", cursor.moveToFirst())
            }
        }
    }
}
