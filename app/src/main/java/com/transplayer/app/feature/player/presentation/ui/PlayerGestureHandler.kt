package com.transplayer.app.feature.player.presentation.ui

import android.util.Log
import androidx.compose.foundation.gestures.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.*
import kotlin.math.abs

fun Modifier.playerGestureDetector(
    onSingleTap: () -> Unit = {},
    onDoubleTap: () -> Unit = {},
    onHorizontalSwipe: (Float) -> Unit = {}, // 正值向右，负值向左
    onVerticalSwipe: (Float, Boolean) -> Unit = { _, _ -> }, // 正值向下，负值向上，第二个参数表示是否在左侧
    enabled: Boolean = true
): Modifier {
    return this.then(
        Modifier.pointerInput(enabled) {
            if (!enabled) return@pointerInput
            
            var startX = 0f
            var startY = 0f
            var lastTapTime = 0L
            var isDragging = false
            var totalDragX = 0f
            var totalDragY = 0f
            var lastSeekTime = 0L
            var lastVolumeBrightnessTime = 0L
            val doubleTapTimeout = 300L
            val seekThrottle = 200L // 跳转节流：每200ms最多触发一次
            val volumeBrightnessThrottle = 50L // 音量/亮度节流：每50ms最多触发一次
            
            // 使用 awaitPointerEventScope 手动处理事件，优先检测拖拽
            awaitPointerEventScope {
                var currentPointer: PointerInputChange? = null
                
                while (true) {
                    val event = awaitPointerEvent()
                    val pointer = event.changes.firstOrNull() ?: continue
                    currentPointer = pointer
                    
                    when {
                        // 按下事件
                        pointer.pressed && !pointer.previousPressed -> {
                            Log.d("PlayerGesture", "Pointer down: x=${pointer.position.x}, y=${pointer.position.y}")
                            startX = pointer.position.x
                            startY = pointer.position.y
                            totalDragX = 0f
                            totalDragY = 0f
                            isDragging = false
                        }
                        
                        // 移动事件 - 检测拖拽
                        pointer.pressed -> {
                            val change = pointer.positionChange()
                            if (change.x != 0f || change.y != 0f) {
                                totalDragX += change.x
                                totalDragY += change.y
                                
                                val horizontalDistance = abs(totalDragX)
                                val verticalDistance = abs(totalDragY)
                                val minDragDistance = 30f
                                
                                // 如果移动距离超过阈值，认为是拖拽
                                if (!isDragging && (horizontalDistance > minDragDistance || verticalDistance > minDragDistance)) {
                                    Log.d("PlayerGesture", "Drag started: totalX=$totalDragX, totalY=$totalDragY")
                                    isDragging = true
                                    lastSeekTime = 0L
                                    lastVolumeBrightnessTime = 0L
                                }
                                
                                if (isDragging) {
                                    val currentTime = System.currentTimeMillis()
                                    
                                    // 水平滑动
                                    if (horizontalDistance > minDragDistance && horizontalDistance > verticalDistance * 1.2) {
                                        val timeSinceLastSeek = currentTime - lastSeekTime
                                        if (timeSinceLastSeek > seekThrottle) {
                                            Log.d("PlayerGesture", "Horizontal swipe: $totalDragX")
                                            onHorizontalSwipe(totalDragX)
                                            lastSeekTime = currentTime
                                        }
                                    }
                                    // 垂直滑动
                                    else if (verticalDistance > minDragDistance && verticalDistance > horizontalDistance * 1.2) {
                                        val timeSinceLastVB = currentTime - lastVolumeBrightnessTime
                                        if (timeSinceLastVB > volumeBrightnessThrottle) {
                                            val isLeftSide = startX < size.width / 2f
                                            Log.d("PlayerGesture", "Vertical swipe: $totalDragY, isLeftSide=$isLeftSide")
                                            onVerticalSwipe(totalDragY, isLeftSide)
                                            lastVolumeBrightnessTime = currentTime
                                        }
                                    }
                                }
                            }
                        }
                        
                        // 抬起事件 - 处理点击
                        !pointer.pressed && pointer.previousPressed -> {
                            Log.d("PlayerGesture", "Pointer up, isDragging=$isDragging")
                            if (!isDragging) {
                                val currentTime = System.currentTimeMillis()
                                val timeSinceLastTap = currentTime - lastTapTime
                                if (timeSinceLastTap < doubleTapTimeout && timeSinceLastTap > 0) {
                                    Log.d("PlayerGesture", "Double tap")
                                    onDoubleTap()
                                } else {
                                    Log.d("PlayerGesture", "Single tap")
                                    onSingleTap()
                                }
                                lastTapTime = currentTime
                            }
                            isDragging = false
                            totalDragX = 0f
                            totalDragY = 0f
                        }
                    }
                }
            }
        }
    )
}

