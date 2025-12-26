package com.transplayer.app.feature.player.data

import android.app.Activity
import android.provider.Settings
import android.view.WindowManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrightnessManager @Inject constructor() {
    
    private var originalBrightness: Float = -1f
    private var isBrightnessControlled: Boolean = false
    
    /**
     * 设置屏幕亮度
     * @param activity Activity实例
     * @param brightness 亮度值，范围 0.0f - 1.0f
     */
    fun setBrightness(activity: Activity, brightness: Float) {
        val brightnessLevel = brightness.coerceIn(0f, 1f)
        val layoutParams = activity.window.attributes
        
        if (!isBrightnessControlled) {
            // 保存原始亮度
            originalBrightness = layoutParams.screenBrightness
            isBrightnessControlled = true
        }
        
        // 设置亮度
        layoutParams.screenBrightness = brightnessLevel
        activity.window.attributes = layoutParams
    }
    
    /**
     * 恢复原始亮度
     */
    fun restoreBrightness(activity: Activity) {
        if (isBrightnessControlled && originalBrightness >= 0) {
            val layoutParams = activity.window.attributes
            layoutParams.screenBrightness = originalBrightness
            activity.window.attributes = layoutParams
            isBrightnessControlled = false
            originalBrightness = -1f
        }
    }
    
    /**
     * 获取当前系统亮度
     */
    fun getSystemBrightness(activity: Activity): Float {
        return try {
            val brightness = Settings.System.getInt(
                activity.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            )
            brightness / 255f
        } catch (e: Exception) {
            0.5f // 默认值
        }
    }
}



