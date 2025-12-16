package com.transplayer.app.feature.player.domain.usecase

import com.transplayer.app.feature.player.domain.model.VideoSource
import com.transplayer.app.feature.player.domain.repository.VideoRepository
import javax.inject.Inject

class PlayVideoUseCase @Inject constructor(
    private val videoRepository: VideoRepository
) {
    suspend operator fun invoke(source: VideoSource): Result<VideoSource> {
        return videoRepository.validateVideoSource(source)
    }
}

