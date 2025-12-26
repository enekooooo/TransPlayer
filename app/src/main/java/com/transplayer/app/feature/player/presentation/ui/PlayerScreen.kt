package com.transplayer.app.feature.player.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.transplayer.app.feature.player.domain.model.VideoSource
import com.transplayer.app.feature.player.presentation.viewmodel.PlayerViewModel

@UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    videoSource: VideoSource?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val player = viewModel.getPlayer()
    val context = LocalContext.current
    
    // 设置 Activity 用于亮度控制
    LaunchedEffect(Unit) {
        val activity = context as? android.app.Activity
        activity?.let { viewModel.setActivity(it) }
    }
    
    // 自动加载视频
    LaunchedEffect(videoSource) {
        if (videoSource != null && uiState.videoSource != videoSource) {
            viewModel.playVideo(videoSource)
        }
    }
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 视频播放器
        VideoPlayerView(
            player = player,
            isLoading = uiState.isLoading,
            aspectRatio = uiState.videoAspectRatio,
            modifier = Modifier.fillMaxSize(),
            onFullscreenChange = { viewModel.setFullscreen(it) }
        )
        
        // 手势检测层 - 放在最上层，透明覆盖，拦截所有手势事件
        Box(
            modifier = Modifier
                .fillMaxSize()
                .playerGestureDetector(
                    onSingleTap = { 
                        android.util.Log.d("PlayerScreen", "Single tap detected")
                        viewModel.toggleControls() 
                    },
                    onDoubleTap = { 
                        android.util.Log.d("PlayerScreen", "Double tap detected")
                        viewModel.togglePlayPause() 
                    },
                    onHorizontalSwipe = { deltaX ->
                        // 水平滑动：调节进度（每50像素 = 5秒）
                        android.util.Log.d("PlayerScreen", "onHorizontalSwipe called: deltaX=$deltaX")
                        val seekSeconds = (deltaX / 50f * 5f).toInt()
                        android.util.Log.d("PlayerScreen", "Calculated seekSeconds: $seekSeconds")
                        if (seekSeconds != 0) {
                            if (seekSeconds > 0) {
                                android.util.Log.d("PlayerScreen", "Seeking forward: $seekSeconds seconds")
                                viewModel.seekForward(seekSeconds)
                            } else {
                                android.util.Log.d("PlayerScreen", "Seeking backward: ${-seekSeconds} seconds")
                                viewModel.seekBackward(-seekSeconds)
                            }
                        } else {
                            android.util.Log.d("PlayerScreen", "SeekSeconds is 0, not seeking")
                        }
                    },
                    onVerticalSwipe = { deltaY, isLeftSide ->
                        // 垂直滑动：左侧调节亮度，右侧调节音量
                        android.util.Log.d("PlayerScreen", "onVerticalSwipe called: deltaY=$deltaY, isLeftSide=$isLeftSide")
                        val delta = deltaY / 500f // 归一化
                        if (isLeftSide) {
                            // 左侧：调节亮度
                            val currentBrightness = uiState.currentBrightness
                            val newBrightness = (currentBrightness - delta).coerceIn(0f, 1f)
                            viewModel.setBrightness(newBrightness)
                            viewModel.setBrightnessControlVisible(true)
                        } else {
                            // 右侧：调节音量
                            val currentVolume = uiState.currentVolume
                            val newVolume = (currentVolume - delta).coerceIn(0f, 1f)
                            viewModel.setVolume(newVolume)
                            viewModel.setVolumeControlVisible(true)
                        }
                    },
                    enabled = true // 始终启用手势检测
                )
        )
        
        // 顶部栏 - 返回按钮始终可见
        TopAppBar(
            title = { },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Black.copy(alpha = 0.5f),
                titleContentColor = Color.White
            ),
            modifier = Modifier.align(Alignment.TopCenter)
        )
        
        // 播放控制
        if (uiState.isControlsVisible) {
            PlayerControls(
                playbackState = uiState.playbackState,
                onPlayPauseClick = { viewModel.togglePlayPause() },
                onSeekTo = { position -> viewModel.seekTo(position) },
                onSeekForward = { viewModel.seekForward(10) },
                onSeekBackward = { viewModel.seekBackward(10) },
                onSpeedClick = { viewModel.toggleSpeedMenu() },
                onAspectRatioClick = { viewModel.toggleAspectRatioMenu() },
                onFullscreenClick = { viewModel.toggleFullscreen() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )
        }
        
        // 播放速度菜单
        SpeedMenu(
            currentSpeed = uiState.playbackState.playbackSpeed,
            isVisible = uiState.isSpeedMenuVisible,
            onSpeedSelected = { speed ->
                viewModel.setPlaybackSpeed(speed)
                viewModel.setSpeedMenuVisible(false)
            },
            modifier = Modifier.align(Alignment.Center)
        )
        
        // 画面比例菜单
        AspectRatioMenu(
            currentAspectRatio = uiState.videoAspectRatio,
            isVisible = uiState.isAspectRatioMenuVisible,
            onAspectRatioSelected = { ratio ->
                viewModel.setVideoAspectRatio(ratio)
                viewModel.setAspectRatioMenuVisible(false)
            },
            modifier = Modifier.align(Alignment.Center)
        )
        
        // 音量控制
        VolumeControl(
            volume = uiState.currentVolume,
            isVisible = uiState.isVolumeControlVisible,
            onVolumeChange = { volume ->
                viewModel.setVolume(volume)
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        )
        
        // 亮度控制
        BrightnessControl(
            brightness = uiState.currentBrightness,
            isVisible = uiState.isBrightnessControlVisible,
            onBrightnessChange = { brightness ->
                viewModel.setBrightness(brightness)
            },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
        )
        
        // 错误提示
        uiState.error?.let { error ->
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "播放错误",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = { viewModel.clearError() }) {
                            Text("关闭")
                        }
                        Button(onClick = { viewModel.retryPlayback() }) {
                            Text("重试")
                        }
                    }
                }
            }
        }
    }
}

