package com.takehome.twinmind.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.takehome.twinmind.core.database.entity.TranscriptSegmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptDao {

    @Query("SELECT * FROM transcript_segments WHERE sessionId = :sessionId ORDER BY segmentIndex ASC")
    fun observeBySession(sessionId: String): Flow<List<TranscriptSegmentEntity>>

    @Query("SELECT * FROM transcript_segments WHERE chunkId = :chunkId ORDER BY segmentIndex ASC")
    suspend fun getByChunk(chunkId: String): List<TranscriptSegmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(segments: List<TranscriptSegmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(segment: TranscriptSegmentEntity)

    @Query(
        """
        SELECT GROUP_CONCAT(text, ' ') FROM transcript_segments 
        WHERE sessionId = :sessionId 
        ORDER BY segmentIndex ASC
        """,
    )
    suspend fun getFullTranscript(sessionId: String): String?

    @Query("DELETE FROM transcript_segments WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: String)
}
