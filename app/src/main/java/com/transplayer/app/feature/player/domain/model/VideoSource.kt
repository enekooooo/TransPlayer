package com.transplayer.app.feature.player.domain.model

import android.net.Uri

sealed class VideoSource {
    data class Local(val uri: Uri, val path: String) : VideoSource()
    data class Remote(val url: String, val type: StreamType) : VideoSource()
    
    enum class StreamType {
        HTTP, HLS, DASH
    }
    
    fun toUri(): Uri {
        return when (this) {
            is Local -> uri
            is Remote -> Uri.parse(url)
        }
    }
    
    fun isHls(): Boolean {
        return this is Remote && type == StreamType.HLS
    }
}

