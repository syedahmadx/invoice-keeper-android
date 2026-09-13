package com.example.invoicekeeper.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Material 3 ships an error role but no warning / success / info roles. Money tooling needs all
 * four, so they travel alongside the ColorScheme in a CompositionLocal.
 */
@Immutable
data class SemanticColors(
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
)

val LightSemanticColors = SemanticColors(
    warning = WarningLight,
    warningContainer = WarningContainerLight,
    onWarningContainer = OnWarningContainerLight,
    success = SuccessLight,
    successContainer = SuccessContainerLight,
    onSuccessContainer = OnSuccessContainerLight,
    info = InfoLight,
    infoContainer = InfoContainerLight,
    onInfoContainer = OnInfoContainerLight,
)

val DarkSemanticColors = SemanticColors(
    warning = WarningDark,
    warningContainer = WarningContainerDark,
    onWarningContainer = OnWarningContainerDark,
    success = SuccessDark,
    successContainer = SuccessContainerDark,
    onSuccessContainer = OnSuccessContainerDark,
    info = InfoDark,
    infoContainer = InfoContainerDark,
    onInfoContainer = OnInfoContainerDark,
)

val LocalSemanticColors = staticCompositionLocalOf { LightSemanticColors }
