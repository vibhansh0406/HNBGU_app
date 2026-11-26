package `in`.campusone.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val MigrationThreeToFour = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE study_resources ADD COLUMN fileHash TEXT NOT NULL DEFAULT ''")
    }
}

internal val MigrationFourToFive = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notices ADD COLUMN category TEXT NOT NULL DEFAULT 'GENERAL'")
        db.execSQL("ALTER TABLE notices ADD COLUMN priority TEXT NOT NULL DEFAULT 'NORMAL'")
        db.execSQL("ALTER TABLE notices ADD COLUMN isOfficial INTEGER NOT NULL DEFAULT 0")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS community_posts (
                id TEXT NOT NULL,
                collegeId TEXT NOT NULL,
                authorUid TEXT NOT NULL,
                authorName TEXT NOT NULL,
                authorRole TEXT NOT NULL,
                category TEXT NOT NULL,
                title TEXT NOT NULL,
                body TEXT NOT NULL,
                likeCount INTEGER NOT NULL,
                commentCount INTEGER NOT NULL,
                isLiked INTEGER NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_community_posts_createdAtMillis " +
                "ON community_posts(createdAtMillis)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_community_posts_category_createdAtMillis " +
                "ON community_posts(category, createdAtMillis)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS community_comments (
                id TEXT NOT NULL,
                postId TEXT NOT NULL,
                authorUid TEXT NOT NULL,
                authorName TEXT NOT NULL,
                authorRole TEXT NOT NULL,
                body TEXT NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_community_comments_postId_createdAtMillis " +
                "ON community_comments(postId, createdAtMillis)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS app_notifications (
                id TEXT NOT NULL,
                userId TEXT NOT NULL,
                type TEXT NOT NULL,
                title TEXT NOT NULL,
                body TEXT NOT NULL,
                targetType TEXT NOT NULL,
                targetId TEXT NOT NULL,
                isRead INTEGER NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_app_notifications_userId_isRead_createdAtMillis " +
                "ON app_notifications(userId, isRead, createdAtMillis)"
        )
    }
}
