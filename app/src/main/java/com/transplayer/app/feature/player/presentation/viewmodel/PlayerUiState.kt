package com.transplayer.app.feature.player.presentation.viewmodel

import com.transplayer.app.feature.player.domain.model.PlaybackState
import com.transplayer.app.feature.player.domain.model.VideoSource

data class PlayerUiState(
    val videoSource: VideoSource? = null,
    val playbackState: PlaybackState = PlaybackState(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isControlsVisible: Boolean = true,
    val isFullscreen: Boolean = false,
    val isSpeedMenuVisible: Boolean = false,
    val isAspectRatioMenuVisible: Boolean = false,
    val isVolumeControlVisible: Boolean = false,
    val isBrightnessControlVisible: Boolean = false,
    val currentBrightness: Float = 0.5f, // 0.0 - 1.0
    val currentVolume: Float = 1.0f, // 0.0 - 1.0
    val videoAspectRatio: VideoAspectRatio = VideoAspectRatio.FIT
)

enum class VideoAspectRatio {
    FIT,           // 适应屏幕
    ORIGINAL,      // 原始比例
    SIXTEEN_NINE,  // 16:9
    FOUR_THREE,    // 4:3
    FILL           // 填充屏幕
}

