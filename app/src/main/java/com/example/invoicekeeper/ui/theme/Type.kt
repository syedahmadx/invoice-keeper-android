package com.example.invoicekeeper.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

private val Default = Typography()

val AppTypography = Typography(
    headlineMedium = Default.headlineMedium.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
    ),
    headlineSmall = Default.headlineSmall.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = Default.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Default.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = Default.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = Default.labelSmall.copy(letterSpacing = 0.6.sp),
)

/**
 * Tabular figures, so currency columns line up no matter which digits appear.
 */
val MoneyTextStyle: TextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontFeatureSettings = "tnum",
    fontWeight = FontWeight.Medium,
    fontSize = 16.sp,
    textAlign = TextAlign.End,
)

val MoneyTotalTextStyle: TextStyle = MoneyTextStyle.copy(
    fontSize = 22.sp,
    fontWeight = FontWeight.SemiBold,
)

val MoneySmallTextStyle: TextStyle = MoneyTextStyle.copy(
    fontSize = 14.sp,
    fontWeight = FontWeight.Normal,
)
