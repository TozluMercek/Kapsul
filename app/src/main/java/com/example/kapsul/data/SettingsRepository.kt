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
    /** Pure black dark theme for OLED screens. */
    val amoled: Boolean = false,
    /** Offer to save copied text as a note when the app opens. */
    val clipboardSuggestions: Boolean = true,
    /** Context capsule: what to record with each new note. */
    val contextBattery: Boolean = true,
    val contextSteps: Boolean = false,
    val contextMusic: Boolean = false,
    val contextLocation: Boolean = false,
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

    fun setAmoled(enabled: Boolean) = update { it.copy(amoled = enabled) }

    fun setClipboardSuggestions(enabled: Boolean) = update { it.copy(clipboardSuggestions = enabled) }

    fun setContextBattery(enabled: Boolean) = update { it.copy(contextBattery = enabled) }

    fun setContextSteps(enabled: Boolean) = update { it.copy(contextSteps = enabled) }

    fun setContextMusic(enabled: Boolean) = update { it.copy(contextMusic = enabled) }

    fun setContextLocation(enabled: Boolean) = update { it.copy(contextLocation = enabled) }

    /**
     * Timestamp of the last clipboard item we looked at, so each copy is read (and Android's
     * "pasted from clipboard" toast shown) at most once.
     */
    var lastClipboardSeen: Long
        get() = prefs.getLong(KEY_LAST_CLIP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_CLIP, value).apply()

    private fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        prefs.edit()
            .putString(KEY_THEME, next.themeMode.name)
            .putBoolean(KEY_DYNAMIC, next.dynamicColor)
            .putInt(KEY_SEED, next.seedColor)
            .putString(KEY_STYLE, next.paletteStyle.name)
            .putBoolean(KEY_ONBOARDED, next.onboarded)
            .putBoolean(KEY_AMOLED, next.amoled)
            .putBoolean(KEY_CLIPBOARD, next.clipboardSuggestions)
            .putBoolean(KEY_CTX_BATTERY, next.contextBattery)
            .putBoolean(KEY_CTX_STEPS, next.contextSteps)
            .putBoolean(KEY_CTX_MUSIC, next.contextMusic)
            .putBoolean(KEY_CTX_LOCATION, next.contextLocation)
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
        amoled = prefs.getBoolean(KEY_AMOLED, false),
        clipboardSuggestions = prefs.getBoolean(KEY_CLIPBOARD, true),
        contextBattery = prefs.getBoolean(KEY_CTX_BATTERY, true),
        contextSteps = prefs.getBoolean(KEY_CTX_STEPS, false),
        contextMusic = prefs.getBoolean(KEY_CTX_MUSIC, false),
        contextLocation = prefs.getBoolean(KEY_CTX_LOCATION, false),
    )

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_DYNAMIC = "dynamic_color"
        const val KEY_SEED = "seed_color"
        const val KEY_STYLE = "palette_style"
        const val KEY_ONBOARDED = "onboarded"
        const val KEY_AMOLED = "amoled"
        const val KEY_CLIPBOARD = "clipboard_suggestions"
        const val KEY_CTX_BATTERY = "ctx_battery"
        const val KEY_CTX_STEPS = "ctx_steps"
        const val KEY_CTX_MUSIC = "ctx_music"
        const val KEY_CTX_LOCATION = "ctx_location"
        const val KEY_LAST_CLIP = "last_clipboard_seen"
    }
}
