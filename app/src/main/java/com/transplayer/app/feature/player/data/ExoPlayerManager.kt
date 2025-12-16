package com.transplayer.app.feature.player.data

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.transplayer.app.util.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class ExoPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var player: ExoPlayer? = null
    
    fun initializePlayer(): ExoPlayer {
        if (player == null) {
            player = ExoPlayer.Builder(context)
                .build()
            AppLogger.d("ExoPlayer initialized")
        }
        return player!!
    }
    
    fun releasePlayer() {
        player?.release()
        player = null
        AppLogger.d("ExoPlayer released")
    }
    
    fun getPlayer(): ExoPlayer? = player
    
    fun loadMedia(uri: Uri, isHls: Boolean = false) {
        val exoPlayer = player ?: initializePlayer()
        
        val mediaSource = if (isHls) {
            createHlsMediaSource(uri)
        } else {
            createProgressiveMediaSource(uri)
        }
        
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        AppLogger.d("Media loaded: $uri, isHls: $isHls")
    }
    
    fun loadMedia(url: String, isHls: Boolean = false) {
        loadMedia(Uri.parse(url), isHls)
    }
    
    private fun createProgressiveMediaSource(uri: Uri): MediaSource {
        val dataSourceFactory = DefaultDataSource.Factory(context)
        return ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(uri))
    }
    
    private fun createHlsMediaSource(uri: Uri): MediaSource {
        val dataSourceFactory = DefaultDataSource.Factory(context)
        return HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(uri))
    }
    
    fun addPlayerListener(listener: Player.Listener) {
        player?.addListener(listener)
    }
    
    fun removePlayerListener(listener: Player.Listener) {
        player?.removeListener(listener)
    }
}

