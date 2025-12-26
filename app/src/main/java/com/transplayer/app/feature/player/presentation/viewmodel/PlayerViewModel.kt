package com.transplayer.app.feature.player.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import android.app.Activity
import com.transplayer.app.data.local.UserPreferences
import com.transplayer.app.feature.player.data.BrightnessManager
import com.transplayer.app.feature.player.data.ExoPlayerManager
import com.transplayer.app.feature.player.domain.model.PlaybackState
import com.transplayer.app.feature.player.domain.model.VideoSource
import com.transplayer.app.feature.player.domain.repository.PlaybackHistoryRepository
import com.transplayer.app.feature.player.domain.usecase.PlayVideoUseCase
import com.transplayer.app.feature.player.presentation.viewmodel.VideoAspectRatio
import com.transplayer.app.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val exoPlayerManager: ExoPlayerManager,
    private val playVideoUseCase: PlayVideoUseCase,
    private val userPreferences: UserPreferences,
    private val brightnessManager: BrightnessManager,
    private val playbackHistoryRepository: PlaybackHistoryRepository
) : ViewModel() {
    
    private var currentActivity: Activity? = null
    
    fun setActivity(activity: Activity) {
        currentActivity = activity
        // 初始化亮度为系统亮度
        val systemBrightness = brightnessManager.getSystemBrightness(activity)
        _uiState.update { it.copy(currentBrightness = systemBrightness) }
    }
    
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlaybackState { it.copy(isPlaying = isPlaying) }
        }
        
        override fun onPlaybackStateChanged(playbackState: Int) {
            val isLoading = playbackState == Player.STATE_BUFFERING
            _uiState.update { it.copy(isLoading = isLoading) }
            updatePlaybackState { it.copy(isLoading = isLoading) }
            
            // 处理播放错误
            if (playbackState == Player.STATE_IDLE) {
                val player = exoPlayerManager.getPlayer()
                val error = player?.playerError
                if (error != null) {
                    handlePlaybackError(error)
                }
            }
        }
        
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            updatePlayerPosition()
        }
    }
    
    private fun handlePlaybackError(error: PlaybackException) {
        val errorMessage = when {
            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> {
                "网络连接失败，请检查网络设置"
            }
            error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> {
                "服务器错误: ${error.message}"
            }
            error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> {
                "视频格式不支持或文件损坏"
            }
            else -> {
                "播放错误: ${error.message ?: "未知错误"}"
            }
        }
        
        _uiState.update {
            it.copy(
                error = errorMessage,
                isLoading = false
            )
        }
        
        AppLogger.e("播放错误", error)
    }
    
    init {
        val player = exoPlayerManager.initializePlayer()
        exoPlayerManager.addPlayerListener(playerListener)
        startPositionUpdates()
        loadSavedPlaybackSpeed()
    }
    
    private fun loadSavedPlaybackSpeed() {
        viewModelScope.launch {
            val savedSpeed = userPreferences.playbackSpeed.first()
            if (savedSpeed != 1.0f) {
                setPlaybackSpeed(savedSpeed)
            }
        }
    }
    
    fun playVideo(source: VideoSource, resumeFromHistory: Boolean = true) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            playVideoUseCase(source).fold(
                onSuccess = { validatedSource ->
                    try {
                        val uri = validatedSource.toUri()
                        val isHls = validatedSource.isHls()
                        exoPlayerManager.loadMedia(uri, isHls)
                        
                        // 尝试从历史记录恢复播放位置
                        var resumePosition: Long? = null
                        if (resumeFromHistory) {
                            val history = playbackHistoryRepository.getHistoryByVideoSource(validatedSource)
                            if (history != null && history.lastPosition > 0 && history.duration > 0) {
                                // 如果历史位置超过视频总长度的90%，不恢复
                                val resumeThreshold = history.duration * 0.9
                                if (history.lastPosition < resumeThreshold) {
                                    resumePosition = history.lastPosition
                                }
                            }
                        }
                        
                        _uiState.update {
                            it.copy(
                                videoSource = validatedSource,
                                isLoading = false
                            )
                        }
                        
                        // 如果有恢复位置，先跳转到该位置
                        resumePosition?.let { position ->
                            seekTo(position)
                        }
                        
                        play()
                    } catch (e: Exception) {
                        AppLogger.e("播放视频失败", e)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "播放失败: ${e.message}"
                            )
                        }
                    }
                },
                onFailure = { error ->
                    AppLogger.e("视频源验证失败", error)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "视频源无效: ${error.message}"
                        )
                    }
                }
            )
        }
    }
    
    fun savePlaybackProgress() {
        viewModelScope.launch {
            val videoSource = _uiState.value.videoSource ?: return@launch
            val player = exoPlayerManager.getPlayer() ?: return@launch
            val currentPosition = player.currentPosition
            val duration = player.duration
            
            if (currentPosition > 0 && duration > 0) {
                val videoTitle = when (videoSource) {
                    is VideoSource.Local -> videoSource.path.substringAfterLast("/")
                    is VideoSource.Remote -> videoSource.url.substringAfterLast("/")
                }
                
                playbackHistoryRepository.savePlaybackHistory(
                    videoSource = videoSource,
                    videoTitle = videoTitle,
                    position = currentPosition,
                    duration = duration
                )
            }
        }
    }
    
    fun play() {
        exoPlayerManager.getPlayer()?.play()
    }
    
    fun pause() {
        exoPlayerManager.getPlayer()?.pause()
    }
    
    fun togglePlayPause() {
        val player = exoPlayerManager.getPlayer()
        if (player?.isPlaying == true) {
            pause()
        } else {
            play()
        }
    }
    
    fun seekTo(position: Long) {
        exoPlayerManager.getPlayer()?.seekTo(position)
    }
    
    fun setPlaybackSpeed(speed: Float) {
        exoPlayerManager.getPlayer()?.setPlaybackSpeed(speed)
        updatePlaybackState { it.copy(playbackSpeed = speed) }
        viewModelScope.launch {
            userPreferences.setPlaybackSpeed(speed)
        }
    }
    
    fun toggleSpeedMenu() {
        _uiState.update { it.copy(isSpeedMenuVisible = !it.isSpeedMenuVisible) }
    }
    
    fun setSpeedMenuVisible(visible: Boolean) {
        _uiState.update { it.copy(isSpeedMenuVisible = visible) }
    }
    
    // 快进快退功能
    fun seekForward(seconds: Int = 10) {
        val player = exoPlayerManager.getPlayer() ?: return
        val currentPosition = player.currentPosition
        val duration = player.duration
        val newPosition = (currentPosition + seconds * 1000L).coerceAtMost(duration)
        seekTo(newPosition)
    }
    
    fun seekBackward(seconds: Int = 10) {
        val player = exoPlayerManager.getPlayer() ?: return
        val currentPosition = player.currentPosition
        val newPosition = (currentPosition - seconds * 1000L).coerceAtLeast(0L)
        seekTo(newPosition)
    }
    
    // 音量控制
    fun setVolume(volume: Float) {
        val player = exoPlayerManager.getPlayer() ?: return
        val volumeLevel = volume.coerceIn(0f, 1f)
        player.volume = volumeLevel
        _uiState.update { it.copy(currentVolume = volumeLevel) }
    }
    
    fun toggleVolumeControl() {
        _uiState.update { it.copy(isVolumeControlVisible = !it.isVolumeControlVisible) }
    }
    
    fun setVolumeControlVisible(visible: Boolean) {
        _uiState.update { it.copy(isVolumeControlVisible = visible) }
    }
    
    // 亮度控制
    fun setBrightness(brightness: Float) {
        val brightnessLevel = brightness.coerceIn(0f, 1f)
        _uiState.update { it.copy(currentBrightness = brightnessLevel) }
        currentActivity?.let { activity ->
            brightnessManager.setBrightness(activity, brightnessLevel)
        }
    }
    
    fun restoreBrightness() {
        currentActivity?.let { activity ->
            brightnessManager.restoreBrightness(activity)
        }
    }
    
    fun toggleBrightnessControl() {
        _uiState.update { it.copy(isBrightnessControlVisible = !it.isBrightnessControlVisible) }
    }
    
    fun setBrightnessControlVisible(visible: Boolean) {
        _uiState.update { it.copy(isBrightnessControlVisible = visible) }
    }
    
    // 画面比例
    fun setVideoAspectRatio(aspectRatio: VideoAspectRatio) {
        _uiState.update { it.copy(videoAspectRatio = aspectRatio) }
    }
    
    fun toggleAspectRatioMenu() {
        _uiState.update { it.copy(isAspectRatioMenuVisible = !it.isAspectRatioMenuVisible) }
    }
    
    fun setAspectRatioMenuVisible(visible: Boolean) {
        _uiState.update { it.copy(isAspectRatioMenuVisible = visible) }
    }
    
    fun toggleControls() {
        _uiState.update { it.copy(isControlsVisible = !it.isControlsVisible) }
    }
    
    fun setControlsVisible(visible: Boolean) {
        _uiState.update { it.copy(isControlsVisible = visible) }
    }
    
    fun toggleFullscreen() {
        _uiState.update { it.copy(isFullscreen = !it.isFullscreen) }
    }
    
    fun setFullscreen(fullscreen: Boolean) {
        _uiState.update { it.copy(isFullscreen = fullscreen) }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun retryPlayback() {
        val videoSource = _uiState.value.videoSource
        if (videoSource != null) {
            clearError()
            playVideo(videoSource)
        }
    }
    
    private fun startPositionUpdates() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(500) // 每500ms更新一次
                updatePlayerPosition()
                
                // 每5秒保存一次播放进度
                val currentTime = System.currentTimeMillis()
                if (currentTime % 5000 < 500) {
                    savePlaybackProgress()
                }
            }
        }
    }
    
    private fun updatePlayerPosition() {
        val player = exoPlayerManager.getPlayer() ?: return
        updatePlaybackState {
            it.copy(
                currentPosition = player.currentPosition,
                duration = player.duration.coerceAtLeast(0),
                bufferedPosition = player.bufferedPosition
            )
        }
    }
    
    private fun updatePlaybackState(update: (PlaybackState) -> PlaybackState) {
        _uiState.update { state ->
            state.copy(playbackState = update(state.playbackState))
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        exoPlayerManager.removePlayerListener(playerListener)
        // 恢复亮度
        restoreBrightness()
        // 注意：不在这里释放player，因为可能需要在Activity/Fragment中管理生命周期
    }
    
    fun getPlayer(): Player? = exoPlayerManager.getPlayer()
}

