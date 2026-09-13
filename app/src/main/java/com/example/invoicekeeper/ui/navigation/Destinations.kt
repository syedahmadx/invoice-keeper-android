package com.example.invoicekeeper.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val HOME = "home"
    const val SCAN = "scan"
    const val INVOICES = "invoices"
    const val SETTINGS = "settings"

    const val INVOICE_DETAIL = "invoice/{invoiceId}"
    const val INVOICE_ID_ARG = "invoiceId"

    fun invoiceDetail(id: Long) = "invoice/$id"
}

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME(Routes.HOME, "Home", Icons.Outlined.Home),
    SCAN(Routes.SCAN, "Scan", Icons.Outlined.DocumentScanner),
    INVOICES(Routes.INVOICES, "Invoices", Icons.AutoMirrored.Outlined.ReceiptLong),
    SETTINGS(Routes.SETTINGS, "Settings", Icons.Outlined.Settings),
}
