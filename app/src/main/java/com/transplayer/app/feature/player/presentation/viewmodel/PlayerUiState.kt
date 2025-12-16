package com.transplayer.app.feature.player.presentation.viewmodel

import com.transplayer.app.feature.player.domain.model.PlaybackState
import com.transplayer.app.feature.player.domain.model.VideoSource

data class PlayerUiState(
    val videoSource: VideoSource? = null,
    val playbackState: PlaybackState = PlaybackState(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isControlsVisible: Boolean = true,
    val isFullscreen: Boolean = false
)

