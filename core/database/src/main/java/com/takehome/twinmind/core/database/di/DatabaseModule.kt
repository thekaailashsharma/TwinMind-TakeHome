package com.takehome.twinmind.core.database.di

import android.content.Context
import androidx.room.Room
import com.takehome.twinmind.core.database.TwinMindDatabase
import com.takehome.twinmind.core.database.dao.AudioChunkDao
import com.takehome.twinmind.core.database.dao.ChatMessageDao
import com.takehome.twinmind.core.database.dao.SessionDao
import com.takehome.twinmind.core.database.dao.SummaryDao
import com.takehome.twinmind.core.database.dao.TranscriptDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TwinMindDatabase =
        Room.databaseBuilder(
            context,
            TwinMindDatabase::class.java,
            "twinmind.db",
        )
            .addMigrations(TwinMindDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideSessionDao(db: TwinMindDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideAudioChunkDao(db: TwinMindDatabase): AudioChunkDao = db.audioChunkDao()

    @Provides
    fun provideTranscriptDao(db: TwinMindDatabase): TranscriptDao = db.transcriptDao()

    @Provides
    fun provideSummaryDao(db: TwinMindDatabase): SummaryDao = db.summaryDao()

    @Provides
    fun provideChatMessageDao(db: TwinMindDatabase): ChatMessageDao = db.chatMessageDao()
}
