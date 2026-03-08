package com.takehome.twinmind.core.data.repository

import com.takehome.twinmind.core.database.dao.SummaryDao
import com.takehome.twinmind.core.database.mapper.toDomain
import com.takehome.twinmind.core.database.mapper.toEntity
import com.takehome.twinmind.core.model.Summary
import com.takehome.twinmind.core.model.SummaryStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SummaryRepository @Inject constructor(
    private val summaryDao: SummaryDao,
) {
    fun observeBySession(sessionId: String): Flow<Summary?> =
        summaryDao.observeBySession(sessionId).map { it?.toDomain() }

    suspend fun getBySession(sessionId: String): Summary? =
        summaryDao.getBySession(sessionId)?.toDomain()

    suspend fun save(summary: Summary) {
        summaryDao.upsert(summary.toEntity())
    }

    suspend fun updateStatus(summaryId: String, status: SummaryStatus) {
        summaryDao.updateStatus(summaryId, status.name)
    }

    suspend fun deleteBySession(sessionId: String) {
        summaryDao.deleteBySession(sessionId)
    }
}
