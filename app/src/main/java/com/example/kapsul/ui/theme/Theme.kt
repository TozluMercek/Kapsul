package com.example.kapsul.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.kapsul.data.DEFAULT_SEED_COLOR
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

private val KapsulShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

/** Black background and surfaces; containers step up in small near-black increments. */
private fun ColorScheme.toAmoled() = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF111111),
    surfaceContainerHigh = Color(0xFF181818),
    surfaceContainerHighest = Color(0xFF202020),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KapsulTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Wallpaper colors, available on Android 12+
    dynamicColor: Boolean = true,
    // Used when dynamic color is off: the whole scheme is generated from this seed.
    seedColor: Int = DEFAULT_SEED_COLOR,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    // Pure black backgrounds in dark mode; saves power on OLED screens.
    amoled: Boolean = false,
    content: @Composable () -> Unit
) {
    val base = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        rememberDynamicColorScheme(seedColor = Color(seedColor), isDark = darkTheme, style = paletteStyle)
    }
    val colorScheme = if (darkTheme && amoled) base.toAmoled() else base

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = KapsulShapes,
        typography = Typography,
        content = content
    )
}
