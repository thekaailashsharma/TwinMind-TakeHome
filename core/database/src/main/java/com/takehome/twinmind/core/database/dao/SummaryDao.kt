package com.takehome.twinmind.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.takehome.twinmind.core.database.entity.SummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {

    @Query("SELECT * FROM summaries WHERE sessionId = :sessionId")
    fun observeBySession(sessionId: String): Flow<SummaryEntity?>

    @Query("SELECT * FROM summaries WHERE sessionId = :sessionId")
    suspend fun getBySession(sessionId: String): SummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(summary: SummaryEntity)

    @Query("UPDATE summaries SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM summaries WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: String)
}
