package com.example.kapsul.data

import android.content.Context
import android.os.Build
import com.materialkolor.PaletteStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Kapsül's own teal; the palette used when wallpaper color is off and nothing else was picked. */
const val DEFAULT_SEED_COLOR: Int = 0xFF00897B.toInt()

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val seedColor: Int = DEFAULT_SEED_COLOR,
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    /** The swipeable introduction has been completed or skipped. */
    val onboarded: Boolean = false,
)

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("kapsul_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    /** Wallpaper-based color is only available on Android 12+. */
    val dynamicColorSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }

    fun setDynamicColor(enabled: Boolean) = update { it.copy(dynamicColor = enabled) }

    /** Picking a palette replaces wallpaper colors. */
    fun setPalette(seedColor: Int, style: PaletteStyle) =
        update { it.copy(seedColor = seedColor, paletteStyle = style, dynamicColor = false) }

    fun setOnboarded() = update { it.copy(onboarded = true) }

    private fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        prefs.edit()
            .putString(KEY_THEME, next.themeMode.name)
            .putBoolean(KEY_DYNAMIC, next.dynamicColor)
            .putInt(KEY_SEED, next.seedColor)
            .putString(KEY_STYLE, next.paletteStyle.name)
            .putBoolean(KEY_ONBOARDED, next.onboarded)
            .apply()
        _settings.value = next
    }

    private fun read() = AppSettings(
        themeMode = prefs.getString(KEY_THEME, null)
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM,
        dynamicColor = prefs.getBoolean(KEY_DYNAMIC, true),
        seedColor = prefs.getInt(KEY_SEED, DEFAULT_SEED_COLOR),
        paletteStyle = prefs.getString(KEY_STYLE, null)
            ?.let { runCatching { PaletteStyle.valueOf(it) }.getOrNull() }
            ?: PaletteStyle.TonalSpot,
        onboarded = prefs.getBoolean(KEY_ONBOARDED, false),
    )

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_DYNAMIC = "dynamic_color"
        const val KEY_SEED = "seed_color"
        const val KEY_STYLE = "palette_style"
        const val KEY_ONBOARDED = "onboarded"
    }
}
