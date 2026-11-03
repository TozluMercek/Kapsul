package com.example.kapsul.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KapsulTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Wallpaper colors, available on Android 12+
    dynamicColor: Boolean = true,
    // Used when dynamic color is off: the whole scheme is generated from this seed.
    seedColor: Int = DEFAULT_SEED_COLOR,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    content: @Composable () -> Unit
) {
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        rememberDynamicColorScheme(seedColor = Color(seedColor), isDark = darkTheme, style = paletteStyle)
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = KapsulShapes,
        typography = Typography,
        content = content
    )
}
