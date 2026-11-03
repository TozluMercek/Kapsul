@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.example.kapsul.reminders

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kapsul.KapsulApplication
import com.example.kapsul.MainActivity
import com.example.kapsul.data.ReminderMode
import com.example.kapsul.ui.theme.KapsulTheme
import com.example.kapsul.data.ThemeMode
import androidx.compose.foundation.isSystemInDarkTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Full-screen alarm shown over the lock screen; the sound stops when it is dismissed. */
class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        enableEdgeToEdge()

        val app = application as KapsulApplication
        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1)

        setContent {
            val settings by app.settings.settings.collectAsStateWithLifecycle()
            val dark = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            KapsulTheme(
                darkTheme = dark,
                dynamicColor = settings.dynamicColor && app.settings.dynamicColorSupported,
                seedColor = settings.seedColor,
                paletteStyle = settings.paletteStyle,
            ) {
                val title by produceState("", noteId) {
                    value = app.repository.get(noteId)?.title?.ifBlank { "Hatırlatma" } ?: "Hatırlatma"
                }
                AlarmScreen(
                    title = title,
                    onSnooze = {
                        ReminderNotifications.dismiss(this, noteId)
                        app.reminders.snooze(noteId, ReminderMode.ALARM)
                        finish()
                    },
                    onDismiss = {
                        ReminderNotifications.dismiss(this, noteId)
                        finish()
                    },
                    onOpen = {
                        ReminderNotifications.dismiss(this, noteId)
                        startActivity(MainActivity.openNoteIntent(this, noteId))
                        finish()
                    },
                )
            }
        }
    }

    companion object {
        private const val EXTRA_NOTE_ID = "noteId"

        fun intent(context: Context, noteId: Long) = Intent(context, AlarmActivity::class.java)
            .putExtra(EXTRA_NOTE_ID, noteId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
    }
}

@Composable
private fun AlarmScreen(title: String, onSnooze: () -> Unit, onDismiss: () -> Unit, onOpen: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "scale",
    )
    val wobble by rememberInfiniteTransition(label = "wobble").animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(tween(160), RepeatMode.Reverse),
        label = "rotation",
    )
    val now = SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr")).format(Date())

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Text(now, style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(32.dp))
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    shape = MaterialShapes.Cookie12Sided.toShape(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(180.dp).scale(pulse),
                ) {}
                Icon(
                    Icons.Filled.Alarm,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(72.dp).rotate(wobble),
                )
            }
            Spacer(Modifier.height(32.dp))
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FilledTonalButton(
                    onClick = onSnooze,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.weight(1f).height(72.dp),
                ) {
                    Icon(Icons.Outlined.Snooze, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("10 dk ertele", style = MaterialTheme.typography.titleMedium)
                }
                Button(
                    onClick = onDismiss,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.weight(1f).height(72.dp),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Kapat", style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(12.dp))
            androidx.compose.material3.TextButton(onClick = onOpen) { Text("Notu aç") }
        }
    }
}
