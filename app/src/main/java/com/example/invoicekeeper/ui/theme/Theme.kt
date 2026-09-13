package com.example.invoicekeeper.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    primaryContainer = Teal90,
    onPrimaryContainer = Teal10,
    secondary = Steel40,
    onSecondary = Color.White,
    secondaryContainer = Steel90,
    onSecondaryContainer = Steel20,
    tertiary = Slate30,
    onTertiary = Color.White,
    tertiaryContainer = Slate90,
    onTertiaryContainer = Slate10,
    background = Slate98,
    onBackground = Slate10,
    surface = Slate99,
    onSurface = Slate10,
    surfaceVariant = Slate95,
    onSurfaceVariant = Slate30,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Slate99,
    surfaceContainer = Slate95,
    surfaceContainerHigh = Slate90,
    surfaceContainerHighest = Slate90,
    outline = Slate50,
    outlineVariant = Slate80,
    error = ErrorLight,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
)

private val DarkColors = darkColorScheme(
    primary = Teal80,
    onPrimary = Teal20,
    primaryContainer = Teal30,
    onPrimaryContainer = Teal95,
    secondary = Steel80,
    onSecondary = Steel20,
    secondaryContainer = Steel30,
    onSecondaryContainer = Steel90,
    tertiary = Slate80,
    onTertiary = Slate20,
    tertiaryContainer = Slate30,
    onTertiaryContainer = Slate90,
    background = Slate10,
    onBackground = Slate90,
    surface = Slate10,
    onSurface = Slate90,
    surfaceVariant = Slate25,
    onSurfaceVariant = Slate80,
    surfaceContainerLowest = Color(0xFF080C0C),
    surfaceContainerLow = Slate20,
    surfaceContainer = Slate20,
    surfaceContainerHigh = Slate25,
    surfaceContainerHighest = Slate30,
    outline = Slate60,
    outlineVariant = Slate30,
    error = ErrorDark,
    onError = Color(0xFF601410),
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
)

@Composable
fun InvoiceKeeperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Deliberately no dynamic colour. A tool for handling money should look the same on every
    // device so the semantic colours always mean the same thing.
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val semantic = if (darkTheme) DarkSemanticColors else LightSemanticColors

    CompositionLocalProvider(LocalSemanticColors provides semantic) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content,
        )
    }
}
