package com.takehome.twinmind.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.takehome.twinmind.core.database.dao.AudioChunkDao
import com.takehome.twinmind.core.database.dao.ChatMessageDao
import com.takehome.twinmind.core.database.dao.SessionDao
import com.takehome.twinmind.core.database.dao.SummaryDao
import com.takehome.twinmind.core.database.dao.TranscriptDao
import com.takehome.twinmind.core.database.entity.AudioChunkEntity
import com.takehome.twinmind.core.database.entity.ChatMessageEntity
import com.takehome.twinmind.core.database.entity.SessionEntity
import com.takehome.twinmind.core.database.entity.SummaryEntity
import com.takehome.twinmind.core.database.entity.TranscriptSegmentEntity

@Database(
    entities = [
        SessionEntity::class,
        AudioChunkEntity::class,
        TranscriptSegmentEntity::class,
        SummaryEntity::class,
        ChatMessageEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class TwinMindDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun audioChunkDao(): AudioChunkDao
    abstract fun transcriptDao(): TranscriptDao
    abstract fun summaryDao(): SummaryDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `chat_messages` (
                        `id` TEXT NOT NULL,
                        `sessionId` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `thinkingSummary` TEXT,
                        `thinkingDurationMs` INTEGER NOT NULL DEFAULT 0,
                        `modelName` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_chat_messages_sessionId` ON `chat_messages` (`sessionId`)",
                )
            }
        }
    }
}
