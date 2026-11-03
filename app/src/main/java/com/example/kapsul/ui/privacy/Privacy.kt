@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.kapsul.ui.privacy

import android.content.Context
import androidx.activity.compose.LocalActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.toPath
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.graphics.shapes.Morph
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.sin

private val TR = Locale.forLanguageTag("tr")
private const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

fun longDate(at: Long): String = SimpleDateFormat("d MMMM yyyy", TR).format(Date(at))

/** Locking needs a screen lock (PIN, pattern, password) or a biometric on the device. */
fun canLockNotes(context: Context): Boolean =
    BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

/** Returns a function that asks for fingerprint / face / device PIN and calls back on success. */
@Composable
fun rememberAuthenticator(): (title: String, onSuccess: () -> Unit) -> Unit {
    val activity = LocalActivity.current as? FragmentActivity
    return remember(activity) {
        fun authenticate(title: String, onSuccess: () -> Unit) {
            val host = activity ?: return
            val prompt = BiometricPrompt(
                host,
                ContextCompat.getMainExecutor(host),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
                },
            )
            prompt.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle("Parmak izi, yüz tanıma veya cihaz PIN'i")
                    .setAllowedAuthenticators(AUTHENTICATORS)
                    .build()
            )
        }
        ::authenticate
    }
}

