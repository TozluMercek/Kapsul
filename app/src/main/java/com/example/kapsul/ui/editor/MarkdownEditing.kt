package com.example.kapsul.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

enum class Format { Heading, Bullet, Task, Quote, Bold, Code, Link }

fun TextFieldValue.apply(format: Format): TextFieldValue = when (format) {
    Format.Heading -> toggleLinePrefix("# ")
    Format.Bullet -> toggleLinePrefix("- ")
    Format.Task -> toggleLinePrefix("- [ ] ")
    Format.Quote -> toggleLinePrefix("> ")
    Format.Bold -> wrap("**", "**")
    Format.Link -> wrap("[[", "]]")
    Format.Code -> if (selectedText().contains('\n') || selection.collapsed) codeBlock() else wrap("`", "`")
}

private fun TextFieldValue.selectedText() = text.substring(selection.min, selection.max)

private fun TextFieldValue.toggleLinePrefix(prefix: String): TextFieldValue {
    val lineStart = text.lastIndexOf('\n', (selection.min - 1).coerceAtLeast(0)).let {
        if (selection.min == 0) 0 else it + 1
    }
    return if (text.startsWith(prefix, lineStart)) {
        copy(
            text = text.removeRange(lineStart, lineStart + prefix.length),
            selection = shift(-prefix.length, lineStart),
        )
    } else {
        copy(
            text = text.substring(0, lineStart) + prefix + text.substring(lineStart),
            selection = shift(prefix.length, lineStart),
        )
    }
}

private fun TextFieldValue.shift(by: Int, from: Int): TextRange {
    fun move(i: Int) = if (i >= from) (i + by).coerceAtLeast(from) else i
    return TextRange(move(selection.start), move(selection.end))
}

private fun TextFieldValue.wrap(left: String, right: String): TextFieldValue {
    val min = selection.min
    val max = selection.max
    val newText = text.substring(0, min) + left + text.substring(min, max) + right + text.substring(max)
    val newSelection = if (selection.collapsed) TextRange(min + left.length)
    else TextRange(min + left.length, max + left.length)
    return copy(text = newText, selection = newSelection)
}

private fun TextFieldValue.codeBlock(): TextFieldValue {
    val min = selection.min
    val max = selection.max
    val before = text.substring(0, min)
    val lead = if (before.isEmpty() || before.endsWith("\n")) "" else "\n"
    val block = "$lead```\n${text.substring(min, max)}\n```\n"
    val cursor = min + lead.length + 4 + (max - min)
    return copy(text = before + block + text.substring(max), selection = TextRange(cursor))
}

/** Inserts a multi-line block (e.g. a table) at the cursor, on its own lines. */
fun TextFieldValue.insertBlock(block: String): TextFieldValue {
    val at = selection.max
    val before = text.substring(0, at)
    val lead = when {
        before.isEmpty() || before.endsWith("\n\n") -> ""
        before.endsWith("\n") -> "\n"
        else -> "\n\n"
    }
    val inserted = lead + block
    return copy(text = before + inserted + text.substring(at), selection = TextRange(at + inserted.length))
}

/** Flips `- [ ]` ↔ `- [x]` on the given line (used by the checkbox in preview mode). */
fun String.toggleTaskAt(line: Int): String {
    val lines = lines().toMutableList()
    if (line !in lines.indices) return this
    val l = lines[line]
    lines[line] = when {
        l.contains("[ ]") -> l.replaceFirst("[ ]", "[x]")
        l.contains("[x]") -> l.replaceFirst("[x]", "[ ]")
        l.contains("[X]") -> l.replaceFirst("[X]", "[ ]")
        else -> l
    }
    return lines.joinToString("\n")
}
