package com.example.invoicekeeper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.invoicekeeper.domain.model.InvoiceStatus
import com.example.invoicekeeper.domain.model.ReviewFinding
import com.example.invoicekeeper.domain.model.SavedInvoice
import com.example.invoicekeeper.ui.theme.LocalSemanticColors
import com.example.invoicekeeper.ui.theme.MoneySmallTextStyle
import com.example.invoicekeeper.ui.theme.MoneyTextStyle
import com.example.invoicekeeper.util.formatAmount
import com.example.invoicekeeper.util.formatTimestamp

/**
 * Names the model that produced a panel of results. The label comes from the provider string the
 * API returns, never from a constant in this app.
 */
@Composable
fun ProviderBadge(provider: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(6.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(Spacing.xs))
            Text(
                text = provider,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/** The standing reminder that this is a model reading a document, not a source of truth. */
@Composable
fun AiDisclaimer(modifier: Modifier = Modifier) {
    Text(
        text = "AI-extracted from your document. Check every figure against the original before " +
            "sending it to your accounting system.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun StatusChip(status: InvoiceStatus, modifier: Modifier = Modifier) {
    val semantic = LocalSemanticColors.current
    val container: Color
    val content: Color
    when (status) {
        InvoiceStatus.SENT -> {
            container = semantic.successContainer
            content = semantic.onSuccessContainer
        }
        InvoiceStatus.DRAFT -> {
            container = MaterialTheme.colorScheme.surfaceContainerHighest
            content = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }
    Surface(modifier = modifier, color = container, contentColor = content, shape = RoundedCornerShape(6.dp)) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 3.dp),
        )
    }
}

@Composable
fun FindingRow(finding: ReviewFinding, modifier: Modifier = Modifier) {
    val semantic = LocalSemanticColors.current
    val scheme = MaterialTheme.colorScheme

    val accent: Color
    val container: Color
    val content: Color
    val icon = when (finding.normalisedSeverity) {
        ReviewFinding.Severity.ERROR -> {
            accent = scheme.error
            container = scheme.errorContainer
            content = scheme.onErrorContainer
            Icons.Outlined.ErrorOutline
        }
        ReviewFinding.Severity.WARNING -> {
            accent = semantic.warning
            container = semantic.warningContainer
            content = semantic.onWarningContainer
            Icons.Outlined.WarningAmber
        }
        else -> {
            accent = semantic.info
            container = semantic.infoContainer
            content = semantic.onInfoContainer
            Icons.Outlined.Info
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(modifier = Modifier.height(intrinsicSize = IntrinsicSize.Min)) {
            Spacer(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accent),
            )
            Row(modifier = Modifier.padding(Spacing.md)) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.md))
                Column {
                    Text(
                        text = finding.field.ifBlank { "General" },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(text = finding.message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun InvoiceCard(
    invoice: SavedInvoice,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = invoice.supplier,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = invoice.invoiceNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(Spacing.md))
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = formatAmount(invoice.total), style = MoneyTextStyle)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = invoice.currency,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatTimestamp(invoice.savedAt),
                    style = MoneySmallTextStyle.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Start),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StatusChip(invoice.status)
            }
        }
    }
}
