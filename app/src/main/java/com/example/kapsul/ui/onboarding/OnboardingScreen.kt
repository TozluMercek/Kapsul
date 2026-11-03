@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.example.kapsul.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import kotlinx.coroutines.launch
import kotlin.math.floor

private data class IntroPage(
    val shape: RoundedPolygon,
    val icon: ImageVector,
    val title: String,
    val body: String,
)

private val Pages = listOf(
    IntroPage(
        MaterialShapes.Cookie9Sided, Icons.Filled.AutoAwesome,
        "Kapsül'e hoş geldin",
        "Notların, ses kayıtların, fotoğrafların ve belgelerin için kişisel bir dijital hafıza.",
    ),
    IntroPage(
        MaterialShapes.Sunny, Icons.Outlined.Bolt,
        "Aklındakini anında bırak",
        "Hızlı not, ses kaydı, kamera ve belge tarama tek dokunuş uzağında. Uygulama simgesine basılı tutarak da başlayabilirsin.",
    ),
    IntroPage(
        MaterialShapes.Flower, Icons.Outlined.Sell,
        "Düzeni Kapsül kurar",
        "Sen yazarken başlıklar ve etiketler kendiliğinden oluşur; ilgili notlar birbirine bağlanır.",
    ),
    IntroPage(
        MaterialShapes.SoftBurst, Icons.Outlined.Search,
        "Hafızana sor",
        "“Geçen ay ev tamiri için hangi malzemeyi konuşmuştuk?” gibi sorulara notlarından yanıt bulur.",
    ),
    IntroPage(
        MaterialShapes.Clover4Leaf, Icons.Outlined.Palette,
        "Sana göre",
        "Renk paletleri, dinamik renk ve koyu mod ile Kapsül senin tarzında.",
    ),
    IntroPage(
        MaterialShapes.Cookie4Sided, Icons.Outlined.Lock,
        "Her şey cihazında",
        "Kapsül'ün internet izni yok. Notların hiçbir sunucuya gitmez; yalnızca senindir.",
    ),
)

/** First-run introduction: swipe through what Kapsül does, then start. */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState { Pages.size }
    val scope = rememberCoroutineScope()
    val lastPage = pagerState.currentPage == Pages.lastIndex
    val colors = MaterialTheme.colorScheme
    val containers = listOf(
        colors.primaryContainer, colors.tertiaryContainer, colors.secondaryContainer,
        colors.primaryContainer, colors.tertiaryContainer, colors.secondaryContainer,
    )
    val onContainers = listOf(
        colors.onPrimaryContainer, colors.onTertiaryContainer, colors.onSecondaryContainer,
        colors.onPrimaryContainer, colors.onTertiaryContainer, colors.onSecondaryContainer,
    )
    val morphs = remember { Pages.zipWithNext { a, b -> Morph(a.shape, b.shape) } }
    val spin by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation",
    )

    Surface(color = colors.surface, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                AnimatedVisibility(visible = !lastPage, enter = fadeIn(), exit = fadeOut()) {
                    TextButton(onClick = onFinish) { Text("Atla") }
                }
            }

            // The shape morphs into the next page's shape as you swipe.
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                val position = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                    .coerceIn(0f, Pages.lastIndex.toFloat())
                val index = floor(position).toInt().coerceAtMost(Pages.lastIndex - 1)
                val progress = position - index
                val fill = lerp(containers[index], containers[index + 1], progress)
                val path = remember { Path() }
                Canvas(Modifier.size(260.dp)) {
                    path.rewind()
                    morphs[index].toPath(progress, path)
                    rotate(spin) {
                        scale(size.width, size.height, pivot = Offset.Zero) { drawPath(path, fill) }
                    }
                }
                AnimatedContent(
                    targetState = pagerState.currentPage,
                    transitionSpec = { (scaleIn() + fadeIn()).togetherWith(scaleOut() + fadeOut()) },
                    label = "icon",
                ) { page ->
                    Icon(Pages[page].icon, contentDescription = null, tint = onContainers[page], modifier = Modifier.size(84.dp))
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().height(200.dp)) { page ->
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(Pages[page].title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        Pages[page].body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    repeat(Pages.size) { i ->
                        val width by animateDpAsState(if (i == pagerState.currentPage) 24.dp else 8.dp, label = "dot")
                        Box(
                            Modifier
                                .height(8.dp)
                                .width(width)
                                .background(if (i == pagerState.currentPage) colors.primary else colors.outlineVariant, CircleShape)
                        )
                    }
                }
                Button(
                    onClick = {
                        if (lastPage) onFinish()
                        else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.height(56.dp),
                ) {
                    Text(if (lastPage) "Başla" else "İleri", style = MaterialTheme.typography.titleMedium)
                    if (!lastPage) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }
}
