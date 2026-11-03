@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.kapsul.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kapsul.data.SettingsRepository
import com.example.kapsul.data.ThemeMode

@Composable
fun SettingsScreen(settingsRepository: SettingsRepository, onBack: () -> Unit) {
    val settings by settingsRepository.settings.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Ayarlar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle("Görünüm")

            SettingsCard {
                Column(Modifier.padding(20.dp)) {
                    Text("Tema", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Açık, koyu ya da cihazın ayarını izle",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    ThemeModeSelector(settings.themeMode, settingsRepository::setThemeMode)
                }
                AmoledRow(settings, settingsRepository)
            }

            SectionTitle("Renkler")

            SettingsCard {
                ListItem(
                    supportingContent = {
                        Text(
                            if (settingsRepository.dynamicColorSupported) {
                                "Renkleri duvar kağıdından al"
                            } else {
                                "Android 12 ve üzeri gerekir"
                            }
                        )
                    },
                    leadingContent = {
                        Surface(
                            shape = MaterialShapes.Cookie6Sided.toShape(),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Palette, contentDescription = null, modifier = Modifier.size(22.dp))
                            }
                        }
                    },
                    trailingContent = {
                        Switch(
                            checked = settings.dynamicColor && settingsRepository.dynamicColorSupported,
                            onCheckedChange = settingsRepository::setDynamicColor,
                            enabled = settingsRepository.dynamicColorSupported,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.padding(vertical = 8.dp),
                ) {
                    Text("Dinamik renk", style = MaterialTheme.typography.titleMedium)
                }
            }

            SettingsCard {
                Column(Modifier.padding(vertical = 20.dp)) {
                    Text(
                        "Renk paleti",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Kaydırarak renk seç; her rengin dört farklı tonu var",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    val usingWallpaper = settings.dynamicColor && settingsRepository.dynamicColorSupported
                    PalettePicker(
                        selected = if (usingWallpaper) null else settings.seedColor to settings.paletteStyle,
                        onSelect = settingsRepository::setPalette,
                    )
                }
            }

            SectionTitle("Notlar")
            SettingsCard { ClipboardRow(settings, settingsRepository) }

            SectionTitle("Bağlam kapsülü")
            Text(
                "Yeni bir not yazdığında o anın küçük bir fotoğrafını notun altına ekler. " +
                    "Yıllar sonra “bunu yazarken neredeydim, ne dinliyordum?” sorusuna yanıt verir.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            SettingsCard { ContextCapsuleSettings(settings, settingsRepository) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ThemeModeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = listOf(
        Triple(ThemeMode.SYSTEM, "Sistem", Icons.Outlined.BrightnessAuto),
        Triple(ThemeMode.LIGHT, "Açık", Icons.Outlined.LightMode),
        Triple(ThemeMode.DARK, "Koyu", Icons.Outlined.DarkMode),
    )
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, (mode, label, icon) ->
            ThemeOption(
                label = label,
                icon = icon,
                checked = selected == mode,
                onClick = { onSelect(mode) },
                position = index,
                count = options.size,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ThemeOption(
    label: String,
    icon: ImageVector,
    checked: Boolean,
    onClick: () -> Unit,
    position: Int,
    count: Int,
    modifier: Modifier = Modifier,
) {
    ToggleButton(
        checked = checked,
        onCheckedChange = { onClick() },
        modifier = modifier,
        shapes = when (position) {
            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
            count - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
        },
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(6.dp))
        Text(label)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 8.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column { content() }
    }
}
