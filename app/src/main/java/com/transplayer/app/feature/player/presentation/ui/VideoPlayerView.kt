package com.transplayer.app.feature.player.presentation.ui

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.transplayer.app.feature.player.presentation.viewmodel.VideoAspectRatio

@UnstableApi
@Composable
fun VideoPlayerView(
    player: Player?,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    aspectRatio: VideoAspectRatio = VideoAspectRatio.FIT,
    onFullscreenChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    
    val resizeMode = when (aspectRatio) {
        VideoAspectRatio.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        VideoAspectRatio.ORIGINAL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        VideoAspectRatio.SIXTEEN_NINE -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        VideoAspectRatio.FOUR_THREE -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        VideoAspectRatio.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false // 使用自定义控制器
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { playerView ->
                playerView.player = player
                playerView.resizeMode = resizeMode
            }
        )
        
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            )
        }
    }
}

