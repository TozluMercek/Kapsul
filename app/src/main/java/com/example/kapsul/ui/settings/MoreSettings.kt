package com.example.kapsul.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.kapsul.KapsulApplication
import com.example.kapsul.data.AppSettings
import com.example.kapsul.data.SettingsRepository

@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    ListItem(
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}

/** AMOLED black under the theme card. */
@Composable
fun AmoledRow(settings: AppSettings, repository: SettingsRepository) {
    SettingRow(
        Icons.Outlined.DarkMode,
        "AMOLED tam siyah",
        "Koyu temada arka planı tamamen siyah yapar; OLED ekranlarda pil tasarrufu sağlar",
        settings.amoled,
        repository::setAmoled,
    )
}

@Composable
fun ClipboardRow(settings: AppSettings, repository: SettingsRepository) {
    SettingRow(
        Icons.Outlined.ContentPaste,
        "Pano önerisi",
        "Uygulama açıldığında kopyaladığın metni not olarak eklemeyi önerir",
        settings.clipboardSuggestions,
        repository::setClipboardSuggestions,
    )
}

/**
 * "Bağlam kapsülü" toggles. Each one asks for its permission when switched on, and
 * re-checks access when the user comes back from system settings.
 */
@Composable
fun ContextCapsuleSettings(settings: AppSettings, repository: SettingsRepository) {
    val context = LocalContext.current
    val collector = (context.applicationContext as KapsulApplication).contextCollector
    // Bumped on resume so permission state is re-read after visiting system settings.
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refresh++
        onPauseOrDispose { }
    }
    val hasSteps = remember(refresh) { collector.hasStepPermission() }
    val hasMusic = remember(refresh) { collector.hasMusicAccess() }
    val hasLocation = remember(refresh) { collector.hasLocationPermission() }

    val stepsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        repository.setContextSteps(granted)
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        repository.setContextLocation(granted)
    }

    Column {
        SettingRow(
            Icons.Outlined.BatteryChargingFull,
            "Pil seviyesi",
            "Not yazılırken şarj durumu",
            settings.contextBattery,
            repository::setContextBattery,
        )
        HorizontalDivider(Modifier.padding(horizontal = 20.dp))
        SettingRow(
            Icons.AutoMirrored.Outlined.DirectionsWalk,
            "Günlük adım sayısı",
            if (hasSteps || !settings.contextSteps) "O güne kadar attığın adımlar" else "Fiziksel etkinlik izni gerekli",
            settings.contextSteps && hasSteps,
            { on ->
                if (on && !hasSteps && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    stepsPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                } else repository.setContextSteps(on)
            },
        )
        HorizontalDivider(Modifier.padding(horizontal = 20.dp))
        SettingRow(
            Icons.Outlined.MusicNote,
            "Çalan müzik / podcast",
            if (hasMusic) "O an dinlediğin şarkı ve sanatçı"
            else "Android bunun için “bildirim erişimi” ister; Kapsül bildirimlerini okumaz",
            settings.contextMusic && hasMusic,
            { on ->
                if (on && !hasMusic) {
                    repository.setContextMusic(true)
                    runCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                } else repository.setContextMusic(on)
            },
        )
        HorizontalDivider(Modifier.padding(horizontal = 20.dp))
        SettingRow(
            Icons.Outlined.LocationOn,
            "Konum",
            "Yaklaşık semt / şehir; yalnızca not ile birlikte cihazında saklanır",
            settings.contextLocation && hasLocation,
            { on ->
                if (on && !hasLocation) locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                else repository.setContextLocation(on)
            },
        )
        Text(
            "Hava durumu eklenmedi: Kapsül internete hiç bağlanmaz.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
        )
    }
}
