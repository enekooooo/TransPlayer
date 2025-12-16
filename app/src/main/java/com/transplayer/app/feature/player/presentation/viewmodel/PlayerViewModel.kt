package com.transplayer.app.feature.player.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.transplayer.app.feature.player.data.ExoPlayerManager
import com.transplayer.app.feature.player.domain.model.PlaybackState
import com.transplayer.app.feature.player.domain.model.VideoSource
import com.transplayer.app.feature.player.domain.usecase.PlayVideoUseCase
import com.transplayer.app.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val exoPlayerManager: ExoPlayerManager,
    private val playVideoUseCase: PlayVideoUseCase
) : ViewModel() {
    
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
    }
    
    fun playVideo(source: VideoSource) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            playVideoUseCase(source).fold(
                onSuccess = { validatedSource ->
                    try {
                        val uri = validatedSource.toUri()
                        val isHls = validatedSource.isHls()
                        exoPlayerManager.loadMedia(uri, isHls)
                        
                        _uiState.update {
                            it.copy(
                                videoSource = validatedSource,
                                isLoading = false
                            )
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
        // 注意：不在这里释放player，因为可能需要在Activity/Fragment中管理生命周期
    }
    
    fun getPlayer(): Player? = exoPlayerManager.getPlayer()
}

