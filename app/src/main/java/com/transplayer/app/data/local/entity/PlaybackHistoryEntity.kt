package com.transplayer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.transplayer.app.data.local.Converters
import com.transplayer.app.feature.player.domain.model.VideoSource

@Entity(tableName = "playback_history")
@TypeConverters(Converters::class)
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @androidx.room.ColumnInfo(name = "video_source")
    val videoSource: VideoSource,
    @androidx.room.ColumnInfo(name = "video_title")
    val videoTitle: String,
    @androidx.room.ColumnInfo(name = "last_position")
    val lastPosition: Long, // 最后播放位置（毫秒）
    @androidx.room.ColumnInfo(name = "duration")
    val duration: Long, // 视频总时长（毫秒）
    @androidx.room.ColumnInfo(name = "last_played_time")
    val lastPlayedTime: Long, // 最后播放时间（时间戳）
    @androidx.room.ColumnInfo(name = "thumbnail_path")
    val thumbnailPath: String? = null // 缩略图路径（可选）
)

