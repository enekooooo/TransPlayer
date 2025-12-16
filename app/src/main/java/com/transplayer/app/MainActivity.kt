package com.transplayer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.media3.common.util.UnstableApi
import com.transplayer.app.feature.player.data.ExoPlayerManager
import com.transplayer.app.feature.player.domain.model.VideoSource
import com.transplayer.app.feature.player.presentation.ui.PlayerScreen
import com.transplayer.app.feature.player.presentation.ui.VideoSelectionScreen
import com.transplayer.app.ui.theme.TransPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var exoPlayerManager: ExoPlayerManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TransPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        // 暂停播放
        exoPlayerManager.getPlayer()?.pause()
    }
    
    override fun onResume() {
        super.onResume()
        // 恢复播放（如果需要）
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 释放播放器资源
        exoPlayerManager.releasePlayer()
    }
}

@Composable
fun MainScreen() {
    var selectedVideo by remember { mutableStateOf<VideoSource?>(null) }
    
    if (selectedVideo == null) {
        VideoSelectionScreen(
            onVideoSelected = { videoSource ->
                selectedVideo = videoSource
            }
        )
    } else {
        PlayerScreen(
            videoSource = selectedVideo,
            onBackClick = {
                selectedVideo = null
            }
        )
    }
}





