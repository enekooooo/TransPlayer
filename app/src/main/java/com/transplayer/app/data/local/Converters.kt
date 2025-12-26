package com.transplayer.app.data.local

import android.net.Uri
import androidx.room.TypeConverter
import com.transplayer.app.feature.player.domain.model.VideoSource
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    @TypeConverter
    fun fromVideoSource(videoSource: VideoSource?): String? {
        return when (videoSource) {
            is VideoSource.Local -> "LOCAL:${videoSource.uri}:${videoSource.path}"
            is VideoSource.Remote -> "REMOTE:${videoSource.url}:${videoSource.type.name}"
            null -> null
        }
    }
    
    @TypeConverter
    fun toVideoSource(value: String?): VideoSource? {
        return value?.let {
            val parts = it.split(":", limit = 3)
            when (parts[0]) {
                "LOCAL" -> {
                    if (parts.size >= 3) {
                        VideoSource.Local(Uri.parse(parts[1]), parts[2])
                    } else {
                        null
                    }
                }
                "REMOTE" -> {
                    if (parts.size >= 3) {
                        val streamType = try {
                            VideoSource.StreamType.valueOf(parts[2])
                        } catch (e: IllegalArgumentException) {
                            VideoSource.StreamType.HTTP
                        }
                        VideoSource.Remote(parts[1], streamType)
                    } else {
                        null
                    }
                }
                else -> null
            }
        }
    }
}





