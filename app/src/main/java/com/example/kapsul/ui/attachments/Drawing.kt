@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.kapsul.ui.attachments

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AutoFixNormal
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private class Stroke2(val points: List<Offset>, val color: Color, val width: Float)

private val Inks = listOf(
    Color(0xFF1F1F1F), Color(0xFFD32F2F), Color(0xFFF57C00), Color(0xFFFBC02D),
    Color(0xFF388E3C), Color(0xFF1976D2), Color(0xFF7B1FA2),
)
private val Widths = listOf(4f, 9f, 18f)
private val Paper = Color.White

private fun pathOf(points: List<Offset>) = Path().apply {
    moveTo(points.first().x, points.first().y)
    // Smooth through midpoints so fast strokes don't look jagged.
    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val cur = points[i]
        quadraticTo(prev.x, prev.y, (prev.x + cur.x) / 2, (prev.y + cur.y) / 2)
    }
    lineTo(points.last().x, points.last().y)
}

/** Finger drawing on a white page, saved as PNG into [target]. */
@Composable
fun DrawingDialog(target: () -> File, onSaved: (File) -> Unit, onDismiss: () -> Unit) {
    val strokes = remember { mutableStateListOf<Stroke2>() }
    val current = remember { mutableStateListOf<Offset>() }
    var ink by remember { mutableStateOf(Inks.first()) }
    var width by remember { mutableFloatStateOf(Widths[1]) }
    var eraser by remember { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun save() {
        if (strokes.isEmpty() || canvasSize == IntSize.Zero) {
            onDismiss(); return
        }
        saving = true
        val snapshot = strokes.toList()
        val size = canvasSize
        scope.launch {
            val file = withContext(Dispatchers.IO) {
                val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(Paper.toArgb())
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                snapshot.forEach { s ->
                    paint.color = s.color.toArgb()
                    paint.strokeWidth = s.width
                    if (s.points.size == 1) {
                        paint.style = Paint.Style.FILL
                        canvas.drawCircle(s.points[0].x, s.points[0].y, s.width / 2, paint)
                        paint.style = Paint.Style.STROKE
                    } else {
                        canvas.drawPath(pathOf(s.points).asAndroidPath(), paint)
                    }
                }
                target().also { f -> f.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
            }
            onSaved(file)
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Çizim") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Vazgeç") }
                    },
                    actions = {
                        IconButton(onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) }, enabled = strokes.isNotEmpty()) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Geri al")
                        }
                        IconButton(onClick = { strokes.clear() }, enabled = strokes.isNotEmpty()) {
                            Icon(Icons.Outlined.DeleteSweep, contentDescription = "Temizle")
                        }
                        TextButton(onClick = ::save, enabled = !saving) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Ekle")
                        }
                    },
                )
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .background(Paper)
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                current.clear()
                                current += down.position
                                down.consume()
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break
                                    current += change.position
                                    change.consume()
                                }
                                strokes += Stroke2(current.toList(), if (eraser) Paper else ink, if (eraser) width * 3 else width)
                                current.clear()
                            }
                        }
                ) {
                    fun draw(points: List<Offset>, color: Color, w: Float) {
                        if (points.size == 1) drawCircle(color, w / 2, points[0])
                        else drawPath(pathOf(points), color, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                    strokes.forEach { draw(it.points, it.color, it.width) }
                    if (current.isNotEmpty()) draw(current, if (eraser) Paper else ink, if (eraser) width * 3 else width)
                }

                HorizontalFloatingToolbar(
                    expanded = true,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                ) {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Inks.forEach { c ->
                            val selected = !eraser && ink == c
                            Box(
                                Modifier
                                    .size(if (selected) 34.dp else 28.dp)
                                    .border(if (selected) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    .padding(if (selected) 5.dp else 0.dp)
                                    .background(c, CircleShape)
                                    .pointerInput(c) {
                                        awaitEachGesture { awaitFirstDown(); ink = c; eraser = false }
                                    }
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Widths.forEach { w ->
                            IconToggleButton(checked = width == w, onCheckedChange = { width = w }) {
                                Box(Modifier.size((w + 4).dp.coerceAtMost(22.dp)).background(MaterialTheme.colorScheme.onSurface, CircleShape))
                            }
                        }
                        IconToggleButton(checked = eraser, onCheckedChange = { eraser = it }) {
                            Icon(Icons.Outlined.AutoFixNormal, contentDescription = "Silgi")
                        }
                    }
                }
            }
        }
    }
}
