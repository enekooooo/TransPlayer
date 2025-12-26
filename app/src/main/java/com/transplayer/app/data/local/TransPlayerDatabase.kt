package com.transplayer.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.transplayer.app.data.local.dao.PlaybackHistoryDao
import com.transplayer.app.data.local.entity.PlaybackHistoryEntity

@Database(
    entities = [
        PlaceholderEntity::class,  // 占位符实体，后续迭代中将被替换
        PlaybackHistoryEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TransPlayerDatabase : RoomDatabase() {
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
}


