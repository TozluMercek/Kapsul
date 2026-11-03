@file:OptIn(ExperimentalLayoutApi::class)

package com.example.kapsul.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.kapsul.data.Attachment
import com.example.kapsul.data.AttachmentType
import com.example.kapsul.data.Note
import com.example.kapsul.data.NoteSource
import com.example.kapsul.ui.attachments.loadBitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Alarm
import com.example.kapsul.ui.background.NoteBackground
import com.example.kapsul.ui.background.rememberBackgroundColors
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import java.io.File
import com.example.kapsul.util.relativeDate
import kotlin.math.absoluteValue

data class TagColors(val container: Color, val content: Color)

/** Each tag gets a stable container color from the current scheme. */
@Composable
fun tagColors(tag: String?): TagColors {
    val c = MaterialTheme.colorScheme
    if (tag == null) return TagColors(c.surfaceContainerHigh, c.onSurface)
    val palette = listOf(
        TagColors(c.primaryContainer, c.onPrimaryContainer),
        TagColors(c.secondaryContainer, c.onSecondaryContainer),
        TagColors(c.tertiaryContainer, c.onTertiaryContainer),
        TagColors(c.surfaceContainerHighest, c.onSurface),
    )
    return palette[tag.hashCode().absoluteValue % palette.size]
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
    attachments: List<Attachment> = emptyList(),
    fileOf: (String) -> File = { File(it) },
) {
    val colors = tagColors(note.tags.firstOrNull())
    // A chosen background style wins over the tag color.
    val background = rememberBackgroundColors(NoteBackground.decode(note.background))
    val cover = attachments.firstOrNull { it.type == AttachmentType.IMAGE || it.type == AttachmentType.DRAWING }
    val haptics = LocalHapticFeedback.current
    val shape = MaterialTheme.shapes.large
    // Selected cards shrink a little and get an outline, the expressive way.
    val scale by animateFloatAsState(if (selected) 0.94f else 1f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "select")
    Surface(
        modifier = modifier
            .scale(scale)
            .border(if (selected) 3.dp else 0.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, shape)
            .clip(shape)
            .then(if (background != null) Modifier.background(background.brush) else Modifier)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick?.let { longClick ->
                    {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        longClick()
                    }
                },
            ),
        shape = shape,
        color = if (background != null) Color.Transparent else colors.container,
        contentColor = background?.scheme?.onPrimaryContainer ?: colors.content,
    ) {
        Box {
            Column {
                cover?.fileName?.let { CoverImage(fileOf(it), isDrawing = cover.type == AttachmentType.DRAWING) }
                NoteCardBody(note, attachments)
            }
            AnimatedVisibility(
                visible = selected,
                enter = scaleIn(),
                exit = scaleOut(),
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
            ) {
                Box(
                    Modifier
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Seçildi",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CoverImage(file: File, isDrawing: Boolean) {
    val bitmap by produceState<ImageBitmap?>(null, file) { value = loadBitmap(file, 600) }
    bitmap?.let {
        Image(
            it,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(if (isDrawing) Color.White else Color.Transparent),
        )
    }
}

@Composable
private fun NoteCardBody(note: Note, attachments: List<Attachment>) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val sourceIcon = when (note.source) {
                NoteSource.VOICE -> Icons.Outlined.Mic
                NoteSource.CHAT -> Icons.AutoMirrored.Outlined.Chat
                NoteSource.TEXT -> null
            }
            if (note.pinned) SmallIcon(Icons.Outlined.PushPin)
            if (note.reminderAt != null) SmallIcon(Icons.Outlined.Alarm)
            if (sourceIcon != null) SmallIcon(sourceIcon)
            Text(
                relativeDate(note.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = LocalContentColor.current.copy(alpha = 0.7f),
            )
        }
        Text(
            note.title.ifBlank { "Başlıksız" },
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        val preview = plainPreview(note.body, note.title)
        if (preview.isNotBlank()) {
            Text(
                preview,
                style = MaterialTheme.typography.bodyMedium,
                color = LocalContentColor.current.copy(alpha = 0.8f),
                maxLines = 7,
                overflow = TextOverflow.Ellipsis,
            )
        }
        AttachmentBadges(attachments)
        if (note.tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                note.tags.forEach {
                    Text(
                        "#$it",
                        style = MaterialTheme.typography.labelMedium,
                        color = LocalContentColor.current.copy(alpha = 0.75f),
                    )
                }
            }
        }
    }
}

/** "🎙 2  📄 1 …" — what's attached, at a glance. */
@Composable
private fun AttachmentBadges(attachments: List<Attachment>) {
    if (attachments.isEmpty()) return
    val counts = attachments.groupingBy {
        when (it.type) {
            AttachmentType.IMAGE, AttachmentType.DRAWING -> Icons.Outlined.Image
            AttachmentType.AUDIO -> Icons.Outlined.Mic
            AttachmentType.PDF -> Icons.Outlined.PictureAsPdf
            AttachmentType.STICKY -> Icons.AutoMirrored.Outlined.StickyNote2
        }
    }.eachCount()
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        counts.forEach { (icon, count) ->
            Surface(
                shape = MaterialTheme.shapes.small,
                color = LocalContentColor.current.copy(alpha = 0.1f),
            ) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                    if (count > 1) {
                        Spacer(Modifier.width(4.dp))
                        Text("$count", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
    Spacer(Modifier.width(6.dp))
}

/** Markdown → short readable preview for cards. */
fun plainPreview(body: String, title: String): String =
    body.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("```") }
        .map { line ->
            when {
                line.startsWith("- [ ]") -> "○ " + line.removePrefix("- [ ]").trim()
                line.startsWith("- [x]", ignoreCase = true) -> "● " + line.drop(5).trim()
                line.startsWith("- ") || line.startsWith("* ") -> "• " + line.drop(2)
                else -> line.trimStart('#', '>', ' ')
            }.replace("**", "").replace("[[", "").replace("]]", "").replace("`", "")
        }
        .dropWhile { it.trimEnd('.', ' ').equals(title, ignoreCase = true) }
        .take(8)
        .joinToString("\n")
