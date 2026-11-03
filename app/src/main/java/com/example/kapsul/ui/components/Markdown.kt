package com.example.kapsul.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

private sealed interface Block {
    data class Heading(val level: Int, val text: String) : Block
    data class Paragraph(val text: String) : Block
    data class Bullet(val marker: String, val text: String) : Block
    data class Task(val text: String, val checked: Boolean, val line: Int) : Block
    data class Quote(val text: String) : Block
    data class Code(val text: String) : Block
    data class Table(val rows: List<List<String>>) : Block
    data object Rule : Block
}

private val TABLE_SEPARATOR = Regex("^\\|?\\s*:?-{3,}:?\\s*(\\|\\s*:?-{3,}:?\\s*)*\\|?$")

private fun cells(line: String): List<String> =
    line.trim().removePrefix("|").removeSuffix("|").split("|").map { it.trim() }

private val HEADING = Regex("^(#{1,6})\\s+(.*)")
private val TASK = Regex("^[-*]\\s+\\[([ xX])]\\s*(.*)")
private val BULLET = Regex("^[-*+]\\s+(.*)")
private val ORDERED = Regex("^(\\d+)[.)]\\s+(.*)")

private fun parse(markdown: String): List<Block> {
    val blocks = mutableListOf<Block>()
    val lines = markdown.lines()
    var i = 0
    while (i < lines.size) {
        val line = lines[i].trimEnd()
        val trimmed = line.trim()
        when {
            trimmed.startsWith("```") -> {
                val code = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    if (code.isNotEmpty()) code.append('\n')
                    code.append(lines[i])
                    i++
                }
                blocks += Block.Code(code.toString())
            }
            trimmed.startsWith("|") -> {
                val rows = mutableListOf<List<String>>()
                while (i < lines.size && lines[i].trim().startsWith("|")) {
                    val row = lines[i].trim()
                    if (!TABLE_SEPARATOR.matches(row)) rows += cells(row)
                    i++
                }
                i-- // the loop below advances past the last table line
                blocks += Block.Table(rows)
            }
            trimmed.isEmpty() -> Unit
            trimmed == "---" || trimmed == "***" -> blocks += Block.Rule
            HEADING.matches(trimmed) -> HEADING.find(trimmed)!!.let {
                blocks += Block.Heading(it.groupValues[1].length, it.groupValues[2])
            }
            TASK.matches(trimmed) -> TASK.find(trimmed)!!.let {
                blocks += Block.Task(it.groupValues[2], it.groupValues[1] != " ", i)
            }
            BULLET.matches(trimmed) -> blocks += Block.Bullet("•", BULLET.find(trimmed)!!.groupValues[1])
            ORDERED.matches(trimmed) -> ORDERED.find(trimmed)!!.let {
                blocks += Block.Bullet(it.groupValues[1] + ".", it.groupValues[2])
            }
            trimmed.startsWith(">") -> blocks += Block.Quote(trimmed.trimStart('>', ' '))
            else -> {
                // Merge consecutive plain lines into one paragraph.
                val last = blocks.lastOrNull()
                if (last is Block.Paragraph && i > 0 && lines[i - 1].isNotBlank()) {
                    blocks[blocks.lastIndex] = Block.Paragraph(last.text + "\n" + trimmed)
                } else {
                    blocks += Block.Paragraph(trimmed)
                }
            }
        }
        i++
    }
    return blocks
}

private val INLINE = Regex("\\*\\*(.+?)\\*\\*|`([^`]+)`|\\[\\[([^\\]]+)]]|(?<![*\\w])\\*([^*\\s][^*]*)\\*|(?<![\\p{L}\\p{N}])(#[\\p{L}][\\p{L}\\p{N}_]*)")

@Composable
private fun inline(text: String, onWikiLink: (String) -> Unit): AnnotatedString {
    val c = MaterialTheme.colorScheme
    return remember(text, c) {
        buildAnnotatedString {
            var cursor = 0
            for (m in INLINE.findAll(text)) {
                append(text.substring(cursor, m.range.first))
                val (bold, code, wiki, italic, tag) = m.destructured
                when {
                    bold.isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
                    code.isNotEmpty() -> withStyle(
                        SpanStyle(fontFamily = FontFamily.Monospace, background = c.surfaceContainerHighest)
                    ) { append(" $code ") }
                    wiki.isNotEmpty() -> withLink(
                        LinkAnnotation.Clickable(
                            tag = "wiki:$wiki",
                            styles = TextLinkStyles(
                                SpanStyle(
                                    color = c.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    textDecoration = TextDecoration.Underline,
                                )
                            ),
                        ) { onWikiLink(wiki) }
                    ) { append(wiki) }
                    italic.isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(italic) }
                    tag.isNotEmpty() -> withStyle(SpanStyle(color = c.tertiary, fontWeight = FontWeight.SemiBold)) {
                        append(tag)
                    }
                }
                cursor = m.range.last + 1
            }
            append(text.substring(cursor))
        }
    }
}

@Composable
private fun TableView(rows: List<List<String>>, onWikiLink: (String) -> Unit) {
    if (rows.isEmpty()) return
    val columns = rows.maxOf { it.size }
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            rows.forEachIndexed { r, row ->
                if (r > 0) HorizontalDivider(color = colors.outlineVariant)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .background(if (r == 0) colors.secondaryContainer else Color.Transparent)
                ) {
                    repeat(columns) { c ->
                        if (c > 0) androidx.compose.material3.VerticalDivider(color = colors.outlineVariant)
                        Text(
                            inline(row.getOrElse(c) { "" }, onWikiLink),
                            style = if (r == 0) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                            color = if (r == 0) colors.onSecondaryContainer else colors.onSurface,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MarkdownView(
    markdown: String,
    onToggleTask: (line: Int) -> Unit,
    onWikiLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(markdown) { parse(markdown) }
    val type = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        blocks.forEach { block ->
            when (block) {
                is Block.Heading -> Text(
                    inline(block.text, onWikiLink),
                    style = when (block.level) {
                        1 -> type.headlineSmall
                        2 -> type.titleLarge
                        else -> type.titleMedium
                    },
                    modifier = Modifier.padding(top = 6.dp),
                )
                is Block.Paragraph -> Text(inline(block.text, onWikiLink), style = type.bodyLarge)
                is Block.Bullet -> Row {
                    Text(block.marker, style = type.bodyLarge, color = colors.primary, modifier = Modifier.width(24.dp))
                    Text(inline(block.text, onWikiLink), style = type.bodyLarge)
                }
                is Block.Task -> Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onToggleTask(block.line) }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (block.checked) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = if (block.checked) "Tamamlandı" else "Yapılacak",
                        tint = if (block.checked) colors.primary else colors.outline,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        inline(block.text, onWikiLink),
                        style = type.bodyLarge.copy(
                            textDecoration = if (block.checked) TextDecoration.LineThrough else null,
                            color = if (block.checked) colors.onSurfaceVariant else Color.Unspecified,
                        ),
                    )
                }
                is Block.Quote -> Row(Modifier.height(IntrinsicSize.Min)) {
                    Box(
                        Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(colors.tertiary)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        inline(block.text, onWikiLink),
                        style = type.bodyLarge.copy(fontStyle = FontStyle.Italic),
                        color = colors.onSurfaceVariant,
                    )
                }
                is Block.Code -> Surface(
                    color = colors.surfaceContainerHighest,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        block.text,
                        style = type.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(14.dp),
                    )
                }
                is Block.Table -> TableView(block.rows, onWikiLink)
                Block.Rule -> HorizontalDivider(color = colors.outlineVariant)
            }
        }
    }
}
