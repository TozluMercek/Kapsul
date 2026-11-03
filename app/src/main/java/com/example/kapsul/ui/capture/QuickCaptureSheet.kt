@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)

package com.example.kapsul.ui.capture

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.kapsul.data.NoteSource
import com.example.kapsul.intelligence.NoteIntelligence
import com.example.kapsul.ui.attachments.AudioPlayer
import com.example.kapsul.ui.attachments.AudioRecorderPanel
import com.example.kapsul.ui.components.tagColors
import java.io.File

/**
 * Quick Capture: opens straight into typing (or recording). A raw sentence like
 * "Yarın Ahmet ile görüşülecek" is titled and tagged on-device while you type;
 * the microphone records real audio that is attached to the note.
 */
@Composable
fun QuickCaptureSheet(
    startWithVoice: Boolean,
    intelligence: NoteIntelligence,
    newAudioFile: () -> File,
    onDismiss: () -> Unit,
    onSave: (text: String, source: NoteSource, recording: Pair<File, Long>?) -> Unit,
    onMessage: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by rememberSaveable { mutableStateOf("") }
    var recording by rememberSaveable { mutableStateOf(startWithVoice) }
    var recorded by remember { mutableStateOf<Pair<File, Long>?>(null) }
    var saved by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (!startWithVoice) focusRequester.requestFocus()
    }

    // A recording that never got saved is thrown away with the sheet.
    val latestRecorded by rememberUpdatedState(recorded)
    DisposableEffect(Unit) {
        onDispose { if (!saved) latestRecorded?.first?.delete() }
    }

    val title = remember(text) { intelligence.suggestTitle(text) }
    val tags = remember(text) { intelligence.suggestTags(text) }

    fun save() {
        saved = true
        onSave(text, if (recorded != null) NoteSource.VOICE else NoteSource.TEXT, recorded)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                if (recording) "Ses kaydı" else "Aklındakini bırak",
                style = MaterialTheme.typography.headlineSmall,
            )

            AnimatedContent(targetState = recording, label = "captureMode") { isRecording ->
                if (isRecording) {
                    AudioRecorderPanel(
                        newFile = newAudioFile,
                        onFinished = { file, duration ->
                            recorded?.first?.delete()
                            recorded = file to duration
                            recording = false
                        },
                        onCancel = { reason ->
                            reason?.let(onMessage)
                            recording = false
                            if (startWithVoice && recorded == null && text.isBlank()) onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        TextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            placeholder = {
                                Text(if (recorded != null) "Kayda bir not ekle (isteğe bağlı)" else "Yarın Ahmet ile görüşülecek…")
                            },
                            minLines = 3,
                            maxLines = 10,
                            shape = MaterialTheme.shapes.large,
                            textStyle = MaterialTheme.typography.bodyLarge,
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                        )

                        recorded?.let { (file, duration) ->
                            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AudioPlayer(file, "Ses kaydı", duration, Modifier.weight(1f).padding(12.dp))
                                    IconButton(onClick = { file.delete(); recorded = null }) {
                                        Icon(Icons.Outlined.Close, contentDescription = "Kaydı sil")
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(visible = text.isNotBlank()) {
                            SmartPreview(title, tags)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalIconButton(
                                onClick = { recording = true },
                                shapes = IconButtonDefaults.shapes(),
                                modifier = Modifier.size(56.dp),
                            ) {
                                Icon(Icons.Outlined.Mic, contentDescription = "Ses kaydet")
                            }
                            Spacer(Modifier.weight(1f))
                            Button(
                                onClick = ::save,
                                enabled = text.isNotBlank() || recorded != null,
                                shapes = ButtonDefaults.shapes(),
                                modifier = Modifier.size(width = 140.dp, height = 56.dp),
                            ) {
                                Text("Kaydet", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartPreview(title: String, tags: List<String>) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            shape = MaterialShapes.Sunny.toShape(),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(9.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (tags.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.forEach { tag ->
                        val colors = tagColors(tag)
                        Surface(shape = MaterialTheme.shapes.small, color = colors.container, contentColor = colors.content) {
                            Text(
                                "#$tag",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            } else {
                Text(
                    "Etiket bulunamadı — #etiket yazarak ekleyebilirsin",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
