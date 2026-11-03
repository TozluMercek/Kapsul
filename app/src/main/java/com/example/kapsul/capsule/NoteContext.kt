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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

/** "Context capsule": a small snapshot of the moment a note was written. */
data class NoteContext(
    val battery: Int? = null,
    val charging: Boolean = false,
    val steps: Int? = null,
    val music: String? = null,
    val place: String? = null,
) {
    val isEmpty get() = battery == null && steps == null && music == null && place == null

    fun encode(): String = JSONObject().apply {
        battery?.let { put("battery", it) }
        if (charging) put("charging", true)
        steps?.let { put("steps", it) }
        music?.let { put("music", it) }
        place?.let { put("place", it) }
    }.toString()

    /** "🎵 Şarkı — Sanatçı · 🔋 %72 · 👣 4.210 adım · 📍 Kadıköy" */
    fun describe(): List<String> = buildList {
        music?.let { add("🎵 $it") }
        battery?.let { add(if (charging) "🔌 %$it (şarjda)" else "🔋 %$it") }
        steps?.let { add("👣 ${"%,d".format(Locale.forLanguageTag("tr"), it)} adım") }
        place?.let { add("📍 $it") }
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
            )
        }.getOrNull()
    }
}

/** Gathers the pieces the user enabled in settings; each one is skipped quietly if unavailable. */
class ContextCollector(private val context: Context) {
    private val prefs = context.getSharedPreferences("kapsul_steps", Context.MODE_PRIVATE)

    suspend fun capture(settings: AppSettings): NoteContext = withContext(Dispatchers.IO) {
        val (battery, charging) = if (settings.contextBattery) battery() else (null to false)
        NoteContext(
            battery = battery,
            charging = charging,
            steps = if (settings.contextSteps) stepsToday() else null,
            music = if (settings.contextMusic) nowPlaying() else null,
            place = if (settings.contextLocation) place() else null,
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
    private fun place(): String? = runCatching {
        if (!hasLocationPermission()) return null
        val manager = context.getSystemService(LocationManager::class.java)
        val location = manager.getProviders(true)
            .mapNotNull { manager.getLastKnownLocation(it) }
            .maxByOrNull { it.time } ?: return null
        @Suppress("DEPRECATION")
        val address = if (Geocoder.isPresent()) {
            runCatching { Geocoder(context, Locale.forLanguageTag("tr")).getFromLocation(location.latitude, location.longitude, 1) }
                .getOrNull()?.firstOrNull()
        } else null
        val name = listOfNotNull(address?.subLocality ?: address?.locality ?: address?.subAdminArea, address?.adminArea)
            .distinct().joinToString(", ")
        name.ifBlank { "%.3f, %.3f".format(Locale.ROOT, location.latitude, location.longitude) }
    }.getOrNull()
}
