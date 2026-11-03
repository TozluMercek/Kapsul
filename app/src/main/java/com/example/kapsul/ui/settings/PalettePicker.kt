@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.example.kapsul.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.kapsul.data.DEFAULT_SEED_COLOR
import com.materialkolor.PaletteStyle
import com.materialkolor.ktx.toDynamicScheme
import com.materialkolor.ktx.toneColor

private data class Seed(val name: String, val argb: Int)

/** One pager page per seed; each page offers the seed in four styles (11 × 4 = 44 palettes). */
private val Seeds = listOf(
    Seed("Kapsül", DEFAULT_SEED_COLOR),
    Seed("Orman", 0xFF4C8C2B.toInt()),
    Seed("Zeytin", 0xFF8A9A2B.toInt()),
    Seed("Hardal", 0xFFE0A800.toInt()),
    Seed("Turuncu", 0xFFE8710A.toInt()),
    Seed("Mercan", 0xFFD9483B.toInt()),
    Seed("Pembe", 0xFFD81B60.toInt()),
    Seed("Mor", 0xFF8E44AD.toInt()),
    Seed("Çivit", 0xFF3F51B5.toInt()),
    Seed("Gökyüzü", 0xFF1A73E8.toInt()),
    Seed("Kahve", 0xFF8D6E63.toInt()),
)

private val Styles = listOf(
    PaletteStyle.TonalSpot to "Sakin",
    PaletteStyle.Neutral to "Nötr",
    PaletteStyle.Vibrant to "Canlı",
    PaletteStyle.Expressive to "Etkileyici",
)

/** Colors shown in a swatch, the same split Android's wallpaper & style picker uses. */
private data class Swatch(val top: Color, val bottomStart: Color, val bottomEnd: Color, val badge: Color, val check: Color)

private fun swatchFor(seed: Int, style: PaletteStyle): Swatch {
    val scheme = Color(seed).toDynamicScheme(isDark = false, style = style)
    return Swatch(
        top = scheme.primaryPalette.toneColor(80),
        bottomStart = scheme.secondaryPalette.toneColor(90),
        bottomEnd = scheme.tertiaryPalette.toneColor(60),
        badge = scheme.primaryPalette.toneColor(30),
        check = scheme.primaryPalette.toneColor(90),
    )
}

/**
 * @param selected the active (seed, style), or null while wallpaper colors are in use.
 */
@Composable
fun PalettePicker(
    selected: Pair<Int, PaletteStyle>?,
    onSelect: (seed: Int, style: PaletteStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialPage = Seeds.indexOfFirst { it.argb == selected?.first }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage) { Seeds.size }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 16.dp,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val seed = Seeds[page]
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Styles.forEach { (style, styleName) ->
                    PaletteTile(
                        seed = seed.argb,
                        style = style,
                        label = "${seed.name}, $styleName",
                        selected = selected?.first == seed.argb && selected.second == style,
                        onClick = { onSelect(seed.argb, style) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        PageDots(
            count = Seeds.size,
            current = pagerState.currentPage,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun PaletteTile(
    seed: Int,
    style: PaletteStyle,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val swatch = remember(seed, style) { swatchFor(seed, style) }
    val circleScale by animateFloatAsState(
        targetValue = if (selected) 0.92f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "swatchScale",
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(24.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label },
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(14.dp)) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .scale(circleScale)
            ) {
                val d = size.minDimension
                val topLeft = Offset((size.width - d) / 2, (size.height - d) / 2)
                val arcSize = Size(d, d)
                // Compose arcs start at 3 o'clock and run clockwise.
                drawArc(swatch.top, 180f, 180f, useCenter = true, topLeft = topLeft, size = arcSize)
                drawArc(swatch.bottomStart, 90f, 90f, useCenter = true, topLeft = topLeft, size = arcSize)
                drawArc(swatch.bottomEnd, 0f, 90f, useCenter = true, topLeft = topLeft, size = arcSize)
            }
            AnimatedVisibility(visible = selected, enter = scaleIn(), exit = scaleOut()) {
                Box(
                    Modifier
                        .fillMaxSize(0.62f)
                        .background(swatch.badge, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = swatch.check,
                        modifier = Modifier.fillMaxSize(0.55f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { index ->
            val color by animateColorAsState(
                if (index == current) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                label = "dot",
            )
            Box(
                Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
        }
    }
}
