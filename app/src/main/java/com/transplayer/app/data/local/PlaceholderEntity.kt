package com.transplayer.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 占位符实体类，用于满足Room数据库的entities要求
 * 在后续迭代中将被真正的实体类替换
 */
@Entity(tableName = "placeholder")
data class PlaceholderEntity(
    @PrimaryKey
    val id: Int = 1
)




