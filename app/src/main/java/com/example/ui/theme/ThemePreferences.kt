package com.example.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemePreset(
    val id: String,
    val displayName: String,
    val subtitle: String
) {
    AURORA_POP("aurora_pop", "Royal Violet", "Calm Violet"),
    TROPICAL_SUNSET("tropical_sunset", "Warm Coral", "Soft Coral"),
    ELECTRIC_OCEAN("electric_ocean", "Ocean Blue", "Crisp Blue"),
    ROYAL_EMERALD("royal_emerald", "Emerald", "Fresh Green");

    companion object {
        fun fromId(id: String?): AppThemePreset =
            entries.firstOrNull { it.id == id } ?: AURORA_POP
    }
}

enum class AppDarkMode(val id: String, val label: String) {
    SYSTEM("system", "Auto"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    companion object {
        fun fromId(id: String?): AppDarkMode =
            entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}

data class ThemeUiSettings(
    val preset: AppThemePreset = AppThemePreset.AURORA_POP,
    val darkMode: AppDarkMode = AppDarkMode.SYSTEM
)

object ThemeController {
    private const val PREFS_NAME = "bintaface_theme_prefs"
    private const val KEY_PRESET = "theme_preset"
    private const val KEY_DARK_MODE = "theme_dark_mode"

    private val _settings = MutableStateFlow(ThemeUiSettings())
    val settings: StateFlow<ThemeUiSettings> = _settings.asStateFlow()

    @Volatile
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val preset = AppThemePreset.fromId(prefs.getString(KEY_PRESET, AppThemePreset.AURORA_POP.id))
            val darkMode = AppDarkMode.fromId(prefs.getString(KEY_DARK_MODE, AppDarkMode.SYSTEM.id))
            _settings.value = ThemeUiSettings(preset = preset, darkMode = darkMode)
            initialized = true
        }
    }

    fun setPreset(context: Context, preset: AppThemePreset) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PRESET, preset.id).apply()
        _settings.value = _settings.value.copy(preset = preset)
    }

    fun setDarkMode(context: Context, darkMode: AppDarkMode) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DARK_MODE, darkMode.id).apply()
        _settings.value = _settings.value.copy(darkMode = darkMode)
    }
}
