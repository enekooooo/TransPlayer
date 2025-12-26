package com.transplayer.app.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val PLAYBACK_SPEED_KEY = floatPreferencesKey("playback_speed")
        private val DEFAULT_PLAYBACK_SPEED = 1.0f
        
        private val BRIGHTNESS_KEY = intPreferencesKey("brightness")
        private val DEFAULT_BRIGHTNESS = -1 // -1表示使用系统亮度
    }
    
    val playbackSpeed: Flow<Float> = dataStore.data.map { preferences ->
        preferences[PLAYBACK_SPEED_KEY] ?: DEFAULT_PLAYBACK_SPEED
    }
    
    suspend fun setPlaybackSpeed(speed: Float) {
        dataStore.edit { preferences ->
            preferences[PLAYBACK_SPEED_KEY] = speed
        }
    }
    
    val brightness: Flow<Int> = dataStore.data.map { preferences ->
        preferences[BRIGHTNESS_KEY] ?: DEFAULT_BRIGHTNESS
    }
    
    suspend fun setBrightness(brightness: Int) {
        dataStore.edit { preferences ->
            preferences[BRIGHTNESS_KEY] = brightness
        }
    }
}



