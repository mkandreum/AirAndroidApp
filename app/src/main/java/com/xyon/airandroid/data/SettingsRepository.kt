package com.xyon.airandroid.data

import android.content.Context
import android.os.Build
import com.xyon.airandroid.model.AppSettings

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("airandroid_settings", Context.MODE_PRIVATE)

    fun load(): AppSettings {
        val fallback = Build.MODEL.ifBlank { "Mi Android" }.take(32)
        val name = prefs.getString(KEY_DEVICE_NAME, fallback)?.trim().orEmpty()
        return AppSettings(deviceName = name.ifBlank { fallback })
    }

    fun saveDeviceName(name: String) {
        val clean = name.trim().take(32).ifBlank { Build.MODEL.ifBlank { "Mi Android" } }
        prefs.edit().putString(KEY_DEVICE_NAME, clean).apply()
    }

    private companion object {
        const val KEY_DEVICE_NAME = "device_name"
    }
}
