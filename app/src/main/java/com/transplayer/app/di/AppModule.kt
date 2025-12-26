package com.transplayer.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.transplayer.app.data.local.TransPlayerDatabase
import com.transplayer.app.data.local.dao.PlaybackHistoryDao
import com.transplayer.app.feature.player.data.repository.PlaybackHistoryRepositoryImpl
import com.transplayer.app.feature.player.data.repository.VideoRepositoryImpl
import com.transplayer.app.feature.player.domain.repository.PlaybackHistoryRepository
import com.transplayer.app.feature.player.domain.repository.VideoRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TransPlayerDatabase {
        return Room.databaseBuilder(
            context,
            TransPlayerDatabase::class.java,
            "transplayer.db"
        )
        .addMigrations(MIGRATION_1_2)
        .fallbackToDestructiveMigration() // 开发阶段：如果迁移失败，删除并重新创建数据库
        .build()
    }
    
    // 数据库迁移：从版本 1 到版本 2
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // 创建 playback_history 表
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS playback_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    video_source TEXT NOT NULL,
                    video_title TEXT NOT NULL,
                    last_position INTEGER NOT NULL,
                    duration INTEGER NOT NULL,
                    last_played_time INTEGER NOT NULL,
                    thumbnail_path TEXT
                )
            """.trimIndent())
        }
    }
    
    @Provides
    @Singleton
    fun provideVideoRepository(
        videoRepositoryImpl: VideoRepositoryImpl
    ): VideoRepository = videoRepositoryImpl
    
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
    
    @Provides
    @Singleton
    fun providePlaybackHistoryDao(database: TransPlayerDatabase): PlaybackHistoryDao {
        return database.playbackHistoryDao()
    }
    
    @Provides
    @Singleton
    fun providePlaybackHistoryRepository(
        repositoryImpl: PlaybackHistoryRepositoryImpl
    ): PlaybackHistoryRepository = repositoryImpl
}





