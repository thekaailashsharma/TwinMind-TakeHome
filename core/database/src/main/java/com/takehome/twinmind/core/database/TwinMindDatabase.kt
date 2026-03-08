package com.takehome.twinmind.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.takehome.twinmind.core.database.dao.AudioChunkDao
import com.takehome.twinmind.core.database.dao.SessionDao
import com.takehome.twinmind.core.database.dao.SummaryDao
import com.takehome.twinmind.core.database.dao.TranscriptDao
import com.takehome.twinmind.core.database.entity.AudioChunkEntity
import com.takehome.twinmind.core.database.entity.SessionEntity
import com.takehome.twinmind.core.database.entity.SummaryEntity
import com.takehome.twinmind.core.database.entity.TranscriptSegmentEntity

@Database(
    entities = [
        SessionEntity::class,
        AudioChunkEntity::class,
        TranscriptSegmentEntity::class,
        SummaryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class TwinMindDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun audioChunkDao(): AudioChunkDao
    abstract fun transcriptDao(): TranscriptDao
    abstract fun summaryDao(): SummaryDao
}
