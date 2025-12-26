package com.transplayer.app.data.local.dao

import androidx.room.*
import com.transplayer.app.data.local.entity.PlaybackHistoryEntity
import com.transplayer.app.feature.player.domain.model.VideoSource
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackHistoryDao {
    
    @Query("SELECT * FROM playback_history ORDER BY last_played_time DESC")
    fun getAllHistory(): Flow<List<PlaybackHistoryEntity>>
    
    @Query("SELECT * FROM playback_history WHERE id = :id")
    suspend fun getHistoryById(id: Long): PlaybackHistoryEntity?
    
    // 注意：Room 不支持直接查询复杂类型，需要通过其他字段查询
    // 这里使用视频标题和路径来查找
    @Query("SELECT * FROM playback_history WHERE video_title = :title LIMIT 1")
    suspend fun getHistoryByTitle(title: String): PlaybackHistoryEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: PlaybackHistoryEntity): Long
    
    @Update
    suspend fun updateHistory(history: PlaybackHistoryEntity)
    
    @Query("DELETE FROM playback_history WHERE id = :id")
    suspend fun deleteHistory(id: Long)
    
    @Query("DELETE FROM playback_history")
    suspend fun deleteAllHistory()
    
    @Query("SELECT * FROM playback_history ORDER BY last_played_time DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): Flow<List<PlaybackHistoryEntity>>
}