/** Shown instead of a locked note's content until the user authenticates. */
@Composable
fun LockedGate(title: String, onUnlock: () -> Unit, onBack: () -> Unit) {
    val authenticate = rememberAuthenticator()
    LaunchedEffect(Unit) { authenticate("“$title” kilitli", onUnlock) }
    GateScaffold(onBack) {
        Box(contentAlignment = Alignment.Center) {
            Surface(shape = MaterialShapes.Cookie9Sided.toShape(), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(160.dp)) {}
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(64.dp))
        }
        Spacer(Modifier.height(28.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Bu not kilitli", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        Button(onClick = { authenticate("“$title” kilitli", onUnlock) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.height(56.dp)) {
            Icon(Icons.Outlined.Fingerprint, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Kilidi aç", style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** A sealed time capsule: only its title and the countdown are visible. */
@Composable
fun CapsuleGate(title: String, createdAt: Long, opensAt: Long, onBack: () -> Unit) {
    val now by produceState(System.currentTimeMillis()) {
        while (true) { delay(1_000); value = System.currentTimeMillis() }
    }
    val flip by rememberInfiniteTransition(label = "hourglass").animateFloat(
        initialValue = 0f, targetValue = 180f,
        animationSpec = infiniteRepeatable(tween(2_400, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "flip",
    )
    GateScaffold(onBack) {
        Box(contentAlignment = Alignment.Center) {
            Surface(shape = MaterialShapes.Cookie12Sided.toShape(), color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.size(170.dp).rotate(flip / 6)) {}
            Icon(
                Icons.Outlined.HourglassTop,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(72.dp).rotate(flip),
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Bu zaman kapsülü ${longDate(opensAt)} tarihinde açılacak",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Countdown(opensAt - now)
        Spacer(Modifier.height(24.dp))
        Text(
            "${longDate(createdAt)} tarihinde mühürlendi · içeriği şifreli",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Countdown(remaining: Long) {
    val left = remaining.coerceAtLeast(0) / 1000
    val parts = listOf(left / 86_400 to "gün", (left % 86_400) / 3_600 to "saat", (left % 3_600) / 60 to "dk", left % 60 to "sn")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        parts.forEach { (value, unit) ->
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$value", style = MaterialTheme.typography.headlineSmall)
                    Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun GateScaffold(onBack: () -> Unit, content: @Composable () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().statusBarsPadding()) {
            IconButton(onClick = onBack, modifier = Modifier.padding(4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
            }
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { content() }
        }
    }
}

/** Choose the opening day; nothing can unseal the note before then. */
@Composable
fun SealCapsuleDialog(onSeal: (Long) -> Unit, onDismiss: () -> Unit) {
    fun after(field: Int, amount: Int) = Calendar.getInstance().apply {
        add(field, amount); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val presets = listOf(
        "1 ay" to after(Calendar.MONTH, 1),
        "6 ay" to after(Calendar.MONTH, 6),
        "1 yıl" to after(Calendar.YEAR, 1),
        "5 yıl" to after(Calendar.YEAR, 5),
    )
    var at by remember { mutableLongStateOf(presets[2].second) }
    var picking by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.HourglassTop, contentDescription = null) },
        title = { Text("Zaman kapsülüne kilitle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Notun içeriği şifrelenir ve seçtiğin güne kadar açılamaz. O gün “Zaman Kapsülünüz Açıldı” bildirimi gelir.")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.forEach { (label, time) ->
                        FilterChip(selected = at == time, onClick = { at = time }, label = { Text(label) })
                    }
                }
                OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) { Text(longDate(at)) }
                Text(
                    "Bu tarihten önce sen de dahil kimse okuyamaz.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSeal(at) }) { Text("Mühürle") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )

    if (picking) {
        val tomorrowUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
        val state = rememberDatePickerState(
            initialSelectedDateMillis = at,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= tomorrowUtc - 86_400_000L
            },
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { picked ->
                        val p = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = picked }
                        at = Calendar.getInstance().apply {
                            clear(); set(p.get(Calendar.YEAR), p.get(Calendar.MONTH), p.get(Calendar.DAY_OF_MONTH), 9, 0)
                        }.timeInMillis
                    }
                    picking = false
                }) { Text("Tamam") }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Vazgeç") } },
        ) { DatePicker(state) }
    }
}

/** "Zaman Kapsülün Açıldı!" — a burst that morphs open, with sparks flying out. */
@Composable
fun CapsuleCelebration(title: String, writtenAt: Long, onRead: () -> Unit) {
    val open = remember { Animatable(0f) }
    val sparks = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        open.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(Unit) { delay(250); sparks.animateTo(1f, tween(1_400, easing = LinearEasing)) }
    LaunchedEffect(Unit) { delay(600); textAlpha.animateTo(1f, tween(600)) }
    val spin by rememberInfiniteTransition(label = "spin").animateFloat(
        0f, 360f, infiniteRepeatable(tween(20_000, easing = LinearEasing)), label = "rotation",
    )
    val morph = remember { Morph(MaterialShapes.Cookie4Sided, MaterialShapes.SoftBurst) }
    val path = remember { Path() }
    val colors = MaterialTheme.colorScheme

    Dialog(onDismissRequest = onRead, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = colors.surface, modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val c = Offset(size.width / 2, size.height / 2)
                        // Sparks
                        repeat(24) { i ->
                            val angle = i * (360f / 24) * (Math.PI / 180)
                            val r = size.minDimension * (0.18f + 0.34f * sparks.value)
                            val color = if (i % 3 == 0) colors.tertiary else if (i % 3 == 1) colors.primary else colors.secondary
                            drawCircle(
                                color.copy(alpha = (1f - sparks.value).coerceIn(0f, 1f)),
                                radius = 6.dp.toPx() * (1f - sparks.value * 0.5f),
                                center = Offset(c.x + (r * cos(angle)).toFloat(), c.y + (r * sin(angle)).toFloat()),
                            )
                        }
                        // Morphing burst
                        path.rewind()
                        morph.toPath(open.value.coerceIn(0f, 1f), path)
                        val s = size.minDimension * 0.62f * open.value
                        rotate(spin, c) {
                            translate(c.x - s / 2, c.y - s / 2) {
                                scale(s, s, pivot = Offset.Zero) { drawPath(path, colors.primaryContainer) }
                            }
                        }
                    }
                    Icon(
                        Icons.Outlined.HourglassTop,
                        contentDescription = null,
                        tint = colors.onPrimaryContainer,
                        modifier = Modifier.size(72.dp).scale(open.value).rotate(180f * open.value),
                    )
                }
                Spacer(Modifier.height(24.dp))
                Column(Modifier.alpha(textAlpha.value), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Zaman Kapsülün Açıldı!", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(title, style = MaterialTheme.typography.titleLarge, color = colors.primary, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Bu notu ${longDate(writtenAt)} tarihinde yazmıştın.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(28.dp))
                    Button(onClick = onRead, shapes = ButtonDefaults.shapes(), modifier = Modifier.height(56.dp)) {
                        Text("Oku", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
