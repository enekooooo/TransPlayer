package com.transplayer.app.feature.player.data.repository

import com.transplayer.app.data.local.dao.PlaybackHistoryDao
import com.transplayer.app.data.local.entity.PlaybackHistoryEntity
import com.transplayer.app.feature.player.domain.model.VideoSource
import com.transplayer.app.feature.player.domain.repository.PlaybackHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PlaybackHistoryRepositoryImpl @Inject constructor(
    private val dao: PlaybackHistoryDao
) : PlaybackHistoryRepository {
    
    override fun getAllHistory(): Flow<List<PlaybackHistoryEntity>> {
        return dao.getAllHistory()
    }
    
    override suspend fun getHistoryByVideoSource(videoSource: VideoSource): PlaybackHistoryEntity? {
        // 通过视频源生成唯一标识来查找
        val identifier = when (videoSource) {
            is VideoSource.Local -> videoSource.path
            is VideoSource.Remote -> videoSource.url
        }
        // 简化实现：通过路径/URL查找，实际应该使用更精确的匹配
        return dao.getHistoryByTitle(identifier)
    }
    
    override suspend fun savePlaybackHistory(
        videoSource: VideoSource,
        videoTitle: String,
        position: Long,
        duration: Long
    ) {
        val existing = getHistoryByVideoSource(videoSource)
        val history = PlaybackHistoryEntity(
            id = existing?.id ?: 0L,
            videoSource = videoSource,
            videoTitle = videoTitle,
            lastPosition = position,
            duration = duration,
            lastPlayedTime = System.currentTimeMillis()
        )
        dao.insertHistory(history)
    }
    
    override suspend fun deleteHistory(id: Long) {
        dao.deleteHistory(id)
    }
    
    override suspend fun deleteAllHistory() {
        dao.deleteAllHistory()
    }
    
    override fun getRecentHistory(limit: Int): Flow<List<PlaybackHistoryEntity>> {
        return dao.getRecentHistory(limit)
    }
}

