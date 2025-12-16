package com.transplayer.app.feature.player.data.repository

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.webkit.URLUtil
import com.transplayer.app.feature.player.domain.model.VideoSource
import com.transplayer.app.feature.player.domain.repository.VideoInfo
import com.transplayer.app.feature.player.domain.repository.VideoRepository
import com.transplayer.app.util.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class VideoRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : VideoRepository {
    
    // 支持的视频格式
    private val supportedVideoFormats = setOf(
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp", "m4v"
    )
    
    // 支持的网络流格式
    private val supportedStreamFormats = setOf(
        "mp4", "m3u8", "mpd", "webm"
    )
    
    override suspend fun validateVideoSource(source: VideoSource): Result<VideoSource> {
        return withContext(Dispatchers.IO) {
            try {
                when (source) {
                    is VideoSource.Local -> {
                        val uri = source.uri
                        
                        // 检查 URI 是否存在
                        val exists = when {
                            // Content URI (从文件管理器选择)
                            uri.scheme == "content" -> {
                                checkContentUriExists(uri)
                            }
                            // File URI
                            uri.scheme == "file" -> {
                                val file = File(uri.path ?: "")
                                file.exists() && file.isFile
                            }
                            // 其他情况，尝试作为文件路径
                            else -> {
                                val file = File(source.path)
                                file.exists() && file.isFile
                            }
                        }
                        
                        if (!exists) {
                            return@withContext Result.failure(
                                Exception("视频文件不存在: ${uri}")
                            )
                        }
                        
                        // 检查文件格式（如果可以获取文件名）
                        val fileName = getFileNameFromUri(uri)
                        if (fileName != null) {
                            val extension = fileName.substringAfterLast(".", "").lowercase()
                            if (extension.isNotEmpty() && !supportedVideoFormats.contains(extension)) {
                                AppLogger.w("不支持的视频格式: $extension")
                                // 仍然允许尝试播放，因为ExoPlayer可能支持
                            }
                        }
                        
                        Result.success(source)
                    }
                    is VideoSource.Remote -> {
                        if (!URLUtil.isValidUrl(source.url)) {
                            return@withContext Result.failure(
                                Exception("无效的视频URL: ${source.url}")
                            )
                        }
                        
                        // 检查URL格式
                        val urlLower = source.url.lowercase()
                        val hasSupportedExtension = supportedStreamFormats.any { ext ->
                            urlLower.contains(".$ext") || urlLower.contains(".m3u8")
                        }
                        
                        if (!hasSupportedExtension && !urlLower.contains("m3u8")) {
                            AppLogger.w("URL可能不是视频格式: ${source.url}")
                            // 仍然允许尝试播放
                        }
                        
                        Result.success(source)
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("视频源验证失败", e)
                Result.failure(e)
            }
        }
    }
    
    private fun checkContentUriExists(uri: Uri): Boolean {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use {
                it.count > 0
            } ?: false
        } catch (e: Exception) {
            AppLogger.e("检查Content URI失败", e)
            false
        }
    }
    
    private fun getFileNameFromUri(uri: Uri): String? {
        return try {
            when {
                uri.scheme == "content" -> {
                    var fileName: String? = null
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                            if (nameIndex >= 0) {
                                fileName = cursor.getString(nameIndex)
                            }
                        }
                    }
                    fileName ?: uri.lastPathSegment
                }
                uri.scheme == "file" -> {
                    File(uri.path ?: "").name
                }
                else -> {
                    File(uri.path ?: "").name.takeIf { it.isNotEmpty() }
                }
            }
        } catch (e: Exception) {
            AppLogger.e("获取文件名失败", e)
            uri.lastPathSegment
        }
    }
    
    override suspend fun getVideoInfo(source: VideoSource): VideoInfo? {
        return withContext(Dispatchers.IO) {
            try {
                when (source) {
                    is VideoSource.Local -> {
                        val fileName = getFileNameFromUri(source.uri) ?: "视频文件"
                        VideoInfo(
                            title = fileName,
                            duration = 0L // 将在播放时获取
                        )
                    }
                    is VideoSource.Remote -> {
                        // 从URL提取文件名
                        val fileName = source.url.substringAfterLast("/")
                            .substringBefore("?")
                        VideoInfo(
                            title = fileName.ifEmpty { "网络视频" },
                            duration = 0L // 将在播放时获取
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("获取视频信息失败", e)
                null
            }
        }
    }
}

