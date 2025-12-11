package com.transplayer.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        PlaceholderEntity::class  // 占位符实体，后续迭代中将被替换
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TransPlayerDatabase : RoomDatabase() {
    // TODO: Add DAOs here
}


