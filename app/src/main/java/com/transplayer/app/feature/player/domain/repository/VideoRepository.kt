package com.transplayer.app.feature.player.domain.repository

import com.transplayer.app.feature.player.domain.model.VideoSource

interface VideoRepository {
    suspend fun validateVideoSource(source: VideoSource): Result<VideoSource>
    suspend fun getVideoInfo(source: VideoSource): VideoInfo?
}

data class VideoInfo(
    val title: String,
    val duration: Long,
    val width: Int = 0,
    val height: Int = 0
)

