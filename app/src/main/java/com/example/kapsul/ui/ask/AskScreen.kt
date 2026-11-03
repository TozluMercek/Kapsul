@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.kapsul.ui.ask

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kapsul.data.Note
import com.example.kapsul.ui.ChatMessage
import com.example.kapsul.ui.KapsulViewModel
import com.example.kapsul.ui.components.rememberVoiceInput
import com.example.kapsul.ui.components.tagColors
import com.example.kapsul.util.relativeDate

private val Examples = listOf(
    "Geçen ay ev tamiri için hangi malzemeyi konuşmuştuk?",
    "İzlediğim videodaki tarif neydi?",
    "Kaydet: Cuma günü elektrik faturasını öde",
)

@Composable
fun AskScreen(
    vm: KapsulViewModel,
    onBack: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onVoiceUnavailable: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val messages = vm.chat

    LaunchedEffect(messages.size, vm.thinking) {
        val last = messages.size + if (vm.thinking) 1 else 0
        if (last > 0) listState.animateScrollToItem(last - 1)
    }

    fun send(text: String = input) {
        vm.ask(text)
        input = ""
    }

    val listen = rememberVoiceInput(onResult = { send(it) }, onUnavailable = onVoiceUnavailable)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Hafızana sor", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Yerel arama · hiçbir veri cihazdan çıkmaz",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
            )
        },
        bottomBar = {
            ChatInput(
                value = input,
                onValueChange = { input = it },
                onSend = { send() },
                onMic = listen,
            )
        },
    ) { padding ->
        if (messages.isEmpty() && !vm.thinking) {
            Intro(Modifier.padding(padding), onExample = { send(it) })
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        onOpenNote = onOpenNote,
                        onSave = { vm.saveMessageAsNote(message) },
                        modifier = Modifier.animateItem(),
                    )
                }
                if (vm.thinking) {
                    item(key = "thinking") {
                        LoadingIndicator(Modifier.size(48.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun Intro(modifier: Modifier, onExample: (String) -> Unit) {
    Column(
        modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = MaterialShapes.SoftBurst.toShape(),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(112.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(44.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Neyi hatırlamak istiyorsun?", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Notlarının, sesli kayıtlarının ve sohbetlerinin arasından nokta atışı bulurum. " +
                "“Kaydet: …” yazarak doğrudan not da bırakabilirsin.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Examples.forEach { example ->
                SuggestionChip(
                    onClick = { onExample(example) },
                    label = { Text(example, maxLines = 2) },
                    shape = MaterialTheme.shapes.medium,
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    onOpenNote: (Long) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fromUser = message.fromUser
    val corner = 24.dp
    val tail = 6.dp
    val shape = if (fromUser) RoundedCornerShape(corner, corner, tail, corner) else RoundedCornerShape(corner, corner, corner, tail)
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = if (fromUser) Alignment.End else Alignment.Start,
    ) {
        Surface(
            shape = shape,
            color = if (fromUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (fromUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Text(
                message.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
        message.sources.forEach { note ->
            Spacer(Modifier.height(6.dp))
            SourceCard(note, onClick = { onOpenNote(note.id) })
        }
        // Chat → note: any message can become a permanent note.
        if (message.sources.isEmpty() || fromUser) {
            if (message.savedNoteId != null) {
                TextButton(onClick = { onOpenNote(message.savedNoteId) }) {
                    Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Not olarak kaydedildi")
                }
            } else {
                TextButton(onClick = onSave) {
                    Icon(Icons.AutoMirrored.Outlined.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Not olarak kaydet")
                }
            }
        }
    }
}

@Composable
private fun SourceCard(note: Note, onClick: () -> Unit) {
    val colors = tagColors(note.tags.firstOrNull())
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = colors.container,
        contentColor = colors.content,
        modifier = Modifier.widthIn(max = 320.dp),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(note.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                relativeDate(note.updatedAt) + note.tags.joinToString("") { "  #$it" },
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ChatInput(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit, onMic: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Sor ya da “Kaydet: …” yaz") },
                maxLines = 5,
                shape = RoundedCornerShape(28.dp),
                trailingIcon = {
                    IconButton(onClick = onMic) { Icon(Icons.Outlined.Mic, contentDescription = "Sesle sor") }
                },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            )
            Spacer(Modifier.size(8.dp))
            FilledIconButton(
                onClick = onSend,
                enabled = value.isNotBlank(),
                shape = MaterialShapes.Cookie6Sided.toShape(),
                modifier = Modifier.size(56.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gönder")
            }
        }
    }
}
