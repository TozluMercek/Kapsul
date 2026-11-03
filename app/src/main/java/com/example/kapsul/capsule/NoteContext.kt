package com.example.kapsul.capsule

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.BatteryManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.kapsul.data.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/** "Context capsule": a small snapshot of the moment a note was written. */
data class NoteContext(
    val battery: Int? = null,
    val charging: Boolean = false,
    val steps: Int? = null,
    val music: String? = null,
    val place: String? = null,
    /** Ready to show, e.g. "⛅ 18°C, parçalı bulutlu". */
    val weather: String? = null,
) {
    val isEmpty get() = battery == null && steps == null && music == null && place == null && weather == null

    fun encode(): String = JSONObject().apply {
        battery?.let { put("battery", it) }
        if (charging) put("charging", true)
        steps?.let { put("steps", it) }
        music?.let { put("music", it) }
        place?.let { put("place", it) }
        weather?.let { put("weather", it) }
    }.toString()

    /** "🎵 Şarkı — Sanatçı · 🔋 %72 · 👣 4.210 adım · 📍 Kadıköy · ⛅ 18°C, parçalı bulutlu" */
    fun describe(): List<String> = buildList {
        music?.let { add("🎵 $it") }
        battery?.let { add(if (charging) "🔌 %$it (şarjda)" else "🔋 %$it") }
        steps?.let { add("👣 ${"%,d".format(Locale.forLanguageTag("tr"), it)} adım") }
        place?.let { add("📍 $it") }
        weather?.let { add(it) }
    }

    companion object {
        fun decode(value: String?): NoteContext? = runCatching {
            val json = JSONObject(value ?: return null)
            NoteContext(
                battery = json.optInt("battery", -1).takeIf { it >= 0 },
                charging = json.optBoolean("charging", false),
                steps = json.optInt("steps", -1).takeIf { it >= 0 },
                music = json.optString("music").ifBlank { null },
                place = json.optString("place").ifBlank { null },
                weather = json.optString("weather").ifBlank { null },
            )
        }.getOrNull()
    }
}

/** Gathers the pieces the user enabled in settings; each one is skipped quietly if unavailable. */
class ContextCollector(private val context: Context) {
    private val prefs = context.getSharedPreferences("kapsul_steps", Context.MODE_PRIVATE)

    suspend fun capture(settings: AppSettings): NoteContext = withContext(Dispatchers.IO) {
        val (battery, charging) = if (settings.contextBattery) battery() else (null to false)
        val location = if (settings.contextLocation || settings.contextWeather) lastLocation() else null
        NoteContext(
            battery = battery,
            charging = charging,
            steps = if (settings.contextSteps) stepsToday() else null,
            music = if (settings.contextMusic) nowPlaying() else null,
            place = if (settings.contextLocation) location?.let(::place) else null,
            weather = if (settings.contextWeather) location?.let(::weather) else null,
        )
    }

    private fun battery(): Pair<Int?, Boolean> {
        val manager = context.getSystemService(BatteryManager::class.java) ?: return null to false
        val level = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).takeIf { it in 0..100 }
        return level to manager.isCharging
    }

    fun hasStepPermission() = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    /** The step sensor counts since boot; keep a per-day baseline to get "today". */
    private suspend fun stepsToday(): Int? {
        if (!hasStepPermission()) return null
        val sensors = context.getSystemService(SensorManager::class.java) ?: return null
        val sensor = sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return null
        val total = withTimeoutOrNull(2_000) {
            suspendCancellableCoroutine<Int> { cont ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        sensors.unregisterListener(this)
                        if (cont.isActive) cont.resume(event.values[0].toInt())
                    }
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }
                sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                cont.invokeOnCancellation { sensors.unregisterListener(listener) }
            }
        } ?: return null
        val today = SimpleDateFormat("yyyyMMdd", Locale.ROOT).format(Date())
        var baseline = prefs.getInt("base_$today", -1)
        if (baseline < 0 || total < baseline) { // first reading today, or the phone rebooted
            baseline = total
            prefs.edit().clear().putInt("base_$today", total).apply()
        }
        return total - baseline
    }

    fun hasMusicAccess() = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    private fun nowPlaying(): String? = runCatching {
        if (!hasMusicAccess()) return null
        val sessions = context.getSystemService(MediaSessionManager::class.java)
            .getActiveSessions(ComponentName(context, MediaListenerService::class.java))
        val controller = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: return null
        val meta = controller.metadata ?: return null
        val title = meta.getString(MediaMetadata.METADATA_KEY_TITLE) ?: return null
        val artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)
        if (artist.isNullOrBlank()) title else "$title — $artist"
    }.getOrNull()

    fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun lastLocation(): Location? = runCatching {
        if (!hasLocationPermission()) return null
        val manager = context.getSystemService(LocationManager::class.java)
        manager.getProviders(true).mapNotNull { manager.getLastKnownLocation(it) }.maxByOrNull { it.time }
    }.getOrNull()

    private fun place(location: Location): String? = runCatching {
        @Suppress("DEPRECATION")
        val address = if (Geocoder.isPresent()) {
            runCatching { Geocoder(context, Locale.forLanguageTag("tr")).getFromLocation(location.latitude, location.longitude, 1) }
                .getOrNull()?.firstOrNull()
        } else null
        val name = listOfNotNull(address?.subLocality ?: address?.locality ?: address?.subAdminArea, address?.adminArea)
            .distinct().joinToString(", ")
        name.ifBlank { "%.3f, %.3f".format(Locale.ROOT, location.latitude, location.longitude) }
    }.getOrNull()

    /**
     * Current weather from Open-Meteo (free, no API key). Only a location rounded to ~1 km is sent;
     * nothing about the note leaves the device.
     */
    private fun weather(location: Location): String? = runCatching {
        val url = URL(
            "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f&current=temperature_2m,weather_code,is_day"
                .format(Locale.ROOT, location.latitude, location.longitude),
        )
        val connection = url.openConnection() as HttpURLConnection
        val body = try {
            connection.connectTimeout = 4_000
            connection.readTimeout = 4_000
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
        val current = JSONObject(body).getJSONObject("current")
        describeWeather(
            current.getDouble("temperature_2m"),
            current.getInt("weather_code"),
            current.optInt("is_day", 1) == 1,
        )
    }.getOrNull()
}

/** WMO weather code → "⛅ 18°C, parçalı bulutlu". */
internal fun describeWeather(temperature: Double, code: Int, isDay: Boolean): String {
    val (emoji, label) = when (code) {
        0 -> (if (isDay) "☀️" else "🌙") to "açık"
        1, 2 -> (if (isDay) "⛅" else "☁️") to "parçalı bulutlu"
        3 -> "☁️" to "kapalı"
        45, 48 -> "🌫️" to "sisli"
        in 51..57 -> "🌦️" to "çisenti"
        in 61..67 -> "🌧️" to "yağmurlu"
        in 71..77 -> "🌨️" to "karlı"
        in 80..82 -> "🌧️" to "sağanak"
        85, 86 -> "🌨️" to "kar sağanağı"
        in 95..99 -> "⛈️" to "fırtınalı"
        else -> "🌡️" to "hava"
    }
    return "$emoji ${temperature.roundToInt()}°C, $label"
}
