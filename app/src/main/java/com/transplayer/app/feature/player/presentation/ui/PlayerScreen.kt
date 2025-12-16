package com.transplayer.app.feature.player.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    
    // 自动加载视频
    LaunchedEffect(videoSource) {
        if (videoSource != null && uiState.videoSource != videoSource) {
            viewModel.playVideo(videoSource)
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // 视频播放器
        VideoPlayerView(
            player = player,
            isLoading = uiState.isLoading,
            modifier = Modifier.fillMaxSize(),
            onFullscreenChange = { viewModel.setFullscreen(it) }
        )
        
        // 顶部栏
        if (uiState.isControlsVisible) {
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
        }
        
        // 播放控制
        if (uiState.isControlsVisible) {
            PlayerControls(
                playbackState = uiState.playbackState,
                onPlayPauseClick = { viewModel.togglePlayPause() },
                onSeekTo = { position -> viewModel.seekTo(position) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )
        }
        
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

