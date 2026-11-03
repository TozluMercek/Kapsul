@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.example.kapsul.ui.attachments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.io.File

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

/** Thin wrapper over [MediaRecorder] that records AAC into an .m4a file. */
private class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null

    fun start(target: File) {
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
        else @Suppress("DEPRECATION") MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioEncodingBitRate(128_000)
        r.setAudioSamplingRate(44_100)
        r.setOutputFile(target.absolutePath)
        r.prepare()
        r.start()
        recorder = r
    }

    fun pause() = recorder?.pause()
    fun resume() = recorder?.resume()
    fun amplitude(): Int = recorder?.maxAmplitude ?: 0

    /** Returns false when the recording was too short to be valid. */
    fun stop(): Boolean {
        val r = recorder ?: return false
        recorder = null
        return try {
            r.stop()
            true
        } catch (_: RuntimeException) {
            false
        } finally {
            r.release()
        }
    }
}

/**
 * Records a voice note: asks for the microphone, shows a live level meter and timer.
 * [onFinished] gets the finished file; [onCancel] is also called when permission is denied.
 */
@Composable
fun AudioRecorderPanel(
    newFile: () -> File,
    onFinished: (file: File, durationMs: Long) -> Unit,
    onCancel: (reason: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val recorder = remember { AudioRecorder(context) }
    var file by remember { mutableStateOf<File?>(null) }
    var recording by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var elapsed by remember { mutableLongStateOf(0L) }
    val levels = remember { mutableStateListOf<Float>() }
    val currentOnCancel by rememberUpdatedState(onCancel)

    fun begin() {
        val target = newFile()
        try {
            recorder.start(target)
            file = target
            recording = true
        } catch (_: Exception) {
            target.delete()
            currentOnCancel("Mikrofon başlatılamadı")
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) begin() else currentOnCancel("Ses kaydı için mikrofon izni gerekli")
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) begin() else permission.launch(Manifest.permission.RECORD_AUDIO)
    }

    LaunchedEffect(recording, paused) {
        while (recording && !paused) {
            delay(80)
            elapsed += 80
            levels += (recorder.amplitude() / 32767f).coerceIn(0.04f, 1f)
            if (levels.size > 48) levels.removeAt(0)
        }
    }

    // Leaving without saving throws the half-written file away.
    DisposableEffect(Unit) {
        onDispose {
            if (recording) {
                recorder.stop()
                file?.delete()
            }
        }
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            formatDuration(elapsed),
            style = MaterialTheme.typography.displayMedium,
            color = if (paused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(16.dp))
        LevelMeter(levels, Modifier.fillMaxWidth().height(56.dp))
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            FilledTonalIconButton(
                onClick = {
                    recording = false
                    recorder.stop()
                    file?.delete()
                    onCancel(null)
                },
                modifier = Modifier.size(56.dp),
            ) { Icon(Icons.Filled.Close, contentDescription = "Vazgeç") }

            FilledIconButton(
                onClick = {
                    recording = false
                    val ok = recorder.stop()
                    val f = file
                    if (ok && f != null) onFinished(f, elapsed) else {
                        f?.delete()
                        onCancel("Kayıt çok kısa")
                    }
                },
                enabled = recording,
                shape = MaterialShapes.Cookie9Sided.toShape(),
                modifier = Modifier.size(84.dp),
            ) { Icon(Icons.Filled.Stop, contentDescription = "Kaydı bitir", modifier = Modifier.size(36.dp)) }

            FilledTonalIconButton(
                onClick = {
                    if (paused) recorder.resume() else recorder.pause()
                    paused = !paused
                },
                enabled = recording,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    if (paused) Icons.Outlined.Mic else Icons.Filled.Pause,
                    contentDescription = if (paused) "Devam et" else "Duraklat",
                )
            }
        }
    }
}

@Composable
private fun LevelMeter(levels: List<Float>, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        val padded = List(48 - levels.size) { 0.04f } + levels
        padded.forEach { level ->
            val h by animateFloatAsState(level, label = "level")
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight(h)
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
    }
}

/** Inline player for a recorded or imported audio file. */
@Composable
fun AudioPlayer(file: File, label: String, durationMs: Long, modifier: Modifier = Modifier) {
    var player by remember(file) { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember(file) { mutableStateOf(false) }
    var position by remember(file) { mutableIntStateOf(0) }
    var duration by remember(file) { mutableLongStateOf(durationMs) }
    var dragging by remember { mutableStateOf<Float?>(null) }

    DisposableEffect(file) {
        onDispose { player?.release() }
    }
    LaunchedEffect(playing) {
        while (playing) {
            position = player?.currentPosition ?: 0
            delay(100)
        }
    }

    fun toggle() {
        val p = player ?: runCatching {
            MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener { playing = false; position = 0 }
            }
        }.getOrNull()?.also {
            player = it
            if (duration <= 0) duration = it.duration.toLong()
        } ?: return
        if (playing) p.pause() else p.start()
        playing = !playing
    }

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        FilledIconButton(
            onClick = ::toggle,
            shape = if (playing) RoundedCornerShape(16.dp) else CircleShape,
            modifier = Modifier.size(52.dp),
        ) {
            Icon(
                if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (playing) "Duraklat" else "Oynat",
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            val fraction = if (duration > 0) position / duration.toFloat() else 0f
            Slider(
                value = dragging ?: fraction.coerceIn(0f, 1f),
                onValueChange = { dragging = it },
                onValueChangeFinished = {
                    dragging?.let { f ->
                        val target = (f * duration).toInt()
                        player?.seekTo(target)
                        position = target
                    }
                    dragging = null
                },
                enabled = duration > 0,
                modifier = Modifier.height(32.dp),
            )
            Text(
                "${formatDuration(position.toLong())} / ${formatDuration(duration)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
