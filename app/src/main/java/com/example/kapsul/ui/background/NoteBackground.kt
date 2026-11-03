@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.kapsul.ui.background

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.FormatColorReset
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.materialkolor.rememberDynamicColorScheme

/** A note's background, stored on the note as a short string (see [encode] / [decode]). */
sealed interface NoteBackground {
    data class Solid(val seed: Int) : NoteBackground
    data class Gradient(val from: Int, val to: Int) : NoteBackground

    fun encode(): String = when (this) {
        is Solid -> "solid:$seed"
        is Gradient -> "gradient:$from:$to"
    }

    companion object {
        fun decode(value: String?): NoteBackground? {
            val parts = value?.split(':') ?: return null
            val n = parts.drop(1).mapNotNull { it.toIntOrNull() }
            return when {
                parts[0] == "solid" && n.size == 1 -> Solid(n[0])
                parts[0] == "gradient" && n.size == 2 -> Gradient(n[0], n[1])
                else -> null
            }
        }
    }
}

private val SolidSeeds = listOf(
    0xFFE57373, 0xFFFF8A65, 0xFFFFB74D, 0xFFFFD54F, 0xFFAED581, 0xFF4DB6AC,
    0xFF4FC3F7, 0xFF7986CB, 0xFFBA68C8, 0xFFF06292, 0xFFA1887F, 0xFF90A4AE,
).map { NoteBackground.Solid(it.toInt()) }

private val GradientSeeds = listOf(
    0xFFFF8A65 to 0xFFF06292, // gün batımı
    0xFF4FC3F7 to 0xFF7986CB, // okyanus
    0xFFAED581 to 0xFF4DB6AC, // orman
    0xFFBA68C8 to 0xFFF06292, // lavanta
    0xFFFFD54F to 0xFFFF8A65, // şeftali
    0xFF4DB6AC to 0xFF4FC3F7, // lagün
    0xFF7986CB to 0xFFBA68C8, // alacakaranlık
    0xFFA1887F to 0xFFFFB74D, // kum
).map { (a, b) -> NoteBackground.Gradient(a.toInt(), b.toInt()) }

/** What a background looks like right now: the brush to paint and a matching color scheme for content. */
data class BackgroundColors(val brush: Brush, val scheme: ColorScheme)

/**
 * Colors are generated from the seed for the current light/dark mode, so text on the
 * background keeps its contrast in both.
 */
@Composable
fun rememberBackgroundColors(background: NoteBackground?): BackgroundColors? {
    if (background == null) return null
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return when (background) {
        is NoteBackground.Solid -> {
            val scheme = rememberDynamicColorScheme(seedColor = Color(background.seed), isDark = dark)
            remember(scheme) { BackgroundColors(SolidColor(scheme.primaryContainer), scheme) }
        }
        is NoteBackground.Gradient -> {
            val a = rememberDynamicColorScheme(seedColor = Color(background.from), isDark = dark)
            val b = rememberDynamicColorScheme(seedColor = Color(background.to), isDark = dark)
            remember(a, b) {
                BackgroundColors(Brush.linearGradient(listOf(a.primaryContainer, b.primaryContainer)), a)
            }
        }
    }
}

/** "Arka plan stili": tap a swatch to apply it right away. */
@Composable
fun BackgroundSheet(current: NoteBackground?, onSelect: (NoteBackground?) -> Unit, onDismiss: () -> Unit) {
    var gradients by remember { mutableStateOf(current is NoteBackground.Gradient) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).navigationBarsPadding()) {
            Text("Arka plan stili", style = MaterialTheme.typography.headlineSmall)
            Row(Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !gradients, onClick = { gradients = false }, label = { Text("Düz renk") })
                FilterChip(selected = gradients, onClick = { gradients = true }, label = { Text("Gradyan") })
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                item { DefaultSwatch(selected = current == null, onClick = { onSelect(null) }) }
                items(if (gradients) GradientSeeds else SolidSeeds) { bg ->
                    Swatch(bg, selected = bg == current, onClick = { onSelect(bg) })
                }
            }
        }
    }
}

@Composable
private fun Swatch(background: NoteBackground, selected: Boolean, onClick: () -> Unit) {
    val colors = rememberBackgroundColors(background) ?: return
    SwatchBox(selected, onClick, Modifier.background(colors.brush)) {
        if (selected) Icon(Icons.Filled.Check, contentDescription = "Seçili", tint = colors.scheme.onPrimaryContainer)
    }
}

@Composable
private fun DefaultSwatch(selected: Boolean, onClick: () -> Unit) {
    SwatchBox(selected, onClick, Modifier.background(MaterialTheme.colorScheme.surface)) {
        Icon(
            if (selected) Icons.Filled.Check else Icons.Outlined.FormatColorReset,
            contentDescription = "Varsayılan",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SwatchBox(selected: Boolean, onClick: () -> Unit, fill: Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(if (selected) 28.dp else 18.dp)
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(shape)
            .then(fill)
            .border(
                if (selected) 3.dp else 1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}
