package com.example.kapsul.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.example.kapsul.R

// Google Sans Flex is a variable font (axes: wght, wdth, opsz, slnt, ROND).
// We build two families from the same file: a crisp one for reading and a fully
// rounded one for the expressive display/headline roles.

private val Weights = listOf(
    FontWeight.Normal,
    FontWeight.Medium,
    FontWeight.SemiBold,
    FontWeight.Bold,
    FontWeight.ExtraBold,
)

@OptIn(ExperimentalTextApi::class)
private fun googleSansFlex(roundness: Float, width: Float) = FontFamily(
    Weights.map { weight ->
        Font(
            resId = R.font.google_sans_flex,
            weight = weight,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(weight.weight),
                FontVariation.width(width),
                FontVariation.Setting("ROND", roundness),
            ),
        )
    }
)

val GoogleSansFlex = googleSansFlex(roundness = 0f, width = 100f)
val GoogleSansFlexRounded = googleSansFlex(roundness = 100f, width = 100f)
val GoogleSansFlexRoundedWide = googleSansFlex(roundness = 100f, width = 112f)

private val base = Typography()

private fun TextStyle.rounded(weight: FontWeight) =
    copy(fontFamily = GoogleSansFlexRounded, fontWeight = weight)

private fun TextStyle.text(weight: FontWeight = fontWeight ?: FontWeight.Normal) =
    copy(fontFamily = GoogleSansFlex, fontWeight = weight)

val Typography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = GoogleSansFlexRoundedWide, fontWeight = FontWeight.SemiBold),
    displayMedium = base.displayMedium.copy(fontFamily = GoogleSansFlexRoundedWide, fontWeight = FontWeight.SemiBold),
    displaySmall = base.displaySmall.rounded(FontWeight.SemiBold),
    headlineLarge = base.headlineLarge.rounded(FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.rounded(FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.rounded(FontWeight.Medium),
    titleLarge = base.titleLarge.rounded(FontWeight.Medium),
    titleMedium = base.titleMedium.text(FontWeight.SemiBold),
    titleSmall = base.titleSmall.text(FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.text(),
    bodyMedium = base.bodyMedium.text(),
    bodySmall = base.bodySmall.text(),
    labelLarge = base.labelLarge.rounded(FontWeight.SemiBold),
    labelMedium = base.labelMedium.rounded(FontWeight.SemiBold),
    labelSmall = base.labelSmall.rounded(FontWeight.Medium),
)
