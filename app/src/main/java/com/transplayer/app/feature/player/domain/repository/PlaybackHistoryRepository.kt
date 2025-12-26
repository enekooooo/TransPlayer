package com.transplayer.app.feature.player.domain.repository

import com.transplayer.app.data.local.entity.PlaybackHistoryEntity
import com.transplayer.app.feature.player.domain.model.VideoSource
import kotlinx.coroutines.flow.Flow

interface PlaybackHistoryRepository {
    fun getAllHistory(): Flow<List<PlaybackHistoryEntity>>
    suspend fun getHistoryByVideoSource(videoSource: VideoSource): PlaybackHistoryEntity?
    suspend fun savePlaybackHistory(
        videoSource: VideoSource,
        videoTitle: String,
        position: Long,
        duration: Long
    )
    suspend fun deleteHistory(id: Long)
    suspend fun deleteAllHistory()
    fun getRecentHistory(limit: Int): Flow<List<PlaybackHistoryEntity>>
}

